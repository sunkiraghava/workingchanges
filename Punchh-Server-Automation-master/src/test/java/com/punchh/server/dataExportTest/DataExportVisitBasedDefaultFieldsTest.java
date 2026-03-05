package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class DataExportVisitBasedDefaultFieldsTest {
	static Logger logger = LogManager.getLogger(DataExportVisitBasedDefaultFieldsTest.class);
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
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);

	}

	@Test(description = "SQ-T5344 Run All Data Export with all field selected on Visit Based Business (Objective-1)", groups = "Regression")
	public void T5344_verifyDataExportVisitBasedForDefaultFields() throws InterruptedException {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		HashMap<String, ArrayList<String>> fieldMap = new HashMap<>();
		ArrayList<String> fieldList = new ArrayList<String>();
		List<String> logList = Arrays.asList("redemption", "user_feedback", "checkin", "checkin_failure", "referral",
				"receipt_item", "tipping", "locations", "guest", "reward", "archive_rewards", "gift_card",
				"migrated_guest", "guests_awaiting_migration", "campaign", "coupon", "redeemable", "payment",
				"coupon_redemption", "user_subscription", "subscription_discount", "subscription_credit",
				"subscription_debit");

		logger.info("== Data export validation test ==");
		String exportName = CreateDateTime.getUniqueString("T5344_AutoDataExport_DefaultField");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnabledataExportBetabox();
		pageObj.menupage().clickCockpitGuest();
		pageObj.dashboardpage().enableGuestMigrationMgmt();
		pageObj.dataExportPage().goToDataExport();
		pageObj.dataExportPage().setDataExportNameAndDate(exportName);

		List<String> exportNames = pageObj.dataExportPage().listOfDataExport();

		for (String str : exportNames) {
			if (str.equalsIgnoreCase("Data Exports with Optional Modified Records")
					|| str.equalsIgnoreCase("Include records modified in the above date range")) {
				logger.info(str);
				continue;
			}
			fieldList = pageObj.dataExportPage().clickDataExportNameCheckBoxDefaultFields(str);
			String a = str.replace(" Data", "");
			String b = (a.replace(" ", "_")).toLowerCase();
			fieldMap.put(b, fieldList);
		}
		pageObj.dataExportPage().clickOnSaveExportButton();
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		ArrayList<String> fileName = pageObj.schedulePage()
				.verifyExportScheduleForAll(prop.getProperty("dataExportSchedule"), exportName, env, logList, fieldMap);
		Assert.assertEquals(fileName.size(), (exportNames.size() - 2), "Count don't match");
		logger.info("Successfully verified export file count: " + fileName.size());
		TestListeners.extentTest.get().pass("Successfully verified export file count: " + fileName.size());
		// Delete Data Export
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulePage().openSchedule(prop.getProperty("dataExportSchedule"), exportName);
		pageObj.schedulePage().selectDeleteOrDeactivateOptionDataExport(exportName, "Delete");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Schedule deleted successfully.", "Message did not match.");
		logger.info(exportName + " Data Export Export deleted Successfully");
		TestListeners.extentTest.get().pass(exportName + " Data Export Export deleted Successfully");
		for (String fname : fileName) {
			utils.deleteExistingDownload(fname);
		}
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
