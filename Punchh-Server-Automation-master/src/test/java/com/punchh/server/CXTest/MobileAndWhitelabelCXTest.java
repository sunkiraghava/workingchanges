package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileAndWhitelabelCXTest {

	private static Logger logger = LogManager.getLogger(MobileAndWhitelabelCXTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	SeleniumUtilities selUtils;
	Utilities utils;

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

	// Anant
	@Test(description = "SQ-T3957 Validate that Dispatch option should not be available under “Excluded Hours of Operation” field."
			+ "SQ-T3906 (1.0) Validate that newly added flag \"Excluded Hours of Operation\" should be available in the mobile configuration page.")
	@Owner(name = "Vansham Mishra")
	public void T3957_DispatchOptionNotAvailable() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Ordering");

		// pageObj.posIntegrationPage().selectDrpDownValue("Guest Lookup Type", "");
		List<String> expExcludedHoursOfOperationDrpDownList = Arrays.asList("Business", "Delivery", "Carry Out",
				"Pickup", "Dine In", "Curbside", "Drivethru", "Rails", "Physical");
		List<String> actualExcludedHoursOfOperationDrpDownList = pageObj.mobileconfigurationPage()
				.ExcludedHoursOfOperationDrpDownList();
		Assert.assertEquals(expExcludedHoursOfOperationDrpDownList, actualExcludedHoursOfOperationDrpDownList,
				"Excluded hours of operation type drp down list is not coming as expected");

		// deselect the value
		pageObj.mobileconfigurationPage().refereshPage();
		pageObj.dashboardpage().navigateToTabs("Ordering");
		pageObj.posIntegrationPage().deselectAllValuesFromDrpDown("Excluded Hours of Operation");

		// select the Delivery value from drp down
		pageObj.posIntegrationPage().selectDrpDownValue("Excluded Hours of Operation", "Delivery");
		pageObj.mobileconfigurationPage().clickVerifyAndUpdateBtn();

		Response card1 = null;
		String orderingAppConfig = null;
		int counter = 0;
		do {
			card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
			Assert.assertEquals(card1.statusCode(), 200, "status code not equal");
			orderingAppConfig = card1.jsonPath().get("[0].ordering_app_config").toString();
			if (orderingAppConfig.contains("EXCLUDE_OPERATION_HOURS\":[\"delivery\",\"dispatch\"]")) {
				utils.logit("value updated in the API");
				break;
			}
			if (counter == 9) {
				logger.info("even after trying 10 times value dodnot updated in the API");
				TestListeners.extentTest.get().fail("even after trying 10 times value dodnot updated in the API");
			}
			selUtils.longWait(1000);
			counter++;
		} while (counter < 10);
		Assert.assertTrue(orderingAppConfig.contains("EXCLUDE_OPERATION_HOURS\":[\"delivery\",\"dispatch\"]"),
				"does not contain the EXCLUDE_OPERATION_HOURS key in API Response");
		utils.logPass("include the key EXCLUDE_OPERATION_HOURS in the API response");

		pageObj.posIntegrationPage().clickAuditLog();

		// deselect the value
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Ordering");
		pageObj.posIntegrationPage().deselectAllValuesFromDrpDown("Excluded Hours of Operation");

		// select the Delivery value from drp down
		pageObj.posIntegrationPage().selectDrpDownValue("Excluded Hours of Operation", "Rails");
		String actualMessage = pageObj.mobileconfigurationPage().clickVerifyMessageOnUpdateBtn();
		String expectedMessage = dataSet.get("expectedMessage");
		Assert.assertTrue(actualMessage.contains(expectedMessage), "Success message not displayed");
		utils.logit("Clicked Verify & Update Button");

		// whitelabel-> mobile configurations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Ordering");

		// hint text
		String hintText1 = pageObj.mobileconfigurationPage().getHintTextForDrpDown("exclude_operation_hours_en");
		Assert.assertEquals(hintText1, dataSet.get("hintText"),
				"hint text is not equal for Excluded Hours of Operation");
		utils.logPass("verified hint text is equal for Excluded Hours of Operation field");

	}

	// Anant
	@Test(description = "SQ-T3958: Validate that Success & Error message should not be flickering on page load in Mobile Configuration; "
			+ "SQ-T3309: Validate that only valid URLs accepted in Youtube URL & Restaurant Feedback URL fields.")
	@Owner(name = "Vansham Mishra")
	public void T3958_SuccessErrorMsgNotFlickering() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Settings->Locations
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// edit footer text
		pageObj.mobileconfigurationPage().editFooterText();
		utils.logit("updated footer text value");

		pageObj.mobileconfigurationPage().clickUpdateBtn();
		utils.waitTillPagePaceDone();
		String getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Disclaimer", "success msg is not equal");
		utils.logPass("correct success msg is displayed updated disclaimer field");

		String getSuccessMsgColor = pageObj.mobileconfigurationPage().msgColor("mobileConfigurationPage.successMsg");
		Assert.assertEquals(getSuccessMsgColor, "#155724", "success msg is not of green color");
		logger.info("success msg is of green color");

		pageObj.posIntegrationPage().clickAuditLog();
		String auditVal1 = pageObj.posIntegrationPage().auditLogValue("Footer Text Info");
		Assert.assertEquals(auditVal1, "MobileConfiguration", "updated value is not display in the logs");
		TestListeners.extentTest.get().pass("update value is displayed in the logs");

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous URLs");

		// invalid url
		pageObj.mobileconfigurationPage().youtubeURlField(dataSet.get("invalidURL"));
		utils.logit("Invalid url entered in Youtube URL field");
		pageObj.mobileconfigurationPage().restaurantFeedbackURlField(dataSet.get("invalidURL"));
		utils.logit("Invalid url entered in Restaurant Feedback URL field");

		pageObj.mobileconfigurationPage().clickUpdateBtn();

		String errorMsg = pageObj.mobileconfigurationPage().getErrorMessage();

		Assert.assertEquals(errorMsg, "Error updating Mobile configuration for Miscellaneous URLs");
		utils.logit("pass", "Error message is correct");

		String errorMsgColor = pageObj.mobileconfigurationPage().msgColor("mobileConfigurationPage.errorMsg");
		Assert.assertEquals(errorMsgColor, "#ff0000", "error msg is not of red color");
		utils.logit("Error message color is red");
		String isInvalidMsg = pageObj.mobileconfigurationPage().isInvalidMsg();
		Assert.assertEquals(isInvalidMsg, "is invalid", "not showing that url is invalid");
		utils.logit("pass", "Error msg is displayed when entered the invalid URL");

		Response card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card1.statusCode(), 200, "Status code is not equal");

		Response card2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card2.statusCode(), 200, "Status code is not equal");

		// valid url
		pageObj.mobileconfigurationPage().youtubeURlField(dataSet.get("validURL"));
		utils.logit("pass", "Valid url entered in Youtube URL field");

		pageObj.mobileconfigurationPage().restaurantFeedbackURlField(dataSet.get("validURL"));
		utils.logit("pass", "Valid url entered in Restaurant Feedback URL field");

		pageObj.mobileconfigurationPage().clickUpdateBtn();

		getSuccessMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(getSuccessMsg, "Mobile configuration updated for Miscellaneous URLs");
		utils.logPass("Success message is correct");

		getSuccessMsgColor = pageObj.mobileconfigurationPage().msgColor("mobileConfigurationPage.successMsg");
		Assert.assertEquals(getSuccessMsgColor, "#155724", "success msg is not of green color");
		utils.logPass("Success msg is of green color");
		utils.logit("pass", "Success message is displayed when entered the valid URL");
	}

	// Anant
	@Test(description = "SQ-T4242 and SQ-T3956: Validate that newly added two \"Guest Lookup Type\" and \"Single Scan\" "
			+ "configurations (dropdowns) for business and locations.")
	@Owner(name = "Vansham Mishra")
	public void T3956_newlyAddedFieldsInBusinessAndLocationPart2() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		//
