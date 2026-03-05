package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class BulkAccountRefresherTest {
	private static Logger logger = LogManager.getLogger(BulkAccountRefresherTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String externalUID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	// Rakhi
	@Test(description = "SQ-5419 Verify UI and hint on BulkAccountRefresher page at Dashboard >> Cockpit"
			+ "SQ-5418 Verify validation on BulkAccountRefresher page at Dashboard >> Cockpit", priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T5419_verifyBulkAccountRefresher() throws Exception {

		String businessID = dataSet.get("business_id");
		// login to instance

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		boolean flag = pageObj.menupage().navigateToSubMenuItem("Cockpit", "Bulk Account Refresher");
		Assert.assertTrue(flag, "Bulk Account Refresher submenu is not visible under cockpit menu");
		logger.info("Bulk Account Refresher submenu is visible under cockpit menu");
		TestListeners.extentTest.get().pass("Bulk Account Refresher submenu is visible under cockpit menu");
		// navigate to bulk account refresher
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Bulk Account Refresher");
		String text = pageObj.dashboardpage().verifyBulkAccountRefresherHint();
		Assert.assertTrue(
				text.contains(
						"This feature allows you to trigger a bulk account refresh for up to 1,000 guests at a time"),
				"Bulk Account Refresher Hint text does not matched");
		logger.info("Bulk Account Refresher Hint text matched");
		TestListeners.extentTest.get().pass("Bulk Account Refresher Hint text matched");

		// click on refresh button without entering any id
		pageObj.dashboardpage().clickRefreshBtn();
		String msg = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg.contains("Please provide at least one user ID."), "Error message did not displayed");
		logger.info("Error message verified as Please provide at least one user ID");
		TestListeners.extentTest.get().pass("Error message verified as Please provide at least one user ID");

		// click on refresh button with valid user ids without spaces
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(dataSet.get("data1"));
		pageObj.dashboardpage().clickRefreshBtn();
		String msg1 = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg1.contains("Bulk Account Refresh initiated successfully"),
				"Success message text did not matched");
		logger.info("Success message verified as Bulk Account Refresh initiated successfully.");
		TestListeners.extentTest.get().pass("Success message verified as Bulk Account Refresh initiated successfully.");

		// click on refresh button with valid user ids with spaces
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(dataSet.get("data2"));
		pageObj.dashboardpage().clickRefreshBtn();
		String msg2 = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg2.contains("Bulk Account Refresh initiated successfully"),
				"Success message text did not matched");
		logger.info("Success message verified as Bulk Account Refresh initiated successfully.");
		TestListeners.extentTest.get().pass("Success message verified as Bulk Account Refresh initiated successfully.");

		// click on refresh button with valid duplicate user ids
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(dataSet.get("data3"));
		pageObj.dashboardpage().clickRefreshBtn();
		String msg3 = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg3.contains("Bulk Account Refresh initiated successfully"),
				"Success message text did not matched");
		logger.info("Success message verified as Bulk Account Refresh initiated successfully.");
		TestListeners.extentTest.get().pass("Success message verified as Bulk Account Refresh initiated successfully.");

		// click on refresh button with invalid user ids
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(dataSet.get("invalidData1"));
		pageObj.dashboardpage().clickRefreshBtn();
		String msg4 = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg4.contains("All provided user IDs must belong to the current business"),
				"Success message text did not matched");
		logger.info("Success message verified as All provided user IDs must belong to the current business.");
		TestListeners.extentTest.get()
				.pass("Success message verified as All provided user IDs must belong to the current business.");

		// click on refresh button with one valid and one invalid user id without space
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(dataSet.get("invalidData2"));
		pageObj.dashboardpage().clickRefreshBtn();
		String msg5 = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg5.contains("All provided user IDs must belong to the current business"),
				"Success message text did not matched");
		logger.info("Success message verified as All provided user IDs must belong to the current business.");
		TestListeners.extentTest.get()
				.pass("Success message verified as All provided user IDs must belong to the current business.");

		// fetch 1000 user IDs from db
		String getUserIDs = "select id from users where business_id = " + businessID + " limit 1000;";
		pageObj.singletonDBUtilsObj();
		List<String> userIdList = DBUtils.getValueFromColumnInList(env, getUserIDs, "id");
		String duplicateID = userIdList.get(1);

		String inputString = String.join(", ", userIdList.stream().map(String::valueOf).toArray(String[]::new));
		String listWithDuplicateId = inputString + ", " + duplicateID;
		String listWithOneInvalidId = inputString + ", " + "444241567";

		// fill valid 1000 ids with comma separated values
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(inputString);
		pageObj.dashboardpage().clickRefreshBtn();
		String msg6 = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg6.contains("Bulk Account Refresh initiated successfully"),
				"Success message text did not matched");
		logger.info("Success message verified as Bulk Account Refresh initiated successfully.");
		TestListeners.extentTest.get().pass("Success message verified as Bulk Account Refresh initiated successfully.");

		// fill valid 1000 ids with some duplicate IDs
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(listWithDuplicateId);
		pageObj.dashboardpage().clickRefreshBtn();
		String msg7 = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg7.contains("Bulk Account Refresh initiated successfully"),
				"Success message text did not matched");
		logger.info("Success message verified as Bulk Account Refresh initiated successfully.");
		TestListeners.extentTest.get().pass("Success message verified as Bulk Account Refresh initiated successfully.");

		// fill valid 1000 valid ids and one invalid id
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(listWithOneInvalidId);
		pageObj.dashboardpage().clickRefreshBtn();
		String msg9 = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg9.contains("The number of user IDs must not exceed 1000"),
				"Success message text did not matched");
		logger.info("Success message verified as the number of user IDs must not exceed 1000.");
		TestListeners.extentTest.get().pass("Success message verified as the number of user IDs must not exceed 1000.");

		// fetch 1001 user IDs from db
		String getUserID = "select id from users where business_id = " + businessID + " limit 1001;";
		pageObj.singletonDBUtilsObj();
		List<String> userIdList1 = DBUtils.getValueFromColumnInList(env, getUserID, "id");

		String inputString1 = String.join(", ", userIdList1.stream().map(String::valueOf).toArray(String[]::new));

		// fill valid 1001 ids with comma separated values
		pageObj.dashboardpage().enterIdsInBulkAccountRefresher(inputString1);
		pageObj.dashboardpage().clickRefreshBtn();
		String msg8 = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg8.contains("The number of user IDs must not exceed 1000"),
				"Success message text did not matched");
		logger.info("Success message verified as the number of user IDs must not exceed 1000.");
		TestListeners.extentTest.get().pass("Success message verified as the number of user IDs must not exceed 1000.");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
