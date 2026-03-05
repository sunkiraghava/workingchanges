package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CockpitTabsValidationsTest {
	static Logger logger = LogManager.getLogger(CockpitTabsValidationsTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		timeStamp = CreateDateTime.getTimeDateString();
		userEmail = email.replace("Temp", timeStamp);
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3196 (1.0) - check the cockpit all tabs ", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3196_validateCockpitAllTabsChekbox() throws InterruptedException {
		String finalResultMessage = "";

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String menuList = dataSet.get("subMenuList");
		String[] arrMenu = menuList.split(",");
		boolean flag = true;

		for (String arrEle : arrMenu) {
			String cockpitSubMenu = arrEle;
			String checkBoxKey = cockpitSubMenu + "_count";
			int expectedCheckboxCount = Integer.parseInt(dataSet.get(checkBoxKey));

			pageObj.utils().logit("                       START -- Validation for the " + cockpitSubMenu + "             ");

			pageObj.menupage().navigateToSubMenuItem("Cockpit", cockpitSubMenu);
			pageObj.cockpitRedemptionsPage().cockpitAllCheckBoxElements("uncheck");
			pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
			pageObj.cockpitRedemptionsPage().verifyCheckboxValue("uncheck");

			pageObj.utils().logit("***************************************************");

			pageObj.cockpitRedemptionsPage().cockpitAllCheckBoxElements("check");
			pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

			int actualCheckBoxCount = pageObj.cockpitRedemptionsPage().verifyCheckboxValue("check");
			int highRange = expectedCheckboxCount + 5;
			int lowRange = expectedCheckboxCount - 5;

			try {
				Assert.assertTrue(lowRange <= actualCheckBoxCount && actualCheckBoxCount <= highRange,
						"Checkbox count " + actualCheckBoxCount + " is NOT matched for " + cockpitSubMenu);
				pageObj.utils().logPass("Checkbox count " + actualCheckBoxCount + " is matched for " + cockpitSubMenu);

			} catch (AssertionError ae) {
				finalResultMessage = finalResultMessage + "Checkbox count " + actualCheckBoxCount
						+ " is NOT matched with expected " + expectedCheckboxCount + " for " + cockpitSubMenu + "\n / ";
				logger.info("Checkbox count " + actualCheckBoxCount + " is NOT matched with expected "
						+ expectedCheckboxCount + " for " + cockpitSubMenu);
				TestListeners.extentTest.get().warning("Checkbox count " + actualCheckBoxCount
						+ " is NOT matched with expected " + expectedCheckboxCount + " for " + cockpitSubMenu);

				flag = false;
			}

			pageObj.utils().logit("                       END -- Validation for the " + cockpitSubMenu + "             ");

		}
		TestListeners.extentTest.get().info(finalResultMessage);
		Assert.assertTrue(flag, "Checkbox count not matched for some reason ");

	}

	@Test(description = "SQ-T3201 When turn on checkpoint flag from cockpit then it should display account history", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Shashank Sharma")
	public void T3201_verifyWhenEnableAccountHistoryCheckpointfordashboardFlagOnInCockpitItShouldDisplayRecentLifetimeAccountHistory()
			throws InterruptedException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Account History Checkpoint for dashboard?",
				"check");

		// Api1 User creation
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		// apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos chekin api");
		logger.info("Response time for Pos checkin api in milliseconds is :" + resp.getTime());

		// Verify checkin in guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		boolean lifetimeTab = pageObj.guestTimelinePage().lifeTimeAccountistory();
		boolean recenttab = pageObj.guestTimelinePage().recentAccountistory();

		Assert.assertTrue(lifetimeTab, "Account history lifetime tab history did not appeared");
		Assert.assertTrue(recenttab, "Account history recent tab history did not appeared");

	}

	@Test(description = "SQ-T3701 Verify redemption code is generated as per Redemption Code Strategy(7 digits) configured in Cockpit->Redemptions->Redemption codes-> Redemption Code Strategy (cloned)", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Shashank Sharma")
	public void T3701_Verify7DigitRedemptionCodeIsGeneratedIfRedemptionCodeStrategyConfigured()
			throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Business Live Now?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Codes");

		String redemptionCodeStrategyValue = pageObj.cockpitRedemptionsPage()
				.RedemptionCodeStrategySelectBoxIsEditable();
		Assert.assertEquals(redemptionCodeStrategyValue, "true", "Redemption Code Strategy is editable ");

		int expLenght = pageObj.cockpitRedemptionsPage().getRedemptionCodeStrategySelectBoxValue();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Some problem in user signup");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		// String token =
		// signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Not able to send gift to user");

		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail, "password@123");
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer("$2.0 OFF (Never Expires)");
		System.out.println(redemptionCode);

		Assert.assertEquals(redemptionCode.length(), expLenght);
		TestListeners.extentTest.get()
				.pass("The lenght of generated redemption code is matched with the expected lenght- " + expLenght);

	}

	@Test(description = "LPE-T1440/ SQ-T5050 Verify both flags i.e. Turn off Checkins & Turn off Redemptions user should be able to enable & disable flags || "
			+ "LPE-T1441/ SQ-T5051 Verify flag i.e. Turn off Checkins is enabled user should not be able to checking via IFrame || "
			+ "LPE-T1444 / T5054 Verify flag i.e. Turn off Checkins is enabled user should not be able to checking via AUTH API || "
			+ "LPE-T1439/ SQ-T5049 Verify both flags i.e. Turn off Checkins & Turn off Redemptions are visible in cockpit -->dashboard --> misc config with proper description || "
			+ "LPE-T1443/ SQ-T5053 Verify flag i.e. Turn off Checkins is enabled user should not be able to checking via API V2", groups = {
					"regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T1440_TurnOffCheckinsAndTurnOffRedemptionsApiCheck() throws Exception {

		// Navigate to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		String flag1 = pageObj.dashboardpage().checkCockpitFlagDescription("Turn off Checkins?");
		Assert.assertEquals(flag1, dataSet.get("checkinFlagDesc"),
				"Description for cockpit flag - Turn off Checkins? is not matching");
		pageObj.utils().logPass("Description for cockpit flag - Turn off Checkins? is matching");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_turn_off_checkins", "check");
		String flag2 = pageObj.dashboardpage().checkCockpitFlagDescription("Turn off Redemptions?");
		Assert.assertEquals(flag2, dataSet.get("RedemptionFlagDesc"),
				"Description for cockpit flag - Turn off Redemptions? is not matching");
		pageObj.utils().logPass("Description for cockpit flag - Turn off Redemptions? is matching");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_turn_off_redemptions", "check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		// generateBarcode
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		pageObj.utils().logPass("barcode generated successfully");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Create Loyalty Checkin by Barcode
		Response barcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token, barcode);
		Assert.assertEquals(barcodeCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Loyalty Checkin by barcode");
		boolean isApi2BarcodeCheckinTryAgainSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, barcodeCheckinResponse.asString());
		Assert.assertTrue(isApi2BarcodeCheckinTryAgainSchemaValidated,
				"Api v2 Create Loyalty Checkin by Barcode Schema Validation failed");
		Assert.assertEquals(barcodeCheckinResponse.jsonPath().getString("errors"),
				"[base:[Loyalty Program Maintenance is currently in Progress. Please try again after some time.]]");
		pageObj.utils().logPass("api2 Loyalty Checkin by barcode is giving expected error");

		// Checkin via auth API

		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, "10", dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Checkin via auth API");
		Assert.assertEquals(checkinResponse.jsonPath().get("[0]"),
				"Loyalty Program Maintenance is currently in Progress. Please try again after some time.");
		boolean isAuthCreateLoyaltyCheckinSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, checkinResponse.asString());
		Assert.assertTrue(isAuthCreateLoyaltyCheckinSchemaValidated,
				"Auth API Create Loyalty Checkin Schema Validation failed");
		pageObj.utils().logPass("Checkin via auth API is giving expected error");

	}

	@Test(description = "SQ-T3195 Cockpit Configuration API", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3195_verifyCockpitConfigurationAPI() throws InterruptedException {

		// Get Dashboard Business Config pratik@punchh.com Super Admin
		Response getDashboardBusinessConfig = pageObj.endpoints()
				.getDashboardBusinessConfig(dataSet.get("superAdminAuthorization"), dataSet.get("slugID"));
		Assert.assertEquals(getDashboardBusinessConfig.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		TestListeners.extentTest.get().pass("Dashboard API Get business config is successful");
		String val = getDashboardBusinessConfig.asString();

		// Update Dashboard Business Config
		Response updateLocationresponse = pageObj.endpoints()
				.updateDashboardBusinessConfig(dataSet.get("superAdminAuthorization"), dataSet.get("slugID"), val);
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched ");
		TestListeners.extentTest.get().pass("Dashboard API Update business config  is successful");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
