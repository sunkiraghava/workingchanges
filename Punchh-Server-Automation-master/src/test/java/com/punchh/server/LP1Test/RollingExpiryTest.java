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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RollingExpiryTest {
	static Logger logger = LogManager.getLogger(RollingExpiryTest.class);
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
//	private Connection conn;
//	private Statement stmt;

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

//	@Test(description = "SQ-T2266 Verify rolling expiry (expires after) in point to currency business")
	public void T2266_rollingExpiry() throws Exception {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Gift points to the user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		String newcreated_at = CreateDateTime.getYesterdayDateTime();
		String newupdated_at = CreateDateTime.getYesterdayDateTime();
		String newscheduled_at = CreateDateTime.getYesterdayDateTime();

		String query = "select created_at, updated_at, scheduled_expiry_on from checkins where user_id ='" + userID
				+ "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		TestListeners.extentTest.get().info("User search successful");

		String query1 = "UPDATE checkins SET `created_at` = '" + newcreated_at + "', `updated_at` = '" + newupdated_at
				+ "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE user_id = '" + userID + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		TestListeners.extentTest.get().info("User information updated successfully.");

//		pageObj.instanceDashboardPage().openSchedule();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4205 Validate the rolling expiry with exact days || "
			+ "SQ-T5797 Verify all the sidekiq workers for Rolling Expiry", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4205_rollingExpiryWithExactDays() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String b_id = dataSet.get("business_id");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLiveFlag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_live", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
//		pageObj.earningPage().setInactiveDays("set", 180);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// cockpit -> earning -> Transfered Loyalty Expiry -> set Transferred Points
		// Expiry Days
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Transfered Loyalty Expiry");
		String expectedSuccessMessage = dataSet.get("expectedSuccessMessage");
		pageObj.earningPage().setTransferredPointsExpiryDaysAsFixedDays(1);
		pageObj.earningPage().verifyMessage(expectedSuccessMessage);

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(2000);

		// verify jobs in sidekiq

		int count2 = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "AccountExpireWorker", 10);
		Assert.assertTrue(count2 > 0, "AccountExpireWorker is not called in sidekiq");
		utils.logit("AccountExpireWorker is called in sidekiq");

		int count = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "BusinessRollingCheckinsWorker", 10);
		Assert.assertTrue(count > 0, "BusinessRollingCheckins is not called in sidekiq");
		utils.logit("BusinessRollingCheckins is called in sidekiq");

		int count3 = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "BulkAccountRefresher", 7);
		Assert.assertTrue(count3 > 0, "BulkAccountRefresher is not called in sidekiq");
		utils.logit("BulkAccountRefresher is called in sidekiq");

		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("UnredeemedPointExpiryWorker");
		Assert.assertEquals(flag, true,
				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
		utils.logPass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2,
				"membership_level_id"); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, null,
				"membership_level_id is not null in membership_level_id column in expiry_events table");
		utils.logPass("membership_level_id is null in membership_level_id column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4207 Validated the expiry with guest inactivity")
	@Owner(name = "Hardik Bhardwaj")
	public void T4207_rollingExpiryWithGuestInactivity() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("InactiveGuests");
		Assert.assertEquals(flag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		utils.logPass("InactiveGuests is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4209 Validated the expiry with annually on guest signup anniversary")
	@Owner(name = "Hardik Bhardwaj")
	public void T4209_rollingExpiryWithannuallyOnGuestSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

//		// DB query for account table updation
		String query1 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		utils.logit("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("AnnualExpiry");
		Assert.assertEquals(flag, true,
				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
		utils.logPass("AnnualExpiry is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4208 Validated the expiry with annually on a specific date", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4208_rollingExpiryWithSpecificDate() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String preferenceExpColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, preferenceExpColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, preferenceExpColValue, "false", "went_live", b_id);

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on a Specific Date");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

//		// DB connection open
//		conn = dbUtils.createMySqlDatabaseConnection(prop.getProperty("pp.host"), prop.getProperty("pp.port"),
//				utils.decrypt(prop.getProperty("pp.username")), utils.decrypt(prop.getProperty("pp.password")));
//		stmt = conn.createStatement();

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(7000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("SpecificDateExpiry");
		Assert.assertEquals(flag, true,
				"SpecificDateExpiry is not present in expire_item_type column in expiry_events table");
		utils.logPass("SpecificDateExpiry is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4210 Validated the expiry with annually on membership level bump anniversary")
	@Owner(name = "Hardik Bhardwaj")
	public void T4210_rollingExpiryWithMembershipLevelBump() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		utils.logit("Accounts table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("Rolling");
		Assert.assertEquals(flag, true, "Rolling is not present in expire_item_type column in expiry_events table");
		utils.logPass("Rolling is present in expire_item_type column in expiry_events table");

//		DBUtils.closeConnection();
	}

	// covering this in
	// T4223_decoupleExpiryWithGuestInactivityDaysAndSignupAnniversary
	@SuppressWarnings("static-access")
//	@Test(description = "SQ-T4220 Validated the decouple expiry with exact days and annually on signup anniversary(points do not expire)")
	public void T4220_decoupleExpiryWithExactDaysAndAnnuallyOnSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually On sign Up Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

//				// DB query for account table updation
		String query1 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		utils.logit("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB queries for expiry_events
		utils.longWait(8000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag = expColValue.contains("UnredeemedPointsExpiry") || expColValue.contains("AnnualExpiry");
		Assert.assertEquals(flag, true,
				expColValue + " is not present in expire_item_type column in expiry_events table");
		utils.logPass(expColValue + " is present in expire_item_type column in expiry_events table");

		String query6 = "Select `expire_item_type` from `expiry_events` where `membership_level_id` = '"
				+ membershipLevelId + "' AND `user_id` = '" + userID + "';";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean flag6 = expColValue6.contains("AnnualExpiry");
		Assert.assertEquals(flag6, true,
				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
		utils.logPass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String query4 = "Select `membership_level_id` from `expiry_events` where `expire_item_type` = 'AnnualExpiry' AND `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"membership_level_id", 100); // dbUtils.getValueFromColumn(query4, "membership_level_id");
		boolean flag4 = expColValue4.contains(dataSet.get("membershipLevelId"));
		Assert.assertEquals(flag4, true,
				"Membership Level Id is not present in membership_level_id column in expiry_events table");
		utils.logPass("Membership Level Id is present in membership_level_id column in expiry_events table");

		String query5 = "Select `value` from `expiry_events` where `expire_item_type` = 'AnnualExpiry' AND `user_id` = '"
				+ userID + "';";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValue(env, query5, "value"); // dbUtils.getValueFromColumn(query5,
		// "value");
		Assert.assertEquals(expColValue5, null,
				"UnredeemedPointsExpiry is not present in value column in expiry_events table");
		utils.logPass("UnredeemedPointsExpiry is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4219 Validated the decouple expiry with exact days and annually on membership bump anniversary(points do not expire)", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4219_decoupleExpiryWithExactDaysAndAnnuallyOnMembershipBumpAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLiveFlag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "', `joined_at` = '" + newcreated_at
				+ "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID
				+ "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		utils.logit("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		utils.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"expire_item_type", 125); /// dbUtils.getValueFromColumn(query6, "expire_item_type");
		boolean flag = expColValue.contains("MembershipExpiryOnlyOnBumpAnniversary");
		Assert.assertEquals(flag, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in expire_item_type column in expiry_events table");
		utils.logPass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 100); // dbUtils.getValueFromColumn(query7, "membership_level_id");
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelId"));
		Assert.assertEquals(flag7, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in membership_level_id column in expiry_events table");
		utils.logPass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value"); // dbUtils.getValueFromColumn(query8,
		// "value");
		Assert.assertEquals(expColValue8, null, "value is not present in value column in expiry_events table");
		utils.logPass("value is present in value column in expiry_events table");

//		DBUtils.closeConnection();
	}

	// covered in T4213_decoupleExpiryWithAnnuallyOnMembershipBumpAnniversary
	@SuppressWarnings("static-access")
//	@Test(description = "SQ-T4222 Validated the decouple expiry with Guest inactivity days and annually on membership bump anniversary(points do not expire)")
	public void T4222_decoupleExpiryWithGuestInactivityDaysAndAnnuallyOnMembershipBumpAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newscheduled_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logit("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		utils.logit("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		utils.logit("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		utils.logit("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		utils.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query6, "expire_item_type");
		boolean flag = (expColValue.contains("MembershipExpiryOnlyOnBumpAnniversary")
				|| expColValue.contains("InactiveGuests"));
		Assert.assertEquals(flag, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in expire_item_type column in expiry_events table");
		utils.logPass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
