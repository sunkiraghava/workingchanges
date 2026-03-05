package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class basicAuthUITest {
	static Logger logger = LogManager.getLogger(basicAuthUITest.class);
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
		userEmail = intUtils.getRandomGmailEmail();
	}

	@Test(description = " (SQ-T6973 INT2-2545 | Change Password (Admin) : Spec + Build (Endpoint).")
	@Owner(name = "Vansham Mishra")
	public void T6973_validateChangePasswordFromAdminDashboard() throws Exception {

		String strongPassword = "1A@" + faker.internet().password();
		// Strong password signup and validate with Punchh APIs
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 200 did not match for deactivate user API");
		// login to Punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String updatedPassword = "2A@" + faker.internet().password();
		pageObj.guestTimelinePage().UpdateUserPWDFromTimeline(updatedPassword);

//         Sign in and validate with Punchh APIs
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, updatedPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		utils.logPass("Verified the Change Password functionality from UI and able to login with updated password from Dashboard successfully");
	}

	@Test(description = " (SQ-T6961 INT2-2103 | INT2-2104 | Basic Auth | Forgot Password | Case -1 :- User not present in both Punchh and Guest DB.")
	@Owner(name = "Vansham Mishra")
	public void T6961_forgotPasswordSilentIfUserNotPresent() throws Exception {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		// Forgot password with token_in_response as false and validate the response
		Response response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "false");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String responseMessage = response.jsonPath().getString("message[0]");
		Assert.assertEquals(responseMessage, dataSet.get("errorMessage"));

