package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DashboardAdminAccessTest {

	private static Logger logger = LogManager.getLogger(DashboardAdminAccessTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "TD-T538 Validatedashboard admin access api permission for new support gifting API", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T538_validateDashboardAdminAccessApiPermissionForNewSupportGiftingAPI() throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");

		// Disable dashboard api access permission for the role and save it
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));
		pageObj.adminRolesPage().onOffAdminRoelPermission("api:manage", "off");

		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send points to the user via new support gifting api
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), 403,
				"Status code 202 did not matched for dashboard api2 support gifting to user");
		String errorMsg = sendPointsResponse.jsonPath().get("no_permission_error").toString();
		Assert.assertEquals(errorMsg, "Insufficient Privileges to access this resource",
				"Insufficient Privileges error message did not matched");
		utils.logPass(
				"Insufficient Privileges admin role for dashboard api access validated successfully");

		// Enaable dashboard api access permission for the role and save it
		pageObj.adminRolesPage().onOffAdminRoelPermission("api:manage", "on");

		// send points to the user via new support gifting api
		Response sendPointsResponse1 = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for dashboard api2 support gifting to user");
		utils.logPass("admin role permission for dashboard api access validated successfully");
	}

	@Test(description = "LPE-T2205 Verify the UUID Redemption Code Generation feature is working for existing businesses when flag is enabled", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void LPE_T2205_verifyTheUUIDRedemptionCodeGenerationFeatureIsWorkingForExistingBusinessesWhenFlagIsEnabled()
			throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Codes");
		// pageObj.dashboardpage().navigateToTabs("Miscellaneous URLs");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable UUID Strategy for Online Ordering Channel", "check");

		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token");
		String userID = signUpResponse.jsonPath().get("user_id").toString();

		// send three reward redeemable_id to user reward 1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, reward_id,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		String redemption_code = resp.jsonPath().get("redemption_code").toString();
		Assert.assertTrue(redemption_code.matches("[a-zA-Z0-9-]+"), "String contains invalid characters");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
