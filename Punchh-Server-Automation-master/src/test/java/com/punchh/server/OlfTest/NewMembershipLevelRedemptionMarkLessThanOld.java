package com.punchh.server.OlfTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
//import java.util.ArrayList;
//import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class NewMembershipLevelRedemptionMarkLessThanOld {

	private static Logger logger = LogManager.getLogger(NewMembershipLevelRedemptionMarkLessThanOld.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl, client, secret, locationKey, apiKey;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		client = "ZXiR-tQKZNAhWOR1777lajR_av_wfcnT6_O6t2p6zzs";
		secret = "QsiDmOMKXgivUEVm5fVbcs5cqmz8gRWjF-yEJdGgLe4";
		locationKey = "cc33b01d7f794e13a2d9ae1d3901af8e";
		apiKey = "6J8cAY_aJqSKxvUWjy-k";
	}

	// Rakhi
	@Test(description = "OLF-T2052 [New ML redemption mark is less than old] Verify user details in membership_level_histories table when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2052_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// previous membership level id
		String query3 = "SELECT `previous_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "'order by previous_membership_level_id desc;";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"previous_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, null, "Previous membership strategy is not null");
		logger.info("Verified that Previous membership strategy is null");
		utils.logPass("Verified that Previous membership strategy is null");

		// effective
		String query4 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " ");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + "");

		// operative
		String query5 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue3, membershipId1, "Operative membership strategy is not " + membershipId1 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId1 + "");
		utils.logPass("Verified that Operative membership strategy is " + membershipId1 + "");

	}

	// Rakhi
	@Test(description = "OLF-T2051 [New ML redemption mark is less than old] Verify PointDebits in reward_debits table when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2051_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}
		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Check PointDebit in reward_debits table
		String query3 = "Select `type` from `reward_debits` where `user_id` ='" + userID + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"type", 5);
		Assert.assertEquals(expColValue1, "PointDebit",
				"type column in reward_debits table is not equal to expected value i.e. PointDebit");
		logger.info("type column in reward_debits table is equal to expected value i.e. PointDebit");
		utils.logPass("type column in reward_debits table is not equal to expected value i.e. PointDebit");

	}

	// Rakhi
	@Test(description = "OLF-T2050 [New ML redemption mark is less than old] Verify user details in checkins table when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2050_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "80" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int expexted_points_earned = Integer.parseInt(dataSet.get("expextedPointsEarned"));
		int expexted_points_spent = Integer.parseInt(dataSet.get("expextedPointsSpent"));

		// Check points_spent
		Assert.assertEquals(points_spent, expexted_points_spent,
				"points_spent column in checkins table is not equal to expected value i.e. " + points_spent);
		logger.info("points_spent column in checkins table is equal to expected value i.e. " + points_spent);
		utils.logPass("points_spent column in checkins table is not equal to expected value i.e. " + points_spent);

		// Check points_earned
		Assert.assertEquals(points_earned, expexted_points_earned,
				"points_earned column in checkins table is not equal to expected value i.e. " + points_earned);
		logger.info("points_earned column in checkins table is equal to expected value i.e. " + points_earned);
		utils.logPass("points_earned column in checkins table is equal to expected value i.e. " + points_earned);

	}

	// Rakhi
	@Test(description = "OLF-T2049 [New ML redemption mark is less than old] Verify user details in accounts table when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2049_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Check total_credits/total_debits in accounts table
		String query5 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"total_debits", 5);
		Assert.assertEquals(expColValue3, points_spent,
				"total_debits column in accounts table is not equal to expected value i.e. 0"); // where to fetch
		// total_debits for comparison
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		utils.logPass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query6 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"total_credits", 5);
		int expColValue3No = Integer.parseInt(expColValue4.replace(".0", ""));
		Assert.assertEquals(expColValue3No, points_earned,
				"total_credits column in accounts table is not equal to expected value i.e. " + expColValue3No);
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);
		utils.logPass("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);

		// membership level
		String query7 = "SELECT `membership_level` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue5, membershipLevel1,
				"Membership level in accounts table is not " + membershipLevel1 + "");
		logger.info("Verified that membership level in accounts table is " + membershipLevel1 + "");
		utils.logPass("Verified that membership level in accounts table is " + membershipLevel1 + "");

	}

	// Rakhi
	@Test(description = "OLF-T2048 [New ML redemption mark is less than old] Verify user balance in pos/checkin API when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2048_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int bankedRewards = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.banked_rewards").toString().replace(".0", ""));
		int pointBalancePosCheckin = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		int netBalance = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		int netDebits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		String membershipLevel = checkinResponse1.jsonPath().get("balance.membership_level");
		String membershipLevelId = checkinResponse1.jsonPath().get("balance.membership_level_id").toString();

		Assert.assertEquals(membershipLevelId, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevel, membershipLevel1, "Membership level is not matching");
		Assert.assertEquals(pointBalancePosCheckin, actualPoints, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, points_spent, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(bankedRewards, 0, "Banked rewards for Pos Checkin is not matching");
		Assert.assertEquals(netBalance, actualPoints, "Net balance for Pos Checkin is not matching");
		Assert.assertEquals(netDebits, points_spent, "Net debits for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");

	}

	// Rakhi
	@Test(description = "OLF-T2047 [New ML redemption mark is less than old] Verify user balance in dashboard API when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2047_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		long phone = (long) (Math.random() * Math.pow(10, 10));

		// hit api api2/dashboard/users/info
		Response userLookUpApi = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, blankString);
		Assert.assertEquals(userLookUpApi.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		int bankedRewards = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.banked_rewards").toString().replace(".0", ""));
		int pointBalancePosCheckin = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		int netBalance = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		int netDebits = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		String membershipLevel = userLookUpApi.jsonPath().get("balance.membership_level");
		String membershipLevelId = userLookUpApi.jsonPath().get("balance.membership_level_id").toString();

		Assert.assertEquals(membershipLevelId, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevel, membershipLevel1, "Membership level is not matching");
		Assert.assertEquals(pointBalancePosCheckin, actualPoints, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, points_spent, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(bankedRewards, 0, "Banked rewards for Pos Checkin is not matching");
		Assert.assertEquals(netBalance, actualPoints, "Net balance for Pos Checkin is not matching");
		Assert.assertEquals(netDebits, points_spent, "Net debits for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");

	}

	// Rakhi
	@Test(description = "OLF-T2046 [New ML redemption mark is less than old] Verify user balance in different Auth API's when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2046_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		logger.info("Auth User Balance is successful");
		utils.logit("Auth User Balance is successful");
		int pointBalanceAccountBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		String membershipLevelName = authUserBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name");
		String membershipLevelId = authUserBalanceResponse.jsonPath().get("account_balance.current_membership_level_id")
				.toString();

		Assert.assertEquals(pointBalanceAccountBalance, actualPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance, actualPoints,
				"redeemable Points Account Balance is not matching");
		Assert.assertEquals(membershipLevelId, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevelName, membershipLevel1, "Membership level is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");

		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		utils.logPass("API v1 Fetch User Checkins Balance for user  is successful");
		logger.info("API v1 Fetch User Checkins Balance for user  is successful");

		int bankedRewards = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		int pointBalancePosCheckin = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int netBalance = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		int netDebits = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_debits").toString().replace(".0", ""));
		String membershipLevel = userCheckinsBalanceResponse.jsonPath().get("membership_level");
		String membershipLevelId1 = userCheckinsBalanceResponse.jsonPath().get("membership_level_id").toString();

		Assert.assertEquals(membershipLevelId1, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevel, membershipLevel1, "Membership level is not matching");
		Assert.assertEquals(pointBalancePosCheckin, actualPoints, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, points_spent, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(bankedRewards, actualPoints, "Banked rewards for Pos Checkin is not matching");
		Assert.assertEquals(netBalance, actualPoints, "Net balance for Pos Checkin is not matching");
		Assert.assertEquals(netDebits, points_spent, "Net debits for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");

	}

	// Rakhi
	@Test(description = "OLF-T2044 [New ML redemption mark is less than old] Verify user balance in different API2 API's when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2044_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		utils.logit("API2 User Balance is successful");
		int pointBalanceAccountBalance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		String membershipLevelName = userBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name");
		String membershipLevelId = userBalanceResponse.jsonPath().get("account_balance.current_membership_level_id")
				.toString();

		Assert.assertEquals(pointBalanceAccountBalance, actualPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance, actualPoints,
				"redeemable Points Account Balance is not matching");
		Assert.assertEquals(membershipLevelId, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevelName, membershipLevel1, "Membership level is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");

		// Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		utils.logPass("api2 account balance  is successful");
		logger.info("api2 account balance is successful");

		int pointBalanceAccountBalance1 = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance1 = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		String membershipLevelName1 = accountBalResponse.jsonPath()
				.get("account_balance_details.current_membership_level_name");
		String membershipLevelId1 = accountBalResponse.jsonPath()
				.get("account_balance_details.current_membership_level_id").toString();

		Assert.assertEquals(pointBalanceAccountBalance1, actualPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance1, actualPoints,
				"redeemable Points Account Balance is not matching");
		Assert.assertEquals(membershipLevelId1, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membershipLevelName1, membershipLevel1, "Membership level is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");

	}

	// Rakhi
	@Test(description = "OLF-T2042 [New ML redemption mark is less than old] Verify user's account history when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2042_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// verify Banking event in account history created
		List<Object> obj = new ArrayList<Object>();
		String description;
		int j = 0;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "].description");
			if (description.contains(dataSet.get("redeemableName"))) {
				i = j;
				break;
			}
		}

		String eventValue = accountHistoryResponse.jsonPath().get("[" + j + "].event_value").toString();
		Assert.assertEquals(eventValue, "+Item", "banking event in account history is not created");
		logger.info("verified that banking event in account history created");
		utils.logPass("verified that banking event in account history created");

	}

	// Rakhi
	@Test(description = "OLF-T2045 [New ML redemption mark is less than old] Verify user balance in different V1 API's when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2045_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		String points_earnedV1Checkins = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString()
				.replace("[", "").replace("]", "");
		String points_earnedV1Checkins1 = checkinsApiV1Response.jsonPath().get("[1].points_earned").toString()
				.replace("[", "").replace("]", "");
		int pointsEarnedV1CheckinsTotal = (Integer.parseInt(points_earnedV1Checkins)
				+ Integer.parseInt(points_earnedV1Checkins1));
		Assert.assertEquals(pointsEarnedV1CheckinsTotal, amt, "points earned is not equal to " + amt);
		logger.info("Verified that points earned is equal to " + amt + " for Api V1 checkins");
		utils.logPass("Verified that points earned is equal to " + amt + " for Api V1 checkins");

		int points_spentV1Checkins = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentV1Checkins1 = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentV1Checkins + points_spentV1Checkins1, points_spent,
				"points_spent is not equal to " + points_spent);
		logger.info("Verified that points_spent is equal to " + points_spent + " for Api V1 checkins");
		utils.logPass("Verified that points_spent is equal to " + points_spent + " for Api V1 checkins");

		int points_availableV1Checkins = Integer.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available")
				.toString().replace("[", "").replace("]", ""));
		int points_availableV1Checkins1 = Integer.parseInt(checkinsApiV1Response.jsonPath().get("[1].points_available")
				.toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_availableV1Checkins + points_availableV1Checkins1, actualPoints,
				"points_spent is not equal to " + actualPoints);
		logger.info("Verified that points_available is equal to " + actualPoints + " for Api V1 checkins");
		utils.logPass("Verified that points_available is equal to " + actualPoints + " for Api V1 checkins");

	}

	// Rakhi
	@Test(description = "OLF-T2043 [New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2043_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		logger.info("Secure Account Balance is successful");
		utils.logit("Secure Account Balance is successful");
		int bankedRewards = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.banked_rewards").toString().replace(".00", ""));
		int pointBalancePosCheckin = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		int netBalance = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.net_balance").toString().replace(".0", ""));
		int netDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.net_debits").toString().replace(".0", ""));
		String membership_Level1 = balance_Response.jsonPath().get("account_balance.membership_level");
		String membershipLevelId = balance_Response.jsonPath().get("account_balance.membership_level_id").toString();

		Assert.assertEquals(membershipLevelId, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membership_Level1, membershipLevel1, "Membership level is not matching");
		Assert.assertEquals(pointBalancePosCheckin, actualPoints, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, points_spent, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(bankedRewards, actualPoints, "Banked rewards for Pos Checkin is not matching");
		Assert.assertEquals(netBalance, actualPoints, "Net balance for Pos Checkin is not matching");
		Assert.assertEquals(netDebits, points_spent, "Net debits for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");

		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		utils.logPass("API v1 Fetch User Checkins Balance for user  is successful");
		logger.info("API v1 Fetch User Checkins Balance for user  is successful");

		int bankedRewards1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		int pointBalancePosCheckin1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int netBalance1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		int netDebits1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_debits").toString().replace(".0", ""));
		String membership_Level2 = userCheckinsBalanceResponse.jsonPath().get("membership_level");
		String membershipLevelId1 = userCheckinsBalanceResponse.jsonPath().get("membership_level_id").toString();

		Assert.assertEquals(membershipLevelId1, membershipId1, "Membership level id is not matching");
		Assert.assertEquals(membership_Level2, membershipLevel1, "Membership level is not matching");
		Assert.assertEquals(pointBalancePosCheckin1, actualPoints, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin1, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin1, points_spent, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(bankedRewards1, actualPoints, "Banked rewards for Pos Checkin is not matching");
		Assert.assertEquals(netBalance1, actualPoints, "Net balance for Pos Checkin is not matching");
		Assert.assertEquals(netDebits1, points_spent, "Net debits for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and banked_rewards ,membership_level, membership_level_id for Pos Checkin is matching the expected result");

		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		logger.info("Secure Api mobile checkin is successful");
		utils.logit(" Secure Api mobile checkin is successful");

		String points_earnedApiFetchCheckin = Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString()
				.replace("[", "").replace("]", "");
		String points_earnedApiFetchCheckin1 = Api1FetchCheckinResp.jsonPath().get("[1].points_earned").toString()
				.replace("[", "").replace("]", "");
		int pointsEarnedApiFetchCheckin = Integer.parseInt(points_earnedApiFetchCheckin)
				+ Integer.parseInt(points_earnedApiFetchCheckin1);
		Assert.assertEquals(pointsEarnedApiFetchCheckin, amt, "points earned is not equal to " + amt);
		logger.info("Verified that points earned is not equal to " + amt + " for Secure Api Fetch Checkin");
		utils.logPass("Verified that points earned is not equal to " + amt + " for Secure Api Fetch Checkin");

		int points_spentV1Checkins = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentV1Checkins1 = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentV1Checkins + points_spentV1Checkins1, points_spent,
				"points_spent is not equal to " + points_spent);
		logger.info("Verified that points_spent is equal to " + points_spent + " for Api V1 checkins");
		utils.logPass("Verified that points_spent is equal to " + points_spent + " for Api V1 checkins");

		int points_availableV1Checkins = Integer.parseInt(Api1FetchCheckinResp.jsonPath().get("[0].points_available")
				.toString().replace("[", "").replace("]", ""));
		int points_availableV1Checkins1 = Integer.parseInt(Api1FetchCheckinResp.jsonPath().get("[1].points_available")
				.toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_availableV1Checkins + points_availableV1Checkins1, actualPoints,
				"points_spent is not equal to " + actualPoints);
		logger.info("Verified that points_available is equal to " + actualPoints + " for Api V1 checkins");
		utils.logPass("Verified that points_available is equal to " + actualPoints + " for Api V1 checkins");

	}

	// Rakhi
	@Test(description = "OLF-T2041 [New ML redemption mark is less than old] Verify user's timeline / pie chart when business configuration has membership level Redemption Mark configured [User case 1]"
			+ "OLF-T2040 [New ML redemption mark is less than old] Verify user's print timeline when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2041_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actual_points = (points_earned - (points_spent + points_expired));

		// Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		String loyaltypieChartPoints = pageObj.guestTimelinePage().getLoyaltyPieChartDetails();
		Assert.assertEquals(actual_points, actualredeemablePoints, "User balance is not correct on timeline");
		Assert.assertEquals(loyaltypieChartPoints, String.valueOf(points_earned),
				"loyalty pie chart points balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline :" + actualredeemablePoints);

		// Check print timeline for user
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains(0 + ".0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains(actual_points + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + actual_points),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

	}

	// Rakhi
	@Test(description = "OLF-T2038 [New ML redemption mark is less than old] Verify point balance in iframe for users when business configuration has membership level Redemption Mark configured [User case 1]")
	public void T2038_VerifyNewMLredemptionMarkLessThanPreviousMark() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String membershipLevel1 = dataSet.get("membership1");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag1"));
		// Checking and Marking "track_points_spent" -> true in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag2"));
		// Checking and Marking "enable_transaction_based_display_account_history" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", dataSet.get("dbFlag3"));
		// Checking and Marking "enable_staged_rewards" -> false in business.preference
		// table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));
		// Checking and Marking "enable_points_spent_backfill" ->false in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("points"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// edit membership levels redemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(5);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " dollar");
		utils.logPass("POS checkin is successful for " + "3" + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query2 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actual_points = (points_earned - (points_spent + points_expired));

		// User balance validation on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 1);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		String membershipLevel = pageObj.iframeSingUpValidationPageClass().getMembershiplevel();
		Assert.assertEquals(Integer.parseInt(iFrameActualpoints), actual_points,
				"Current Points for Iframe is not matching");
		Assert.assertEquals(membershipLevel, membershipLevel1, "Membership Levelfor Iframe is not matching");
		utils.logPass("Iframe points and membership level validated");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
