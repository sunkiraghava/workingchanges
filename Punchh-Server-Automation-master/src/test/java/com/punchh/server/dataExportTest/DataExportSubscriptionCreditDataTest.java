package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportSubscriptionCreditDataTest {
	static Logger logger = LogManager.getLogger(DataExportSubscriptionCreditDataTest.class);
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
	@Test(description = "SQ-T3460 Verify new Data export being added for Subscription Credit Data || "
			+ "SQ-T3502	Verify the Data sheet generated through Subscription Credit Data Export", groups = "Regression", priority = 0)
	public void T3460_verifySubscriptionCreditDataExport() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		logger.info("== Data export validation test ==");
		String exportField = dataSet.get("exportField");
		logger.info("== Data export validation test ==");
		List<String> dbList = new ArrayList<>();
		String b_id = dataSet.get("business_id");
		String query = "select rc.id as reward_credit_id, rc.user_id, rc.rewarded_by_id as user_subscription_id, us.subscription_plan_id, sp.name as subscription_plan_name, rc.rewarded_for_id as subscription_discount_id,\n"
				+ "rc.type as reward_credit_type, rc.banked_reward_value, rc.created_at as reward_credit_created_date_time_utc, rc.reward_reason as reward_credit_reason, rc.expiring_at as reward_credit_expiration_date_time_utc\n"
				+ "from reward_credits rc join user_subscriptions us on us.business_id = rc.business_id and us.user_id = rc.user_id and us.id = rc.rewarded_by_id\n"
				+ "join subscription_plans sp on us.business_id = sp.business_id and us.subscription_plan_id = sp.id where rc.rewarded_by_type = 'UserSubscription' and rc.type = 'SubscriptionCredit' and rc.rewarded_for_type = 'SubscriptionDiscount'\n"
				+ "and rc.business_id =" + b_id + ";";
		ResultSet resultSet = DBUtils.getResultSet(env, query);
		while (resultSet.next()) {
			String id = resultSet.getString("user_id");
			dbList.add(id);
			logger.info(id + " is ID");
		}
		DBManager.closeConnection();
		String exportName = CreateDateTime.getUniqueString("T3460_AutoDataExport");
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
		logger.info("Sucessfully verfied cloumns of " + exportField + " report");
		TestListeners.extentTest.get().pass("Sucessfully verfied cloumns of " + exportField + " report");
		boolean flag = pageObj.dataExportPage().verifyColumnsValueWithDbValue(fileName, dbList);
		Assert.assertTrue(flag, "user id from downloaded file is not equal to DB user id");
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
