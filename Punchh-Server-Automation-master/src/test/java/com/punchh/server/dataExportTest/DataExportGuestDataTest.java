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
public class DataExportGuestDataTest {
	static Logger logger = LogManager.getLogger(DataExportGuestDataTest.class);
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

	@Test(description = "SQ-T2763 Verify Data Report Guest Data set || "
			+ "SQ-T2678 To Verify the attribute added in Guest Data Export || "
			+ "SQ-T2695 Verify 'User Id' is being populated as Selected fields || "
			+ "SQ-T2617	Verify if flag is visible to fetch Guest Export Data by joined date || "
			+ "SQ-T5331 To Verify the attribute added in Guest Data Export", groups = "Regression", priority = 0)
	public void T2763_verifyGuestDataExport() throws InterruptedException {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test for " + exportField + " ==");
		String exportName = CreateDateTime.getUniqueString("T2763_AutoDataExport");
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
		pageObj.dataExportPage().goToDataExport();
		boolean flag = pageObj.dataExportPage().checkLabelPresent(exportField, "User ID", "Selected Fields");
		Assert.assertTrue(flag, "User ID label is not present");
		utils.logit("User ID label is present");
		driver.navigate().refresh();

		boolean flag3 = pageObj.dataExportPage().checkLabelPresent(exportField, "Referral Code", "Available Fields");
		Assert.assertTrue(flag3, "Referral Code label is not present");
		utils.logPass("Referral Code label is present");
		driver.navigate().refresh();

		boolean flag1 = pageObj.dataExportPage().checkLabelPresent(exportField, "Date of Last Earned Points",
				"Selected Fields");
		Assert.assertTrue(flag1, "Date of Last Earned Points label is not present");
		utils.logit("Date of Last Earned Points label is present");
		String value = pageObj.dataExportPage().verifyFlagsInDataExport(exportField);
		Assert.assertEquals(value, dataSet.get("expectedFlagValue"), "Flag value is not matching");
		utils.logit("Flag value " + dataSet.get("expectedFlagValue") + " is matching");
		pageObj.dataExportPage().clickOnExport(exportField);
		List<String> fieldList = pageObj.dataExportPage().createNewDataExport(exportName, exportField, "select all");
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		String fileName = pageObj.schedulePage().verifyExportSchedule(env, prop.getProperty("dataExportSchedule"),
				exportName, exportName);
		Assert.assertTrue(pageObj.schedulePage().verifyColumns(fileName, fieldList), "Failed to verify columns");
		utils.logPass("Successfully verified columns of " + exportField + " report");
		// Delete Data Export
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulePage().openSchedule(prop.getProperty("dataExportSchedule"), exportName);
		pageObj.schedulePage().selectDeleteOrDeactivateOptionDataExport(exportName, "Delete");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Schedule deleted successfully.", "Message did not match.");
		utils.logPass(exportName + " Data Export Export deleted Successfully");
		utils.deleteExistingDownload(fileName);
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
