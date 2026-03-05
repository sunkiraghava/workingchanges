package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.ExcelUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportSubscriptionDebitDataTest {
	static Logger logger = LogManager.getLogger(DataExportSubscriptionDebitDataTest.class);
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

	@SuppressWarnings("static-access")

	@Test(description = "SQ-T3462 Verify new Data export being added for Subscription Debit Data || "
			+ "SQ-T3503	Verify the Data sheet generated through Subscription Debit Data Export", groups = "Regression", priority = 0)
	public void T3462_verifySubscriptionDebitDataExport() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		logger.info("== Data export validation test ==");
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test ==");
		List<String> dbList = new ArrayList<>();
		String b_id = dataSet.get("business_id");
		String query = "select rd.id as reward_debit_id, rd.user_id, rd.honored_reward_value, rd.reward_credit_id, rd.redemption_id, rd.created_at as reward_debit_created_date_time_utc, rd.type as reward_debit_type from reward_debits rd where rd.business_id = "
				+ b_id + "\n"
				+ "and rd.reward_credit_id in ( select id from reward_credits where rewarded_by_type = 'UserSubscription' and type = 'SubscriptionCredit' and rewarded_for_type = 'SubscriptionDiscount' and business_id = "
				+ b_id + " );";

		ResultSet resultSet = DBUtils.getResultSet(env, query);
		while (resultSet.next()) {
			String id = resultSet.getString("user_id");
			dbList.add(id);
			logger.info(id + " is ID");
		}
		DBManager.closeConnection();

		String exportName = CreateDateTime.getUniqueString("T3462_AutoDataExport");
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
		List<String> fieldList = pageObj.dataExportPage().createNewDataExport(exportName, exportField, "select all");
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		String fileName = pageObj.schedulePage().verifyExportSchedule(env, prop.getProperty("dataExportSchedule"),
				exportName, exportName);
		Assert.assertTrue(pageObj.schedulePage().verifyColumns(fileName, fieldList), "Failed to verify columns");
		logger.info("Successfully verified columns of " + exportField + " report");
		TestListeners.extentTest.get().pass("Successfully verified columns of " + exportField + " report");
		boolean flag = pageObj.dataExportPage().verifyColumnsValueWithDbValue(fileName, dbList);
		Assert.assertTrue(flag, "user id from downloaded file is not equal to DB user id");
		logger.info("user id from downloaded file is equal to DB user id");
		TestListeners.extentTest.get().pass("user id from downloaded file is equal to DB user id");
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

	// This test is just a sample code to validate the data export data with Data
	// base data validation ... we need to remove _Sample from test case to run
	@SuppressWarnings("static-access")
//	@Test(description = "SQ-T3462 Verify new Data export being added for Subscription Debit Data || "
//		+ "SQ-T3503	Verify the Data sheet generated through Subscription Debit Data Export", groups = "Regression")
	public void T3462_verifySubscriptionDebitDataExport_Sample() throws Exception {
		String b_id = dataSet.get("business_id");
		String query = "select rd.id as reward_debit_id, rd.user_id, rd.honored_reward_value, rd.reward_credit_id, rd.redemption_id, rd.created_at as reward_debit_created_date_time_utc, rd.type as reward_debit_type from reward_debits rd where rd.business_id = "
				+ b_id + "\n"
				+ "and rd.reward_credit_id in ( select id from reward_credits where rewarded_by_type = 'UserSubscription' and type = 'SubscriptionCredit' and rewarded_for_type = 'SubscriptionDiscount' and business_id = "
				+ b_id + " ) limit 2;";

		Map<String, Map<String, String>> parentMapFromCSVFile = new LinkedHashMap<String, Map<String, String>>();
		Map<String, Map<String, String>> parentMapFromDataBase = new LinkedHashMap<String, Map<String, String>>();
		parentMapFromDataBase = ExcelUtils.readDataFromDataBaseForDP(env, "user_id", query);

//		System.out.println(
//				"DataExportSubscriptionDebitDataTest.T3462_verifySubscriptionDebitDataExport()  parentMapFromDataBase -- "
//						+ parentMapFromDataBase);

		parentMapFromCSVFile = ExcelUtils.readCSVFile("User ID",
				"subscription_debit-2024070209-4f38b2e53799496eb3cd07266645f5b2.csv", 2);

//		System.out.println(
//				"DataExportSubscriptionDebitDataTest.T3462_verifySubscriptionDebitDataExport()  parentMapFromCSVFile -- "
//						+ parentMapFromCSVFile);

		ExcelUtils.validateExportDataWithDBData(parentMapFromCSVFile, parentMapFromDataBase);
//		System.out.println("*************************************************************8");

		ExcelUtils.validateDatabaseDataWithExportData(parentMapFromDataBase, parentMapFromCSVFile);

	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
