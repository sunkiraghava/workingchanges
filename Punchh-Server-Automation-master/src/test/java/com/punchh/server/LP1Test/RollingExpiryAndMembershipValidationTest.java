package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RollingExpiryAndMembershipValidationTest {

	private static Logger logger = LogManager.getLogger(RollingExpiryAndMembershipValidationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	private Utilities utils;
	String run = "ui";
	String amount = "65.0";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4521 [Points to Reward] Validate reward gifting on the basis of reward redemption mark set in membership level", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T4521_validateRewardGiftingWithMembershipLevel() throws Exception {
		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMark"));
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			TestListeners.extentTest.get().info("update the membership -- " + dataSet.get("membership" + i));
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(200, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList1.get(i) + " is visible");
			counter++;
		}
		Assert.assertEquals(counter, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(200, resp2.getStatusCode(), "Status code 200 did not matched for post checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
			Assert.assertTrue(val, "point range -- " + rangeList3.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList3.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList3.get(i) + " is visible");
			counter3++;
		}
		Assert.assertEquals(counter3, Integer.parseInt(dataSet.get("expectedSize3")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize3"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");
	}

	@Test(description = "SQ-T4523 [Points to Reward] Validate reward gifting after guest inactivity expiry", groups = {
			"regression", "unstable", "dailyrun" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T4523_ValidateRewardGiftingAfterGuestInactivityExpiry() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "2000", "2500");

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update loyalty goal completion
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);

			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMark"));
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			TestListeners.extentTest.get().info("update the membership -- " + dataSet.get("membership" + i));
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(200, resp1.getStatusCode(), "Status code 200 did not matched for post checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList1.get(i) + " is visible");
			counter++;
		}
		Assert.assertEquals(counter, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(200, resp2.getStatusCode(), "Status code 200 did not matched for post checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
			Assert.assertTrue(val, "point range -- " + rangeList3.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList3.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList3.get(i) + " is visible");
			counter3++;
		}
		Assert.assertEquals(counter3, Integer.parseInt(dataSet.get("expectedSize3")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize3"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");

		String updateQuery = "update users set last_activity_at='" + CreateDateTime.getPreviousDate(2)
				+ " 12:01:58' where id=" + userID;

		// String updateQuery="update users set last_activity_at='2024-05-10 12:01:58'
		// where id="+userID;
		DBUtils.executeUpdateQuery(env, updateQuery);

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// effective
		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		String expColValue_flag = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "membership_level_id",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue(expColValue_flag.equals(membershipId1) || expColValue_flag.equals(membershipId2),
				"Effective membership strategy is not " + membershipId1 + " after scheduler run");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");

		// operative
		String query1 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId2,
				"Operative membership strategy is not " + membershipId2 + "after scheduler run");
		logger.info("Verified that Operative membership strategy is " + membershipId2 + "after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Operative membership strategy is " + membershipId2 + "after scheduler run");
		utils.longWaitInSeconds(8);
		// loyalty points
		String query2 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "loyalty_points", 10); // dbUtils.getValueFromColumn(query2,
																													// "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for user");
		logger.info("Verified that Initial points is 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 1200.0 for user");

		// giftpoints
		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																												// "membership_level_id");
		Assert.assertEquals(expColValue3, "0.0", "Gifted points is not 0.0 for user");
		logger.info("Verified that Gifted points is 0.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 0.0 for user");

		// credits
		String query4 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "total_credits", 10); // dbUtils.getValueFromColumn(query2,
																													// "membership_level_id");
		Assert.assertEquals(expColValue4, "0.0", "total_credits is not 0.0 for Migrated user");
		logger.info("Verified that total_credits is 0.0 for Migrated user");
		utils.logPass("Verified that total_credits is 0.0 for Migrated user");

		// total lifetime points
		String query5 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "total_lifetime_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue5, "1200.0", "total_lifetime_points is not 1200.0 for Migrated user");
		logger.info("Verified that total_lifetime_points is 1200.0 for user");
		utils.logPass("Verified that total_lifetime_points is 1200.0 for user");

		// previous membership
		String query6 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "previous_membership_level",
				10);
		Assert.assertEquals(expColValue6, "Gold Level", "membership_levels is not Bronze Level for Migrated user");
		logger.info("Verified that previous_membership_level is Bronze Level for Migrated user");
		TestListeners.extentTest.get()
				.pass("Verified that previous_membership_level is Bronze Level for Migrated user");

		// initial visits
		String query7 = "select initial_visits from accounts where user_id=" + userID;
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "initial_visits", 3);
		Assert.assertEquals(expColValue7, null, "initials visit is not null");
		logger.info("Verfied initials visits are null");
		utils.logPass("Verfied initials visits are null");

		// gift some points
		Response resp4 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount4"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for post checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// initial visits
		String query8 = "select initial_visits from accounts where user_id=" + userID;
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "initial_visits", 10);
		Assert.assertEquals(expColValue8, "200", "initials visit is not 2");
		logger.info("Verfied initials visits are 2");
		utils.logPass("Verfied initials visits are 2");

		// membership level
		String query9 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "membership_level", 10);
		Assert.assertEquals(expColValue9, "Silver Level", "membership is not silver");
		logger.info("Verified membership is silver");
		utils.logPass("Verified membership is silver");

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