//			// Settings->Locations
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		pageObj.locationPage().clickTopLocation();

		// pageObj.locationPage().clickOnLocationName(dataSet.get("locationName"));
		pageObj.dashboardpage().navigateToTabs("POS");
		//
//			// v1 API when no value is selected
		Response card5 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card5.statusCode(), 200);
		String guestLookUP5 = card5.jsonPath().get("[0].locations[1].location_extra.guest_lookup_type").toString();
		String singleScanType5 = card5.jsonPath().get("[0].locations[1].location_extra.single_scan_type").toString();
		Assert.assertEquals(guestLookUP5, "");
		Assert.assertEquals(singleScanType5, "");
		utils.logPass("when no value is selected for the guest and single scan type then the null value is coming in v1");
		//
//			// v2 API when no value is selected
		Response card6 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card6.statusCode(), 200);
		String guestLookUP6 = card6.jsonPath().get("locations[1].guest_lookup_type").toString();
		String singleScanType6 = card6.jsonPath().get("locations[1].single_scan_type").toString();
		Assert.assertEquals(guestLookUP6, "");
		Assert.assertEquals(singleScanType6, "");
		utils.logPass("when no value is selected for the guest and single scan type then the null value is coming in v2");
		//
		List<String> expGuestLookUpTypeListLocation = Arrays.asList("Phone Number", "Short code", "Long code",
				"Short-lived code");
		pageObj.posIntegrationPage().verifyGuestLookUpTypeDrpDownList(expGuestLookUpTypeListLocation,
				"Guest Lookup Type");

		String hintText3 = pageObj.posIntegrationPage().getHintText("Guest Lookup Type");
		Assert.assertEquals(hintText3,
            "Identifier in the mobile app and iFrame that is used for guest lookup at the POS. This setting will override the business setting for this location. In the case that “Display Guest Identity as” is Bar Code, Punchh app and iFrame will only support phone number guest lookup type",
				"for Guest look up type for loaction hint text is not same ");
		utils.logPass("for Guest look up type hint text is same for location ");

		pageObj.posIntegrationPage().selectDrpDownValue("Guest Lookup Type", "Long code");
