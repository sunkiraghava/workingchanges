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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportLiabilityReportTest {
	static Logger logger = LogManager.getLogger(DataExportLiabilityReportTest.class);
	public WebDriver driver;
	private ApiUtils apiUtils;
	private String userEmail;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);

	}

	@Test(description = "SQ-T4292 Verify 'iframe' to embed Business Liability Report", priority = 0)
	public void T4292_businessLiabilityReport() throws Exception {

		// need to re-verify the code

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable cockpit ->dashboard->major features->Enable Tableau Access
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Tableau Access", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// navigate to cockpit -> Tableau Analytics
		// check box are not available
		// so commenting below code as discussed with ashwini
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
//		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
//		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable business liability report", "check");
//		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Liability Report");
		pageObj.cockpitGuestPage().enterDataInEmbedCode(dataSet.get("locator"),dataSet.get("link"));
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Reports", "Liability Report");
		pageObj.dataExportPage().verifyBusinessLiabilityReportValues("Business Liability Report");
		pageObj.dataExportPage().verifySubHeadingOnBusinessLiabilityReport("Liability as on");

	}

	@Test(description = "SQ-T4534 Redemption report: Verify that clicking the redemption link on the dashboard landing page directs the user to the redemption Tableau report URL: /report_center?tab=redemption_report. || "
			+ "SQ-T4535 Redemption report:  Confirm that clicking the redemption link on the dashboard landing page directs the user to the redemption Tableau report URL for 2-3 different businesses.", groups = "Regression", dataProvider = "TestDataProvider")
	public void T197_verifyRedeemableReport(String slug) throws InterruptedException {
		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// navigate to stats config. -> location Scoreboards -> Databricks
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().dashboardsScoreboards("Databricks");
		pageObj.dashboardpage().dashboardsGraph("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// Navigate to business
		pageObj.instanceDashboardPage().selectBusiness(slug);

		// enable cockpit ->dashboard->major features->Enable Tableau Access
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Tableau Access", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// navigate to cockpit -> Tableau Analytics
		/// check box are not available
		// so commenting below code as discussed with ashwini
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
//		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
//		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable redemption report", "check");
//		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Redemption Report");
		pageObj.cockpitGuestPage().enterDataInEmbedCode("redemption_report", dataSet.get("link"));
		pageObj.dashboardpage().clickOnUpdateButton();

		// dashboard -> click on Redemption under Redemptions And Sales section on
		// dashboard stats page
//		pageObj.menupage().navigateToSubMenuItem("Dashboard", "");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectDataRange("Last 180 Days");
		String heading = pageObj.dashboardpage().dashboardStatsRedemptionsAndSales();
		Assert.assertEquals(heading, "Redemption Report", "Report title is not matching");
		logger.info("Report title is matching which is " + heading);
		TestListeners.extentTest.get().pass("Report title is matching which is " + heading);

		// reports -> Redemption Report
		boolean verification = utils.verifyPartOfURL(dataSet.get("partOfURL"));
		Assert.assertTrue(verification, "Redemption Report URL is not correct");
		logger.info("Redemption Report URL is correct");
		TestListeners.extentTest.get().pass("Redemption Report URL is correct");

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"slug"},
				{ "wingstop" }, { "winghouse" }, { "autoseven" } };

	}

	@Test(description = "DP1-T211 databricks_enabled flag from WOW and MOM Report: Verify that Punchh report via databricks is available under cockpit dashboard, also user is able to enable and disable the flag.", priority = 0)
	public void T211_punchhReportViaDatabricks() throws Exception {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// navigate to stats config. -> location Scoreboards -> Databricks
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().StatsConfigToDatabrickes("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();
		logger.info("Punchh reports connection is changed to Databricks");
		TestListeners.extentTest.get().info("Punchh reports connection is changed to Databricks");
		pageObj.dashboardpage().verifyPunchhReportStatsConfig("Databricks");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// Navigate to business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// cockpit -> dashboard -> Miscl. -> Punchh reports via databricks -> check
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Punchh reports via databricks", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		boolean result = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(result, "Cockpit flag updation is not successful");
		logger.info("Cockpit flag updation is successful");
		TestListeners.extentTest.get().pass("Cockpit flag updation is successful");

		// Run Schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().createScheduleWOWorMOM(dataSet.get("scheduleHREF"), "", "",
				"Schedule updated successfully.");
		logger.info(dataSet.get("scheduleName") + " Schedule is successsfully verified");
		TestListeners.extentTest.get().pass(dataSet.get("scheduleName") + " Schedule is successsfully verified");

		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName1"));
		pageObj.schedulespage().createScheduleWOWorMOM(dataSet.get("scheduleHREF1"), "", "",
				"Schedule updated successfully.");
		logger.info(dataSet.get("scheduleName1") + " Schedule is successsfully verified");
		TestListeners.extentTest.get().pass(dataSet.get("scheduleName1") + " Schedule is successsfully verified");

		// cockpit -> dashboard -> Miscl. -> Punchh reports via databricks -> uncheck
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Punchh reports via databricks", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		boolean result2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(result2, "Cockpit flag updation is not successful");
		logger.info("Cockpit flag updation is successful");
		TestListeners.extentTest.get().pass("Cockpit flag updation is successful");

	}

