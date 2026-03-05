package com.punchh.server.Integration2;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import com.github.javafaker.Faker;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.Utilities;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;

import io.restassured.response.Response;

@SuppressWarnings("static-access")
public class IntUtils {
	SeleniumUtilities selUtils;
	public WebDriver driver;
	private PageObj pageObj;
	private String env;
	private String guestIdentityhost = "guestIdentity";
	private Utilities utils;
	private Faker faker;
	private String baseUrl;
	private String auth0FromEmail;

	public IntUtils(WebDriver driver) {
		this.driver = driver;
		this.utils = new Utilities(driver);
		this.pageObj = new PageObj(driver);
		this.faker = new Faker();
		this.auth0FromEmail = utils.getConfigProperty("auth0.fromEmail");
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
	}

	public IntUtils() {
		this.utils = new Utilities();
		this.pageObj = new PageObj();
		this.faker = new Faker();
		env = pageObj.getEnvDetails().setEnv();
		auth0FromEmail = utils.getConfigProperty("auth0.fromEmail");
	}

	// Verify using JWT token as authorisation in few API2, API1 and Auth API's
	public void verifyGISAccessTokenWithPunchhAPIs(String client, String secret, String jwtToken) {
		// Mobile API2 - user balace API
		Response balMobileAPI2Resp = pageObj.endpoints().Api2FetchUserBalance(client, secret, jwtToken); // api2/mobile/balance_timelines
		Assert.assertEquals(balMobileAPI2Resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v2 mobile users balance");
		utils.logPass("User balance API is verified with JWT token");

		// Mobile API2 - get user offers API
		Response offerMobileAPI2Resp = pageObj.endpoints().getUserOffers(jwtToken, client, secret); // api2/mobile/offers
		Assert.assertEquals(offerMobileAPI2Resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		utils.logPass("User offers API is verified with JWT token");

		// Auth - Account History
		Response accHistAuthResp = pageObj.endpoints().authApiAccountHistory(jwtToken, client, secret); // api/auth/checkins/balance
		Assert.assertEquals(accHistAuthResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth account history");
		utils.logPass("Account history API is verified with JWT token");

		// API1 - Fetch User Notifications
		Response fetchUserOffersResponse = pageObj.endpoints().api1FetchNotifications(client, secret, jwtToken); // api/mobile/users/notifications
		Assert.assertEquals(fetchUserOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 fetch user notifications");
		utils.logPass("User notifications API is verified with JWT token");

		utils.logit("Pass", "JWT token is verified with mobile and auth API's");
	}

	// Update existing phone only user data so phone can be reused
	public void updateExistingPhoneOnlyUserInDB(String env, String client, String phone) throws Exception {

		// Get Punchh business id and external_identity_uuid
		String query = "select concat(id, '|', external_identity_uuid) as punchhBusinessIdAndUuid from businesses where id in (select owner_id from oauth_applications where uid = '"
				+ client + "')";
		String punchhBusinessIdAndUuid = DBUtils.executeQueryAndGetColumnValue(env, query, "punchhBusinessIdAndUuid");
		String punchhBusinessId = punchhBusinessIdAndUuid.split("\\|")[0],
				externalIdentityUuid = punchhBusinessIdAndUuid.split("\\|")[1];
		Assert.assertTrue(!punchhBusinessId.equals(""), "Punchh business ID is empty");
		Assert.assertTrue(!externalIdentityUuid.equals(""), "External identity UUID is empty");

		// Get guest identity business_id
		query = "select id from businesses where uuid ='" + externalIdentityUuid + "';";
		String identityBusinessId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		// Assert.assertTrue(!identityBusinessId.equals(""), "Identity business ID is
		// empty");

		utils.logit("Updating existing phone user data for phone: " + phone + " in business_id: " + punchhBusinessId);

		// Update Punchh users table
		query = "UPDATE users SET email = REPLACE(email, '" + phone + "', UUID()), phone = UUID() "
				+ "WHERE business_id = " + punchhBusinessId + " AND (phone = '" + phone + "' OR email LIKE '" + phone
				+ "%')";
		DBUtils.executeQuery(env, query);
		utils.logPass("Successfully updated existing phone user in Punchh users table");

		// Update guest_identity users table
		query = "UPDATE users SET email = REPLACE(email, '" + phone + "', UUID()), phone_number = UUID() "
				+ "WHERE business_id = " + identityBusinessId + " AND (phone_number = '" + phone + "' OR email LIKE '"
				+ phone + "%')";
		DBUtils.executeQuery(env, guestIdentityhost, query);
		utils.logPass("Successfully updated existing phone user in guest_identity users table");

		// Update guest_identity auth_requests table
		query = "UPDATE auth_requests SET communication_channel = REPLACE(communication_channel, '" + phone
				+ "', UUID()) " + "WHERE business_mapping_id = " + identityBusinessId
				+ " AND communication_channel LIKE '%" + phone + "%'";
		DBUtils.executeQuery(env, guestIdentityhost, query);
		utils.logPass("Successfully updated existing phone user in guest_identity auth_requests table");

		utils.logit("Pass", "All database updates completed successfully for phone: " + phone);
	}

	// Get token (auth_token or OTP) from email or SMS body
	public String getTokenFromMessage(String emailorSMSBody) throws Exception {
		// Pattern for extracting auth_token/OTP from the URL
		String magicLinkPattern = "auth_token=([a-zA-Z0-9]+)";
		String otpPattern = "\\b\\d{6,12}\\b";

		// Check for auth_token in the email body
		Pattern pattern = Pattern.compile(magicLinkPattern);
		Matcher matcher = pattern.matcher(emailorSMSBody);
		if (matcher.find()) {
			utils.logit("Found magic_link in email body : " + matcher.group(1));
			return matcher.group(1);
		}

		// Check for OTP in the email or SMS body
		pattern = Pattern.compile(otpPattern);
		matcher = pattern.matcher(emailorSMSBody);
		if (matcher.find()) {
			utils.logit("Found OTP in email/SMS body: " + matcher.group(0));
			return matcher.group(0);
		}
		utils.logit("Fail", "No auth_token or OTP found in the email/SMS body.");
		Assert.fail("No auth_token or OTP found in the email/SMS body.");
		return null;
	}

	public String generateAuthDigest(String secret, String authDigestToken) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(secretKeySpec);
			byte[] hmacBytes = mac.doFinal(authDigestToken.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hmacBytes) {
				String hex = String.format("%02x", b);
				hexString.append(hex);
			}
			Assert.assertTrue(hexString.length() > 0, "HMAC is not generated successfully");
			utils.logPass("HMAC is generated successfully");
			return hexString.toString();
		} catch (Exception e) {
			utils.logit("Fail", "HMAC generation failed: " + e.getMessage());
			Assert.fail("HMAC generation failed");
			return null;
		}
	}

	public String getRandomGmailEmail() {
		return utils.getConfigProperty("gmail.username") + "+" + utils.getTimestampInNanoseconds() + "@gmail.com";
	}

	public void validateGIS_SignUp_SignIn_Response(Response response) {
		SoftAssert softAssert = new SoftAssert();
		String jsonString = response.getBody().asString();
		JSONObject jsonObj = new JSONObject(jsonString);
		JSONObject data = jsonObj.getJSONObject("data");

		softAssert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_OK || response.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Expected status code 200 or 201 for Basic Auth Sign Up/Sign In");
		softAssert.assertFalse(data.getString("access_token").isEmpty(),
				"access_token is empty in Basic Auth Sign Up/Sign In response");
		softAssert.assertFalse(data.getString("refresh_token").isEmpty(),
				"refresh_token is empty in Basic Auth Sign Up/Sign In response");
		softAssert.assertTrue(utils.isJwtToken(data.getString("access_token")), "access_token is not a valid JWT");
		int expiresIn = data.getInt("expires_in");
		softAssert.assertTrue(expiresIn > 0, "'expires_in' is not greater than 0");
		softAssert.assertAll();

		utils.logit("Pass", "GIS Sign Up/Sign In response is validated successfully");
	}

	public String punchhUserSignUpandValidateSyncWithIdentity(String client, String secret, String email, Long phone)
			throws Exception {
		Response signUpRes = null;

		// Register a new user via API2 SignUp
		utils.logit("Registering a new user via API2 SignUp");
		if (phone == null || phone == 0)
			signUpRes = pageObj.endpoints().Api2SignUp(email, client, secret);
		else
			signUpRes = pageObj.endpoints().Api2SignUp(email, client, secret, phone);

		Assert.assertEquals(signUpRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API2 Punchh SignUp failed");
		String punchhUserId = signUpRes.jsonPath().get("user.user_id").toString();
		Assert.assertTrue(!punchhUserId.equals(""), "User ID is empty in Punchh SignUp response");
		utils.logit("Punchh SignUp is successful with user ID: " + punchhUserId);

		// Validate user in identity DB
		validateUserSyncWithGIS(punchhUserId);
		return punchhUserId;
	}

	// User sync with Guest Identity validation including social login details
	public String validateUserSyncWithGIS(String punchhUserId) throws Exception {

		// Get user details from Punchh DB
		String query = "SELECT "
				// Standard user profile fields
				+ "u.email, " + "u.business_id, " + "u.identity_uuid, " 
				
				// Safely include phone number, fallback to empty string if null
				+ "COALESCE(u.phone, '') AS phone, "

				// Extract the portion after ':original_phone:'
				// 1. Get everything after ':original_phone:'
				// 2. Extract the line (up to newline character)
				// 3. Trim any whitespace and double quotes
				// 4. Finally, Trim single quotes
				+ "CASE WHEN u.user_details LIKE '%:original_phone:%' THEN "
				+ "TRIM(BOTH '\\'' FROM TRIM(BOTH '\"' FROM TRIM(BOTH ' ' FROM "
				+ "SUBSTRING_INDEX(SUBSTRING_INDEX(u.user_details, ':original_phone:', -1), '\\n', 1)))) "

				// If :original_phone: not found, return empty string
				+ "ELSE '' END AS original_phone, "

				// Check subscription status range (32 to 63) and return 'true' or 'false'
				+ "CASE WHEN u.subscription_status BETWEEN 32 AND 63 THEN 'true' ELSE 'false' END AS is_subscribed, "

				// Social identifiers
				+ "COALESCE(u.fb_uid, '') AS facebook_id, "
				+ "COALESCE(google.source_id, '') AS google_id, "
				+ "COALESCE(apple.source_id, '') AS apple_id "

				+ "FROM users u "

				// Retrieve the most recent Google identifier associated with the user
				+ "LEFT JOIN ("
				+ "  SELECT user_id, source_id FROM user_external_identifiers "
				+ "  WHERE source = 'google' AND user_id = '" + punchhUserId + "' "
				+ "  ORDER BY id DESC LIMIT 1"
				+ ") google ON u.id = google.user_id "

				// Retrieve the most recent Apple identifier associated with the user
				+ "LEFT JOIN ("
				+ "  SELECT user_id, source_id FROM user_external_identifiers "
				+ "  WHERE source = 'apple' AND user_id = '" + punchhUserId + "' "
				+ "  ORDER BY id DESC LIMIT 1"
				+ ") apple ON u.id = apple.user_id "

				// Filter by the specific primary user ID
				+ "WHERE u.id = '" + punchhUserId + "';";

		List<Map<String, String>> userDetails = DBUtils.executeQueryAndGetAllRows(env, query);
		Map<String, String> userDetailsMap = userDetails.get(0);
		Assert.assertTrue(!userDetailsMap.isEmpty(), "User not found in Punchh DB");

		String email = userDetailsMap.get("email");
		String punchhBusinessId = userDetailsMap.get("business_id");
		String identityUuid = userDetailsMap.get("identity_uuid");
		String phoneNumber = userDetailsMap.get("phone");
		String originalPhoneNumber = userDetailsMap.get("original_phone");
		String deactivated = userDetailsMap.get("is_subscribed");
		String facebookId = userDetailsMap.get("facebook_id");
		String googleId = userDetailsMap.get("google_id");
		String appleId = userDetailsMap.get("apple_id");

		utils.logit("User details from Punchh DB:\n" + "ID: " + punchhUserId + "\n" + "Identity UUID: " + identityUuid
				+ "\n" + "Email: " + email + "\n" + "Business ID: " + punchhBusinessId + "\n" + "Phone: " + phoneNumber
				+ "\n" + "Original Phone: " + originalPhoneNumber + "\n" + "Deactivation Status: " + deactivated
				+ "\n" + "Facebook ID: " + facebookId + "\n" + "Google ID: " + googleId + "\n" + "Apple ID: " + appleId);

		// Get punchh business Uuid
		query = "SELECT external_identity_uuid FROM businesses WHERE id = '" + punchhBusinessId + "'";
		String punchhBusinessUuid = DBUtils.executeQueryAndGetColumnValue(env, query, "external_identity_uuid");
		Assert.assertTrue(!punchhBusinessUuid.equals(""), "Punchh business UUID is empty");

		// Get identity business_id from UUID
		query = "SELECT id FROM businesses WHERE uuid = '" + punchhBusinessUuid + "'";
		String identityBusinessId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		Assert.assertTrue(!identityBusinessId.equals(""), "Guest identity business_id is empty");

		// Validate same user details in guest identity DB
		query = "SELECT id FROM users WHERE email = '" + email + "'" + " AND business_id = '" + identityBusinessId + "'"
				+ " AND uuid = '" + identityUuid + "'" + " AND COALESCE(phone_number, '') = '" + phoneNumber + "'"
				+ " AND COALESCE(original_phone_number, '') = '" + originalPhoneNumber + "'"
				+ " AND ( preferences IS NULL OR preferences NOT LIKE '%:deactivated:%'"
				+ " OR TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(preferences, ':deactivated:', -1), '\n', 1)) = '"
				+ deactivated + "' )" + " AND COALESCE(facebook_source_id, '') = '" + facebookId + "'"
				+ " AND COALESCE(google_source_id, '') = '" + googleId + "'" + " AND COALESCE(apple_source_id, '') = '" + appleId + "'";

		// With polling as waiting for user to sync in identity DB
		String guestIdentityUserId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query,
				"id", 30);
		Assert.assertTrue(!guestIdentityUserId.equals(""), "User not found in Guest Identity DB");
		utils.logit("Pass", "User properly synced in Guest Identity DB");
		return guestIdentityUserId;
	}

	public void verifyErrorResponse(Response response, int expectedStatusCode, String errorField,
			String expectedErrorMsg, String apiName) {
		Assert.assertEquals(response.getStatusCode(), expectedStatusCode,
				"Status code did not match for: '" + apiName + "'");
		Assert.assertTrue(response.jsonPath().getString(errorField).contains(expectedErrorMsg),
				"Error message did not match for: '" + apiName + "'");
		utils.logit("Proper error message is displayed for: " + apiName);
	}

	public String fetchTwillioLatestMessageBody(String toPhoneNumber, ZonedDateTime cutoffUtc, int pollingAttempts)
			throws Exception {
		String accountSid = utils.decrypt(utils.getConfigProperty("twillio.accountSID"));
		String authToken = utils.decrypt(utils.getConfigProperty("twillio.authToken"));
		String fromPhoneNumber = utils.getConfigProperty("twillio.fromPhoneNumber");

		Twilio.init(accountSid, authToken);
		utils.logit("Twilio initialized.");

		// Convert to IST for display
		ZoneId istZone = ZoneId.of("Asia/Kolkata");
		ZonedDateTime cutoffIst = cutoffUtc.withZoneSameInstant(istZone);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		System.out.println("Cutoff IST Time: " + cutoffIst.format(formatter));

		for (int attempt = 1; attempt <= pollingAttempts; attempt++) {
			utils.logit("Polling attempt " + attempt + " of " + pollingAttempts);
			ResourceSet<Message> messages = Message.reader().setTo(new com.twilio.type.PhoneNumber(toPhoneNumber))
					.setFrom(new com.twilio.type.PhoneNumber(fromPhoneNumber)).setDateSent(cutoffUtc).limit(1).read();

			// Check if any message was found
			if (messages.iterator().hasNext()) {
				Message latest = messages.iterator().next();
				utils.logit("Latest message found: " + latest.getBody() + " at " + latest.getDateSent());
				return latest.getBody();
			}

			if (attempt < pollingAttempts) {
				utils.logit("No message found in attempt " + attempt + ". Retrying after 5 seconds...");
				utils.longWaitInSeconds(5);
			}
		}
		utils.logit("Warn", "No message found after " + pollingAttempts + " polling attempts");
		return null;
	}

	public String[] userSignUpSignInAdvanceAuth(String client, String communicationChannel, String clientType,
			String activity) throws Exception {
		utils.logit("AdvanceAuth OTP request and verification with client type: " + clientType
				+ " and communication channel: " + communicationChannel);

		String codeVerifier = utils.generateCodeVerifier(32);
		String codeChallenge = utils.generateCodeChallenge(codeVerifier);
		boolean privacyPolicy = true;
		boolean tAndc = true;

		// Record current time in UTC (use millisecond precision for parameterized
		// tests)
		ZonedDateTime cutoffUtc = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

		String communicationChannelType = communicationChannel.toLowerCase().contains(".com") ? "email" : "sms";

		// STEP 1: Generate OTP - Token API
		Response responseToken = null;
		if (communicationChannelType.equals("email")) {
			responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", communicationChannel, null, null,
					codeChallenge, privacyPolicy, tAndc);
		} else {
			// Update existing phone user in DB in case of signup
			if (activity.equals("SignUp"))
				updateExistingPhoneOnlyUserInDB(env, client, communicationChannel);
			responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", null, "+91", communicationChannel,
					codeChallenge, privacyPolicy, tAndc);
		}
		Assert.assertEquals(responseToken.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(responseToken.jsonPath().getList("data.message").contains("OTP sent successfully."));
		utils.logit("Advance auth token API successful, OTP sent successfully on " + communicationChannel);

		// STEP 2: Extract OTP from Email/SMS
		String token;
		if (communicationChannelType.equals("email")) {
			String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail",
					communicationChannel + "," + auth0FromEmail, true);
			token = getTokenFromMessage(emailBody);
		} else {
			String smsBody = fetchTwillioLatestMessageBody("+91" + communicationChannel, cutoffUtc, 12);
			token = getTokenFromMessage(smsBody);
		}

		Assert.assertNotNull(token, "Auth token/OTP not extracted");
		Assert.assertFalse(token.isEmpty(), "Auth token/OTP not extracted");
		utils.logit("Auth token/OTP extracted: " + token);

		// STEP 3: Verify OTP - Verify API
		Response responseVerify = null;
		if (communicationChannelType.equals("email"))
			responseVerify = pageObj.endpoints().advancedAuthVerify(client, communicationChannel, null, null, token,
					codeVerifier, clientType);
		else
			responseVerify = pageObj.endpoints().advancedAuthVerify(client, null, "+91", communicationChannel, token,
					codeVerifier, clientType);
		Assert.assertEquals(responseVerify.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		validateGIS_SignUp_SignIn_Response(responseVerify);
		utils.logPass("Advance auth verify API successful");

		String accessToken = responseVerify.jsonPath().getString("data.access_token");
		String refreshToken = responseVerify.jsonPath().getString("data.refresh_token");
		return new String[] { accessToken, refreshToken };
	}

	// Validate refresh token and sign out for advance auth
	public void validateRefreshAndSignOutAdvanceAuth(String client, String secret, String communicationChannel,
			String refreshToken) throws Exception {
		utils.logit("AdvanceAuth token validation and cleanup for communication channel: " + communicationChannel);

		// STEP 1: Refresh token - Refresh API
		Response responseRefresh = null;
		responseRefresh = pageObj.endpoints().advancedAuthRefresh(client, refreshToken);
		Assert.assertEquals(responseRefresh.getStatusCode(), 200);
		validateGIS_SignUp_SignIn_Response(responseRefresh);
		utils.logPass("Advance auth refresh API successful");

		// Get access token, id token after refresh and validate in Punchh APIs
		String accessToken = responseRefresh.jsonPath().getString("data.access_token");
		utils.logit("Access token is present in Advanced Auth Refresh response");
		verifyGISAccessTokenWithPunchhAPIs(client, secret, accessToken);

		// STEP 2: Sign out - SignOut API
		Response responseSignOut = pageObj.endpoints().advancedAuthSignOut(client,
				responseRefresh.jsonPath().getString("data.refresh_token"), accessToken);
		Assert.assertEquals(responseSignOut.getStatusCode(), 200);
		utils.logPass("Advance auth signout API successful");

		// Token blacklist validation
		Map<String, Response> apiResponses = Map.of("api2/mobile/balance_timelines",
				pageObj.endpoints().Api2FetchUserBalance(client, secret, accessToken), "api2/mobile/offers",
				pageObj.endpoints().getUserOffers(accessToken, client, secret), "api/auth/checkins/balance",
				pageObj.endpoints().authApiAccountHistory(accessToken, client, secret),
				"api/mobile/users/notifications",
				pageObj.endpoints().api1FetchNotifications(client, secret, accessToken));

		for (Map.Entry<String, Response> entry : apiResponses.entrySet()) {
			verifyErrorResponse(entry.getValue(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "errors.blacklisted_token",
					"Authentication failed as token sent is blacklisted.", entry.getKey());
		}
		utils.logPass("Token blacklist validation passed for all Punchh APIs");
		utils.logit("Pass",
				"Advanced Auth flow validated successfully for user signup / signin with communication channel: "
						+ communicationChannel);
	}

	// Punchh user signin
	public String userSignInPunchh(String namespace, String client, String secret, String email, String password)
			throws Exception {
		utils.logit("Punchh user signin with namespace: " + namespace + " and email: " + email);

		Response response = null;
		String bearer = null;
		switch (namespace) {
		case "api2":
			response = pageObj.endpoints().Api2Login(email, client, secret);
			utils.logit("API2 login response status: " + response.getStatusCode());
			utils.logit("API2 login response body: " + response.asString());

			bearer = response.jsonPath().getString("access_token.token");
			if (bearer == null) {
				utils.logit("Fail", "API2 login response does not contain access_token.token");
				Assert.fail("API2 login response does not contain access_token.token");
			}
			break;
		case "api1":
			response = pageObj.endpoints().Api1UserLogin(email, client, secret);
			utils.logit("API1 login response status: " + response.getStatusCode());
			utils.logit("API1 login response body: " + response.asString());

			bearer = response.jsonPath().get("auth_token.token");
			if (bearer == null) {
				utils.logit("Fail", "API1 login response does not contain auth_token.token");
				Assert.fail("API1 login response does not contain auth_token.token");
			}
			break;
		case "auth":
			response = pageObj.endpoints().authApiUserLogin(email, client, secret);
			utils.logit("Auth API login response status: " + response.getStatusCode());
			utils.logit("Auth API login response body: " + response.asString());

			bearer = response.jsonPath().get("access_token");
			if (bearer == null) {
				utils.logit("Fail", "Auth API login response does not contain authentication_token");
				Assert.fail("Auth API login response does not contain authentication_token");
			}
			break;
		default:
			utils.logit("Fail", "Unknown Punchh signin namespace: " + namespace);
			Assert.fail("Unknown Punchh signin namespace: " + namespace);
		}
		Assert.assertNotNull(bearer, "Bearer token is not present in login response");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_OK || response.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Punchh signin failed with status " + response.getStatusCode());
		utils.logit("Punchh signin successful via " + namespace + " for email: " + email);
		return bearer;
	}

	// Implemented method for Punchh user signup across namespaces
	public Response userSignUpPunchh(String namespace, String userType, String client, String secret, String email,
			String phone, String locationKey) throws Exception {
		Response resp = null;

		// Update existing phone user in DB in case of signup
		if (userType.contains("Phone"))
			updateExistingPhoneOnlyUserInDB(env, client, phone);

		// Signup via Punchh APIs
		switch (namespace) {
		case "api2":
			switch (userType) {
			case "EmailOnly":
				resp = pageObj.endpoints().Api2SignUp(email, client, secret);
				break;
			case "EmailPhone":
				resp = pageObj.endpoints().Api2SignUp(email, client, secret, Long.parseLong(phone));
				break;
			default:
				utils.logit("Fail", "Unknown Punchh signup for api2 with user type: " + userType);
				Assert.fail("Unknown Punchh signup for api2 with user type: " + userType);
				break;
			}
			break;
		case "api1":
			switch (userType) {
			case "EmailOnly":
				resp = pageObj.endpoints().Api1UserSignUp(email, client, secret);
				break;
			case "EmailPhone":
				resp = pageObj.endpoints().Api1UserSignUp(email, client, secret, phone);
				break;
			default:
				utils.logit("Fail", "Unknown Punchh signup for api1 with user type: " + userType);
				Assert.fail("Unknown Punchh signup for api1 with user type: " + userType);
				break;
			}
			break;
		case "auth":
			switch (userType) {
			case "EmailOnly":
				resp = pageObj.endpoints().authApiSignUp(email, client, secret);
				break;
			case "EmailPhone":
				resp = pageObj.endpoints().authApiSignUp(email, client, secret, phone);
				break;
			default:
				utils.logit("Fail", "Unknown Punchh signup for auth with user type: " + userType);
				Assert.fail("Unknown Punchh signup for auth with user type: " + userType);
				break;
			}
			break;
		case "pos":
			switch (userType) {
			case "EmailOnly":
				resp = pageObj.endpoints().posSignUp(email, locationKey);
				break;
			case "PhoneOnly":
				resp = pageObj.endpoints().posSignUpWithoutEmail(Long.parseLong(phone), locationKey);
				break;
			case "EmailPhone":
				resp = pageObj.endpoints().posSignUpWithPhone(email, locationKey, phone);
				break;
			default:
				utils.logit("Fail", "Unknown Punchh signup for POS with user type: " + userType);
				Assert.fail("Unknown Punchh signup for POS with user type: " + userType);
				break;
			}
			break;
		default:
			utils.logit("Fail", "Unknown Punchh signup namespace: " + namespace);
			Assert.fail("Unknown Punchh signup namespace: " + namespace);
			break;
		}

		Assert.assertTrue(resp.getStatusCode() == ApiConstants.HTTP_STATUS_OK || resp.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Punchh signup failed with status " + resp.getStatusCode());
		utils.logit("Punchh signup successful via " + namespace + " for user type: " + userType);
		return resp;
	}

	// Punchh user update with different possible ways of updating email and phone
	// and different namespaces
	public void legacyUserUpdateWithAdvanceAuthTokens(String client, String secret, String userType,
			String updateNamespace, String updateType, String accessToken) throws Exception {
		utils.logit("Punchh user update with namespace: " + updateNamespace + ", user type: " + userType
				+ " with update type: " + updateType);

		// Initialize email and phone based on update type
		String newEmail = null;
		String newPhone = null;
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();

		switch (updateType.toLowerCase()) {
		case "email":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			break;
		case "phone":
			newPhone = utils.phonenumber();
			break;
		case "emailphone":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			newPhone = utils.phonenumber();
			break;
		default:
			utils.logit("Fail", "Update type must be one of: email, phone, emailphone");
			Assert.fail("Update type must be one of: email, phone, emailphone");
			return;
		}

		Response response = null;
		switch (updateNamespace) {
		case "api1":
			response = pageObj.endpoints().api1UpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName);
			break;
		case "api2":
			response = pageObj.endpoints().Api2UpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName);
			break;
		case "auth":
			response = pageObj.endpoints().authApiUpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName);
			break;
		default:
			utils.logit("Fail", "Update namespace must be one of api1, api2, auth");
			Assert.fail("Update namespace must be one of api1, api2, auth");
		}
		Assert.assertNotNull(response, "Update response is not correct");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_OK || response.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Punchh update failed with status " + response.getStatusCode());
		utils.logit("Punchh user update successful via " + updateNamespace + " for user type: " + userType
				+ " with update type: " + updateType);
	}

