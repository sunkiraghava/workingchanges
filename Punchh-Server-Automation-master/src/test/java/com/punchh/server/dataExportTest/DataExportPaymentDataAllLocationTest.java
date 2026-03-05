package com.punchh.server.dataExportTest;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.Utilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class DataExportPaymentDataAllLocationTest {
	static Logger logger = LogManager.getLogger(DataExportPaymentDataAllLocationTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4319 Payment dataSet Export: Ensure that Payments Report is listed in the Data Export section with an option to schedule delivery. Also, check that no errors appear in logs during payment export run. || "
			+ "SQ-T4323 Payment dataSet export: Existing and New schedule: Verify that the payment export runs successfully with all other data sets, including location, location group, and single location for new data export. || "
			+ "SQ-T4429 Location stats export and Payment report option cleanup: Confirm that the removal of these options does not impact the payment and location data sets under the data export schedule, ensuring compatibility with other data sets. || "
			+ "SQ-T4428 Location stats export and Payment report option cleanup: Verify that payment report export and location stats export are successfully removed from the Schedules dropdown.", groups = "Regression", priority = 0)
	public void T4319_T4323_verifyPaymentDataExport() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test for " + exportField + " ==");
		String exportName = CreateDateTime.getUniqueString("T4319_23_AutoDataExportPaymentExportPunch");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_data_exports_v2", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");

		boolean flag = pageObj.schedulePage().verifySchedulePresent("Payment Report Schedule");
		Assert.assertFalse(flag, "Payment Report Schedule is Visible [condition fail]");
		logger.info("Payment Report Schedule is not Visible [condition pass]");
		TestListeners.extentTest.get().pass("Payment Report Schedule is not Visible [condition pass]");

		// pageObj.menupage().clickCockpitGuest();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().enableGuestMigrationMgmt();
		pageObj.dataExportPage().goToDataExport();
		List<String> fieldList = pageObj.dataExportPage().createNewDataExport(exportName, exportField, "select all");
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		String fileName = pageObj.schedulePage().verifyExportSchedule(env, prop.getProperty("dataExportSchedule"),
				exportName, exportName);
		Assert.assertTrue(pageObj.schedulePage().verifyColumns(fileName, fieldList), "Failed to verify columns");
		logger.info("Successfully verified columns of " + exportField + " report");
		TestListeners.extentTest.get().pass("Successfully verified columns of " + exportField + " report");
		// Delete Data Export
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulePage().openSchedule(prop.getProperty("dataExportSchedule"), exportName);
		pageObj.schedulePage().selectDeleteOrDeactivateOptionDataExport(exportName, "Delete");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Schedule deleted successfully.", "Message did not match.");
		logger.info(exportName + " Data Export Export deleted Successfully");
		TestListeners.extentTest.get().pass(exportName + " Data Export Export deleted Successfully");
		utils.deleteExistingDownload(fileName);
	}

	@Test(description = "SQ-T4325 Location scoreboard: Email location scoreboard working fine after switching to databricks. || "
			+ "SQ-T4324 Location scoreboard: Location scoreboard is loading up without any 500 internal server error and email scoreboard functionality working fine after switching to databricks.", groups = "Regression", priority = 0)
	public void T4325_verifyLocationScoreboard() throws Exception {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		// navigate to stats config. -> location Scoreboards -> Databricks
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().locationScoreboards("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();
		logger.info("location Scoreboards connection is changed to Databricks");
		TestListeners.extentTest.get().info("location Scoreboards connection is changed to Databricks");
		logger.info("Stats Configuration location Scoreboards is updated successfully");
		TestListeners.extentTest.get().info("Stats Configuration location Scoreboards is updated successfully");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// Navigate to business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(dataSet.get("location_name"));
		pageObj.locationPage().locationOperation(dataSet.get("operation"));
		String actualErrorMessage = pageObj.locationPage().getErrorSuccessMessage();
		Assert.assertEquals(actualErrorMessage, dataSet.get("expErrorMessage"),
				"Location Scoreboard success message is not matched");
		logger.info("Location Scoreboard success message is :- " + actualErrorMessage);
		TestListeners.extentTest.get().pass("Location Scoreboard success message is :- " + actualErrorMessage);

		// SQ-T4324 Location scoreboard: Location scoreboard is loading up without any
		// 500 internal server error and email scoreboard functionality working fine
		// after switching to databricks.

		// 'Export Location Scoreboard' button had been removed as discussed with Pratik
		// so commenting below code.

//		pageObj.menupage().navigateToSubMenuItem("Reports", "Location Scoreboard");
//		pageObj.locationPage().locationScoreboardReport(dataSet.get("dateRange"));
//		String actualErrorMessage1 = pageObj.locationPage().getErrorSuccessMessage();
//		Assert.assertEquals(actualErrorMessage1, dataSet.get("expErrorMessage1"),
//				"Export Location Scoreboard success message is not matched");
//		logger.info("Export Location Scoreboard success message is :- " + actualErrorMessage1);
//		TestListeners.extentTest.get().pass("Export Location Scoreboard success message is :- " + actualErrorMessage1);

		logger.info("Email scoreboard functionality working fine after switching to databricks");
		TestListeners.extentTest.get()
				.pass("Email scoreboard functionality working fine after switching to databricks");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
