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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class DataExportSubscriptionTest {
	static Logger logger = LogManager.getLogger(DataExportSubscriptionTest.class);
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
	@Test(description = "SQ-T3500 Verify the Data sheet generated through User Subscription Data Export || "
			+ "SQ-T3693 Data Export: Verify that Data is getting exported for Schedule export after Subscription export tables are moved under ‘*Data Exports*’ section.", priority = 0)
	public void T3500_verifySubscriptionDataExport() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test ==");
		List<String> dbList = new ArrayList<>();
		String b_id = dataSet.get("business_id");
		String query = "select us.user_id, us.id as user_subscription_id, us.subscription_plan_id, sp.name as subscription_plan_name, us.price as user_subscription_price, us.parent_subscription_id as parent_user_subscription_id, us.created_at as user_subscription_created_date_time_utc,\n"
				+ "us.start_time as user_subscription_start_date_time, us.end_time as user_subscription_end_date_time, sp.timezone as timezone, us.auto_renewal as user_subscription_auto_renwal_flag, us.canceled_at as user_subscription_cancelled_at_date_time_utc,\n"
				+ "us.reason as user_subscription_cancellation_reason, us.canceled_by_id as cancelled_by_id, us.canceled_by_type as cancelled_by_type, us.status as user_subscription_status, us.migrated as user_subscription_migration_flag, us.transition_plan_id, us.gift_reason,\n"
				+ "us.transition_subscription_id from user_subscriptions us join subscription_plans sp on us.business_id = sp.business_id and us.subscription_plan_id = sp.id where us.business_id = "
				+ b_id + ";";
		ResultSet resultSet = DBUtils.getResultSet(env, query);
		while (resultSet.next()) {
			String id = resultSet.getString("user_id");
			dbList.add(id);
			logger.info(id + " is ID");
		}

		String exportName = CreateDateTime.getUniqueString("T3500_AutoDataExport");
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

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
