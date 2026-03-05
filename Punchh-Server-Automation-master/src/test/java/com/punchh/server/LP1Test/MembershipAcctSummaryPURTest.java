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
public class MembershipAcctSummaryPURTest {

	private static Logger logger = LogManager.getLogger(MembershipAcctSummaryPURTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env, baseUrl, businessesQuery, businessId, oneYearAgoDate, oneYearAgoYesterdayDate,
			todaysDate, yesterdayDate, twoDaysAgoDate, currentDay, currentMonth;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	boolean flag1, flag2, flag3, flag4;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6426: Verify Membership Evaluation for PUR with Unredeemed Expiry", priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6426_unredeemedExpiryMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150";
		String checkinAmount2 = "100";
		String checkinAmount3 = "50";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		yesterdayDate = CreateDateTime.getYesterdayDays(1);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry and Membership level configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150 * 3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + twoDaysAgoDate + "' , `updated_at` = '" + twoDaysAgoDate
				+ "', `joined_at` = '" + twoDaysAgoDate + "', `last_activity_at`= '" + twoDaysAgoDate + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + twoDaysAgoDate + "' WHERE `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + twoDaysAgoDate + "', `updated_at` = '"
				+ yesterdayDate + "', `scheduled_expiry_on` = '" + yesterdayDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + twoDaysAgoDate
				+ "', `updated_at` = '" + twoDaysAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);

		// second checkin of {100} points -> {100 * 3 = 300} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);
		// third checkin of {50} points -> {50 * 3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount3);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);

