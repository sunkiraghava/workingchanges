package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeACDeactivationDeletionAndQRBarcodeTest {

	private static Logger logger = LogManager.getLogger(IframeACDeactivationDeletionAndQRBarcodeTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T5349-- Verify iFrame configs for account deactivation followed by deactivation from iframe", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rajasekhar Reddy")
	public void VerifyIframeACDeactivationConfigurations() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String Durl = driver.getCurrentUrl();

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// Account deletion configurations
		pageObj.iframeConfigurationPage().clickOnPageTab("Account Deletion");
		pageObj.dashboardpage().checkUncheckAnyFlag("Allow Account Deactivation from iFrame?", "check");

		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().clickEditprofileButton();

		// Deactivate account via iFrame
		pageObj.iframeSingUpPage().verifyACDeactivation();

		// User timeline validations for deactivation
		driver.get(Durl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		String status = pageObj.guestTimelinePage().deactivationStatus();
		utils.logit("Account status is : " + status);
	}

	@Test(description = "SQ-T5350-- Verify iFrame configs for account deletion followed by deletion on iframe", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Rajasekhar Reddy")
	public void VerifyIframeACDeletionConfigurations() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// Account deletion configurations
		pageObj.iframeConfigurationPage().clickOnPageTab("Account Deletion");
		pageObj.dashboardpage().checkUncheckAnyFlag("Allow Account Deletion from iFrame?", "check");

		// Set user incinerate days
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.cockpitGuestPage().editUserIncinerateDaysField("2");

		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().clickEditprofileButton();

		// Delete account via iFrame
		pageObj.iframeSingUpPage().verifyACDeletion();

		// User timeline validations for deletion
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		String status = pageObj.guestTimelinePage().delationStatus();
		utils.logit("Account status is : " + status);
	}

	@Test(description = "SQ-T5982 verify QR code  displays when location is selected on the Checkin screen of iFrame.", enabled = true)
	@Owner(name = "Rajasekhar Reddy")
	public void ValidateQRCodeOnCheckInPageIniFrameBasedOnLocationSelection() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().searchAndSelectLocation(dataSet.get("locationName"));
		pageObj.locationPage().SelectPOSDisplayGuestIdentityTypeAsQRCode();

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// Enable QR code flag on check-in tab of iframe configurations
		pageObj.iframeConfigurationPage().clickCheckinTab();
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Show QR code on Checkin screen for earning?",
				"check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Show Location List?", "check");
		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().clickChekinButton();
		pageObj.iframeSingUpPage().verifyQRCodeinCheckinPageWhenLoctionSelected();

	}

	@Test(description = "SQ-T5982 verify that a message displays on iframe checkin screen when No phone number present for a user and "
			+ " POSDisplayGuestIdentityType= Barcode for a location.", enabled = true)
	@Owner(name = "Rajasekhar Reddy")
	public void ValidateBarCodeOnCheckInPageIniFrameBasedOnLocationSelection() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().searchAndSelectLocation(dataSet.get("locationName"));
		pageObj.locationPage().SelectPOSDisplayGuestIdentityTypeAsBarCode();

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");

		// Enable QR code flag on check-in tab of iframe configurations
		pageObj.iframeConfigurationPage().clickCheckinTab();
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Show QR code on Checkin screen for earning?",
				"check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Show Location List?", "check");
		String durl = driver.getCurrentUrl();
		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().clickChekinButton();
		pageObj.iframeSingUpPage().VerifyMSGWhenNOPhone();
		// Add phone number to the user and verify Bar code for selected location
		pageObj.iframeSingUpPage().AddPhoneForaUserOniFrame();
		pageObj.iframeSingUpPage().verifyBarCodeinCheckinPageWhenLoctionSelected();
		String furl = driver.getCurrentUrl();
		// Verify NO code displays on iframe when Show QR code on Checkin screen for
		// earning? flag is turned off
		driver.get(durl);
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Show QR code on Checkin screen for earning?",
				"uncheck");
		driver.get(furl);
		pageObj.iframeSingUpPage().verifyNOQR_BarCodeWhenFlagoff();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
