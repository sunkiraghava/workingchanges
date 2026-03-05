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
public class MembershipCalculationPURTest {
	private static Logger logger = LogManager.getLogger(MembershipCalculationPURTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env, baseUrl, businessesQuery, businessId, oneYearAgoDate, oneYearAgoYesterdayDate,
			todaysDate, yesterdayDate, twoDaysAgoDate, currentDay, currentMonth;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	boolean flag1, flag2, flag3, flag4;

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
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
		todaysDate = CreateDateTime.getCurrentDate();
		yesterdayDate = CreateDateTime.getYesterdayDays(1);
		twoDaysAgoDate = CreateDateTime.getYesterdayDateTimeUTC(2);
		oneYearAgoDate = CreateDateTime.previousYearDate(1);
		oneYearAgoYesterdayDate = CreateDateTime.getNyearAgoYesterdayDate(1);
		currentDay = CreateDateTime.getCurrentDateOnly();
		currentMonth = CreateDateTime.getCurrentMonthOnly();
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6624 Verify gifted points are included in membership calculation when flag is unchecked and exclude_gifted_points_from_tier_progression is true")
	@Owner(name = "Rakhi Rawat")
	public void T6624_VerifyGiftedPointsWhenExcludeGiftedPointsFromTierProgressionTrue() throws Exception {

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
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
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify user should be bronze level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership1") + " visible");
		// navigate to send message
		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		String checkBox1 = pageObj.dashboardpage()
				.checkBoxResponse("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertEquals(checkBox1, "true", "exclude_from_membership_points flag is not by default checked");
		logger.info("exclude_from_membership_points flag is by default checked");
		TestListeners.extentTest.get().info("exclude_from_membership_points flag is by default checked");

		// uncheck exclude from membership flag and gift 205 points
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression", "uncheck");
		pageObj.guestTimelinePage().messagePointsToUserNew(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		// verify user should be silver level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// check membership_level_histories for new entry
		String query1 = "SELECT count(*) as total from `membership_level_histories` where `user_id` = '" + userID
				+ "';";
		int expColValue1 = DBUtils.executeQueryAndGetCount(env, query1);
		Assert.assertEquals(expColValue1, 2, "Membership level history is not updated after gifting points to user");
		logger.info("Verified that membership level history is updated after gifting points to user");
		TestListeners.extentTest.get()
				.pass("Verified that membership level history is updated after gifting points to user");

		// effective
		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		boolean expColValue_flag = DBUtils.verifyValueFromDBUsingPolling(env, query, "membership_level_id",
				membershipId2); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue(expColValue_flag,
				"Effective membership strategy is not " + membershipId2 + " after gifting points to user");
		logger.info(
				"Verified that Effective membership strategy is " + membershipId2 + " after gifting points to user");
		TestListeners.extentTest.get().pass(
				"Verified that Effective membership strategy is " + membershipId2 + " after gifting points to user");

		// verify exclude_from_membership_points column
		String query2 = "SELECT `exclude_from_membership_points` from `checkins` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"exclude_from_membership_points", 10);
		Assert.assertNull(expColValue2, "exclude_from_membership_points column is not null");
		logger.info("Verified exclude_from_membership_points column is null");
		TestListeners.extentTest.get().pass("Verified exclude_from_membership_points column is null");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6623 Verify gifted points are not included in membership calculation when flag is checked and exclude_gifted_points_from_tier_progression is true membership level is not enabled")
	@Owner(name = "Rakhi Rawat")
	public void T6623_VerifyGiftedPointsWhenMembershipLevelDisabled() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// navigate to send message
		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		// verify presence of exclude from membership flag
		Boolean flag = pageObj.dashboardpage()
				.flagPresentorNot("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertFalse(flag,
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is presnt after disabling membership level");
		logger.info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not presnt after disabling membership level");
		TestListeners.extentTest.get().pass(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not presnt after disabling membership level");

		// gift 205 points
		pageObj.guestTimelinePage().messagePointsToUserNew(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		// check user membership level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertFalse(val2, "Membership level is visible for the user afer gifting points on guest timeline");
		logger.info(
				"Verified there should be no membership level defined when exclude_from_membership_points flag is checked");
		TestListeners.extentTest.get().pass(
				"Verified there should be no membership level defined when exclude_from_membership_points flag is checked");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6622 Verify gifted points are included in membership calculation when exclude_gifted_points_from_tier_progression is false"
			+ "SQ-T6729 Verify value of exclude_from_membership_points column for send message gift checkin [exclude_gifted_points_from_tier_progression -> false and ML true in business]")
	@Owner(name = "Rakhi Rawat")
	public void T6622_VerifyGiftedPointsWhenExcludeGiftedPointsFromTierProgressionFalse() throws Exception {

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to false
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
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
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify user should be bronze level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership1") + " visible");
		// navigate to send message
		pageObj.guestTimelinePage().clickOnMessageGiftBtn();

		// verify presence of exclude from membership flag on guest timeline
		Boolean flag = pageObj.dashboardpage()
				.flagPresentorNot("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertFalse(flag,
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is present after disabling "
						+ dataSet.get("dbFlag3"));
		logger.info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not present after disabling "
						+ dataSet.get("dbFlag3"));
		TestListeners.extentTest.get().info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not present after disabling "
						+ dataSet.get("dbFlag3"));
		// gift 205 points
		pageObj.guestTimelinePage().messagePointsToUserNew(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		// verify user should be silver level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership2") + " visible");

		// check membership_level_histories for new entry
		String query1 = "SELECT count(*) as total from `membership_level_histories` where `user_id` = '" + userID
				+ "';";
		int expColValue1 = DBUtils.executeQueryAndGetCount(env, query1);
		Assert.assertEquals(expColValue1, 2, "Membership level history is not updated after gifting points to user");
		logger.info("Verified that membership level history is updated after gifting points to user");
		TestListeners.extentTest.get()
				.pass("Verified that membership level history is updated after gifting points to user");

		// effective
		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		boolean expColValue_flag = DBUtils.verifyValueFromDBUsingPolling(env, query, "membership_level_id",
				membershipId2); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue(expColValue_flag,
				"Effective membership strategy is not " + membershipId2 + " after gifting points to user");
		logger.info(
				"Verified that Effective membership strategy is " + membershipId2 + " after gifting points to user");
		TestListeners.extentTest.get().pass(
				"Verified that Effective membership strategy is " + membershipId2 + " after gifting points to user");

		// verify exclude_from_membership_points column
		String query2 = "SELECT `exclude_from_membership_points` from `checkins` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"exclude_from_membership_points", 10);
		Assert.assertNull(expColValue2, "exclude_from_membership_points column value is not Null");
		logger.info(
				"Verified exclude_from_membership_points column value is Null in checkins table when exclude_gifted_points_from_tier_progression is false");
		TestListeners.extentTest.get().pass(
				"Verified exclude_from_membership_points column value is Null in checkins table when exclude_gifted_points_from_tier_progression is false");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6621 Verify gifted points are not included in membership calculation when flag is checked and exclude_gifted_points_from_tier_progression is true")
	@Owner(name = "Rakhi Rawat")
	public void T6621_VerifyGiftedPointsWhenMembershipLevelEnabled() throws Exception {

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set enable_account_summary to false
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
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

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify user should be bronze level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership1") + " visible");
		// navigate to send message
		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		String checkBox1 = pageObj.dashboardpage()
				.checkBoxResponse("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertEquals(checkBox1, "true", "exclude_from_membership_points flag is not by default checked");
		logger.info("exclude_from_membership_points flag is by default checked");
		TestListeners.extentTest.get().info("exclude_from_membership_points flag is by default checked");

		// keep exclude from membership flag checked and gift 205 points
		pageObj.guestTimelinePage().messagePointsToUserNew(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// verify user should be silver level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified user is on same Membership level --" + dataSet.get("membership1"));
		TestListeners.extentTest.get()
				.pass("Verified user is on same Membership level --" + dataSet.get("membership1"));

		// check membership_level_histories for new entry
		String query1 = "SELECT count(*) as total from `membership_level_histories` where `user_id` = '" + userID
				+ "';";
		int expColValue1 = DBUtils.executeQueryAndGetCount(env, query1);
		Assert.assertEquals(expColValue1, 1, "Membership level history is updated after gifting points to user");
		logger.info("Verified that no new entry created for membership level history after gifting points to user");
		TestListeners.extentTest.get()
				.pass("Verified that no new entry created for membership level history after gifting points to user");

		// effective
		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		boolean expColValue_flag = DBUtils.verifyValueFromDBUsingPolling(env, query, "membership_level_id",
				membershipId1); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue(expColValue_flag,
				"Effective membership strategy is not " + membershipId1 + " after gifting points to user");
		logger.info(
				"Verified that Effective membership strategy is " + membershipId1 + " after gifting points to user");
		TestListeners.extentTest.get().pass(
				"Verified that Effective membership strategy is " + membershipId1 + " after gifting points to user");

		// verify exclude_from_membership_points column
		String query2 = "SELECT `exclude_from_membership_points` from `checkins` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"exclude_from_membership_points", 10);
		Assert.assertTrue(expColValue2.isEmpty(), "exclude_from_membership_points column is not blank");
		logger.info("Verified exclude_from_membership_points column is blank");
		TestListeners.extentTest.get().pass("Verified exclude_from_membership_points column is blank");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6642 Verify decouple expiry with membership level bump when flag is enabled and exclude_from_membership_points is true")
	@Owner(name = "Rakhi Rawat")
	public void T6642_VerifyDecoupleExpiryWhenMembershipLevelBumpEnabled() throws Exception {

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set enable_account_summary to false
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
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
		// set Checkin expiry
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
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

		// set Base Conversion Rate to Points configuration
		pageObj.earningPage().navigateToEarningTabs("Checkin Earning");
		pageObj.earningPage().setBaseConversionRate("1.0");
		pageObj.earningPage().clickUpdateBtn();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify user should be bronze level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership1") + " visible");

		// checkin of 220 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify user should be silver level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified membership level bumped to --" + dataSet.get("membership2"));
		TestListeners.extentTest.get().pass("Verified membership level bumped to --" + dataSet.get("membership2"));

		// keep exclude from membership flag checked and gift 200 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// verify membership level should not be bumped
		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified membership level did not bump to --" + dataSet.get("membership2"));
		TestListeners.extentTest.get()
				.pass("Verified membership level did not bump to --" + dataSet.get("membership2"));

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
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		String membershipLevelId = dataSet.get("membershipId2");
		String query4 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + todaysDate
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		DBUtils.executeUpdateQuery(env, query4);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(12);

		// Verify effective membership level as {Bronze}
		String query5 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc limit 1;";
		String expColValue_flag = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "membership_level_id",
				10);
		Assert.assertTrue(expColValue_flag.equals(membershipId1) || expColValue_flag.equals(membershipId2),
				"Expected membership to be " + membershipId1 + " or " + membershipId2 + " but found "
						+ expColValue_flag);
		logger.info("Verified that Effective membership level is " + membershipId1
				+ " after running decouple rolling expiry");
		TestListeners.extentTest.get().pass("Verified that Effective membership level is " + membershipId1
				+ " after running decouple rolling expiry");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6644 Verify gifted points are excluded when flag is enabled and exclude_from_membership_points is true and migration strategy is include with original and initial points")
	@Owner(name = "Rakhi Rawat")
	public void T6644_VerifyGiftedPointsWhenMigrationStrategyInclude() throws Exception {
		String b_id = dataSet.get("business_id");

		String query = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		logger.info(query);
		TestListeners.extentTest.get().info(query);
		int rs = DBUtils.executeUpdateQuery(env, query);

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set enable_account_summary to false
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 2);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
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
		// Create Business Migration User
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "10", "200");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// keep exclude from membership flag checked and gift 205 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// verify membership level should not be bumped
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified user is on same Membership level and not bumped --" + dataSet.get("membership1"));
		TestListeners.extentTest.get()
				.pass("Verified user is on same Membership level and not bumped --" + dataSet.get("membership1"));

	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = " SQ-T6645 Verify gifted points are excluded when flag is enabled and exclude_from_membership_points is true and migration strategy is exclude with original and initial points")
	@Owner(name = "Rakhi Rawat")
	public void T6645_VerifyGiftedPointsWhenMigrationStrategyExclude() throws Exception {
		String b_id = dataSet.get("business_id");

		String query = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		logger.info(query);
		TestListeners.extentTest.get().info(query);
		int rs = DBUtils.executeUpdateQuery(env, query);

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set enable_account_summary to false
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Exclude Modulus Of Initial Points From Earning", 2);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
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

		// Create Business Migration User
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "10", "200");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// keep exclude from membership flag checked and gift 205 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// verify membership level should not be bumped
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified user is on same Membership level and not bumped --" + dataSet.get("membership2"));
		TestListeners.extentTest.get()
				.pass("Verified user is on same Membership level and not bumped --" + dataSet.get("membership2"));

	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6701 Verify Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox is available on existing gaming level page [exclude_gifted_points_from_tier_progression and ML true in business]"
			+ "SQ-T6702 Verify default value of Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox in gaming level [exclude_gifted_points_from_tier_progression and ML true in business]"
			+ "SQ-T6705 Verify value of exclude_from_membership_points column when Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox is true in gaming level [exclude_gifted_points_from_tier_progression and ML true in business]")
	@Owner(name = "Rakhi Rawat")
	public void T6701_VerifyExcludeGiftedPointFromMembershipFlagOnGamingLevelPage() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Dashboard and enable Has Games?, Has Scratch Game?,
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_cataboom_integration", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_games", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_scratch_game", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_slot_machine_game", "check");
		pageObj.dashboardpage().updateButton();

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		// navigate to gaming level
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().clickOnGamingLevel(dataSet.get("gamingLevelName"));
		// Verify Exclude Gifted Point From Membership Tier Evaluation And Progression
		// flag presence
		boolean flag = pageObj.dashboardpage()
				.isElementDisplayed("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertTrue(flag,
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not displayed on Gaming Level Page");
		logger.info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is displayed on Gaming Level Page");
		TestListeners.extentTest.get().pass(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is displayed on Gaming Level Page");

		// Verify Exclude Gifted Point From Membership Tier Evaluation And Progression
		// flag is by default checked
		String checkBox1 = pageObj.dashboardpage()
				.checkBoxResponse("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertEquals(checkBox1, "true", "exclude_from_membership_points flag is not by default checked");
		logger.info("exclude_from_membership_points flag is by default checked on Gaming Level Page");
		TestListeners.extentTest.get()
				.pass("exclude_from_membership_points flag is by default checked on Gaming Level Page");

		// SQ-T6705 starting from here
		// navigate to gaming level
		String gamingLevelName = "AutomationGamingLevel" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().createGamingLevel(dataSet.get("game"), dataSet.get("kind"), gamingLevelName);
		// keep Exclude Gifted Point From Membership Tier Evaluation And Progression
		// checked
		pageObj.oAuthAppPage().clickSave();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message did not displayed....");
		logger.info("Gaming level created successfully");
		TestListeners.extentTest.get().pass("Gaming level created successfully");

		// fetch gaming level id
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().clickOnGamingLevel(gamingLevelName);
		String gaminglevelId = pageObj.gamingLevelPage().getGamingLevelId();
		// verify of exclude_from_membership_points in the gaming_levels created
		String query = "SELECT exclude_from_membership_points from gaming_levels where id = '" + gaminglevelId + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"exclude_from_membership_points", 10);
		Assert.assertEquals(expColValue1, "1", "exclude_from_membership_points value is not 1");
		logger.info("Verified that exclude_from_membership_points value is " + expColValue1 + " for gaming level id "
				+ gaminglevelId);
		TestListeners.extentTest.get().pass("Verified that exclude_from_membership_points value is " + expColValue1
				+ " for gaming level id " + gaminglevelId);
		// delete gaming level
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().deleteGaminglevel(gamingLevelName);
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6703 Verify Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox is not available in new gaming level [exclude_gifted_points_from_tier_progression true and ML false in business]"
			+ "SQ-T6706 Verify value of exclude_from_membership_points column when Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox is false in gaming_level [exclude_gifted_points_from_tier_progression and ML true in business]"
			+ "SQ-T6704 Verify Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox is not available in new gaming level [exclude_gifted_points_from_tier_progression-> false and ML true in business]")
	@Owner(name = "Rakhi Rawat")
	public void T6703_VerifyExcludeGiftedPointFlagOnGamingLevelWhenFlagDisabled() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Dashboard and enable Has Games?, Has Scratch Game?,
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_cataboom_integration", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_games", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_scratch_game", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_slot_machine_game", "check");
		pageObj.dashboardpage().updateButton();

		// disable membership levels
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// navigate to gaming level
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().clickOnGamingLevel(dataSet.get("gamingLevelName"));
		// Verify Exclude Gifted Point From Membership Tier Evaluation And Progression
		// flag presence when membership is disabled
		boolean flag = pageObj.dashboardpage()
				.isElementDisplayed("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertFalse(flag,
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is displayed on Gaming Level Page when membership is disabled");
		logger.info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not displayed on Gaming Level Page when membership is disabled");
		TestListeners.extentTest.get().pass(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not displayed on Gaming Level Page when membership is disabled");

		// SQ-T6706 part starting from here
		// enable membership levels
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// navigate to gaming level
		String gamingLevelName = "AutomationGamingLevel" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().createGamingLevel(dataSet.get("game"), dataSet.get("kind"), gamingLevelName);
		// uncheck exclude from membership flag
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression", "uncheck");
		pageObj.oAuthAppPage().clickSave();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message did not displayed....");
		logger.info("Gaming level created successfully");
		TestListeners.extentTest.get().pass("Gaming level created successfully");

		// fetch gaming level id
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().clickOnGamingLevel(gamingLevelName);
		String gaminglevelId = pageObj.gamingLevelPage().getGamingLevelId();
		// verify of exclude_from_membership_points in the gaming_levels created
		String query = "SELECT exclude_from_membership_points from gaming_levels where id = '" + gaminglevelId + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"exclude_from_membership_points", 10);
		Assert.assertEquals(expColValue1, "0", "exclude_from_membership_points value is not 0");
		logger.info("Verified that exclude_from_membership_points value is " + expColValue1 + " for gaming level id "
				+ gaminglevelId);
		TestListeners.extentTest.get().pass("Verified that exclude_from_membership_points value is " + expColValue1
				+ " for gaming level id " + gaminglevelId);
		// delete gaming level
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().deleteGaminglevel(gamingLevelName);

		// SQ-T6704 part
		// Set exclude_gifted_points_from_tier_progression to false
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to false");
		logger.info("exclude_gifted_points_from_tier_progression flags is updated to false");
		TestListeners.extentTest.get().info("exclude_gifted_points_from_tier_progression flags is updated to false");

//		// enable membership levels
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
//		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
//		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// navigate to gaming level
		pageObj.menupage().navigateToSubMenuItem("Custom", "Gaming Levels");
		pageObj.gamingLevelPage().clickOnGamingLevel(dataSet.get("gamingLevelName"));
		// Verify Exclude Gifted Point From Membership Tier Evaluation And Progression
		// flag presence when membership is enabled and
		// exclude_gifted_points_from_tier_progression is false
		boolean flag1 = pageObj.dashboardpage()
				.isElementDisplayed("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertFalse(flag1,
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is displayed on Gaming Level Page when membership is enabled and exclude_gifted_points_from_tier_progression is false");
		logger.info(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not displayed on Gaming Level Page when membership is enabled and exclude_gifted_points_from_tier_progression is false");
		TestListeners.extentTest.get().pass(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression flag is not displayed on Gaming Level Page when membership is enabled and exclude_gifted_points_from_tier_progression is false");
	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6710 Verify value of exclude_from_membership_points column for campaign point gifting [exclude_gifted_points_from_tier_progression and ML true in business]")
	@Owner(name = "Rakhi Rawat")
	public void T6710_VerifyValueOfExcludeFromMembershipColumnForPostChekinCampaign() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		// activate Post checkin campaign
		String query = "UPDATE `free_punchh_campaigns` SET `status` = 'active' WHERE `id` = '" + dataSet.get("campId")
				+ "' ;";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "free_punchh_campaigns table query is not working");
		logger.info("Campaign status set to active successfully");
		TestListeners.extentTest.get().info("Campaign status set to active successfully");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// checkin of 205 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify campaign gets triggered
		String rewardGiftedAccountHistory1 = pageObj.guestTimelinePage().getUserAccountHistory(dataSet.get("campaign"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(
				rewardGiftedAccountHistory1
						.contains("10 Bonus punches earned for participating in " + dataSet.get("campaign")),
				"Points not gifted to user from campaign " + dataSet.get("campaign"));
		logger.info("Verified campaign is triggered and points are gifted to user from campaign "
				+ dataSet.get("campaign"));
		TestListeners.extentTest.get()
				.pass("Verified campaign is triggered and points are gifted to user from campaign "
						+ dataSet.get("campaign"));

		// verify exclude_from_membership_points column
		String query2 = "SELECT `exclude_from_membership_points` from `checkins` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"exclude_from_membership_points", 10);
		Assert.assertTrue(expColValue2.isEmpty(), "exclude_from_membership_points column is not empty");
		logger.info("Verified exclude_from_membership_points column is empty");
		TestListeners.extentTest.get().pass("Verified exclude_from_membership_points column is empty");

	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6728 Verify Exclude Gifted Point From Membership Tier Evaluation And Progression checkbox has no impact on gifting via Message/Gift section when Gift Type is other than points [exclude_gifted_points_from_tier_progression and ML true in business]"
			+ "SQ-T6711 Verify value of exclude_from_membership_points column for POS checkin [exclude_gifted_points_from_tier_progression and ML true in business]")
	@Owner(name = "Rakhi Rawat")
	public void T6728_VerifyExcludeGiftedPointsFromTierProgressionHasNoImpactOnGifting() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// SQ-T6711 part starting from here
		// checkin of 220 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("points"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify exclude_from_membership_points column
		String query2 = "SELECT `exclude_from_membership_points` from `checkins` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"exclude_from_membership_points", 10);
		Assert.assertNull(expColValue2, "exclude_from_membership_points column is not NULL");
		logger.info("Verified exclude_from_membership_points column is NULL");
		TestListeners.extentTest.get().pass("Verified exclude_from_membership_points column is NULL");

		// SQ-T6728 part starting from here
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// navigate to send message
		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		String checkBox1 = pageObj.dashboardpage()
				.checkBoxResponse("Exclude Gifted Point From Membership Tier Evaluation And Progression");
		Assert.assertEquals(checkBox1, "true", "exclude_from_membership_points flag is not checked");

		// uncheck exclude from membership flag and gift Reward Amount
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression", "uncheck");
		// gift currency
		pageObj.guestTimelinePage().messageRewardAmountOrRedeemableToUserNew(dataSet.get("subject"), "Reward Amount",
				dataSet.get("giftReason"), "", dataSet.get("amount"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");
		logger.info("Reward Amount gifted to the user successfully when exclude_from_membership_points is unchecked");
		TestListeners.extentTest.get()
				.pass("Reward Amount gifted to the user successfully when exclude_from_membership_points is unchecked");

		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		// keep exclude from membership flag checked and gift Reward Amount
		pageObj.guestTimelinePage().messageRewardAmountOrRedeemableToUserNew(dataSet.get("subject"), "Reward Amount",
				dataSet.get("giftReason"), "", dataSet.get("amount"));
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Message sent did not displayed on timeline");
		logger.info("Reward Amount gifted to the user successfully when exclude_from_membership_points is checked");
		TestListeners.extentTest.get()
				.pass("Reward Amount gifted to the user successfully when exclude_from_membership_points is checked");

		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		// uncheck exclude from membership flag and gift redeemable
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
				"Exclude Gifted Point From Membership Tier Evaluation And Progression", "uncheck");
		pageObj.guestTimelinePage().messageRewardAmountOrRedeemableToUserNew(dataSet.get("subject"), "Redeemable",
				dataSet.get("giftReason"), dataSet.get("redeemableName"), "");

		boolean status3 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status3, "Message sent did not displayed on timeline");
		logger.info("Redeemable gifted to the user successfully when exclude_from_membership_points is unchecked");
		TestListeners.extentTest.get()
				.pass("Redeemable gifted to the user successfully when exclude_from_membership_points is unchecked");

		pageObj.guestTimelinePage().clickOnMessageGiftBtn();
		// keep exclude from membership flag checked and gift redeemable
		pageObj.guestTimelinePage().messageRewardAmountOrRedeemableToUserNew(dataSet.get("subject"), "Redeemable",
				dataSet.get("giftReason"), dataSet.get("redeemableName"), "");

		boolean status4 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status4, "Message sent did not displayed on timeline");
		logger.info("Redeemable gifted to the user successfully when exclude_from_membership_points is checked");
		TestListeners.extentTest.get()
				.pass("Redeemable gifted to the user successfully when exclude_from_membership_points is checked");

	}

	// Rakhi
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6643 Verify membership level bump anniversary expiry when flag is enabled and exclude_from_membership_points is true")
	@Owner(name = "Rakhi Rawat")
	public void T6643_VerifyMembershipAnniversaryExpiryWhenMembershipLevelBumpEnabled() throws Exception {

		String membershipId1 = dataSet.get("membershipId1");
		String membershipId2 = dataSet.get("membershipId2");

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_expiry_unification to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_points_spent_backfill to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set exclude_gifted_points_from_tier_progression to true
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		// Set enable_account_summary to false
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag4") + " value is not updated to false");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "2000", "2500");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
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
		// Update Checkin Expiry configs
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
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

		// set Base Conversion Rate to Points configuration
		pageObj.earningPage().navigateToEarningTabs("Checkin Earning");
		pageObj.earningPage().setBaseConversionRate("1.0");
		pageObj.earningPage().clickUpdateBtn();

		// Create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify user should be bronze level
		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("membership1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("membership1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("membership1") + " visible");

		// checkin of 220 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// verify user should be silver level
		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified membership level bumped to --" + dataSet.get("membership2"));
		TestListeners.extentTest.get().pass("Verified membership level bumped to --" + dataSet.get("membership2"));

		// keep exclude from membership flag checked and gift 200 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// verify membership level should not be bumped
		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("membership2"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("membership2") + " not visible");
		logger.info("Verified membership level did not bump to --" + dataSet.get("membership2"));
		TestListeners.extentTest.get()
				.pass("Verified membership level did not bump to --" + dataSet.get("membership2"));

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
		// changing the dates in membership_level_histories table
		String query3 = "UPDATE `membership_level_histories` SET `created_at` = '" + oneYearAgoDate
				+ "', `updated_at` = '" + oneYearAgoDate + "' WHERE `user_id` = '" + userID + "';";
		DBUtils.executeUpdateQuery(env, query3);
		String membershipLevelId = dataSet.get("membershipId2");
		String query4 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + todaysDate
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		DBUtils.executeUpdateQuery(env, query4);

		// Run Rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();
		utils.longWaitInSeconds(8);

		// Verify effective membership level as {Bronze}
		String query5 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "membership_level_id", 10);
		Assert.assertEquals(expColValue7, membershipId1,
				"Effective membership level is not " + membershipId1 + " after running decouple rolling expiry");
		logger.info("Verified that Effective membership level is " + membershipId1
				+ " after running decouple rolling expiry");
		TestListeners.extentTest.get().pass("Verified that Effective membership level is " + membershipId1
				+ " after running decouple rolling expiry");

		// Verify operative membership level as {Silver}
		String query6 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"operative_membership_level_id", 10);
		Assert.assertEquals(expColValue8, membershipId2,
				"Effective membership level is not " + membershipId2 + " after running decouple rolling expiry");
		logger.info("Verified that Operative membership level is " + membershipId2
				+ " after running decouple rolling expiry");
		TestListeners.extentTest.get().pass("Verified that Operative membership level is " + membershipId2
				+ " after running decouple rolling expiry");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		if (sTCName.contains("T6710")) {
			// deactivate Post checkin campaign
			String query1 = "UPDATE `free_punchh_campaigns` SET `status` = 'inactive' WHERE `id` = '"
					+ dataSet.get("campId") + "' ;";
			pageObj.singletonDBUtilsObj();
			int rs1 = DBUtils.executeUpdateQuery(env, query1);
			Assert.assertEquals(rs1, 1, "free_punchh_campaigns table query is not working");
			utils.logit("Campaign '" + dataSet.get("campId") + "' status set to inactive successfully");
		}
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}

}