		// Verify total_credits = total converted redeemable points
		// 900 = 450 + 300 + 150
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "900.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query9 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9,
				"total_debits", 10);
		Assert.assertEquals(expColValue9, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify remaining redeemable points in Mobile User Balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		String remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points")
				.toString();
		Assert.assertEquals(remainingRedeemablePoints, "880.0",
				"remaining redeemable points in user balance is not matching");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("UnredeemedPointsExpiry");
		Assert.assertTrue(expireItemTypeflag,
				"UnredeemedPointsExpiry is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify that first checkin is expired
		String query6 = "SELECT expired_at FROM checkins WHERE id = '" + checkinID + "';";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"expired_at", 5);
		Assert.assertEquals(expColValue6, yesterdayDate, "expired_at is not matching");
		// Verify total_credits
		// = total converted redeemable points-(expired points-redeemed points)
		// 470 = 900 - (450 - 20)
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "470.0", "total_credits after expiry is not matching");
		// Verify membership_evaluation_points = total converted redeemable points
		String query7 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue7, "900", "membership_evaluation_points is not matching");
		// Verify non_transaction_total_points = total converted redeemable points
		String query8 = "SELECT non_transaction_total_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "900", "non_transaction_total_points is not matching");
		// Verify effective membership ID for {Gold}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify remaining redeemable points in Mobile User Balance API
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "450.0",
				"remaining redeemable points in user balance is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6433: Verify Membership Evaluation for PUR with Annually on signup anniversary", priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6433_signupAnniversaryMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150 * 3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");

		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + oneYearAgoDate + "', `updated_at` = '" + oneYearAgoDate
				+ "', `joined_at` = '" + oneYearAgoDate + "', `last_activity_at`= '" + oneYearAgoDate + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + oneYearAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("AnnualExpiry");
		Assert.assertTrue(expireItemTypeflag,
				"AnnualExpiry is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {0}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "0", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");

		// second checkin of {50} points -> {50 * 3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "150.0", "total_credits is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "150", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6427: Verify Membership Evaluation for PUR with Guest Inactivity", priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6427_guestInactivityMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150";
		String checkinAmount2 = "60";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoYesterdayDate = CreateDateTime.getNyearAgoYesterdayDate(1);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150 * 3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");

		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoYesterdayDate + "' , `updated_at` = '"
				+ oneYearAgoYesterdayDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");
		// changing the dates in accounts table
		String query1 = "Update `accounts` Set `created_at` = '" + oneYearAgoYesterdayDate + "' where `user_id` = '"
				+ userID + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + oneYearAgoYesterdayDate + "' , `updated_at` = '"
				+ oneYearAgoYesterdayDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoYesterdayDate
				+ "', `updated_at` = '" + oneYearAgoYesterdayDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + oneYearAgoYesterdayDate + "' , `updated_at` = '"
				+ oneYearAgoYesterdayDate + "', `joined_at` = '" + oneYearAgoYesterdayDate + "', `last_activity_at`= '"
				+ oneYearAgoYesterdayDate + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that first checkin is expired
		String query8 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue8.contains("InactiveGuests");
		Assert.assertTrue(expireItemTypeflag,
				"InactiveGuests is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 60);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify non_transaction_unexpired_points as {0}
		String query7 = "SELECT non_transaction_unexpired_points FROM account_summaries WHERE user_id = " + userID
				+ ";";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"non_transaction_unexpired_points", 5);
		Assert.assertEquals(expColValue7, "0", "non_transaction_unexpired_points is not matching");

		// second checkin of {60} points -> {60 * 3 = 180} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify total_credits as {180}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 60);
		Assert.assertEquals(expColValue4, "180.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {180}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "180", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify non_transaction_unexpired_points as {180}
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"non_transaction_unexpired_points", 5);
		Assert.assertEquals(expColValue7, "180", "non_transaction_unexpired_points is not matching");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6434: Verify Membership Evaluation for PUR with Annually on membership-bump anniversary", priority = 3)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6434_bumpAnniversaryMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		todaysDate = CreateDateTime.getCurrentDate();
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150 * 3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");

		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");
		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + oneYearAgoDate + "', `updated_at` = '" + oneYearAgoDate
				+ "', `joined_at` = '" + oneYearAgoDate + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + oneYearAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		String membershipLevelId = dataSet.get("membership3Id");
		String query8 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + todaysDate
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		DBUtils.executeUpdateQuery(env, query8);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("Rolling");
		Assert.assertTrue(expireItemTypeflag,
				"Rolling is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {0}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "0", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");

		// second checkin of {50} points -> {50 * 3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "150.0", "total_credits is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "150", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6435: Verify Membership Evaluation for PUR with Annually on specific date", priority = 4)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6435_specificDateMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		currentDay = CreateDateTime.getCurrentDateOnly();
		currentMonth = CreateDateTime.getCurrentMonthOnly();
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on a Specific Date");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiryDay("set", currentDay);
		pageObj.earningPage().setExpiryMonth(currentMonth);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150 * 3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {430}
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		String remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points")
				.toString();
		Assert.assertEquals(remainingRedeemablePoints, "430.0",
				"remaining redeemable points in user balance is not matching");

		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + twoDaysAgoDate + "' , `updated_at` = '"
				+ twoDaysAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");
		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + twoDaysAgoDate + "', `updated_at` = '" + twoDaysAgoDate
				+ "', `joined_at` = '" + twoDaysAgoDate + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + twoDaysAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + twoDaysAgoDate + "' , `updated_at` = '"
				+ twoDaysAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + twoDaysAgoDate
				+ "', `updated_at` = '" + twoDaysAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("SpecificDateExpiry");
		Assert.assertTrue(expireItemTypeflag,
				"SpecificDateExpiry is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {0}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "0", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {0}
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "0.0",
				"remaining redeemable points in user balance is not matching");

		// second checkin of {50} points -> {50 * 3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "150.0", "total_credits is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {150}
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "150.0",
				"remaining redeemable points in user balance is not matching");
		// Verify total_debits as {0}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "0.00", "total_debits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "150", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {0}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "0", "non_transaction_unexpired_spent_points is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6436: Verify Membership Evaluation for PUR with decouple Annually on signup anniversary", priority = 5)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6436_decoupleSignupAnniversaryMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually On sign Up Anniversary (Points do not expire)");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150*3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");

		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + oneYearAgoDate + "', `updated_at` = '" + oneYearAgoDate
				+ "', `joined_at` = '" + oneYearAgoDate + "', `last_activity_at`= '" + oneYearAgoDate + "' WHERE id = '"
				+ userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + oneYearAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("AnnualExpiry");
		Assert.assertTrue(expireItemTypeflag,
				"AnnualExpiry is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 100);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {450}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {450}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "450", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {450}
		String query8 = "SELECT non_transaction_total_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "450", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {430=450-20}
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		String remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points")
				.toString();
		Assert.assertEquals(remainingRedeemablePoints, "430.0",
				"remaining redeemable points in user balance is not matching");

		// second checkin of {50} points -> {50*3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {600=450+150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "600.0", "total_credits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {600=450+150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "600", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {600}
		expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "600", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as
		// {580=430+150}
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "580.0",
				"remaining redeemable points in user balance is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6437: Verify Membership Evaluation for PUR with decouple Annually on membership-bump anniversary", priority = 6)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6437_decoupleBumpAnniversaryMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		todaysDate = CreateDateTime.getCurrentDate();
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150*3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");

		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");
		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + oneYearAgoDate + "', `updated_at` = '" + oneYearAgoDate
				+ "', `joined_at` = '" + oneYearAgoDate + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + oneYearAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		String membershipLevelId = dataSet.get("membership3Id");
		String query7 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + todaysDate
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		DBUtils.executeUpdateQuery(env, query7);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `business_id` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"business_id", 60);
		Assert.assertEquals(expColValue16, dataSet.get("business_id"),
				"Expiry event is not present for user_id " + userID + " on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {450}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {450}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "450", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {450}
		String query8 = "SELECT non_transaction_total_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "450", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {430=450-20}
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		String remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points")
				.toString();
		Assert.assertEquals(remainingRedeemablePoints, "430.0",
				"remaining redeemable points in user balance is not matching");

		// second checkin of {50} points -> {50*3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {600=450+150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "600.0", "total_credits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {600=450+150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "600", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {600}
		expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "600", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as
		// {580=430+150}
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "580.0",
				"remaining redeemable points in user balance is not matching");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6438: Verify Membership Evaluation for PUR with decouple Annually on specific date", priority = 7)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6438_decoupleSpecificDateMembershipEvaluationPUR() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150.0";
		String checkinAmount2 = "50.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		currentDay = CreateDateTime.getCurrentDateOnly();
		currentMonth = CreateDateTime.getCurrentMonthOnly();
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		updateAcctSummaryValidationDBFlagsPUR(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().setBaseConversionRate("3.0");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually on a specific date (points don’t expire)");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiryDay("set", currentDay);
		pageObj.earningPage().setExpiryMonth(currentMonth);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
		updateMembershipLevelsDB();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points -> {150*3 = 450} redeemable points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {20} points
		Response forceRedeemResponse = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "20");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201);

		// Verify total_credits = total converted redeemable points
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		String query10 = "SELECT total_debits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify points_spent as {20}
		String query11 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"pointsSpentAmount", 5);
		Assert.assertEquals(expColValue11, "20", "points_spent is not matching");

		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + twoDaysAgoDate + "' , `updated_at` = '"
				+ twoDaysAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");
		// changing the dates in users table
		String query = "UPDATE `users` SET `created_at` = '" + twoDaysAgoDate + "', `updated_at` = '" + twoDaysAgoDate
				+ "', `joined_at` = '" + twoDaysAgoDate + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		// changing the dates in accounts table
		String query1 = "UPDATE `accounts` SET `created_at` = '" + twoDaysAgoDate + "' where `user_id` = '" + userID
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "accounts table query for updating created_at is not working");
		// changing the dates in checkins table
		String query2 = "UPDATE `checkins` SET `created_at` = '" + twoDaysAgoDate + "' , `updated_at` = '"
				+ twoDaysAgoDate + "' WHERE id = '" + checkinID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + twoDaysAgoDate
				+ "', `updated_at` = '" + twoDaysAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query16 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue16.contains("MembershipExpiryOnSpecificDate");
		Assert.assertTrue(expireItemTypeflag,
				"MembershipExpiryOnSpecificDate is not present in expire_item_type column on expiry_events table");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify total_credits as {450}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "450.0", "total_credits is not matching");
		// Verify total_debits as {20}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"total_debits", 10);
		Assert.assertEquals(expColValue10, "20.00", "total_debits is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {450}
		String query12 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "450", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		String query13 = "SELECT non_transaction_spent_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		String query14 = "SELECT non_transaction_unexpired_spent_points FROM account_summaries WHERE user_id = "
				+ userID + ";";
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {450}
		String query8 = "SELECT non_transaction_total_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "450", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as {430=450-20}
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		String remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points")
				.toString();
		Assert.assertEquals(remainingRedeemablePoints, "430.0",
				"remaining redeemable points in user balance is not matching");

		// second checkin of {50} points -> {50*3 = 150} redeemable points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		// Verify membership_evaluation_points as {150}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "150", "membership_evaluation_points is not matching");
		// Verify total_credits as {600=450+150}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "600.0", "total_credits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {600=450+150}
		expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue12, "600", "loyalty_points is not matching");
		// Verify non_transaction_spent_points as {20}
		expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, expColValue11, "non_transaction_spent_points is not matching");
		// verify non_transaction_unexpired_spent_points as {20}
		expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, "20", "non_transaction_unexpired_spent_points is not matching");
		// Verify non_transaction_total_points as {600}
		expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"non_transaction_total_points", 5);
		Assert.assertEquals(expColValue8, "600", "non_transaction_total_points is not matching");
		// Verify remaining redeemable points in Mobile User Balance API as
		// {580=430+150}
		userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		remainingRedeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(remainingRedeemablePoints, "580.0",
				"remaining redeemable points in user balance is not matching");
	}
	
	@Owner(name = "Vaibhav Agnihotri")
	public void updateAcctSummaryValidationDBFlagsPUR(String expColValue) throws Exception {
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		// Set enable_account_summary to false
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, "date_to_determine_membership_level value is not updated to false");
		// Set date_to_determine_membership_level to one year ago date
		DBUtils.updatePreference(env, expColValue, oneYearAgoDate, businessId,
				dataSet.get("dbFlag4"),"businesses");
		// Set track_points_spent to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, "track_points_spent value is not updated to true");
		// Set track_reward_banked_points to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, "track_reward_banked_points value is not updated to true");
		// Below flags are not mentioned in TCs but can impact the results
		// Set has_membership_levels to true
		boolean hasMembershipLevelsFlag = DBUtils.updateBusinessesPreference(env, expColValue,
				"true", "has_membership_levels", businessId);
		Assert.assertTrue(hasMembershipLevelsFlag, "has_membership_levels value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to false
		boolean excludeGiftedPointsFlag = DBUtils.updateBusinessesPreference(env, expColValue,
				"false", "exclude_gifted_points_from_tier_progression", businessId);
		Assert.assertTrue(excludeGiftedPointsFlag,
				"exclude_gifted_points_from_tier_progression value is not updated to false");
		// Set enable_points_spent_backfill to false
		boolean enablePointsSpentBackfillFlag = DBUtils.updateBusinessesPreference(env,
				expColValue, "false", "enable_points_spent_backfill", businessId);
		Assert.assertTrue(enablePointsSpentBackfillFlag, "enable_points_spent_backfill value is not updated to false");
		// Set enable_expiry_unification to false
		boolean enableExpiryUnificationFlag = DBUtils.updateBusinessesPreference(env, expColValue,
				"false", "enable_expiry_unification", businessId);
		Assert.assertTrue(enableExpiryUnificationFlag, "enable_expiry_unification value is not updated to false");
		logger.info("Business preferences flags are updated to respective values");
		TestListeners.extentTest.get().info("Business preferences flags are updated to respective values");
	}

	// Check and update the membership levels
	@Owner(name = "Vaibhav Agnihotri")
	public void updateMembershipLevelsDB() throws Exception {
		String membershipMinPoints, membershipMaxPoints, membershipId;
		int maxLevels = 3;
		for (int i = 1; i <= maxLevels; i++) {
			membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			membershipId = dataSet.get("membership" + i + "Id");

			String minPointsCheckQuery = "SELECT min_points FROM membership_levels WHERE id = $id;".replace("$id",
					membershipId);
			String expMinPoints = DBUtils.executeQueryAndGetColumnValue(env, minPointsCheckQuery,
					"min_points");
			String maxPointsCheckQuery = "SELECT max_points FROM membership_levels WHERE id = $id;".replace("$id",
					membershipId);
			String expMaxPoints = DBUtils.executeQueryAndGetColumnValue(env, maxPointsCheckQuery,
					"max_points");

			boolean minMatches = expMinPoints.equals(membershipMinPoints);
			boolean maxMatches = expMaxPoints.equals(membershipMaxPoints);

			if (minMatches && maxMatches) {
				utils.logit("Membership level: " + dataSet.get("membership" + i) + " already has min_points: "
						+ membershipMinPoints + " and max_points: " + membershipMaxPoints);
			} else {
				if (!minMatches) {
					String updateMinQuery = "UPDATE membership_levels SET min_points = $points WHERE id = $id;"
							.replace("$points", membershipMinPoints).replace("$id", membershipId);
					int result = DBUtils.executeUpdateQuery(env, updateMinQuery);
					Assert.assertEquals(result, 1);
				}
				if (!maxMatches) {
					String updateMaxQuery = "UPDATE membership_levels SET max_points = $points WHERE id = $id;"
							.replace("$points", membershipMaxPoints).replace("$id", membershipId);
					int result = DBUtils.executeUpdateQuery(env, updateMaxQuery);
					Assert.assertEquals(result, 1);
				}
				utils.logit("Updated membership level: " + dataSet.get("membership" + i) + " (min_points="
						+ membershipMinPoints + ", max_points=" + membershipMaxPoints + ")");
			}
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