	// Punchh user update with different possible ways of updating email and phone
	// and different namespaces
	public void legacyUserUpdateWithBearerToken(String client, String secret, String userType, String updateNamespace,
			String updateType, String bearerToken) throws Exception {
		utils.logit("Punchh user update with namespace: " + updateNamespace + ", user type: " + userType
				+ " with update type: " + updateType);

		// Initialize email and phone based on update type
		String newEmail = null;
		String newPhone = null;
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();

		switch (updateType.toLowerCase()) {
		case "email":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			break;
		case "phone":
			newPhone = utils.phonenumber();
			break;
		case "emailphone":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			newPhone = utils.phonenumber();
			break;
		default:
			utils.logit("Fail", "Update type must be one of: email, phone, emailphone");
			Assert.fail("Update type must be one of: email, phone, emailphone");
			return;
		}

		Response response = null;
		switch (updateNamespace) {
		case "api1":
			response = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, bearerToken, newEmail, newPhone,
					firstName, lastName);
			break;
		case "api2":
			response = pageObj.endpoints().Api2UpdateUserEmailPhone(client, secret, bearerToken, newEmail, newPhone,
					firstName, lastName);
			break;
		case "auth":
			response = pageObj.endpoints().authApiUpdateUserInfoEmailPhone(client, secret, bearerToken, newEmail,
					newPhone, firstName, lastName);
			break;
		default:
			utils.logit("Fail", "Update namespace must be one of api1, api2, auth");
			Assert.fail("Update namespace must be one of api1, api2, auth");
		}
		Assert.assertNotNull(response, "Update response is not correct");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_OK || response.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Punchh update failed with status " + response.getStatusCode());
		utils.logit("Punchh user update successful via " + updateNamespace + " for user type: " + userType
				+ " with update type: " + updateType);
	}

	// Validate guest identity db records for advance auth
	public List<String> validateGuestIdentityDbRecords(String communicationChannel, String signupUserType,
			String signupClientType, String slug, String punchhBusinessId) throws Exception {

		String expectedSignupChannel = "WebEmailAdvanceAuth"; // Default
		if ((signupClientType.toLowerCase().equals("internal_mobile_app"))
				|| (signupClientType.toLowerCase().equals("external_mobile_app"))) {
			expectedSignupChannel = signupUserType.contains("Email") ? "AppEmailAdvanceAuth" : "AppPhoneAdvanceAuth";
		} else if ((signupClientType.toLowerCase().equals("online_ordering"))
				|| (signupClientType.toLowerCase().equals("iframe"))) {
			expectedSignupChannel = signupUserType.contains("Email") ? "WebEmailAdvanceAuth" : "WebPhoneAdvanceAuth";
		}
		utils.logit("Expected signup channel: " + expectedSignupChannel);

		String expectedCommunicationChannel;
		if ("PhoneOnly".equalsIgnoreCase(signupUserType)) {
			expectedCommunicationChannel = "phone_number";
		} else if ("EmailPhone".equalsIgnoreCase(signupUserType)) {
			expectedCommunicationChannel = "phone_number";

		} else {
			expectedCommunicationChannel = "email";
		}
		utils.logit("Expected communication channel type: " + expectedCommunicationChannel);

		String ownerId = "SELECT bm.id FROM business_mappings bm "
				+ "JOIN business_unit_stacks bus ON bm.business_unit_stack_id = bus.id "
				+ "JOIN business_units bu ON bus.business_unit_id = bu.id " + "WHERE bm.slug = '" + slug + "' "
				+ "AND bu.name = 'par_loyalty';";

		String businessMappingId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, ownerId, "id");
		Assert.assertNotNull(businessMappingId, "Business mapping id is null");
		utils.logit("Business mapping id is: " + businessMappingId);

		String punchhUserID;
		String identityUserID;
		String stream = "";
		if (punchhBusinessId.contains("_downstream")) {
			punchhBusinessId = punchhBusinessId.split("_downstream")[0];
			stream = "downstream";
		}

		if ("PhoneOnly".equalsIgnoreCase(signupUserType) || "EmailPhone".equalsIgnoreCase(signupUserType)) {
			// fetch userID from users table where email = communicationChannel and
			// business_id = businessMappingId
			String userIdQuery = "select id from users where phone = '${communicationChannel}' and business_id = '${punchhBusinessId}'";
			userIdQuery = userIdQuery.replace("${communicationChannel}", communicationChannel)
					.replace("${punchhBusinessId}", punchhBusinessId);
			punchhUserID = DBUtils.executeQueryAndGetColumnValue(env, userIdQuery, "id");
			utils.logit("UserID: " + punchhUserID);
			// get user id from identity users table where email = communicationChannel
			String identityUserIdQuery = "select id from users where phone_number = '${communicationChannel}' and business_id = '${businessMappingId}'";
			identityUserIdQuery = identityUserIdQuery.replace("${communicationChannel}", communicationChannel)
					.replace("${businessMappingId}", businessMappingId);
			identityUserID = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, identityUserIdQuery, "id");
			utils.logit("Identity UserID: " + identityUserID);
		} else {
			String identityUserIdQuery = "select id from users where email = '${communicationChannel}' and business_id = '${businessMappingId}'";
			identityUserIdQuery = identityUserIdQuery.replace("${communicationChannel}", communicationChannel)
					.replace("${businessMappingId}", businessMappingId);
			identityUserID = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, identityUserIdQuery, "id");
			utils.logit("Identity UserID: " + identityUserID);
			// fetch userID from users table where email = communicationChannel
			String userIdQuery = "select id from users where email = '${communicationChannel}' and business_id = '${punchhBusinessId}'";
			userIdQuery = userIdQuery.replace("${communicationChannel}", communicationChannel)
					.replace("${punchhBusinessId}", punchhBusinessId);
			punchhUserID = DBUtils.executeQueryAndGetColumnValue(env, userIdQuery, "id");
			utils.logit("UserID: " + punchhUserID);
		}
		if (stream.equalsIgnoreCase("downstream")) {
			// fetch the signup_channel from users table in punchh db using punchhUserID
			String getSignupChannelQuery = "select signup_channel from users where id='${punchhUserID}'";
			getSignupChannelQuery = getSignupChannelQuery.replace("${punchhUserID}", punchhUserID);
			expectedSignupChannel = DBUtils.executeQueryAndGetColumnValue(env, getSignupChannelQuery, "signup_channel");
		}

		// Validate user sync with guest identity
		validateUserSyncWithGIS(punchhUserID);

		// auth_requests validation (guest_identity) and business_id validation
		String getAuthRequestDetailsQuery = "SELECT validation_method, communication_channel_type, status, business_mapping_id FROM auth_requests WHERE user_id='${identityUserID}' ORDER BY created_at DESC LIMIT 1";
		getAuthRequestDetailsQuery = getAuthRequestDetailsQuery.replace("${identityUserID}", identityUserID);
		String[] authRequestColumns = { "validation_method", "communication_channel_type", "status",
				"business_mapping_id" };
		List<Map<String, String>> authRequestDetails = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost,
				getAuthRequestDetailsQuery, authRequestColumns);

		Assert.assertEquals(authRequestDetails.get(0).get("validation_method"), "otp", "Validation method is not otp");
		String expectedCommType = "PhoneOnly".equalsIgnoreCase(signupUserType)
				|| "EmailPhone".equalsIgnoreCase(signupUserType) ? "phone_number" : "email";
		Assert.assertEquals(authRequestDetails.get(0).get("communication_channel_type"), expectedCommType,
				"Communication channel type mismatch");
		Assert.assertEquals(authRequestDetails.get(0).get("status"), "3", "Status is not 3");
		Assert.assertEquals(authRequestDetails.get(0).get("business_mapping_id"), businessMappingId,
				"user_id mismatch in auth_requests");
		utils.logit("pass", "auth_requests verified for " + signupUserType + " user");

		// Define the query with placeholders
		String auditLogQuery = "SELECT event_type, event_status, communication_channel_type, user_id "
				+ "FROM audit_logs WHERE user_id = '${identityUserID}' "
				+ "and event_type = 'otp_verification' ORDER BY created_at DESC LIMIT 1";

		// Replace placeholder with actual value
		auditLogQuery = auditLogQuery.replace("${identityUserID}", identityUserID);

		// Define column names
		String[] auditLogColumnNames = { "event_type", "event_status", "communication_channel_type", "user_id" };

		// Execute the query and fetch values as a map
		List<Map<String, String>> auditLogDetailsList = DBUtils.executeQueryAndGetMultipleColumns(env,
				guestIdentityhost, auditLogQuery, auditLogColumnNames);

		// Get the first (and only) result
		Map<String, String> auditLogDetails = auditLogDetailsList.get(0);

		// Assertions
		Assert.assertEquals(auditLogDetails.get("event_type"), "otp_verification",
				"event_type is not otp_verification in audit_logs table of guest_identity db");

		Assert.assertEquals(auditLogDetails.get("event_status"), "success",
				"event_status is not success in audit_logs table of guest_identity db");

		Assert.assertEquals(auditLogDetails.get("communication_channel_type"), expectedCommunicationChannel,
				"communication_channel_type is not email in audit_logs table of guest_identity db");

		Assert.assertNotNull(auditLogDetails.get("user_id"),
				"user_id is null in audit_logs table of guest_identity db");

		utils.logit("pass", "All audit log validations passed for guest_identity db");

		// user_details validations (guest_identity)
		String getUserDetailsQuery = "SELECT source_business_unit_user_id, privacy_policy, terms_and_conditions FROM user_details WHERE user_id = '${identityUserID}'";
		getUserDetailsQuery = getUserDetailsQuery.replace("${identityUserID}", identityUserID);
		String[] userDetailsColumns = { "source_business_unit_user_id", "privacy_policy", "terms_and_conditions" };
		List<Map<String, String>> userDetailsResults = DBUtils.executeQueryAndGetMultipleColumns(env, guestIdentityhost,
				getUserDetailsQuery, userDetailsColumns);
		Map<String, String> userDetails = userDetailsResults.get(0);

		Assert.assertEquals(userDetails.get("source_business_unit_user_id"), punchhUserID,
				"source_business_unit_user_id mismatch");
		Assert.assertEquals(userDetails.get("privacy_policy"), "1", "privacy_policy is not true");
		Assert.assertEquals(userDetails.get("terms_and_conditions"), "1", "terms_and_conditions is not true");
		utils.logit("pass", "user_details validations passed");

		// signup_channel and identity_uuid in punchh users table
		String getUsersDetailsQuery = "select signup_channel, identity_uuid from users where id='${punchhUserID}'";

		getUsersDetailsQuery = getUsersDetailsQuery.replace("${punchhUserID}", punchhUserID);
		String[] usersDetailsColumns = { "signup_channel", "identity_uuid" };
		List<Map<String, String>> usersDetails = DBUtils.executeQueryAndGetMultipleColumns(env, getUsersDetailsQuery,
				usersDetailsColumns);
		Assert.assertEquals(usersDetails.get(0).get("signup_channel"), expectedSignupChannel,
				"signup_channel mismatch in punchh users table");
		utils.logit("pass", "signup_channel and identity_uuid validations passed in punchh users table");
		// add the punchh user id and guest identity user id in list
		List<String> createdUserIds = new ArrayList<>();

		// Replace the comment `//add the punchh user id and guest identity user id in
		// list`
		// with this code at the end of `validateGuestIdentityDbRecords`:
		createdUserIds.add(punchhUserID);
		createdUserIds.add(identityUserID);
		utils.logit("pass", "Collected user IDs: " + createdUserIds);
		return createdUserIds;
	}

	public void validateMetaDataInGuestIdentityDb(String userId) throws Exception {
		SoftAssert softAssert = new SoftAssert();

		String query = "SELECT * FROM user_details WHERE user_id = '" + userId + "';";
		List<Map<String, String>> userDetailsResults = DBUtils.executeQueryAndGetAllRows(env, guestIdentityhost, query);
		Map<String, String> data = userDetailsResults.get(0);

		// Validate last_sign_in_ip, last_sign_in_at, last_user_agent in user_details
		// table
		softAssert.assertNotNull(data.get("last_sign_in_ip"),
				"last_sign_in_ip is null in user_details table for user_id: " + userId);
		softAssert.assertNotNull(data.get("last_sign_in_at"),
				"last_sign_in_at is null in user_details table for user_id: " + userId);
		softAssert.assertNotNull(data.get("last_user_agent"),
				"last_user_agent is null in user_details table for user_id: " + userId);
		softAssert.assertAll();

		utils.logit("Pass",
				"Metadata last_sign_in_ip, last_sign_in_at, last_user_agent mapping is correct in user_details table for user_id: "
						+ userId);
	}

	public String userSignUpAdvanceAuthWithUserAgent(String client, String communicationChannel, String clientType,
			String userAgentToken, String userAgentVerify) throws Exception {
		utils.logit("AdvanceAuth OTP request and verification with client type: " + clientType
				+ " and communication channel: " + communicationChannel);

		String codeVerifier = utils.generateCodeVerifier(32);
		String codeChallenge = utils.generateCodeChallenge(codeVerifier);
		boolean privacyPolicy = true;
		boolean tAndc = true;

		// Record current time in UTC (use millisecond precision for parameterized tests)
		ZonedDateTime cutoffUtc = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

		String communicationChannelType = communicationChannel.toLowerCase().contains(".com") ? "email" : "sms";

		// STEP 1: Generate OTP - Token API
		Response responseToken = null;
		if (communicationChannelType.equals("email")) {
			responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", communicationChannel, null, null,
					codeChallenge, privacyPolicy, tAndc, userAgentToken);
		} else {
			// Update existing phone user in DB in case of signup
			updateExistingPhoneOnlyUserInDB(env, client, communicationChannel);
			responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", null, "+91", communicationChannel,
					codeChallenge, privacyPolicy, tAndc, userAgentToken);
		}

		Assert.assertEquals(responseToken.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(responseToken.jsonPath().getList("data.message").contains("OTP sent successfully."));
		utils.logit("Advance auth token API successful, OTP sent successfully on " + communicationChannel);

		// STEP 2: Extract OTP from Email/SMS
		String token;
		if (communicationChannelType.equals("email")) {
			String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail",
					communicationChannel + "," + auth0FromEmail, true);
			token = getTokenFromMessage(emailBody);
		} else {
			String smsBody = fetchTwillioLatestMessageBody("+91" + communicationChannel, cutoffUtc, 12);
			token = getTokenFromMessage(smsBody);
		}

		Assert.assertNotNull(token, "Auth token/OTP not extracted");
		Assert.assertFalse(token.isEmpty(), "Auth token/OTP not extracted");
		utils.logit("Auth token/OTP extracted: " + token);

		// STEP 3: Verify OTP - Verify API
		Response responseVerify = null;
		if (communicationChannelType.equals("email"))
			responseVerify = pageObj.endpoints().advancedAuthVerify(client, communicationChannel, null, null, token,
					codeVerifier, clientType, null, userAgentVerify);
		else
			responseVerify = pageObj.endpoints().advancedAuthVerify(client, null, "+91", communicationChannel, token,
					codeVerifier, clientType, null, userAgentVerify);
		Assert.assertEquals(responseVerify.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("Advance auth verify API successful");

		String query = "SELECT id FROM users WHERE email = '" + communicationChannel + "' or phone_number = '"
				+ communicationChannel + "' order by created_at desc limit 1";
		String identityUserId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		Assert.assertNotNull(identityUserId, "Identity user id is null");
		utils.logit("Identity user id: " + identityUserId);
		return identityUserId;
	}

	public void legacyUserUpdateWithAdvanceAuthToken(String client, String secret, String userType,
			String updateNamespace, String updateType, String accessToken, String email, String currentPassword,
			String newPassword) throws Exception {
		utils.logit("Punchh user update with namespace: " + updateNamespace + ", user type: " + userType
				+ " with update type: " + updateType);

		// Initialize email and phone based on update type
		String newEmail = null;
		String newPhone = null;
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();

		switch (updateType.toLowerCase()) {
		case "email":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			break;
		case "phone":
			newPhone = utils.phonenumber();
			break;
		case "emailphone":
			newEmail = "advanceAuth_updatedEmail_" + utils.getTimestampInNanoseconds() + "@partech.com";
			newPhone = utils.phonenumber();
			break;
		default:
			utils.logit("Fail", "Update type must be one of: email, phone, emailphone");
			Assert.fail("Update type must be one of: email, phone, emailphone");
			return;
		}
		newEmail = email;
		Response response = null;
		switch (updateNamespace) {
		case "pos_async_users":
			response = pageObj.endpoints().Api2SecureAsynchronousUserUpdate(client, email, secret, accessToken,
					currentPassword, newPassword);
			break;
		case "async_users":
			response = pageObj.endpoints().Api2AsynchronousUserUpdate(client, email, secret, accessToken,
					currentPassword, newPassword);
			break;
		case "api1":
			response = pageObj.endpoints().api1UpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName);
			break;
		case "api2":
			response = pageObj.endpoints().Api2UpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName, currentPassword, newPassword);
			break;
		case "auth":
			response = pageObj.endpoints().authApiUpdateUser(client, secret, accessToken, newEmail, newPhone, firstName,
					lastName, currentPassword, newPassword);
			break;
		default:
			utils.logit("Fail", "Update namespace must be one of api1, api2, auth");
			Assert.fail("Update namespace must be one of api1, api2, auth");
		}
		Assert.assertNotNull(response, "Update response is not correct");
		Assert.assertTrue(
				response.getStatusCode() == ApiConstants.HTTP_STATUS_OK || response.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED || response.getStatusCode() == 202,
				"Punchh update failed with status " + response.getStatusCode());
		utils.logit("Punchh user update successful via " + updateNamespace + " for user type: " + userType
				+ " with update type: " + updateType);
	}

	public void basicAuthSignUpSignInDBValidations(String email, String userActivity) throws Exception {

		SoftAssert softAssert = new SoftAssert();

		// Punchh user id
		String query = "SELECT * FROM users WHERE email = '" + email + "' ORDER BY created_at DESC LIMIT 1";
		Map<String, String> userData = DBUtils.executeQueryAndGetAllRows(env, query).get(0);
		String punchhUserId = userData.get("id");

		// Punchh business uuid
		query = "SELECT external_identity_uuid FROM businesses WHERE id = '" + userData.get("business_id") + "'";
		String punchhBusinessUuid = DBUtils.executeQueryAndGetColumnValue(env, query, "external_identity_uuid");
		utils.logit("Punchh business uuid: " + punchhBusinessUuid);

		// Guest identity business id
		query = "SELECT id FROM businesses WHERE uuid = '" + punchhBusinessUuid + "'";
		String guestIdentityBusinessId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		utils.logit("Guest identity business id: " + guestIdentityBusinessId);

		// Validate users table
		String guestIdentityUserId = validateUserSyncWithGIS(punchhUserId);
		utils.logit("Guest identity user id: " + guestIdentityUserId);

		String eventType;
		switch (userActivity) {
		case "sign_in":
			eventType = "user_signin_basic_auth";
			break;
		case "sign_up":
			eventType = "user_signup_basic_auth";
			break;
		default:
			softAssert.fail("Invalid user activity: " + userActivity + ". Expected 'sign_in' or 'sign_up'");
			eventType = null;
		}

		// Validate audit_logs table
		query = "SELECT * FROM audit_logs WHERE user_id = '" + guestIdentityUserId + "' and event_type = '" + eventType
				+ "' ORDER BY created_at DESC LIMIT 1";
		Map<String, String> auditLogData = DBUtils.executeQueryAndGetAllRows(env, guestIdentityhost, query).get(0);
		softAssert.assertEquals(auditLogData.get("event_status"), "success",
				"event_status is not success in audit_logs table");
		softAssert.assertEquals(auditLogData.get("communication_channel"), email,
				"communication_channel is not " + email + " in audit_logs table");
		softAssert.assertEquals(auditLogData.get("communication_channel_type"), "email",
				"communication_channel_type is not email in audit_logs table");
		softAssert.assertEquals(auditLogData.get("business_id"), guestIdentityBusinessId,
				"business_id is not " + guestIdentityBusinessId + " in audit_logs table");

		// Validate user_details table
		query = "SELECT * FROM user_details WHERE user_id = '" + guestIdentityUserId + "';";
		Map<String, String> userDetailsData = DBUtils.executeQueryAndGetAllRows(env, guestIdentityhost, query).get(0);
		softAssert.assertNotNull(userDetailsData.get("last_sign_in_ip"),
				"last_sign_in_ip is null in user_details table");
		softAssert.assertNotNull(userDetailsData.get("last_sign_in_at"),
				"last_sign_in_at is null in user_details table");
		softAssert.assertNotNull(userDetailsData.get("last_user_agent"),
				"last_user_agent is null in user_details table");
		if (email.contains("EmailOnlyUpsert")) {
			softAssert.assertEquals(userDetailsData.get("privacy_policy"), "0",
					"privacy_policy is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("terms_and_conditions"), "0",
					"terms_and_conditions is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("signup_channel"), "WebEmail",
					"signup_channel is not WebEmailBasicAuth in user_details table");
		} else if (email.contains("EmailPhoneUpsert")) {
			softAssert.assertEquals(userDetailsData.get("privacy_policy"), "0",
					"privacy_policy is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("terms_and_conditions"), "0",
					"terms_and_conditions is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("signup_channel"), "WebEmail",
					"signup_channel is not WebEmailBasicAuth in user_details table");
		} else {
			softAssert.assertEquals(userDetailsData.get("privacy_policy"), "1",
					"privacy_policy is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("terms_and_conditions"), "1",
					"terms_and_conditions is not true in user_details table");
			softAssert.assertEquals(userDetailsData.get("signup_channel"), "WebEmailBasicAuth",
					"signup_channel is not WebEmailBasicAuth in user_details table");
		}
		softAssert.assertEquals(userDetailsData.get("source_business_unit"), "par_loyalty",
				"source_business_unit is not par_loyalty in user_details table");

		// Validate that an entry is created in the refresh_tokens table.
		query = "SELECT * FROM refresh_tokens WHERE user_id = '" + guestIdentityUserId
				+ "' ORDER BY created_at DESC LIMIT 1";
		softAssert.assertNotNull(DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id"),
				"Entry is not created in the refresh_token table");

		softAssert.assertAll();
		utils.logit("Pass", "User details validations passed for user_id: " + guestIdentityUserId);
	}

	// Enable advance auth/ basic auth UI configuration
	public void updateAdvanceAndBasicAuthConfig(boolean enableAdvanceAuth, boolean enableBasicAuth) throws Exception {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");
		pageObj.dashboardpage().navigateToTabs("Advanced Authentication");
		boolean advanceUpdated = utils.setCheckboxStateViaCheckBoxText("Enable Advanced Authentication",
				enableAdvanceAuth);
		if (advanceUpdated) {
			utils.getLocator("GISConfigurationPage.aa_updateButton").click();
			utils.longWaitInSeconds(2);
			Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.guestIdentityConfigUpdateSuccessMsg);
			utils.logit("Advanced Authentication configuration updated successfully");
		}

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");
		pageObj.dashboardpage().navigateToTabs("Basic Authentication");
		boolean basicUpdated = utils.setCheckboxStateViaCheckBoxText("Enable Basic Authentication", enableBasicAuth);
		if (basicUpdated) {
			utils.getLocator("GISConfigurationPage.ba_updateButton").click();
			utils.longWaitInSeconds(2);
			Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.guestIdentityConfigUpdateSuccessMsg);
			utils.logit("Basic Authentication configuration updated successfully");
		}
		utils.logPass("Advance and Basic Auth configuration updated successfully");
	}

	public void updateSocialConfig(boolean enableFacebook, boolean enableGoogle, boolean enableApple) throws Exception {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");
		pageObj.dashboardpage().navigateToTabs("Social Authentication");
		boolean anyUpdated = false;
		anyUpdated |= utils.setCheckboxStateViaCheckBoxText("Enable Facebook Sign-In", enableFacebook);
		anyUpdated |= utils.setCheckboxStateViaCheckBoxText("Enable Google Sign-In", enableGoogle);
		anyUpdated |= utils.setCheckboxStateViaCheckBoxText("Enable Apple Sign-In", enableApple);
		if (anyUpdated) {
			utils.getLocator("GISConfigurationPage.sa_updateButton").click();
			utils.longWaitInSeconds(2);
			Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.guestIdentityConfigUpdateSuccessMsg);
		}
		utils.logPass("Social authentication configuration updated successfully");
	}

	public List<String> extractUrls(String text) {
		if (text == null || text.isEmpty()) {
			return Collections.emptyList();
		}
		Pattern pattern = Pattern.compile("(https?://[^\\s\"'<>]+)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		Set<String> found = new LinkedHashSet<>(); // preserve order, ensure uniqueness

		while (matcher.find()) {
			String url = matcher.group(1);
			// Remove common trailing punctuation that may be attached to the URL in plain
			// text
			url = url.replaceAll("[\\.,;:\\)\\]\\!\\?]+$", "");
			found.add(url);
		}
		return new ArrayList<>(found);
	}

	public void verifyGISUserSyncForActivity(String client, String adminKey, String userEmail, String userActivity)
			throws Exception {

		// Get punchh user id from users table
		String query = "SELECT id FROM users WHERE email = '" + userEmail + "'";
		String punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertTrue(!punchhUserId.isEmpty(), "Punchh user ID is empty");
		utils.logit("Punchh user ID: " + punchhUserId);

		// Validate initial sync with guest identity DB
		validateUserSyncWithGIS(punchhUserId);

		// Deactivate user
		Response deacResponse = pageObj.endpoints().deactivateUser(punchhUserId, adminKey);
		Assert.assertEquals(deacResponse.getStatusCode(), 200,
				"Status code 200 did not matched for deactivate user API");

		// Validate user deactivation sync with guest identity DB
		validateUserSyncWithGIS(punchhUserId);
		utils.logit("Pass", "Verified that user is deactivated in guest identity DB");

		// Perform user activity
		Response response = null;
		switch (userActivity) {
		case "Delete":
			response = pageObj.endpoints().deleteUser(punchhUserId, adminKey);
			break;
		case "Anonymise":
			response = pageObj.endpoints().anonymiseUser(punchhUserId, adminKey);
			break;
		default:
			utils.logit("Fail", "Unknown user activity: " + userActivity);
			Assert.fail("Unknown user activity: " + userActivity);
		}

		Assert.assertEquals(response.getStatusCode(), 200,
				"Status code 200 did not match for " + userActivity + " user API");
		utils.logit("Pass", "User " + userActivity + " API executed successfully");

		// Trigger Sidekiq job
		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		pageObj.sidekiqPage().filterByJob(punchhUserId);
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();
		utils.logit("Job is added in the queue");

		switch (userActivity) {
		case "Delete":
			utils.longWaitInSeconds(10);
			query = "select id from users where email = '" + userEmail + "'";
			String count = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
			Assert.assertTrue(count.equals(""), "User is not deleted from guest identity DB");
			utils.logit("Pass", "Verified that user is deleted in guest identity DB");
			break;
		case "Anonymise":
			validateUserSyncWithGIS(punchhUserId);
			utils.logit("Pass", "Verified that user is anonymised in identity DB");
			break;
		default:
			utils.logit("Fail", "Unknown user activity: " + userActivity);
			break;
		}
		utils.logit("Pass", "Verified that user deletion and anonymisation sync is successful for Basic Auth");
	}

	public void validateUpdatedUserSyncsWithGIS(String client, String secret, String userEmail, String accessToken) throws Exception {

		SoftAssert softAssert = new SoftAssert();
		Response response = null;
		String query, punchhUserId, updateEmail, phoneNumber;

		// Get punchh user id from users table
		query = "SELECT id FROM users WHERE email = '" + userEmail + "'";
		punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		softAssert.assertTrue(punchhUserId != null && !punchhUserId.isEmpty(), "Punchh user ID is empty");
		utils.logit("Punchh user ID: " + punchhUserId);

		// User update api1 - email only
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		response = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, accessToken, updateEmail, null,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api1 update email only - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api1 - Add Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api1 add phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api1 - Update Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api1 update phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api1 - email + phone
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api1 update email + phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api2 - email only
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		response = pageObj.endpoints().Api2UpdateUserEmailPhone(client, secret, accessToken, updateEmail, null,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api2 update email only - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api2 - Add Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().Api2UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api2 add phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api2 - Update Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().Api2UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api2 update phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update api2 - email + phone
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().Api2UpdateUserEmailPhone(client, secret, accessToken, updateEmail, phoneNumber,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Api2 update email + phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update auth - email only
		response = pageObj.endpoints().authApiUpdateUserInfoEmailPhone(client, secret, accessToken, updateEmail, null,
				faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Auth update email only - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update auth - Add Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().authApiUpdateUserInfoEmailPhone(client, secret, accessToken, updateEmail,
				phoneNumber, faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Auth add phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update auth - Update Phone
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().authApiUpdateUserInfoEmailPhone(client, secret, accessToken, updateEmail,
				phoneNumber, faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Auth update phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		// User update auth - email + phone
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		phoneNumber = utils.phonenumber();
		response = pageObj.endpoints().authApiUpdateUserInfoEmailPhone(client, secret, accessToken, updateEmail,
				phoneNumber, faker.name().firstName(), faker.name().lastName());
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Auth update email + phone - status code mismatch");
		validateUserSyncWithGIS(punchhUserId);

		utils.logit("Pass", "User sync flows test and validation with Guest Identity completed successfully for all APIs (api1, api2, auth)");
		softAssert.assertAll();
	}
}