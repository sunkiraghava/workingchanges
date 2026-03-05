package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class ReceiptDetailsPendingCheckinTest {
	private static Logger logger = LogManager.getLogger(ReceiptDetailsPendingCheckinTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3959 INT2-992 | Receipts menu_items copied from receipt details - Pending checkin : Double Earning On, Checkin Yes, Slug Same", groups = {
			"nonNightly" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3959_ReceiptDetailsPendingCheckin() throws Exception {

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		utils.longWaitInSeconds(2);
		pageObj.dashboardpage().selectServiceOptionInWhitelabel("OLO");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("olo_service_stop_double_earning", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("olo_service_enable_order_closed_webhook", "check");
		pageObj.dashboardpage().enterProcessOrderClosedEventSlug("Punchh");
		pageObj.dashboardpage().updateWhitelabelOLO();

		// User register/signup using auth Signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		TestListeners.extentTest.get().pass("user signup is done using auth signup api");
		String userId = signUpResponse.jsonPath().get("id").toString();

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();

		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza", "1", "5", "M", "1", "1", "1.0", "1395");
		parentMap.put("Pizza", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", "1", "0", "M", "1", "1", "1.1", "6029");
		parentMap.put("Pizza2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza3", "1", "0", "M", "1", "1", "1.2", "6029");
		parentMap.put("Pizza3", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Pizza4", dataSet.get("item_qty"), "7",
				dataSet.get("item_type"), dataSet.get("item_family"), dataSet.get("item_group"), "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza4", detailsMap4);

//		step-1

		// Auth api online odering checkin
		String externalUid = Integer.toString(Utilities.getRandomNoFromRange(50000, 1000000));
		String date = CreateDateTime.getCurrentDate() + "T23:44:00-08:00";
		String txn = "123456" + CreateDateTime.getTimeDateString();

		Response resp = pageObj.endpoints().authOnlineOrderCheckinWithQC(authToken, "50", dataSet.get("client"),
				dataSet.get("secret"), txn, externalUid, date, parentMap);

		Assert.assertEquals(resp.getStatusCode(), 200, "Status code 200 did not matched for auth online checkin api");
		TestListeners.extentTest.get().pass("pending checkin is done using auth checkin api");
		String externalUID = resp.jsonPath().get("checkin.external_uid").toString();

//		conn = dbUtils.createMySqlDatabaseConnection(prop.getProperty("pp.host"), prop.getProperty("pp.port"),
//				utils.decrypt(prop.getProperty("pp.username")), utils.decrypt(prop.getProperty("pp.password")));
//		stmt = conn.createStatement();

//		step-3

		Thread.sleep(8000);
		String query = "Select menu_items from receipt_details where user_id = '" + userId + "'";

		logger.info("query = " + query);
		TestListeners.extentTest.get().info("query = " + query);

		String menuItems = DBUtils.executeQueryAndGetColumnValue(env,query, "menu_items");
		logger.info("menu_items i.e " + menuItems + " of  " + userEmail);
		TestListeners.extentTest.get().info("menu_items i.e " + menuItems + " of  " + userEmail);

		boolean verification1 = utils.textContains(menuItems, dataSet.get("item_type"));
		Assert.assertTrue(verification1, "menu items did not contains expected item_type");

		boolean verification2 = utils.textContains(menuItems, dataSet.get("item_family"));
		Assert.assertTrue(verification2, "menu items did not contains expected item_family");

		// User login using API2 Signin
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 login");
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user Login is successful ");

//		step-5

		String posRef = Integer.toString(Utilities.getRandomNoFromRange(100000000, 1000000000));
		String orderID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response resp1 = pageObj.endpoints().closeOrderOnline(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("slug"), externalUID, token, userEmail, dataSet.get("locationStoreNumber"), posRef,
				"Punchh", userId, orderID);
		Assert.assertEquals(resp1.getStatusCode(), 200, "Status code 200 did not matched for close Order Online Api");

//		step-6

		Thread.sleep(8000);
		String query1 = "Select punchh_key from checkins where `checkin_type` = 'OnlineCheckin' AND user_id = '"
				+ userId + "';";

		logger.info("query = " + query1);
		TestListeners.extentTest.get().info("query = " + query1);

		String punchhKey = DBUtils.executeQueryAndGetColumnValue(env, query1, "punchh_key");
		logger.info("punchh_key i.e " + punchhKey + " of  " + userEmail);
		TestListeners.extentTest.get().pass("punchh_key i.e " + punchhKey + " of  " + userEmail);

		boolean verification3 = utils.textContains(punchhKey, posRef);
		Assert.assertTrue(verification3, "punchh key did not contains expected posRef");
		logger.info("Verified above posref Number is substring of Punchh_Key after oder close API");
		TestListeners.extentTest.get()
				.pass("Verified above posref Number is substring of Punchh_Key after oder close API");

		String query2 = "Select menu_items from receipt_details where user_id = '" + userId + "'";

		logger.info("query = " + query2);
		TestListeners.extentTest.get().pass("query = " + query2);

		String menuItems2 = DBUtils.executeQueryAndGetColumnValue(env,query2, "menu_items");
		Assert.assertEquals(menuItems2, menuItems, "menu items updated ");
		logger.info("menu_items i.e " + menuItems2 + " of  " + userEmail);
		TestListeners.extentTest.get().info("menu_items i.e " + menuItems2 + " of  " + userEmail);
		logger.info("menu_items i.e " + menuItems2 + " did not change after oder close API");
		TestListeners.extentTest.get().pass("menu_items i.e " + menuItems2 + " did not change after oder close API");

//		step-7

		String query3 = "Select punchh_key from receipt_details where user_id = '" + userId + "'";
		logger.info("query = " + query3);
		TestListeners.extentTest.get().info("query = " + query3);

		String punchhKey1 = DBUtils.executeQueryAndGetColumnValue(env,query3, "punchh_key");
		logger.info("punchh_key i.e " + punchhKey1 + " of  " + userEmail);
		TestListeners.extentTest.get().pass("punchh_key i.e " + punchhKey1 + " of  " + userEmail);

		boolean verification4 = utils.textContains(punchhKey1, posRef);
		Assert.assertTrue(verification4, "punchh key did not contains expected posRef");
		logger.info("Verified above posref Number is substring of Punchh_Key after oder close API");
		TestListeners.extentTest.get()
				.pass("Verified above posref Number is substring of Punchh_Key after oder close API");

		utils.switchToWindowN(1);

		// SideKiq schedules running
		pageObj.cockpitRedemptionsPage().runSidekiqJob(baseUrl, "ReceiptDetailsRestoreWorker"); // Timeline validation
		utils.switchToParentWindow();

//		step-9

		Thread.sleep(10000);
		String query4 = "Select pending_refresh from checkins where user_id = '" + userId + "'";

		logger.info("query = " + query4);
		TestListeners.extentTest.get().info("query = " + query4);

		String pendingRefresh = DBUtils.executeQueryAndGetColumnValue(env,query4, "pending_refresh");
		Assert.assertEquals(pendingRefresh, "0",
				"Sidekiq worker i.e. ReceiptDetailsRestoreWorker is not executed properly");
		logger.info(
				"Verified in checkins that pending_refresh is updated as 0 after oder close API and executing sidekiq worker");
		TestListeners.extentTest.get().pass(
				"Verified in checkins that pending_refresh is updated as 0 after oder close API and executing sidekiq worker");

//		step-11

		String query5 = "Select menu_items from receipt_details where user_id = '" + userId + "'";

		logger.info("query = " + query5);
		TestListeners.extentTest.get().pass("query = " + query5);

		String menuItems3 = DBUtils.executeQueryAndGetColumnValue(env,query5, "menu_items");
		Assert.assertEquals(menuItems3, menuItems, "menu items updated ");
		logger.info("menu_items i.e " + menuItems3 + " of  " + userEmail);
		TestListeners.extentTest.get().info("menu_items i.e " + menuItems3 + " of  " + userEmail);
		logger.info(
				"menu_items i.e " + menuItems3 + " did not change after oder close API and executing sidekiq worker");
		TestListeners.extentTest.get().pass(
				"menu_items i.e " + menuItems3 + " did not change after oder close API and executing sidekiq worker");

//		step-12

		String query6 = "Select menu_items from receipt_details where user_id = '" + userId + "'";

		logger.info("query = " + query6);
		TestListeners.extentTest.get().info("query = " + query6);

		String menuItems4 = DBUtils.executeQueryAndGetColumnValue(env,query6, "menu_items");
		Assert.assertEquals(menuItems4, menuItems, "menu items updated ");
		logger.info("menu_items i.e " + menuItems4 + " of  " + userEmail);
		TestListeners.extentTest.get().info("menu_items i.e " + menuItems4 + " of  " + userEmail);
		logger.info(
				"menu_items i.e " + menuItems4 + " did not change after oder close API and executing sidekiq worker");
		TestListeners.extentTest.get().pass(
				"menu_items i.e " + menuItems4 + " did not change after oder close API and executing sidekiq worker");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
