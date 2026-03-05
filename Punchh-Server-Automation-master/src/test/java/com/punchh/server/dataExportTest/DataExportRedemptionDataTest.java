package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportRedemptionDataTest {
	static Logger logger = LogManager.getLogger(DataExportRedemptionDataTest.class);
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
		utils = new Utilities(driver);
	}

    @Test(description = "DP1-T243 Validate the redemption data export with the new columns || "
        + "SQ-T5693 Validate addition of 'Reward Origin' field in Redemption Data Export payload.",
        groups = "Regression", priority = 0)
	public void T243_VerifyRedemptionDataExport() throws InterruptedException {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test for " + exportField + " ==");
		String exportName = CreateDateTime.getUniqueString("T243_VerifyRedemptionDataExport");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_data_exports_v2", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		// pageObj.menupage().clickCockpitGuest();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().enableGuestMigrationMgmt();
//		pageObj.dataExportPage().goToDataExport();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Data Export");
		logger.info("Navigated to dataexport page");
		TestListeners.extentTest.get().info("Navigated to dataexport page");

		boolean flag = pageObj.dataExportPage().checkLabelPresent(exportField, "First Name", "Available Fields");
		Assert.assertTrue(flag, "First Name label is not present");
		logger.info("First Name label is present");
		TestListeners.extentTest.get().pass("First Name label is present");
		driver.navigate().refresh();

		boolean flag1 = pageObj.dataExportPage().checkLabelPresent(exportField, "Last Name", "Available Fields");
		Assert.assertTrue(flag1, "Last name label is not present");
		logger.info("Last name label is present");
		TestListeners.extentTest.get().pass("Last name label is present");
		driver.navigate().refresh();

		boolean flag2 = pageObj.dataExportPage().checkLabelPresent(exportField, "User Email", "Available Fields");
		Assert.assertTrue(flag2, "User Email is not present");
		logger.info("User Email label is present");
		TestListeners.extentTest.get().pass("User Email label is present");
		driver.navigate().refresh();

        boolean flag3 = pageObj.dataExportPage().checkLabelPresent(exportField,
				"Points/Amount Requested", "Available Fields");
		Assert.assertTrue(flag3, "Points/Amount requested label is not present");
		logger.info("Points/Amount requested label is present");
		TestListeners.extentTest.get().pass("Points/Amount requested ID label is present");
		driver.navigate().refresh();

        boolean flag5 = pageObj.dataExportPage().checkLabelPresent(exportField, "Reward Origin",
            "Available Fields");
        Assert.assertTrue(flag5, "Reward Origin label is not present");
        logger.info("Reward Origin label is present");
        TestListeners.extentTest.get().pass("Reward Origin label is present");
        driver.navigate().refresh();

		boolean flag4 = pageObj.dataExportPage().checkLabelPresent(exportField, "Discount Amount", "Selected Fields");
		Assert.assertTrue(flag4, "Discount Amount label is not present");
		logger.info("Discount Amounte label is present");
		TestListeners.extentTest.get().pass("Discount Amount ID label is present");

		driver.navigate().refresh();
//		pageObj.dataExportPage().clickOnExport(exportField);

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

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
