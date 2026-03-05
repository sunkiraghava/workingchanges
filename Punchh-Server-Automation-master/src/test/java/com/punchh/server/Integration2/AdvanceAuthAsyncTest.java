package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AdvanceAuthAsyncTest {
	static Logger logger = LogManager.getLogger(AdvanceAuthAsyncTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private IntUtils intUtils;
	private String client, secret;
	private String guestIdentityhost = "guestIdentity";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
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
	}

	@DataProvider(name = "Par_Ordering_Signup")
	public Object[][] Par_Ordering_Signup() {
		return new Object[][] {
				// SignupClientType, SignupUserType
				{ "internal_mobile_app", "EmailOnly" }, { "external_mobile_app", "PhoneOnly" },
				{ "online_ordering", "EmailOnly" }, { "iframe", "PhoneOnly" }, };
	}

	@DataProvider(name = "Par_loyalty_Signup")
	public Object[][] Par_loyalty_Signup() {
		return new Object[][] {
				// SignupClientType, SignupUserType, SigninClientType,
				{ "internal_mobile_app", "EmailOnly", "external_mobile_app" },
				{ "internal_mobile_app", "PhoneOnly", "online_ordering" },
				{ "external_mobile_app", "EmailOnly", "internal_mobile_app" },
				{ "external_mobile_app", "PhoneOnly", "iframe" },
				{ "online_ordering", "EmailOnly", "external_mobile_app" },
				{ "online_ordering", "PhoneOnly", "online_ordering" }, { "iframe", "EmailOnly", "internal_mobile_app" },
				{ "iframe", "PhoneOnly", "iframe" }, };
	}

	@Test(description = "SQ-T6975 INT2-2459 | Verify Advanced Authentication with Guest Auth DB flow end to end for the phone only user. Case- when PAR Loyalty client is used and mapping is present for both PAR Loyalty and PAR Ordering.", dataProvider = "Par_loyalty_Signup")
	@Owner(name = "Vansham Mishra")
	public void verifyAdvanceAuthGuestAuthDbFlow_PhoneOnly_PARLoyaltyAndOrdering(String signupClientType,
			String signupUserType, String signinClientType) throws Exception {

		// Signup channel
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

		// Verify AdvanceAuth tokens with Punchh APIs
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, tokensSignUp[0]);

		// AdvanceAuth SignIn
		intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signinClientType, "SignIn");

		// Validate Refresh and sign out AdvanceAuth tokens
		intUtils.validateRefreshAndSignOutAdvanceAuth(client, secret, communicationChannel, tokensSignUp[1]);
		verifyAsyncCallInAuditLogs(communicationChannel);
	}

	@Test(description = "SQ-T6978 Verify Advanced Authentication with Guest Auth DB flow end to end for the phone only user . Case- when  PAR Loyalty client is used and mapping is present for PAR Loyalty only.", dataProvider = "Par_loyalty_Signup")
	@Owner(name = "Vansham Mishra")
	public void verifyAdvanceAuthGuestAuthDbFlow_PhoneOnly_PARLoyaltyOnly(String signupClientType,
			String signupUserType, String signinClientType) throws Exception {
		// setup a par loyalty only business
		// also When par loyalty only business -> no async entry in audit logs
		utils.logit(dataSet.get("slug"));
		// Signup channel
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);
		// AdvanceAuth Signup
		String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(dataSet.get("Client"), communicationChannel,
				signupClientType, "SignUp");

		// Verify AdvanceAuth tokens with Punchh APIs
		intUtils.verifyGISAccessTokenWithPunchhAPIs(dataSet.get("Client"), dataSet.get("Secret"), tokensSignUp[0]);

		// AdvanceAuth SignIn
		intUtils.userSignUpSignInAdvanceAuth(dataSet.get("Client"), communicationChannel, signinClientType, "SignIn");

		// Validate Refresh and sign out AdvanceAuth tokens
		intUtils.validateRefreshAndSignOutAdvanceAuth(dataSet.get("Client"), dataSet.get("Secret"),
				communicationChannel, tokensSignUp[1]);
		verifySyncCallInAuditLogs(communicationChannel);
	}

	@DataProvider(name = "sessionless_reset_password")
	public Object[][] sessionless_reset_password() {
		return new Object[][] {
				// SignupNamespace, SignupUserType, SigninClientType, UpdateNamespace,
				// UpdateType
				{ "api1", "EmailOnly", "internal_mobile_app", "api1", "Email" }, };
	}

	@Test(description = "SQ-T6772 INT2-2365 - Password reset through sessionless API's", dataProvider = "sessionless_reset_password")
	@Owner(name = "Vansham Mishra")
	public void T6772_verifyPasswordResetThroughAllSessionLessAPIsPart1(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {
		// advance auth signup only
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// Rest advance auth signup user flow is covered in AA_Signup_Punchh_SignIn_Test
		String userPassword = Utilities.getApiConfigProperty("password");

		// AdvanceAuth SignUp
		String[] tokens2 = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signinClientType,
				"SignUp");
		Response resetPasswordTokenOfTheUserResponse = pageObj.endpoints()
				.authApiResetPasswordTokenOfTheUser(dataSet.get("client"), dataSet.get("secret"), communicationChannel);
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth Api Get reset_password_token of the user");
		String resetPasswordToken = resetPasswordTokenOfTheUserResponse.jsonPath().getString("reset_password_token");
		// Set password
		pageObj.endpoints().authApiChangePasswordAdvanceAuthWithResetPasswordToken(tokens2[0], client, secret,
				userPassword, resetPasswordToken);

		// Punchh SignIn
		String bearerToken = intUtils.userSignInPunchh(updateNamespace, client, secret, communicationChannel,
				userPassword);
		utils.logit("bearerToken after Punchh SignIn: " + bearerToken);
		// Forgot password
		Response forgotPasswordResponse = pageObj.endpoints().authForgotPassword(communicationChannel,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, forgotPasswordResponse.getStatusCode(),
				"Status code 200 did not matched for api2 forgot password");

		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls = intUtils.extractUrls(emailBody);
		// assert url is not empty
		Assert.assertTrue(!urls.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword = "New" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls.get(0), newPassword);
		Response loginResponse = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword);
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from auth forget password api");

		// Api v1 forgot password
		Response forgotPasswordResponse2 = pageObj.endpoints().Api1MobileForgotPassword(communicationChannel,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logit("pass", "verified that user is able to login with new password after reset");
		String emailBody2 = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls2 = intUtils.extractUrls(emailBody2);
		// assert url is not empty
		Assert.assertTrue(!urls2.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword2 = "New2" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls2.get(0), newPassword2);
		Response loginResponse2 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword2);
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from api1 mobile forgot password api");

		// Forgot password with invalid client id
		Response forgotPasswordResponse3 = pageObj.endpoints().Api2ForgotPassword(dataSet.get("client"),
				dataSet.get("secret"), communicationChannel);
		Assert.assertEquals(forgotPasswordResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 401 did not matched for api2 forgot password");
		String emailBody3 = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls3 = intUtils.extractUrls(emailBody3);
		// assert url is not empty
		Assert.assertTrue(!urls3.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword3 = "New3" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls3.get(0), newPassword3);
		Response loginResponse3 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword3);
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from api2 forgot password api");
	}

	@Test(description = "SQ-T6772 INT2-2365 - Password reset through sessionless API's", dataProvider = "sessionless_reset_password")
	@Owner(name = "Vansham Mishra")
	public void T6772_verifyPasswordResetThroughAllSessionLessAPIsPart2(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {
		// legacy user signup only
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// rest advance auth signup user flow is covered in AA_Signup_Punchh_SignIn_Test
		String email = intUtils.getRandomGmailEmail();
		String userPassword = Utilities.getApiConfigProperty("password");

		communicationChannel = email;
		Response signUpResponse = pageObj.endpoints().authApiSignUp(email, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		// Forgot password
		Response forgotPasswordResponse = pageObj.endpoints().authForgotPassword(communicationChannel,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, forgotPasswordResponse.getStatusCode(),
				"Status code 200 did not matched for api2 forgot password");

		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls = intUtils.extractUrls(emailBody);
		// assert url is not empty
		Assert.assertTrue(!urls.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword = "New" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls.get(0), newPassword);
		Response loginResponse = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword);
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from auth forget password api");

		// Api v1 forgot password
		Response forgotPasswordResponse2 = pageObj.endpoints().Api1MobileForgotPassword(communicationChannel,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logit("pass", "verified that user is able to login with new password after reset ");
		String emailBody2 = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls2 = intUtils.extractUrls(emailBody2);
		// assert url is not empty
		Assert.assertTrue(!urls2.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword2 = "New2" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls2.get(0), newPassword2);
		Response loginResponse2 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword2);
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from api1 mobile forgot password api");

		// Forgot password with invalid client id
		Response forgotPasswordResponse3 = pageObj.endpoints().Api2ForgotPassword(dataSet.get("client"),
				dataSet.get("secret"), communicationChannel);
		Assert.assertEquals(forgotPasswordResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 401 did not matched for api2 forgot password");
		String emailBody3 = GmailConnection.getGmailEmailBody("toAndFromEmail",
				communicationChannel + "," + "help@punchh.com", true);
		List<String> urls3 = intUtils.extractUrls(emailBody3);
		// assert url is not empty
		Assert.assertTrue(!urls3.isEmpty(), "No URLs found in the email body");
		// navigate to extracted url and reset password
		String newPassword3 = "New3" + userPassword;
		pageObj.instanceDashboardPage().LoginThroughForgetPasswordLink(urls3.get(0), newPassword3);
		Response loginResponse3 = pageObj.endpoints().authApiUserLogin(communicationChannel, dataSet.get("client"),
				dataSet.get("secret"), newPassword3);
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for auth Api user login with new password after reset");
		utils.logit("pass",
				"verified that user is able to login with new password after reset from api2 forgot password api");
	}

	public void verifyAsyncCallInAuditLogs(String communicationChannel) throws Exception {
		String query = "SELECT id FROM users WHERE email = '" + communicationChannel + "' or phone_number = '"
				+ communicationChannel + "' order by created_at desc limit 1";
		String identityUserId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		Assert.assertNotNull(identityUserId, "Identity user id is null");
		utils.logit("Identity user id: " + identityUserId);

		// execute the below query and verify that count should be > 1
		String query2 = "SELECT COUNT(*) AS count from audit_logs where user_id = '" + identityUserId
				+ "' and event_type ='user_signup_async';";
		String count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query2, "count", 10);
		Assert.assertNotNull(count, "Audit log count is null");
		int countValue = Integer.parseInt(count);
		Assert.assertTrue(countValue > 0, "Audit log count is not greater than 0");
		utils.logit("Audit log count for user_signup_async event: " + countValue);
	}

	public void verifySyncCallInAuditLogs(String communicationChannel) throws Exception {
		String query = "SELECT id FROM users WHERE email = '" + communicationChannel + "' or phone_number = '"
				+ communicationChannel + "' order by created_at desc limit 1";
		String identityUserId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		Assert.assertNotNull(identityUserId, "Identity user id is null");
		utils.logit("Identity user id: " + identityUserId);

		// execute the below query and verify that count should be > 1
		String query2 = "SELECT COUNT(*) AS count from audit_logs where user_id = '" + identityUserId
				+ "' and event_type ='otp_verification';";
		String count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query2, "count", 10);
		Assert.assertNotNull(count, "Audit log count is null");
		int countValue = Integer.parseInt(count);
		Assert.assertTrue(countValue > 0, "Audit log count is not greater than 0");
		utils.logit("Audit log count for user_signup_async event: " + countValue);

		String getAuthRequestQuery = "select count(*) as count from auth_requests where user_id='${UserId_identity}' and status='3'"
				.replace("${UserId_identity}", identityUserId);
		String authRequestCount = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, getAuthRequestQuery,
				"count");
		// assert that authRequestCount > 1
		Assert.assertNotEquals(authRequestCount, "0",
				"Entry for event_type -> otp_verification. is not created in auth_requests table");
		utils.logit("Auth request count for otp_verification event: " + authRequestCount);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}