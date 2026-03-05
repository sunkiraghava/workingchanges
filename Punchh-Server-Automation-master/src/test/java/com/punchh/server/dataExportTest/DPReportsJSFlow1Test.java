package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DPReportsJSFlow1Test {
	static Logger logger = LogManager.getLogger(DPReportsJSFlow1Test.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws InterruptedException {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5215 Location Report handling access based on JS side", groups = "Regression", priority = 1)
	public void T5215_LocationReport() throws InterruptedException {
		// Step 1: Login to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Step 2: Navigate to Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Precondition : Flag for Tableau availability under Cockpit.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Tableau Access", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Step 3: Navigate to Cockpit -> Enable Flag for "Punchh report via databrick"
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Punchh reports via databricks", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Step 4: Navigate to Cockpit -> Pre-condition to save the embed link, set the
		// permission level Tier1/2/3 and update
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.dashboardpage().selectPermissionLevel("Tier1");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Step 5: Navigate to Cockpit -> Tableau Analytics > Location Report
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Location Report");

		// Step 6: Enter the Embed link for Location Report
		pageObj.cockpitGuestPage().enterDataInEmbedCode("location_report", dataSet.get("link"));

		// Step 7: Navigate to Reports -> Location Report
		pageObj.menupage().navigateToSubMenuItem("Reports", "Location Report");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		logger.info("Location Report: Time field, selected susscefully");

		// Step 8: Validation of the Location Report, All available fields
		boolean isLocationSelected = pageObj.dashboardpage().isLocationSelected("All Locations");
		boolean locationReportHeading = pageObj.dashboardpage().verifyReportHeading("Location Reports");
		Assert.assertTrue(isLocationSelected, "Location is not selected");
		Assert.assertTrue(locationReportHeading, "Location Report heading did not match");

		TestListeners.extentTest.get().pass("Location Report: Available JS fields are validated successfully");
	}

	@Test(description = "SQ-T5182 Verify 'Liability Report", groups = "Regression", priority = 2)
	public void T5182_LiabilityReport() throws Exception {
		// Step 1: Login to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Step 2: Navigate to Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step 3 : Navigate to Cockpit page -> Tableau Analytics > Tableau Reporting >
		// Business Liability
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Business Liability");

		// Step 4: Enter the Embed link
		pageObj.cockpitGuestPage().enterDataInEmbedCode("business_liability", dataSet.get("link"));

		// Step 5: Navigate to Reports -> Liability Report
		pageObj.menupage().navigateToSubMenuItem("Reports", "Liability Report");
		pageObj.dashboardpage().selectLiabilityAsOnDate();
		logger.info("Liability As On Date field, selected successfully");

		// Step 6: Validation of the Liability Report, All available fields
		boolean liabilityReportHeading = pageObj.dashboardpage().verifyReportHeading("Liabilty");
		Assert.assertTrue(liabilityReportHeading, "Liability Report heading did not match");

		TestListeners.extentTest.get().pass("Liability Report detail: Available JS fields are validated successfully");
	}

	@Test(description = "SQ-T3780 Admin Activity report handling JS", groups = "Regression", priority = 3)
	public void T3780_AdminActivityReport() throws InterruptedException {
		// Step 1: Login to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Step 2: Navigate to Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step 3: Navigate to Cockpit -> Tableau Analytics
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Admin Activity");

		// Step 4: Enter the Embed link
		pageObj.cockpitGuestPage().enterDataInEmbedCode("admin_activity", dataSet.get("link"));

		// Step 5: Navigate to Reports -> Admin Activity
		pageObj.menupage().navigateToSubMenuItem("Reports", "Admin Activity Report");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		logger.info("Admin Activity Report: Time field, selected successfully");

		// Step 6: Validation of Admin Activity Report for all available fields
		boolean adminActivityReportHeading = pageObj.dashboardpage().verifyReportHeading("Admin Activity Report");
		Assert.assertTrue(adminActivityReportHeading, "Admin Activity Report heading did not match");

		TestListeners.extentTest.get().pass("Admin Activity Report: Available JS fields are validated successfully");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

}