package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
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
public class DecoupleRollingExpiryRollingCheckinSumPTRTest {
	static Logger logger = LogManager.getLogger(DecoupleRollingExpiryRollingCheckinSumPTRTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	int rs;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		rs = 0;

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6368 Verify membership reset without point expiry using Annually on Specific Date in decoupled strategy in PTR || "
			+ "SQ-T7020 AccountSummary_Update_PartialAndFull_Unredeemed")
	@Owner(name = "Hardik Bhardwaj")
	public void T6368_decoupleExpiryWithGuestInactivityAndAnnuallyOnSpecificDate() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		List<String> dateToDetermineMembershipLevel = Utilities.getPreferencesKeyValue(expColValue,
				"date_to_determine_membership_level");
		String dateToDetermineMembershipLevelVal = dateToDetermineMembershipLevel.get(0).replace("[", "")
				.replace("]", "").replace("'", "");

		if (!(dateToDetermineMembershipLevelVal.equalsIgnoreCase(date))) {
			String dateToDetermineMembershipLevelquery = "UPDATE businesses SET preferences = REPLACE(preferences, "
					+ "':date_to_determine_membership_level: ''" + dateToDetermineMembershipLevelVal + "''', "
					+ "':date_to_determine_membership_level: ''" + date + "''') "
					+ "WHERE preferences LIKE '%:date_to_determine_membership_level: ''"
					+ dateToDetermineMembershipLevelVal + "''%' " + "AND id = " + b_id;

			rs = DBUtils.executeUpdateQuery(env, dateToDetermineMembershipLevelquery);
			Assert.assertEquals(rs, 1);
			logger.info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
			TestListeners.extentTest.get().info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
		}
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