//			// pageObj.posIntegrationPage().selectDrpDownValue("Single Scan Type", "Long
//			// token");
		pageObj.posIntegrationPage().clickUpdateBtn();

		// v1 API when value is selected
		Response card7 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card7.statusCode(), 200);
		String guestLookUP7 = card7.jsonPath().get("[0].locations[0].location_extra.guest_lookup_type").toString();
		Assert.assertEquals(guestLookUP7, "long_code");
		utils.logPass(
				"when no value is selected for the guest and single scan type then the selected value is coming in v1");

//			// v2 API when no value is selected
		Response card8 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card8.statusCode(), 200);

		String guestLookUP8 = card8.jsonPath().get("locations[0].guest_lookup_type").toString();
		Assert.assertEquals(guestLookUP8, "long_code");
		utils.logPass(
				"when no value is selected for the guest and single scan type then the selected value is coming in v2");

		// remove

		pageObj.dashboardpage().navigateToTabs("POS");
		// pageObj.posIntegrationPage().deselectAllValuesFromDrpDown("Guest Lookup
		// Type");
		pageObj.posIntegrationPage().selectDrpDownValue("Guest Lookup Type", "");
		pageObj.posIntegrationPage().clickUpdateBtn();
		utils.logPass("de-select the value POS");
	}

	// Anant
	@Test(description = "SQ-T4242 and SQ-T3956: Validate that newly added two \"Guest Lookup Type\" and \"Single Scan\" "
			+ "configurations (dropdowns) for business and locations.")
	@Owner(name = "Vansham Mishra")
	public void T3956_newlyAddedFieldsInBusinessAndLocation() throws InterruptedException {

		List<String> expGuestLookUpTypeList = Arrays.asList("Phone Number", "Short code", "Long code",
				"Short-lived code");
		List<String> expSingleScanTypeList = Arrays.asList("None", "Short token", "Long token", "Single Scan Code");
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// cockpit->POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().verifyGuestLookUpTypeDrpDownList(expGuestLookUpTypeList, "Guest Lookup Type");

		String hintText1 = pageObj.posIntegrationPage().getHintText("Guest Lookup Type");
        Assert.assertEquals(hintText1,
            "Identifier in the mobile app and iFrame that is used for guest lookup at the POS. In the case that “Display Guest Identity as” is Bar Code, Punchh app and iFrame will only support phone number guest lookup type",
				"for Guest look up type hint text is not same ");
		utils.logPass("for Guest look up type hint text is same ");

		pageObj.posIntegrationPage().singleScanTypeTypeDrpDownList(expSingleScanTypeList, "Single Scan Type");
		String hintText2 = pageObj.posIntegrationPage().getHintText("Single Scan Type");
		Assert.assertEquals(hintText2,
				"Code displayed by the mobile app for a single scan transaction at the POS. Short and long token are not supported by Punchh mobile framework.",
				"for Single Scan Type hint text is not same ");
		utils.logPass("for Single Scan Type hint text is same ");

		// deselect the value
		pageObj.cockpitGuestPage().clearDropdownValue("Guest Lookup Type");
		pageObj.cockpitGuestPage().clearDropdownValue("Single Scan Type");
        utils.getLocator("CockpitGuestPage.enterPOSConfigUpdateInterval").click();
		pageObj.posIntegrationPage().clickUpdateBtn();

		// v1 API when no value is selected
		Response card1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card1.statusCode(), 200);
		String guestLookUP1 = card1.jsonPath().get("[0].guest_lookup_type").toString();
		String singleScanType1 = card1.jsonPath().get("[0].single_scan_type").toString();
		Assert.assertEquals(guestLookUP1, "");
		Assert.assertEquals(singleScanType1, "");
		logger.info("when no value is selected for the guest and single scan type then the null value is coming in v1");
		TestListeners.extentTest.get().pass(
				"when no value is selected for the guest and single scan type then the null value is coming in v1");

		// v2 API when no value is selected
		Response card2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card2.statusCode(), 200);
		String guestLookUP2 = card2.jsonPath().get("guest_lookup_type").toString();
		String singleScanType2 = card2.jsonPath().get("single_scan_type").toString();
		Assert.assertEquals(guestLookUP2, "");
		Assert.assertEquals(singleScanType2, "");
		utils.logPass("when no value is selected for the guest and single scan type then the null value is coming in v2");

		// select the drp down value
		pageObj.posIntegrationPage().selectDrpDownValue("Guest Lookup Type", "Long code");
		pageObj.posIntegrationPage().selectDrpDownValue("Single Scan Type", "Long token");
		pageObj.posIntegrationPage().clickUpdateBtn();
		// actions.click(utils.getLocator("PosIntegrationPage.updateBtn1")).build().perform();

		// v1 API when value is selected
		Response card3 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card3.statusCode(), 200);
		String guestLookUP3 = card3.jsonPath().get("[0].guest_lookup_type").toString();
		String singleScanType3 = card3.jsonPath().get("[0].single_scan_type").toString();
		Assert.assertEquals(guestLookUP3, "long_code");
		Assert.assertEquals(singleScanType3, "long_token");
		utils.logPass(
				"when no value is selected for the guest and single scan type then the selected value is coming in v1");

		// v2 API when value is selected
		Response card4 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(card4.statusCode(), 200);
		String guestLookUP4 = card4.jsonPath().get("guest_lookup_type").toString();
		String singleScanType4 = card4.jsonPath().get("single_scan_type").toString();
		Assert.assertEquals(guestLookUP4, "long_code");
		Assert.assertEquals(singleScanType4, "long_token");
		utils.logPass(
				"when no value is selected for the guest and single scan type then the selected value is coming in v2");

		// audit
		pageObj.posIntegrationPage().clickAuditLog();
		String auditVal1 = pageObj.posIntegrationPage().auditLogValue("Updated At");
		Assert.assertEquals(auditVal1, "Business", "updated value is not display in the logs");
		utils.logPass("update value is displayed in the logs");

	}

	// author=Anant
	@Test(description = "SQ-T3239 Validate that Koupon Media Cockpit Configurations", enabled = true, priority = 8)
	@Owner(name = "Vansham Mishra")
	public void T3239_kouponMediaCockpitConfigurations() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// cockpit->guest->guest validation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.cockpitGuestPage().clickGuestValidationBtn();

		pageObj.cockpitGuestPage().SelectValGuestMandatoryInputFields("First Name");
		logger.info("value is selected in the guest mandatory input field");

		pageObj.cockpitGuestPage().removeSelectValueUsingx("First Name");
		logger.info("value is deselected in the guest mandatory input field");

		// koupon coupon
		pageObj.cockpitGuestPage().selectAgeVerificationAdapter("Koupon Media");
		logger.info("koupon media value is selected");

		// pageObj.cockpitGuestPage().removeSelectValueUsingx("Koupon Media");
		pageObj.cockpitGuestPage().clearDropdownValue("Age Verification Adapter");
		logger.info("koupon media value is deselected");

		pageObj.cockpitGuestPage().clickUpdateBtn();

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");

		Assert.assertFalse(pageObj.whitelabelPage().isKouponMediaDisplayed(), "Kouupon Media is displayed");
		utils.logPass("Koupon Media is not displayed ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.cockpitGuestPage().clickGuestValidationBtn();

		pageObj.cockpitGuestPage().selectAgeVerificationAdapter("Koupon Media");
		logger.info("koupon media value is selected");

		pageObj.cockpitGuestPage().clickUpdateBtn();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		Assert.assertTrue(pageObj.whitelabelPage().isKouponMediaDisplayed(), "Kouupon Media is not displayed");
		utils.logPass("Koupon Media is displayed ");

		// v1 meta api
		Response cardResponse1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse1.statusCode(), 200);

		String age_verification_provider1 = cardResponse1.jsonPath().get("[0].age_verification_provider").toString();
		Assert.assertTrue(age_verification_provider1.contains("kouponmedia_config"));

		TestListeners.extentTest.get()
				.pass("both age_verification_provider and kouponmedia_config keys are present in the V1 api ");

		// v2 meta
		Response cardResponse2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResponse2.statusCode(), 200);

		String age_verification_provider2 = cardResponse2.jsonPath().get("age_verification_provider").toString();
		Assert.assertTrue(age_verification_provider2.contains("kouponmedia_config"),"age_verification_provider key is not present in the V2 api ");

		TestListeners.extentTest.get().pass("age_verification_provider key is present in the V2 api ");

	}

	// author=Anant
	@Test(description = "SQ-T3226 User can update email via PUT APIv1 when dependent flags are disabled", enabled = true, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T3226_userEmailUpdate() throws InterruptedException {
		String userEmail = pageObj.iframeSingUpPage().generateEmail();

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel iframe configuration
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("iframe_configuration_mandatory_phone_number", "uncheck");
		pageObj.iframeConfigurationPage().clickUpdateBtn();

		// creating user
		Response user1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(user1.statusCode(), 200);

		String token = user1.jsonPath().get("auth_token.token").toString();
		String user1Email = user1.jsonPath().get("email").toString();
		String newUserEmail = "new" + user1Email;

		Response changeEmail = pageObj.endpoints().Api1MobileUpdateGuestEmailDetails(newUserEmail,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(changeEmail.statusCode(), 200);

		String updatedEmail = changeEmail.jsonPath().get("email").toString();
		Assert.assertEquals(updatedEmail, newUserEmail);

		TestListeners.extentTest.get().pass("Email have been updated successfully");

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