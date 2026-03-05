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
public class DataExportSubscriptionDiscountDataTest {
	static Logger logger = LogManager.getLogger(DataExportSubscriptionDiscountDataTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	private String blankSpace;
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
		blankSpace = "";
		utils = new Utilities(driver);
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T3461 Verify new Data export being added for Subscription Discount Data || "
			+ "SQ-T3501 Verify the Data sheet generated through Subscription Discount Data Export", groups = "Regression", priority = 0)
	public void T3461_verifySubscriptionDiscountDataExport() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		logger.info("== Data export validation test ==");
		List<String> dbList = new ArrayList<>();
		String b_id = dataSet.get("business_id");
		String query = "select sd.id as subscription_discount_id, sd.user_id, sd.user_subscription_id, sd.subscription_plan_id,sp.name as subscription_plan_name, sd.created_at as subscription_discount_created_date_time_utc, sd.threshold,\n"
				+ "dr.priority as threshold_order, sd.amount as discount_amount_per_unit_of_threshold, sd.discounted_quantity, sd.discounted_amount as discounted_amount, sd.discounting_rule_id, sd.program_benefit_id, pb.benefit_type\n"
				+ "from subscription_discounts sd join subscription_plans sp on sd.business_id = sp.business_id and sd.subscription_plan_id = sp.id join discounting_rules dr on sd.business_id = dr.business_id and sd.discounting_rule_id = dr.id\n"
				+ "join program_benefits pb on sd.business_id = pb.business_id and sd.program_benefit_id = pb.id where sd.business_id = "
				+ b_id + ";";
		ResultSet resultSet = DBUtils.getResultSet(env, query);
		while (resultSet.next()) {
			String id = resultSet.getString("user_id");
			dbList.add(id);
			logger.info(id + " is ID");
		}

		String exportName = CreateDateTime.getUniqueString("T3461_AutoDataExport");
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
		List<String> fieldList = pageObj.dataExportPage().createNewDataExport(exportName, "Subscription Discount Data",
				"select all");
		int size = fieldList.size();
		Assert.assertEquals(size, 15, "No of fields in export list is not expected (i.e. 15)");
		logger.info("15 elements is present in data export");
		TestListeners.extentTest.get().info("15 elements is present in data export");
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		String fileName = pageObj.schedulePage().verifyExportSchedule(env, prop.getProperty("dataExportSchedule"),
				exportName, exportName);
		Assert.assertTrue(pageObj.schedulePage().verifyColumns(fileName, fieldList), "Failed to verify columns");
		logger.info("Successfully verified cloumns of Subscription Discount Data report");
		TestListeners.extentTest.get().pass("Successfully verified cloumns of Subscription Discount Data report");
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
