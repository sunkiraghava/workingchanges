package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileConfigurationAndWhitelabelNewAddedFieldsTest {

	private static Logger logger = LogManager.getLogger(MobileConfigurationAndWhitelabelNewAddedFieldsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	SeleniumUtilities selUtils;

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
		selUtils = new SeleniumUtilities(driver);
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// author=Anant
	@Test(description = "SQ-T3189 Validate that newly added subsection title "
			+ "Ordering Custom Text (Header/Footer Text for Ordering) on Mobile configuration page", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3189_ValidateSubsectionTitleOrderingCustomText() throws InterruptedException {

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to mobile configuration and click ordering app config
		// pageObj.menupage().clickWhiteLabelMenu();
		// pageObj.menupage().clickMobileConfigurationLink();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().clickOrderingAppConfigBtn();

		// verify ordering custom text is visible
		boolean orderingCustomTextPresent = pageObj.mobileconfigurationPage().orderingCustomTextPresent();
		Assert.assertTrue(orderingCustomTextPresent, "Ordering Custom Text is not visible");
		TestListeners.extentTest.get().pass("Ordering Custom Text is visible and is bold");

		// uncheck the ordering custom text checkbox

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("menu_custom_text_en", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("item_custom_text_en", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("nested_item_custom_text_en", "uncheck");
		logger.info("uncheck the ordering custom text checkbox");
		TestListeners.extentTest.get().info("uncheck the ordering custom text checkbox");

		pageObj.mobileconfigurationPage().greyAreaOrderingCustomText();
		TestListeners.extentTest.get().info("dropdown and textarea are greyout when checkbox is disabled");

		// check the ordering custom text checkbox
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("menu_custom_text_en", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("item_custom_text_en", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("nested_item_custom_text_en", "check");

		Boolean asterik1 = pageObj.mobileconfigurationPage().checkAsteriskOnFlag("Menu Custom Text");
		Assert.assertTrue(asterik1, "asterisk is not visible for the Menu Custom Text");
		logger.info("asterisk is visible for the Menu Custom Text");
		TestListeners.extentTest.get().info("asterisk is visible for the Menu Custom Text");

		Boolean asterik2 = pageObj.mobileconfigurationPage().checkAsteriskOnFlag("Item Custom Text");
		Assert.assertTrue(asterik2, "asterisk is not visible for the Item Custom Text");
		logger.info("asterisk is visible for the Item Custom Text");
		TestListeners.extentTest.get().info("asterisk is visible for the Item Custom Text");

		Boolean asterik3 = pageObj.mobileconfigurationPage().checkAsteriskOnFlag("Nested Item Custom Text");
		Assert.assertTrue(asterik3, "asterisk is not visible for the Nested Item Custom Text");
		logger.info("asterisk is visible for the Nested Item Custom Text");
		TestListeners.extentTest.get().info("asterisk is visible for the Nested Item Custom Text");

		pageObj.mobileconfigurationPage().verifyNegativeCounter(370);
		TestListeners.extentTest.get().pass("negative value is displayed when there are more than 350 characters");

		// verify black color
		pageObj.mobileconfigurationPage().enterTextinTextArea(318);
		pageObj.mobileconfigurationPage().verifyCounterColor("#6c757d");
		logger.info("text area value is less than 320 and counter color is black");
		TestListeners.extentTest.get().pass("text area value is less than 320 and counter color is black");

		// verify orange color
		pageObj.mobileconfigurationPage().enterTextinTextArea(349);
		pageObj.mobileconfigurationPage().verifyCounterColor("#ffad00");
		logger.info("text area value is more than 320 and counter color is orange");
		TestListeners.extentTest.get().pass("text area value is more than 320 and counter color is orange");

		// verify textarea should be scroll
		pageObj.mobileconfigurationPage().enterTextinTextArea(520);
		pageObj.mobileconfigurationPage().textAreaScrollable();
		TestListeners.extentTest.get().pass("text area is scrollable");

		// hint text
		String menuCustomHintText = pageObj.mobileconfigurationPage()
				.orderingCustomTextHint("menu_custom_text_textbox_en");
		Assert.assertEquals(menuCustomHintText, "Displays text at the top level of the online ordering menu.",
				"menu custom hint text value is not same");
		TestListeners.extentTest.get().pass("menu custom hint text value is not same");

		String itemCustomHintText = pageObj.mobileconfigurationPage()
				.orderingCustomTextHint("item_custom_text_textbox_en");
		Assert.assertEquals(itemCustomHintText,
				"Displays text on an individual item screen in the online ordering menu.",
				"item custom hint text value is not same");
		TestListeners.extentTest.get().pass("item custom hint text value is not same");

		String nestedItemHintText = pageObj.mobileconfigurationPage()
				.orderingCustomTextHint("nested_item_custom_text_textbox_en");
		Assert.assertEquals(nestedItemHintText,
				"Displays text on an individual nested item screen (e.g. modifiers) in the online ordering menu.",
				"nested custom hint text value is not same");
		TestListeners.extentTest.get().pass("nested custom hint text value is not same");

		// dropdown validation
		pageObj.mobileconfigurationPage().oderingCustomdropdownValidation();
		logger.info("drop down values are verified");
		TestListeners.extentTest.get().pass("all the values are present in the dropdown");

		pageObj.mobileconfigurationPage().enterInvalidDetailsOrderingCustomText();
		TestListeners.extentTest.get()
				.pass("error msg is displayed when invalid details are field for the Ordering Custom Text");

		// pageObj.menupage().clickDashboardMenu();
		// pageObj.menupage().clickWhiteLabelMenu();
		// pageObj.menupage().clickMobileConfigurationLink();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().clickOrderingAppConfigBtn();
		// clear Excluded Hours of Operation option
		pageObj.mobileconfigurationPage().clearExcludedHoursOfOperationField();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("menu_custom_text_en", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("item_custom_text_en", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("nested_item_custom_text_en", "check");

		String textAreaValue = pageObj.mobileconfigurationPage().enterValidDetailsOrderingCustomText().trim();

		int counter = 0;
		String MENU_CUSTOM_TEXT_header_scrollable_Value;
		JSONObject json;
		do {
			// v1 Meta API
			Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(cardsResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
			String val = cardsResponse.jsonPath().getString("ordering_app_config").replace("[{", "{").replace("}]",
					"}");
			json = new JSONObject(val);
			MENU_CUSTOM_TEXT_header_scrollable_Value = json.getJSONObject("MENU_CUSTOM_TEXT").getString("value").trim();
			counter++;
			Thread.sleep(1000);
		} while (!MENU_CUSTOM_TEXT_header_scrollable_Value.equals(textAreaValue) && (counter != 10));

		Assert.assertEquals(MENU_CUSTOM_TEXT_header_scrollable_Value, textAreaValue,
				textAreaValue + " Value is not matched in API response for MENU_CUSTOM_TEXT");
		TestListeners.extentTest.get().pass("updated value for the menu custom text showing in the API");

		String ITEM_CUSTOM_TEXT_footer_sticky_value = json.getJSONObject("ITEM_CUSTOM_TEXT").getString("value").trim();
		Assert.assertEquals(ITEM_CUSTOM_TEXT_footer_sticky_value, textAreaValue,
				textAreaValue + " Value is not matched in API response for ITEM_CUSTOM_TEX");
		TestListeners.extentTest.get().pass("updated value for the item custom text showing in the API");

		String NESTED_ITEM_CUSTOM_TEXT_footer_scrollable_value = json.getJSONObject("NESTED_ITEM_CUSTOM_TEXT")
				.getString("value").trim();
		Assert.assertEquals(NESTED_ITEM_CUSTOM_TEXT_footer_scrollable_value, textAreaValue,
				textAreaValue + " Value is not matched in API response for NESTED_ITEM_CUSTOM_TEXT");
		TestListeners.extentTest.get().pass("updated value for the item custom text showing in the API");

	}

	// author=Anant
	// Merged this test case's steps into T3958_SuccessErrorMsgNotFlickering
	//@Test(description = "SQ-T3309 Validate that only valid URLs accepted in Youtube URL & Restaurant Feedback URL fields.", enabled = true, groups = { "regression", "dailyrun" }, priority = 0)
	public void T3309_ValidateValiURLsInFields() throws InterruptedException {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to mobile configuration and click ordering app config
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().clickMiscellaneousURLBtn();

		// invalid url
		pageObj.mobileconfigurationPage().youtubeURlField(dataSet.get("invalidURL"));
		TestListeners.extentTest.get().info("invalid url entered in Youtube URL field");
		logger.info("invalid url entered in Youtube URL field");

		pageObj.mobileconfigurationPage().restaurantFeedbackURlField(dataSet.get("invalidURL"));
		logger.info("invalid url entered in Resturant Feedback URL field");
		TestListeners.extentTest.get().info("invalid url entered in Resturant Feedback URL field");

		pageObj.mobileconfigurationPage().clickUpdateBtn();

		String errorMsg = pageObj.mobileconfigurationPage().getErrorMessage();

		Assert.assertEquals(errorMsg, "Error updating Mobile configuration for Miscellaneous URLs");
		logger.info("error msg is correct");
		TestListeners.extentTest.get().info("error msg is correct");

		String errorMsgColor = pageObj.mobileconfigurationPage().msgColor("mobileConfigurationPage.errorMsg");
		Assert.assertEquals(errorMsgColor, "#ff0000", "error msg is not of red color");
		logger.info("error msg color is red");
		String isInvalidMsg = pageObj.mobileconfigurationPage().isInvalidMsg();
		Assert.assertEquals(isInvalidMsg, "is invalid", "not showing that url is invalid");

		logger.info("Error msg is displayed when entered the invalid URL");
		TestListeners.extentTest.get().pass("Error msg is displayed when entered the invalid URL");

		// valid url
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous URLs");
		pageObj.mobileconfigurationPage().youtubeURlField(dataSet.get("validURL"));
		logger.info("valid url entered in Youtube URL field");
		TestListeners.extentTest.get().pass("valid url entered in Youtube URL field");

		pageObj.mobileconfigurationPage().restaurantFeedbackURlField(dataSet.get("validURL"));
		logger.info("valid url entered in Resturant Feedback URL field");
		TestListeners.extentTest.get().pass("valid url entered in Youtube URL field");

		pageObj.mobileconfigurationPage().clickUpdateBtn();

		String getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Miscellaneous URLs");
		logger.info("success msg is correct");

		String getSuccessMsgColor = pageObj.mobileconfigurationPage().msgColor("mobileConfigurationPage.successMsg");
		Assert.assertEquals(getSuccessMsgColor, "#155724", "success msg is not of green color");
		logger.info("success msg is of green color");

		TestListeners.extentTest.get().pass("Success msg is displayed when entered the valid URL");

	}

	// author=Anant
	// Merged this test case's steps into T5523_verifyParOrderingGeocodeKey
	//@Test(description = "SQ-T3458 Validate that newly added fields under menu tab.+ ", enabled = true, groups = {"regression", "dailyrun" }, priority = 0)
	public void T3458_newAddedfieldsUnderMenuTab() throws InterruptedException {
		// Login to instance

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to whitelabel->service->menu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.whitelabelPage().clickMenuBtn();

		// This field is moved to global config and hence cannot be validated through
		// automation so
		// disabled this part after discussing with Raja
		// hint text
		/*
		 * String menuGeocodeKey =
		 * pageObj.whitelabelPage().MenuTextHint("menu_service_menu_geocode_key");
		 * Assert.assertEquals(menuGeocodeKey,
		 * "Used by mobile app in MENU online ordering flow to convert delivery addresses to lat/long to determine nearest available locations."
		 * , "hint text is not equal for menu geo code");
		 * TestListeners.extentTest.get().pass("hint text is equal for menu geo code");
		 */

		String menuAPiVersion = pageObj.whitelabelPage().MenuTextHint("menu_service_menu_api_version");
		Assert.assertEquals(menuAPiVersion,
				"Identifies which version of PAR Ordering API should be used by Punchh mobile apps.",
				"hint text is not equal for api version");
		TestListeners.extentTest.get().pass("hint text is equal for api version");
		logger.info("hint text is equal for api version");

		/*
		 * boolean isGeocodeHashedValue =
		 * pageObj.whitelabelPage().isHashedValue("whitelabelPage.menuGeocodeKey");
		 * Assert.assertTrue(isGeocodeHashedValue, "MENU Geocode Key is not hashed");
		 * TestListeners.extentTest.get().pass("MENU Geocode Key is hashed");
		 */

		boolean isClientSecretHashedValue = pageObj.whitelabelPage()
				.isHashedValue("whitelabelPage.menuClientSecretKey");
		Assert.assertTrue(isClientSecretHashedValue, "MENU Client Secret is not hashed");
		TestListeners.extentTest.get().pass("MENU Client Secret is hashed");
		logger.info("MENU Client Secret is hashed");

		// pageObj.whitelabelPage().editMenuGeocode("123444");
		pageObj.whitelabelPage().editMenuclientScret("123123");
		pageObj.whitelabelPage().editMenuAPIVersion("dfbhbnumjden");
		pageObj.whitelabelPage().clickMenuUpdate();

		String getErrorMsg = pageObj.whitelabelPage().getErrorMessage();
		Assert.assertEquals(getErrorMsg,
				"Error in saving PAR Ordering settings. PAR Ordering API Version is an invalid",
				"Error msg is not same");
		TestListeners.extentTest.get().pass("error msg is validated");
		logger.info("error msg is validated");
		// v1 API
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		String menuConfigs = cardsResponse.jsonPath().get("menu_configs").toString();
		// Assert.assertTrue(menuConfigs.contains("menu_geocode_key"), "v1 API does not
		// contain
		// menu_geocode_keys");
		Assert.assertTrue(menuConfigs.contains("menu_api_version"), "v1 API does not contain menu_api_version");
		Assert.assertFalse(menuConfigs.contains("menu_client_id"), "v1 API conatin menu_client_id");
		Assert.assertFalse(menuConfigs.contains("menu_client_sceret"), "v1 API conatin menu_client_sceret");

		// v2 api
		Response cardsResponse2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse2.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		String menuConfigs2 = cardsResponse.jsonPath().get("menu_configs").toString();
		// Assert.assertTrue(menuConfigs2.contains("menu_geocode_key"), "v2 API does not
		// contain
		// menu_geocode_keys");
		Assert.assertTrue(menuConfigs2.contains("menu_api_version"), "v2 API does not contain menu_api_version");
		Assert.assertFalse(menuConfigs2.contains("menu_client_id"), "v2 API conatin menu_client_id");
		Assert.assertFalse(menuConfigs2.contains("menu_client_sceret"), "v2 API conatin menu_client_sceret");
	}

	// author=Anant
	@Test(description = "SQ-T3242 Validate that the Moving Account Deletion Fields to Mobile Configurations  "
			+ "SQ-T3243 Validate that Language support for the Account Deletion. ", groups = { "regression", "unstable",
					"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3242_MovingAccountDeletionFieldsToMobileConfigurations() throws InterruptedException {
		String msg = "";
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto cockpit->guest->misc config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		String guestPageSource = pageObj.cockpitGuestPage().getPageSource();

		Assert.assertFalse(guestPageSource.contains("Mobile Guest Offloading Flow"));
		Assert.assertFalse(guestPageSource.contains("Deletion Request Email"));
		Assert.assertFalse(guestPageSource.contains("Deletion Request Email Subject"));
		Assert.assertFalse(guestPageSource.contains("Deletion Request Email Body"));
		Assert.assertFalse(guestPageSource.contains("Deletion Description"));
		Assert.assertFalse(guestPageSource.contains("Deactivation Description"));

		logger.info("all this fields are not available in the cockpit-> guest -> misc configs");
		TestListeners.extentTest.get().pass("all this fields are not available in the cockpit-> guest -> misc configs");

		// go to mobile configuration and click Account delection
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Account Deletion");

		// uncheck the checkbox
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_ios_guest_account_deactivation",
				"uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_android_guest_account_deactivation",
				"uncheck");
		pageObj.mobileconfigurationPage().clickUpdateButton();
		msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"Mobile configuration updated is not Updated");

		boolean flag = false;
		int counter = 0;
		do {
			try {
				utils.longWaitInSeconds(1);
				Response cardResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
				Assert.assertEquals(cardResponse1.statusCode(), 200);

				String iosGuestAccountDeactivation = cardResponse1.jsonPath().get("[0].ios_guest_account_deactivation")
						.toString().replace("[", "").replace("]", "");
				Assert.assertEquals(iosGuestAccountDeactivation, "false", "");

				String androidGuestAccountDeactivation = cardResponse1.jsonPath()
						.get("[0].android_guest_account_deactivation").toString().replace("[", "").replace("]", "");
				Assert.assertEquals(androidGuestAccountDeactivation, "false", "");

				TestListeners.extentTest.get().pass("when checkbox are uncheck then "
						+ "ios_guest_account_deactivation and android_guest_account_deactivation value are coming false ");
				flag = true;
				break;

			} catch (AssertionError ae) {
				flag = false;
				counter++;
				selUtils.longWait(3000);
			}

		} while (flag || (counter <= 10));

		Assert.assertTrue(flag, "values did not get updated in the api even after pooling");

		// check the checkbox
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_ios_guest_account_deactivation",
				"check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_android_guest_account_deactivation",
				"check");
		msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"Mobile configuration updated is not Updated");

		// app delection type dropdown
		List<String> appDelectionDropdown = (Arrays.asList("Allow Direct Deletion", "Request by Email", "None"));
		List<String> iosLst = pageObj.mobileconfigurationPage().appDelectionTypeDropdown("ios");
		List<String> androidLst = pageObj.mobileconfigurationPage().appDelectionTypeDropdown("android");

		Assert.assertTrue(appDelectionDropdown.equals(androidLst), "androidLst value are not match");
		Assert.assertTrue(appDelectionDropdown.equals(iosLst), "iosLst value are not match");

		TestListeners.extentTest.get().pass("drop down lst value are same");

		// select 'None' from appDelectionTypeDropdown

		pageObj.mobileconfigurationPage().iosAppDelectionTypeNew(dataSet.get("option1"));
		pageObj.mobileconfigurationPage().clickUpdateButton();
		pageObj.mobileconfigurationPage().androidAppDelectionTyp(dataSet.get("option1"));
		pageObj.mobileconfigurationPage().clickUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Account Deletion");
		// pageObj.mobileconfigurationPage().clickToggleAccountDelDropdown();

		boolean isdelectionRequestEmail = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delectionRequestEmail");
		boolean isdelectionRequestEmailSubject = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delectionRequestEmailSubject");
		boolean isdelectionRequestEmailBody = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delectionRequestEmailBody");
		boolean isdelectionDescription = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delDescription");

		Assert.assertFalse(isdelectionRequestEmail, "field is not greyout");
		Assert.assertFalse(isdelectionRequestEmailSubject, "field is not greyout");
		Assert.assertFalse(isdelectionRequestEmailBody, "field is not greyout");
		Assert.assertFalse(isdelectionDescription, "field is not greyout");

		logger.info("Verified fields are greyout when 'None' is selected from the dropdown");
		TestListeners.extentTest.get().pass("Verified fields are greyout when 'None' is selected from the dropdown");

		// select 'Request by Email' from appDelectionTypeDropdown
		pageObj.mobileconfigurationPage().iosAppDelectionTypeNew(dataSet.get("option2"));
		pageObj.mobileconfigurationPage().enterRequestEmail(dataSet.get("requestEmail"));
		pageObj.mobileconfigurationPage().clickUpdateButton();
		pageObj.mobileconfigurationPage().refereshPage();
		// pageObj.dashboardpage().navigateToTabs("Account Deletion");
		pageObj.mobileconfigurationPage().androidAppDelectionTypeNew(dataSet.get("option2"));
		pageObj.mobileconfigurationPage().enterRequestEmail(dataSet.get("requestEmail"));
		msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"Mobile configuration updated is not Updated");

		flag = false;
		counter = 0;
		Response cardResponse2 = null;
		do {
			try {
				cardResponse2 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
				Assert.assertEquals(cardResponse2.statusCode(), 200);
				String iosGuestAccountDeactivation2 = cardResponse2.jsonPath().get("[0].ios_guest_account_deactivation")
						.toString().replace("[", "").replace("]", "").trim();
				Assert.assertEquals(iosGuestAccountDeactivation2, "true", "");

				String androidGuestAccountDeactivation2 = cardResponse2.jsonPath()
						.get("[0].android_guest_account_deactivation").toString().replace("[", "").replace("]", "")
						.trim();
				Assert.assertEquals(androidGuestAccountDeactivation2, "true", "");

				logger.info(
						"verified when checkbox are check then ios_guest_account_deactivation and android_guest_account_deactivation value are coming true ");
				TestListeners.extentTest.get().pass("verified when checkbox are check then "
						+ "ios_guest_account_deactivation and android_guest_account_deactivation value are coming true ");
				break;
			} catch (AssertionError ae) {
				flag = false;
				counter++;
				selUtils.longWait(2000);
			}
		} while (flag || (counter <= 30));

		String guestAccountDeletionrequestEmail1 = cardResponse2.jsonPath().get("guest_account_deletion_request_email")
				.toString().replace("[", "").replace("]", "").trim();

		Assert.assertTrue(guestAccountDeletionrequestEmail1.contains("to="), "does not contain the email in api");
		Assert.assertTrue(guestAccountDeletionrequestEmail1.contains("subject="), "does not contain the email in api");
		Assert.assertTrue(guestAccountDeletionrequestEmail1.contains("body="), "does not contain the email in api");

		logger.info(
				"when drpdown value is request by email then api contain " + "email,email subject and the email body");
		TestListeners.extentTest.get().pass(
				"when drpdown value is request by email then api contain " + "email,email subject and the email body");

		// when drpdown value is allow direct delection
		pageObj.mobileconfigurationPage().iosAppDelectionTypeNew(dataSet.get("option3"));
		msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"Mobile configuration updated is not Updated");

		pageObj.mobileconfigurationPage().androidAppDelectionTypeNew(dataSet.get("option3"));
		msg = pageObj.Accountdeletionpage().Update();
		Assert.assertEquals(msg, "Mobile configuration updated for Account Deletion",
				"Mobile configuration updated is not Updated");
		// pageObj.mobileconfigurationPage().refereshPage();
		// pageObj.dashboardpage().navigateToTabs("Account Deletion");
		// pageObj.mobileconfigurationPage().refereshPage();
		// pageObj.mobileconfigurationPage().clickToggleAccountDelDropdown();

		flag = false;
		counter = 0;
		do {
			try {
				Response cardResponse3 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
				Assert.assertEquals(cardResponse3.statusCode(), 200);

				String guestAccountDeletionrequestEmail2 = cardResponse3.jsonPath()
						.get("[0].guest_account_deletion_request_email").toString();

				Assert.assertEquals(guestAccountDeletionrequestEmail2, "{}",
						"guest Account Deletion request Email contains values");
				TestListeners.extentTest.get().pass("when drpdown value is allow direct delection then api "
						+ "does not contain email,email subject and the email body");
				flag = true;
				break;

			} catch (AssertionError ae) {
				flag = false;
				counter++;
				selUtils.longWait(2500);
			}

		} while (flag || (counter <= 30));

		Assert.assertTrue(flag, "guestAccountDeletionrequestEmail2 not matched even after api pooling");

		pageObj.mobileconfigurationPage().clickRomaniaLang();

		isdelectionRequestEmail = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delectionRequestEmail");
		isdelectionRequestEmailSubject = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.delectionRequestEmailSubject");

		Assert.assertFalse(isdelectionRequestEmail, "field is not greyout");
		Assert.assertFalse(isdelectionRequestEmailSubject, "field is not greyout");
		logger.info("fields are greyout when different language in selected");
		TestListeners.extentTest.get().pass("fields are greyout when different language in selected");

		// supported language
		List<String> ExpectedSupportLangList = Arrays.asList("en", "en-CY", "es", "fr", "fr-ca", "ro", "vi");
		List<String> supportLangList = pageObj.mobileconfigurationPage().supportedLangList();

		Assert.assertTrue(ExpectedSupportLangList.containsAll(supportLangList),
				"all values are not present in the supported language tab");
		logger.info("all values are not present in the supported language tab");
		TestListeners.extentTest.get().pass("all values are not present in the supported language tab");

		// click extented settings
		pageObj.mobileconfigurationPage().clickExtendedSettingsBtn();

		boolean isSupportEmail = pageObj.mobileconfigurationPage().isEnabled("mobileConfigurationPage.supportEmail");
		boolean isresturantSupportEmail = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.resturantSupportEmail");

		Assert.assertTrue(isSupportEmail, "field is greyout");
		Assert.assertTrue(isresturantSupportEmail, "field is greyout");
		logger.info("fields are not greyout when default language in selected inn extented settings");
		TestListeners.extentTest.get()
				.pass("fields are not greyout when default language in selected inn extented settings");

	}

	// author=Anant
	@Test(description = "SQ-T3244 Validate that Error/Success message navigation for tabs present on mobile configuration page."
			+ "SQ-T3315 Validate that when user clicked on cancel button on the mobile config page, "
			+ "then user should be re-directed to the same page & same tab only.", enabled = true, groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3244_validateMsgNavigationMobileConfigurationPage() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable meta cache update on request", "check");

		// go to whitelabel
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// check language support available in the disclaimer
		boolean supportLanguageVisible = pageObj.mobileconfigurationPage()
				.isDisplayed("mobileConfigurationPage.supportlanguage");
		Assert.assertTrue(supportLanguageVisible);
		logger.info("support language is available for disclaimer field");
		TestListeners.extentTest.get().pass("support language is available for disclaimer field");

		// check tab are visible
		List<String> actualTabList = pageObj.mobileconfigurationPage().nonActiveTabs();
		Assert.assertTrue(actualTabList.contains("Miscellaneous URLs"));
		Assert.assertTrue(actualTabList.contains("Extended Settings"));
		Assert.assertTrue(actualTabList.contains("App Messages"));
		Assert.assertTrue(actualTabList.contains("Miscellaneous Fields"));
		Assert.assertTrue(actualTabList.contains("App Strings"));
		Assert.assertTrue(actualTabList.contains("Loyalty"));
		Assert.assertTrue(actualTabList.contains("Ordering"));
		Assert.assertTrue(actualTabList.contains("Gift Card"));
		logger.info(
				"Account deletion,subscriptions,miscellaneous URLs and other Tabs are present in the mobile configuration page");
		TestListeners.extentTest.get().pass(
				"Account deletion,subscriptions,miscellaneous URLs and other Tabs are present in the mobile configuration page");

		// edit footer text
		String footerEditText = pageObj.mobileconfigurationPage().editFooterText();
		logger.info("updated footer text value");

		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();

		String getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();

		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Disclaimer", "success msg is not equal");
		logger.info("correct success msg is displayed updated disclaimer field");
		TestListeners.extentTest.get().pass("correct success msg is displayed updated disclaimer field");

		// miscellaous urls
		pageObj.mobileconfigurationPage().navigateToTab("Miscellaneous URLs");
//		pageObj.mobileconfigurationPage().refereshPage();
//		pageObj.mobileconfigurationPage().clickMiscellaneousURLBtn();
		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();

		getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Miscellaneous URLs",
				"success msg is not equal");
		logger.info("correct success msg is displayed updated miscellaeous URls field");
		TestListeners.extentTest.get().pass("correct success msg is displayed updated miscellaeous URls field");

		// App message
		pageObj.mobileconfigurationPage().navigateToTab("App Messages");
//		pageObj.mobileconfigurationPage().refereshPage();
//		pageObj.mobileconfigurationPage().clickAppMessageBtn();
		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();
		Assert.assertTrue(pageObj.mobileconfigurationPage().isDisplayed("mobileConfigurationPage.supportlanguage"));

		getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for App Messages", "success msg is not equal");
		logger.info("correct success msg is displayed updated App Messages field");
		TestListeners.extentTest.get().pass("correct success msg is displayed updated App Messages field");

		// miscellaneous field
		pageObj.mobileconfigurationPage().navigateToTab("Miscellaneous Fields");
//		pageObj.mobileconfigurationPage().refereshPage();
//		pageObj.mobileconfigurationPage().clickMiscellaneousFieldsBtn();
		pageObj.mobileconfigurationPage().clickMobileConfigUpdateBtn();
		Assert.assertTrue(pageObj.mobileconfigurationPage().isDisplayed("mobileConfigurationPage.supportlanguage"));

		getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Miscellaneous Fields",
				"success msg is not equal");
		logger.info("correct success msg is displayed updated App Messages field");
		TestListeners.extentTest.get().pass("correct success msg is displayed updated App Messages field");

		// app strings
		pageObj.mobileconfigurationPage().navigateToTab("App Strings");
//		pageObj.mobileconfigurationPage().refereshPage();
//		pageObj.mobileconfigurationPage().clickAppStringsBtn();
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();
		Assert.assertTrue(pageObj.mobileconfigurationPage().isDisplayed("mobileConfigurationPage.supportlanguage"));
//		utils.waitTillPagePaceDone();
		getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();

		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for App Strings", "success msg is not equal");
		logger.info("correct success msg is displayed updated App Strings field");
		TestListeners.extentTest.get().pass("correct success msg is displayed updated App Strings field");

		// disclaimer
		pageObj.mobileconfigurationPage().clickDisclaimerBtn();
//		pageObj.mobileconfigurationPage().refereshPage();
//		pageObj.mobileconfigurationPage().clickDisclaimerBtn();

		// click on cancel btn
		pageObj.mobileconfigurationPage().clickCancelBtn();
		String attrValue = pageObj.mobileconfigurationPage().checkDisclaimerFieldSelected();
		Assert.assertEquals(attrValue.contains("active"), true,
				"after clicking on the cancel redirect to some other page");
		logger.info("after clicking on the cancel btn redirect to disclaimer page");
		TestListeners.extentTest.get().pass("after clicking on the cancel btn redirect to disclaimer page");

		utils.waitTillPagePaceDone();

		// api v1
		Response cardResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse1.statusCode(), 200);

		String footerText = cardResponse1.jsonPath().get("[0].footer_text_info").toString();
		Assert.assertEquals(footerText, footerEditText, "value is not updated in v1 api");
		logger.info("updated value is shown in the v1 api");
		TestListeners.extentTest.get().pass("updated value is shown in the v1 api");

		// api v2
		Response cardResponse2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse2.statusCode(), 200);

		String footerText2 = cardResponse2.jsonPath().get("footer_text_info").toString();
		Assert.assertEquals(footerText2, footerEditText, "value is not updated in v2 api");
		logger.info("updated value is shown in the v2 api");
		TestListeners.extentTest.get().pass("updated value is shown in the v2 api");

	}

	// author=Anant
	@Test(description = "Validate that newly added field 'password does not match.'", enabled = true, groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3416_ValidateFieldPasswordDoesNotMatch() throws InterruptedException {
		String successMsg, errorMsg;
		String editVal = "doesn't match Password english new";

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whiteLabel->api messages->guest profile
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "API Messages");
		pageObj.iframeConfigurationPage().clickOnPageTab("Guest Profile");

		// verify asterik
		String asterik = pageObj.whitelabelPage().verifyAsterik("Password does not match");
		Assert.assertTrue(asterik.contains("*"), "field is not mandatory");
		logger.info("'PasswordDoesNotMatch' fields is mandatory");
		TestListeners.extentTest.get().pass("'PasswordDoesNotMatch' fields is mandatory");

		// very field is kept empty
		pageObj.whitelabelPage().editPasswordDoesnotMatch("");
		pageObj.whitelabelPage().saveBtn();
		errorMsg = pageObj.whitelabelPage().getErrorMessage();
		Assert.assertEquals(errorMsg, "Password does not match can't be blank");
		logger.info("error msg is displayed");

		// update the field value
		pageObj.whitelabelPage().editPasswordDoesnotMatch(editVal);
		pageObj.whitelabelPage().saveBtn();
		successMsg = pageObj.whitelabelPage().getSuccessMsg();
		Assert.assertEquals(successMsg, "API Messages saved for Guest Profile");
		logger.info("success msg is displayed");

		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		errorMsg = pageObj.iframeSingUpPage().iframeSignUpWrongPassword();

		Assert.assertEquals(errorMsg, "Confirm Password " + editVal,
				"update value of field is not displayed at the iframe signup");

		logger.info("update value of field is displayed at the iframe signup");
		TestListeners.extentTest.get().pass("update value of field is displayed at the iframe signup");

	}

	// author=Anant
	@Test(description = "SQ-T3246 Validate that saved a specific tab, when tap on update button in mobile "
			+ "configuration page.", enabled = true, groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3246_ValidateSpecificTabWhenTapOnUpdateButton() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to whitelabel
		// pageObj.menupage().clickWhiteLabelMenu();
		// pageObj.menupage().clickMobileConfigurationLink();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// edit footer text
		String oldFooterTextValue = pageObj.mobileconfigurationPage().getFooterTextValue();
		String footerEditText = pageObj.mobileconfigurationPage().editFooterText();
		logger.info("updated footer text value");
		pageObj.mobileconfigurationPage().clickCancelBtn();

		Assert.assertNotEquals(oldFooterTextValue, footerEditText,
				"after pressing the cancel button old value is not displayed in footer text");
		logger.info("after pressing the cancel button old value is  displayed in footer text");
		TestListeners.extentTest.get().pass("after pressing the cancel button old value is  displayed in footer text");

		String beforechallengeDisclaimer = pageObj.mobileconfigurationPage().getchallengeDisclaimer();
		footerEditText = pageObj.mobileconfigurationPage().editFooterText();
		String gameDisclaimerEditText = pageObj.mobileconfigurationPage().gameDisclaimerEditText();
		logger.info("updated footer text value");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String afterchallengeDisclaimer = pageObj.mobileconfigurationPage().getchallengeDisclaimer();

		Assert.assertEquals(beforechallengeDisclaimer, afterchallengeDisclaimer,
				"both the values of the unchange fields are not equal after updation");
		logger.info("both the values of the unchange fields are equal after updation");
		TestListeners.extentTest.get().pass("both the values of the unchange fields are equal after updation");

		// click account delection
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickToggleAccountDelDropdown();

		// uncheck ios and android checkbox
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_ios_guest_account_deactivation",
				"uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_android_guest_account_deactivation",
				"uncheck");
		pageObj.mobileconfigurationPage().clickUpdateBtn();

		boolean deactivateDescription = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.deactivateDescription");
		Assert.assertFalse(deactivateDescription, "Field is not greyout");
		logger.info("field is greyout when ios and andriod is unchecked");
		TestListeners.extentTest.get().pass("field is greyout when ios and andriod is unchecked");

		// checkios and android checkbox
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickToggleAccountDelDropdown();

		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_ios_guest_account_deactivation",
				"check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("mobile_configuration_android_guest_account_deactivation",
				"check");
		pageObj.mobileconfigurationPage().clickUpdateBtn();

		deactivateDescription = pageObj.mobileconfigurationPage()
				.isEnabled("mobileConfigurationPage.deactivateDescription");
		Assert.assertTrue(deactivateDescription, "Field is  greyout");
		logger.info("field is not greyout when ios and andriod is checked");
		TestListeners.extentTest.get().pass("field is not greyout when ios and andriod is checked");

		// v1 api
		Response cardResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse1.statusCode(), 200);

		String footerText = cardResponse1.jsonPath().get("[0].footer_text_info").toString();
		Assert.assertEquals(footerText, footerEditText, "value is not updated in v1 api");
		logger.info("updated value is shown in the v1 api");
		TestListeners.extentTest.get().pass("updated value is shown in the v1 api");

		String gameDisclaimer = cardResponse1.jsonPath().get("[0].game_disclaimer").toString();
		Assert.assertEquals(gameDisclaimer, gameDisclaimerEditText, "game disclaimer value is not updated in v1 api");
		logger.info("game disclaimer updated value is shown in the v1 api");
		TestListeners.extentTest.get().pass("game disclaimer updated value is shown in the v1 api");
	}

	// author=Anant/shaleen
	@Test(description = "SQ-T3275 Validate that Text area (Text box) is showing two times for "
			+ "additional loyal, gift card and order app configs.", enabled = true, groups = { "regression",
					"dailyrun" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T3275_validateTextAreaShowing() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// change preferred language
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.settingsPage().editPreferredLanguage("French");
		logger.info("French language is selected as the preferred language");

		// go to whitelabel
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// LoyaltyAppConfig
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Loyalty");
		// pageObj.mobileconfigurationPage().clickLoyaltyAppConfig();
		// invalid json
		pageObj.mobileconfigurationPage().editAdditionalLoyaltyAppConfig("{\"key\":\"ds dada patil f\"");
		String errorMsg = pageObj.mobileconfigurationPage().getErrorMessage();
		Assert.assertEquals(errorMsg, "Error updating Mobile configuration for Loyalty");
		logger.info("Error msg is displayed when invalid json is entered in the additional loyalty app config");
		TestListeners.extentTest.get()
				.pass("Error msg is displayed when invalid json is entered in the additional loyalty app config");
		// valid json
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Loyalty");
		// pageObj.mobileconfigurationPage().clickLoyaltyAppConfig();
		pageObj.mobileconfigurationPage().editAdditionalLoyaltyAppConfig("{\"key\":\"ds dada patil f\"}");
		String successMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Loyalty");
		logger.info("Success msg is displayed when valid json is entered in the additional loyalty app config");
		TestListeners.extentTest.get()
				.pass("Success msg is displayed when valid json is entered in the additional loyalty app config");

		// orderingAppConfig
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickOrderingAppConfigBtn();
		// invalid json
		pageObj.mobileconfigurationPage()
				.editAdditionalOrderingAppConfig("{\"key1\":\"value\",\"key2\":\"ashishbabaso value\"");
		errorMsg = pageObj.mobileconfigurationPage().getErrorMessage();
		Assert.assertEquals(errorMsg, "Error updating Mobile configuration for Ordering");
		logger.info("Error msg is displayed when invalid json is entered in the additional ordering app config");
		TestListeners.extentTest.get()
				.pass("Error msg is displayed when invalid json is entered in the additional ordering app config");
		// valid json
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickOrderingAppConfigBtn();
		pageObj.mobileconfigurationPage()
				.editAdditionalOrderingAppConfig("{\"key1\":\"value\",\"key2\":\"ashishbabaso value\"}");
		successMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Ordering");
		logger.info("Success msg is displayed when valid json is entered in the additional ordering app config");
		TestListeners.extentTest.get()
				.pass("Success msg is displayed when valid json is entered in the additional ordering app config");

		// gift card app config
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickGiftCardAppBtn();
		// invalid json
		pageObj.mobileconfigurationPage()
				.editAdditionalGiftCardAppConfig("{\"key1\":\"value\",\"key2\":\"ashishbabaso value\"");
		errorMsg = pageObj.mobileconfigurationPage().getErrorMessage();
		Assert.assertEquals(errorMsg, "Error updating Mobile configuration for Gift Card");
		logger.info("Error msg is displayed when invalid json is entered in the additional gift card app config");
		TestListeners.extentTest.get()
				.pass("Error msg is displayed when invalid json is entered in the additional gift card app config");
		// valid json
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.mobileconfigurationPage().clickGiftCardAppBtn();
		pageObj.mobileconfigurationPage()
				.editAdditionalGiftCardAppConfig("{\"key1\":\"value\",\"key2\":\"ashishbabaso value\"}");
		successMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Gift Card");
		logger.info("Success msg is displayed when valid json is entered in the additional gift card app config");
		TestListeners.extentTest.get()
				.pass("Success msg is displayed when valid json is entered in the additional gift card app config");

		// v1 API
		Response cardResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse1.statusCode(), 200);

		String loyaltyAppConfig = cardResponse1.jsonPath().get("[0].loyalty_app_config").toString();
		String orderingAppConfig = cardResponse1.jsonPath().get("[0].ordering_app_config").toString();
		String giftCardAppConfig = cardResponse1.jsonPath().get("[0].giftcard_app_config").toString();

		Assert.assertTrue(loyaltyAppConfig.contains("\"key\":\"ds dada patil f\""));
		TestListeners.extentTest.get()
				.pass("updated values of additional Loyalty app config is present in the v1 meta api");

		Assert.assertTrue(orderingAppConfig.contains("\"key1\":\"value\",\"key2\":\"ashishbabaso value\""));
		TestListeners.extentTest.get()
				.pass("updated values of additional ordering app config is present in the v1 meta api");

		Assert.assertTrue(giftCardAppConfig.contains("\"key1\":\"value\",\"key2\":\"ashishbabaso value\""));
		TestListeners.extentTest.get()
				.pass("updated values of additional gift card app config is present in the v1 meta api");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.settingsPage().editPreferredLanguage("English");
		logger.info("English language is selected as the preferred language");

	}

	// author=Anant
	@Test(description = "SQ-T3237: Validate that White-space warning alert for configurational values in services fields", enabled = true, groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3237_whiteSpaceWarningAlert() throws InterruptedException {
		String errorMsg;

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel -> services
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("Aloha Web Services");

		pageObj.whitelabelPage().editAlohaCompanyID(" automation");
		pageObj.whitelabelPage().editAlohaNamespace("automation ");
		pageObj.whitelabelPage().editAlohaUserName(" automation ");

		pageObj.whitelabelPage().clickUpdateAlohaWebServices();

		errorMsg = pageObj.whitelabelPage().getErrorMessage();

		Assert.assertEquals(errorMsg, "Error in saving Aloha Web Services settings. Aloha Company ID is invalid,"
				+ "Aloha User Name is invalid,Aloha Namespace is invalid");

		TestListeners.extentTest.get()
				.pass("Error msg is displayed when extra space is edit in the beginning and the end");

	}

	// author=Anant
	@Test(description = "SQ-T3240: Validate that Koupon Media Services Configurations.", enabled = true, groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3240_ValidateKouponMediaServicesConfigurations() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu clear business alternate languages
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().clearAllAlternateLanguages();

		// Cockpit > Guests > Guest Validation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.cockpitGuestPage().clickGuestValidationBtn();

		// click koupon media value
		pageObj.cockpitGuestPage().selectAgeVerificationAdapter("Koupon Media");
		logger.info("koupon media value is selected");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// whitelabel -> services
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.whitelabelPage().clickKouponMediaBtn();

		// asterik
		String ageVerificationBaseURLasterik = pageObj.whitelabelPage().verifyAsterik("Age Verification Base URL");
		Assert.assertTrue(ageVerificationBaseURLasterik.contains("*"));
		TestListeners.extentTest.get().pass(" * is present in the age Verification Base URL field");

		String ageVerificationClientIDasterik = pageObj.whitelabelPage().verifyAsterik("Age Verification Client ID");
		Assert.assertTrue(ageVerificationClientIDasterik.contains("*"));
		TestListeners.extentTest.get().pass(" * is present in the age Verification Base URL field");

		// Age Verification Base URL
		String ageVerificationBaseUrl = pageObj.whitelabelPage().editKouponMediaFeild("Age Verification Base URL", "",
				"age_verification_base_url");
		Assert.assertEquals(ageVerificationBaseUrl, "https://verify.kouponmedia.com");
		TestListeners.extentTest.get().pass("default value of the age verification base URL is matched");

		// Age Verification Client ID
		String ageVerificationClientID = pageObj.whitelabelPage().editKouponMediaFeild("Age Verification Client ID", "",
				"age_verification_client_id");
		Assert.assertEquals(ageVerificationClientID, "");
		TestListeners.extentTest.get().pass("default value of the age Verification Client ID is matched");

		// Age Verification Client Secret
		String ageVerificationClientSecret = pageObj.whitelabelPage()
				.editKouponMediaFeild("Age Verification Client Secret", "", "age_verification_client_secret");
		Assert.assertEquals(ageVerificationClientSecret, "");
		TestListeners.extentTest.get().pass("default value of the Age Verification Client Secret is matched");

		// Age Verification API Error Message
		String ageVerificationAPIErrMsg = pageObj.whitelabelPage()
				.editKouponMediaFeild("Age Verification API Error Message", "", "age_verification_api_error_message");
		Assert.assertEquals(ageVerificationAPIErrMsg, "Something went wrong.");
		TestListeners.extentTest.get().pass("default value of the Age Verification API Error Message is matched");

		// Age Verification Opt Out Message
		String ageVerificationOptOutMsg = pageObj.whitelabelPage()
				.editKouponMediaFeild("Age Verification Opt Out Message", "", "age_verification_optout_message");
		Assert.assertEquals(ageVerificationOptOutMsg, "Are you sure you want to opt out from receiving 21+ offers?");
		TestListeners.extentTest.get().pass("default value of the Age Verification Opt Out Message is matched");

		// Age Verification Client Secret
		pageObj.whitelabelPage().editKouponMediaFeild("Age Verification Client Secret", "12345678",
				"age_verification_client_secret");
		pageObj.whitelabelPage().clickUpdateKouponMedia();
		String ageVerificationClientSecret2 = pageObj.whitelabelPage().isHashed("age_verification_client_secret");
		// System.out.println(ageVerificationClientSecret2);
		Assert.assertTrue(ageVerificationClientSecret2.contains("***"), "value is not hashed");
		TestListeners.extentTest.get().pass("age Verification Client Secret value is hashed");

		pageObj.whitelabelPage().clickEditBtn("age_verification_client_secret");

		// cockpit->guest->guest validation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.cockpitGuestPage().clickGuestValidationBtn();
		// pageObj.cockpitGuestPage().removeSelectValueUsingx("Koupon Media");
		pageObj.cockpitGuestPage().clearDropdownValue("Age Verification Adapter");
		logger.info("koupon media value is deselected");
		pageObj.cockpitGuestPage().clickUpdateBtn();
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
