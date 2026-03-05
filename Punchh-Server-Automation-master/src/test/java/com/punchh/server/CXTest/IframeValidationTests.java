package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class IframeValidationTests {
	static Logger logger = LogManager.getLogger(IframeValidationTests.class);
	public WebDriver driver;
	private String userEmail, barcode, sTCName;
	private PageObj pageObj;
	private static Map<String, String> dataSet;
	private String env;
	private String baseUrl;
	Properties prop;
	String run = "ui";
	String invalidBarCode = "0087405763899";
	String otherBuisnessBarcode = "0087405763815";
	SeleniumUtilities selUtils;
	Utilities utils;
	private Object String;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		selUtils = new SeleniumUtilities(driver);
	}

	@Test(description = "SQ-T2169 Barcode Lookup >> Verify that the barcode lookup provide receipt of transaction for the provided barcode "
			+ "|| SQ-T2558, Barcode Search >> Verify that the user can search for a receipt by using transaction number", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void barcodeLookupValidation() throws InterruptedException {
		logger.info("== Iframe barcode validation test ==");
		// SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		// pageObj.instanceDashboardPage().generateBarcode();
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		barcode = pageObj.instanceDashboardPage().getBarcode();
		logger.info(barcode);
		String barcodeAmount = pageObj.instanceDashboardPage().captureBarcodeAmount();
		logger.info(barcodeAmount);
		String barcodeTransactionNo = pageObj.instanceDashboardPage().captureBarcodeTransactionNumber();
		selUtils.switchToNewWindow();
		pageObj.menupage().clickOnBarcodeLookup();
		pageObj.instanceDashboardPage().barcodelookup(barcode);
		Assert.assertTrue(
				pageObj.instanceDashboardPage().verifyBarcodeLookupTransactionNumber(barcode, barcodeTransactionNo));
		Assert.assertTrue(
				pageObj.instanceDashboardPage().verifyBarcodeLookupAmount(barcodeTransactionNo, barcodeAmount));

		/*
		 * SQ-505, Barcode Search >> Verify that the user can search for a receipt by
		 * using transaction number
		 */
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.
		 * getConfigProperty("instanceUrl"));
		 * pageObj.instanceDashboardPage().selectBusiness("moes");
		 * pageObj.menupage().clickSupportMenu();
		 * pageObj.menupage().clickOnBarcodeLookup();
		 * Assert.assertTrue(pageObj.instanceDashboardPage().verifyBarcodeSearch(),
		 * "Barcode search failed"); //softAssertion.assertAll();
		 */

	}

	@Test(description = "SQ-T2199, iFrame Barcode Checkin >> Check-in through already used barcode. || SQ-T2270 "
			+ "iFrame Barcode Checkin >> Check-in through invalid Barcode. || SQ-T2384 "
			+ "iFrame Barcode Checkin >> Check-in with random barcode which is not associated with business.", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void InvalidBarcodeCheckinValidation() throws InterruptedException {
		logger.info("== Iframe barcode validation test ==");
		SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		barcode = pageObj.instanceDashboardPage().captureBarcode();
		// iframeCheckin
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		// verify already used barcode
		pageObj.iframeSingUpPage().iframeCheckin(barcode);
		driver.navigate().refresh();
		Assert.assertTrue(pageObj.iframeSingUpPage().verifyIframeCheckinWithDuplicateBarcode(barcode),
				"Duplicate barcode test failed");
		// verify invalid barcode
		driver.navigate().refresh();
		Assert.assertTrue(pageObj.iframeSingUpPage().verifyIframeCheckinWithInvalidBarcode(invalidBarCode),
				"Duplicate barcode test failed");
		// verify other business barcode
		driver.navigate().refresh();
		Assert.assertTrue(pageObj.iframeSingUpPage().verifyIframeCheckinWithInvalidBarcode(otherBuisnessBarcode),
				"Other buisness barcode test failed");
		softAssertion.assertAll();
	}

	@Test(description = "SQ-T2385 iFrame Barcode Checkin >> Check-in through old Barcode.", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void OldBarcodeCheckinValidation() throws InterruptedException {
		logger.info("== Iframe barcode validation test ==");
		String oldBarcode;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateOldBarcode(dataSet.get("location"));
		oldBarcode = pageObj.instanceDashboardPage().captureBarcode();
		// iframeCheckin
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		Assert.assertTrue(pageObj.iframeSingUpPage().verifyIframeCheckinWithOldBarcode(oldBarcode),
				"Oldbarcode barcode test failed");
		// edit profile.
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2545, Edit Guest profile from iframe", groups = { "regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void editIframeGuest() throws InterruptedException {

		String oldBarcode;
		// iframeCheckin
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		userEmail = "updated" + userEmail;
		// edit profile.
		String updatedFname = CreateDateTime.getUniqueString("Testfname");
		String updateLname = CreateDateTime.getUniqueString("Testlname");
		pageObj.iframeSingUpPage().editProfile(userEmail, updatedFname, updateLname);
		//
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyUpdatedGuestTimeline(dataSet.get("joinedViaWebEmail"),
				userEmail, updatedFname, updateLname), "Error in verifying guest time line ");
	}

	@Test(description = "SQ-T3828	Verify superadmin can create new redeemables "
			+ "SQ-T3829	Verify superadmin can edit redeemables", groups = { "regression", "dailyrun" }, priority = 5)
	@Owner(name = "Amit Kumar")
	public void T3829_verifyRedeemableIsEditable() throws InterruptedException {
		String redeemableName = "AutomationRedeemable_" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(redeemableName, "Flat Discount", "",
				dataSet.get("amount"));

		String description = dataSet.get("description");
		double updatedAmount = Double.parseDouble(dataSet.get("updatedAmount"));

		pageObj.redeemablePage().editTheRedeemable(redeemableName, description, updatedAmount + "");

		pageObj.redeemablePage().searchAndClickOnRedeemable(redeemableName);

		boolean result = pageObj.redeemablePage().verifyRedeemableDescriptionAndAmount(redeemableName, description,
				updatedAmount + "");

		Assert.assertTrue(result, "Description/Amount did not matched after updation ");
		TestListeners.extentTest.get().pass("Verified that superadmin is able to create and edit the redeemable ");
		logger.info("Verified that superadmin is able to create and edit the redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		// add delete created redeemable code here
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	@Test(description = "SQ-T6123: Validate that hardcoded placeholder text is removed in phone number field of iframe", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Rajasekhar Reddy")
	public void T61213_VerifyPhonePlaceholderText() throws InterruptedException {

		// Verify phone placeholder in iframesignup page
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String phnplaceholdertxt = pageObj.iframeSingUpPage().getPhonePlaceholder();
		Assert.assertTrue(phnplaceholdertxt == null || phnplaceholdertxt.isEmpty(),
				"Expected no phone placeholder text in signup page, but found: " + phnplaceholdertxt);
		// Verify phone placeholder in editprofile page
		String editphnplaceholdertxt = pageObj.iframeSingUpPage().getEditProfilePhonePlaceholder();
		Assert.assertTrue(editphnplaceholdertxt == null || editphnplaceholdertxt.isEmpty(),
				"Expected no phone placeholder text in edit profile page, but found: " + editphnplaceholdertxt);
		TestListeners.extentTest.get().pass(
				"Placeholder text is removed in phone number field in both sigup and edit profile page of iframe");
		logger.info("Placeholder text is removed in phone number field in both sigup and edit profile page of iframe");

	}

	@Test(description = "SQ-T6630: Verify Email Verification confirmation message on landing page is configurable", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Rajasekhar Reddy")
	public void BOP_T365_VerifyEmailVerificationConfirmationMSG() throws Exception {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().setSendEmailVerification();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.iframeConfigurationPage().ClickUserSignupEditProfileTab();
		pageObj.iframeConfigurationPage().verifyEmailConfirmationFields(dataSet.get("confirmtxt"),
				dataSet.get("alreadyconfirmtxt"));
		// User signup and conformation link generation
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();
		String query = "SELECT confirmation_token FROM users where email='" + userEmail + "'";
		// String confirmation_token =
		// SingletonDBUtils.executeQueryAndGetColumnValue(env, query,
		// "confirmation_token");
		String confirmation_token = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "confirmation_token",
				20);
		TestListeners.extentTest.get().info("confirmation_token is availble: " + confirmation_token);
		logger.info("confirmation_token is availble: " + confirmation_token);
		// Verify custom message when user used Confirmation link was clicked first time
		// and user is not logged in.
		String confirmtxt1 = pageObj.iframeSingUpPage()
				.navigateToEmailConfirmationPage(baseUrl + "/customers/confirmation.iframe?confirmation_token="
						+ confirmation_token + "&slug=" + dataSet.get("slug"));
		Assert.assertEquals(dataSet.get("confirmtxt"), confirmtxt1, "Email confirmation custom text didn't matched");
		TestListeners.extentTest.get()
				.pass("Email Verification Confirmation custom text was displayed on the iframe confirmation page");
		logger.info("Email Verification Confirmation custom text was displayed on the iframe confirmation page");
		// Verify custom message when link was already used and user is not logged in.
		String alreadyconfirmtxt = pageObj.iframeSingUpPage()
				.navigateToEmailAlreadyConfirmPage(baseUrl + "/customers/confirmation.iframe?confirmation_token="
						+ confirmation_token + "&slug=" + dataSet.get("slug"));
		Assert.assertEquals(dataSet.get("alreadyconfirmtxt"), alreadyconfirmtxt,
				"Email Verification Already Confirmed text didn't matched");
		TestListeners.extentTest.get()
				.pass("Email Verification Already Confirmed custom text was displayed on the iframe confirmation page");
		logger.info("Email Verification Already Confirmed custom text was displayed on the iframe confirmation page");
		// Verify custom message when user was logged in and clicked on link which was
		// already used.
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		String alreadyconfirmtxt2 = pageObj.iframeSingUpPage().verifyEmailCustomConfirmationTxtWhenUserloggedIn(
				baseUrl + "/customers/confirmation.iframe?confirmation_token=" + confirmation_token + "&slug="
						+ dataSet.get("slug"));
		Assert.assertEquals(dataSet.get("alreadyconfirmtxt"), alreadyconfirmtxt2,
				"Email Verification Already Confirmed text didn't matched");

	}

	@Test(description = "SQ-T4931: Validate added configurable title for merchandise form.", groups = { "regression" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T4931_configurableTitleForMerchandiseForm() throws Exception {

		// API2 User sign up and gift points
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Response giftPointsResponse = pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"), "", "",
				"", dataSet.get("giftCount"));
		Assert.assertEquals(giftPointsResponse.getStatusCode(), 201);
		TestListeners.extentTest.get().pass("API2 User is signed up and points are gifted");
		logger.info("API2 User is signed up and points are gifted");

		// Ensure 'size' is added in the 'Redeemable Attributes' field
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().enterRedeemableAttributes("", "size");

		// Ensure required flags are enabled
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().navigateToTabs("Redemption");
		utils.checkUncheckFlag("Show Available Redeemable for loyalty card redemption?", "check");
		utils.checkUncheckFlag("Show additional details form in offers", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		/*
		 * Verify the text for 'Merchandise Form Title' is set to 'Please provide
		 * additional details to redeem'
		 */
		String expectedMerchandiseFormTitle = dataSet.get("merchandiseFormTitle");
		pageObj.iframeConfigurationPage().editInputField("Merchandise Form Title",
				"iframePage.redemptionTabMerchandiseFormTitle", expectedMerchandiseFormTitle);
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		String redemptionTabMerchandiseFormTitle = pageObj.iframeConfigurationPage()
				.getAttributeValue("iframePage.redemptionTabMerchandiseFormTitle", "value", "");
		Assert.assertEquals(redemptionTabMerchandiseFormTitle, expectedMerchandiseFormTitle,
				"Merchandise form title on Redemption tab did not match");

		// Verify the update in Audit Log
		pageObj.guestTimelinePage().clickAuditLog();
		String merchandiseFormTitleAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogs("Merchandise Form Title", "");
		Assert.assertEquals(merchandiseFormTitleAuditLogValue, expectedMerchandiseFormTitle,
				"'Merchandise Form Title' value not matched");
		TestListeners.extentTest.get().pass("Verified 'Merchandise Form Title' on Redemption tab and Audit log as: "
				+ merchandiseFormTitleAuditLogValue);
		logger.info("Verified 'Merchandise Form Title' on Redemption tab and Audit log as: "
				+ merchandiseFormTitleAuditLogValue);

		// Verify that form doesn't appear for redeemable without size attribute
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		pageObj.segmentsBetaPage().verifyPresenceAndClick(utils.getLocatorValue("iframePage.redeemRewardsLink"));
		pageObj.iframeConfigurationPage().selectReward(dataSet.get("rewardWithoutAttribute"));
		String merchandiseFormAttributeValue = pageObj.iframeConfigurationPage()
				.getAttributeValue("iframePage.merchandiseForm", "style", "");
		Assert.assertTrue(merchandiseFormAttributeValue.contains("display: none"),
				"Merchandise form displayed for redeemable without size attribute");
		TestListeners.extentTest.get()
				.pass("Verified that merchandise form does not appear for redeemable without size attribute");
		logger.info("Verified that merchandise form does not appear for redeemable without size attribute");

		// Verify that form appears for redeemable with size attribute
		pageObj.iframeConfigurationPage().selectReward(dataSet.get("reward"));
		merchandiseFormAttributeValue = pageObj.iframeConfigurationPage()
				.getAttributeValue("iframePage.merchandiseForm", "style", "");
		Assert.assertTrue(merchandiseFormAttributeValue.contains("display: block"),
				"Merchandise form did not display for redeemable with size attribute");
		TestListeners.extentTest.get()
				.pass("Verified that merchandise form appears for redeemable with size attribute");
		logger.info("Verified that merchandise form appears for redeemable with size attribute");

		// Verify the font weight for title matches with Unlocked rewards
		String unlockedRewardsFontSize = pageObj.iframeConfigurationPage().getCssPropertyValue("iframePage.labelText",
				"font-weight", "Unlocked rewards");
		String merchandiseFormTitleFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.merchandiseFormTitle", "font-weight", "");
		Assert.assertEquals(merchandiseFormTitleFontSize, unlockedRewardsFontSize, "Font size did not match");

		// Verify the font weight for form fields matches with each other
		String merchandiseFormAddressFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "Address");
		String merchandiseFormCityFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "City");
		String merchandiseFormStateFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "State");
		String merchandiseFormPhoneFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "Phone number");
		String merchandiseFormZipFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "Zip code");
		String merchandiseFormSizeFontSize = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("iframePage.labelText", "font-weight", "Size");
		List<String> fontSizes = Arrays.asList(merchandiseFormAddressFontSize, merchandiseFormCityFontSize,
				merchandiseFormStateFontSize, merchandiseFormPhoneFontSize, merchandiseFormZipFontSize,
				merchandiseFormSizeFontSize);
		for (String fontSize : fontSizes) {
			Assert.assertEquals(fontSize, dataSet.get("formFieldsFontWeight"), "Font sizes did not match");
		}
		TestListeners.extentTest.get().pass("Verified font sizes for merchandise title and form fields");
		logger.info("Verified font sizes for merchandise title and form fields");

		// Verify the set merchandise form title on iFrame Redeem Rewards page
		String merchandiseFormTitle = pageObj.iframeConfigurationPage()
				.getElementText("iframePage.merchandiseFormTitle", "");
		Assert.assertEquals(merchandiseFormTitle, expectedMerchandiseFormTitle,
				"Merchandise form title on iFrame Redeem Rewards page did not match");
		String rewardCode = pageObj.iframeSingUpPage().redeemRewardFromRedeemrewardsWithNewUI(dataSet.get("reward"));
		TestListeners.extentTest.get()
				.pass("Verified merchandise form title, submitted the form and received reward code: " + rewardCode);
		logger.info("Verified merchandise form title, submitted the form and received reward code: " + rewardCode);

		/*
		 * Verify that a changed title on iFrame Configuration is reflected on the
		 * iFrame Redeem Rewards page
		 */
		expectedMerchandiseFormTitle = "Updated " + expectedMerchandiseFormTitle;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().navigateToTabs("Redemption");
		pageObj.iframeConfigurationPage().editInputField("Merchandise Form Title",
				"iframePage.redemptionTabMerchandiseFormTitle", expectedMerchandiseFormTitle);
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		redemptionTabMerchandiseFormTitle = pageObj.iframeConfigurationPage()
				.getAttributeValue("iframePage.redemptionTabMerchandiseFormTitle", "value", "");
		Assert.assertEquals(redemptionTabMerchandiseFormTitle, expectedMerchandiseFormTitle,
				"New Merchandise form title on Redemption tab did not match");
		pageObj.iframeSingUpPage().navigateToEcrm(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.segmentsBetaPage().verifyPresenceAndClick(utils.getLocatorValue("iframePage.redeemRewardsLink"));
		pageObj.iframeConfigurationPage().selectReward(dataSet.get("reward"));
		merchandiseFormTitle = pageObj.iframeConfigurationPage().getElementText("iframePage.merchandiseFormTitle", "");
		Assert.assertTrue(merchandiseFormTitle.equalsIgnoreCase(expectedMerchandiseFormTitle),
				"Updated Merchandise form title on iFrame Redeem Rewards page did not match");
		TestListeners.extentTest.get()
				.pass("Verified the updated Merchandise Form Title on Redemption tab and iFrame Redeem Rewards page");
		logger.info("Verified the updated Merchandise Form Title on Redemption tab and iFrame Redeem Rewards page");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
