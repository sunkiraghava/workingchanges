package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.javafaker.Faker;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.DynamicPayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class advanceAuthTest {
	static Logger logger = LogManager.getLogger(advanceAuthTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private IntUtils intUtils;
	private String client, secret;
	private String guestIdentityhost = "guestIdentity";
	private Faker faker;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		intUtils = new IntUtils(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		client = dataSet.get("client");
		secret = dataSet.get("secret");
		this.faker = new Faker();
	}

	@DataProvider(name = "iFrameSsoFlow")
	public Object[][] iFrameSsoFlow() {
		return new Object[][] {
				// loginType, signupChannel,
				{ "EmailOnly", "AppEmailAdvanceAuth" }, { "PhoneOnly", "WebPhoneAdvanceAuth" },

		};
	}

	@Test(description = "SQ-T6737 Advance Auth - Iframe - SSO flow end to end for phone only user on Guest Auth DB."
			+ "SQ-T6738 Advance Auth - Iframe - SSO flow end to end for email only user on Guest Auth DB.", dataProvider = "iFrameSsoFlow")
	@Owner(name = "Vansham Mishra")
	public void verifyAdvancedAuthIframeSSOFlowForEmailAndPhoneUsers(String loginType, String signupChannel)
			throws Exception {
		String email;
		String phone;
		if (loginType.equals("EmailOnly")) {
			email = intUtils.getRandomGmailEmail();
			phone = "";
		} else {
			phone = dataSet.get("phoneNumber");
			email = "";
		}
		validateIframeSso(loginType, email, phone, client, secret, dataSet.get("slug"), dataSet.get("business_id"),
				dataSet.get("redirect_uri"));
	}

	public void validateIframeSso(String loginType, String email, String phone, String client, String secret,
			String slug, String punchhBusinessId, String redirect_uri) throws Exception {
		String codeVerifier = utils.generateCodeVerifier(32);
		String codeChallenge = utils.generateCodeChallenge(codeVerifier);
		boolean privacyPolicy = true;
		boolean tAndc = true;
		switch (loginType) {
		case "EmailOnly":
			// STEP 1: Generate OTP Email
			Response responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", email, null, null,
					codeChallenge, privacyPolicy, tAndc);
			Assert.assertEquals(responseToken.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			Assert.assertTrue(responseToken.jsonPath().getList("data.message").contains("OTP sent successfully."));
			utils.logit("Advance auth token API successful, OTP sent successfully on " + email);

			// STEP 2: Extract OTP from Email
			String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail",
					email + "," + utils.getConfigProperty("auth0.fromEmail"), true);
			String token = intUtils.getTokenFromMessage(emailBody);
			Assert.assertNotNull(token, "Auth token/OTP not extracted");
			Assert.assertFalse(token.isEmpty(), "Auth token/OTP not extracted");
			utils.logit("Auth token/OTP extracted: " + token);
			// STEP 3: Verify OTP
			Response responseVerify = pageObj.endpoints().advancedAuthVerify(client, email, null, null, token,
					codeVerifier, "iframe_sso", redirect_uri);
			Assert.assertEquals(responseVerify.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code did not match for advanced auth verify");
			utils.logPass("Advance auth verify API successful");
			String oauth_token = responseVerify.jsonPath().getString("data.oauth_token");
			Assert.assertNotNull(oauth_token, "OAuth token is null");
			Response ssoTokenResponse = pageObj.endpoints().authApiGetSSOToken(oauth_token, client, secret,
					"authorization_code", redirect_uri);
			Assert.assertEquals(ssoTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code did not match for SSO token");
			intUtils.validateGIS_SignUp_SignIn_Response(ssoTokenResponse);
			String access_token = ssoTokenResponse.jsonPath().getString("data.access_token");
			intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, access_token);
			intUtils.validateGuestIdentityDbRecords(email, loginType, "iframe", slug, punchhBusinessId);
			break;
		case "PhoneOnly":
			ZonedDateTime cutoffUtc = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);
			// STEP 1: Generate OTP Email
			intUtils.updateExistingPhoneOnlyUserInDB(env, client, phone);
			Response responseTokenPhone = pageObj.endpoints().advancedAuthToken(client, "otp", null, "+91", phone,
					codeChallenge, privacyPolicy, tAndc);
			Assert.assertEquals(responseTokenPhone.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			Assert.assertTrue(responseTokenPhone.jsonPath().getList("data.message").contains("OTP sent successfully."));
			utils.logit("Advance auth token API successful, OTP sent successfully on " + phone);

			// STEP 2: Extract OTP from sms
			String smsBody = intUtils.fetchTwillioLatestMessageBody("+91" + phone, cutoffUtc, 12);
			token = intUtils.getTokenFromMessage(smsBody);
			Assert.assertNotNull(token, "Auth token/OTP not extracted");
			Assert.assertFalse(token.isEmpty(), "Auth token/OTP not extracted");
			utils.logit("Auth token/OTP extracted: " + token);

			// STEP 3: Verify OTP
			responseVerify = pageObj.endpoints().advancedAuthVerify(client, null, "+91", phone, token, codeVerifier,
					"iframe_sso", redirect_uri);
			Assert.assertEquals(responseVerify.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code did not match for advanced auth verify");
			utils.logPass("Advance auth verify API successful");
			oauth_token = responseVerify.jsonPath().getString("data.oauth_token");
			Assert.assertNotNull(oauth_token, "OAuth token is null");
			ssoTokenResponse = pageObj.endpoints().authApiGetSSOToken(oauth_token, client, secret, "authorization_code",
					redirect_uri);
			Assert.assertEquals(ssoTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code did not match for SSO token");
			intUtils.validateGIS_SignUp_SignIn_Response(ssoTokenResponse);
			access_token = ssoTokenResponse.jsonPath().getString("data.access_token");
			intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, access_token);
			intUtils.validateGuestIdentityDbRecords(phone, loginType, "iframe", slug, punchhBusinessId);

			break;
		default:
			utils.logit("fail", "Invalid login type for iframe SSO: " + loginType);
			Assert.fail("Invalid login type for iframe SSO: " + loginType);
		}
	}

	@DataProvider(name = "Punchh_Signup_AA_SignIn_Update")
	public Object[][] Punchh_Signup_AA_SignIn_Update() {
		return new Object[][] {
				// SignupNamespace, SignupUserType, SigninClientType, UpdateNamespace,
				// UpdateType
				{ "pos", "PhoneOnly", "internal_mobile_app", "api2", "Phone" }, };
	}

	@Test(description = "SQ-T6769 Handle usecases for POS user merge ( Merging while updating the phone number) on Guest Auth DB", dataProvider = "Punchh_Signup_AA_SignIn_Update")
	@Owner(name = "Vansham Mishra")
	public void T6769_mergePosUserWithPhoneUpdate(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {

		String email = intUtils.getRandomGmailEmail();
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		// Punchh SignUp
		intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, email, phone, locationKey);

		// AdvanceAuth SignIn
		switch (signupUserType) {
		case "EmailOnly":
			intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			break;
		case "PhoneOnly":
			intUtils.userSignUpSignInAdvanceAuth(client, phone, signinClientType, "SignIn");
			break;
		case "EmailPhone":
			intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			intUtils.userSignUpSignInAdvanceAuth(client, phone, signinClientType, "SignIn");
			break;
		}

		// take the legacy user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		String[] userIds = getUserIds(phone);
		utils.logit("guest user id is :- " + userIds[0]);
		utils.logit("punchh user id is :- " + userIds[1]);
		String npwd = "String@123";

		// Update phone number to merge with advance auth user
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail(npwd,
				dataSet.get("phoneNumber"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");

		// verify in DB that phone number is updated and merged with advance auth user
		validateGuestMergeInDB(userEmail, dataSet.get("phoneNumber"), userIds);
	}

	@DataProvider(name = "punchhAccessTokenValidation")
	public Object[][] AA_Signup_AA_SignIn_Update() {
		return new Object[][] {
				// SignupClientType, SignupUserType
				{ "internal_mobile_app", "EmailOnly" }, { "external_mobile_app", "PhoneOnly" },
				{ "online_ordering", "EmailOnly" }, { "iframe", "PhoneOnly" }, };
	}

	@Test(description = "SQ-T6771 INT2-2370| Open up access_token field for api/mobile namespace", dataProvider = "punchhAccessTokenValidation")
	@Owner(name = "Vansham Mishra")
	public void T6771verifyAccessTokenFieldForMobileApi(String signupClientType, String signupUserType)
			throws Exception {
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// AdvanceAuth Signup
		String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signupClientType,
				"SignUp");

		// api/auth/users API
		Response userInfoResponse = pageObj.endpoints().authApiGetUserInfoWithHeaderAuth(client, secret,
				tokensSignUp[0]);
		String accessToken = userInfoResponse.jsonPath().getString("access_token");

		// Verify that 'access_token' field is present in response
		Assert.assertEquals(userInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not match for user info API");
		Assert.assertNotNull(userInfoResponse.jsonPath().getString("access_token"),
				"'access_token' field is null in user info API response");
		utils.logPass("'access_token' field is present in user info API response");

		// Validate api/mobile/users with api/auth/users access_token
		Response customerInfo = pageObj.endpoints().api1UserShow(client, secret, accessToken);
		Assert.assertEquals(customerInfo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not match for user info API");
		Assert.assertNotNull(customerInfo.jsonPath().getString("access_token"),
				"'access_token' field is null in customer info API response");
		utils.logPass("'access_token' field is present in customer info API response");
	}

	@DataProvider(name = "UserAgentForAdvanceAuthSignup")
	public Object[][] UserAgentForAdvanceAuthSignup() {
		return new Object[][] {
				// SignupClientType, SignupUserType
				{ "internal_mobile_app", "EmailOnly" }, { "external_mobile_app", "PhoneOnly" },
				{ "online_ordering", "EmailOnly" }, };
	}

	@Test(description = "SQ-T6891 INT2-2265 | Validate last_user_agent with different values.", dataProvider = "UserAgentForAdvanceAuthSignup")
	@Owner(name = "Vansham Mishra")
	public void T6891_ValidateUserAgentForAdvanceAuthSignup(String signupClientType, String signupUserType)
			throws Exception {
		String communicationChannel;
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// Generate random string of 300 characters for user agent
		String userAgentToken = utils.generateRandomString(300);
		String userAgentVerify = utils.generateRandomString(300);

		// AdvanceAuth Signup
		String identityUserId = intUtils.userSignUpAdvanceAuthWithUserAgent(client, communicationChannel,
				signupClientType, userAgentToken, userAgentVerify);

		// Validate token user_agent is not saved in user_details table
		String query = "SELECT COUNT(*) AS count FROM user_details WHERE last_user_agent = '" + userAgentToken + "'";
		String countStr = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 30);
		Assert.assertEquals(Integer.parseInt(countStr), 0,
				"User agent value is saved in user_details table for identity user: " + identityUserId);
		utils.logit("User agent value is not saved in user_details table for identity user: " + identityUserId);

		// Validate verify user_agent is saved in user_details table (truncated to 252
		// chars + "..." = 255 chars as per Ruby concat pattern and DB column size)
		query = "SELECT COUNT(*) AS count FROM user_details WHERE last_user_agent = CONCAT(SUBSTRING('"
				+ userAgentVerify + "', 1, 252), '...') and user_id = '" + identityUserId + "';";
		countStr = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 30);
		Assert.assertEquals(Integer.parseInt(countStr), 1,
				"User agent value is not saved in user_details table for identity user: " + identityUserId);
		utils.logit("User agent value is saved in user_details table for identity user: " + identityUserId);

		// Validate user_agent is saved in Punchh users table
		query = "SELECT id from businesses where slug = '" + dataSet.get("slug") + "'";
		String businessId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(businessId, "Business id is null for slug: " + dataSet.get("slug"));
		utils.logit(
				"Business id fetched from businesses table for slug: " + dataSet.get("slug") + " is: " + businessId);

		query = "SELECT id FROM users WHERE (email = '" + communicationChannel + "' or phone = '" + communicationChannel
				+ "') and business_id = '" + businessId + "'";
		String userId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(userId, "User id is null for communication channel: " + communicationChannel);
		utils.logit("User id fetched from users table for communication channel: " + communicationChannel + " is: "
				+ userId);

		// Validate user_agent is saved in Punchh users table
		query = "SELECT user_id FROM user_agent_trackings WHERE last_user_agent = CONCAT(SUBSTRING('" + userAgentVerify
				+ "', 1, 252), '...')";
		String userIdUserAgentTrackings = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "user_id", 30);
		Assert.assertEquals(userIdUserAgentTrackings, userId,
				"User id is not saved in user_agent_trackings table for communication channel: "
						+ communicationChannel);
		utils.logit("User agent value is saved in Punchh users table");
	}

	@DataProvider(name = "emailPhoneVerified")
	public Object[][] emailPhoneVerified() {
		return new Object[][] {
				// SignupClientType, SignupUserType, SigninClientType, UpdateNamespace,
				// UpdateType
				{ "internal_mobile_app", "PhoneOnly", "online_ordering", "api1", "Email" },
				{ "external_mobile_app", "EmailOnly", "internal_mobile_app", "api2", "Phone" } };
	}

	@Test(description = "SQ-T6888 INT2-2265 | Validate proper updation of email_verified if phone_verified is already true and vice-versa.", dataProvider = "emailPhoneVerified")
	@Owner(name = "Vansham Mishra")
	public void T6888_validateEmailAndPhoneVerificationLogic(String signupClientType, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// AdvanceAuth Signup
		String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signupClientType,
				"SignUp");
		String newEmail = null;
		String newPhone = null;
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();

		// Update email or phone number based on updateType
		if (updateType.equalsIgnoreCase("email")) {
			newEmail = intUtils.getRandomGmailEmail();
			communicationChannel = newEmail;
		} else if (updateType.equalsIgnoreCase("phone")) {
			newPhone = dataSet.get("phoneNumber");
			intUtils.updateExistingPhoneOnlyUserInDB(env, client, newPhone);
		}

		Response response = pageObj.endpoints().api1UpdateUser(client, secret, tokensSignUp[0], newEmail, newPhone,
				firstName, lastName);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not match for update user API");
		utils.logit("User update API successful with new email: " + newEmail);
		if (updateType.equalsIgnoreCase("Email")) {
			intUtils.userSignUpSignInAdvanceAuth(client, newEmail, signinClientType, "SignIn");
		} else {
			intUtils.userSignUpSignInAdvanceAuth(client, newPhone, signinClientType, "SignIn");
		}
		validateVerifiedSignupTypeInGuestIdentityDb(communicationChannel);
	}

	@Test(description = "SQ-T6895 INT2-2493 | Verify the error message key in response coming from okta.")
	@Owner(name = "Vansham Mishra")
	public void T6895verifyErrorMessageKeyOkta() throws Exception {
		String codeVerifier = utils.generateCodeVerifier(32);
		String codeChallenge = utils.generateCodeChallenge(codeVerifier);
		boolean privacyPolicy = true;
		boolean tAndc = true;
		String communicationChannel = "1234567890";
		Response responseToken = pageObj.endpoints().advancedAuthToken(client, "otp", null, "+5", communicationChannel,
				codeChallenge, privacyPolicy, tAndc);
		String errorKey = responseToken.jsonPath().getString("errors.invalid_phone_format[0]");
		Assert.assertEquals(errorKey, "Error sending SMS, please check the details and try again.",
				"Error message key did not match for invalid phone format");
		utils.logPass("Verified the error message key in response coming from okta for invalid phone format");
	}

	@DataProvider(name = "Punchh_Signup_AA_SignIn_Update2")
	public Object[][] Punchh_Signup_AA_SignIn_Update2() {
		return new Object[][] {
				// SignupNamespace, SignupUserType, SigninClientType, UpdateNamespace,
				// UpdateType
				{ "api1", "EmailOnly", "internal_mobile_app", "api1", "Email" },
				{ "api1", "EmailOnly", "online_ordering", "api2", "Email" },
				{ "api2", "EmailOnly", "iframe", "auth", "Email" },
				{ "auth", "EmailOnly", "online_ordering", "api2", "Email" },
				{ "pos", "EmailOnly", "external_mobile_app", "api1", "Email" }, };
	}

	@Test(description = "SQ-T6773 INT2-2365 - Password reset through all session API's")
	@Owner(name = "Vansham Mishra")
	public void T6773verifyPasswordResetSessionAPIsPart1() throws Exception {
		// Legacy signup -> passwrod reset bearer token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String accessToken = signUpResponse.jsonPath().get("access_token");

		// Update userInfo with no email
		Response updateUserResponse = pageObj.endpoints().authApiUpdateUserInfoAndPassword(dataSet.get("client"),
				dataSet.get("secret"), authToken, userEmail, dataSet.get("updateFName"), dataSet.get("updateLName"));
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth Api Update userInfo with email");

		// login via auth API using updated user Password
		Response loginResponse1 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"), utils.decrypt(dataSet.get("authUpdatedPassword")));
		Assert.assertEquals(loginResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		// Asynchronous User Update
		Response updateUserInfoResponse = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
				userEmail, dataSet.get("secret"), accessToken);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, updateUserInfoResponse.getStatusCode(),
				"Status code 202 did not matched for api2 asynchronous user update");

		// login via auth API using updated user Password
		Response loginResponse2 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"), utils.decrypt(dataSet.get("newPassword2")));
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		// Update user profile
		Response updateUserInfoResponse2 = pageObj.endpoints().Api2UpdateUserProfile2(dataSet.get("client"), userEmail,
				dataSet.get("secret"), accessToken, utils.decrypt(dataSet.get("newPassword2")),
				utils.decrypt(dataSet.get("newPassword3")), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, updateUserInfoResponse2.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");

		// login via auth API using updated user Password
		Response loginResponse3 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"), utils.decrypt(dataSet.get("newPassword3")));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		Response response = pageObj.endpoints().Api2SecureAsynchronousUserUpdate(client, userEmail, secret, accessToken,
				"password@123", "password@1234");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, response.getStatusCode(),
				"Status code 202 did not matched for api2 secure asynchronous user update");
		// login via auth API using updated user Password
		Response loginResponse4 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"), "password@1234");
		Assert.assertEquals(loginResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		utils.logPass("Password reset through all session API's is successful when legacy user is signed in");
	}

	@Test(description = "SQ-T6773 INT2-2365 - Password reset through all session API's", dataProvider = "Punchh_Signup_AA_SignIn_Update2")
	@Owner(name = "Vansham Mishra")
	public void T6773verifyPasswordResetSessionAPIsPart2(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {

		// rest advance auth signup user flow is covered in AA_Signup_Punchh_SignIn_Test
		String email = intUtils.getRandomGmailEmail();
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		// Punchh SignUp
		Response signUpResponse2 = intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, email,
				phone, locationKey);
		String authToken2 = signUpResponse2.jsonPath().get("auth_token.token");
		utils.logit("auth token after punchh signup: " + authToken2);

		// AdvanceAuth SignIn
		String[] tokens = null;
		tokens = intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");

		// Update userInfo with no email
		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "auth", updateType, tokens[0],
				email, "password@12345", "punchh@123");

		// login via auth API using updated user Password
		Response loginResponse4 = pageObj.endpoints().authApiUserLogin(email, dataSet.get("client"),
				dataSet.get("secret"), "punchh@123");
		Assert.assertEquals(loginResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");
		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "async_users", updateType,
				tokens[0], email, "punchh@123", "punchh@1234");
		Response loginResponse5 = pageObj.endpoints().authApiUserLogin(email, dataSet.get("client"),
				dataSet.get("secret"), "punchh@1234");
		Assert.assertEquals(loginResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "api2", updateType, tokens[0],
				email, "punchh@1234", "punchh@12345");
		Response loginResponse6 = pageObj.endpoints().authApiUserLogin(email, dataSet.get("client"),
				dataSet.get("secret"), "punchh@12345");
		Assert.assertEquals(loginResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "pos_async_users", updateType,
				tokens[0], email, "punchh@12345", "punchh@12346");
		Response loginResponse7 = pageObj.endpoints().authApiUserLogin(email, dataSet.get("client"),
				dataSet.get("secret"), "punchh@12346");
		Assert.assertEquals(loginResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		utils.logit(
				"Password reset through all session API's is successful when  advance auth signup -> legacy signin");
	}

	@Test(description = "SQ-T6773 INT2-2365 - Password reset through all session API's", dataProvider = "Punchh_Signup_AA_SignIn_Update2")
	@Owner(name = "Vansham Mishra")
	public void T6773verifyPasswordResetSessionAPIsPart3(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {
		// advance auth signup -> legacy signin to get legacy bearer token
		String communicationChannel;
		// Signup channel
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// AdvanceAuth SignUp
		String[] tokens2 = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signinClientType,
				"SignUp");
		String userPassword = Utilities.getApiConfigProperty("password");
		// Set password
		pageObj.endpoints().authApiChangePassword(tokens2[0], client, secret, userPassword);
		// Punchh SignIn
		String bearerToken = intUtils.userSignInPunchh(updateNamespace, client, secret, communicationChannel,
				userPassword);

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "auth", updateType, bearerToken,
				communicationChannel, "password@123", "punchh@123");
		Response loginResponse7 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@123");
		Assert.assertEquals(loginResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "async_users", updateType,
				bearerToken, communicationChannel, "punchh@123", "punchh@1234");
		Response loginResponse9 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@1234");
		Assert.assertEquals(loginResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "pos_async_users", updateType,
				bearerToken, communicationChannel, "punchh@1234", "punchh@12345");
		Response loginResponse10 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@12345");
		Assert.assertEquals(loginResponse10.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "api2", updateType, bearerToken,
				communicationChannel, "punchh@12345", "punchh@123456");
		Response loginResponse11 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@123456");
		Assert.assertEquals(loginResponse11.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		utils.logit(
				"Password reset through all session API's is successful when  advance auth signup -> punchh signin");
	}

	@Test(description = "SQ-T6773 INT2-2365 - Password reset through all session API's", dataProvider = "Punchh_Signup_AA_SignIn_Update2")
	@Owner(name = "Vansham Mishra")
	public void T6773verifyPasswordResetSessionAPIsPart4(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {
		// advance auth signup only
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// rest advance auth signup user flow is covered in AA_Signup_Punchh_SignIn_Test
		String userPassword = Utilities.getApiConfigProperty("password");

		// AdvanceAuth Signup
		String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signinClientType,
				"SignUp");

		// Set password
		pageObj.endpoints().authApiChangePassword(tokensSignUp[0], client, secret, userPassword);

		// Update userInfo with no email
		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "async_users", updateType,
				tokensSignUp[0], communicationChannel, "password@123", "punchh@123");

		// login via auth API using updated user Password
		Response loginResponse4 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@123");
		Assert.assertEquals(loginResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "auth", updateType,
				tokensSignUp[0], communicationChannel, "password@123", "punchh@1234");
		Response loginResponse5 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@1234");
		Assert.assertEquals(loginResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "api2", updateType,
				tokensSignUp[0], communicationChannel, "punchh@1234", "punchh@12345");
		Response loginResponse6 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@12345");
		Assert.assertEquals(loginResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		intUtils.legacyUserUpdateWithAdvanceAuthToken(client, secret, signupUserType, "pos_async_users", updateType,
				tokensSignUp[0], communicationChannel, "punchh@12345", "punchh@12346");
		Response loginResponse7 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), "punchh@12346");
		Assert.assertEquals(loginResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with valid password");

		utils.logit(
				"Password reset through all session API's is successful when  advance auth signup -> legacy signin");
	}

	@Test(description = "SQ-T6960 INT2-2600 | Update user sync flows for new flag basic auth |Handle usecases for POS user merge ( Merging while updating the phone number) on Guest Auth DB." +
            "SQ-T7406 INT2-2794 | Update user sync flows for new flag social login(google,facebook,apple) |Handle usecases for POS user merge ( Merging while updating the phone number) on Guest Auth DB.", dataProvider = "Punchh_Signup_AA_SignIn_Update", priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T6960_updateUserSyncForBasicAuthAndPosPhoneMerge(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {

		// loginto punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to settings page and enable basic auth flag
		intUtils.updateAdvanceAndBasicAuthConfig(true, true);
        // Only social flags ON
        intUtils.updateSocialConfig(true, true, true);

		String email = intUtils.getRandomGmailEmail();
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		// Punchh SignUp
		intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, email, phone, locationKey);

		// AdvanceAuth SignIn
		switch (signupUserType) {
		case "EmailOnly":
			intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			break;
		case "PhoneOnly":
			intUtils.userSignUpSignInAdvanceAuth(client, phone, signinClientType, "SignIn");
			break;
		case "EmailPhone":
			intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			break;
		}

		// take the legacy user
		intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignUp");
		// add the validation that the user has been created in punchh and guest
		// identity both dbs
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Strong password signup and validate with Punchh APIs
		String strongPassword = "1A@" + faker.internet().password();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token").toString();
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		String[] userIds = getUserIds(phone);
		utils.logit("guest user id is :- " + userIds[0]);
		utils.logit("punchh user id is :- " + userIds[1]);
		String npwd = "String@123";

		// Update phone number to merge with advance auth user
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail(npwd,
				dataSet.get("phoneNumber"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		// verify in DB that phone number is updated and merged with advance auth user
		validateGuestMergeInDB(userEmail, dataSet.get("phoneNumber"), userIds);
		// enabling adv auth so that other cases should not be impacted
		intUtils.updateAdvanceAndBasicAuthConfig(true, false);
	}
	public void validateGuestMergeInDB(String legacyUserEmail, String actualPhoneNumber, String[] userIds)
			throws Exception {
		// Phone updated in email user - validate both DB’s
		// loyalty_pos_users , loyalty_user_id - email user id, merged_user_id - phone
		// user id (edited)
		utils.longWaitInSeconds(30);
		// verify that there should not be any entry with id = userIds.get(0) in punchh
		// users table
		String fetchPunchhUserQuery = "SELECT COUNT(*) AS count FROM users WHERE id = '" + userIds[1] + "';";
		String countStrPunchh = DBUtils.executeQueryAndGetColumnValue(env, fetchPunchhUserQuery, "count");
		int countPunchh = Integer.parseInt(countStrPunchh);
		Assert.assertEquals(countPunchh, 0, "Entry found in punchh users table for id: " + userIds[1]);
		utils.logit("No entry found in punchh users table for id: " + userIds[1], " ,expected");

		// verify that there should not be any entry with id = userIds.get(0) in guest
		// identity users table
		String fetchGuestIdentityUserQuery = "SELECT COUNT(*) AS count FROM users WHERE id = '" + userIds[0] + "';";
		String countStrGuestIdentity = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost,
				fetchGuestIdentityUserQuery, "count");
		int countGuestIdentity = Integer.parseInt(countStrGuestIdentity);
		Assert.assertEquals(countGuestIdentity, 0, "Entry found in guest identity users table for id: " + userIds[0]);
		utils.logit("No entry found in guest identity users table for id: " + userIds[0]);
		String query = "SELECT id, phone FROM users WHERE email = '" + legacyUserEmail + "';";
		String[] cols = { "id", "phone" };
		List<Map<String, String>> res = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env, query, cols, 2, 20);
		Map<String, String> row = res.get(0);
		String userId = row.get("id");
		String phoneNumber = row.get("phone");
		Assert.assertEquals(phoneNumber, actualPhoneNumber,
				"Phone number is null in guest identity table for legacy user: " + legacyUserEmail);
		utils.logit("Phone number fetched from guest identity table for legacy user: " + phoneNumber);
		Assert.assertFalse(userId.isEmpty(),
				"User ID is empty in guest identity table for legacy user: " + legacyUserEmail);
		utils.logit("User ID fetched from guest identity table for legacy user: " + userId);

		// Assert that an entry with loyalty_user_id == user_id should be present in
		// loyalty_pos_users table
		String fetchLoyaltyPosUserQuery = "SELECT COUNT(*) AS count FROM loyalty_pos_users WHERE loyalty_user_id = '"
				+ userId + "' and merged_user_id = '" + userIds[1] + "';";
		String countStr = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, fetchLoyaltyPosUserQuery, "count", 20);
		int count = Integer.parseInt(countStr);
		Assert.assertTrue(count > 0, "No entry found in loyalty_pos_users table for loyalty_user_id: " + userId);
		utils.logit("pass", "Entry found in loyalty_pos_users table for loyalty_user_id: " + userId);
	}

	public void validateVerifiedSignupTypeInGuestIdentityDb(String communicationChannel) throws Exception {
		String identityUserID;
		String identityUserIdQuery = "select id from users where email = '${communicationChannel}'";
		identityUserIdQuery = identityUserIdQuery.replace("${communicationChannel}", communicationChannel);
		identityUserID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, identityUserIdQuery,
				"id", 20);
		utils.logit("Identity UserID: " + identityUserID);

		// Verify email_verified and phone_verified in user_details table based on
		// signupUserType and updateType
		String query = "SELECT email_verified, phone_verified FROM user_details WHERE user_id = '" + identityUserID
				+ "';";
		String[] cols = { "email_verified", "phone_verified" };
		List<Map<String, String>> userDetailsResults = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env,
				guestIdentityhost, query, cols, 2, 20);
		Map<String, String> userDetails = userDetailsResults.get(0);
		Assert.assertEquals(userDetails.get("email_verified"), "1",
				"verified_signup_type is not Email in users table for user_id: " + identityUserID);
		Assert.assertEquals(userDetails.get("phone_verified"), "1",
				"verified_signup_type is not Phone in users table for user_id: " + identityUserID);
		utils.logit("email_verified is true in user_details table for user_id: " + identityUserID);
	}

	public String[] getUserIds(String Phonenumber) throws Exception {
		// Phone user deleted from both DB’s - check via phone user id
		// user this query to fetch the user id number Select * from
		// guest_identity_production.users u where u.phone_number ='8109175140' order by
		// created_at desc;
		String getIdentityUserIdQuery = "SELECT id FROM users WHERE phone_number = '" + Phonenumber
				+ "' ORDER BY created_at DESC LIMIT 1;";
		String identityUserId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getIdentityUserIdQuery,
				"id");
		Assert.assertNotNull(identityUserId,
				"Phone user ID is null in guest identity table for phone number: " + Phonenumber);
		utils.logit("Phone user ID fetched from guest identity table for phone number: " + identityUserId);

		String getPunchhUserIdQuery = "SELECT id FROM users WHERE phone = '" + Phonenumber
				+ "' ORDER BY created_at DESC LIMIT 1;";
		String punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, getPunchhUserIdQuery, "id");
		Assert.assertNotNull(punchhUserId, "Phone user ID is null in punchh table for phone number: " + Phonenumber);
		utils.logit("Phone user ID fetched from punchh table for phone number: " + punchhUserId);
		List<String> createdUserIds = new ArrayList<>();
		createdUserIds.add(getIdentityUserIdQuery);
		createdUserIds.add(punchhUserId);
		return new String[] { identityUserId, punchhUserId };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}