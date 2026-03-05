package com.punchh.server.CXTest;

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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileConfigurationTest {
	private static Logger logger = LogManager.getLogger(MobileConfigurationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
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
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Author : Amit
	// Merged this test case's steps into T3312_verifyRedeemablePointThresholdsUpdatedValuesAppearingInV1MetaAPIResponse
	//@Test(description = "SQ-T3248 Verify V1 meta-API should not lead to 500 error whenCheck-In Frequency for App/Play Store Rating set blank.", groups = {"regression", "dailyrun" }, priority = 0)
	public void T3248_verifyV1MetaAPIShoulNotThrow500ErrorWhenCheckInFrequencyForAppPlayStoreRatingIsSetBlank()
			throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		pageObj.mobileconfigurationPage().setCheckInFrequencyAppPlayStoreRating(dataSet.get("checkInFrequency"));
		String successMsg = pageObj.mobileconfigurationPage().verifyAndUpdate();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Loyalty", "Success message didn't matched");
		// Meta API validation
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 V1 meta-API is successful with response :" + cardsResponse.asString());
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T3312, SQ-T3313: Verify redeemable point thresholds updated values appearing in v1 meta-API response; "
			+ "SQ-T3248: Verify V1 meta-API should not lead to 500 error when Check-In Frequency for App/Play Store Rating set blank.; "
			+ "SQ-T3311: Verify newly added field Redeemable Point Thresholds", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3312_verifyRedeemablePointThresholdsUpdatedValuesAppearingInV1MetaAPIResponse()
			throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Menu Items
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");

		// SQ-T3248 starts
		pageObj.mobileconfigurationPage().setCheckInFrequencyAppPlayStoreRating(dataSet.get("checkInFrequency"));
		String successMsg = pageObj.mobileconfigurationPage().verifyAndUpdate();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Loyalty", "Success message didn't matched");
		// Meta API validation
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 V1 meta-API is successful with response :" + cardsResponse.asString());
		// SQ-T3248 ends
		
		// with data // Meta API validation
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds(dataSet.get("RedeemablePointThresholds"));
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String successMsg1 = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg1, "Mobile configuration updated for Loyalty", "Success message didn't matched");

		Response cardsResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		String redeemablePointThresholdsData = cardsResponse1.jsonPath().get("[0].redeemable_point_thresholds[4]")
				.toString();
		Assert.assertEquals(redeemablePointThresholdsData, "9", "Redeemable Point Thresholds Data did not matched");
		TestListeners.extentTest.get()
				.pass("Api1 V1 meta-API is successful with response :" + cardsResponse1.asString());

		Response metaAPI2Response1 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaAPI2Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		String redeemablePointThresholdsDataapi2 = metaAPI2Response1.jsonPath().get("redeemable_point_thresholds[4]")
				.toString();
		Assert.assertEquals(redeemablePointThresholdsDataapi2, "9",
				"Redeemable Point Thresholds Data did not matched in meta api2");
		TestListeners.extentTest.get()
				.pass("Api1 V1 meta-API is successful with response :" + metaAPI2Response1.asString());

		// with empty data // Meta API validation
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Disclaimer");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds("");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String successMsg2 = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg2, "Mobile configuration updated for Loyalty", "Success message didn't matched");

		boolean flag = false;
		int attempts = 0;
		while (attempts < 40) {
			try {
				utils.longWait(5000);
				Response cardsResponse2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
				Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api1 cards");
				String redeemablePointThresholdsEmpty = cardsResponse2.jsonPath().get("[0].redeemable_point_thresholds")
						.toString();
				if (redeemablePointThresholdsEmpty.contains("[]")) {
					flag = true;
					Assert.assertEquals(redeemablePointThresholdsEmpty, "[]",
							"Redeemable Point Thresholds Data did not matched in meta api1");
					TestListeners.extentTest.get()
							.pass("Api1 V1 meta-API is successful with response status " + cardsResponse2.statusCode());
					break;
				}
			} catch (Exception e) {
				logger.info("exception :" + e.getMessage());
			}
			attempts++;
		}

		Response metaAPI2Response = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaAPI2Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		String redeemablePointThresholdsEmptyapi2 = metaAPI2Response.jsonPath().get("redeemable_point_thresholds")
				.toString();
		Assert.assertEquals(redeemablePointThresholdsEmptyapi2, "[]",
				"Redeemable Point Thresholds Data did not matched in meta api2");
		TestListeners.extentTest.get()
				.pass("Api1 V1 meta-API is successful with response :" + metaAPI2Response.asString());

		// SQ-T3311 starts
		String optionalStatus = pageObj.mobileconfigurationPage().checkIFRedeemablePointThresholdsOptional();
		Assert.assertTrue(optionalStatus.contains("string optional form-control no-locale"),
				"Field Optional type check did not match");
		String fieldHintText = pageObj.mobileconfigurationPage().getRedeemablePointThresholdsHinttext();
		Assert.assertEquals(fieldHintText,
				"Point thresholds used by the mobile app for grouping and filtering loyalty goal redeemables. Multiple values should be separated by commas (e.g. “4,10,20”).",
				"Hint text check did not match");
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds("4.5,8.6,8.9");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String topErrorMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		String invalidInputErrMsg = pageObj.mobileconfigurationPage()
				.getRedeemablePointThresholdsInvalidInputErrorMsg();
		Assert.assertEquals(topErrorMsg, "Error updating Mobile configuration for Loyalty",
				"invalid input top error msg did not match");
		Assert.assertEquals(invalidInputErrMsg, "Please input only numeric values separated by a comma",
				"invalid input error msg did not match");
		// SQ-T3311 ends
	}

	// Merged this test case's steps into T3312_verifyRedeemablePointThresholdsUpdatedValuesAppearingInV1MetaAPIResponse
	//@Test(description = "SQ-T3311 Verify newly added field Redeemable Point Thresholds", groups = { "regression", "dailyrun" }, priority = 2)
	public void T3311_verifyNewlyAddedFieldRedeemablePointThresholds() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Menu Items
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		String optinalStatus = pageObj.mobileconfigurationPage().checkIFRedeemablePointThresholdsOptional();
		Assert.assertTrue(optinalStatus.contains("string optional form-control no-locale"),
				"Filed  Optional type check did not matched");
		String fieldHinttext = pageObj.mobileconfigurationPage().getRedeemablePointThresholdsHinttext();
		Assert.assertEquals(fieldHinttext,
				"Point thresholds used by the mobile app for grouping and filtering loyalty goal redeemables. Multiple values should be separated by commas (e.g. “4,10,20”).",
				"Hint text check did not matched");
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds("4.5,8.6,8.9");
		String topErrorMsg = pageObj.mobileconfigurationPage().verifyAndUpdate();
		String invalidInputErrMsg = pageObj.mobileconfigurationPage()
				.getRedeemablePointThresholdsInvalidInputErrorMsg();
		Assert.assertEquals(topErrorMsg, "Error updating Mobile configuration for Loyalty",
				"invalid input top error msg did not matched");
		Assert.assertEquals(invalidInputErrMsg, "Please input only numeric values separated by a comma",
				"invalid input error msg did not matched");
	}

	@Test(description = "SQ-T3310 Verify when user refreshing/updating the account deletion page then page focusing on correct tab.", groups = {
			"regression", "dailyrun" }, priority = 3)
	public void T3310_verifyWhenUserRefreshingUpdatingAccountDeletionPageThenPageFocusingOnCorrectTab()
			throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Account Deletion");

		pageObj.Accountdeletionpage().clickIosDeactivation();
		pageObj.Accountdeletionpage().clickAndroidDeactivation();
		pageObj.Accountdeletionpage().appDeletionTypeAndReason(dataSet.get("iosDeletionType"),
				dataSet.get("androidDeletionType"), dataSet.get("iosDeletionReason"));
		pageObj.Accountdeletionpage().setDeletionRequestEmail();
		String msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"success message for account deletion update did not matched");
		pageObj.Accountdeletionpage().appDeletionTypeAndReason(dataSet.get("iosDeletionType"),
				dataSet.get("androidDeletionType"), dataSet.get("iosDeletionReason"));
	}

	// By Ajeet
	@Test(description = "SQ-T5974 verify for $ 0 no max limit under mobile Configuration Gift card", enabled = true, groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Ajeet")
	public void T5974_verifyForNoMaximumAmountLimitUnderMobileConfigGiftCards() throws Exception {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_gift_card_auto_reload", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_gift_card_auto_reload value is not updated to true");
		utils.logit("enable_gift_card_auto_reload value is updated to true");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdaptor(dataSet.get("giftCardAdaptorName"));
		pageObj.giftcardsPage().setMinMaxAmountGiftCard(dataSet.get("minimumAmount"), dataSet.get("maximumAmount"));
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto Reload", "check");
		pageObj.giftcardsPage().clickOnUpdateButton();
		// navigate to the subMenu item
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Gift Card");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Purchase", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Reload", "check");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 1",
				dataSet.get("purchaseAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 2",
				dataSet.get("purchaseAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 3",
				dataSet.get("purchaseAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 1",
				dataSet.get("reloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 2",
				dataSet.get("reloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 3",
				dataSet.get("reloadAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 1",
				dataSet.get("autoReloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 2",
				dataSet.get("autoReloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 3",
				dataSet.get("autoReloadAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 1",
				dataSet.get("thresoldAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 2",
				dataSet.get("thresoldAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 3",
				dataSet.get("thresoldAmount3"));
		String message = pageObj.mobileconfigurationPage().verifyAndUpdate();
		Assert.assertEquals(message, "Mobile configuration updated for Gift Card");
		TestListeners.extentTest.get().pass("No max amount limit verified: " + message);
	}

	@Test(description = " SQ-T5986 Validating the error message under the cockpit against the whitelebel>Mobile Configuration> Gift cards", enabled = true, groups = {
			"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Ajeet")
	public void T5986_ValidateErrorMessagesUnderCockpit() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit> Gift Cards
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdaptor(dataSet.get("giftCardAdaptorName"));
		pageObj.giftcardsPage().setMinMaxAmountGiftCard(dataSet.get("minimumAmount"), dataSet.get("maximumAmount"));
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto Reload", "check");
		pageObj.giftcardsPage().clickOnUpdateButton();
		// Navigate to whitelabel> Mobile Configuration
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Gift Card");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Purchase", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Reload", "check");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 1",
				dataSet.get("purchaseAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 2",
				dataSet.get("purchaseAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 3",
				dataSet.get("purchaseAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 1",
				dataSet.get("reloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 2",
				dataSet.get("reloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 3",
				dataSet.get("reloadAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 1",
				dataSet.get("autoReloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 2",
				dataSet.get("autoReloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 3",
				dataSet.get("autoReloadAmount3"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 1",
				dataSet.get("thresoldAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 2",
				dataSet.get("thresoldAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 3",
				dataSet.get("thresoldAmount3"));
		String s = pageObj.mobileconfigurationPage().verifyAndUpdate();
		Assert.assertEquals("Mobile configuration updated for Gift Card", s);
		TestListeners.extentTest.get().pass("Mobile configuration updated for Gift Card: " + s);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdaptor(dataSet.get("giftCardAdaptorName"));
		pageObj.giftcardsPage().setMinMaxAmountGiftCard(dataSet.get("minimumAmount"), dataSet.get("maximumAmount1"));
		String text = pageObj.giftcardsPage()
				.errorMessageForInvailidAmountOnGiftCards("Whitelabel Mobile Configuration:"
						+ " \"Gift Card Purchase Default Amounts\" values should be less than or equal to Cockpit:"
						+ " \"Maximum Amount on Gift Card\"");
		Assert.assertEquals("Whitelabel Mobile Configuration: \"Gift Card Purchase Default Amounts\" values should be"
				+ " less than or equal to Cockpit: \"Maximum Amount on Gift Card\"\n"
				+ "Whitelabel Mobile Configuration: \"Gift Card Reload Default Amounts\" values should be less than or "
				+ "equal to Cockpit: \"Maximum Amount on Gift Card\"\n"
				+ "Whitelabel Mobile Configuration: \"Gift Card Auto-Reload Default Amounts\" values should be less than"
				+ " or equal to Cockpit: \"Maximum Amount on Gift Card\"", text);
		pageObj.giftcardsPage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdaptor(dataSet.get("giftCardAdaptorName"));
		pageObj.giftcardsPage().setMinMaxAmountGiftCard(dataSet.get("minimumAmount1"), dataSet.get("maximumAmount"));
		String text2 = pageObj.giftcardsPage()
				.errorMessageForInvailidAmountOnGiftCards("Whitelabel Mobile Configuration: \"Gift "
						+ "Card Purchase Default" + " Amounts\" values should be more than or"
						+ " equal to Cockpit: \"Minimum Transaction Amount\"");
		Assert.assertEquals(
				"Whitelabel Mobile Configuration: \"Gift Card Purchase Default Amounts\" values should be more"
						+ " than or equal to Cockpit: \"Minimum Transaction Amount\"\n"
						+ "Whitelabel Mobile Configuration: \"Gift Card Reload Default Amounts\" values should be more than or equal "
						+ "to Cockpit: \"Minimum Transaction Amount\"\n"
						+ "Whitelabel Mobile Configuration: \"Gift Card Auto-Reload Default Amounts\" values should be more than or equal "
						+ "to Cockpit: \"Minimum Transaction Amount\"",
				text2);
		pageObj.giftcardsPage().clickOnUpdateButton();
		TestListeners.extentTest.get()
				.pass("Error message verified under cockpit for mimimum amount greater than maximum amount "
						+ "on gift cards against whitelabel: " + text2);
	}

	@Test(description = "SQ-T5974 verify for $ 0 no max limit under mobile Configuration Gift card", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Ajeet")
	public void T5974_NegativeTestVerifyForNoMaximumAmountLimitUnderMobileConfigGiftCards()
			throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set the pre-requisite configs
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdaptor(dataSet.get("giftCardAdaptorName"));
		pageObj.giftcardsPage().setMinMaxAmountGiftCard(dataSet.get("minimumAmount"), dataSet.get("maximumAmount"));
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto Reload", "check");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Gift Card");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Purchase", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Reload", "check");
		
		// Success scenario validation
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 1",
				dataSet.get("purchaseAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 2",
				dataSet.get("purchaseAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 3",
				dataSet.get("purchaseAmount3"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.defaultPurchaseAmountDropdown"), "Purchase Amount 1");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 1",
				dataSet.get("reloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 2",
				dataSet.get("reloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 3",
				dataSet.get("reloadAmount3"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.defaultReloadAmountDropdown"), "Reload Amount 3");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 1",
				dataSet.get("autoReloadAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 2",
				dataSet.get("autoReloadAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 3",
				dataSet.get("autoReloadAmount3"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.autoReloadAmountDropdown"), "Auto-Reload Amount 2");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 1",
				dataSet.get("thresoldAmount1"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 2",
				dataSet.get("thresoldAmount2"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 3",
				dataSet.get("thresoldAmount3"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.balanceThresholdAmountDropdown"),
				"Balance Threshold Amount 1");
		pageObj.mobileconfigurationPage().verifyAndUpdate();
		
		// Negative scenario validation
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 1",
				dataSet.get("purchaseAmount11"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 2",
				dataSet.get("purchaseAmount22"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Purchase Amount 3",
				dataSet.get("purchaseAmount33"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.defaultPurchaseAmountDropdown"), "Purchase Amount 1");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 1",
				dataSet.get("reloadAmount11"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 2",
				dataSet.get("reloadAmount22"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Reload Amount 3",
				dataSet.get("reloadAmount33"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.defaultReloadAmountDropdown"), "Reload Amount 3");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 1",
				dataSet.get("autoReloadAmount11"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 2",
				dataSet.get("autoReloadAmount22"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Auto-Reload Amount 3",
				dataSet.get("autoReloadAmount33"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.autoReloadAmountDropdown"), "Auto-Reload Amount 2");
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 1",
				dataSet.get("thresoldAmount11"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 2",
				dataSet.get("thresoldAmount22"));
		pageObj.mobileconfigurationPage().enterAmountsInSeveralGiftCards("Balance Threshold Amount 3",
				dataSet.get("thresoldAmount33"));
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.balanceThresholdAmountDropdown"),
				"Balance Threshold Amount 1");
		// Error message validation
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String dashBoardErrorMessage = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals("Error updating Mobile configuration for Gift Card", dashBoardErrorMessage,
				"error message Verified");
		utils.logPass("Error message text found and error message is =>" + dashBoardErrorMessage);
		// purchase amounts field's error messages
		String purchaseAmount1 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Purchase Amount 1", "Must be larger than the Minimum Transaction Amount");
		Assert.assertEquals("Must be larger than the Minimum Transaction Amount", purchaseAmount1,
				"Failed to verify purchase amount1 field's error message");
		utils.logPass("Expected error message text found and error message is => " + purchaseAmount1);
		String purchaseAmount2 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Purchase Amount 2", "Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", purchaseAmount2,
				"Failed to verify purchase amount2 field's error message");
		utils.logPass("Expected error message text found and error message is => " + purchaseAmount2);
		String purchaseAmount3 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Purchase Amount 3", "Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", purchaseAmount3,
				"Failed to verify purchase amount3 field's error message");
		utils.logPass("Expected error message text found and error message is => " + purchaseAmount3);
		// Reload amount field's error messages
		String reloadAmount1 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage("Reload Amount 1",
				"Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", reloadAmount1,
				"Failed to verify reload amount1 field's error message");
		utils.logPass("Expected error message text found and error message is => " + reloadAmount1);
		String reloadAmount2 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage("Reload Amount 2",
				"Must be larger than the Minimum Transaction Amount");
		Assert.assertEquals("Must be larger than the Minimum Transaction Amount", reloadAmount2,
				"Failed to verify reload amount2 field's error message");
		utils.logPass("Expected error message text found and error message is => " + reloadAmount2);
		String reloadAmount3 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage("Reload Amount 3",
				"Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", reloadAmount3,
				"Failed to verify reload amount3 field's error message");
		utils.logPass("Expected error message text found and error message is => " + reloadAmount3);
		// Auto reload amounts field's error messages
		String autoReloadAamount1 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Auto-Reload Amount 1", "Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", autoReloadAamount1,
				"Failied to verify auto reload amount1 field's error message");
		utils.logPass("Expected error message text found and error message is =>" + autoReloadAamount1);
		String autoReloadAamount2 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Auto-Reload Amount 2", "Cannot exceed the Maximum Amount of Gift Card");
		Assert.assertEquals("Cannot exceed the Maximum Amount of Gift Card", autoReloadAamount2,
				"Failed to verify auto reload amount2 field's error message");
		utils.logPass("Expected error message text found and error message is => " + autoReloadAamount2);
		String autoReloadAamount3 = pageObj.mobileconfigurationPage().verifyTheGiftCardsAmountFieldMessage(
				"Auto-Reload Amount 3", "Must be larger than the Minimum Transaction Amount");
		Assert.assertEquals("Must be larger than the Minimum Transaction Amount", autoReloadAamount3,
				"Failed to verify auto reload amount3 field's error message");
		utils.logPass("Expected error message text found and error message is => " + autoReloadAamount3);
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