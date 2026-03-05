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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MembershipAcctSummaryPTCTest {

	private static Logger logger = LogManager.getLogger(MembershipAcctSummaryPTCTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env, baseUrl, businessesQuery, businessId, oneYearAgoDate, oneYearAgoYesterdayDate,
			todaysDate, yesterdayDate, twoDaysAgoDate, currentDay, currentMonth;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	boolean flag1, flag3, flag4;
	int rs;
	private String userEmail;

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
		rs = 0;
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6424: Verify Membership Evaluation for PTC with Unredeemed Expiry", priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6424_unredeemedExpiryMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "110";
		String checkinAmount2 = "100";
		String checkinAmount3 = "50";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		yesterdayDate = CreateDateTime.getYesterdayDays(1);
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry and Membership level configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
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
			utils.logit(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {110} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
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

		// second checkin of {100} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Force redemption of {10} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "10", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		// third checkin of {50} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount3);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify expired_at for first checkin
		String query4 = "SELECT expired_at FROM checkins WHERE id = '" + checkinID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"expired_at", 60);
		Assert.assertEquals(expColValue4, yesterdayDate, "expired_at is not matching");

		// Set enable_account_summary to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Refresh the user account
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();

		// Verify membership_evaluation_points = earned points + expired points
		// {260} = {110} + {100} + {50}
		int membershipEvaluationPoints = Integer.parseInt(checkinAmount1) + Integer.parseInt(checkinAmount2)
				+ Integer.parseInt(checkinAmount3);
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, String.valueOf(membershipEvaluationPoints),
				"membership_evaluation_points is not matching");
		// Verify non_transaction_unexpired_points = earned points - expired points
		// {150} = {260} - {110}
		int unexpiredPoints = membershipEvaluationPoints - Integer.parseInt(checkinAmount1);
		String query7 = "SELECT non_transaction_unexpired_points FROM account_summaries WHERE user_id = " + userID
				+ ";";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"non_transaction_unexpired_points", 5);
		Assert.assertEquals(expColValue7, String.valueOf(unexpiredPoints),
				"non_transaction_unexpired_points is not matching");
		// Verify effective membership ID for {Gold}
		String query8 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue8, dataSet.get("membership3Id"), "membership level ID is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6425: Verify Membership Evaluation for PTC with Guest Inactivity", priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6425_guestInactivityMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "150";
		String checkinAmount2 = "120";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoYesterdayDate = CreateDateTime.getNyearAgoYesterdayDate(1);
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {150} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

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
				+ oneYearAgoYesterdayDate + "' WHERE id = '" + checkin_id + "';";
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
		// Verify that expiry is ran
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

		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Bronze}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " ORDER BY created_at DESC LIMIT 1;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");

		// second checkin of {120} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID2 = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Verify membership_evaluation_points as {120}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "120", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Silver}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6430: Verify Membership Evaluation for PTC with Annually on signup anniversary", priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6430_signupAnniversaryMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "250.0";
		String checkinAmount2 = "110.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {250} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");

		// Verify total_credits as {245}
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "245.0", "total_credits is not matching");
		// Verify effective membership ID for {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		String query7 = "SELECT expiring_at FROM reward_credits WHERE user_id = " + userID
				+ " and expiring_at IS NOT NULL;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

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

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query8 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue8.contains("AnnualExpiry");
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
		// Verify effective membership ID for {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// second checkin of {110} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Verify membership_evaluation_points as {110}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "110", "membership_evaluation_points is not matching");
		// Verify total_credits as {110}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, checkinAmount2, "total_credits is not matching");
		// Verify effective membership ID for {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6439: Verify Membership Evaluation for PTC with Decouple Annually on signup anniversary", priority = 3)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6439_decoupleSignupAnniversaryMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "250.0";
		String checkinAmount2 = "110.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {250} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		// changing the dates in redemptions table
		String query9 = "UPDATE `redemptions` SET `created_at` = '" + oneYearAgoDate + "' , `updated_at` = '"
				+ oneYearAgoDate + "' WHERE user_id = '" + userID + "';";
		int rs9 = DBUtils.executeUpdateQuery(env, query9);
		Assert.assertEquals(rs9, 1, "redemptions table query is not working");

		// Verify total_credits as {245}
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "245.0", "total_credits is not matching");
		// Verify effective membership ID for {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		String query7 = "SELECT expiring_at FROM reward_credits WHERE user_id = " + userID
				+ " AND expiring_at IS NOT NULL;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

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

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query8 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue8.contains("AnnualExpiry");
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

		// Verify total_credits as {245}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 5);
		Assert.assertEquals(expColValue4, "245.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify effective membership ID for {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// second checkin of {110} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Verify membership_evaluation_points as {110}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "110", "membership_evaluation_points is not matching");
		// Verify total_credits as {355}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "355.0", "total_credits is not matching");
		// Verify effective membership ID for {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6431: Verify Membership Evaluation for PTC with Annually on membership-bump anniversary", priority = 4)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6431_bumpAnniversaryMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "210.0";
		String checkinAmount2 = "110.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		todaysDate = CreateDateTime.getCurrentDate();
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {210} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
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

		// Verify total_credits as {205}
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "205.0", "total_credits is not matching");
		// Verify effective membership ID for {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND scheduled_expiry_on IS NOT NULL ORDER BY updated_at DESC LIMIT 1;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		String query7 = "SELECT expiring_at FROM reward_credits WHERE user_id = " + userID
				+ " AND expiring_at IS NOT NULL;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query10 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue10.contains("Rolling");
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

		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 5);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify effective membership ID for {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// second checkin of {110} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Verify total_credits as {110}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "110.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {110}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "110", "membership_evaluation_points is not matching");
		// Verify effective membership ID for {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6440: Verify Membership Evaluation for PTC with decouple Annually on membership-bump anniversary", priority = 5)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6440_decoupleBumpAnniversaryMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "210.0";
		String checkinAmount2 = "110.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		todaysDate = CreateDateTime.getCurrentDate();
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().clickUpdateBtn();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {210} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
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

		// Verify total_credits as {205}
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "205.0", "total_credits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND scheduled_expiry_on IS NOT NULL ORDER BY updated_at DESC LIMIT 1;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		String query7 = "SELECT expiring_at FROM reward_credits WHERE user_id = " + userID
				+ " AND expiring_at IS NOT NULL;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query10 = "SELECT `business_id` FROM `expiry_events` WHERE `user_id` = " + userID + ";";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"business_id", 60);
		Assert.assertEquals(expColValue10, dataSet.get("business_id"),
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

		// Verify total_credits as {205}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 5);
		Assert.assertEquals(expColValue4, "205.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Bronze}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// second checkin of {110} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Verify membership_evaluation_points as {110}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "110", "membership_evaluation_points is not matching");
		// Verify total_credits as {315}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "315.0", "total_credits is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6432: Verify Membership Evaluation for PTC with Annually on specific date", priority = 6)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6432_specificDateMembershipEvaluationPTC() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		String checkinAmount1 = "210.0";
		String checkinAmount2 = "110.0";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery,
				"preferences");
		currentDay = CreateDateTime.getCurrentDateOnly();
		currentMonth = CreateDateTime.getCurrentMonthOnly();
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		updateAcctSummaryValidationDBFlagsPTC(expColValue);

		// Update Checkin Expiry configs
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
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

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// first checkin of {210} points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				checkinAmount1);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinID = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// Force redemption of {5} points
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
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

		// Verify total_credits as {205}
		String query4 = "SELECT total_credits FROM accounts WHERE user_id = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "205.0", "total_credits is not matching");
		// Verify effective membership level as {Gold}
		String query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND previous_membership_level_id IS NOT NULL;";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership3Id"), "membership level ID is not matching");
		// Verify loyalty_points as {210}
		String query8 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue8, "210.0", "loyalty_points is not matching");
		// Verify that banked points have no expiry date
		String query7 = "SELECT expiring_at FROM reward_credits WHERE user_id = " + userID
				+ " AND expiring_at IS NOT NULL;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);
		// Verify that expiry is ran
		String query11 = "SELECT `expire_item_type` FROM `expiry_events` WHERE `user_id` = '" + userID + "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"expire_item_type", 60);
		boolean expireItemTypeflag = expColValue11.contains("SpecificDateExpiry");
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

		// Verify total_credits as {0}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 5);
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {0}
		String query6 = "SELECT membership_evaluation_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 60);
		Assert.assertEquals(expColValue6, "0", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Bronze}
		query5 = "SELECT membership_level_id FROM membership_level_histories where user_id = " + userID
				+ " AND expired_at IS NULL ORDER BY updated_at DESC LIMIT 1;";
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1Id"), "membership level ID is not matching");
		// Verify loyalty_points as {0}
		String query10 = "SELECT loyalty_points FROM account_summaries WHERE user_id = " + userID + ";";
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue10, "0", "loyalty_points is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

		// second checkin of {110} points
		checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), checkinAmount2);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Verify total_credits as {110}
		expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"total_credits", 10);
		Assert.assertEquals(expColValue4, "110.0", "total_credits is not matching");
		// Verify membership_evaluation_points as {110}
		expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_evaluation_points", 5);
		Assert.assertEquals(expColValue6, "110", "membership_evaluation_points is not matching");
		// Verify effective membership level as {Silver}
		expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership2Id"), "membership level ID is not matching");
		// Verify loyalty_points as {110}
		expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"loyalty_points", 5);
		Assert.assertEquals(expColValue10, "110", "loyalty_points is not matching");
		// Verify that banked points have no expiry date
		expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"expiring_at", 2);
		Assert.assertEquals(expColValue7, "", "expiring_at is having some value");

	}

	@Owner(name = "Vaibhav Agnihotri")
	public void updateAcctSummaryValidationDBFlagsPTC(String expColValue) throws Exception {
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		// Set enable_account_summary to false
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		// Set date_to_determine_membership_level to one year ago date
		DBUtils.updatePreference(env, expColValue, oneYearAgoDate, businessId,
				dataSet.get("dbFlag2"),"businesses");
		// Set track_reward_banked_points to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set consider_effective_redeemed_unbanked_points to true
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to true");
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
		utils.logit("Business preferences flags are updated to respective values");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6365 Verify membership reset without point expiry using Annually on Membership Bump Anniversary in decoupled strategy in PTC")
	@Owner(name = "Hardik Bhardwaj")
	public void T6365_decoupleExpiryWithAnnuallyOnMembershipBumpAnniversary() throws Exception {
		
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String date = CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

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
			utils.logit(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
		}
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
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
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		String newscheduled_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		String query = " UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		utils.logit("Users table updated successfully.");

		// DB query for accounts updation

		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		utils.logit("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		utils.logit("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		utils.logit("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelIdGold = dataSet.get("membershipLevelIdGold");

		// DB query for membership_level_histories updation
		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelIdGold + "' ;";
		int rs5 = DBUtils.executeUpdateQuery(env, query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		utils.logit("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"expire_item_type", 150);
		boolean expireItemTypeFlag = expireItemTypeColValue.contains("MembershipExpiryOnlyOnBumpAnniversary");
		Assert.assertEquals(expireItemTypeFlag, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in expire_item_type column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");
		utils.logPass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in expire_item_type column in expiry_events table");

		String query7 = "Select `membership_level_id` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "membership_level_id", 100);
		boolean flag7 = expColValue7.contains(dataSet.get("membershipLevelIdGold"));
		Assert.assertEquals(flag7, true,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in membership_level_id column in expiry_events table");
		logger.info(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");
		utils.logPass(
				"MembershipExpiryOnlyOnBumpAnniversary is present in membership_level_id column in expiry_events table");

		String query8 = "Select `value` from `expiry_events` where `user_id` = '" + userID + "';";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValue(env, query8, "value");
		Assert.assertEquals(expColValue8, null,
				"MembershipExpiryOnlyOnBumpAnniversary is not present in value column in expiry_events table");
		logger.info("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");
		utils.logPass("MembershipExpiryOnlyOnBumpAnniversary is present in value column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		utils.logPass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		utils.logPass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6364 Verify membership reset without point expiry using Annually on Sign-Up Anniversary in decoupled strategy in PTC")
	@Owner(name = "Hardik Bhardwaj")
	public void T6364_decoupleExpiryWithGuestInactivityDaysAndSignupAnniversary() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String date = CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

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
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage()
				.decoupledMembershipLevelEvaluationStrategy("Annually On sign Up Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
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
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		utils.logit("Users table updated successfully.");

		// DB query for accounts updation

		String query1 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		utils.logit("Accounts table updated successfully.");

		// DB query for checkins updation
		String query2 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		utils.logit("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query4 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		int rs4 = DBUtils.executeUpdateQuery(env, query4);
		logger.info("membership_level_histories table updated successfully.");
		utils.logit("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB queries for expiry_events
		Utilities.longWait(4000);
		String query6 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "expire_item_type",
				150);
		boolean expireItemTypeFlag = expireItemTypeValue.contains("AnnualExpiry");
		Assert.assertEquals(expireItemTypeFlag, true,
				"AnnualExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("AnnualExpiry is present in expire_item_type column in expiry_events table");
		utils.logPass("AnnualExpiry is present in expire_item_type column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		utils.logPass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		utils.logPass(
				"Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6363 Validate In guest_inactivity expiry, membership_evaluation_points should reset to zero for PTC business || "
			+ "SQ-T6353 Validate that guest inactivity expiry and check all applicable field adjustments for PTC business || "
			+ "SQ-T6349 Validate that last_visit is set to NULL when all loyalty check-ins are expired for PTC business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6363_rollingExpiryWithGuestInactivity() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String date = CreateDateTime.getYesterdayDays(181); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

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
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
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
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
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
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(8000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("InactiveGuests");
		Assert.assertEquals(expireItemTypeflag, true,
				"InactiveGuests is not present in expire_item_type column in expiry_events table");
		logger.info("InactiveGuests is present in expire_item_type column in expiry_events table");
		utils.logPass("InactiveGuests is present in expire_item_type column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		utils.logPass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "0",
				"Value is not 0 for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is 0 for the membership_evaluation_points column in account_summaries table.");
		utils.logPass(
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
		utils.logit(
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
		utils.logit(
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
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

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
		utils.logPass("verified that non_transaction_unexpired_spent_points is " + zero
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// Check non_transaction_total_points in account_summaries table
		String lastCheckinTimeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, lastCheckinTimeQuery,
				"last_checkin_time", 2);
		Assert.assertEquals(lastCheckinTimeValue, null,
				"last_checkin_time column in account_summaries table is not equal to expected value i.e. NULL");
		logger.info("verified that last_checkin_time in account_summary table is equal to NULL");
		utils.logPass("verified that last_checkin_time in account_summary table is equal to NULL");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6354 Validate that If no check-ins qualify for expiry, summary should remain unchanged for PTC business. || "
			+ "SQ-T6352 Validate In unredeemed_point_expiry, membership_evaluation_points should not change. PTC business || "
			+ "SQ-T6347 Validate that loyalty_points and loyalty_count are reduced after loyalty check-ins expire for PTC business || "
			+ "SQ-T6348 Validate that without_transaction_checkins_points and without_transaction_checkins_count update on expiry for PTC business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6354_rollingExpiryWithExactDays() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String date = CreateDateTime.getYesterdayDays(180); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

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
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
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
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
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
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(6000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
		Assert.assertEquals(expireItemTypeflag, true,
				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
		utils.logPass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "membership_level_id");
		Assert.assertEquals(expColValue2, null,
				"membership_level_id is not null in membership_level_id column in expiry_events table");
		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
		utils.logPass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdGold = dataSet.get("membershipLevelIdGold");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdGold,
				"Value is not " + membershipLevelIdGold + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdGold + " for the membership level expiry entry.");
		utils.logPass("Verified that Value is " + membershipLevelIdGold + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, dataSet.get("amount"), "Value is not "
				+ dataSet.get("amount") + " for the membership_evaluation_points column in account_summaries table");
		logger.info("Verified that Value is " + dataSet.get("amount")
				+ " for the membership_evaluation_points column in account_summaries table.");
		utils.logPass("Verified that Value is " + dataSet.get("amount")
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
		utils.logit(
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
		utils.logit(
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
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

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
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		logger.info("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		utils.logPass("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// Check non_transaction_unexpired_count in account_summaries table
		String nonTransactionUnexpiredCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredCountQuery, "non_transaction_unexpired_count", 5);
		Assert.assertEquals(nonTransactionUnexpiredCountValue, "0",
				"non_transaction_unexpired_count column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");
		utils.logPass(
				"verified that non_transaction_unexpired_count in account_summary table is equal to 0 after rolling expiry");

		// Check non_transaction_unexpired_points in account_summaries table
		String nonTransactionUnexpiredPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredPointsQuery, "non_transaction_unexpired_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredPointsValue, "0",
				"non_transaction_unexpired_points column in account_summaries table is not equal to expected value i.e. 0");
		logger.info(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
		utils.logPass(
				"verified that non_transaction_unexpired_points in account_summary table is equal to 0 after rolling expiry");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6350 Verify pending_points field should not get  reduced by amount from expired pending check-ins. for PTC business")
	@Owner(name = "Hardik Bhardwaj")
	public void T6350_rollingExpiryWithExactDaysPendingCheckin() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String date = CreateDateTime.getYesterdayDays(181); // CreateDateTime.previousYearDate(1);

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for track_points_spent
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// updating the business preference for enable_account_summary
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag1") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

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
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkin_resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
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
		utils.logit("Users table updated successfully.");

		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "', `scheduled_expiry_on` = '" + newscheduled_at + "' WHERE id = '" + checkin_id
				+ "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		utils.logit("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(6000);
		String query3 = "Select `expire_item_type` from `expiry_events` where `user_id` = '" + userID + "';";
		String expireItemTypeValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "expire_item_type",
				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		boolean expireItemTypeflag = expireItemTypeValue.contains("UnredeemedPointsExpiry");
		Assert.assertEquals(expireItemTypeflag, true,
				"UnredeemedPointsExpiry is not present in expire_item_type column in expiry_events table");
		logger.info("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");
		utils.logPass("UnredeemedPointsExpiry is present in expire_item_type column in expiry_events table");

		String query2 = "Select `membership_level_id` from `expiry_events` where `user_id`= '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "membership_level_id");
		Assert.assertEquals(expColValue2, null,
				"membership_level_id is not null in membership_level_id column in expiry_events table");
		logger.info("membership_level_id is null in membership_level_id column in expiry_events table");
		utils.logPass("membership_level_id is null in membership_level_id column in expiry_events table");

		String membershipLevelIdBronze = dataSet.get("membershipLevelIdBronze");
		// DB query for membership_level_histories
		String membershipLevelIdQuery = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "' order by id desc LIMIT 1 ;";
		String membershipLevelIdColValue = DBUtils.executeQueryAndGetColumnValue(env, membershipLevelIdQuery,
				"membership_level_id");
		Assert.assertEquals(membershipLevelIdColValue, membershipLevelIdBronze,
				"Value is not " + membershipLevelIdBronze + " for the membership level expiry entry");
		logger.info("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");
		utils.logPass("Verified that Value is " + membershipLevelIdBronze + " for the membership level expiry entry.");

		String membershipEvaluationPointsQuery = "Select `membership_evaluation_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String membershipEvaluationPointsColValue = DBUtils.executeQueryAndGetColumnValue(env,
				membershipEvaluationPointsQuery, "membership_evaluation_points");
		Assert.assertEquals(membershipEvaluationPointsColValue, "20",
				"Value is not 20 for the membership_evaluation_points column in account_summaries table");
		logger.info(
				"Verified that Value is 20 for the membership_evaluation_points column in account_summaries table.");
		utils.logPass(
				"Verified that Value is 20 for the membership_evaluation_points column in account_summaries table.");

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
		String accountSummaryLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		logger.info(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");
		utils.logit(
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
		utils.logit(
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
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		String nonTransactionUnexpiredSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionUnexpiredSpentPointsQuery, "non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		logger.info("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");
		utils.logPass("verified that non_transaction_unexpired_spent_points is " + pointsSpent
				+ " and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				nonTransactionTotalPointsQuery, "non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		logger.info(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		logger.info(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		logger.info(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