		String currentDate = CreateDateTime.getCurrentDateOnly();
		String currentMonth = CreateDateTime.getCurrentMonthOnly();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
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
		pageObj.earningPage().setExpiryDay("set", currentDate);
		pageObj.earningPage().setExpiryMonth(currentMonth);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// POS checkin of 210 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
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
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		utils.longWaitInSeconds(10);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				query6, "expire_item_type", 150);
		boolean expireItemTypeFlag = expireItemTypeColValue.contains("MembershipExpiryOnSpecificDate");
		Assert.assertEquals(expireItemTypeFlag, true,
				"MembershipExpiryOnSpecificDate is not present in expire_item_type column in expiry_events table");
		logger.info(
				"MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnSpecificDate is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 100);
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelIdGold"));
		Assert.assertEquals(flag7, true,
				"MembershipExpiryOnSpecificDate is not present in membership_level_id column in expiry_events table");
		logger.info(
				"MembershipExpiryOnSpecificDate is present in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnSpecificDate is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value");
		Assert.assertEquals(expColValue8, null,
				"MembershipExpiryOnSpecificDate is not present in value column in expiry_events table");
		logger.info("MembershipExpiryOnSpecificDate is present in value column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnSpecificDate is present in value column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6367 Verify membership reset without point expiry using Annually on Membership Bump Anniversary in decoupled strategy in PTR")
	@Owner(name = "Hardik Bhardwaj")
	public void T6367_decoupleExpiryWithAnnuallyOnMembershipBumpAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		List<String> dateToDetermineMembershipLevel = Utilities.getPreferencesKeyValue(expColValue,
				"date_to_determine_membership_level");
		String dateToDetermineMembershipLevelVal = dateToDetermineMembershipLevel.get(0).replace("[", "")
				.replace("]", "").replace("'", "");

		if (!(dateToDetermineMembershipLevelVal.equalsIgnoreCase(date))) {
			String dateToDetermineMembershipLevelquery = "UPDATE businesses SET preferences = REPLACE(preferences, "
					+ "':date_to_determine_membership_level: ''" + dateToDetermineMembershipLevelVal + "''', "
					+ "':date_to_determine_membership_level: ''" + date + "''') "
					+ "WHERE preferences LIKE '%:date_to_determine_membership_level: ''"
					+ dateToDetermineMembershipLevelVal + "''%' " + "AND id = " + b_id;

			rs = DBUtils.executeUpdateQuery(env, dateToDetermineMembershipLevelquery);
			Assert.assertEquals(rs, 1);
			logger.info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
			TestListeners.extentTest.get().info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
		}
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
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// POS checkin of 210 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
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
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelIdGold = dataSet.get("membershipLevelIdGold");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelIdGold + "' ;";
		int rs5 = DBUtils.executeUpdateQuery(env, query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().info("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		utils.longWaitInSeconds(10);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				query6, "expire_item_type", 150);
		boolean expireItemTypeFlag = expireItemTypeColValue.contains("MembershipExpiryOnlyOnBumpAnniversary");
		Assert.assertEquals(expireItemTypeFlag, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in expire_item_type column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 100);
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelIdGold"));
		Assert.assertEquals(flag7, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in membership_level_id column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get().pass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value");
		Assert.assertEquals(expColValue8, null,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in value column in expiry_events table");
		logger.info("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6366 Verify membership reset without point expiry using Annually on Sign-Up Anniversary in decoupled strategy in PTR")
	@Owner(name = "Hardik Bhardwaj")
	public void T6366_decoupleExpiryWithGuestInactivityDaysAndSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		List<String> dateToDetermineMembershipLevel = Utilities.getPreferencesKeyValue(expColValue,
				"date_to_determine_membership_level");
		String dateToDetermineMembershipLevelVal = dateToDetermineMembershipLevel.get(0).replace("[", "")
				.replace("]", "").replace("'", "");

		if (!(dateToDetermineMembershipLevelVal.equalsIgnoreCase(date))) {
			String dateToDetermineMembershipLevelquery = "UPDATE businesses SET preferences = REPLACE(preferences, "
					+ "':date_to_determine_membership_level: ''" + dateToDetermineMembershipLevelVal + "''', "
					+ "':date_to_determine_membership_level: ''" + date + "''') "
					+ "WHERE preferences LIKE '%:date_to_determine_membership_level: ''"
					+ dateToDetermineMembershipLevelVal + "''%' " + "AND id = " + b_id;

			rs = DBUtils.executeUpdateQuery(env, dateToDetermineMembershipLevelquery);
			Assert.assertEquals(rs, 1);
			logger.info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
			TestListeners.extentTest.get().info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
		}

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

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
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
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for accounts updation

		String query1 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().info("Accounts table updated successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
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
		String expireItemTypeQuery = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID
				+ "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				expireItemTypeQuery, "expire_item_type", 150); // dbUtils.getValueFromColumn(query6,
																// "expire_item_type");
		boolean expireItemTypeFlag = expireItemTypeValue.contains("AnnualExpiry");
		Assert.assertEquals(expireItemTypeFlag, true,
				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("AnnualExpiry is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value"); // dbUtils.getValueFromColumn(query8,
		// "value");
		Assert.assertEquals(expColValue8, dataSet.get("value"),
				"AnnualExpiry is not present in value column in expiry_events table");
		logger.info("AnnualExpiry is present in value column in expiry_events table");
		TestListeners.extentTest.get().pass("AnnualExpiry is present in value column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

	}



	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6361 Validate that guest inactivity expiry and check all applicable field adjustments for PTR business || "
			+ "SQ-T6357 Validate that last_visit is set to NULL when all loyalty check-ins are expired for PTR business || "
			+ "SQ-T6359 Validate For classic coupled strategy, that membership_evaluation_points is reduced by expired non-checkin earnings for PTR business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6361_rollingExpiryWithGuestInactivity() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(181); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		DBUtils.updatePreference(env, expColValue, date, b_id,
				"date_to_determine_membership_level","businesses");

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
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 180);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(181);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("InactiveGuests");
		Assert.assertEquals(expireItemTypeflag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

		// DB query for accounts and account_summaries table
		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		String lastCheckinTimeQuery = "Select `last_checkin_time` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// Check non_transaction_total_points in account_summaries table
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				lastCheckinTimeQuery, "last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6362 Validate that If no check-ins qualify for expiry, summary should remain unchanged for PTR business. || "
			+ "SQ-T6360 Validate In unredeemed_point_expiry, membership_evaluation_points should not change PTR business || "
			+ "SQ-T6355 Validate that loyalty_points and loyalty_count are reduced after loyalty check-ins expire for PTR business || "
			+ "SQ-T6356 Validate that without_transaction_checkins_points and without_transaction_checkins_count update on expiry for PTR business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6362_rollingExpiryWithExactDays() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		DBUtils.updatePreference(env, expColValue, date, b_id, "date_to_determine_membership_level", "businesses");

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
		pageObj.earningPage().setExpiresAfter("set", 180);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(180);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
		Assert.assertEquals(expireItemTypeflag, true,
				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2,
				"membership_level_id");
		Assert.assertEquals(expColValue2, null,
				"membership_level_id is not null in membership_level_id column in expiry_events table");
		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdGold = dataSet.get("membershipLevelIdGold");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdGold,
				"Value is not " + membershipLevelIdGold + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdGold + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdGold + " for the membership level expiry entry.");

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 3);
		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, amount,
				"Value is not " + amount + " for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is " + amount
				+ " for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass("Verified that Value is " + amount
				+ " for the membership_evaluation_points column in account_summaries table.");

		// DB query for accounts and account_summaries table
		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		String nonTransactionUnexpiredCountQuery = "Select `non_transaction_unexpired_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionUnexpiredPointsQuery = "Select `non_transaction_unexpired_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		logger.info("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// Check non_transaction_unexpired_count in account_summaries table
		String nonTransactionUnexpiredCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredCountQuery,
						"non_transaction_unexpired_count", 5);
		Assert.assertEquals(nonTransactionUnexpiredCountValue, "0",
				"non_transaction_unexpired_count column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");

		// Check non_transaction_unexpired_points in account_summaries table
		String nonTransactionUnexpiredPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredPointsQuery,
						"non_transaction_unexpired_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredPointsValue, "0",
				"non_transaction_unexpired_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6358 Verify pending_points field should not get  reduced by amount from expired pending check-ins. for PTR business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6358_rollingExpiryWithExactDaysPendingCheckin() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(181); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		DBUtils.updatePreference(env, expColValue, date, b_id, "date_to_determine_membership_level", "businesses");

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
		pageObj.earningPage().setExpiresAfter("set", 180);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Pos api checkin
		String checkin_key = CreateDateTime.getTimeDateString();
		String checkin_txn = "123456" + CreateDateTime.getTimeDateString();
		String checkin_date = CreateDateTime.getFutureDate(2) + "T10:50:00+05:30";
		Response checkin_resp = pageObj.endpoints().posCheckin(checkin_date, userEmail, checkin_key, checkin_txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(200, checkin_resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(181);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
		Assert.assertEquals(expireItemTypeflag, true,
				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2,
				"membership_level_id");
		Assert.assertEquals(expColValue2, null,
				"membership_level_id is not null in membership_level_id column in expiry_events table");
		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "60",
				"Value is not 60 for the membership_evaluation_points column in account_summaries table");
		logger.info(
				"Verified that Value is 60 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 60 for the membership_evaluation_points column in account_summaries table.");

		// DB query for accounts and account_summaries table
		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		logger.info("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6351 Validate For classic coupled strategy, that membership_evaluation_points is reduced by expired non-checkin earnings. ")
	@Owner(name = "Hardik Bhardwaj")
	public void T6351_rollingExpiryWithGuestInactivity() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(181); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery,
				"preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag1") + " value is updated to true");

		// Fetching the value of date_to_determine_membership_level from
		// business.preference from DB
		DBUtils.updatePreference(env, expColValue, date, b_id,
				"date_to_determine_membership_level","businesses");

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
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 180);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");

		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().info("Send redeemable to the user successfully");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().info("Api2  send reward amount to user is successful");

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(181);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");
		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"expire_item_type", 100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("InactiveGuests");
		Assert.assertEquals(expireItemTypeflag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		TestListeners.extentTest.get()
				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipLevelIdQuery, "membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		TestListeners.extentTest.get().pass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

		// DB query for accounts and account_summaries table
		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		String lastCheckinTimeQuery = "Select `last_checkin_time` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 2);
		Assert.assertNull(pointsSpent);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 2);
		Assert.assertEquals(nonTransactionSpentPointsValue, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. 0");
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 2);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, zero,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), zero,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + zero);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		TestListeners.extentTest.get().pass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// Check non_transaction_total_points in account_summaries table
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				lastCheckinTimeQuery, "last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