//	@Test(description = "DP1-T225 Enable Databricks flag removal from business cockpit dashboard Miscellaneous Config:  Verify that the flag Enable Databricks is removed from the UI at the location Cockpit > Dashboard > Miscellaneous Config. Also, verify this for 2-3 businesses. || "
//			+ "DP1-T226 Enable Databricks flag removal from business cockpit dashboard Miscellaneous Config:  Verify that lift reports are loaded without encountering any 500 internal error.")
	public void T225_punchhReportViaDatabricks() throws Exception {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// navigate to stats config. -> location Scoreboards -> Databricks
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().StatsConfigToDatabrickes("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();
		logger.info("Punchh reports connection is changed to Databricks");
		TestListeners.extentTest.get().info("Punchh reports connection is changed to Databricks");
		pageObj.dashboardpage().verifyPunchhReportStatsConfig("Databricks");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Stats Service?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Databricks enabled", "check");
		pageObj.dashboardpage().clickOnUpdateGlobalConfigButton();

		pageObj.dashboardpage().navigateToAllBusinessPage();

		// Navigate to business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// cockpit -> dashboard -> Miscl. -> Enable Databricks -> check
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		Boolean flag = pageObj.dashboardpage().flagPresentorNot("Enable Databricks");
		Assert.assertFalse(flag, "Enable Databricks flag is presnt in Miscellaneous Config");
		logger.info("Enable Databricks flag is not presnt in Miscellaneous Config");
		TestListeners.extentTest.get().pass("Enable Databricks flag is not presnt in Miscellaneous Config");

		pageObj.menupage().navigateToSubMenuItem("Reports", "Lift Report");
		pageObj.settingsPage().liftReport(dataSet.get("liftReportHeading"));
		logger.info("Verified that lift reports are loaded without encountering any 500 internal error");
		TestListeners.extentTest.get()
				.pass("Verified that lift reports are loaded without encountering any 500 internal error");
	}

//	@Test(description = "SQ-T5216 Location scoreboard report handling access based on JS side.", priority = 0)
	public void T5216_LocationScoreboardReport() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// navigate to stats config. -> location Scoreboards -> Databricks
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().locationScoreboards("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// Navigate to business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable cockpit ->dashboard->major features->Enable Tableau Access
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Tableau Access", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// enable cockpit ->dashboard->Report->Enable Reports
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Reporting");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Show Dumpster Divers Report?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Show Admin Gifting Report?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Show Total Gifts in Statistics?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Show App Download Statistics?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Supports Online Ordering?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Show Lift Report on Dashboard?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Category Reporting?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Send Tobacco Scan Data", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Punchh reports via databricks", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// navigate to cockpit -> Tableau Analytics
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.dashboardpage().selectPermissionLevel("Tier1");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Tableau Reporting");
		pageObj.dashboardpage().navigateToTabs("Location Scoreboard");
		pageObj.cockpitGuestPage().enterDataInEmbedCode("business_liability", dataSet.get("link"));

		pageObj.dashboardpage().clickOnUpdateButton();

		// Reports -> Location Scoreboard
		pageObj.menupage().navigateToSubMenuItem("Reports", "Location Scoreboard");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		pageObj.dashboardpage().selectLocation("All Locations");

		pageObj.dashboardpage().dpTablue();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}