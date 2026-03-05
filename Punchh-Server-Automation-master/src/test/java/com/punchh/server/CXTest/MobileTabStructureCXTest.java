package com.punchh.server.CXTest;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import java.lang.reflect.Method;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileTabStructureCXTest {

	private static Logger logger = LogManager.getLogger(MobileTabStructureCXTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;
	String currentTime;

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

	@Test(description = "SQ-T2732: Validate that JSON for fields under App strings tab.", groups = { "regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T2732_verifyAppStringsJsonFields() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set the preferred language of Business to English
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.settingsPage().editPreferredLanguage("English");
		utils.logit("English is selected as the preferred language");
		
		// Go to Whitelabel > Mobile Configuration > App Strings tab
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().clickAppStringsBtn();

		// Verify the Help text for all App Strings fields
		boolean isHelpTextVerified = pageObj.mobileconfigurationPage()
				.validateTextAreaHelpMsg(dataSet.get("appStringsHintTextList"));
		Assert.assertTrue(isHelpTextVerified, "Help texts are not matched.");
		utils.logPass("Help texts for all App Strings fields are verified.");

		// UI Validation: Empty App Strings fields
		pageObj.mobileconfigurationPage().updateAppStringsFields("Clear", "", "", "");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		String message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(message, "Mobile configuration updated for App Strings", "Success message did not match.");

		// API Validation: Empty App Strings fields
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String actualLoyaltyAppStringsVal = metaApiResponse.jsonPath().getString("[0].loyalty_app_text");
		String actualOrderingAppStringsVal = metaApiResponse.jsonPath().getString("[0].ordering_app_text");
		String actualGiftCardAppStringsVal = metaApiResponse.jsonPath().getString("[0].giftcard_app_text");
		Assert.assertEquals(actualLoyaltyAppStringsVal, "", "Text is not empty.");
		Assert.assertEquals(actualOrderingAppStringsVal, "", "Text is not empty.");
		Assert.assertEquals(actualGiftCardAppStringsVal, "", "Text is not empty.");
		utils.logPass("UI and API validations are done when App Strings fields are empty.");

		// UI Validation: Valid App Strings fields
		String loyaltyAppStringsJson = dataSet.get("loyaltyAppStringsJson");
		String orderingAppStringsJson = dataSet.get("orderingAppStringsJson");
		String giftCardAppStringsJson = dataSet.get("giftCardAppStringsJson");
		pageObj.mobileconfigurationPage().updateAppStringsFields("Clear & Add Valid JSON", loyaltyAppStringsJson,
				orderingAppStringsJson, giftCardAppStringsJson);
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(message, "Mobile configuration updated for App Strings", "Success message did not match.");

		// API Validation: Valid App Strings fields
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		actualLoyaltyAppStringsVal = metaApiResponse.jsonPath().getString("[0].loyalty_app_text");
		actualOrderingAppStringsVal = metaApiResponse.jsonPath().getString("[0].ordering_app_text");
		actualGiftCardAppStringsVal = metaApiResponse.jsonPath().getString("[0].giftcard_app_text");
		Assert.assertEquals(actualLoyaltyAppStringsVal, loyaltyAppStringsJson, "Text did not match.");
		Assert.assertEquals(actualOrderingAppStringsVal, orderingAppStringsJson, "Text did not match.");
		Assert.assertEquals(actualGiftCardAppStringsVal, giftCardAppStringsJson, "Text did not match.");
		utils.logPass("UI and API validations are done when App Strings fields contain valid JSON.");

		// UI Validation: Invalid App Strings fields
		pageObj.mobileconfigurationPage().updateAppStringsFields("Append invalid JSON", " 1", " 2", " 3");
		pageObj.mobileconfigurationPage().clickUpdateButton();
		boolean inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Invalid JSON. Please correct it and try updating again", dataSet.get("redColorHexCode"), 3);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for App Strings", "Message did not match.");
		utils.logPass("UI validation is done when App Strings fields contain invalid JSON.");

		// Reset the fields and update
		message = pageObj.mobileconfigurationPage().validateResetLinks();
		Assert.assertEquals(message, "Mobile configuration updated for App Strings", "Message did not match.");
		utils.logPass("Reset links are verified for App Strings fields.");

	}

	@Test(description = "SQ-T4407: Validate the 'Display Fees as a List at Checkout' flag; "
			+ "SQ-T5188: Validate the dropdown menu with three options for 'Show Unavailable Menu Items' flag; "
			+ "SQ-T5307: Validate the 'Hide menu categories when no items are available' flag; "
			+ "SQ-T5582: Validate the 'Only show Cancel Order CTA for direct order cancellation' flag", groups = { "regression",
					"dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T4407_verifyShowFeesAsList() throws Exception {
		/*
		 * Discussed with Raja. Audit Log validations below are commented due to
		 * dependency on Lock Version. Refer SQ-1933.
		 */
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Whitelabel > Mobile Configuration > Ordering
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Ordering");
		/*
		String businessId = dataSet.get("businessId");
		int lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		// If checkboxes status are already as expected, do nothing
		String isShowFeesAsListBoxChecked = pageObj.mobileconfigurationPage()
				.isFlagChecked("Display Fees as a List at Checkout");
		String isShowUnavailableMenuItemsBoxChecked = pageObj.mobileconfigurationPage()
				.isFlagChecked("Show Unavailable Menu Items");
		String isHideEmptyMenuCategoriesBoxChecked = pageObj.mobileconfigurationPage()
				.isFlagChecked("Hide menu categories when no items are available");
		String isDirectCancelOnlyBoxChecked = pageObj.mobileconfigurationPage()
				.isFlagChecked("Only show Cancel Order CTA for direct order cancellation");
		String showFeesAsListAuditLogValue, showUnavailableItemsAuditLogValue, hideEmptyMenuCategoriesAuditLogValue,
				directCancelOnlyAuditLogValue;
		if (isShowFeesAsListBoxChecked.isEmpty() || isShowUnavailableMenuItemsBoxChecked.equals("true")
				|| isHideEmptyMenuCategoriesBoxChecked.isEmpty() || isDirectCancelOnlyBoxChecked.equals("true")) {
			// Check/Un-check the flags
			utils.logit(
					"Check: 'Display Fees as a List at Checkout', 'Hide menu categories when no items are available'; "
							+ "Uncheck: 'Show Unavailable Menu Items', 'Only show Cancel Order CTA for direct order cancellation'");
			utils.checkUncheckFlag("Display Fees as a List at Checkout", "check");
			utils.checkUncheckFlag("Show Unavailable Menu Items", "uncheck");
			utils.checkUncheckFlag("Hide menu categories when no items are available", "check");
			utils.checkUncheckFlag("Only show Cancel Order CTA for direct order cancellation", "uncheck");
			pageObj.mobileconfigurationPage().clickUpdateBtn();
			/*
			// In this case, verify the updates in Audit log
			pageObj.guestTimelinePage().clickAuditLog();
			showFeesAsListAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config", "SHOW_FEES_AS_LIST");
			Assert.assertEquals(showFeesAsListAuditLogValue, "true");
			showUnavailableItemsAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
							"SHOW_UNAVAILABLE_ITEMS");
			Assert.assertEquals(showUnavailableItemsAuditLogValue, "false");
			hideEmptyMenuCategoriesAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
							"HIDE_EMPTY_MENU_CATEGORIES");
			Assert.assertEquals(hideEmptyMenuCategoriesAuditLogValue, "true");
			directCancelOnlyAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config", "DIRECT_CANCEL_ONLY");
			Assert.assertEquals(directCancelOnlyAuditLogValue, "false");
			utils.logPass("Verified in the Audit log: "
					+ "1) SHOW_FEES_AS_LIST is having value 'true' as the 'Display Fees as a List at Checkout' flag is checked. "
					+ "2) SHOW_UNAVAILABLE_ITEMS is having value 'false' as the 'Show Unavailable Menu Items' flag is unchecked. "
					+ "3) HIDE_EMPTY_MENU_CATEGORIES is having value 'true' as the 'Hide menu categories when no items are available' flag is checked. "
					+ "4) DIRECT_CANCEL_ONLY is having value 'false' as the 'Only show Cancel Order CTA for direct order cancellation' flag is unchecked.");
			utils.navigateBackPage();
			lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
			*/
		}

		/*
		 * Verify that since 'Show Unavailable Menu Items' is unchecked, the 'Show
		 * Unavailable Menu Items - Options' dropdown's field should be disabled
		 */
		boolean isShowUnavailableMenuOptionsDropdownEnabled = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.showUnavailableMenuOptionsDropdown");
		Assert.assertFalse(isShowUnavailableMenuOptionsDropdownEnabled,
				"Show Unavailable Menu Items - Options dropdown is enabled.");
		utils.logPass("'Show Unavailable Menu Items - Options' dropdown is disabled as expected.");

		// Hit v1 Meta API and verify the updated values
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String orderingAppConfig = metaApiResponse.jsonPath().getString("[0].ordering_app_config");
		JSONObject orderingAppConfigJsonObject = new JSONObject(orderingAppConfig);
		String isShowFeesAsListChecked = orderingAppConfigJsonObject.get("SHOW_FEES_AS_LIST").toString();
		String isShowUnavailableItemsChecked = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_ITEMS").toString();
		String isShowUnavailableOptionsPresent = orderingAppConfigJsonObject.optString("SHOW_UNAVAILABLE_OPTION");
		String isHideEmptyMenuCategoriesChecked = orderingAppConfigJsonObject.get("HIDE_EMPTY_MENU_CATEGORIES")
				.toString();
		String isDirectCancelOnlyChecked = orderingAppConfigJsonObject.get("DIRECT_CANCEL_ONLY").toString();
		Assert.assertTrue(isShowUnavailableOptionsPresent.isEmpty(), "SHOW_UNAVAILABLE_OPTION is present.");
		Assert.assertEquals(isShowUnavailableItemsChecked, "false", "Show Unavailable Items is checked.");
		Assert.assertEquals(isShowFeesAsListChecked, "true", "Show Fees As List is not checked.");
		Assert.assertEquals(isHideEmptyMenuCategoriesChecked, "true", "Hide Empty Menu Categories is not checked.");
		Assert.assertEquals(isDirectCancelOnlyChecked, "false",
				"Only Show Cancel Order CTA for Direct Order Cancellation is checked.");
		utils.logPass(
				"Verified in the v1 Meta API response: 1) SHOW_FEES_AS_LIST is having value 'true' as the 'Display Fees as a List at Checkout' flag is checked. "
						+ "2) SHOW_UNAVAILABLE_ITEMS is having value 'false' as the 'Show Unavailable Menu Items' flag is unchecked. "
						+ "3) SHOW_UNAVAILABLE_OPTION key is not present in Meta API response. "
						+ "4) HIDE_EMPTY_MENU_CATEGORIES is having value 'true' as the 'Hide menu categories when no items are available' flag is checked. "
						+ "5) DIRECT_CANCEL_ONLY is having value 'false' as the 'Only show Cancel Order CTA for direct order cancellation' flag is unchecked.");
		
		// Check/Un-check the flags
		utils.logit(
				"Check: 'Show Unavailable Menu Items', 'Only show Cancel Order CTA for direct order cancellation', 'Show Availability Description in the Menu'; "
						+ "Uncheck: 'Display Fees as a List at Checkout', 'Hide menu categories when no items are available'");
		utils.checkUncheckFlag("Show Unavailable Menu Items", "check");
		utils.checkUncheckFlag("Display Fees as a List at Checkout", "uncheck");
		utils.checkUncheckFlag("Hide menu categories when no items are available", "uncheck");
		utils.checkUncheckFlag("Only show Cancel Order CTA for direct order cancellation", "check");
		utils.checkUncheckFlag("Show Availability Description in the Menu", "check");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		showFeesAsListAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "SHOW_FEES_AS_LIST");
		Assert.assertEquals(showFeesAsListAuditLogValue, "false");
		showUnavailableItemsAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "SHOW_UNAVAILABLE_ITEMS");
		Assert.assertEquals(showUnavailableItemsAuditLogValue, "true");
		hideEmptyMenuCategoriesAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
						"HIDE_EMPTY_MENU_CATEGORIES");
		Assert.assertEquals(hideEmptyMenuCategoriesAuditLogValue, "false");
		String showUnavailableOptionsAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
						"SHOW_UNAVAILABLE_OPTION");
		Assert.assertEquals(showUnavailableOptionsAuditLogValue, "show_unavailable_items");
		directCancelOnlyAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "DIRECT_CANCEL_ONLY");
		Assert.assertEquals(directCancelOnlyAuditLogValue, "true");
		String showAvailabilityDescriptionInMenuAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
						"SHOW_AVAILABILITY_DESC");
		Assert.assertEquals(showAvailabilityDescriptionInMenuAuditLogValue, "true");
		utils.logPass("Verified in the Audit log: "
				+ "1) SHOW_FEES_AS_LIST is having value 'false' as the 'Display Fees as a List at Checkout' flag is unchecked. "
				+ "2) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
				+ "3) HIDE_EMPTY_MENU_CATEGORIES is having value 'false' as the 'Hide menu categories when no items are available' flag is unchecked. "
				+ "4) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items' as 'Show All Unavailable Menu Items' is the selected option. "
				+ "5) DIRECT_CANCEL_ONLY is having value 'true' as the 'Only show Cancel Order CTA for direct order cancellation' flag is checked. "
				+ "6) SHOW_AVAILABILITY_DESC is having value 'true' as the 'Show Availability Description in the Menu' flag is checked.");
		utils.navigateBackPage();
		*/
		/*
		 * Verify that since 'Show Unavailable Menu Items' is checked now, the 'Show
		 * Unavailable Menu Items - Options' dropdown's field should be enabled
		 */
		isShowUnavailableMenuOptionsDropdownEnabled = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.showUnavailableMenuOptionsDropdown");
		Assert.assertTrue(isShowUnavailableMenuOptionsDropdownEnabled,
				"'Show Unavailable Menu Items - Options' dropdown is not enabled.");
		utils.logPass("'Show Unavailable Menu Items - Options' dropdown is enabled as expected.");

		/*
		 * Verify that default selected option in 'Show Unavailable Menu Items -
		 * Options' dropdown is 'Show All Unavailable Menu Items'
		 */
		String unavailableMenuItemsDropdownDefaultOption = (String) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("Show Unavailable Menu Items - Options");
		Assert.assertEquals(unavailableMenuItemsDropdownDefaultOption, "Show All Unavailable Menu Items");

		// Verify the hint texts
		boolean isShowUnavailableMenuItemsHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.showUnavailableMenuItemsHintText"));
		Assert.assertTrue(isShowUnavailableMenuItemsHintTextVerified, "Hint text is not matched.");
		boolean isShowUnavailableMenuOptionsHintTextVerified = utils.checkElementPresent(
				utils.getLocator("mobileConfigurationPage.showUnavailableMenuOptionsDropdownHintText"));
		Assert.assertTrue(isShowUnavailableMenuOptionsHintTextVerified, "Hint text is not matched.");
		boolean isShowAvailabilityDescriptionInMenuHintTextVerified = utils.checkElementPresent(
				utils.getLocator("mobileConfigurationPage.showAvailabilityDescriptionInMenuHintText"));
		Assert.assertTrue(isShowAvailabilityDescriptionInMenuHintTextVerified,
				"'Show Availability Description in the Menu' hint text is not matched.");
		boolean isShowFeesAsListHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.showFeesAsListHintText"));
		Assert.assertTrue(isShowFeesAsListHintTextVerified, "Hint text is not matched.");
		utils.logPass("All hint texts are verified.");

		// Hit v1 Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		orderingAppConfig = metaApiResponse.jsonPath().getString("[0].ordering_app_config");
		orderingAppConfigJsonObject = new JSONObject(orderingAppConfig);
		isShowFeesAsListChecked = orderingAppConfigJsonObject.get("SHOW_FEES_AS_LIST").toString();
		isShowUnavailableItemsChecked = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_ITEMS").toString();
		isHideEmptyMenuCategoriesChecked = orderingAppConfigJsonObject.get("HIDE_EMPTY_MENU_CATEGORIES").toString();
		String showUnavailableOptionsValue = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_OPTION").toString();
		isDirectCancelOnlyChecked = orderingAppConfigJsonObject.get("DIRECT_CANCEL_ONLY").toString();
		String isShowAvailabilityDescriptionInMenuChecked = orderingAppConfigJsonObject.get("SHOW_AVAILABILITY_DESC")
				.toString();
		Assert.assertEquals(isShowUnavailableItemsChecked, "true", "Show Unavailable Items is not checked.");
		Assert.assertEquals(showUnavailableOptionsValue, "show_unavailable_items");
		Assert.assertEquals(isShowFeesAsListChecked, "false", "Show Fees As List is checked.");
		Assert.assertEquals(isHideEmptyMenuCategoriesChecked, "false", "Hide Empty Menu Categories is checked.");
		Assert.assertEquals(isDirectCancelOnlyChecked, "true",
				"Only Show Cancel Order CTA for Direct Order Cancellation is not checked.");
		Assert.assertEquals(isShowAvailabilityDescriptionInMenuChecked, "true",
				"Show Availability Description in the Menu is not checked.");
		utils.logPass(
				"Verified in the v1 Meta API response: 1) SHOW_FEES_AS_LIST is having value 'false' as the 'Display Fees as a List at Checkout' flag is unchecked. "
						+ "2) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
						+ "3) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items' as 'Show All Unavailable Menu Items' is the selected option. "
						+ "4) HIDE_EMPTY_MENU_CATEGORIES is having value 'false' as the 'Hide menu categories when no items are available' flag is unchecked. "
						+ "5) DIRECT_CANCEL_ONLY is having value 'true' as the 'Only show Cancel Order CTA for direct order cancellation' flag is checked. "
						+ "6) SHOW_AVAILABILITY_DESC is having value 'true' as the 'Show Availability Description in the Menu' flag is checked.");
		/*
		lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		/*
		 * Select and update the 'Unavailable Menu Items by Date Range Only' option in
		 * 'Show Unavailable Menu Items - Options' dropdown. Un-check the 'Show
		 * Availability Description in the Menu' flag
		 */
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.showUnavailableMenuOptionsDropdown"),
				"Show Unavailable Menu Items by Date Range Only");
		utils.checkUncheckFlag("Show Availability Description in the Menu", "uncheck");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		showUnavailableItemsAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "SHOW_UNAVAILABLE_ITEMS");
		Assert.assertEquals(showUnavailableItemsAuditLogValue, "true");
		String showUnavailableOptionAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config",
						"SHOW_UNAVAILABLE_OPTION");
		Assert.assertEquals(showUnavailableOptionAuditLogValue, "show_unavailable_items_date_range");
		showAvailabilityDescriptionInMenuAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Ordering App Config", "SHOW_AVAILABILITY_DESC");
		Assert.assertEquals(showAvailabilityDescriptionInMenuAuditLogValue, "false");
		utils.logPass(
				"Verified in the Audit log: 1) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
						+ "2) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items_date_range' as 'Unavailable Menu Items by Date Range Only' is the selected option. "
						+ "3) SHOW_AVAILABILITY_DESC is having value 'false' as the 'Show Availability Description in the Menu' flag is unchecked.");
		utils.navigateBackPage();
		*/
		// Hit v1 Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		orderingAppConfig = metaApiResponse.jsonPath().getString("[0].ordering_app_config");
		orderingAppConfigJsonObject = new JSONObject(orderingAppConfig);
		isShowUnavailableItemsChecked = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_ITEMS").toString();
		showUnavailableOptionsValue = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_OPTION").toString();
		isShowAvailabilityDescriptionInMenuChecked = orderingAppConfigJsonObject.get("SHOW_AVAILABILITY_DESC")
				.toString();
		Assert.assertEquals(isShowUnavailableItemsChecked, "true", "Show Unavailable Items is not checked.");
		Assert.assertEquals(showUnavailableOptionsValue, "show_unavailable_items_date_range");
		Assert.assertEquals(isShowAvailabilityDescriptionInMenuChecked, "false",
				"Show Availability Description in the Menu is checked.");
		utils.logPass(
				"Verified in the v1 Meta API response: 1) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
						+ "2) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items_date_range' as 'Unavailable Menu Items by Date Range Only' is the selected option. "
						+ "3) SHOW_AVAILABILITY_DESC is having value 'false' as the 'Show Availability Description in the Menu' flag is unchecked.");
		/*
		lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		/*
		 * Select and update the 'Show Unavailable Menu Items by Day of Week / Time of
		 * Day Only' option in 'Show Unavailable Menu Items - Options' dropdown
		 */
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.showUnavailableMenuOptionsDropdown"),
				"Show Unavailable Menu Items by Day of Week / Time of Day Only");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		showUnavailableItemsAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "SHOW_UNAVAILABLE_ITEMS");
		Assert.assertEquals(showUnavailableItemsAuditLogValue, "true");
		showUnavailableOptionAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
				lockVersion, "Ordering App Config", "SHOW_UNAVAILABLE_OPTION");
		Assert.assertEquals(showUnavailableOptionAuditLogValue, "show_unavailable_items_week_time");
		utils.logPass(
				"Verified in the Audit log: 1) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
						+ "2) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items_week_time' as 'Show Unavailable Menu Items by Day of Week / Time of Day Only' is the selected option.");
		utils.navigateBackPage();
		*/
		// Hit v1 Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		orderingAppConfig = metaApiResponse.jsonPath().getString("[0].ordering_app_config");
		orderingAppConfigJsonObject = new JSONObject(orderingAppConfig);
		isShowUnavailableItemsChecked = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_ITEMS").toString();
		showUnavailableOptionsValue = orderingAppConfigJsonObject.get("SHOW_UNAVAILABLE_OPTION").toString();
		Assert.assertEquals(isShowUnavailableItemsChecked, "true", "Show Unavailable Items is not checked.");
		Assert.assertEquals(showUnavailableOptionsValue, "show_unavailable_items_week_time");
		utils.logPass(
				"Verified in the v1 Meta API response: 1) SHOW_UNAVAILABLE_ITEMS is having value 'true' as the 'Show Unavailable Menu Items' flag is checked. "
						+ "2) SHOW_UNAVAILABLE_OPTION is having value 'show_unavailable_items_week_time' as 'Show Unavailable Menu Items by Day of Week / Time of Day Only' is the selected option.");

		// Hit Meta API2 and verify that specific flags are not present
		Response metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		boolean isShowFeesAsListPresent = metaApi2Response.asString().contains("show_fees_as_list");
		boolean isDirectCancelOnlyPresent = metaApi2Response.asString().contains("direct_cancel_only");
		Assert.assertFalse(isShowFeesAsListPresent, "show_fees_as_list is present in Meta API2 response.");
		Assert.assertFalse(isDirectCancelOnlyPresent, "direct_cancel_only is present in Meta API2 response.");
		utils.logPass("Verified in the v2 Meta API response: 1) show_fees_as_list is not present. "
				+ "2) direct_cancel_only is not present.");

	}

	@Test(description = "SQ-T4290: Validate that additional loyalty, ordering & Gift card App configs fields.", groups = {"regression", "dailyrun"})
	@Owner(name = "Vaibhav Agnihotri")
	public void T4290_verifyLoyaltyOrderingGiftCardFields() throws InterruptedException {

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set the preferred language of Business to English
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.settingsPage().editPreferredLanguage("English");
		utils.logit("English is selected as the preferred language");
		
		// Go to Whitelabel > Mobile Configuration > Loyalty tab
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// Verify Additional Loyalty App Configs Field gets updated with valid JSON
		utils.logPass("== Loyalty tab ==");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		String validAddlLoyaltyAppConfigsJson = "{" + dataSet.get("addlLoyaltyAppConfigsJson") + "}";
		pageObj.mobileconfigurationPage().additionalAppConfig("Update",
				"mobileConfigurationPage.additionalAppStringsConfig", "Loyalty", validAddlLoyaltyAppConfigsJson);
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		String message = pageObj.mobileconfigurationPage().getSuccessMessage();
		String messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		Assert.assertEquals(message, "Mobile configuration updated for Loyalty", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("greenColorHexCode"), "Color did not match.");
		utils.logPass("Verified: Additional Loyalty App Configs Field gets updated with valid JSON.");

		// Verify Additional Loyalty App Configs Field does not update with invalid JSON
		String invalidAddlAppConfigsJson = dataSet.get("addlLoyaltyAppConfigsJson");
		pageObj.mobileconfigurationPage().additionalAppConfig("Update",
				"mobileConfigurationPage.additionalAppStringsConfig", "Loyalty", invalidAddlAppConfigsJson);
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		boolean inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Invalid JSON. Please correct it and try updating again", dataSet.get("redColorHexCode"), 1);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Loyalty", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		utils.logPass("Verified: Additional Loyalty App Configs Field does not update with invalid JSON.");

		/*
		 * Verify 'Check-In Frequency for App/Play Store Rating' does not update with
		 * invalid value and Existing Additional Loyalty App Configs Field remains
		 * unchanged
		 */
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage()
				.setCheckInFrequencyAppPlayStoreRating(dataSet.get("invalidCommaSeparatedValues"));
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Please input only numeric values separated by a comma", dataSet.get("redColorHexCode"), 1);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		String currentAddlAppConfigValue = pageObj.mobileconfigurationPage().additionalAppConfig("GetText",
				"mobileConfigurationPage.additionalAppStringsConfig", "Loyalty", "");
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Loyalty", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		Assert.assertTrue(currentAddlAppConfigValue.contains(dataSet.get("addlLoyaltyAppConfigsJson")));
		utils.logPass("Verified: (1) Check-In Frequency for App/Play Store Rating does not update with invalid value. "
				+ "(2) Existing Additional Loyalty App Configs Field remains unchanged");

		/*
		 * Verify 'Redeemable Point Thresholds' does not update with invalid value and
		 * Existing Additional Loyalty App Configs Field remains unchanged
		 */
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds(dataSet.get("invalidCommaSeparatedValues"));
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Please input only numeric values separated by a comma", dataSet.get("redColorHexCode"), 1);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		currentAddlAppConfigValue = pageObj.mobileconfigurationPage().additionalAppConfig("GetText",
				"mobileConfigurationPage.additionalAppStringsConfig", "Loyalty", "");
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Loyalty", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		Assert.assertTrue(currentAddlAppConfigValue.contains(dataSet.get("addlLoyaltyAppConfigsJson")));
		utils.logPass("Verified: (1) Redeemable Point Thresholds does not update with invalid value. "
				+ "(2) Existing Additional Loyalty App Configs Field remains unchanged");

		/*
		 * Verify both 'Check-In Frequency for App/Play Store Rating' and 'Redeemable
		 * Point Thresholds' do not update with invalid values simultaneously and
		 * existing Additional Loyalty App Configs Field remains unchanged
		 */
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage()
				.setCheckInFrequencyAppPlayStoreRating(dataSet.get("invalidCommaSeparatedValues"));
		pageObj.mobileconfigurationPage().setRedeemablePointThresholds(dataSet.get("invalidCommaSeparatedValues"));
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Please input only numeric values separated by a comma", dataSet.get("redColorHexCode"), 2);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		currentAddlAppConfigValue = pageObj.mobileconfigurationPage().additionalAppConfig("GetText",
				"mobileConfigurationPage.additionalAppStringsConfig", "Loyalty", "");
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Loyalty", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		Assert.assertTrue(currentAddlAppConfigValue.contains(dataSet.get("addlLoyaltyAppConfigsJson")));
		utils.logPass(
				"Verified: (1) Both Check-In Frequency for App/Play Store Rating and Redeemable Point Thresholds do not update with invalid values simultaneously. "
						+ "(2) Existing Additional Loyalty App Configs Field remains unchanged");

		// Verify Additional Ordering App Configs Field does not update with invalid
		// JSON
		utils.logPass("== Ordering tab ==");
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Ordering");
		pageObj.mobileconfigurationPage().additionalAppConfig("Update",
				"mobileConfigurationPage.additionalAppStringsConfig", "Ordering", invalidAddlAppConfigsJson);
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Invalid JSON. Please correct it and try updating again", dataSet.get("redColorHexCode"), 1);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Ordering", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		utils.logPass("Verified: Additional Ordering App Configs Field does not update with invalid JSON.");

		// Verify Additional Gift Card App Configs Field does not update with invalid
		// JSON
		utils.logPass("== Gift Card tab ==");
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Gift Card");
		pageObj.mobileconfigurationPage().additionalAppConfig("Update",
				"mobileConfigurationPage.additionalAppStringsConfig", "Gift Card", invalidAddlAppConfigsJson);
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText",
				"Invalid JSON. Please correct it and try updating again", dataSet.get("redColorHexCode"), 1);
		message = pageObj.mobileconfigurationPage().getSuccessMessage();
		messageHexCode = pageObj.mobileconfigurationPage()
				.getTextColour(utils.getLocator("locationPage.getSuccessErrorMessage"));
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		Assert.assertEquals(message, "Error updating Mobile configuration for Gift Card", "Message did not match.");
		Assert.assertEquals(messageHexCode, dataSet.get("redColorHexCode"), "Color did not match.");
		utils.logPass("Verified: Additional Gift Card App Configs Field does not update with invalid JSON.");

	}
	
	@Test(description = "SQ-T5580: Validate the 'Enable Gifting' flag; SQ-T5581: Validate the 'Auto-Delete Empty Card After Consolidation' flag", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T5580_verifyMobileConfigGiftCard() throws Exception {
		/*
		 * Discussed with Raja. Audit Log validations below are commented due to
		 * dependency on Lock Version. Refer SQ-1933.
		 */
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard > Miscellaneous Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable meta cache update on request", "check");

		// Go to Whitelabel > Mobile Configuration > Gift Card tab
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Gift Card");
		/*
		String businessId = dataSet.get("businessId");
		int lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		// If checkboxes status are already as expected, do nothing
		String isEnableGiftingBoxChecked = pageObj.mobileconfigurationPage().isFlagChecked("Enable Gifting");
		String isAutoDeleteCardAfterConsolidationBoxChecked = pageObj.mobileconfigurationPage()
				.isFlagChecked("Auto-Delete Empty Card After Consolidation");
		String showEnableGiftingAuditLogValue, autoDeleteCardAfterConsolidationAuditLogValue;
		if (isEnableGiftingBoxChecked.equals("true") || isAutoDeleteCardAfterConsolidationBoxChecked.equals("true")) {
			// Check/Un-check the flags
			utils.logit("Uncheck: 'Enable Gifting', 'Auto-Delete Empty Card After Consolidation'");
			utils.checkUncheckFlag("Enable Gifting", "uncheck");
			utils.checkUncheckFlag("Auto-Delete Empty Card After Consolidation", "uncheck");
			pageObj.mobileconfigurationPage().clickUpdateBtn();
			/*
			// In this case, verify the updates in Audit log
			pageObj.guestTimelinePage().clickAuditLog();
			showEnableGiftingAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Enable Gift Card Gifting", "");
			Assert.assertEquals(showEnableGiftingAuditLogValue, "false");
			autoDeleteCardAfterConsolidationAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Delete Gift Card After Consolidation", "");
			Assert.assertEquals(autoDeleteCardAfterConsolidationAuditLogValue, "false");
			utils.logPass("Verified in the Audit log: "
					+ "1) Enable Gift Card Gifting is having value 'false' as the flag is unchecked. "
					+ "2) Delete Gift Card After Consolidation is having value 'false' as the flag is unchecked.");
			utils.navigateBackPage();
			lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
			*/
		}

		// Hit v1 Meta API and verify the updated values
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String isMetaApiEnableGiftingChecked = metaApiResponse.jsonPath().getString("[0].enable_gift_card_gifting");
		String isMetaApiDeleteCardAfterConsolidationChecked = metaApiResponse.jsonPath()
				.getString("[0].delete_gift_card_after_consolidation");
		Assert.assertEquals(isMetaApiEnableGiftingChecked, "false", "Enable Gifting is checked.");
		Assert.assertEquals(isMetaApiDeleteCardAfterConsolidationChecked, "false",
				"Auto-Delete Empty Card After Consolidation is checked.");
		utils.logPass("Verified in the v1 Meta API response: "
				+ "1) enable_gift_card_gifting is having value 'false' as the flag is unchecked. "
				+ "2) delete_gift_card_after_consolidation is having value 'false' as the flag is unchecked.");

		// Hit v2 Meta API and verify the updated values
		Response metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		String isMetaApi2EnableGiftingChecked = metaApi2Response.jsonPath().getString("enable_gift_card_gifting");
		String isMetaApi2DeleteCardAfterConsolidationChecked = metaApi2Response.jsonPath()
				.getString("delete_gift_card_after_consolidation");
		Assert.assertEquals(isMetaApi2EnableGiftingChecked, "false", "Enable Gifting is checked.");
		Assert.assertEquals(isMetaApi2DeleteCardAfterConsolidationChecked, "false",
				"Auto-Delete Empty Card After Consolidation is checked.");
		utils.logPass("Verified in the v2 Meta API response: "
				+ "1) enable_gift_card_gifting is having value 'false' as the flag is unchecked. "
				+ "2) delete_gift_card_after_consolidation is having value 'false' as the flag is unchecked.");

		// Check/Un-check the flags
		utils.logit("Check: 'Enable Gifting', 'Auto-Delete Empty Card After Consolidation'");
		utils.checkUncheckFlag("Enable Gifting", "check");
		utils.checkUncheckFlag("Auto-Delete Empty Card After Consolidation", "check");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		showEnableGiftingAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Enable Gift Card Gifting", "");
		Assert.assertEquals(showEnableGiftingAuditLogValue, "true");
		autoDeleteCardAfterConsolidationAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Delete Gift Card After Consolidation", "");
		Assert.assertEquals(autoDeleteCardAfterConsolidationAuditLogValue, "true");
		utils.logPass("Verified in the Audit log: "
				+ "1) Enable Gift Card Gifting is having value 'true' as the flag is checked. "
				+ "2) Delete Gift Card After Consolidation is having value 'true' as the flag is checked.");
		utils.navigateBackPage();
		*/
		// Hit v1 Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		isMetaApiEnableGiftingChecked = metaApiResponse.jsonPath().getString("[0].enable_gift_card_gifting");
		isMetaApiDeleteCardAfterConsolidationChecked = metaApiResponse.jsonPath()
				.getString("[0].delete_gift_card_after_consolidation");
		Assert.assertEquals(isMetaApiEnableGiftingChecked, "true", "Enable Gifting is not checked.");
		Assert.assertEquals(isMetaApiDeleteCardAfterConsolidationChecked, "true",
				"Auto-Delete Empty Card After Consolidation is not checked.");
		utils.logPass("Verified in the v1 Meta API response: "
				+ "1) enable_gift_card_gifting is having value 'true' as the flag is checked. "
				+ "2) delete_gift_card_after_consolidation is having value 'true' as the flag is checked.");

		// Hit v2 Meta API and verify the updated values
		metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		isMetaApi2EnableGiftingChecked = metaApi2Response.jsonPath().getString("enable_gift_card_gifting");
		isMetaApi2DeleteCardAfterConsolidationChecked = metaApi2Response.jsonPath()
				.getString("delete_gift_card_after_consolidation");
		Assert.assertEquals(isMetaApi2EnableGiftingChecked, "true", "Enable Gifting is not checked.");
		Assert.assertEquals(isMetaApi2DeleteCardAfterConsolidationChecked, "true",
				"Auto-Delete Empty Card After Consolidation is not checked.");
		utils.logPass("Verified in the v2 Meta API response: "
				+ "1) enable_gift_card_gifting is having value 'true' as the flag is checked. "
				+ "2) delete_gift_card_after_consolidation is having value 'true' as the flag is checked.");

	}

	@Test(description = "SQ-T4233: Validate the 'Reward Screen CTAs' & 'Scanning Style' dropdowns; "
			+ "SQ-T5361: Validate the 'Distance Units Display' dropdown; "
			+ "SQ-T4234: Validate the 'Rate & Review Screen Visibility' dropdown", groups = { "regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T4233_verifyRewardScreenAndScanningStyle() throws Exception {
		/*
		 * Discussed with Raja. Audit Log validations below are commented due to
		 * dependency on Lock Version. Refer SQ-1933.
		 */
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Whitelabel > Mobile Configuration > Loyalty tab
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Loyalty");
		/*
		String businessId = dataSet.get("businessId");
		int lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		boolean isRateReviewScreenDropdownSingleSelect = pageObj.mobileconfigurationPage().isSingleSelectDropdown(
				utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"),
				"Rate & Review Screen Visibility");
		Assert.assertTrue(isRateReviewScreenDropdownSingleSelect,
				"Rate & Review Screen Visibility dropdown is not a single-select dropdown.");
		String rateReviewScreenSelectedOption = (String) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("Rate & Review Screen Visibility");
		String rewardScreenCtaSelectedOption = (String) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("Reward Screen CTAs");
		String rateReviewScreenAuditLogValue, rewardScreenCtaAuditLogValue;
		
		// If dropdowns doesn't have desired selections, then select the options
		if (!rateReviewScreenSelectedOption.equals("Full Rate & Review screen")
				|| !rewardScreenCtaSelectedOption.equals("One CTA (Scan)")) {
			pageObj.mobileconfigurationPage().selectDropdownValue(
					utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"), "Full Rate & Review screen");
			pageObj.mobileconfigurationPage().selectDropdownValue(
					utils.getLocator("mobileConfigurationPage.rewardScreenCtaDropdown"), "One CTA (Scan)");
			pageObj.mobileconfigurationPage().clickUpdateBtn();
			utils.logPass("Selected dropdown values are updated.");
			/*
			// In this case, verify the update in Audit log
			pageObj.guestTimelinePage().clickAuditLog();
			rateReviewScreenAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
					lockVersion, "Loyalty App Config", "FEEDBACK_SCREEN_TYPE");
			Assert.assertEquals(rateReviewScreenAuditLogValue, "full_rating");
			rewardScreenCtaAuditLogValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogsForSection(
					lockVersion, "Loyalty App Config", "REWARD_SCREEN_CTA_TYPE");
			Assert.assertEquals(rewardScreenCtaAuditLogValue, "single");
			utils.logPass(
					"Verified in the Audit log: 1) REWARD_SCREEN_CTA_TYPE is having value 'single' as the 'Reward Screen CTAs' dropdown is updated with 'One CTA (Scan)' option. "
							+ "2) FEEDBACK_SCREEN_TYPE is having value 'full_rating' as the 'Rate & Review Screen Visibility' dropdown is updated with 'Full Rate & Review screen' option.");
			utils.navigateBackPage();
			lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
			*/
		}

		// Hit Meta API and verify the updated values
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String loyaltyAppConfig = metaApiResponse.jsonPath().getString("[0].loyalty_app_config");
		JSONObject loyaltyAppConfigJsonObject = new JSONObject(loyaltyAppConfig);
		String rewardScreenCtaType = loyaltyAppConfigJsonObject.get("REWARD_SCREEN_CTA_TYPE").toString();
		String scanningStyle = loyaltyAppConfigJsonObject.optString("SCANNING_STYLE");
		String rateReviewScreenType = loyaltyAppConfigJsonObject.get("FEEDBACK_SCREEN_TYPE").toString();
		Assert.assertEquals(rateReviewScreenType, "full_rating", "Rate & Review Screen Visibility is not updated.");
		Assert.assertEquals(rewardScreenCtaType, "single", "Reward Screen CTA Type is not present.");
		Assert.assertTrue(scanningStyle.isEmpty(), "Scanning Style is present.");
		utils.logPass(
				"REWARD_SCREEN_CTA_TYPE is having value 'single' and SCANNING_STYLE is not present in API response.");

		// Verify the 'Scanning Style' dropdown is not enabled
		boolean isScanningStyleDropdownEnabled = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.scanningStyleDropdown");
		Assert.assertFalse(isScanningStyleDropdownEnabled, "Scanning Style dropdown is enabled.");
		utils.logPass("Scanning Style dropdown is not enabled as expected.");

		// Verify dropdowns are having expected options
		List<String> rateReviewScreenDropdownOptions = utils.getAllVisibleTextFromDropdwon(
				utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"));
		Assert.assertEquals(rateReviewScreenDropdownOptions, dataSet.get("rateReviewScreenDropdownList"));
		List<String> distanceUnitsDisplayDropdowns = utils.getAllVisibleTextFromDropdwon(
				utils.getLocator("mobileConfigurationPage.distanceUnitsDisplayDropdown"));
		Assert.assertEquals(distanceUnitsDisplayDropdowns, dataSet.get("distanceUnitsDisplayDropdownList"));
		utils.logPass("Verified the dropdown options.");

		/*
		 * Under 'Reward Screen CTAs' dropdown, select 'Two CTAs (Earn and Redeem)'
		 * option and verify that 'Scanning Style' dropdown gets enabled
		 */
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.rewardScreenCtaDropdown"), "Two CTAs (Earn and Redeem)");
		isScanningStyleDropdownEnabled = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.scanningStyleDropdown");
		Assert.assertTrue(isScanningStyleDropdownEnabled, "Scanning Style dropdown is not enabled.");
		utils.logPass("Scanning Style dropdown is enabled as expected.");

		// Verify the error message when 'Scanning Style' dropdown is not selected
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		boolean inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.inlineErrorText", "Selection is required", dataSet.get("redColorHexCode"), 1);
		Assert.assertTrue(inlineErrorMessage, "Inline error message or color did not match.");
		utils.logPass("Inline error message is verified when Scanning Style is not selected.");
		
		// Select dropdown values
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"), "Experience rating only");
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.scanningStyleDropdown"), "Consolidated Scanning");
		pageObj.mobileconfigurationPage()
				.selectDropdownValue(utils.getLocator("mobileConfigurationPage.distanceUnitsDisplayDropdown"), "Miles");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		utils.logPass("Selected dropdown values are updated.");
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		rateReviewScreenAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "FEEDBACK_SCREEN_TYPE");
		Assert.assertEquals(rateReviewScreenAuditLogValue, "star_rating");
		rewardScreenCtaAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "REWARD_SCREEN_CTA_TYPE");
		Assert.assertEquals(rewardScreenCtaAuditLogValue, "double");
		String scanningStyleAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "SCANNING_STYLE");
		Assert.assertEquals(scanningStyleAuditLogValue, "consolidated");
		String distanceUnitsDisplayAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "DISTANCE_UNITS_DISPLAY");
		Assert.assertEquals(distanceUnitsDisplayAuditLogValue, "mi");
		utils.logPass(
				"Verified in the Audit log: 1) REWARD_SCREEN_CTA_TYPE is having value 'double' as the 'Reward Screen CTAs' dropdown is updated with 'Two CTAs (Earn and Redeem)' option. "
						+ "2) SCANNING_STYLE is having value 'consolidated' as the 'Scanning Style' dropdown is updated with 'Consolidated Scanning' option. "
						+ "3) DISTANCE_UNITS_DISPLAY is having value 'mi' as the 'Distance Units Display' dropdown is updated with 'Miles' option. "
						+ "4) FEEDBACK_SCREEN_TYPE is having value 'star_rating' as the 'Rate & Review Screen Visibility' dropdown is updated with 'Experience rating only' option.");
		utils.navigateBackPage();
		*/
		// Hit Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		loyaltyAppConfig = metaApiResponse.jsonPath().getString("[0].loyalty_app_config");
		loyaltyAppConfigJsonObject = new JSONObject(loyaltyAppConfig);
		rewardScreenCtaType = loyaltyAppConfigJsonObject.get("REWARD_SCREEN_CTA_TYPE").toString();
		scanningStyle = loyaltyAppConfigJsonObject.get("SCANNING_STYLE").toString();
		String distanceUnitsDisplay = loyaltyAppConfigJsonObject.get("DISTANCE_UNITS_DISPLAY").toString();
		rateReviewScreenType = loyaltyAppConfigJsonObject.get("FEEDBACK_SCREEN_TYPE").toString();
		Assert.assertEquals(rateReviewScreenType, "star_rating", "Rate & Review Screen Visibility is not updated.");
		Assert.assertEquals(rewardScreenCtaType, "double", "Reward Screen CTA Type is not present.");
		Assert.assertEquals(scanningStyle, "consolidated", "Scanning Style is not present.");
		Assert.assertEquals(distanceUnitsDisplay, "mi", "Distance Units Display is not present.");
		utils.logPass(
				"Verified that following are present in v1 Meta API response: 1) REWARD_SCREEN_CTA_TYPE with value 'double'. "
						+ "2) SCANNING_STYLE with value 'consolidated'. 3) DISTANCE_UNITS_DISPLAY with value 'mi'. 4) FEEDBACK_SCREEN_TYPE with value 'star_rating'.");
		/*
		lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		// Select dropdown values
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"), "Text field only");
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.scanningStyleDropdown"), "Classic Scanning");
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.distanceUnitsDisplayDropdown"), "Kilometers");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		utils.logPass("Selected dropdown values are updated.");
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		rateReviewScreenAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "FEEDBACK_SCREEN_TYPE");
		Assert.assertEquals(rateReviewScreenAuditLogValue, "text_rating");
		rewardScreenCtaAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "REWARD_SCREEN_CTA_TYPE");
		Assert.assertEquals(rewardScreenCtaAuditLogValue, "double");
		scanningStyleAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "SCANNING_STYLE");
		Assert.assertEquals(scanningStyleAuditLogValue, "traditional");
		distanceUnitsDisplayAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "DISTANCE_UNITS_DISPLAY");
		Assert.assertEquals(distanceUnitsDisplayAuditLogValue, "km");
		utils.logPass(
				"Verified in the Audit log: 1) REWARD_SCREEN_CTA_TYPE is having value 'double' as the 'Reward Screen CTAs' dropdown is updated with 'Two CTAs (Earn and Redeem)' option. "
						+ "2) SCANNING_STYLE is having value 'traditional' as the 'Scanning Style' dropdown is updated with 'Classic Scanning' option. "
						+ "3) DISTANCE_UNITS_DISPLAY is having value 'km' as the 'Distance Units Display' dropdown is updated with 'Kilometers' option. "
						+ "4) FEEDBACK_SCREEN_TYPE is having value 'text_rating' as the 'Rate & Review Screen Visibility' dropdown is updated with 'Text field only' option.");
		utils.navigateBackPage();
		*/
		// Hit Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		loyaltyAppConfig = metaApiResponse.jsonPath().getString("[0].loyalty_app_config");
		loyaltyAppConfigJsonObject = new JSONObject(loyaltyAppConfig);
		rewardScreenCtaType = loyaltyAppConfigJsonObject.get("REWARD_SCREEN_CTA_TYPE").toString();
		scanningStyle = loyaltyAppConfigJsonObject.get("SCANNING_STYLE").toString();
		distanceUnitsDisplay = loyaltyAppConfigJsonObject.get("DISTANCE_UNITS_DISPLAY").toString();
		rateReviewScreenType = loyaltyAppConfigJsonObject.get("FEEDBACK_SCREEN_TYPE").toString();
		Assert.assertEquals(rateReviewScreenType, "text_rating", "Rate & Review Screen Visibility is not updated.");
		Assert.assertEquals(rewardScreenCtaType, "double", "Reward Screen CTA Type is not present.");
		Assert.assertEquals(scanningStyle, "traditional", "Scanning Style is not present.");
		Assert.assertEquals(distanceUnitsDisplay, "km", "Distance Units Display is not present.");
		utils.logPass(
				"Verified that following are present in v1 Meta API response: 1) REWARD_SCREEN_CTA_TYPE with value 'double'. "
						+ "2) SCANNING_STYLE with value 'traditional'. 3) DISTANCE_UNITS_DISPLAY with value 'km'. 4) FEEDBACK_SCREEN_TYPE with value 'text_rating'.");
		/*
		lockVersion = getMobileConfigurationNextLockVersion(env, businessId, 3);
		*/
		// Select dropdown values
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.rateReviewScreenDropdown"), "No Rate & Review screen");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		utils.logPass("Selected dropdown values are updated.");
		/*
		// Verify the updates in Audit log
		pageObj.guestTimelinePage().clickAuditLog();
		rateReviewScreenAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogsForSection(lockVersion, "Loyalty App Config", "FEEDBACK_SCREEN_TYPE");
		Assert.assertEquals(rateReviewScreenAuditLogValue, "none");
		utils.logPass(
				"Verified in the Audit log: 1) FEEDBACK_SCREEN_TYPE is having value 'none' as the 'Rate & Review Screen Visibility' dropdown is updated with 'No Rate & Review screen' option.");
		utils.navigateBackPage();
		*/
		// Hit Meta API and verify the updated values
		metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		loyaltyAppConfig = metaApiResponse.jsonPath().getString("[0].loyalty_app_config");
		loyaltyAppConfigJsonObject = new JSONObject(loyaltyAppConfig);
		rateReviewScreenType = loyaltyAppConfigJsonObject.get("FEEDBACK_SCREEN_TYPE").toString();
		Assert.assertEquals(rateReviewScreenType, "none", "Rate & Review Screen Visibility is not updated.");
		utils.logPass(
				"Verified that following are present in v1 Meta API response: 1) FEEDBACK_SCREEN_TYPE with value 'none'.");

		// Verify the hint texts
		boolean isRewardScreenCtaHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.rewardScreenCtaHintText"));
		Assert.assertTrue(isRewardScreenCtaHintTextVerified, "Reward Screen CTAs hint text is not matched.");
		boolean isRateReviewScreenHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.rateReviewScreenHintText"));
		Assert.assertTrue(isRateReviewScreenHintTextVerified,
				"Rate & Review Screen Visibility hint text is not matched.");
		boolean isScanningStyleHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.scanningStyleHintText"));
		Assert.assertTrue(isScanningStyleHintTextVerified, "Scanning Style hint text is not matched.");
		utils.logPass("Hint texts are verified.");

		/*
		 * Verify that when user switches from 'Two CTAs (Earn and Redeem)' to 'One CTA
		 * (Scan)', then no value gets saved in 'Scanning Style' dropdown.
		 */
		pageObj.mobileconfigurationPage().selectDropdownValue(
				utils.getLocator("mobileConfigurationPage.rewardScreenCtaDropdown"), "One CTA (Scan)");
		String scanningStyleDisabledValue = (String) pageObj.dashboardpage().getSelectedValueFromDropdown("Scanning Style");
		Assert.assertTrue(scanningStyleDisabledValue.isEmpty(), "Scanning Style dropdown value is not empty.");
		utils.logPass(
				"Verified that no value is saved in 'Scanning Style' dropdown when user switches to 'One CTA (Scan)'.");
		
		// Hit Meta API2 and verify that specific flags are not present
		Response metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		boolean isDistanceUnitsDisplayPresent = metaApi2Response.asString().contains("distance_units_display");
		Assert.assertFalse(isDistanceUnitsDisplayPresent, "distance_units_display is present in Meta API2 response.");
		utils.logPass("Verified in the v2 Meta API response: 1) distance_units_display is not present.");

		// Verify 'Request experience rating on feedback screen on mobile app?' flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Feedback");
		pageObj.dashboardpage().navigateToTabs("Feedback Rating");
		pageObj.dashboardpage().checkUncheckAnyFlag("Request experience rating on feedback screen on mobile app?", "check");
		utils.logPass("'Request experience rating on feedback screen on mobile app?' is verified as checked.");

	}
	
	@Test(description = "SQ-T5523: Validate the 'PAR Ordering Geocode Key' under Global Configurations; "
			+ "SQ-T3458: Validate that newly added fields under menu tab", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T5523_verifyParOrderingGeocodeKey() throws Exception {

		// Navigate to Settings > Global Configuration
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");

		// Verify that 'PAR Ordering Geocode Key' field is displayed
		boolean isParOrderingGeocodeKeyDisplayed = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.parOrderingGeocodeKey"));
		Assert.assertTrue(isParOrderingGeocodeKeyDisplayed, "'PAR Ordering Geocode Key' is not present.");
		utils.logPass("'PAR Ordering Geocode Key' is present.");

		// Verify the 'PAR Ordering Geocode Key' hint text
		boolean isParOrderingGeocodeKeyHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.parOrderingGeocodeKeyHintText"));
		Assert.assertTrue(isParOrderingGeocodeKeyHintTextVerified,
				"PAR Ordering Geocode Key hint text is not matched.");
		utils.logPass("PAR Ordering Geocode Key hint text is verified.");

		/*
		 * Negative case: Validate that the PAR Ordering geocode key field should not be
		 * available under Whitelabel > Services > PAR Ordering tab.
		 */
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("PAR Ordering");
		boolean isMenuGeocodeKeyPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.menuGeocodeKey"));
		Assert.assertFalse(isMenuGeocodeKeyPresent, "PAR Ordering Geocode Key field is present.");
		utils.logPass(
				"Negative case: Verified that PAR Ordering Geocode Key field is not present under Whitelabel > Services > PAR Ordering tab.");

		boolean isParOrderingApiVersionPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("mobileConfigurationPage.parOrderingApiVersion"));
		Assert.assertTrue(isParOrderingApiVersionPresent);
		boolean isParOrderingApiVersionHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.parOrderingApiVersionHintText"));
		Assert.assertTrue(isParOrderingApiVersionHintTextVerified);
		boolean isParOrderingClientIdPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("mobileConfigurationPage.parOrderingClientId"));
		Assert.assertTrue(isParOrderingClientIdPresent);
		boolean isParOrderingClientSecretPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("mobileConfigurationPage.parOrderingClientSecret"));
		Assert.assertTrue(isParOrderingClientSecretPresent);
		utils.logit("pass", "Verified that API Version, Client ID and Client Secret fields are present");

		boolean isClientSecretHashedValue = pageObj.whitelabelPage()
				.isHashedValue("whitelabelPage.menuClientSecretKey");
		Assert.assertTrue(isClientSecretHashedValue, "PAR Ordering Client Secret is not hashed");
		utils.logit("pass", "PAR Ordering Client Secret is hashed");

		pageObj.whitelabelPage().editMenuclientScret("123123");
		pageObj.whitelabelPage().editMenuAPIVersion("abcdefg");
		pageObj.whitelabelPage().clickMenuUpdate();

		String errorMsg = pageObj.whitelabelPage().getErrorMessage();
		Assert.assertEquals(errorMsg, "Error in saving PAR Ordering settings. PAR Ordering API Version is an invalid",
				"Error message is not same");
		utils.logit("pass", "Verified error message when invalid PAR Ordering API version is entered");

		// Hit Meta API and verify the key-value presence/absence
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String menuGeocodeKey = metaApiResponse.jsonPath().getString("[0].menu_configs.menu_geocode_key");
		String menuConfigs = metaApiResponse.jsonPath().getString("[0].menu_configs");
		Assert.assertEquals(menuGeocodeKey, dataSet.get("menuGeocodeKey"));
		Assert.assertTrue(menuConfigs.contains("menu_api_version"));
		Assert.assertFalse(menuConfigs.contains("menu_client_id"));
		Assert.assertFalse(menuConfigs.contains("menu_client_secret"));
		utils.logit("pass",
				"Verified in the v1 Meta API response: menu_geocode_key and menu_api_version are present, whereas menu_client_id and menu_client_secret are absent.");

		// Hit Meta API2 and verify the key-value presence/absence
		Response metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		String menuConfigs2 = metaApi2Response.jsonPath().get("menu_configs").toString();
		Assert.assertFalse(menuConfigs2.contains("menu_geocode_key"));
		Assert.assertTrue(menuConfigs2.contains("menu_api_version"));
		Assert.assertFalse(menuConfigs2.contains("menu_client_id"));
		Assert.assertFalse(menuConfigs2.contains("menu_client_secret"));
		utils.logit("pass",
				"Verified in the v2 Meta API response: menu_api_version is present, whereas menu_geocode_key, menu_client_id and menu_client_secret are absent.");

	}
	
	// Gets the Mobile Configuration's lock version from DB and increments it by 1
	public int getMobileConfigurationNextLockVersion(String env, String businessId, int pollingCount)
			throws Exception {
		String lockVersionQuery = "SELECT lock_version FROM mobile_configurations WHERE business_id = " + businessId;
		String lockVersionCurrent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lockVersionQuery,
				"lock_version", pollingCount);
		int lockVersionAfterUpdate = Integer.parseInt(lockVersionCurrent) + 1;
		return lockVersionAfterUpdate;
	}
	
	@SuppressWarnings("unchecked")
	@Test(description = "SQ-T5530: Validate that values should be seen in the V1 & V2 meta-API response for values selected under “Preferred Gift Cards” field; "
			+ "SQ-T2757: Validate that Guest Creation Strategy field hint text", groups = { "regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T5530_verifyPreferredGiftCards() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// SQ-T5530 starts
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");

		// Verify the values in dropdown
		List<String> selectedOptions = (List<String>) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("Preferred Gift Cards");
		List<String> preferredGiftCardsUiExpectedList = Arrays.asList(dataSet.get("preferredGiftCardsUi").split(","));
		List<String> preferredGiftCardsApiExpectedList = Arrays.asList(dataSet.get("preferredGiftCardsApi").split(","));

		// If dropdown doesn't have desired selections, then select the options
		if (!selectedOptions.containsAll(preferredGiftCardsUiExpectedList)
				|| selectedOptions.size() != preferredGiftCardsUiExpectedList.size()) {
			for (String option : preferredGiftCardsUiExpectedList) {
				pageObj.mobileconfigurationPage().selectDropdownValue(
						utils.getLocator("mobileConfigurationPage.preferredGiftCardsDropdown"), option);
			}
			pageObj.mobileconfigurationPage().clickUpdateButton();
			selectedOptions = (List<String>) pageObj.dashboardpage()
					.getSelectedValueFromDropdown("Preferred Gift Cards");

			// In this case, verify the update in Audit log
			pageObj.guestTimelinePage().clickAuditLog();
			String hideGcPaymentInSsfAuditLogValue = pageObj.mobileconfigurationPage()
					.verifyUpdatedConfigInAuditLogs("Preferred Gift Card Types", "");
			Assert.assertEquals(hideGcPaymentInSsfAuditLogValue, dataSet.get("preferredGiftCardsApi"));
			utils.logPass("Verified updated 'Preferred Gift Card Types' in Audit log.");
			utils.navigateBackPage();
		}
		Assert.assertTrue(utils.compareList(selectedOptions, preferredGiftCardsUiExpectedList),
				"Expected GCs are not present in Preferred Gift Cards dropdown.");
		TestListeners.extentTest.get()
				.pass("Verified that Preferred Gift Cards dropdown is having expected selected options in UI: "
						+ dataSet.get("preferredGiftCardsUi"));
		logger.info("Verified that Preferred Gift Cards dropdown is having expected selected options in UI: "
				+ dataSet.get("preferredGiftCardsUi"));

		// Verify the values in Meta v1 and v2 APIs
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String v1PreferredGiftCards = metaApiResponse.jsonPath().getString("[0].preferred_gift_cards");
		List<String> v1PreferredGiftCardsList = Arrays.asList(v1PreferredGiftCards.split(","));
		Assert.assertTrue(utils.compareList(v1PreferredGiftCardsList, preferredGiftCardsApiExpectedList),
				"Expected GCs are not present in v1 Meta API response.");
		Response metaApi2Response = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApi2Response.statusCode(), 200);
		String v2PreferredGiftCards = metaApi2Response.jsonPath().getString("preferred_gift_cards");
		List<String> v2PreferredGiftCardsList = Arrays.asList(v2PreferredGiftCards.split(","));
		Assert.assertTrue(utils.compareList(v2PreferredGiftCardsList, preferredGiftCardsApiExpectedList),
				"Expected GCs are not present in v2 Meta API response.");
		utils.logPass("Verified in both v1 and v2 Meta API response that "
				+ preferredGiftCardsApiExpectedList + " are present under Preferred Gift Cards.");

		// SQ-T5530 ends and SQ-T2757 starts
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable OLO Webhooks?", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		// Verify the 'Guest Creation Strategy' hint text
		boolean isShowUnavailableMenuItemsHintTextVerified = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.guestCreationStrategyHintText"));
		Assert.assertTrue(isShowUnavailableMenuItemsHintTextVerified, "Hint text is not matched.");
		utils.logPass("Verified Guest Creation Strategy hint text.");
		// SQ-T2757 ends
	}
	
	@Test(description = "SQ-T2322 Verify tab structure ui for mobile configuration page", dataProvider = "TestDataProvider", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T2322_verifytabstructureuiformobileconfigurationpage(String slug, String client, String secret)
			throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(slug);
		// Click Menu Items
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		// Validate all tabs on the page
		List<String> tabNames = pageObj.mobileconfigurationPage().validateTabLinks();
		System.out.println(tabNames);
		List<String> tabs = (Arrays.asList("", "Disclaimer", "Miscellaneous URLs", "Extended Settings", "App Messages",
				"Miscellaneous Fields", "App Strings", "App Configs", "Account Deletion"));

		String disclaimerDetails = pageObj.mobileconfigurationPage().getPageSource();
		SoftAssert softassert = new SoftAssert();
		Assert.assertTrue(disclaimerDetails.contains("Disclaimer"), "Disclaimer is not present on page");
		Assert.assertTrue(disclaimerDetails.contains("Game Disclaimer"), "Game Disclaimer is not present on page");
		Assert.assertTrue(disclaimerDetails.contains("Upgrade Disclaimer"),
				"Upgrade Disclaimer is not present on page");
		Assert.assertTrue(disclaimerDetails.contains("Challenges Disclaimer"),
				"Challenges Disclaimer is not present on page");
		Assert.assertTrue(disclaimerDetails.contains("Earning Disclaimer"),
				"Earning Disclaimer is not present on page");
		Assert.assertTrue(disclaimerDetails.contains("Earning Description"),
				"Earning Description is not present on page");

		pageObj.mobileconfigurationPage().clickMiscellaneousURLBtn();
		String miscellaneousURLDetails = pageObj.mobileconfigurationPage().getPageSource();
		Assert.assertTrue(miscellaneousURLDetails.contains("FAQ URL"), "FAQ URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Instagram Page"), "Instagram Page is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Order URL"), "Order URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Order URL for Single Sign On"),
				"Order URL for Single Sign On is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Menu URL"), "Menu URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Privacy Policy URL"),
				"Privacy URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Terms and Conditions URL"),
				"Terms and Conditions URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Nutrition URL"), "Nutrition URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Catering URL"), "Catering URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Careers URL"),
				"Careers URL for Single Sign On is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Games Rules URL"),
				"Games Rules URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("User Referral URL"),
				"User Referral URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Twitter Handle"), "Twitter Handle is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Facebook Page"), "Facebook Page is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Youtube URL"), "Youtube URL is not present on page");
		Assert.assertTrue(miscellaneousURLDetails.contains("Restaurant Feedback URL"),
				"Restaurant Feedback URL is not present on page");

		pageObj.mobileconfigurationPage().clickExtendedSettingsBtn();
		String extendedSettingsDetails = pageObj.mobileconfigurationPage().getPageSource();
		Assert.assertTrue(extendedSettingsDetails.contains("Support Email"), "Support Email is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Restaurant Support Email Address"),
				"Restaurant Support Email Address is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Facebook Signup Text"),
				"Facebook Signup Text is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Facebook Invitees Limit"),
				"Refer a Friend Message is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Promotional Offer"),
				"Promotional Offer is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Max Map Radius"), "Max Map Radius is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Share Invite Code Title"),
				"Share Invite Code Title is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Share Invite Code Message"),
				"Share Invite Code Message is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Share Invite Code Description"),
				"Share Invite Code Description is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Game Score Sharing on Facebook Template"),
				"Game Score Sharing on Facebook Template for Single Sign On is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Enable Promotional Coupons?"),
				"Enable Promotional Coupons? is not present on page");
		Assert.assertTrue(extendedSettingsDetails.contains("Enable Gift Cards?"),
				"Enable Gift Cards? is not present on page");
		pageObj.mobileconfigurationPage().clickMiscellaneousFieldsBtn();
		String miscellaneousFieldsDetails = pageObj.mobileconfigurationPage().getPageSource();
		Assert.assertTrue(miscellaneousFieldsDetails.contains("Miscellaneous 1"),
				"Miscellaneous 1 is not present on page");
		Assert.assertTrue(miscellaneousFieldsDetails.contains("Miscellaneous 2"),
				"Miscellaneous 2 is not present on page");
		Assert.assertTrue(miscellaneousFieldsDetails.contains("Miscellaneous 3"),
				"Miscellaneous 3 is not present on page");
		Assert.assertTrue(miscellaneousFieldsDetails.contains("Miscellaneous 4"),
				"Miscellaneous 4 is not present on page");
		pageObj.mobileconfigurationPage().clickMiscellaneousURLBtn();
		pageObj.mobileconfigurationPage().validateInvalidUpdation();
		pageObj.mobileconfigurationPage().validUpdation();

		// Meta API validation
		Response cardsResponse = pageObj.endpoints().Api1Cards(client, secret);
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse.statusCode());
		String faqUrl = cardsResponse.jsonPath().get("[0].faq_url").toString();
		String gameDisclaimer = cardsResponse.jsonPath().get("[0].game_disclaimer").toString();
		String supportEmail = cardsResponse.jsonPath().get("[0].support_email_address").toString();

		Assert.assertTrue(faqUrl.equalsIgnoreCase("https://testurl.com/"), "faq url did not matched with api response");
		Assert.assertTrue(gameDisclaimer.equalsIgnoreCase("Test Game Disclaimer"),
				"game disclaimer did not matched with api response");
		Assert.assertTrue(supportEmail.equalsIgnoreCase("moerewards@moes.com"),
				"support email did not matched with api response");
		softassert.assertAll();
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				// {"slug","client","secret"},
				{ "autotwo", "605e1ee5829746f51c01a314b42fc77eb36de98f944aac1366c6ed5dcdfb2804",
						"28dfb57b0f8def64818f7471591eef16cec90be65b119cb387e882685a4b72fa" },
				/*
				 * { "auntieannes",
				 * "bbb1261f61730b78ef1ee3d7c6b19b6b4518be2485f8ed68e9b0f1fcd0d92f88",
				 * "e0f3e0281ddddae8d157efa01cbecaba4c3f452a2020ac72254ff945fa612689" }, {
				 * "lchin", "2885df2597aeadeb9e6783dea1e35cacc254e3c1c84e06f671944d6cc0db68c1",
				 * "83efd76de93125e192fc5c2be5cb99785fd4343fb9bad6579934d760924058c8" }, {
				 * "caferio",
				 * "4d05f4d0e242de7ea9510695358bf2fbb6da1613758b267a643535e4090d6b9c",
				 * "8f0ba73e7712cab184ab5eb714b83433831c06d260e1543be7155e044f82223f" }, {
				 * "slimchickens",
				 * "e58e6cff7afc73eb2330e62b71f31948cb7386bb07c72cbc3ec8407aefd7ab1e",
				 * "6f3906f4dd02929566cfe16804948bf52c13a27adb0ad5a260e0e9dd354239ca" }, {
				 * "freebirds",
				 * "ca29693e96bc96ef4b5bb4a803a3537f2abbb7d340ccd7e578d060aaa9c66179",
				 * "824b448119c84ad74a5b2cc02e7011cc9f8db4c1fcdc54317940a3d448af6c5d" }, {
				 * "fuzzystaco",
				 * "96da970d4503c66c49a36fbd8c7fa932ffa485441d7d51c223d77d9ea4b639af",
				 * "e199b1809f93c5827ce57f7b25839845c782b4830632e3bc853ea6cf20f75d23" }, {
				 * "graeters",
				 * "39b8609ae825de9cf7efba1f989a073be3378d1ea9a2b411e95d10eb57903901",
				 * "dc5f18c944a4c599f3864aad9dcbb4394e584fad4e7915fcf1c2ad6d58b2939f" }, {
				 * "HaagenDazs",
				 * "cc806438355ac0fd18b622809f6935114eaf8ee52c8758a9ee04a18805f82e83",
				 * "beabaee682d980e80b617d62b109935d43777f09ed52240571b9a35db1d41470" }, {
				 * "jeremiahs",
				 * "8ce89d26246aeaf41706af39e8e861fed5bde084006227504f6acfda6a3e96fd",
				 * "ff73f6db3553da14887807b92c89a49aceac51a60348604cfaf40cfe846cdb3f" }, {
				 * "lees", "6017df892be42bc55d6d48a0e62156cec5af6326b9d5c0312bff55f1032b78ff",
				 * "b9f9e73800003e1ff0d8078a86161eedf0ce9127eb2011c2fd866979d692f76f" }, {
				 * "modpizza",
				 * "362a8431cd306e926a9755aa2ffbc7f2c7c0407c103abc044e4c9a0b275fc915",
				 * "feedc85b0bb90d966647e9aeaa9e718bfedbf9c15925d83981ea10535bba6988" },
				 */
		};

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