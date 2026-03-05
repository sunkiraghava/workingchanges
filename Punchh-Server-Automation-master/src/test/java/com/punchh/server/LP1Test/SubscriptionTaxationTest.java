package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SubscriptionTaxationTest {

	static Logger logger = LogManager.getLogger(SubscriptionTaxationTest.class);
	public WebDriver driver;
	PageObj pageObj;
	String sTCName, env;
	private String run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	Utilities utils;

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

	@Test(description = "SQ-T6860 Verify that the new feature flag can be enabled/disabled when Subscriptions are enabled + "
			+ "SQ-T6861 Verify Dependency on Subscriptions Flag + "
			+ "SQ-T6864 Verify that the Subscription Taxation page is hidden when the flag is disabled +"
			+ "SQ-T6866 Verify that disabling the Subscriptions flag also disables the Taxation Support flag + "
			+ "SQ-T6867 Verify that changes to the feature flag are captured in audit logs", groups = { "regression",
					"dailyrun" }, priority = 1)
	@Owner(name = "Shubham Gupta")
	public void T6860_verifyFeatureFlagEnableDisableWhenSubscriptionEnable() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		boolean isSubscriptionChecked = pageObj.dashboardpage().isCheckboxChecked("Enable Subscriptions?");
		boolean isTaxationEnable = pageObj.dashboardpage().isCheckboxEnable("Enable Taxation Support In Subscription?");
		if (isSubscriptionChecked) {
			// If Subscription flag is checked, then uncheck it and verify that Taxation
			// flag is disabled
			Assert.assertTrue(isTaxationEnable, "Taxation is not enabled when subscription is checked");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "uncheck");
			pageObj.dashboardpage().clickOnUpdateButton();
			isTaxationEnable = pageObj.dashboardpage().isCheckboxEnable("Enable Taxation Support In Subscription?");
			Assert.assertFalse(isTaxationEnable, "Taxation is enabled when subscription is unchecked");
			utils.logit("Taxation is disabled when subscription is unchecked");
		} else {
			// If Subscription flag is unchecked, then check it and verify that Taxation
			// flag is enabled
			Assert.assertFalse(isTaxationEnable, "Taxation is enabled when subscription is unchecked");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
			pageObj.dashboardpage().clickOnUpdateButton();
			isTaxationEnable = pageObj.dashboardpage().isCheckboxEnable("Enable Taxation Support In Subscription?");
			Assert.assertTrue(isTaxationEnable, "Taxation is not enabled when subscription is checked");
			utils.logit("Taxation is enabled when subscription is checked");
			// Once Taxation flag is enabled, check it and verify the change in Audit log
			pageObj.dashboardpage().checkUncheckAnyFlag("Enable Taxation Support In Subscription?", "check");
			boolean isTextFoundInAuditLog = pageObj.dashboardpage()
					.checkIfGivenTextIsPresentInAuditLog("Enable Taxation Support In Subscription");
			Assert.assertTrue(isTextFoundInAuditLog,
					"Changes to Enable Taxation Support In Subscription? flag is not captured in audit log");
			utils.logit("pass", "Changes to Enable Taxation Support In Subscription? flag is captured in audit log");
		}

	}

	@Test(description = "SQ-T6862 Verify visibility of Subscription Taxation Page (New Navigation) +"
			+ " SQ-T6863 Verify visibility of Subscription Taxation Page (Old Navigation)	", groups = { "regression",
					"dailyrun" }, priority = 2)
	@Owner(name = "Shubham Gupta")
	public void T6862_verifyVisibilityOfSubscriptionTaxationNewNavigation() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Taxation Support In Subscription?", "check");
		List<String> WalletAndPassesSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_WalletAndPasses);
		boolean isSubMenuPresent = pageObj.menupage().checkIfSubMenuPresentInMenuItems(WalletAndPassesSubMenus,
				"Subscription Taxation");
		Assert.assertTrue(isSubMenuPresent,
				"Subscription Taxation sub menu is not present under Wallet and Passes menu");
		utils.logit("Subscription Taxation sub menu is present under Wallet and Passes menu in new navigation");

		// Verify in old navigation
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable New SideNav, Header and Footer?", "uncheck");
		List<String> subscriptionSubMenus = pageObj.menupage().subMenuItems_OldNav("Subscriptions");
		boolean isSubMenuPresents = pageObj.menupage().checkIfSubMenuPresentInMenuItems(subscriptionSubMenus,
				"Subscription Taxation");
		Assert.assertTrue(isSubMenuPresents, "Subscription Taxation sub menu is not present under Subscriptions menu");
		utils.logit("Subscription Taxation sub menu is present under Subscriptions menu in old navigation");

		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable New SideNav, Header and Footer?", "check");

	}

	@Test(description = "SQ-T6870 Check if the new State Level Tax tab is added under Subscription Taxation +"
			+ " SQ-T6871 Verify if error message when country doesn’t have any state	+"
			+ " SQ-T6872 Verify that admins can select countries and view corresponding states + "
			+ " SQ-T6873 Verify if the table displays correct columns and allows tax rate entry + "
			+ " SQ-T6874 Verify the tax rate field only accepts valid numeric values +"
			+ " SQ-T6875 Verify if error message when same country tried to be selected again + "
			+ " SQ-T6876 Verify if changes are saved when the Save button is clicked", groups = { "regression",
					"dailyrun" }, priority = 3)
	@Owner(name = "Shubham Gupta")
	public void T6870_verifyStateLevelTaxFeature() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Taxation Support In Subscription?", "check");
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Taxation");
		pageObj.subscriptionTaxationPage().clickOnGivenTab("State Level Tax");
		pageObj.subscriptionTaxationPage().removeCountry();
		pageObj.subscriptionTaxationPage().selectCountryFromDrpdown("India (IND)");
		pageObj.subscriptionTaxationPage().clickOnAddCountryBtn();
		boolean isStateTableApprearing = pageObj.subscriptionTaxationPage().isStateTableAppeared();
		Assert.assertTrue(isStateTableApprearing, "State table is not appearing after adding country");

		// Verify error message when country doesn't have any state
		pageObj.subscriptionTaxationPage().selectCountryFromDrpdown("Åland Islands (ALA)");
		pageObj.subscriptionTaxationPage().clickOnAddCountryBtn();
		String alertText = pageObj.subscriptionTaxationPage().getAlertText();
		Assert.assertTrue(alertText.contains(dataSet.get("errorMsg")),
				"Alert text is not as expected when country doesn't have any state");
		utils.acceptAlert(driver);

		// Verify error message when same country is selected again
		pageObj.subscriptionTaxationPage().selectCountryFromDrpdown("India (IND)");
		pageObj.subscriptionTaxationPage().clickOnAddCountryBtn();
		String alertTextWhenSameCountrySelect = pageObj.subscriptionTaxationPage().getAlertText();
		Assert.assertTrue(alertTextWhenSameCountrySelect.contains(dataSet.get("alertMsgWhenSameCountrySelect")),
				"Alert text is not as expected when same country is selected again");
		utils.acceptAlert(driver);

		// Verify error when entered tax value more than 100
		pageObj.subscriptionTaxationPage().enterTaxPercentage(dataSet.get("stateCode"), "105");
		pageObj.subscriptionTaxationPage().clickOnSaveBtn(dataSet.get("stateCode"));
		String alertTextWhenTaxMoreThan100 = pageObj.subscriptionTaxationPage().getAlertText();
		Assert.assertTrue(alertTextWhenTaxMoreThan100.contains(dataSet.get("alertMsgTaxValueMoreThan100")),
				"Alert text is not as expected when tax value more than 100 is entered");
		utils.acceptAlert(driver);

		// Enter valid tax percentage(including alphabets and special characters) and
		// save
		pageObj.subscriptionTaxationPage().enterTaxPercentage(dataSet.get("stateCode"), dataSet.get("taxPercentage"));
		pageObj.subscriptionTaxationPage().clickOnSaveBtn(dataSet.get("stateCode"));
		utils.refreshPage();
		String textWithoutSpecialChar = dataSet.get("taxPercentage").replaceAll("[^0-9.]", "");
		String actualPercentValue = pageObj.subscriptionTaxationPage()
				.getEnteredTaxPercentage(dataSet.get("stateCode"));
		Assert.assertEquals(actualPercentValue, textWithoutSpecialChar, "Entered tax percentage is not saved properly");
	}

	@Test(description = "SQ-T6880 Check if the API(/api2/mobile/meta.json and /api2/mobile/cards.json) "
			+ "returns true for the flag enable_subscription_taxation_support when it is enabled; "
			+ "SQ-T6920: Verify that the \"Subscription Taxation\" option is hidden under Wallets and Passes if \"Enable Taxation Support in Subscription\" flag is disabled; "
			+ "SQ-T6881: Check if the API(/api2/mobile/meta.json and /api2/mobile/cards.json) returns false for the flag enable_subscription_taxation_support when it is disabled", groups = {
					"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Shubham Gupta")
	public void T6880_CheckAPIResponseReturnsFlagValue() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Check Enable Taxation Support In Subscription? flag
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Taxation Support In Subscription?", "check");

		// Meta API v1 validation
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse.statusCode());
		String api1TaxationFlagValue = cardsResponse.jsonPath().get("[0].enable_subscription_taxation_support")
				.toString();
		boolean isApi1TaxationFlagEnabled = Boolean.parseBoolean(api1TaxationFlagValue);
		Assert.assertTrue(isApi1TaxationFlagEnabled, "enable_subscription_taxation_support value is not matched");

		// Meta API v2 validation
		Response cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 does not matched for api2 cards");
		TestListeners.extentTest.get()
				.pass("Api2 cards is successful with response code : " + cardsResponse2.statusCode());
		String api2TaxationFlagValue = cardsResponse2.jsonPath().get("enable_subscription_taxation_support").toString();
		boolean isApi2TaxationFlagEnabled = Boolean.parseBoolean(api2TaxationFlagValue);
		Assert.assertTrue(isApi2TaxationFlagEnabled, "enable_subscription_taxation_support value is not matched");

		// Uncheck Enable Taxation Support In Subscription? flag
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Taxation Support In Subscription?",
				"uncheck");

		// Verify that the Subscription Taxation sub menu is unavailable
		List<String> walletsSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_WalletAndPasses);
		pageObj.menupage().pinSidenavMenu();
		Assert.assertFalse(walletsSubMenusList.contains("Subscription Taxation"));
		utils.logit("pass",
				"Wallet and Passes does not contain Subscription Taxation when 'Enable Taxation Support In Subscription?' flag is disabled");

		// Meta API v1 validation
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 does not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse.statusCode());
		api1TaxationFlagValue = cardsResponse.jsonPath().get("[0].enable_subscription_taxation_support").toString();
		isApi1TaxationFlagEnabled = Boolean.parseBoolean(api1TaxationFlagValue);
		Assert.assertFalse(isApi1TaxationFlagEnabled, "enable_subscription_taxation_support value is not matched");
		utils.logPass("enable_subscription_taxation_support value is matched");

		// Meta API v2 validation
		cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 does not matched for api2 cards");
		TestListeners.extentTest.get()
				.pass("Api2 cards is successful with response code : " + cardsResponse2.statusCode());
		api2TaxationFlagValue = cardsResponse2.jsonPath().get("enable_subscription_taxation_support").toString();
		isApi2TaxationFlagEnabled = Boolean.parseBoolean(api2TaxationFlagValue);
		Assert.assertFalse(isApi2TaxationFlagEnabled, "enable_subscription_taxation_support value is not matched");
		utils.logPass("enable_subscription_taxation_support value is matched");
	}

	@Test(description = "SQ-T6918: Verify Subscription Taxation's Tax Rules page; "
			+ "SQ-T6869: Verify Major admin has access to Bulk update tab to upload location tax rate CSV successfully; "
			+ "SQ-T6919: Verify Subscription Taxation's Location Level Tax page", groups = "regression", priority = 5)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6918_verifySubscriptionTaxationTaxRules() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Taxation Support In Subscription?", "check");
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Taxation");
		pageObj.subscriptionTaxationPage().clickOnGivenTab("Bulk Update");
		utils.logit("pass", "Bulk Update tab is appearing and accessible");

		// Verify all 3 Tax Rules radio button are able to update successfully
		pageObj.subscriptionTaxationPage().clickOnGivenTab("Tax Rules");
		pageObj.dashboardpage().checkUncheckAnyFlag("State Sales Tax", "check");
		boolean isStateSalesTaxChecked = pageObj.dashboardpage().isCheckboxChecked("State Sales Tax");
		Assert.assertTrue(isStateSalesTaxChecked);
		pageObj.dashboardpage().checkUncheckAnyFlag("Business Level Sales Tax", "check");
		boolean isBusinessSalesTaxChecked = pageObj.dashboardpage().isCheckboxChecked("Business Level Sales Tax");
		Assert.assertTrue(isBusinessSalesTaxChecked);
		pageObj.dashboardpage().checkUncheckAnyFlag("Local Sales Tax (Location Level)", "check");
		boolean isLocationSalesTaxChecked = pageObj.dashboardpage()
				.isCheckboxChecked("Local Sales Tax (Location Level)");
		Assert.assertTrue(isLocationSalesTaxChecked);
		utils.logit("pass", "All 3 Tax Rules radio button are able to update successfully");

		// Verify only one Tax Rule can be updated at a time
		utils.checkUncheckFlag("State Sales Tax", "check");
		utils.checkUncheckFlag("Local Sales Tax (Location Level)", "check");
		utils.checkUncheckFlag("Business Level Sales Tax", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		String message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(message, MessagesConstants.taxConfigSuccessUpdate);
		isStateSalesTaxChecked = pageObj.dashboardpage().isCheckboxChecked("State Sales Tax");
		Assert.assertFalse(isStateSalesTaxChecked);
		isLocationSalesTaxChecked = pageObj.dashboardpage().isCheckboxChecked("Local Sales Tax (Location Level)");
		Assert.assertFalse(isLocationSalesTaxChecked);
		isBusinessSalesTaxChecked = pageObj.dashboardpage().isCheckboxChecked("Business Level Sales Tax");
		Assert.assertTrue(isBusinessSalesTaxChecked);
		utils.logit("pass", "Only one Tax Rule can be updated at a time");

		// Verify the 3 Tax Rules hint texts
		String stateSalesTaxHintText = pageObj.iframeConfigurationPage()
				.getElementText("subscriptionTaxationPage.taxRuleHintText", "State Sales Tax");
		Assert.assertEquals(stateSalesTaxHintText, MessagesConstants.stateSalesTaxHintText);
		String businessSalesTaxHintText = pageObj.iframeConfigurationPage()
				.getElementText("subscriptionTaxationPage.taxRuleHintText", "Business Level Sales Tax");
		Assert.assertEquals(businessSalesTaxHintText, MessagesConstants.businessSalesTaxHintText);
		String locationSalesTaxHintText = pageObj.iframeConfigurationPage()
				.getElementText("subscriptionTaxationPage.taxRuleHintText", "Local Sales Tax (Location Level)");
		Assert.assertEquals(locationSalesTaxHintText, MessagesConstants.locationSalesTaxHintText);
		utils.logit("pass", "All hint texts are verified successfully");

		// Verify Tax Rate field with invalid value
		pageObj.subscriptionTaxationPage().enterTaxRate(dataSet.get("invalidTaxRate"));
		pageObj.dashboardpage().clickOnUpdateButton();
		String errorMessage = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(errorMessage, MessagesConstants.taxRateError);
		boolean isInlineErrorMsgVerified = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText", MessagesConstants.taxRateFieldInlineError,
				dataSet.get("redColorHexCode"), 1);
		Assert.assertTrue(isInlineErrorMsgVerified);
		utils.logit("pass", "Tax Rate (%) field with invalid value is verified successfully");

		// Verify Tax Rate field with valid value and its hint text
		String taxRateHintText = pageObj.iframeConfigurationPage()
				.getElementText("subscriptionTaxationPage.taxRateHintText", "Tax Rate (%)");
		Assert.assertEquals(taxRateHintText, MessagesConstants.taxRateHintText);
		pageObj.subscriptionTaxationPage().enterTaxRate(dataSet.get("validTaxRate"));
		pageObj.dashboardpage().clickOnUpdateButton();
		String successMessage = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMessage, MessagesConstants.taxConfigSuccessUpdate);
		utils.logit("pass", "Tax Rate (%) field with valid value and its hint text are verified successfully");

		// Changes to Tax Rate persist after page refresh
		utils.refreshPage();
		String actualTaxRate = pageObj.iframeConfigurationPage()
				.getAttributeValue("subscriptionTaxationPage.taxRateField", "value", "");
		Assert.assertEquals(actualTaxRate, dataSet.get("validTaxRate"));
		utils.logit("pass", "Changes to Tax Rate persist after page refresh");

		// Tax Rate hides when "Business Level Sales Tax" is unselected
		utils.checkUncheckFlag("State Sales Tax", "check");
		boolean isTaxRateFieldDisplayed = pageObj.gamesPage().isPresent("subscriptionTaxationPage.taxRateField");
		Assert.assertFalse(isTaxRateFieldDisplayed);
		utils.logit("pass", "Tax Rate gets hidden when 'Business Level Sales Tax' is unselected");

		// Verify Location Level Tax tab when Local Sales Tax is disabled
		pageObj.dashboardpage().checkUncheckAnyFlag("State Sales Tax", "check");
		pageObj.subscriptionTaxationPage().clickOnGivenTab("Location Level Tax");
		boolean isLocationTaxDescriptionDisplayed = pageObj.gamesPage().isPresent("locationPage.locationsPageTopMsg");
		Assert.assertFalse(isLocationTaxDescriptionDisplayed);
		boolean isTaxRateDropdownPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("locationPage.locationHasTaxRateDropdown"));
		Assert.assertFalse(isTaxRateDropdownPresent);
		String locationTableHeadersCount = pageObj.segmentsBetaPage().verifyAndGetTextsCount(
				utils.getLocatorList("locationPage.locationTableHeaders"),
				dataSet.get("tableHeadersWithLocalTaxDisabled"));
		Assert.assertEquals(locationTableHeadersCount, "8");
		utils.logit("pass",
				"Location Tax description, Tax Rate (%) column and 'Has Tax Rate' filter are not present when Local Sales Tax is disabled");

		// Verify Location Level Tax tab when Local Sales Tax is enabled
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Taxation");
		pageObj.dashboardpage().checkUncheckAnyFlag("Local Sales Tax (Location Level)", "check");
		pageObj.subscriptionTaxationPage().clickOnGivenTab("Location Level Tax");
		Assert.assertTrue(utils.verifyPartOfURL(baseUrl + "/locations"));
		String locationTaxDescriptionText = pageObj.iframeConfigurationPage()
				.getElementText("locationPage.locationsPageTopMsg", "");
		Assert.assertEquals(locationTaxDescriptionText, MessagesConstants.locationLevelTaxDescription);
		utils.logit("pass",
				"Location Level Tax tab navigates to All Store Locations page and has the description text");
		locationTableHeadersCount = pageObj.segmentsBetaPage().verifyAndGetTextsCount(
				utils.getLocatorList("locationPage.locationTableHeaders"),
				dataSet.get("tableHeadersWithLocalTaxEnabled"));
		Assert.assertEquals(locationTableHeadersCount, "9");
		utils.logit("pass", "Location table includes Tax Rate (%) column.");
		String taxRate = pageObj.locationPage().getLocationStatus(dataSet.get("locationName1"), "Tax Rate (%)");
		Assert.assertTrue(taxRate.equals("-"));
		utils.logPass("Tax Rate (%) is empty for given location");

		// Verify existing filter and pagination remains operational
		pageObj.gamesPage().clickButton("locationPage.clearFilters");
		boolean isPaginationPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("adminUserPage.adminTablePagination"));
		Assert.assertTrue(isPaginationPresent);
		utils.logPass("Pagination is present for Location table as expected");
		pageObj.mobileconfigurationPage().selectDropdownValue(utils.getLocator("locationPage.locationCityDropdown"),
				"gurgaon");
		pageObj.gamesPage().clickButton("locationPage.submitButton");
		String city = pageObj.locationPage().getLocationStatus(dataSet.get("locationName2"), "City");
		Assert.assertEquals(city, "gurgaon");
		utils.logPass("Location can be filtered based on City dropdown selection");
		isPaginationPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("adminUserPage.adminTablePagination"));
		Assert.assertFalse(isPaginationPresent);
		utils.logPass("Pagination is not present as expected");

		// Verify Has Tax Rate filter with Yes, No and All options
		pageObj.gamesPage().clickButton("locationPage.clearFilters");
		pageObj.mobileconfigurationPage()
				.selectDropdownValue(utils.getLocator("locationPage.locationHasTaxRateDropdown"), "No");
		pageObj.gamesPage().clickButton("locationPage.submitButton");
		taxRate = pageObj.locationPage().getLocationStatus(dataSet.get("locationName1"), "Tax Rate (%)");
		Assert.assertTrue(taxRate.equals("-"));
		pageObj.mobileconfigurationPage()
				.selectDropdownValue(utils.getLocator("locationPage.locationHasTaxRateDropdown"), "Yes");
		pageObj.gamesPage().clickButton("locationPage.submitButton");
		taxRate = pageObj.locationPage().getLocationStatus(dataSet.get("locationName3"), "Tax Rate (%)");
		Assert.assertEquals(taxRate, "5.0");
		pageObj.mobileconfigurationPage()
				.selectDropdownValue(utils.getLocator("locationPage.locationHasTaxRateDropdown"), "All");
		pageObj.gamesPage().clickButton("locationPage.submitButton");
		taxRate = pageObj.locationPage().getLocationStatus(dataSet.get("locationName1"), "Tax Rate (%)");
		Assert.assertTrue(taxRate.equals("-"));
		taxRate = pageObj.locationPage().getLocationStatus(dataSet.get("locationName3"), "Tax Rate (%)");
		Assert.assertEquals(taxRate, "5.0");
		utils.logit("pass", "Locations can be filtered as per Has Tax Rate options");
	}

	@Test(description = "SQ-T6929 Verify Create Location API when Subscription Taxations flags are enabled/disabled"
			+ "SQ-T6932: Verify Update locations API when subscription Taxation flags are enabled/disabled"
			+ "SQ-T6931: Verify Get Location details API when subscription taxation flags are enabled/disabled", groups = {
					"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Jeevraj")
	public void T6929_verifyCreateLocationAPIWithNewTaxRateParameter() throws InterruptedException {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Check Enable Taxation Support In Subscription? flag
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Taxation Support In Subscription?", "check");
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Taxation");
		pageObj.subscriptionTaxationPage().clickOnGivenTab("Tax Rules");
		pageObj.dashboardpage().checkUncheckAnyFlag("Local Sales Tax (Location Level)", "check");

		// Create location API v2 validation with new tax_rate parameter.
		Response createAPIResponse = pageObj.endpoints().createLocationWithTaxRate(location_name, store_number,
				dataSet.get("apiKey"), dataSet.get("tax_rate"));
		Assert.assertEquals(createAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for create location api");
		TestListeners.extentTest.get()
				.pass("Create Location is successful with response code : " + createAPIResponse.statusCode());
		String api2TaxRateParameterValue = createAPIResponse.jsonPath().get("tax_rate").toString();
		String locationid = createAPIResponse.jsonPath().get("location_id").toString();
		Assert.assertEquals(api2TaxRateParameterValue, "32.0");
		utils.logit("pass", "Create location API validation with new tax rate parameter is successful");

		// Get Location details API v2 validation with tax_rate parameter.
		Response getLocationResponse = pageObj.endpoints().getLocationDetails(locationid, store_number,
				dataSet.get("apiKey"));
		Assert.assertEquals(getLocationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for create location api");
		String api2TaxRateParameterValueGet = getLocationResponse.jsonPath().get("[0].tax_rate").toString();
		Assert.assertEquals(api2TaxRateParameterValueGet, "32.0");
		utils.logit("pass", "Get location details API validation with new tax rate parameter is successful");

		// Check Update location API V2 validation to update tax_rate parameter when
		// Subscription Taxation flag is enabled.
		Response updateLocationTaxRate = pageObj.endpoints().updateLocationTaxRate(locationid, store_number,
				dataSet.get("apiKey"), dataSet.get("tax_rate_updated"));
		Assert.assertEquals(updateLocationTaxRate.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for create location api");
		TestListeners.extentTest.get()
				.pass("Update Location is successful with response code : " + updateLocationTaxRate.statusCode());
		String api2TaxRateUpdatedValue = updateLocationTaxRate.jsonPath().get("tax_rate").toString();
		Assert.assertEquals(api2TaxRateUpdatedValue, "50.9");
		utils.logit("pass", "Update location API validation with new tax rate parameter is successful");

		// Delete location
		Response deleteLocation = pageObj.endpoints().deleteLocation(locationid, store_number, dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocation.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not matched for delete location api");
		utils.logit("Location is deleted:" + location_name);

		// Check Create Location API response and error message when tax_rate parameter
		// null/empty value is passed.
		store_number = CreateDateTime.getTimeDateString();
		location_name = "Test Location" + store_number;
		Response createAPIErrorResponse = pageObj.endpoints().createLocationWithTaxRate(location_name, store_number,
				dataSet.get("apiKey"), dataSet.get("tax_rate_null"));
		Assert.assertEquals(createAPIErrorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code '422' did not matched for create location api");
		TestListeners.extentTest.get()
				.pass("Create Location is successful with response code : " + createAPIErrorResponse.statusCode());
		String createAPIErrorRes = createAPIErrorResponse.jsonPath().get("tax_rate").toString();
		Assert.assertEquals(createAPIErrorRes,
				"[Tax rate Location Level Subscription Plan Sales Tax is required and must be between 0 and 100.]");
		utils.logit("pass",
				"Create location API error validation with new tax rate parameter empty/null is successful");

		// Check create localtion API v2 response when Subscription taxation flag is
		// disabled.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Taxation Support In Subscription?", "uncheck");
		utils.waitTillPagePaceDone();

		// Check create location API v2 validation when Subscription taxation flag is
		// disabled and tax_rate parameter value is not null.
		store_number = CreateDateTime.getTimeDateString();
		location_name = "Test Location" + store_number;
		Response createLocationAPIResponse = pageObj.endpoints().createLocationWithTaxRate(location_name, store_number,
				dataSet.get("apiKey"), dataSet.get("tax_rate"));
		Assert.assertEquals(createLocationAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code '201' did not matched for create location api");
		TestListeners.extentTest.get()
				.pass("Create Location is successful with response code : " + createLocationAPIResponse.statusCode());
		String locationid2 = createLocationAPIResponse.jsonPath().get("location_id").toString();
		String body = createLocationAPIResponse.asString();
		Assert.assertTrue(body.contains("\"tax_rate\": null"),
				"tax_rate is not null in response when Subscription Taxation flag is disabled");
		utils.logit("pass",
				"Create location API response with new tax rate parameter value is null when Subscription Taxation flag is disabled");

		// Check Update location API V2 validation to update tax_rate parameter when
		// Subscription Taxation flag is disabled.
		Response updateLocationTaxRate2 = pageObj.endpoints().updateLocationTaxRate(locationid2, store_number,
				dataSet.get("apiKey"), dataSet.get("tax_rate_updated"));
		Assert.assertEquals(updateLocationTaxRate2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for create location api");
		String updateTaxbody = updateLocationTaxRate2.asString();
		Assert.assertTrue(updateTaxbody.contains("\"tax_rate\": null"),
				"tax_rate is not null in response when Subscription Taxation flag is disabled");
		utils.logit("pass",
				"Update location tax rate parameter value is null when Subscription Taxation flag is disabled");

		// delete location
		Response deleteLocation2 = pageObj.endpoints().deleteLocation(locationid2, store_number, dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocation2.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not matched for delete location api");
		utils.logit("Location is deleted:" + location_name);

	}

	@Test(description = "SQ-6935 Verify tax_value, discount_price and price in case of /api/mobile/subscriptions/purchase "
			+ "SQ-6933 Verify tax_value, discount_price and price in case of /api2/dashboard/subscriptions/purchase "
			+ "SQ-6934 Verify tax_value, discount_price and price in case of /api2/mobile/subscriptions/purchase", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shubham Gupta")
	public void T6933_VerifyTaxValueDiscountPrice() throws Exception {

		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Check Enable Taxation Support In Subscription? flag
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Taxation Support In Subscription?", "check");

		// user signup using api2
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionAPI2response = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("planID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime,
				new String[] { dataSet.get("taxValue"), dataSet.get("locationID"), dataSet.get("discountValue") });
		Assert.assertEquals(purchaseSubscriptionAPI2response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionAPI2response.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("planID"));

		String getPrice = "SELECT price from user_subscriptions where user_id = " + userID + " limit 1";
		String price = DBUtils.executeQueryAndGetColumnValue(env, getPrice, "price");
		logger.info("Price is--" + price);
		Assert.assertEquals(price, dataSet.get("spPrice"), "Subscription price is not matching");

		String getTaxValue = "SELECT tax_value from user_subscriptions where user_id = " + userID + " limit 1";
		String taxValue = DBUtils.executeQueryAndGetColumnValue(env, getTaxValue, "tax_value");
		logger.info("Tax Value is--" + taxValue);
		Assert.assertEquals(taxValue, dataSet.get("taxValue"), "Tax value is not matching");

		String getDiscountValue = "SELECT discount_value from user_subscriptions where user_id = " + userID
				+ " limit 1";
		String discountValue = DBUtils.executeQueryAndGetColumnValue(env, getDiscountValue, "discount_value");
		logger.info("Discount Value is--" + discountValue);
		Assert.assertEquals(discountValue, dataSet.get("discountValue"), "Discount value is not matching");

		// For /api2/dashboard/subscriptions/purchase
		String iFrameEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(iFrameEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse1.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail1.toLowerCase());
		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionAPI2response1 = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("adminToken"), dataSet.get("planID"), dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("spPrice"), endDateTime, "true", userID1,
				new String[] { dataSet.get("taxValue"), dataSet.get("locationID"), dataSet.get("discountValue") });
		Assert.assertEquals(purchaseSubscriptionAPI2response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String subscription_id1 = purchaseSubscriptionAPI2response1.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail1 + " purchased " + subscription_id1 + " Plan id = " + dataSet.get("planID"));

		String getPrice1 = "SELECT price from user_subscriptions where user_id = " + userID1 + " limit 1";
		String price1 = DBUtils.executeQueryAndGetColumnValue(env, getPrice1, "price");
		logger.info("Price is--" + price1);
		Assert.assertEquals(price1, dataSet.get("spPrice"), "Subscription price is not matching");

		String getTaxValue1 = "SELECT tax_value from user_subscriptions where user_id = " + userID1 + " limit 1";
		String taxValue1 = DBUtils.executeQueryAndGetColumnValue(env, getTaxValue1, "tax_value");
		logger.info("Tax Value is--" + taxValue1);
		Assert.assertEquals(taxValue1, dataSet.get("taxValue"), "Tax value is not matching");

		String getDiscountValue1 = "SELECT discount_value from user_subscriptions where user_id = " + userID1
				+ " limit 1";
		String discountValue1 = DBUtils.executeQueryAndGetColumnValue(env, getDiscountValue1, "discount_value");
		logger.info("Discount Value is--" + discountValue1);
		Assert.assertEquals(discountValue1, dataSet.get("discountValue"), "Discount value is not matching");

		// For /api/mobile/subscriptions/purchase
		String iFrameEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(iFrameEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse2.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail2.toLowerCase());
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionAPI2response2 = pageObj.endpoints().ApiSubscriptionPurchase(token2,
				dataSet.get("planID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime, "true",
				new String[] { dataSet.get("taxValue"), dataSet.get("locationID"), dataSet.get("discountValue") });
		Assert.assertEquals(purchaseSubscriptionAPI2response2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id2 = purchaseSubscriptionAPI2response2.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail2 + " purchased " + subscription_id2 + " Plan id = " + dataSet.get("planID"));

		String getPrice2 = "SELECT price from user_subscriptions where user_id = " + userID2 + " limit 1";
		String price2 = DBUtils.executeQueryAndGetColumnValue(env, getPrice2, "price");
		logger.info("Price is--" + price2);
		Assert.assertEquals(price2, dataSet.get("spPrice"), "Subscription price is not matching");

		String getTaxValue2 = "SELECT tax_value from user_subscriptions where user_id = " + userID2 + " limit 1";
		String taxValue2 = DBUtils.executeQueryAndGetColumnValue(env, getTaxValue2, "tax_value");
		logger.info("Tax Value is--" + taxValue2);
		Assert.assertEquals(taxValue2, dataSet.get("taxValue"), "Tax value is not matching");

		String getDiscountValue2 = "SELECT discount_value from user_subscriptions where user_id = " + userID2
				+ " limit 1";
		String discountValue2 = DBUtils.executeQueryAndGetColumnValue(env, getDiscountValue2, "discount_value");
		logger.info("Discount Value is--" + discountValue2);
		Assert.assertEquals(discountValue2, dataSet.get("discountValue"), "Discount value is not matching");

		utils.logPass("Tax value, discount value and subscription price are matching in DB");

	}
	
	@Test(description = "SQ-T7186 Verify /api/auth/subscription_taxes/applicable_taxes returns correct tax calculation"
	           +"SQ-T7187 Verify /api/mobile/subscription_taxes/applicable_taxes returns correct tax calculation and total price in api response."
	           +"SQ-T7188 Verify /api2/mobile/subscription_taxes/applicable_taxes returns correct tax calculation and total price in api response when Local sales tax option is selected under Tax rules", groups = {
				"regression", "dailyrun" }, priority = 8)
    @Owner(name = "Jeevraj Singh")
    public void T7186_verifySubscriptionTaxationApplicableTaxesAPI() {

        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Check Enable Taxation Support In Subscription? flag
        pageObj.dashboardpage().checkUncheckAnyFlag("Enable Subscriptions?", "check");
        pageObj.dashboardpage().checkUncheckAnyFlag("Enable Taxation Support In Subscription?", "check");
        pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Taxation");
        pageObj.subscriptionTaxationPage().clickOnGivenTab("Tax Rules");
        pageObj.dashboardpage().checkUncheckAnyFlag("Local Sales Tax (Location Level)", "check");

        // Verify /api/auth/subscription_taxes/applicable_taxes API
        utils.logit("== /api/auth/subscription_taxes/applicable_taxes ==");
        Response response = pageObj.endpoints().apiAuthSubscriptionTaxesApplicableTaxes(
        dataSet.get("client"),
        dataSet.get("plan_id"),
        dataSet.get("location_id"),
        dataSet.get("secret")
          );
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(response.jsonPath().getInt("plan_id"), Integer.parseInt(dataSet.get("plan_id")), "plan_id mismatch");
        Assert.assertEquals(response.jsonPath().getInt("location_id"), Integer.parseInt(dataSet.get("location_id")), "location_id mismatch");
        Assert.assertNotNull(response.jsonPath().get("base_price"), "base_price missing");
        Assert.assertNotNull(response.jsonPath().get("tax_rate"), "tax_rate missing");
        Assert.assertNotNull(response.jsonPath().get("tax_value"), "tax_value missing");
        Assert.assertNotNull(response.jsonPath().get("total_price"), "total_price missing");
        float basePrice = response.jsonPath().getFloat("base_price");
        float taxValue = response.jsonPath().getFloat("tax_value");
        float totalPrice = response.jsonPath().getFloat("total_price");
        Assert.assertEquals(totalPrice, basePrice + taxValue, "total price calculation mismatch");
        utils.logPass("/api/auth/subscription_taxes/applicable_taxes API is successful");

        // Verify /api/mobile/subscription_taxes/applicable_taxes API
        utils.logit("== /api/mobile/subscription_taxes/applicable_taxes ==");
        Response response1 = pageObj.endpoints().apiMobileSubscriptionTaxesApplicableTaxes(
        dataSet.get("plan_id"),
        dataSet.get("location_id"),
        dataSet.get("punchhAppKey"),
        dataSet.get("secret")
          );
        Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(response1.jsonPath().getInt("plan_id"), Integer.parseInt(dataSet.get("plan_id")), "plan_id mismatch");
        Assert.assertEquals(response1.jsonPath().getInt("location_id"), Integer.parseInt(dataSet.get("location_id")), "location_id mismatch");
        Assert.assertNotNull(response1.jsonPath().get("base_price"), "base_price missing");
        Assert.assertNotNull(response1.jsonPath().get("tax_rate"), "tax_rate missing");
        Assert.assertNotNull(response1.jsonPath().get("tax_value"), "tax_value missing");
        Assert.assertNotNull(response1.jsonPath().get("total_price"), "total_price missing");
        float basePrice1 = response1.jsonPath().getFloat("base_price");
        float taxValue1 = response1.jsonPath().getFloat("tax_value");
        float totalPrice1 = response1.jsonPath().getFloat("total_price");
        Assert.assertEquals(totalPrice1, basePrice1 + taxValue1, "total price calculation mismatch for mobile api1 subscription applicalbe taxes endpoint");
        utils.logPass("/api/mobile/subscription_taxes/applicable_taxes API is successful");

        // Verify /api2/mobile/subscription_taxes/applicable_taxes API
        utils.logit("== /api2/mobile/subscription_taxes/applicable_taxes ==");
        Response response2 = pageObj.endpoints().api2MobileSubscriptionTaxesApplicableTaxes(
        dataSet.get("client"),
        dataSet.get("plan_id"),
        dataSet.get("location_id"),
        dataSet.get("secret")
          );
        Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
        Assert.assertEquals(response2.jsonPath().getInt("plan_id"), Integer.parseInt(dataSet.get("plan_id")), "plan_id mismatch");
        Assert.assertEquals(response2.jsonPath().getInt("location_id"), Integer.parseInt(dataSet.get("location_id")), "location_id mismatch");
        Assert.assertNotNull(response2.jsonPath().get("base_price"), "base_price missing");
        Assert.assertNotNull(response2.jsonPath().get("tax_rate"), "tax_rate missing");
        Assert.assertNotNull(response2.jsonPath().get("tax_value"), "tax_value missing");
        Assert.assertNotNull(response2.jsonPath().get("total_price"), "total_price missing");
        float basePrice2 = response2.jsonPath().getFloat("base_price");
        float taxValue2 = response2.jsonPath().getFloat("tax_value");
        float totalPrice2 = response2.jsonPath().getFloat("total_price");
        Assert.assertEquals(totalPrice2, basePrice2 + taxValue2, "total price calculation mismatch for mobile api1 subscription applicalbe taxes endpoint");
        utils.logPass("/api2/mobile/subscription_taxes/applicable_taxes API is successful");

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