package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DecoupleRollingExpiryAndApiTest {
	static Logger logger = LogManager.getLogger(DecoupleRollingExpiryAndApiTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4495 (1.0)Verify the API's response relation with flags- :enable_qualification_points_from_column, :membership_level_qualification_points", priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T4495_VerifyAPIResponseRelationWithFlag() throws Exception {

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("slugId") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_qualification_points_from_column", dataSet.get("slugId"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info("enable_qualification_points_from_column" + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		// String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		pageObj.endpoints().Api1MobileUsersMembershipLevel(token, dataSet.get("client"), dataSet.get("secret"));
		logger.info("Verified that API response relation with flag");
		TestListeners.extentTest.get().pass("Verified that API response relation with flag");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5930 Verify remove point expiry flag in cockpit >> earning >> checkin expiry should not be visible only when decoupling is OFF. || "
			+ "SQ-T5929 Verify remove point expiry flag in cockpit >> earning >> checkin expiry should be visible only when decoupling is ON.", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5930_VerifyDecouplingFlag() throws Exception {
		String b_id = dataSet.get("business_id");

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// Navigate to earning page -> Checkin Expiry
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");

		// Navigate to earning page -> Checkin Expiry -> Decouple Checkin and Membership
		// Expiry -> On
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Verify that remove point expiry flag is visible
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		boolean flagNameOn = utils.isflagNameVisible(dataSet.get("flagName"));
		Assert.assertTrue(flagNameOn, dataSet.get("flagName") + " is not visible");
		logger.info("Verified that " + dataSet.get("flagName") + " flag will be shown on UI");
		TestListeners.extentTest.get().pass("Verified that " + dataSet.get("flagName") + " flag will be shown on UI");

		// Navigate to earning page -> Checkin Expiry -> Decouple Checkin and Membership
		// Expiry -> Off
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Verify that remove point expiry flag is not visible
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		boolean flagName = utils.isflagNameVisible(dataSet.get("flagName"));
		Assert.assertFalse(flagName, dataSet.get("flagName") + " is visible");
		logger.info("Verified that " + dataSet.get("flagName") + " flag will get hide from UI");
		TestListeners.extentTest.get().pass("Verified that " + dataSet.get("flagName") + " flag will get hide from UI");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value false
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		utils.refreshPage();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");

		// Verify that Decouple Checkin and Membership Expiry flag is not visible
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		boolean flagNameOff = utils.isToggleNameVisible("Decouple Checkin and Membership Expiry");
		Assert.assertFalse(flagNameOff, "Decouple Checkin and Membership Expiry is visible");
		logger.info("Verified that Decouple Checkin and Membership Expiry flag will get hide from UI");
		TestListeners.extentTest.get()
				.pass("Verified that Decouple Checkin and Membership Expiry flag will get hide from UI");

	}

	@Test(description = "SQ-T3859 Decouple expiry : Validate the DECOUPLED_MEMBERSHIP_EXPIRY_ON_SPECIFIC_DATE(points do not expire) strategy on business type points to reward", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T3859_validateDecoupledMembershipExpiryOnSpecificDate() throws Exception {

		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		String status2 = "false";
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		// set "enable_tier_expiry_startergy" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		logger.info(dataSet.get("dbFlag1") + " value is updated to " + status1);
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// step2 - Expiry is set to 1 days
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(dataSet.get("evaluationStrategy"));
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		logger.info(userID);
		String query3 = "Select `expire_item_type` from `expiry_events` where `business_id` = '" + b_id
				+ "' and user_id=" + userID + " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100);
		Assert.assertEquals(expColValue2, "MembershipExpiryOnSpecificDate",
				"Expire item type is not matched with checkin");
		logger.info("Verified that Membership level expiry entry should created successfully.");
		TestListeners.extentTest.get().pass("Verified that Membership level expiry entry should created successfully.");

		String query4 = "Select `value` from `expiry_events` where `business_id` = '" + b_id + "'and user_id=" + userID
				+ " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValue(env, query4, "value");
		Assert.assertNull(expColValue3, "Value is not null for the membership level expiry entry");
		logger.info("Verified that Value is null for the membership level expiry entry.");
		TestListeners.extentTest.get().pass("Verified that Value is null for the membership level expiry entry.");

	}

	@Test(description = "SQ-T3860 Decouple expiry : Validate the DECOUPLED_MEMBERSHIP_EXPIRY_ON_SIGN_UP_ANNIVERSARY(points do not expire) strategy on business type points to reward", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T3860_validateDecoupledMembershipExpiryOnSignUpAnniversary() throws Exception {

		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		String status2 = "false";
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		// set "enable_tier_expiry_startergy" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		logger.info(dataSet.get("dbFlag1") + " value is updated to " + status1);
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// update the user created_at and joined_at date to previous year in users table
		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		logger.info("newcreated_at: " + newcreated_at);
		String query1 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "',`joined_at` = '" + newcreated_at
				+ "' WHERE id = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// update the user created_at date to previous year in accounts table
		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// step2 - Expiry is set to 1 days
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(dataSet.get("evaluationStrategy"));
		pageObj.earningPage().updateConfiguration();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		logger.info(userID);
		String query3 = "Select `expire_item_type` from `expiry_events` where `business_id` = '" + b_id
				+ "' and user_id=" + userID + " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100);
		Assert.assertEquals(expColValue2, "AnnualExpiry", "Expire item type is not matched with checkin");
		logger.info("Verified that Membership level expiry entry has been created successfully.");
		TestListeners.extentTest.get()
				.pass("Verified that Membership level expiry entry has been created successfully.");

		String query4 = "Select `value` from `expiry_events` where `business_id` = '" + b_id + "' and user_id=" + userID
				+ " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValue(env, query4, "value");
		Assert.assertNull(expColValue3, "Value is not null for the membership level expiry entry");
		logger.info("Verified that Value is null for the membership level expiry entry.");
		TestListeners.extentTest.get().pass("Verified that Value is null for the membership level expiry entry.");

	}

	@Test(description = "SQ-T3858 Decouple expiry : Validate the MEMBERSHIP_EXPIRY_ON_BUMP_ANNIVERSARY(points do not expire) strategy on business type points to reward", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T3858_validateDecoupledMembershipExpiryOnBumpAnniversary() throws Exception {

		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		String status2 = "false";
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		// set "enable_tier_expiry_startergy" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		logger.info(dataSet.get("dbFlag1") + " value is updated to " + status1);
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// set the current date in schedule expiry on column in the membership level
		// history table
		String newcreated_at = CreateDateTime.getCurrentDate();
		String membershipLevelId = dataSet.get("membershipLevelId");
		String query1 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at
				+ "' WHERE `user_id` = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// step2 - Expiry is set to 1 days
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(dataSet.get("evaluationStrategy"));
		pageObj.earningPage().updateConfiguration();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		logger.info(userID);
		String query3 = "Select `expire_item_type` from `expiry_events` where `business_id` = '" + b_id
				+ "' and user_id=" + userID + " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100);
		Assert.assertEquals(expColValue2, "MembershipExpiryOnlyOnBumpAnniversary",
				"Expire item type is not matched with checkin");
		logger.info("Verified that Membership level expiry entry has been created successfully.");
		TestListeners.extentTest.get()
				.pass("Verified that Membership level expiry entry has been created successfully.");

		String query4 = "Select `value` from `expiry_events` where `business_id` = '" + b_id + "' and user_id=" + userID
				+ " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValue(env, query4, "value");
		Assert.assertNull(expColValue3, "Value is not null for the membership level expiry entry");
		logger.info("Verified that Value is null for the membership level expiry entry.");
		TestListeners.extentTest.get().pass("Verified that Value is null for the membership level expiry entry.");

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
