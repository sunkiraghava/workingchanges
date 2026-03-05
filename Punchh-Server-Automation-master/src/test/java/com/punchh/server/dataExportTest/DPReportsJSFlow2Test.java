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
public class DPReportsJSFlow2Test {
	static Logger logger = LogManager.getLogger(DPReportsJSFlow2Test.class);
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

	@Test(description = "SQ-T5216 Location scoreboard report handling JS", groups = "Regression", priority = 1)

	public void T5216_LocationScoreboardReport() throws InterruptedException {
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

		// Step 4 : Navigate to Cockpit -> Pre-condition to save the embed link, set the
		// permission level and update.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.dashboardpage().selectPermissionLevel("Tier1");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Step 5 : Navigate to Cockpit -> Tableau Analytics > Location Scoreboard
		// Report
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Location Scoreboard");

		// Step 6 : Enter the Embed link for Location Score-board Report.
		pageObj.cockpitGuestPage().enterDataInEmbedCode("location_scoreboard", dataSet.get("link"));

		// Step 7 : Navigate to Reports -> Location Scoreboard
		pageObj.menupage().navigateToSubMenuItem("Reports", "Location Scoreboard");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		logger.info("Location Scoreboard Report: Time field, selected susscefully");

		// Step 8 : Validation the Location Scoreboard Report for all available fields
		boolean isLocationSelected = pageObj.dashboardpage().isLocationSelected("All Locations");
		boolean locationScoreboardReportHeading = pageObj.dashboardpage().verifyReportHeading("Location Scoreboard");
		Assert.assertTrue(isLocationSelected, "Location did not match");
		Assert.assertTrue(locationScoreboardReportHeading, "Location Scoreboard Report heading did not match");

		TestListeners.extentTest.get().pass("Location Scoreboard Report: Available JS fields validated successfully");
	}

	@Test(description = "SQ-T5217 Payments report handling JS", groups = "Regression", priority = 2)
	public void T5217_PaymentsReport() throws InterruptedException {
		// Step 1: Login to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Step 2: Navigate to Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step 3 : Navigate -> Cockpit -> Tableau Analytics > Payments Report Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Payments");

		// Step 4 : Enter the Embed link in Text box of Payments Tab
		pageObj.cockpitGuestPage().enterDataInEmbedCode("payments", dataSet.get("link"));

		// Step 5 : Navigate to Reports -> Payments
		pageObj.menupage().navigateToSubMenuItem("Reports", "Payments Report");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		pageObj.dashboardpage().selectLocation("All Locations");
		logger.info("Payments Report: Time: and Location: fields, are selected susscefully");

		// Step 6 : Validate the Payments Report all available JS fields.
		boolean paymentsReportHeading = pageObj.dashboardpage().verifyReportHeading("Payments Report");
		boolean paymentModeHeading = pageObj.dashboardpage().verifyPaymentMode();
		boolean paymentTransactionTypeHeading = pageObj.dashboardpage().verifyPaymentTransactionType();
		boolean paymentStatusHeading = pageObj.dashboardpage().verifyPaymentStatus();
		boolean paymentFilterByHeading = pageObj.dashboardpage().verifyPaymentFilterBy();
		boolean isSearchButtonClicked = pageObj.dashboardpage().verifyPaymentSearchButton();

		Assert.assertTrue(paymentsReportHeading, "Payments Report heading did not match");
		Assert.assertTrue(paymentModeHeading, "Payment Mode heading did not match");
		Assert.assertTrue(paymentTransactionTypeHeading, "Payment Transaction Type heading did not match");
		Assert.assertTrue(paymentStatusHeading, "Payment Status heading did not match");
		Assert.assertTrue(paymentFilterByHeading, "Payment Filter By heading did not match");
		Assert.assertTrue(isSearchButtonClicked, "Search button was not clicked successfully");

		TestListeners.extentTest.get().pass("Payments report detail: Available JS fields are validated successfully");
	}

	@Test(description = "SQ-T5218 POS Stats report handling JS", groups = "Regression", priority = 3)
	public void T5218_POSStatsReport() throws InterruptedException {
		// Step 1: Login to Business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// Step 2: Navigate to Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step 3 : Navigate -> Cockpit -> Tableau Analytics
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("POS Stats");

		// Step 4 : Enter the Embed link in text box for POS Stats Report.
		pageObj.cockpitGuestPage().enterDataInEmbedCode("pos_stats", dataSet.get("link"));

		// Step 5 : Navigate to Support -> POS Stats Report
		pageObj.menupage().navigateToSubMenuItem("Support", "POS Stats");

		// Step 6 : Validation of the POS Stats Report for all available fields
		boolean posStatsReportHeading = pageObj.dashboardpage().verifyReportHeading("POS Reports");
		Assert.assertTrue(posStatsReportHeading, "POS Stats Report heading did not match");

		TestListeners.extentTest.get().pass("POS Stats Report detail: Available JS fields are validated successfully");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

}