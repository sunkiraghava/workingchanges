package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeConfigurationTest {

	private static Logger logger = LogManager.getLogger(IframeConfigurationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

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

	// author=Anant
	@Test(description = "SQ-T3457 Validate that in the origin of URL space should not be accepted for iFrame Configuration page.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T3457_OriginUrlSpaceInIframeConfigurationPage() throws InterruptedException {
		String errorMsg, successMsg;
		String errorMsgColor, successMsgColor;

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// eclub widget
		pageObj.iframeConfigurationPage().clickEClubWidgetBtn();

		// using invalid origin in Eclub
		pageObj.iframeConfigurationPage().editEClubPostRegistrationRedirectURL(dataSet.get("invalidURL"));
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		errorMsg = pageObj.iframeConfigurationPage().getErrorMessage();
		errorMsgColor = pageObj.iframeConfigurationPage().msgColor("iframeConfigurationPage.errorMsg");
		Assert.assertEquals(errorMsg, "Error updating Iframe configuration for eClub Widget");
		Assert.assertEquals(errorMsgColor, "#ff0000");

		TestListeners.extentTest.get().pass(
				"Error msg is displayed when invalid url is enter in the Eclub Post Registration Redirect URL and the error msg is coming in the red color");

		// using valid url in Eclub
		pageObj.iframeConfigurationPage().editEClubPostRegistrationRedirectURL(dataSet.get("validURL"));
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		successMsg = pageObj.iframeConfigurationPage().getSuccessMessage();
		successMsgColor = pageObj.iframeConfigurationPage().msgColor("iframeConfigurationPage.successMsg");
		Assert.assertEquals(successMsg, "Iframe configuration updated for eClub Widget");
		Assert.assertEquals(successMsgColor, "#155724");

		TestListeners.extentTest.get().pass(
				"Success msg is displayed when valid url is enter in the Eclub Post Registration Redirect URL and the success msg is coming in the green color");

		// whitelabel URL
		pageObj.iframeConfigurationPage().clickWhitelabelURLBtn();

		// invalid url
		pageObj.iframeConfigurationPage().editInputField("Reset Password URL", "iframeConfigurationPage.resetURLs",
				dataSet.get("invalidURL"));
		pageObj.iframeConfigurationPage().editInputField("Email Confirmation URL",
				"iframeConfigurationPage.emailConfiramtionURL", dataSet.get("invalidURL"));
		pageObj.iframeConfigurationPage().editInputField("T&C URL", "iframeConfigurationPage.TnCUrl",
				dataSet.get("invalidURL"));
		pageObj.iframeConfigurationPage().editInputField("Privacy URL", "iframeConfigurationPage.privacyURL",
				dataSet.get("invalidURL"));
		pageObj.iframeConfigurationPage().editInputField("FAQ URL", "iframeConfigurationPage.faqURl",
				dataSet.get("invalidURL"));

		pageObj.iframeConfigurationPage().clickUpdateBtn();
		errorMsg = pageObj.iframeConfigurationPage().getErrorMessage();
		errorMsgColor = pageObj.iframeConfigurationPage().msgColor("iframeConfigurationPage.errorMsg");
		Assert.assertEquals(errorMsg, "Error updating Iframe configuration for Whitelabel URLs");
		Assert.assertEquals(errorMsgColor, "#ff0000");

		logger.info("error msg is displayed when invalid url is enter in the fields of the whitelabel url tab");
		TestListeners.extentTest.get().pass(
				"error msg is displayed when invalid url is enter in the fields of the whitelabel url tab and error msg is of red color");

		// valid url
		pageObj.iframeConfigurationPage().editInputField("Reset Password URL", "iframeConfigurationPage.resetURLs", "");

		pageObj.iframeConfigurationPage().editInputField("Email Confirmation URL",
				"iframeConfigurationPage.emailConfiramtionURL", dataSet.get("validURL2"));

		pageObj.iframeConfigurationPage().editInputField("T&C URL", "iframeConfigurationPage.TnCUrl",
				dataSet.get("validURL3"));

		pageObj.iframeConfigurationPage().editInputField("Privacy URL", "iframeConfigurationPage.privacyURL",
				dataSet.get("validURL3"));

		pageObj.iframeConfigurationPage().editInputField("FAQ URL", "iframeConfigurationPage.faqURl",
				dataSet.get("validURL4"));

		pageObj.iframeConfigurationPage().clickUpdateBtn();
		successMsg = pageObj.iframeConfigurationPage().getSuccessMessage();
		successMsgColor = pageObj.iframeConfigurationPage().msgColor("iframeConfigurationPage.successMsg");
		Assert.assertEquals(successMsg, "Iframe configuration updated for Whitelabel URLs");
		Assert.assertEquals(successMsgColor, "#155724");
		logger.info("success msg is displayed when valid url is enter in the fields of the whitelabel url tab");
		TestListeners.extentTest.get().pass(
				"success msg is displayed when valid url is enter in the fields of the whitelabel url tab and success msg is of green color");

		// reset the default url value
		pageObj.iframeConfigurationPage().editInputField("T&C URL", "iframeConfigurationPage.TnCUrl", "");
		pageObj.iframeConfigurationPage().editInputField("Privacy URL", "iframeConfigurationPage.privacyURL", "");
		pageObj.iframeConfigurationPage().clickUpdateBtn();
	}

	@Test(description = "SQ-T3647: Validate that the improvements in Editing/Saving in iFrame Configurations.", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T3647_iFrameConfigurationTabsLanguageSupport() throws Exception {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// Verify Update and Cancel button are present on all iFrame Configuration tabs
		List<String> tabs = Arrays.asList("Basic Configuration", "User Signup/Edit Profile", "Button Labels",
				"Customize text", "eClub Widget", "Checkin", "Redemption", "Whitelabel URLs", "Gift Card",
				"Invite Code", "Account Deletion");
		String buttonText;
		for (String tab : tabs) {
			pageObj.dashboardpage().navigateToTabs(tab);
			buttonText = pageObj.iframeConfigurationPage().getElementText("iframeConfigurationPage.updateBtn", "");
			Assert.assertEquals(buttonText, "Update");
			buttonText = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.cancelBtn", "");
			Assert.assertEquals(buttonText, "Cancel");
		}
		TestListeners.extentTest.get()
				.pass("Update and Cancel button are present on all iFrame Configuration tabs: " + tabs);
		logger.info("Update and Cancel button are present on all iFrame Configuration tabs: " + tabs);

		// Set the preferred and alternate language of the business
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.settingsPage().editPreferredLanguage("English");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().selectAlternateLanguage("Spanish");
		TestListeners.extentTest.get().info("Set the Preferred to English and Alternate Language to Spanish");
		logger.info("Set the Preferred to English and Alternate Language to Spanish");

		// Ensure desired fields have language support cleared
		pageObj.menupage().navigateToSubMenuItem("Settings", "Translations");
		pageObj.mobileconfigurationPage().clickFieldInTranslation("Iframe Configuration");
		pageObj.dashboardpage().navigateToTabs("es");
		List<String> translationFields = Arrays.asList("Password Hint", "Checkin Button", "More to fill up card",
				"Barcode Text", "Redemption Code Text", "Invite code hint");
		for (String field : translationFields) {
			pageObj.settingsPage().editInputField("settingsPage.translationField", "", field);
		}
		pageObj.settingsPage().clickUpdateBtn();

		// Go to iFrame Configuration and navigate to tabs with language support
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		Map<String, String> tabToFieldMap = new LinkedHashMap<>();
		tabToFieldMap.put("User Signup/Edit Profile", "Password Hint");
		tabToFieldMap.put("Button Labels", "Checkin Button");
		tabToFieldMap.put("Customize text", "More To Fill Up Card");
		tabToFieldMap.put("Checkin", "Bar Code text");
		tabToFieldMap.put("Redemption", "Redemption Code text");
		tabToFieldMap.put("Invite Code", "Invite code hint");

		for (Map.Entry<String, String> entry : tabToFieldMap.entrySet()) {
			String tab = entry.getKey();
			String field = entry.getValue();
			pageObj.dashboardpage().navigateToTabs(tab);
			// Select en. Input en text. Update changes. Verify en color to be blue
			pageObj.iframeConfigurationPage().verifyLanguageSupportAfterUpdate(field, "en",
					field + " Automation English Text", dataSet.get("blueRgbColor"));
			// Select es. Input es text. Update changes. Verify es color to be green
			pageObj.iframeConfigurationPage().verifyLanguageSupportAfterUpdate(field, "es",
					field + " Automation Spanish Text", dataSet.get("greenRgbColor"));
			// Verify success message
			Assert.assertEquals(utils.getSuccessMessage(), "Iframe configuration updated for " + tab);
		}

		// When user make some changes on first tab then navigate to second tab & click
		// update of second tab, then changes should not be updated in the first tab.
		pageObj.dashboardpage().navigateToTabs("Whitelabel URLs");
		String newUrl = baseUrl + "/businesses";
		pageObj.iframeConfigurationPage().editInputField("Reset Password URL", "iframeConfigurationPage.resetURLs",
				newUrl);
		pageObj.dashboardpage().navigateToTabs("Checkin");
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Whitelabel URLs");
		String currentUrl = pageObj.iframeConfigurationPage().getAttributeValue("iframeConfigurationPage.resetURLs",
				"value", "");
		Assert.assertNotEquals(currentUrl, newUrl);
		TestListeners.extentTest.get().pass(
				"Changes made in the first tab are not updated when navigating to second tab and clicking update.");
		logger.info("Changes made in the first tab are not updated when navigating to second tab and clicking update.");

		// Verify that the changes made also reflect in the Audit log
		pageObj.dashboardpage().navigateToTabs("User Signup/Edit Profile");
		String subscribeTextNew = CreateDateTime.getUniqueString("Automation");
		pageObj.iframeConfigurationPage().editInputField("Subscribe Text",
				"iframeConfigurationPage.subscribeTextInputField", subscribeTextNew);
		pageObj.iframeConfigurationPage().clickUpdateBtn();
		pageObj.guestTimelinePage().clickAuditLog();
		String subscribeTextAuditLogValue = pageObj.mobileconfigurationPage()
				.verifyUpdatedConfigInAuditLogs("Subscribe Text", "");
		Assert.assertEquals(subscribeTextAuditLogValue, subscribeTextNew);
		TestListeners.extentTest.get()
				.pass("Verified change to 'Subscribe Text' on 'User Signup/Edit Profile' tab in Audit log as: "
						+ subscribeTextNew);
		logger.info("Verified change to 'Subscribe Text' on 'User Signup/Edit Profile' tab in Audit log as: "
				+ subscribeTextNew);
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
