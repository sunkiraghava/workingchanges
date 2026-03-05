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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MembershipValidationTest3 {

	private static Logger logger = LogManager.getLogger(MembershipValidationTest3.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, businessId, businessesQuery;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	String run = "ui";
	String amount = "65.0";
	Utilities utils;

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
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T5396 [Points to Currency] Validate banked currency gifting from membership configuration", priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T5396_verifyBankedCurrencyGiftingFromMembership() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// set redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().setRewardValueInMembershipLevel(dataSet.get("rewardValue" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// open guestTimeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().refreshTimeline();

		int totalExpectedCurrency1 = 2 * Integer.parseInt(dataSet.get("rewardValue1").replace(".0", ""));
		int userCurrency1 = pageObj.guestTimelinePage().getRedeemableRewardOrBankedCurrencyCount("Redeemable Rewards");

		Assert.assertEquals(userCurrency1, totalExpectedCurrency1, "Banked currency did not matched as expected");
		logger.info("Verified user successfully get banked currency from membership configuration for level : "
				+ dataSet.get("membership1") + " And point range - " + dataSet.get("range1"));
		TestListeners.extentTest.get()
				.pass("Verified user successfully get banked currency from membership configuration for level : "
						+ dataSet.get("membership1") + " And point range - " + dataSet.get("range1"));

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().refreshTimeline();

		int totalExpectedCurrency2 = 4 * Integer.parseInt(dataSet.get("rewardValue1").replace(".0", ""));
		int userCurrency2 = pageObj.guestTimelinePage().getRedeemableRewardOrBankedCurrencyCount("Redeemable Rewards");

		Assert.assertEquals(userCurrency2, totalExpectedCurrency2, "Banked currency did not matched as expected");
		logger.info("Verified user successfully get banked currency from membership configuration for level : "
				+ dataSet.get("membership1") + " And point range - " + dataSet.get("range2"));
		TestListeners.extentTest.get()
				.pass("Verified user successfully get banked currency from membership configuration for level : "
						+ dataSet.get("membership1") + " And point range - " + dataSet.get("range2"));

		// create checkin-3
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount3"));
		pageObj.apiUtils().verifyResponse(checkinResponse3, "POS checkin");
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().refreshTimeline();

		int totalExpectedCurrency3 = 4 * Integer.parseInt(dataSet.get("rewardValue1").replace(".0", ""))
				+ Integer.parseInt(dataSet.get("rewardValue2").replace(".0", ""));
		int userCurrency3 = pageObj.guestTimelinePage().getRedeemableRewardOrBankedCurrencyCount("Redeemable Rewards");

		Assert.assertEquals(userCurrency3, totalExpectedCurrency3, "Banked currency did not matched as expected");
		logger.info("Verified user successfully get banked currency from membership configuration for level : "
				+ dataSet.get("membership2") + " And point range - " + dataSet.get("range3"));
		TestListeners.extentTest.get()
				.pass("Verified user successfully get banked currency from membership configuration for level : "
						+ dataSet.get("membership2") + " And point range - " + dataSet.get("range3"));

		String level = pageObj.guestTimelinePage().verifyGuestmembershipLabel();
		Assert.assertEquals(level, "Silver Level", "Membership Level does not match as expected");
		logger.info("Verified membership level bump to : " + level);
		TestListeners.extentTest.get().pass("Verified membership level bump to : " + level);

	}

	// Rakhi
	@Test(description = "SQ-T6057  [Point converts to Currency] Verify the Reward Value field process for the user which is set on Business Level", priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T6057_verifyRewardValueFiledProcess() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().setBankedRewardValue("10");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// ** commenting beow section to not update memebrship level **
//		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().setRewardValueInMembershipLevel("");
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag1 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardValue1"));
		Assert.assertTrue(flag1, "Verfied Reward Credit " + dataSet.get("rewardValue1") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardValue1") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardValue1") + " is visible");

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag2 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardValue2"));
		Assert.assertTrue(flag2, "Verfied Reward Credit " + dataSet.get("rewardValue2") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardValue2") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardValue2") + " is visible");

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership3") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership3") + " visible");

		// create checkin-3
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount3"));
		pageObj.apiUtils().verifyResponse(checkinResponse3, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag3 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardValue3"));
		Assert.assertTrue(flag3, "Verfied Reward Credit " + dataSet.get("rewardValue3") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardValue3") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardValue3") + " is visible");

		int redeemableReward = pageObj.guestTimelinePage()
				.getRedeemableRewardOrBankedCurrencyCount("Redeemable Rewards");
		int point = Integer.parseInt(dataSet.get("totalRewardAmount"));
		Assert.assertEquals(redeemableReward, point, "Redeemable Rewards is not correct on user timeline");
		logger.info("Verified that Redeemable Rewards is correct on user timeline ie : " + redeemableReward);
		TestListeners.extentTest.get()
				.pass("Verified that Redeemable Rewards is correct on user timeline ie : " + redeemableReward);
	}

	// Rakhi
	@SuppressWarnings("unused")
	@Test(description = "SQ-T6062 [Point converts to Currency] Verify the Reward Value field process for the user which is set on Membership Level"
			+ "SQ-T6064 [Points converts to Currency] Verify the Expiry process for business by running the membership level bump anniversary expiry", priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T6062_verifyMembershipRewardValueFiledProcess() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		// Set live flag to false
		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		// Set went_live to false
		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "2000", "2500");

		// open business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update pending checkin
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().selectPendingCheckinStrategy("No pending checkins", "0");

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().setRewardValueInMembershipLevel(dataSet.get("rewardValue" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().editRedeemptionMarkFromMembership("");
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		// String token =
		// signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag1 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardCredit1"));
		Assert.assertTrue(flag1, "Verfied Reward Credit " + dataSet.get("rewardCredit1") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardCredit1") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardCredit1") + " is visible");

		// verify membership is bumped to silver level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag2 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardCredit2"));
		Assert.assertTrue(flag2, "Verfied Reward Credit " + dataSet.get("rewardCredit2") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardCredit2") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardCredit2") + " is visible");

		// verify membership is bumped to gold level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership3") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership3") + " visible");

		// create checkin-3
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount3"));
		pageObj.apiUtils().verifyResponse(checkinResponse3, "POS checkin");

		// verify credited reward value
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag3 = pageObj.guestTimelinePage().verifyRewardCredit(dataSet.get("rewardCredit3"));
		Assert.assertTrue(flag3, "Verfied Reward Credit " + dataSet.get("rewardCredit3") + " is not visible");
		logger.info("Verfied Reward Credit " + dataSet.get("rewardCredit3") + " is visible");
		TestListeners.extentTest.get().pass("Verfied Reward Credit " + dataSet.get("rewardCredit3") + " is visible");

		// verify total earned Redeemable Rewards
		int redeemableReward = pageObj.guestTimelinePage()
				.getRedeemableRewardOrBankedCurrencyCount("Redeemable Rewards");
		int points = Integer.parseInt(dataSet.get("totalRewardAmount"));
		Assert.assertEquals(redeemableReward, points, "Redeemable Rewards is not correct on user timeline");
		logger.info("Verified that Redeemable Rewards is correct on user timeline ie : " + redeemableReward);
		TestListeners.extentTest.get()
				.pass("Verified that Redeemable Rewards is correct on user timeline ie : " + redeemableReward);

		// Set checkin expiry Account re-evaluation strategy
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().updateConfiguration();

		String newcreated_at = CreateDateTime.getCurrentDate();
		// DB query for accounts updation
		String query = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at
				+ "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query2);
		// Assert.assertEquals(rs, 1, "membership_level_histories table query is not
		// working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().pass("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// DB query for expiry_events table
		String query1 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "business_id", 100);
		Assert.assertEquals(expColValue, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		logger.info("Entry is created in expiry_events table in DB");
		TestListeners.extentTest.get().pass("Entry is created in expiry_events table in DB");

		// accounts table
		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits", 100);
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_debits column in accounts table is equal to expected value i.e. 0");

		// effective
		String query3 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by id desc limit 1";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "membership_level_id", 10);
		Assert.assertEquals(expColValue1, dataSet.get("membershipId1"),
				"Effective membership strategy is not " + dataSet.get("membershipId1") + "");
		logger.info("Verified that Effective membership strategy is " + dataSet.get("membershipId1") + " ");
		TestListeners.extentTest.get()
				.pass("Verified that Effective membership strategy is " + dataSet.get("membershipId1") + "");

		// operative
		String query4 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "'order by previous_membership_level_id desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue3, dataSet.get("membershipId3"),
				"Operative membership strategy is not " + dataSet.get("membershipId3") + "");
		logger.info("Verified that Operative membership strategy is " + dataSet.get("membershipId3") + "");
		TestListeners.extentTest.get()
				.pass("Verified that Operative membership strategy is " + dataSet.get("membershipId3") + "");

		// expired_at from
		String query5 = "Select expired_at from membership_level_histories where user_id = '" + userID
				+ "' AND expired_at IS NOT NULL ORDER BY expired_at DESC LIMIT 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "expired_at", 100);
		Assert.assertNotEquals(expColValue4, null,
				"Value is not present at expired_at column in membership_level_histories");
		logger.info("Value is present at expired_at column in membership_level_histories");
		TestListeners.extentTest.get().pass("Value is present at expired_at column in membership_level_histories");

	}

	// Rakhi
	@Test(description = "SQ-T6072 [Points Convert to Category Chosen by Guest] Verify the Reward Value field process for the user which is set on Business Level", priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T6072_validateRewardGiftingWithMembershipLevelPointToManual() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optout_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// set banked reward value on business level
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().setBankedRewardValue("10");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		String token = signUpResponse.jsonPath().get("access_token").toString();
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify gift reward in account history
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		String text = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text.contains("100 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount1"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount1"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints1 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints1, dataSet.get("expectedPoints1"),
				"User balance is not correct on timeline");
		logger.info("Verified that User balance is correct on timeline :" + actualredeemablePoints1);
		TestListeners.extentTest.get()
				.pass("Verified that User balance is correct on timeline :" + actualredeemablePoints1);

		// verify membership is bumped to silver level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		String text1 = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text1.contains("400 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount2"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount2"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints2 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints2, dataSet.get("expectedPoints2"),
				"User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		TestListeners.extentTest.get()
				.pass("Verified thet User balance is correct on timeline :" + actualredeemablePoints2);

		// verify membership is bumped to gold level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership3") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership3") + " visible");

		// create checkin-3
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount3"));
		pageObj.apiUtils().verifyResponse(checkinResponse3, "POS checkin");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		String text2 = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text2.contains("200 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount3"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount3"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints3 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints3, dataSet.get("expectedPoints3"),
				"User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		TestListeners.extentTest.get()
				.pass("Verified thet User balance is correct on timeline :" + actualredeemablePoints3);

	}

	// Rakhi
	@Test(description = "SQ-T6071 [Points Convert to Category Chosen by Guest] Perform the checkin process for the user which is set on Membership Level", priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T6071_validateRewardGiftingWithMembershipLevelPointToManual() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		String token = signUpResponse.jsonPath().get("access_token").toString();
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify gift reward in account history
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		String text = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text.contains("100 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount1"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount1"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints1 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints1, dataSet.get("expectedPoints1"),
				"User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline :" + actualredeemablePoints1);
		TestListeners.extentTest.get()
				.pass("Verified thet User balance is correct on timeline :" + actualredeemablePoints1);

		// verify membership is bumped to silver level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");


		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		String text1 = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text1.contains("400 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount2"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount2"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints2 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints2, dataSet.get("expectedPoints2"),
				"User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		TestListeners.extentTest.get()
				.pass("Verified thet User balance is correct on timeline :" + actualredeemablePoints2);

		// verify membership is bumped to gold level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership3") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership3") + " visible");

		// create checkin-3
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount3"));
		pageObj.apiUtils().verifyResponse(checkinResponse3, "POS checkin");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		String text2 = pageObj.iframeConfigurationPage().getElementText("guestTimeLine.pointsEarned", "");
		Assert.assertTrue(text2.contains("200 points earned"), "Expected points did not gifted to the user");
		logger.info("Verified user earned expected points " + dataSet.get("amount3"));
		TestListeners.extentTest.get().pass("Verified user earned expected points " + dataSet.get("amount3"));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String actualredeemablePoints3 = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints3, dataSet.get("expectedPoints3"),
				"User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		TestListeners.extentTest.get()
				.pass("Verified thet User balance is correct on timeline :" + actualredeemablePoints3);

	}

	// Rakhi
	@Test(description = "SQ-T6073 [Points Convert to Category Chosen by Guest] Verify that Reward Value is listed for the user based on the Conversion Rule", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T6073_validateRewardGiftingWithMembershipLevelPointToManual() throws Exception {

		// open instance
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// set banked reward value on business level
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().setBankedRewardValue("");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule("currency");
		pageObj.settingsPage().setConversionRuleValues("Source Value", "100");
		pageObj.settingsPage().setConversionRuleValues("Converted Value", "10");
		pageObj.settingsPage().clickSaveBtn();

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		// String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// create checkin-1
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount1"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify membership is bumped to silver level
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// Point conversion
		Response pointsResponse = pageObj.endpoints().Api2PointConversion(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 point conversion");
		Assert.assertTrue(pointsResponse.asString().contains("100 points converted into 10"),
				"Force Redemption is successful.");
		logger.info("Verified point conversion is successful");
		TestListeners.extentTest.get().pass("Verified point conversion is successful");

		// create checkin-2
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");

		// verify membership is bumped to gold level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership3") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership3") + " visible");

		// Point conversion
		Response pointsResponse1 = pageObj.endpoints().Api2PointConversion(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for api2 point conversion");
		Assert.assertTrue(pointsResponse1.asString().contains("100 points converted into 10"),
				"Force Redemption is successful.");
		logger.info("Verified point conversion is successful");
		TestListeners.extentTest.get().pass("Verified point conversion is successful");

		pageObj.guestTimelinePage().refreshTimeline();
		int totalExpectedCurrency = Integer.parseInt(dataSet.get("totalCurrency"));
		int userCurrency1 = pageObj.guestTimelinePage().getRedeemableRewardOrBankedCurrencyCount("Currency");
		Assert.assertEquals(userCurrency1, totalExpectedCurrency, "Banked currency did not matched as expected");
		logger.info("Verified that Total currency is correct on user timeline ie : " + userCurrency1);
		TestListeners.extentTest.get()
				.pass("Verified that Total currency is correct on user timeline ie : " + userCurrency1);
	}

	// Rakhi
	@Test(description = "SQ-T5404 Verify UnbankedPointRedemption with the consider_effective_redeemed_unbanked_points flag enabled", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T5404_verifyUnbankedPointRedemption() throws Exception {

		String membershipId1 = dataSet.get("membership1Id");
		String membershipId2 = dataSet.get("membership2Id");
//			String membershipId3 = dataSet.get("gold");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// open instance
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		updateMembershipLevelsDB();

		// set redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership("");
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// effective
		String query1 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "membership_level_id", 10); // dbUtils.getValueFromColumn(query2,
																														// "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId1 + "");

		// Pos api checkin of 110 points
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "120");
		Assert.assertEquals(resp1.getStatusCode(), 200, "Status code 200 did not matched for post chekin api");

		// effective
		String query2 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "membership_level_id", 10); // dbUtils.getValueFromColumn(query2,
																														// "membership_level_id");
		Assert.assertEquals(expColValue2, membershipId2, "Effective membership strategy is not " + membershipId2 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId2 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId2 + "");

		// force redemption of 30 points
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeemWithType(dataSet.get("apiKey"), userID,
				"30", "unbanked_points_redemption");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not match for force Redemption of Points");

		// effective
		String query3 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "membership_level_id", 10); // dbUtils.getValueFromColumn(query2,
																														// "membership_level_id");
		Assert.assertEquals(expColValue3, membershipId2,
				"Effective membership strategy is not " + membershipId2 + " after scheduler run");
		logger.info(
				"Verified that Effective membership strategy is " + membershipId2 + " after doing force redemption");
		TestListeners.extentTest.get().pass(
				"Verified that Effective membership strategy is " + membershipId2 + " after doing force redemption");

	}

	// Update membership levels
	public void updateMembershipLevelsDB() throws Exception {
		String membershipMinPoints, membershipMaxPoints, membershipId;
		int maxLevels = 3;
		for (int i = 1; i <= maxLevels; i++) {
			membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			membershipId = dataSet.get("membership" + i + "Id");
			String query = "UPDATE membership_levels SET min_points = $points WHERE id = $id;"
					.replace("$points", membershipMinPoints).replace("$id", membershipId);
			int result = DBUtils.executeUpdateQuery(env, query);
			Assert.assertTrue(result >= 0,  "Query execution failed for membershipId: " + membershipId);
			query = "UPDATE membership_levels SET max_points = $points WHERE id = $id;"
					.replace("$points", membershipMaxPoints).replace("$id", membershipId);
			result = DBUtils.executeUpdateQuery(env, query);
			Assert.assertTrue(result >= 0, "Query execution failed for membershipId: " + membershipId);
			utils.logit("Updated membership level: " + dataSet.get("membership" + i));
		}
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
