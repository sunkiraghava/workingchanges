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
public class RollingCheckinSumExpiryTest {
	static Logger logger = LogManager.getLogger(RollingCheckinSumExpiryTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	String BlankSpace = "";

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

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7014 AccountSummary_Update_Unredeemed_TrackSpentTrue_MembershipOn")
	@Owner(name = "Hardik Bhardwaj")
	public void T7014_rollingExpiryWithExactDays() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

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
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
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

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 1);

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(1);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

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

		utils.longWaitInSeconds(4);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
//				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
//		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
//		Assert.assertEquals(expireItemTypeflag, true,
//				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
//		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150); // dbUtils.getValueFromColumn(query3,
																														// "expire_item_type");
		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

//		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
//		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "membership_level_id");
//		Assert.assertEquals(expColValue2, null,
//				"membership_level_id is not null in membership_level_id column in expiry_events table");
//		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_spent_points is 0 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_spent_points is 0 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String nonTransactionUnexpiredCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredCountQuery, "non_transaction_unexpired_count", 5);
		Assert.assertEquals(nonTransactionUnexpiredCountValue, "0",
				"non_transaction_unexpired_count column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");

		// Check non_transaction_unexpired_points in account_summaries table
		String nonTransactionUnexpiredPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredPointsQuery, "non_transaction_unexpired_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredPointsValue, "0",
				"non_transaction_unexpired_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7015 AccountSummary_Update_Unredeemed_TrackSpentFalse_MembershipOff")
	@Owner(name = "Hardik Bhardwaj")
	public void T7015_rollingExpiryWithExactDays() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

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
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
//		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
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

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 1);

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(1);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);

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

		utils.longWaitInSeconds(4);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
//				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
//		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
//		Assert.assertEquals(expireItemTypeflag, true,
//				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
//		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150); // dbUtils.getValueFromColumn(query3,
																														// "expire_item_type");
		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

//		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
//		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "membership_level_id");
//		Assert.assertEquals(expColValue2, null,
//				"membership_level_id is not null in membership_level_id column in expiry_events table");
//		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("membership_level_id is null in membership_level_id column in expiry_events table");

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_spent_points is 0 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_spent_points is 0 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String nonTransactionUnexpiredCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredCountQuery, "non_transaction_unexpired_count", 5);
		Assert.assertEquals(nonTransactionUnexpiredCountValue, "0",
				"non_transaction_unexpired_count column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");

		// Check non_transaction_unexpired_points in account_summaries table
		String nonTransactionUnexpiredPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredPointsQuery, "non_transaction_unexpired_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredPointsValue, "0",
				"non_transaction_unexpired_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7016 AccountSummary_Update_GuestInactivity_TrackSpentTrue_MembershipOn")
	@Owner(name = "Hardik Bhardwaj")
	public void T7016_rollingExpiryWithGuestInactivity() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

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

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 1);

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(4);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
//				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
//		boolean expireItemTypeflag = expireItemTypeValue.contains("InactiveGuests");
//		Assert.assertEquals(expireItemTypeflag, true,
//				"InactiveGuests is not present in expire_item_type column in expiry_events table");
//		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");
//
//		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
//		// DB query for membership_level_histories
//		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
//				+ userID + "' order by id desc LIMIT 1 ;";
//		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
//				"membership_level_id");
//		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
//				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
//		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
//		TestListeners.extentTest.get()
//				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150);

		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lastCheckinTimeQuery,
				"last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7017 AccountSummary_Update_GuestInactivity_TrackSpentFalse_MembershipOff")
	@Owner(name = "Hardik Bhardwaj")
	public void T7017_rollingExpiryWithGuestInactivity() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

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
//		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
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

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 1);

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(4);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
//				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
//		boolean expireItemTypeflag = expireItemTypeValue.contains("InactiveGuests");
//		Assert.assertEquals(expireItemTypeflag, true,
//				"InactiveGuests is not present in expire_item_type column in expiry_events table");
//		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("InactiveGuests is present in expire_item_type column in expiry_events table");
//
//		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
//		// DB query for membership_level_histories
//		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
//				+ userID + "' order by id desc LIMIT 1 ;";
//		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
//				"membership_level_id");
//		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
//				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
//		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
//		TestListeners.extentTest.get()
//				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150);

		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lastCheckinTimeQuery,
				"last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7018 AccountSummary_Update_SignupAnniversary_TrackSpentTrue_MembershipOn", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T7018_signupAnniversaryMembershipEvaluation() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
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
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

