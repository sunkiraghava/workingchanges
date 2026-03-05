package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DecoupleRollingExpiryTest {
	static Logger logger = LogManager.getLogger(DecoupleRollingExpiryTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
//	
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
//		
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4212 Validated the decouple expiry with annually on a specific date(points do not expire)", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4211_decoupleExpiryWithSpecificDate() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

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

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
//		pageObj.earningPage()
//				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		Utilities.longWait(8000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("MembershipExpiryOnSpecificDate");
		Assert.assertEquals(flag, true,
				"MembershipExpiryOnSpecificDate is not present in expire_item_type column in expiry_events table");
		logger.info("MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4224 Validated the decouple expiry with Guest inactivity days and annually on specific date(points do not expire)", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4224_decoupleExpiryWithGuestInactivityAndAnnuallyOnSpecificDate() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

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

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
//		pageObj.earningPage()
//				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		Utilities.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("InactiveGuests");
		Assert.assertEquals(flag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4221 Validated the decouple expiry with exact days and annually on specific date(points do not expire)", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4221_decoupleExpiryWithExactDaysAndAnnuallyOnSpecificDate() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, businessPreferenceLiveExpColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, businessPreferenceLiveExpColValue, "false", "went_live", b_id);

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

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
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for account table updation
		String query1 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		logger.info("accouts table query for updating created_at is successfully.");
		TestListeners.extentTest.get().info("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("UnredeemedPointsExpiry");
		Assert.assertEquals(flag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");

		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB queries for expiry_events
		Utilities.longWait(20000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID
				+ "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "expire_item_type",
				100);
		boolean flag6 = expColValue6.contains("MembershipExpiryOnSpecificDate");
		Assert.assertEquals(flag6, true,
				"MembershipExpiryOnSpecificDate is not present in expire_item_type column in expiry_events table");
		logger.info("MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");

		String query5 = "Select `value` from `expiry_events` where `user_id` = '" + userID
				+ "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValue(env, query5, "value");
		Assert.assertEquals(expColValue5, null,
				"MembershipExpiryOnSpecificDate is not present in value column in expiry_events table");
		logger.info("MembershipExpiryOnSpecificDate is present in value column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnSpecificDate is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4212 Validated the decouple expiry with annually on signup anniversary(points do not expire)", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4212_decoupleExpiryWithSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

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

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually On sign Up Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newscheduled_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query1 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(6000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "expire_item_type",
				100); // dbUtils.getValueFromColumn(query6, "expire_item_type");
		boolean flag = expColValue.contains("AnnualExpiry");
		Assert.assertEquals(flag, true,
				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("AnnualExpiry is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 100); // dbUtils.getValueFromColumn(query7, "membership_level_id");
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelId"));
		Assert.assertEquals(flag7, true,
				"AnnualExpiry is not present in membership_level_id column in expiry_events table");
		logger.info("AnnualExpiry is present in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("AnnualExpiry is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value"); // dbUtils.getValueFromColumn(query8,
		// "value");
		Assert.assertEquals(expColValue8, null, "AnnualExpiry is not present in value column in expiry_events table");
		logger.info("AnnualExpiry is present in value column in expiry_events table");
		TestListeners.extentTest.get().pass("AnnualExpiry is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4213 Validated the decouple expiry with annually on membership bump anniversary(points do not expire) || "
			+ "SQ-T4222 Validated the decouple expiry with Guest inactivity days and annually on membership bump anniversary(points do not expire)", priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T4213_decoupleExpiryWithAnnuallyOnMembershipBumpAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

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

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newscheduled_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs3 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "expire_item_type",
				150); // dbUtils.getValueFromColumn(query6, "expire_item_type");
		boolean flag = expColValue.contains("MembershipExpiryOnlyOnBumpAnniversary");
		Assert.assertEquals(flag, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in expire_item_type column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 100); // dbUtils.getValueFromColumn(query7, "membership_level_id");
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelId"));
		Assert.assertEquals(flag7, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in membership_level_id column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value"); // dbUtils.getValueFromColumn(query8,
		// "value");
		Assert.assertEquals(expColValue8, null,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in value column in expiry_events table");
		logger.info("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4223 Validated the decouple expiry with Guest inactivity days and annually on signup anniversary(points do not expire) || "
			+ "SQ-T4220 Validated the decouple expiry with exact days and annually on signup anniversary(points do not expire)", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T4223_decoupleExpiryWithGuestInactivityDaysAndSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

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

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually On sign Up Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newscheduled_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query1 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "expire_item_type",
				150); // dbUtils.getValueFromColumn(query6, "expire_item_type");
		boolean flag = expColValue.contains("InactiveGuests");
		Assert.assertEquals(flag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value"); // dbUtils.getValueFromColumn(query8,
		// "value");
		Assert.assertEquals(expColValue8, dataSet.get("value"),
				"AnnualExpiry is not present in value column in expiry_events table");
		logger.info("AnnualExpiry is present in value column in expiry_events table");
		TestListeners.extentTest.get().pass("AnnualExpiry is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
