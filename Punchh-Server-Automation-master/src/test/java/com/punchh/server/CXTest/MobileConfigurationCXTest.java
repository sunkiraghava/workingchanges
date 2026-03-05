package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileConfigurationCXTest {

	private static Logger logger = LogManager.getLogger(MobileConfigurationCXTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	SeleniumUtilities selUtils;
	Utilities utils;
	public String lockedRewardsScreenredeemable = "";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		selUtils = new SeleniumUtilities(driver);
		utils = new Utilities(driver);
	}

	// Anant
	// Merged this test case's steps into
	// ValidateShowRedeemOnlineButtonInConsolidatedScanningFlag
	// @Test(description = "SQ-T3821 Validate that newly added two flags \"Display
	// legal text at signup\" & \"Require explicit acknowledgment of legal text\"
	// should be is available in the Mobile config page", priority = 0)
	public void T3821_newlyAddedTwoFlags() throws InterruptedException {

		String message = "";
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		// hint text
		String hintText1 = pageObj.mobileconfigurationPage().getHintText("Display legal text at signup");
		Assert.assertEquals(hintText1,
				"Displays legal text on the signup/login screen. Supports dynamic tags {{terms}} and {{privacy}} to link URLs from Miscellaneous URLs tab",
				"hint text is not equal for display legal text at signup");
		TestListeners.extentTest.get().pass("hint text is equal for display legal text at signup");

		String hintText2 = pageObj.mobileconfigurationPage()
				.getHintText("Require explicit acknowledgment of legal text");
		Assert.assertEquals(hintText2,
				"Enables checkbox next to legal text in the mobile app. Display legal text at signup flag must also be enabled",
				"hint text is not equal for Require explicit acknowledgment of legal text");
		TestListeners.extentTest.get().pass("hint text is equal for Require explicit acknowledgment of legal text");

		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.dashboardpage().navigateToTabs("Loyalty");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_legal_en", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("require_checkbox_in_legal_en", "uncheck");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(message, "Mobile configuration updated for Loyalty", "Mobile configuration is not updated");
		Response card1;
		int counter = 0;
		String loyaltyAppConfig;
		boolean flag = true;
		do {
			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig = card1.jsonPath().getString("[0].loyalty_app_config").toString();
			utils.longwait(1000);
			counter++;
			if (loyaltyAppConfig.equals("\"SHOW_LEGAL\":true")) {
				flag = false;
			}
		} while (flag && (counter != 20));
		loyaltyAppConfig = card1.jsonPath().getString("[0].loyalty_app_config").toString();
		Assert.assertTrue(loyaltyAppConfig.contains("\"SHOW_LEGAL\":true"),
				"when flag is ON then also incorrect value is coming");
		Assert.assertTrue(loyaltyAppConfig.contains("\"REQUIRE_CHECKBOX_IN_LEGAL\":false"));
		utils.logPass("Verfied SHOW_LEGAL and REQUIRE_CHECKBOX_IN_LEGAL value is updated in the API");

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_legal_en", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("require_checkbox_in_legal_en", "check");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(message, "Mobile configuration updated for Loyalty", "Mobile configuration is not updated");

		Response card2;
		int counter2 = 0;
		String loyaltyAppConfig2;
		boolean flag2 = true;
		do {
			card2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card2.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config").toString();
			utils.longwait(1000);
			counter2++;
			if (loyaltyAppConfig2.equals("\"SHOW_LEGAL\":true")) {
				flag2 = false;
			}
		} while (flag2 && (counter2 != 20));
		loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config").toString();
		Assert.assertTrue(loyaltyAppConfig2.contains("\"SHOW_LEGAL\":false"),
				"when flag is OFF then also incorrect value is coming");
		Assert.assertTrue(loyaltyAppConfig2.contains("\"REQUIRE_CHECKBOX_IN_LEGAL\":true"));

		utils.logPass("Verfied SHOW_LEGAL and REQUIRE_CHECKBOX_IN_LEGAL value is updated in the API");
	}

	// Anant
	// Merged this test case's steps into
	// ValidateShowRedeemOnlineButtonInConsolidatedScanningFlag
	// @Test(description = "SQ-T3822 Validate that newly added \"Display onboarding
	// tutorial\" flag is available in the Mobile config page", priority = 1)
	public void T3822_newFieldDisplayOnboardingTutorial() throws InterruptedException {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		// hint text
		String hintText1 = pageObj.mobileconfigurationPage().getHintText("Display onboarding tutorial");
		Assert.assertEquals(hintText1, "Displays tutorial carousel during the welcome flow when user is logged out",
				"hint text is not equal for display legal text at signup");
		TestListeners.extentTest.get().pass("hint text is equal for Display onboarding tutorial field");

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_onboarding_tutorial_en", "uncheck");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		int counter = 0;
		String loyaltyAppConfig;
		String loyaltyAppConfigResponse;
		Response card1;
		boolean flag = true;
		do {
			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig = card1.jsonPath().getString("[0].loyalty_app_config").toString();
			JSONObject jsonObject = new JSONObject(loyaltyAppConfig);
			loyaltyAppConfigResponse = jsonObject.get("SHOW_ONBOARDING_TUTORIAL").toString();
			Thread.sleep(1000);
			counter++;
			if (loyaltyAppConfigResponse.equals("false")) {
				flag = false;
			}
		} while (flag && (counter != 80));
		Assert.assertTrue(loyaltyAppConfig.contains("\"SHOW_ONBOARDING_TUTORIAL\":false"),
				"when flag is OFF then also incorrect value is coming");

		utils.logPass(
				"verified when Display onboarding tutorial field is unchecked then SHOW_ONBOARDING_TUTORIAL key response is false");

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_onboarding_tutorial_en", "check");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		int counter2 = 0;
		String loyaltyAppConfig2;
		String loyaltyAppConfigResponse2;
		Response card2;
		boolean flag2 = true;
		do {
			card2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card2.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config").toString();
			JSONObject jsonObject2 = new JSONObject(loyaltyAppConfig2);
			loyaltyAppConfigResponse2 = jsonObject2.get("SHOW_ONBOARDING_TUTORIAL").toString();
			Thread.sleep(1000);
			counter2++;
			if (loyaltyAppConfigResponse2.equals("true")) {
				flag2 = false;
			}
		} while (flag2 && (counter2 != 80));
		loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config").toString();
		Assert.assertTrue(loyaltyAppConfig2.contains("\"SHOW_ONBOARDING_TUTORIAL\":true"),
				"when flag is ON then also incorrect value is coming");

		utils.logPass(
				"verified when Display onboarding tutorial field is checked then SHOW_ONBOARDING_TUTORIAL key response is True");

		pageObj.posIntegrationPage().clickAuditLog();
		String auditVal1 = pageObj.posIntegrationPage().auditLogValue("Loyalty App Config");
		Assert.assertEquals(auditVal1, "MobileConfiguration", "updated value is not display in the logs");
		utils.logPass("Verified update value is displayed in the logs");
	}

	// Merged this test case's steps into
	// ValidateShowRedeemOnlineButtonInConsolidatedScanningFlag
	// @Test(description = "SQ-T3823 Validate that newly added flag \"Display future
	// Challenges\" is available in the Mobile config page", priority = 2)
	public void T3823_newFieldDisplayFutureChallenges() throws InterruptedException {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		// // hint text
		String hintText1 = pageObj.mobileconfigurationPage().getHintText("Display future Challenges");
		Assert.assertEquals(hintText1,
				"Displays Challenge campaigns that are configured with a start date in the future",
				"hint text is not equal for Display onboarding tutorial");
		TestListeners.extentTest.get().pass("hint text is equal for Display onboarding tutorial field");

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_future_challenges_en", "uncheck");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		int counter = 0;
		String loyaltyAppConfig;
		String loyaltyAppConfigResponse;
		Response card1;
		boolean flag = true;
		do {
			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig = card1.jsonPath().getString("[0].loyalty_app_config").toString();
			JSONObject jsonObject = new JSONObject(loyaltyAppConfig);
			loyaltyAppConfigResponse = jsonObject.get("SHOW_FUTURE_CHALLENGES").toString();
			// System.out.println(loyaltyAppConfigResponse);
			Thread.sleep(1000);
			counter++;
			if (loyaltyAppConfigResponse.equals("false")) {
				flag = false;
			}
		} while (flag && (counter != 80));

		Assert.assertTrue(loyaltyAppConfig.contains("\"SHOW_FUTURE_CHALLENGES\":false"),
				"when flag is OFF then also incorrect value is coming");

		utils.logPass(
				"Verified when Display future Challenges is unchecked then SHOW_FUTURE_CHALLENGES key response is false");

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Loyalty");

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("show_future_challenges_en", "check");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		int counter2 = 0;
		String loyaltyAppConfig2;
		String loyaltyAppConfigResponse2;
		Response card2;
		boolean flag2 = true;
		do {
			card2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card2.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config").toString();
			JSONObject jsonObject2 = new JSONObject(loyaltyAppConfig2);
			loyaltyAppConfigResponse2 = jsonObject2.get("SHOW_FUTURE_CHALLENGES").toString();
			Thread.sleep(1000);
			counter2++;
			if (loyaltyAppConfigResponse2.equals("true")) {
				flag2 = false;
			}
		} while (flag2 && (counter2 != 80));

		Assert.assertTrue(loyaltyAppConfig2.contains("\"SHOW_FUTURE_CHALLENGES\":true"),
				"when flag is ON then also incorrect value is coming");

		utils.logPass(
				"verified when Display future Challenges field is checked then SHOW_FUTURE_CHALLENGES key response is True");

		pageObj.posIntegrationPage().clickAuditLog();
		String auditVal1 = pageObj.posIntegrationPage().auditLogValue("Loyalty App Config");
		Assert.assertEquals(auditVal1, "MobileConfiguration", "updated value is not display in the logs");
		utils.logPass("Verified update value is displayed in the logs");
	}

	// Amit
	@Test(description = "SQ-T4455 To validate the \"Locked rewards\" screen and functionalities", priority = 4)
	@Owner(name = "Amit Kumar")
	public void T4455_validateLockedRewardsScreenPartTwo() throws InterruptedException {
		// Part two
		lockedRewardsScreenredeemable = "T4455_AutoRedeemable" + CreateDateTime.getTimeDateString();
		// create new redeemable having 'Should be available as loyalty points based
		// redemption?' flag enable
		// iFrame Configuration :Redeemable tab enable Show Available Redeemable for
		// loyalty card redemption?,Show Reward redemption code applicability
		// description?
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// no need to create new redeemable as already many available
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		 * pageObj.redeemablePage().createRedeemable(lockedRewardsScreenredeemable);
		 * pageObj.redeemablePage().selectRecieptRule(dataSet.get("points"));
		 * pageObj.redeemablePage().enableLoyaltyFlagAndCreateRedeemable(dataSet.get(
		 * "pointsToRedeem1")); logger.
		 * info("created a new reedemable which can be redeem by a user as it have the less points"
		 * ); TestListeners.extentTest.get()
		 * .info("created a new reedemable which can be redeem by a user as it have the less points"
		 * );
		 */
		// create a new user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser("", "", dataSet.get("gifttype"),
				dataSet.get("pointsToGiftUser"), dataSet.get("reason"));

		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		pageObj.iframeSingUpPage().clickRedeemReward();

		boolean visible = pageObj.iframeSingUpPage().viewMoreVisible();
		Assert.assertTrue(visible, "view more button is visible");

		utils.logPass("Verified when the locked redeemable count is more than  5 then  View More button is  visible");

		// clicked on view more
		pageObj.iframeSingUpPage().clickViewMoreButton();
		int size = pageObj.iframeSingUpPage().getLockedRedeemableList();

		Assert.assertTrue(size > 5, "all the locked redeemable are not present when clicked on view more button");
		utils.logPass("Verified all the locked redeemable are present when clicked on view more button");

		// redeem an unlocked reward
		List<String> ele = pageObj.iframeSingUpPage().redeemUnlockedReward(dataSet.get("redeemableName"));
		Assert.assertEquals(ele.get(0), dataSet.get("successMsg"), "Error Message is not equal");
		Assert.assertEquals(ele.get(1), dataSet.get("errorHexCode"), "Error message color is not same");

		utils.logPass("Verified able to redeem the unlocked redeemable");

		// switch to parent window
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 * pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		 * pageObj.redeemablePage().deleteRedeemable(lockedRewardsScreenredeemable);
		 */
	}

	// Anant
	@Test(description = "SQ-T4507 Validate that when user changed point unblock Redeemables to other types (eg. point based) in the Loyalty Program Type field."
			+ "SQ-T4508 (1.0) Verify behaving of Loyalty Program Type field & iFrame, while selecting option other than Points unlock redeemables program type.", priority = 5)
	@Owner(name = "Vansham Mishra")
	public void T4507_validateUserChangePointUnblockRedeemable() throws InterruptedException {
		String redeemableName = "AutoRedeemable" + CreateDateTime.getTimeDateString();
		// String redeemableName2 = "AutoRedeemable2" +
		// CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable the iframe
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_iframe", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// nagivate to cockpit earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");

		// check all the earning types should visible
		List<String> earningTypeLst = pageObj.earningPage().loyaltyProgramTypeLst();
		Assert.assertTrue(earningTypeLst.contains(dataSet.get("earningType")),
				dataSet.get("earningType") + " value is not present");
		Assert.assertTrue(earningTypeLst.contains(dataSet.get("earningType2")),
				dataSet.get("earningType2") + " value is not present");
		Assert.assertTrue(earningTypeLst.contains(dataSet.get("earningType3")),
				dataSet.get("earningType3") + " value is not present");
		Assert.assertTrue(earningTypeLst.contains(dataSet.get("earningType4")),
				dataSet.get("earningType4") + " value is not present");
		Assert.assertTrue(earningTypeLst.contains(dataSet.get("earningType5")),
				dataSet.get("earningType5") + " value is not present");

		utils.logPass("Verfied all the value present in the loyalty Program Type");

		// nagivate to cockpit earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");

		// select the earning type
		pageObj.earningPage().selectLoyaltyProgramType(dataSet.get("earningType"));
		pageObj.earningPage().clickUpdateBtn();

		// create a new user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser("", "Automation", dataSet.get("gifttype"),
				dataSet.get("pointsToGiftUser"), dataSet.get("reason"));

		// create new redeemables having 'Should be available as loyalty points based
		// redemption?' flag enable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule(dataSet.get("points"));
		pageObj.redeemablePage().enableLoyaltyFlagAndCreateRedeemable(dataSet.get("pointsToRedeem1"));

		utils.logit("created a new reedemable which can be redeem by a user as it have the less points");

		// switch to child window
		String parentWindow = driver.getWindowHandle();
		utils.createNewWindowAndSwitch(parentWindow);
		utils.longWaitInSeconds(4);

		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginUsingWindowIndex(userEmail, prop.getProperty("iFramePassword"), 2);
		pageObj.iframeSingUpPage().clickRedeemReward();

		boolean val = pageObj.iframeSingUpPage().pointsVisible(dataSet.get("pointsName"),
				dataSet.get("pointsToGiftUser"));
		Assert.assertTrue(val, dataSet.get("pointsName") + " is not visible");
		utils.logPass("Verified " + dataSet.get("pointsName") + " is visible");

		val = pageObj.iframeSingUpPage().pointsVisible(dataSet.get("pointsName2"), dataSet.get("pointsToGiftUser"));
		Assert.assertTrue(val, dataSet.get("pointsName2") + " is not visible");
		utils.logPass("Verified " + dataSet.get("pointsName2") + " is visible");

		// switch to parent window
		utils.switchToWindowByIndex(driver, 0);

		// try to change the earning type
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");
		pageObj.earningPage().selectLoyaltyProgramType(dataSet.get("earningType2"));
		pageObj.earningPage().clickUpdateBtn();

		List<String> ele = pageObj.earningPage().getErrorOrSuccessMsg();
		Assert.assertEquals(ele.get(0), dataSet.get("errorMsg2"), "Error Message is not equal");
		Assert.assertEquals(ele.get(1), dataSet.get("errorHexCode2"), "Error message color is not same");

		utils.logPass("Verified not able to change the earning type as expected and getting the error msg");

		// edit the created redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().clickRedeemable(redeemableName);
		pageObj.redeemablePage().offLoyaltyFlagInRedeemable();

		// switch to child window
		utils.switchToWindowByIndex(driver, 1);
		boolean visible = pageObj.iframeSingUpPage().unlockedRewardVisible(redeemableName);
		Assert.assertFalse(visible, "redeemable is visible when the flag is off");
		utils.logPass("Verfied redeemable is not visible when flag is off");

		for (int i = 2; i < 6; i++) {
			utils.switchToWindowByIndex(driver, 0);
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
			pageObj.dashboardpage().navigateToTabs("Checkin Earning");
			pageObj.earningPage().selectLoyaltyProgramType(dataSet.get("earningType" + i));
			pageObj.earningPage().clickUpdateBtn();

			utils.switchToWindowByIndex(driver, 1);
			visible = pageObj.iframeSingUpPage().unlockedRewardVisible(redeemableName);
			Assert.assertFalse(visible, "redeemable is visible when the earning type is "
					+ dataSet.get("earningType" + i) + " is selected");
			utils.logPass("Verified redeemable is not visible when the earning type is "
					+ dataSet.get("earningType" + i) + " is selected");
		}

		// delete the redeemable
		utils.switchToWindowByIndex(driver, 0);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	// Anant
	@Test(description = "SQ-T4536 Validate that Mobile Configuration, Marketing Message, Redeemables, Iframe configurations, under Settings > Translations", priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T4536_ValidateFieldsUnderSettingsTranslation() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		for (int i = 1; i < 5; i++) {
			pageObj.menupage().navigateToSubMenuItem("Settings", "Translations");
			pageObj.mobileconfigurationPage().checkColorInTranslation(dataSet.get("field" + i), dataSet.get("hexcode"));
			pageObj.mobileconfigurationPage().clickFieldInTranslation(dataSet.get("field" + i));

			int fieldExpectedCount = Integer.parseInt(dataSet.get("fieldCount" + i));
			pageObj.mobileconfigurationPage().checkRequiredFieldsInTranslation(fieldExpectedCount,
					dataSet.get("field" + i));

			pageObj.mobileconfigurationPage().enterValueInTranslationField(dataSet.get("value"),
					dataSet.get("input" + i));
			pageObj.mobileconfigurationPage().clickUpdateButton();
			String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
			Assert.assertEquals(msg, dataSet.get("expectedMsg"), "updated msg is not equal");
			utils.logit("Verified field updated successfully");

			pageObj.guestTimelinePage().clickAuditLog();
			boolean bol = pageObj.mobileconfigurationPage().checkTranslationAuditLogs(dataSet.get("input" + i),
					dataSet.get("value"));
			Assert.assertTrue(bol, "audits log not updated");
			utils.logit("Verified audits log updated successfully");

			pageObj.menupage().navigateToSubMenuItem("Settings", "Translations");
			pageObj.mobileconfigurationPage().checkColorInTranslation(dataSet.get("field" + i), dataSet.get("hexcode"));
			pageObj.mobileconfigurationPage().clickFieldInTranslation(dataSet.get("field" + i));
			pageObj.mobileconfigurationPage().enterValueInTranslationField("", dataSet.get("input" + i));
			pageObj.mobileconfigurationPage().clickUpdateButton();
		}
	}

	@Test(description = "SQ-T5463: Validate that functionality and behavior of the newly added Allow Gift Card Payments in SSF checkbox under Loyalty tab; "
			+ "SQ-T5446: Validate that functionality and behavior 'Hide gift card payment in SSF' checkbox; "
			+ "SQ-T2731: Validate the Meta API response for time zone field under location tab", groups = {
					"regression", "dailyrun" }, priority = 7)
	@Owner(name = "Vaibhav Agnihotri")
	public void ValidateAllowGiftCardPaymentsinSSF_Flag() throws Exception {

		String locationName = dataSet.get("locationName");
		String locationId = dataSet.get("locationId");
		String locationTimezoneApi = dataSet.get("locationTimezoneApi");
		String location2Name = dataSet.get("location2Name");
		String location2Id = dataSet.get("location2Id");
		String location2TimezoneApi = dataSet.get("location2TimezoneApi");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard > Miscellaneous Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable meta cache update on request", "check");

		/*
		 * Go to Whitelabel > Mobile Configuration > Loyalty tab. Uncheck 'Allow Gift
		 * Card Payments in SSF' flag.
		 */
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		utils.checkUncheckFlag("Allow Gift Card Payments in SSF", "uncheck");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		// Meta v1 API response validations
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String allow_ssf_gift_card = cardsResponse.jsonPath().get("[0].allow_ssf_gift_card").toString();
		Assert.assertEquals(allow_ssf_gift_card, "false", "allow_ssf_gift_card value not matched");

		// Meta v2 API response validations
		Response cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String allow_ssf_gift_card2 = cardsResponse2.jsonPath().get("allow_ssf_gift_card").toString();
		Assert.assertEquals(allow_ssf_gift_card2, "false", "allow_ssf_gift_card value not matched");
		utils.logPass("Verified in v1 and v2 Meta API response: allow_ssf_gift_card is having value false.");

		/*
		 * Go to Settings > Locations > Search and open location 1. Navigate to Mobile
		 * App tab. Verify that 'Hide gift card payment in SSF' flag is not present.
		 */
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(locationName);
		pageObj.dashboardpage().navigateToTabs("Mobile App");
		boolean isHideGcPaymentInSsfPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.hideGcPaymentInSsf"));
		Assert.assertFalse(isHideGcPaymentInSsfPresent, "Hide gift card payment in SSF flag is present.");
		utils.logPass("Verified that Hide gift card payment in SSF flag is not present in Mobile App tab.");

		/*
		 * Go back to Whitelabel > Mobile Configuration > Loyalty tab. Check 'Allow Gift
		 * Card Payments in SSF' flag.
		 */
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		utils.checkUncheckFlag("Allow Gift Card Payments in SSF", "check");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(msg, "Mobile configuration updated for Loyalty",
				"success message for Loyalty tab update did not matched");

		/*
		 * Go to Settings > Locations > Search and open the location 1. Navigate to
		 * Mobile App tab. Uncheck the 'Hide gift card payment in SSF' flag.
		 */
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(locationName);
		pageObj.dashboardpage().navigateToTabs("Mobile App");
		utils.checkUncheckFlag("Hide gift card payment in SSF", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Verify the updates in Audit log on Locations page
		pageObj.guestTimelinePage().clickAuditLog();
		String hideGcPaymentInSsfAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogs("Hide Ssf Gift Card", "");
		Assert.assertEquals(hideGcPaymentInSsfAuditLogValue, "false", "'Hide Ssf Gift Card' value not matched");
		utils.logPass("Verified 'Hide gift card payment in SSF' as false in Audit log.");
		utils.navigateBackPage();

		// Meta v1 API response validations
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		allow_ssf_gift_card = cardsResponse.jsonPath().get("[0].allow_ssf_gift_card").toString();
		Assert.assertEquals(allow_ssf_gift_card, "true", "allow_ssf_gift_card value not matched");
		String hide_ssf_gift_card = utils.extractValueFromJsonArray(cardsResponse, "locations", "location_id",
				locationId, "location_extra", "hide_ssf_gift_card");
		Assert.assertEquals(hide_ssf_gift_card, "false", "hide_ssf_gift_card value not matched");

		// Meta v2 API response validations
		cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		allow_ssf_gift_card2 = cardsResponse2.jsonPath().get("allow_ssf_gift_card").toString();
		Assert.assertEquals(allow_ssf_gift_card2, "true", "allow_ssf_gift_card value not matched");
		String hide_ssf_gift_card2 = utils.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(cardsResponse2,
				"locations", "location_id", locationId, "hide_ssf_gift_card");
		Assert.assertEquals(hide_ssf_gift_card2, "false", "hide_ssf_gift_card value not matched");
		utils.logPass("Verified in v1 and v2 Meta API response: 1) allow_ssf_gift_card is having value true. "
				+ "2) hide_ssf_gift_card is having value false for location_id '" + locationId + "'.");

		/*
		 * Go to Settings > Locations > Search and open the location 1. Update the
		 * timezone. Navigate to Mobile App tab. Check the 'Hide gift card payment in
		 * SSF' flag.
		 */
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(locationName);
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.locationTimezone"), dataSet.get("locationTimezoneUi"));
		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Mobile App");
		utils.checkUncheckFlag("Hide gift card payment in SSF", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Verify the updates in Audit log on Locations page
		pageObj.guestTimelinePage().clickAuditLog();
		hideGcPaymentInSsfAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogs("Hide Ssf Gift Card", "");
		Assert.assertEquals(hideGcPaymentInSsfAuditLogValue, "true", "'Hide Ssf Gift Card' value not matched");
		utils.logPass("Verified 'Hide gift card payment in SSF' as true in Audit log.");
		utils.navigateBackPage();

		// Open location 2 and update its timezone as well
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(location2Name);
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.locationTimezone"), dataSet.get("location2TimezoneUi"));
		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();

		// Meta v1 API response validations
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		hide_ssf_gift_card = utils.extractValueFromJsonArray(cardsResponse, "locations", "location_id", locationId,
				"location_extra", "hide_ssf_gift_card");
		Assert.assertEquals(hide_ssf_gift_card, "true", "hide_ssf_gift_card value not matched");
		String locationTimezone = utils.extractValueFromJsonArray(cardsResponse, "locations", "location_id", locationId,
				"time_zone", "");
		Assert.assertEquals(locationTimezone, locationTimezoneApi, "time_zone value not matched");
		String location2Timezone = utils.extractValueFromJsonArray(cardsResponse, "locations", "location_id",
				location2Id, "time_zone", "");
		Assert.assertEquals(location2Timezone, location2TimezoneApi, "time_zone value not matched");

		// Meta v2 API response validations
		cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		hide_ssf_gift_card2 = utils.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(cardsResponse2,
				"locations", "location_id", locationId, "hide_ssf_gift_card");
		Assert.assertEquals(hide_ssf_gift_card2, "true", "hide_ssf_gift_card value not matched");
		String locationTimeone = utils.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(cardsResponse2,
				"locations", "location_id", locationId, "time_zone");
		Assert.assertEquals(locationTimeone, locationTimezoneApi, "time_zone value not matched");
		String location2Timeone = utils.getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(cardsResponse2,
				"locations", "location_id", location2Id, "time_zone");
		Assert.assertEquals(location2Timeone, location2TimezoneApi, "time_zone value not matched");
		utils.logPass(
				"Verified in v1 and v2 Meta API response: 1) hide_ssf_gift_card is having value true for location_id "
						+ locationId + "; 2) time_zone is having value " + locationTimezoneApi + " for location_id "
						+ locationId + " and " + location2TimezoneApi + " for location_id " + location2Id);

	}

	@Test(description = "SQ-T6877: To validate platform configuration to enable \"Redeem Online\" in Consolidated scan flows; "
			+ "SQ-T3821: Validate that newly added two flags \"Display legal text at signup\" & \"Require explicit acknowledgment of legal text\" should be is available in the Mobile config page; "
			+ "SQ-T3822: Validate that newly added \"Display onboarding tutorial\" flag is available in the Mobile config page; "
			+ "SQ-T3823 Validate that newly added flag \"Display future Challenges\" is available in the Mobile config page", groups = {
					"regression" }, priority = 8)
	@Owner(name = "Vansham Mishra")
	public void ValidateShowRedeemOnlineButtonInConsolidatedScanningFlag() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		utils.checkUncheckFlag("Show Redeem Online Button in Consolidated Scanning", "uncheck");
		utils.checkUncheckFlag("Display onboarding tutorial", "uncheck");
		utils.checkUncheckFlag("Display legal text at signup", "check");
		utils.checkUncheckFlag("Require explicit acknowledgment of legal text", "uncheck");
		utils.checkUncheckFlag("Display future Challenges", "uncheck");
		pageObj.mobileconfigurationPage().clickUpdateBtn();

		// Verify hint texts
		String hintText1 = pageObj.mobileconfigurationPage().getHintText("Display onboarding tutorial");
		Assert.assertEquals(hintText1, "Displays tutorial carousel during the welcome flow when user is logged out",
				"hint text is not equal for display legal text at signup");
		utils.logit("pass", "Hint text is matching for Display onboarding tutorial field");
		String hintText2 = pageObj.mobileconfigurationPage().getHintText("Display legal text at signup");
		Assert.assertEquals(hintText2,
				"Displays legal text on the signup/login screen. Supports dynamic tags {{terms}} and {{privacy}} to link URLs from Miscellaneous URLs tab",
				"hint text is not equal for display legal text at signup");
		utils.logit("pass", "Hint text is matching for display legal text at signup");
		String hintText3 = pageObj.mobileconfigurationPage()
				.getHintText("Require explicit acknowledgment of legal text");
		Assert.assertEquals(hintText3,
				"Enables checkbox next to legal text in the mobile app. Display legal text at signup flag must also be enabled",
				"hint text is not equal for Require explicit acknowledgment of legal text");
		utils.logit("pass", "Hint text is matching for Require explicit acknowledgment of legal text field");
		String hintText4 = pageObj.mobileconfigurationPage().getHintText("Display future Challenges");
		Assert.assertEquals(hintText4,
				"Displays Challenge campaigns that are configured with a start date in the future",
				"hint text is not equal for Display onboarding tutorial");
		utils.logit("pass", "Hint text is matching for Display future Challenges field");

		// Hit Meta API1 and verify the updated values
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String loyaltyAppConfig = metaApiResponse.jsonPath().getString("[0].loyalty_app_config");
		JSONObject loyaltyAppConfigJsonObject = new JSONObject(loyaltyAppConfig);
		String showOnboardingTutorial = loyaltyAppConfigJsonObject.get("SHOW_ONBOARDING_TUTORIAL").toString();
		String redeemOnlineBtn = loyaltyAppConfigJsonObject.optString("SHOW_REDEEM_ONLINE_BTN_ON_REWARD_DETAIL");
		String showLegal = loyaltyAppConfigJsonObject.get("SHOW_LEGAL").toString();
		String requireCheckboxInLegal = loyaltyAppConfigJsonObject.get("REQUIRE_CHECKBOX_IN_LEGAL").toString();
		String showFutureChallenges = loyaltyAppConfigJsonObject.get("SHOW_FUTURE_CHALLENGES").toString();
		Assert.assertEquals(showOnboardingTutorial, "false");
		Assert.assertEquals(redeemOnlineBtn, "false");
		Assert.assertEquals(showLegal, "true");
		Assert.assertEquals(requireCheckboxInLegal, "false");
		Assert.assertEquals(showFutureChallenges, "false");
		utils.logit("pass", "Verified the respected values in meta API1 response");

		utils.checkUncheckFlag("Show Redeem Online Button in Consolidated Scanning", "check");
		utils.checkUncheckFlag("Display onboarding tutorial", "check");
		utils.checkUncheckFlag("Display legal text at signup", "uncheck");
		utils.checkUncheckFlag("Require explicit acknowledgment of legal text", "check");
		utils.checkUncheckFlag("Display future Challenges", "check");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(msg, "Mobile configuration updated for Loyalty",
				"success message for Loyalty tab update did not matched");

		// Hit Meta API1 again and verify the updated values
		Response metaApiResponse2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse2.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String loyaltyAppConfig2 = metaApiResponse2.jsonPath().getString("[0].loyalty_app_config");
		JSONObject loyaltyAppConfigJsonObject2 = new JSONObject(loyaltyAppConfig2);
		String redeemOnlineBtn2 = loyaltyAppConfigJsonObject2.get("SHOW_REDEEM_ONLINE_BTN_ON_REWARD_DETAIL").toString();
		String showOnboardingTutorial2 = loyaltyAppConfigJsonObject2.get("SHOW_ONBOARDING_TUTORIAL").toString();
		String showLegal2 = loyaltyAppConfigJsonObject2.get("SHOW_LEGAL").toString();
		String requireCheckboxInLegal2 = loyaltyAppConfigJsonObject2.get("REQUIRE_CHECKBOX_IN_LEGAL").toString();
		String showFutureChallenges2 = loyaltyAppConfigJsonObject2.get("SHOW_FUTURE_CHALLENGES").toString();
		Assert.assertEquals(showOnboardingTutorial2, "true");
		Assert.assertEquals(redeemOnlineBtn2, "true");
		Assert.assertEquals(showLegal2, "false");
		Assert.assertEquals(requireCheckboxInLegal2, "true");
		Assert.assertEquals(showFutureChallenges2, "true");
		utils.logit("pass", "Verified the updated values in meta API1 response");

		pageObj.posIntegrationPage().clickAuditLog();
		String auditVal1 = pageObj.posIntegrationPage().auditLogValue("Loyalty App Config");
		Assert.assertEquals(auditVal1, "MobileConfiguration", "updated value is not display in the logs");
		utils.logit("pass", "Verified update value is displayed in the logs");
	}

	// Raja
	@Test(description = "SQ-T6878: Verify the platform Configurations for Disabling Category/Product/Modifier Images; "
			+ "SQ-T5529: Validate that newly added “Disable Guest Ordering” flag under Whitelabel → Mobile Configuration → “Ordering” tab."
			+ "SQ-T7005: Validate platform Configuration for Olo Marketing Opt-In Checkbox for Guest Orders", groups = {
					"regression" }, priority = 9)
	@Owner(name = "Rajasekhar Reddy")
	public void ValidateCategory_Product_ModifierImagesFlags() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Ordering");
		utils.checkUncheckFlag("Disable Category Images", "uncheck");
		utils.checkUncheckFlag("Disable Product Images", "uncheck");
		utils.checkUncheckFlag("Disable Modifier Images", "uncheck");
		utils.checkUncheckFlag("Disable Guest (Non-Loyalty) Ordering", "uncheck");
		utils.checkUncheckFlag("Collect Marketing Opt-In for Guest (Non-Loyalty) Orders", "uncheck");

		pageObj.mobileconfigurationPage().clickUpdateBtn();

		// Hit Meta API1 and verify the updated values
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String orderingAppConfig = metaApiResponse.jsonPath().getString("[0].ordering_app_config");
		JSONObject orderingAppConfigJsonObject = new JSONObject(orderingAppConfig);
		String disableCategoryImages = orderingAppConfigJsonObject.get("DISABLE_CATEGORY_IMAGES").toString();
		String disableProductImages = orderingAppConfigJsonObject.optString("DISABLE_PRODUCT_IMAGES");
		String disableModifierImages = orderingAppConfigJsonObject.get("DISABLE_MODIFIER_IMAGES").toString();
		String disableGuestOrdering = orderingAppConfigJsonObject.get("DISABLE_GUEST_ORDERING").toString();
		String marketingOptInforGuestOrders = orderingAppConfigJsonObject.get("MARKETING_OPTIN_FOR_GUEST_ORDERS")
				.toString();
		Assert.assertEquals(disableCategoryImages, "false");
		Assert.assertEquals(disableProductImages, "false");
		Assert.assertEquals(disableModifierImages, "false");
		Assert.assertEquals(disableGuestOrdering, "false");
		Assert.assertEquals(marketingOptInforGuestOrders, "false");
		utils.logit("pass", "Verified the respected values in meta API1 response");

		utils.checkUncheckFlag("Disable Category Images", "check");
		utils.checkUncheckFlag("Disable Product Images", "check");
		utils.checkUncheckFlag("Disable Modifier Images", "check");
		utils.checkUncheckFlag("Disable Guest (Non-Loyalty) Ordering", "check");
		utils.checkUncheckFlag("Collect Marketing Opt-In for Guest (Non-Loyalty) Orders", "check");

		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(msg, "Mobile configuration updated for Ordering",
				"success message for Ordering tab update did not matched");

		// Hit Meta API1 again and verify the updated values
		Response metaApiResponse2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse2.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String orderingAppConfig2 = metaApiResponse2.jsonPath().getString("[0].ordering_app_config");
		JSONObject orderingAppConfigJsonObject2 = new JSONObject(orderingAppConfig2);
		String disableCategoryImages2 = orderingAppConfigJsonObject2.get("DISABLE_CATEGORY_IMAGES").toString();
		String disableProductImages2 = orderingAppConfigJsonObject2.get("DISABLE_PRODUCT_IMAGES").toString();
		String disableModifierImages2 = orderingAppConfigJsonObject2.get("DISABLE_MODIFIER_IMAGES").toString();
		String disableGuestOrdering2 = orderingAppConfigJsonObject2.get("DISABLE_GUEST_ORDERING").toString();
		String marketingOptInforGuestOrders2 = orderingAppConfigJsonObject2.get("MARKETING_OPTIN_FOR_GUEST_ORDERS")
				.toString();
		Assert.assertEquals(disableCategoryImages2, "true");
		Assert.assertEquals(disableProductImages2, "true");
		Assert.assertEquals(disableModifierImages2, "true");
		Assert.assertEquals(disableGuestOrdering2, "true");
		Assert.assertEquals(marketingOptInforGuestOrders2, "true");
		utils.logit("pass", "Verified the updated values in meta API1 response");
	}

	// Raja
	// Merged this test case's steps into
	// ValidateCategory_Product_ModifierImagesFlags
	// @Test(description = "SQ-T5529: Validate that newly added “Disable Guest
	// Ordering” flag under Whitelabel → Mobile Configuration → “Ordering” tab.",
	// groups = {"Regression" }, priority = 10)
	public void ValidateDisableGuestOrderingFlag() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Ordering");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Disable Guest (Non-Loyalty) Ordering",
				"uncheck");

		// Meta v1 api response validations
		int counter = 0;
		String orderingappconfig;
		String orderingappconfigResponse;
		Response card1;
		boolean flag = true;
		do {
			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			orderingappconfig = card1.jsonPath().getString("[0].ordering_app_config").toString();
			JSONObject jsonObject = new JSONObject(orderingappconfig);
			orderingappconfigResponse = jsonObject.get("DISABLE_GUEST_ORDERING").toString();
			Thread.sleep(1000);
			counter++;
			if (orderingappconfigResponse.equals("false")) {
				flag = false;
			}
		} while (flag && (counter != 50));
		Assert.assertTrue(orderingappconfig.contains("\"DISABLE_GUEST_ORDERING\":false"),
				"when flag is OFF then also incorrect value is coming");

		utils.logPass(
				"verified when Disable Guest Ordering Flag was unchecked then DISABLE_GUEST_ORDERING Flag key response was false in V1 meta API ");

		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Disable Guest (Non-Loyalty) Ordering", "check");

		int counter1 = 0;
		String orderingappconfig1;
		String orderingappconfigResponse1;
		Response card;
		boolean flag1 = true;
		do {
			card = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			orderingappconfig1 = card.jsonPath().getString("[0].ordering_app_config").toString();
			JSONObject jsonObject = new JSONObject(orderingappconfig1);
			orderingappconfigResponse1 = jsonObject.get("DISABLE_GUEST_ORDERING").toString();
			Thread.sleep(1000);
			counter++;
			if (orderingappconfigResponse1.equals("false")) {
				flag = false;
			}
		} while (flag && (counter != 50));
		Assert.assertTrue(orderingappconfig1.contains("\"DISABLE_GUEST_ORDERING\":true"),
				"when flag is ON then also incorrect value is coming");

		utils.logPass(
				"verified when Disable Guest Ordering Flag was checked then DISABLE_GUEST_ORDERING Flag key response was True in V1 meta API ");

	}

	// By Ajeet

	@Test(description = "SQ-T6241 Validate the newly added SMS Privacy Policy URL field , SMS Terms"
			+ " and Conditions URL field and updated “Privacy URL”Validate_T6256_newly added flags SHOW_MAP_Toolbar and SHOW_GET_DIRECTIoN", groups = {
					"regression", "dailyrun" }, enabled = true, priority = 11)
	@Owner(name = "Rajasekhar Reddy")
	public void T6241_T6256_ValidateNewlyAddedFieldSmsPrivacyPolicyAndSmsTermsAndConditionAndSmsPrivacyAndSHowMapToolBarAndShowgetDirectionFlags()
			throws InterruptedException {
		String message = "";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to the subMenu item
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().navigateToTab("Miscellaneous URLs");
		pageObj.mobileconfigurationPage().clickMiscellaneousURLBtn();
		String miscellaneousURLDetails = pageObj.mobileconfigurationPage().getPageSource();
		SoftAssert softAssert = new SoftAssert();
		// verify newly urls fields are added or not "SMS Privacy Policy
		// URL","SMS Privacy Policy URL","Privacy Policy URL"
		softAssert.assertTrue(miscellaneousURLDetails.contains("Privacy Policy URL"),
				"Privacy Policy URL is not present on page");
		softAssert.assertTrue(miscellaneousURLDetails.contains("SMS Privacy Policy URL"),
				"SMS Privacy Policy URL is not present on page");
		softAssert.assertTrue(miscellaneousURLDetails.contains("SMS Terms and Conditions URL"),
				"SMS Terms and Conditions URLis not present on page");
		// Meta API v1 validation for whithout Urls field value for "SMS Privacy Policy
		// URL","SMS Privacy Policy URL","Privacy Policy URL"
		pageObj.mobileconfigurationPage().clearTheURLsField("Privacy Policy URL");
		pageObj.mobileconfigurationPage().clearTheURLsField("SMS Privacy Policy URL");
		pageObj.mobileconfigurationPage().clearTheURLsField("SMS Terms and Conditions URL");
		pageObj.dashboardpage().updateButton();

		Response card;
		int counter = 0;
		boolean flag = true;
		String smsPrivacyURl;
		String smsTermsCondition;
		String privacyPolicyUrl;
		boolean smsPrivacyUrlEmpty;
		boolean smsTermsConditionEmpty;
		boolean privacyPolicyUrlEmpty;

		do {
			card = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			softAssert.assertEquals(card.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not matched for api1 cards");
			smsPrivacyURl = card.jsonPath().getString("[0].sms_privacy_policy_url");
			smsTermsCondition = card.jsonPath().getString("[0].sms_terms_conditions_url");
			privacyPolicyUrl = card.jsonPath().getString("[0].privacy_url");
			utils.longWaitInSeconds(3);

			smsPrivacyUrlEmpty = smsPrivacyURl == null || smsPrivacyURl.isEmpty();
			smsTermsConditionEmpty = smsTermsCondition == null || smsTermsCondition.isEmpty();
			privacyPolicyUrlEmpty = privacyPolicyUrl == null || privacyPolicyUrl.isEmpty();

			if (smsPrivacyUrlEmpty && smsTermsConditionEmpty && privacyPolicyUrlEmpty) {
				flag = false;
			}
			counter++;
		} while (flag && counter != 50);

		softAssert.assertTrue(smsPrivacyUrlEmpty, "smsPrivacyURl is not null or empty as expected");
		softAssert.assertTrue(smsTermsConditionEmpty, "smsTermsCondition is not null or empty as expected");
		softAssert.assertTrue(privacyPolicyUrlEmpty, "privacyPolicyUrl is not null or empty as expected");
		// Meta API v1 validation for customized Urls field "SMS Privacy Policy
		// URL","SMS Privacy Policy URL","Privacy Policy URL"

		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("smsPrivacyPolicyLabel"),
				dataSet.get("validUrlsmsPrivacyPolicy"));
		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("smsTermsAndConditionLabel"),
				dataSet.get("validUrlSmstermsCondition"));
		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("privacyPolicyLabel"),
				dataSet.get("validUrlPrivacyPolicy"));
		pageObj.dashboardpage().updateButton();

		Response card1;
		int counter1 = 0;
		boolean flag1 = true;
		String smsPrivacyURl1;
		String smsTermsCondition1;
		String privacyPolicyUrl1;

		do {

			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			softAssert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_OK, "Status code not equal");
			smsPrivacyURl1 = card1.jsonPath().getString("[0].sms_privacy_policy_url");
			smsTermsCondition1 = card1.jsonPath().getString("[0].sms_terms_conditions_url");
			privacyPolicyUrl1 = card1.jsonPath().getString("[0].privacy_url");
			utils.longWaitInSeconds(4);

			if (smsPrivacyURl1 != null && smsPrivacyURl1.equals("https://dashboard-development.punchh.com:3000/")
					&& smsTermsCondition1 != null
					&& smsTermsCondition1.equals("https://dashboard-development.punchh.com:3000/")
					&& privacyPolicyUrl1 != null
					&& privacyPolicyUrl1.equals("https://dashboard-development.punchh.com:3000/")) {
				flag1 = false;
			}
			utils.longwait(1000);
			counter1++;
		} while (flag1 && counter1 != 50);

		softAssert.assertEquals("https://dashboard-development.punchh.com:3000/", smsPrivacyURl1,
				"smsPrivacyURl1 did not matched with api response");
		utils.logPass("smsPrivacyURl1 verified after sending the valid url in url field");

		softAssert.assertEquals("https://dashboard-development.punchh.com:3000/", smsTermsCondition1,
				"smsTermsCondition1 did not matched with api response");
		utils.logPass("smsTermsCondition1 verified after sending the valid url in url field");

		softAssert.assertEquals("https://dashboard-development.punchh.com:3000/", privacyPolicyUrl1,
				"privacyPolicyUrl1 did not matched with api response");
		utils.logPass("privacyPolicyUrl1 verified after sending the valid url in url field");

		// URLs field verification for newly added flags "SHOW_GET_DIRECTION" and
		// "SHOW_MAP_TOOLBAR"
		pageObj.mobileconfigurationPage().navigateToTab("Loyalty");
		pageObj.mobileconfigurationPage().clickLoyaltyAppConfig();
		String loyaltyTabDetails = pageObj.mobileconfigurationPage().getPageSource();
		softAssert.assertTrue(loyaltyTabDetails.contains("Show “Get Directions” CTA"),
				"Show “Get Directions” CTA is not present on page");
		softAssert.assertTrue(loyaltyTabDetails.contains("Show Map Toolbar"),
				"TShow Map Toolbar is not present on page");
		// Hint text verification for newly added flags "SHOW_GET_DIRECTION" and
		// "SHOW_MAP_TOOLBAR"
		String getDirectionHintText = pageObj.mobileconfigurationPage().getHintText("Show “Get Directions” CTA");
		softAssert.assertEquals(
				"Provides a button in the app that takes user to native maps app. This will display the button in Store Details screens as well as in the native online ordering flow",
				getDirectionHintText);
		TestListeners.extentTest.get().pass("Expected hint text matched for flag Show “Get Directions” CTA ");
		String showMapToolbarHintText = pageObj.mobileconfigurationPage().getHintText("Show Map Toolbar");
		softAssert.assertEquals("Provides buttons in the Google maps view that takes user to the native maps app",
				showMapToolbarHintText);
		TestListeners.extentTest.get().pass("Expected hint text matched for flag Show Map Toolbar");
		// Meta API V1 validation for flag checked "SHOW_GET_DIRECTION" and
		// "SHOW_MAP_TOOLBAR"
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Show “Get Directions” CTA", "check");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Show Map Toolbar", "check");
		// Dashboard updated
		pageObj.dashboardpage().updateButton();
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		softAssert.assertEquals(message, "Mobile configuration updated for Loyalty",
				"Mobile configuration is not updated");

		Response card2;
		int counter2 = 0;
		String loyaltyAppConfig2;
		boolean showDirection;
		boolean showMapToolBar;

		boolean flag2 = true;
		do {
			card2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			softAssert.assertEquals(card2.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig2 = card2.jsonPath().getString("[0].loyalty_app_config");
			JSONObject jsonObject = new JSONObject(loyaltyAppConfig2);
			showDirection = jsonObject.getBoolean("SHOW_GET_DIRECTION");
			showMapToolBar = jsonObject.getBoolean("SHOW_MAP_TOOLBAR");
			utils.longWaitInSeconds(3);

			if (showDirection == true && showMapToolBar == true) {
				flag2 = false;
			}
			utils.longwait(1000);
			counter2++;
		} while (flag2 && (counter2 != 50));
		softAssert.assertTrue(showDirection == true, "when flag is ON then also incorrect value is coming");
		utils.logPass("Verfied SHOW_GET_DIRECTION value is updated in the API");
		softAssert.assertTrue(showMapToolBar == true, "when flag is ON then also incorrect value is coming");
		utils.logPass("Verfied SHOW_MAP_TOOLBAR value is updated in the API");
		// Meta API V1 validation for unchecked flag "SHOW_GET_DIRECTION" and
		// "SHOW_MAP_TOOLBAR"
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Show “Get Directions” CTA", "uncheck");
		pageObj.mobileconfigurationPage().checkUncheckFlagMobileConfig("Show Map Toolbar", "uncheck");
		// Dashboard updated
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		softAssert.assertEquals(message, "Mobile configuration updated for Loyalty",
				"Mobile configuration is not updated");
		Response card3;
		int counter3 = 0;
		String loyaltyAppConfig3;
		boolean showGetDirectionFalse;
		boolean showMapToolBarFalse;
		boolean flag3 = true;
		do {
			card3 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			softAssert.assertEquals(card3.statusCode(), ApiConstants.HTTP_STATUS_OK, "status code not equal");
			loyaltyAppConfig3 = card3.jsonPath().getString("[0].loyalty_app_config");
			JSONObject jsonObject = new JSONObject(loyaltyAppConfig3);
			showGetDirectionFalse = jsonObject.getBoolean("SHOW_GET_DIRECTION");
			showMapToolBarFalse = jsonObject.getBoolean("SHOW_MAP_TOOLBAR");
			utils.longWaitInSeconds(3);

			if (showGetDirectionFalse == false && showMapToolBarFalse == false) {
				flag3 = false;
			}
			utils.longwait(1000);
			counter3++;
		} while (flag3 && (counter3 != 50));
		softAssert.assertTrue(showGetDirectionFalse == false, "when flag is OFF then also incorrect value is coming");
		utils.logPass("Verfied SHOW_GET_DIRECTION value is updated in the API");
		softAssert.assertTrue(showMapToolBarFalse == false, "when flag is OFF then also incorrect value is coming");
		utils.logPass("Verfied SHOW_MAP_TOOLBAR value is updated in the API");
		// Url fields error validation "SMS Privacy Policy
		// URL","SMS Privacy Policy URL","Privacy Policy URL"
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().navigateToTab("Miscellaneous URLs");
		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("smsPrivacyPolicyLabel"),
				dataSet.get("smsPrivacyPolicyInvalidURL"));
		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("smsTermsAndConditionLabel"),
				dataSet.get("smsTermsAndConditionInValidURl"));
		pageObj.mobileconfigurationPage().sendURlsToURLsFields(dataSet.get("privacyPolicyLabel"),
				dataSet.get("privacyPolicyInvalidUrl"));
		pageObj.dashboardpage().updateButton();
		String smsPrivacyError = pageObj.mobileconfigurationPage()
				.errormessageFromAllTheURlsField(dataSet.get("smsPrivacyPolicyLabel"));
		softAssert.assertEquals("is invalid", smsPrivacyError, " Failed to verify is invalid error message");
		String smsTermsAndConditionError = pageObj.mobileconfigurationPage()
				.errormessageFromAllTheURlsField(dataSet.get("smsTermsAndConditionLabel"));
		softAssert.assertEquals("is invalid", smsTermsAndConditionError, "Failed to verify is invalid error message");
		String privacyPolicyError = pageObj.mobileconfigurationPage()
				.errormessageFromAllTheURlsField(dataSet.get("privacyPolicyLabel"));
		softAssert.assertEquals("is invalid", privacyPolicyError, "Failed to verify is invalid error message");
		softAssert.assertAll();
	}

	@Test(description = "SQ-T7174:Validate that the \"Excluded Order Types\" field in Ordering tab of Mobile configuration")
	@Owner(name = "Rajasekhar Reddy")
	public void T7174_ValidateExcludedOrderTypes() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Ordering");

		// deselect the value
		pageObj.posIntegrationPage().deselectAllValuesFromDrpDown("Excluded Order Types");
		List<String> expExcludedOrdertypesDrpDownList = Arrays.asList("Table Service / Dine-In",
				"Dine-In Pickup / Pickup", "Takeout", "Delivery", "Curbside", "Catering Delivery", "Catering Pickup",
				"Drive-Thru", "Dispatch");
		List<String> actualExcludedOrdertypesDrpDownList = pageObj.mobileconfigurationPage()
				.getExcludedOrderTypesDrpDownList();
		Assert.assertEquals(actualExcludedOrdertypesDrpDownList, expExcludedOrdertypesDrpDownList,
				"Excluded order types drp down list is not coming as expected");

		// select the Delivery value from drp down
		pageObj.posIntegrationPage().selectDrpDownValue("Excluded Order Types", "Delivery");
		pageObj.posIntegrationPage().selectDrpDownValue("Excluded Order Types", "Table Service / Dine-In");
		pageObj.posIntegrationPage().selectDrpDownValue("Excluded Order Types", "Takeout");
		pageObj.mobileconfigurationPage().clickUpdateBtn();

		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String orderingAppConfig = cardsResponse.jsonPath().get("[0].ordering_app_config").toString();
		orderingAppConfig.contains(
				"EXCLUDE_ORDER_TYPES\":[{\"order_type_id\":1,\"order_type_name\":\"dinein\"},{\"order_type_id\":6,\"order_type_name\":null},{\"order_type_id\":7,\"order_type_name\":\"delivery\"}]");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}