//		// DB query for account table updation
		String query1 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		logger.info("accouts table query for updating created_at is successfully.");
		TestListeners.extentTest.get().info("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type", 150); // dbUtils.getValueFromColumn(query3,
//																														// "expire_item_type");
//		boolean flag = expColValue11.contains("AnnualExpiry");
//		Assert.assertEquals(flag, true,
//				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
//		logger.info("AnnualExpiry is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150);

		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lastCheckinTimeQuery,
				"last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7019 AccountSummary_Update_SignupAnniversary_TrackSpentFalse_MembershipOff", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T7019_signupAnniversaryMembershipEvaluation() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
//		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);

		// DB query for user updation

		String query = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

//		// DB query for account table updation
		String query1 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		logger.info("accouts table query for updating created_at is successfully.");
		TestListeners.extentTest.get().info("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(4000);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type", 150); // dbUtils.getValueFromColumn(query3,
//																														// "expire_item_type");
//		boolean flag = expColValue11.contains("AnnualExpiry");
//		Assert.assertEquals(flag, true,
//				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
//		logger.info("AnnualExpiry is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 150);

		Assert.assertNotNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String zero = "0";
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, zero,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ zero);
		logger.info("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lastCheckinTimeQuery,
				"last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		TestListeners.extentTest.get()
				.pass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T7021 NoExpiry_NoUpdate")
	@Owner(name = "Hardik Bhardwaj")
	public void T7021_NoExpiryNoUpdate() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");
		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag"), b_id);

		// updating the business preference for enable_account_summary
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);

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

//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
//		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
//		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
//		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
//		pageObj.earningPage().updateConfiguration();
//		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
//		pageObj.earningPage().setExpiresAfter("set", 1);
//		pageObj.earningPage().expiryDateStrategy("Exact Days");
//		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
//		pageObj.earningPage().updateConfiguration();
//		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");

		String amount = String.valueOf(Integer.parseInt(dataSet.get("amount")) * 1);

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(4);
//		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
//		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
//				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
//		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
//		Assert.assertEquals(expireItemTypeflag, true,
//				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
//		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query0 = "SELECT `expired_at` FROM `checkins` WHERE `user_id` = '" + userID + "';";
		String expireItemTypeValue0 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query0, "expired_at", 5); // dbUtils.getValueFromColumn(query3,
																														// "expire_item_type");
		Assert.assertNull(expireItemTypeValue0, "expired_at is null in checkins table");
		logger.info("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);
		TestListeners.extentTest.get()
				.pass("expired_at value is present in checkins table i.e. " + expireItemTypeValue0);

//		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
//		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "membership_level_id");
//		Assert.assertEquals(expColValue2, null,
//				"membership_level_id is not null in membership_level_id column in expiry_events table");
//		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
//		TestListeners.extentTest.get()
//				.pass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		TestListeners.extentTest.get()
				.pass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");
		TestListeners.extentTest.get().info(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, "100",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 100");
		logger.info(
				"verified that non_transaction_unexpired_spent_points is 100 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_spent_points is 100 and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
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
		String nonTransactionUnexpiredCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredCountQuery, "non_transaction_unexpired_count", 5);
		Assert.assertEquals(nonTransactionUnexpiredCountValue, "1",
				"non_transaction_unexpired_count column in account_summaries table is not equal to expected value i.e. 1");
		logger.info(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 1 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 1 after rolling expiry");

		// Check non_transaction_unexpired_points in account_summaries table
		String nonTransactionUnexpiredPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredPointsQuery, "non_transaction_unexpired_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredPointsValue, "100",
				"non_transaction_unexpired_points column in account_summaries table is not equal to expected value i.e. 100");
		logger.info(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 100 after rolling expiry");
		TestListeners.extentTest.get().pass(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 100 after rolling expiry");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