//        validate the audit log entry
		String detailsFromAuditLog = fetchAuditLogDetailsByEmail(userEmail);
		Assert.assertTrue(detailsFromAuditLog.contains(dataSet.get("auditLogsErrorMessage")),
				"Audit log message not found!");
		utils.logPass("Verified the error message and code when users not present in both Punchh and Guest and we are calling Basic Auth forgot password api \"/api2/basic_auth/forgot_password\"  with \"token_in_response\" is false. ");

		// Forgot password with token_in_response as true and validate the response
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String responseMessage2 = response.jsonPath().getString("errors.incorrect_info[0]");
		Assert.assertEquals(responseMessage2, dataSet.get("incorrect_info"));

		// validate the audit log entry
		detailsFromAuditLog = fetchAuditLogDetailsByEmail(userEmail);
		Assert.assertTrue(detailsFromAuditLog.contains(MessagesConstants.auditLogsUserNotFound),
				"Audit log message not found!");
		utils.logPass("Verified the error message and code when users not present in both Punchh and Guest and we are calling Basic Auth forgot password api \"/api2/basic_auth/forgot_password\"  with \"token_in_response\" is true. ");
	}

	@Test(description = " (SQ-T6962 INT2-2103 | INT2-2104 | Basic Auth | Forgot Password | Case -2 :- User present in both Punchh and Guest DB." +
            "SQ-T7195 INT2-2771 | Send random uuid in forgot password if user not present.")
	@Owner(name = "Vansham Mishra")
	public void T6962_forgotPasswordSilentIfUserIsPresent() throws Exception {
		// login to Punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		// set password policy to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
		String strongPassword = "1A@" + faker.internet().password();
		// Strong password signup and validate with Punchh APIs
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response basicAuthSignUpResponse = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(basicAuthSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not match for deactivate user API");

        // fetch identity_uuid from punchh users table where email = userEmail
        String query = "SELECT identity_uuid FROM users WHERE email = '" + userEmail + "'";
        String identity_uuid = DBUtils.executeQueryAndGetColumnValue(env, query, "identity_uuid");
        // assert identity_uuid is not null
        Assert.assertNotNull(identity_uuid, "identity_uuid is null for email: " + userEmail);

		// Forgot password and validate with Reset password and Signin
		Response response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "false");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String responseMessage = response.jsonPath().getString("message[0]");
		Assert.assertEquals(responseMessage, dataSet.get("message1"));
		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail", userEmail + "," + "help@punchh.com",
				true);
		List<String> resetPasswordToken = extractResetPasswordToken(emailBody);
		// assert url is not empty
		Assert.assertTrue(!resetPasswordToken.isEmpty(), "No URLs found in the email body");
		utils.logPass("verified that user has received the reset password token on email: " + userEmail);
        // fetch the identity_uuid from guest users table for the userEmail
        String guestIdentityUuidQuery = "SELECT uuid FROM users WHERE email = '" + userEmail + "'";
        String guestIdentityUuid = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, guestIdentityUuidQuery, "uuid");
        // assert that identity_uuid from users and guest_users table should be same
        Assert.assertEquals(guestIdentityUuid, identity_uuid, "identity_uuid from users and guest_users table did not match for email: " + userEmail);
        utils.logit("Verified that UUID has not been updated");
		// verify the count entry in audit logs with communication channel as user email
		// and validation_method as 'forgot_password'
		String count = validatValidationMethod(userEmail);
		Assert.assertEquals(count, "1", "Audit log entry count did not match!");
		// Verify that en entry should be created/updated for that token and token time
		// in the Guest users table under "reset_password_token" and
		// "reset_password_sent_at".
		vaidateResetPasswordTokenDetails(userEmail);
		utils.logPass("Verified the error message and code when users not present in both Punchh and Guest and we are calling Basic Auth forgot password api \"/api2/basic_auth/forgot_password\"  with \"token_in_response\" is false. ");

		// try the above flow with different user and with token_in_response as true
		String userEmail2 = intUtils.getRandomGmailEmail();
		DynamicPayloadBuilder userPayload2 = new DynamicPayloadBuilder();
		userPayload2.setEmail(userEmail2).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response basicAuthSignUpResponse2 = pageObj.endpoints().basicAuthSignUp(client, userPayload2.buildPayloadMap());
		Assert.assertEquals(basicAuthSignUpResponse2.getStatusCode(), 201,
				"Status code 200 did not match for deactivate user API");
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail2, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String responseMessage2 = response.jsonPath().getString("message");
		String tokenInResponse = response.jsonPath().getString("data.reset_token");
		String tokenExpiryAtInResponse = response.jsonPath().getString("data.token_expiry_at");
		Assert.assertNotNull(tokenExpiryAtInResponse, "token_expiry_at in response is null!");
		Assert.assertNotNull(tokenInResponse, "reset_password_token in response is null!");
		Assert.assertEquals(responseMessage2, dataSet.get("trueRespTokenMsg"));

		String count2 = validatValidationMethod(userEmail2);
		Assert.assertEquals(count2, "1", "Audit log entry count did not match!");
		// Verify that en entry should be created/updated for that token and token time
		// in the Guest users table under "reset_password_token" and
		// "reset_password_sent_at".
		vaidateResetPasswordTokenDetails(userEmail2);
		utils.logPass("Verified the error message and code when users not present in both Punchh and Guest and we are calling Basic Auth forgot password api \"/api2/basic_auth/forgot_password\"  with \"token_in_response\" is true. ");
	}

	@Test(description = " (SQ-T6963 INT2-2103 | INT2-2104 | Basic Auth | Forgot Password | Case -3 :- User present in Punchh but not in the Guest DB.")
	@Owner(name = "Vansham Mishra")
	public void T6963_forgotPasswordIfUserPresentInPunchh() throws Exception {
		// login to Punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkUncheckAnyFlag("Send Email Verification", "uncheck");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");

		// set password policy to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
		// API1 user signup
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// basic auth forgot password with token_in_response as false and validate the
		// response
		Response response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "false");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail", userEmail + "," + "help@punchh.com",
				true);
		List<String> resetPasswordToken = extractResetPasswordToken(emailBody);
		// assert url is not empty
		Assert.assertTrue(!resetPasswordToken.isEmpty(), "No URLs found in the email body");
		utils.logPass("verified that user has received the reset password token on email: " + userEmail);
		verifyEntryInUsersAndUserDetailsTable(userEmail);
		vaidateResetPasswordTokenDetails(userEmail);

		String newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthResetPassword(client, resetPasswordToken.get(0), newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		utils.logPass("Verified the complete flow of basic auth forgot password with token_in_response as false and able to reset and login with new password successfully");

		// basic auth forgot password withParams token_in_response as true and validate
		// the response
		String userEmail2 = intUtils.getRandomGmailEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse2.jsonPath().get("email").toString(), userEmail2.toLowerCase());
		Response response2 = pageObj.endpoints().basicAuthForgotPassword(client, userEmail2, "true");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		vaidateResetPasswordTokenDetails(userEmail2);
		String resetToken = response2.jsonPath().get("data.reset_token");

		newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail2, newStrongPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		verifyEntryInUsersAndUserDetailsTable(userEmail2);

	}

	@Test(description = "(SQ-T6953) INT2-2607 | Validate Basic and Advanced Auth in Meta API")
	@Owner(name = "Nipun Jain")
	public void T6953_testBasicAndAdvanceAuthInMetaAPI() throws Exception {

		String slug = dataSet.get("slug");
		String client = dataSet.get("client");
		String secret = dataSet.get("secret");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);

		intUtils.updateAdvanceAndBasicAuthConfig(true, true);
		validateBasicAndAdvanceAuthInMetaAPIs(client, secret, true, true);
		intUtils.updateAdvanceAndBasicAuthConfig(false, false);
		validateBasicAndAdvanceAuthInMetaAPIs(client, secret, false, false);
		intUtils.updateAdvanceAndBasicAuthConfig(true, false);
		validateBasicAndAdvanceAuthInMetaAPIs(client, secret, true, false);
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		validateBasicAndAdvanceAuthInMetaAPIs(client, secret, false, true);

		utils.logPass("Basic and Advance Auth in Meta API test completed successfully");
	}

	@Test(description = "(SQ-T6957) INT2-2600 | Update user sync flows for new flag basic auth dashboard")
	@Owner(name = "Nipun Jain")
	public void T6957_testUpdateUserSyncFlows_BasicAuthDashboard() throws Exception {

		String slug = dataSet.get("slug");
		String client = dataSet.get("client");

		// Strong password signup and validate with Punchh APIs
		String strongPassword = "1A@" + faker.internet().password();
		String userEmail = "basic_auth_" + utils.getTimestampInNanoseconds() + "@partech.com";
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);

		// Get punchh user id from users table
		String query = "SELECT id FROM users WHERE email = '" + userEmail + "'";
		String punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertTrue(!punchhUserId.equals(""), "Punchh user ID is empty");
		utils.logit("Punchh user ID: " + punchhUserId);

		// Navigate to instance dashboard and select business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);

		// Navigate to users page
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickEditProfile();

		// User update dashboard - email only
		String updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		pageObj.guestTimelinePage().setEmail(updateEmail);
		pageObj.guestTimelinePage().saveProfile();
		intUtils.validateUserSyncWithGIS(punchhUserId);

		// User update dashboard - Add Phone
		String phoneNumber = utils.phonenumber();
		pageObj.guestTimelinePage().setPhone(phoneNumber);
		pageObj.guestTimelinePage().saveProfile();
		intUtils.validateUserSyncWithGIS(punchhUserId);

		// User update dashboard - email + phone
		updateEmail = "basic_auth" + utils.getTimestampInNanoseconds() + "@partech.com";
		pageObj.guestTimelinePage().setEmail(updateEmail);
		pageObj.guestTimelinePage().setPhone(phoneNumber);
		pageObj.guestTimelinePage().saveProfile();
		intUtils.validateUserSyncWithGIS(punchhUserId);

		utils.logPass("User sync flows test completed successfully for Basic Auth Dashboard");
	}

	@Test(description = "SQ-T6958 | Basic Auth | UserSync | Anonymise user")
	@Owner(name = "Nipun Jain")
	public void SQ_T6958_VerifyBasicAuthUserSyncForAnonymise() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		String adminKey = dataSet.get("adminKey");
		
		// Basic Auth Signup
		String strongPassword = "1A@" + faker.internet().password();
		String userEmail = "basic_auth_" + utils.getTimestampInNanoseconds() + "@partech.com";
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for basic auth sign up API");
		
		intUtils.verifyGISUserSyncForActivity(client, adminKey, userEmail, "Anonymise");
	}

	@Test(description = "SQ-T6959 | Basic Auth | UserSync | Deactivate and Delete user")
	@Owner(name = "Nipun Jain")
	public void SQ_T6959_VerifyBasicAuthUserSyncForDeactivateDelete() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		String adminKey = dataSet.get("adminKey");

		// Basic Auth Signup
		String strongPassword = "1A@" + faker.internet().password();
		String userEmail = "basic_auth_" + utils.getTimestampInNanoseconds() + "@partech.com";
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for basic auth sign up API");
		
		intUtils.verifyGISUserSyncForActivity(client, adminKey, userEmail, "Delete");
	}

	private void validateBasicAndAdvanceAuthInMetaAPIs(String client, String secret, boolean advanceAuthEnabled,
			boolean basicAuthEnabled) throws Exception {

		// api2/mobile/meta.json
		Response response = pageObj.endpoints().Api2Meta(client, secret);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Validate enable_advance_auth in meta response - api2/mobile/meta.json
		if (advanceAuthEnabled)
			Assert.assertTrue(response.jsonPath().getBoolean("enable_advance_auth"),
					"'enable_advance_auth' should be true in api2/mobile/meta.json response");
		else
			Assert.assertFalse(response.jsonPath().toString().contains("enable_advance_auth"),
					"'enable_advance_auth' field should not be present in api2/mobile/meta.json response when advance auth is disabled.");

		// Validate enable_basic_auth in meta response - api2/mobile/meta.json
		if (basicAuthEnabled)
			Assert.assertTrue(response.jsonPath().getBoolean("enable_basic_auth"),
					"'enable_basic_auth' should be true in api2/mobile/meta.json response");
		else
			Assert.assertFalse(response.jsonPath().toString().contains("enable_basic_auth"),
					"'enable_basic_auth' field should not be present in api2/mobile/meta.json response when basic auth is disabled.");

		// api/mobile/meta.json
		response = pageObj.endpoints().Api1Cards(client, secret);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Validate enable_advance_auth in meta response - api/mobile/meta.json
		if (advanceAuthEnabled)
			Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().getList("enable_advance_auth").get(0)),
					"'enable_advance_auth' should be true in api/mobile/meta.json response");
		else
			Assert.assertFalse(response.jsonPath().toString().contains("enable_advance_auth"),
					"'enable_advance_auth' field should not be present in api/mobile/meta.json response when advance auth is disabled.");

		// Validate enable_basic_auth in meta response - api/mobile/meta.json
		if (basicAuthEnabled)
			Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().getList("enable_basic_auth").get(0)),
					"'enable_basic_auth' should be true in api/mobile/meta.json response");
		else
			Assert.assertFalse(response.jsonPath().toString().contains("enable_basic_auth"),
					"'enable_basic_auth' field should not be present in api/mobile/meta.json response when basic auth is disabled.");

		utils.logPass("Basic and Advance Auth are as expected in Meta API responses");
	}

	public String fetchAuditLogDetailsByEmail(String email) throws Exception {
		String query = "SELECT details FROM audit_logs WHERE communication_channel = '" + email + "';";
		String[] cols = { "details" };
		List<Map<String, String>> res = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env, guestIdentityhost,
				query, cols, 2, 20);
		if (res == null || res.isEmpty()) {
			throw new AssertionError("No audit log rows found for: " + email);
		}
		return res.get(0).get("details");
	}

	public String validatValidationMethod(String email) throws Exception {
		String auditQuery = "SELECT COUNT(*) as count FROM audit_logs WHERE communication_channel = '" + userEmail
				+ "' AND validation_method = 'forgot_password';";
		String[] auditCols = { "count" };
		List<Map<String, String>> auditRes = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env,
				guestIdentityhost, auditQuery, auditCols, 2, 20);
		if (auditRes == null || auditRes.isEmpty()) {
			throw new AssertionError("No audit log rows found for: " + email);
		}
		Map<String, String> auditRow = auditRes.get(0);
		String count = auditRow.get("count");
		return count;
	}

	public void vaidateResetPasswordTokenDetails(String email) throws Exception {
		String guestUserQuery = "SELECT reset_password_token, reset_password_sent_at FROM users WHERE email = '" + email
				+ "';";
		String[] guestUserCols = { "reset_password_token", "reset_password_sent_at" };
		List<Map<String, String>> guestUserRes = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env,
				guestIdentityhost, guestUserQuery, guestUserCols, 2, 20);

		if (guestUserRes == null || guestUserRes.isEmpty()) {
			throw new AssertionError("No guest user rows found for: " + email);
		}

		Map<String, String> guestUserRow = guestUserRes.get(0);
		String resetPasswordToken = guestUserRow.get("reset_password_token");
		String resetPasswordSentAt = guestUserRow.get("reset_password_sent_at");

		Assert.assertNotNull(resetPasswordToken, "reset_password_token is null!");
		Assert.assertNotNull(resetPasswordSentAt, "reset_password_sent_at is null!");
		utils.logPass("Validated that reset_password_token and reset_password_sent_at are present in the Guest users table for email: "
						+ email);
	}

	public void verifyEntryInUsersAndUserDetailsTable(String email) throws Exception {
		String query = "SELECT id FROM users WHERE email = '" + email + "';";
		String userId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "id", 10);
		Assert.assertNotNull(userId, "User ID is null in users table for email: " + email);
		String userDetailsQuery = "SELECT count(*) as count FROM user_details WHERE user_id = '" + userId + "';";
		String count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, userDetailsQuery,
				"count", 10);
		Assert.assertEquals(count, "1", "User details entry not found for user ID: " + userId);
		utils.logPass("Validated that user entry is present in users and user_details table for email: " + email);

		// validate entry in audit logs table
		String auditQuery = "SELECT COUNT(*) as count FROM audit_logs WHERE communication_channel = '" + email
				+ "' and event_type ='forgot_token_sent';";
		String auditCount = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, auditQuery,
				"count", 10);
		Assert.assertEquals(auditCount, "1", "Audit log entry not found for email: " + email);
		utils.logPass("Validated that audit log entry is present for email: " + email);
	}

	public static List<String> extractResetPasswordToken(String text) {
		if (text == null || text.isEmpty()) {
			return Collections.emptyList();
		}

		Pattern pattern = Pattern.compile("Reset\\s+Password\\s+Token:\\s*([A-Za-z0-9_\\-]+)",
				Pattern.CASE_INSENSITIVE);

		Matcher matcher = pattern.matcher(text);
		List<String> tokens = new ArrayList<>();

		while (matcher.find()) {
			tokens.add(matcher.group(1));
		}

		return tokens;
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}