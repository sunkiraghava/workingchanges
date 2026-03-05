package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class UserEmailSignUpFunctionalityTest {
	static Logger logger = LogManager.getLogger(UserEmailSignUpFunctionalityTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private Utilities utils;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	// Owner : Amit

	@Test(description = "SQ-T3202 Enable user to signup via email when dependent flags are disabled", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name ="Shashank Sharma")
	public void T3202_EnableUserToSignupViaEmailWhenDependentFlagsDisabled() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Accept Phone Number?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Phone Number as a mandatory field?",
				"uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Accept minimum 10 digit Phone Number?", "uncheck");
		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUp();
		// pageObj.iframeSingUpPage().iframeSignOut();
		TestListeners.extentTest.get()
				.pass(" Enable user to signup via email when dependent flags are disabled passed");

	}

	@Test(description = "SQ-T3213 || SQ-T3214 || SQ-T3209 Validate SsoSignUp SignIn ChangePassword", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name ="Shashank Sharma")
	public void T3213_verifySsoSignUpSignInChangePassword() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// SSO signup
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().goToSsoSignUp(dataSet.get("OAuthAppsName"));
		String ssoEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.ssoLoginSingupPage().signUpViaSso(ssoEmail);
		utils.waitTillPagePaceDone();
		// Verify redirected URL after SSO signup
		Assert.assertTrue(utils.verifyPartOfURL(baseUrl),
				"Redirected URL after SSO signup did not match");

		// sso signin and verify guest on timeline
		// navigate to menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().goToSsoSignUp(dataSet.get("OAuthAppsName"));
		pageObj.ssoLoginSingupPage().ssoLogin(ssoEmail);
		// Verify redirected URL after SSO login
		Assert.assertTrue(utils.verifyPartOfURL(baseUrl),
				"Redirected URL after SSO signup did not match");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(ssoEmail);

		// sso password reset
		// navigate to menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().goToSsoSignUp();
		String msg = pageObj.ssoLoginSingupPage().forgotPassword(ssoEmail);
		Assert.assertEquals(msg,
				"If an account currently exists within our system, an email will be sent to the associated address with instructions on resetting the password.");
		TestListeners.extentTest.get().pass("SsoSignUp SignIn ChangePassword validated successfully");

	}

	@Test(description = "SQ-T3227,T3216,T3230,T3249,T3250,T3931 Verify Update Guest Details Using APiV1 APiV2", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name ="Shashank Sharma")
	public void T3227_verifyUpdateGuestDetailsUsingAPiV1APiV2() {
		// User register/signup using APIV1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String Fname = signUpResponse.jsonPath().get("first_name").toString();
		String Lname = signUpResponse.jsonPath().get("last_name").toString();

		String userEmailNew = pageObj.iframeSingUpPage().generateEmail();
		// Update user profile using apiV2 {email and pwd update}
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmailNew,
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, updateUserInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");
		TestListeners.extentTest.get().pass("Api2 update guest profile is successful");

		String userNewEmail = pageObj.iframeSingUpPage().generateEmail();
		// Update user profile using apiV1 {fname, Lname and pwd update}
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetails("New" + Fname, "New" + Lname,
				dataSet.get("Npwd"), dataSet.get("client"), dataSet.get("secret"), token, userNewEmail);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String UpdatedFname = updateGuestResponse.jsonPath().get("first_name").toString();
		String UpdatedLname = updateGuestResponse.jsonPath().get("last_name").toString();
		Assert.assertNotEquals(Fname, UpdatedFname);
		Assert.assertNotEquals(Lname, UpdatedLname);
		TestListeners.extentTest.get().pass("Api1 guest details update is successful");

		// Api v1 forgot password
		Response forgotPasswordResponse = pageObj.endpoints().Api1MobileForgotPassword(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String forgotPwdResponse = forgotPasswordResponse.jsonPath().get("[0]").toString();
		Assert.assertNotEquals(forgotPwdResponse,
				"You will receive an email with instructions about how to reset your password in a few minutes.");
		TestListeners.extentTest.get().pass("Api v1 forgot password is successful");

		// Auth API forgot password
		Response authForgotPasswordResponse = pageObj.endpoints().authForgotPassword(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(authForgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Auth API forgot passwordis successful");
	}

	@Test(description = "SQ-T3265 Transfer loyalty points VIA API V1", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name ="Shashank Sharma")
	public void T3265_verifyTransferLoyaltyPointsviaAPIV1() throws InterruptedException {

		// User 1 register/signup using APIV1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();

		// User 2 register/signup using APIV1 Signup
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");

		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Transfered Loyalty Expiry");
		String expectedSuccessMessage = dataSet.get("expectedSuccessMessage");
		pageObj.earningPage().setTransferredPointsExpiryDaysAsFixedDays(1);
		pageObj.earningPage().verifyMessage(expectedSuccessMessage);

		// Gift points to the user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// Transfer points to other user via apiv1 transferred_points_expiry_strategy =
		// fixed in cockpit> earning
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2, "10",
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for transfer points to other user via apiv1");
		String val = transferPointsResponse.jsonPath().get("[0]").toString();
		Assert.assertTrue(val.contains("10 transferred to"),
				"Status code 200 did not matched for transfer points to other user via apiv1");

	}

	@Test(description = "SQ-T4091 Verify user can be signed up via /api2/dashboard/eclub_guests as eclub "
			+ "user with \"terms_and_conditions\" and \"privacy_policy\" true "
			+ " SQ-T6520 : verify that eclub users can be added into business via eclub post API ", groups = {
					"regression", "dailyrun" }, priority = 4)
	@Owner(name ="Shashank Sharma")
	public void T4091_TermsPrivacyPolicy() throws Exception {

		// user singup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.endpoints().dashboardApiEClubUploadPrivacyAndTerms(userEmail, dataSet.get("apiKey"),
				dataSet.get("storeNumber"));

		String query = "Select user_details from users where email = '" + userEmail + "'";
		String statusColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"user_details", 15);
		Assert.assertNotNull(statusColValue, "Value is not present at user_details column in user table");

		pageObj.singletonDBUtilsObj();
		String flag = DBUtils.businessesPreferenceFlag(statusColValue, "terms_and_conditions");
		Assert.assertEquals(flag, "true", "terms_and_conditions flag is not true");
		utils.logPass("terms_and_conditions is true for eclub user");
		pageObj.singletonDBUtilsObj();
		String flag1 = DBUtils.businessesPreferenceFlag(statusColValue, "privacy_policy");
		Assert.assertEquals(flag1, "true", "privacy_policy flag is not true");
		utils.logPass("privacy_policy is true for eclub user");

		TestListeners.extentTest.get()
				.pass("'terms and conditions' and 'privacy policy' are coming True when user signup "
						+ "using api2/dashboard/eclub_guests");

		// verify user is not in custom list segment
		Response response1 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmentId"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		Assert.assertFalse(response1.jsonPath().getBoolean("result"));
		utils.logPass("User is not in custom list segment");

		// add eclub user to custom list segment and verify user is in segment
		int segmentId = Integer.parseInt(dataSet.get("segmentId"));
		Response addUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(segmentId, userEmail,
				dataSet.get("apiKey"));
		Assert.assertEquals(addUserSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 not matched for Add user to custom segment");
		utils.longWaitInSeconds(2);
		Response response2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmentId"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		Assert.assertTrue(response2.jsonPath().getBoolean("result"));
		utils.logPass("User is added in custom list segment");

		// user signup using api/auth/customers.json api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signup = pageObj.endpoints().authApiSignUpPrivacyAndTerms(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String termsConditions1 = signup.jsonPath().get("terms_and_conditions").toString();
		String privacyPolicy1 = signup.jsonPath().get("privacy_policy").toString();

		Assert.assertEquals(termsConditions1, "true");
		Assert.assertEquals(privacyPolicy1, "true");
		TestListeners.extentTest.get()
				.pass("'terms and conditions' and 'privacy policy' are coming True when user signup "
						+ "using api/auth/customers.json api");

		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		String termsConditions2 = login.jsonPath().get("terms_and_conditions").toString();
		String privacyPolicy2 = login.jsonPath().get("privacy_policy").toString();

		Assert.assertEquals(termsConditions2, "true");
		Assert.assertEquals(privacyPolicy2, "true");
		TestListeners.extentTest.get()
				.pass("'terms and conditions' and 'privacy policy' are coming True when user login ");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
