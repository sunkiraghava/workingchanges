package com.punchh.server.OlfTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class UserCase2Test {
	private static Logger logger = LogManager.getLogger(UserCase2Test.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl, client, secret, apiKey, locationKey;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String blankString = "";

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
		apiKey = "6J8cAY_aJqSKxvUWjy-k";
		locationKey = "cc33b01d7f794e13a2d9ae1d3901af8e";
	}

	@Test(description = "OLF-T2065 [New ML redemption mark is less than old] Verify user details in membership_level_histories table when business configuration has membership level Redemption Mark configured [User case 2]")
	public void T2065_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

//        // update redeemption mark
//        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
//        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
//        pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedemptionMark1"));
//        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

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

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");

		// previous membership level
		String query3 = "SELECT `previous_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "'order by previous_membership_level_id desc;";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"previous_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, previousMembershipId, "Previous membership strategy is not null");
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

	@Test(description = "OLF-T2064 [New ML redemption mark is less than old] Verify PointDebits in reward_debits table when business configuration has membership level Redemption Mark configured [User case 2]")
	public void T2064_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

//        // update redeemption mark
//        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
//        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
//        pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedemptionMark1"));
//        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

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

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");

		// Check PointDebit in reward_debits table
		String query6 = "Select `status` from `reward_debits` where `user_id` ='" + userID + "'order by id desc;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"status", 5);
		Assert.assertEquals(expColValue7, "banked",
				"status column in reward_debits table is not equal to expected value i.e. banked");
		logger.info("status column in reward_debits table is equal to expected value i.e. banked");
		utils.logPass("status column in reward_debits table is equal to expected value i.e. banked");

	}

	@Test(description = "OLF-T2063 [New ML redemption mark is less than old] Verify user details in checkins table when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2063_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");

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
		int expected_points_earned = Integer.parseInt(dataSet.get("expected_points_earned"));
		int expected_points_spent = Integer.parseInt(dataSet.get("expected_points_spent"));

		// Check points_earned in checkins table
		Assert.assertEquals(points_earned, expected_points_earned, "Expected points earned does not match in db");
		logger.info("Expected points_earned verified from db");
		utils.logPass("Expected points_earned verified from db");

		// Check points_spent in checkins table
		Assert.assertEquals(points_spent, expected_points_spent, "Expected points spent does not match in db");
		logger.info("Expected points_spent verified from db");
		utils.logPass("Expected points_spent verified from db");
	}

	@Test(description = "OLF-T2062 [New ML redemption mark is less than old] Verify user details in accounts table when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2062_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");

		// checking value for user in accounts table
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String query1 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String total_credits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"total_credits", 5);
		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String total_debits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"total_debits", 5);
		String query3 = "Select `membership_level` from `accounts` where `user_id` = '" + userID + "';";
		String membership_level = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"membership_level", 5);
		float total_credit = Float.parseFloat(total_credits);
		float total_debit = Float.parseFloat(total_debits);
		float expected_total_credits = Float.parseFloat(dataSet.get("expected_total_credits"));
		float expected_total_debits = Float.parseFloat(dataSet.get("expected_total_debits"));
		String expected_membership_level = dataSet.get("expected_membership_level");

		// Check total_credit in accounts table
		Assert.assertEquals(total_credit, expected_total_credits, "Expected total_credits does not match in db");
		logger.info("Expected total_credits verified from db");
		utils.logPass("Expected total_credits verified from db");

		// Check total_debit in accounts table
		Assert.assertEquals(total_debit, expected_total_debits, "Expected total_debits does not match in db");
		logger.info("Expected total_debit verified from db");
		utils.logPass("Expected total_debit verified from db");

		// Check membership_level in accounts table
		Assert.assertEquals(membership_level, expected_membership_level,
				"Expected membership_level does not match in db");
		logger.info("Expected membership_level verified from db");
		utils.logPass("Expected membership_level verified from db");
	}

	@Test(description = "OLF-T2061 [New ML redemption mark is less than old] Verify user balance in pos/checkin API when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2061_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}
		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));

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
		int expected_net_debit = amt - actualPoints;
		int banked_rewards = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.banked_rewards").toString().replace(".0", ""));
		String membership_level = checkinResponse1.jsonPath().get("balance.membership_level").toString();
		String membership_level_id = checkinResponse1.jsonPath().get("balance.membership_level_id").toString();
		int net_balance = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		int net_debits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		int points_balance = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int total_credits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int total_debits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		Assert.assertEquals(banked_rewards, actualPoints, "Banked rewards doesn't match");
		Assert.assertEquals(membership_level, dataSet.get("membership_level"),
				"Membership Level rewards doesn't match");
		Assert.assertEquals(membership_level_id, dataSet.get("membership_level_id"),
				"Membership Level ID rewards doesn't match");
		Assert.assertEquals(net_balance, actualPoints, "Net Balance doesn't match");
		Assert.assertEquals(net_debits, expected_net_debit, "Net Debits doesn't match");
		Assert.assertEquals(points_balance, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(total_credits, amt, "Total Credits doesn't match");
		Assert.assertEquals(total_debits, expected_net_debit, "Total Debits doesn't match");

		logger.info("Response of POS Checkin API has been validated");
		utils.logPass("Response of POS Checkin API has been validated");
	}

	@Test(description = "OLF-T2060 [New ML redemption mark is less than old] Verify user balance in dashboard API when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2060_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;

		long phone = (long) (Math.random() * Math.pow(10, 10));

		// hit api api2/dashboard/users/info
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, blankString);
		Assert.assertEquals(userLookUpApi2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		int banked_rewards = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.banked_rewards").toString().replace(".0", ""));
		String membership_level = userLookUpApi2.jsonPath().get("balance.membership_level").toString();
		String membership_level_id = userLookUpApi2.jsonPath().get("balance.membership_level_id").toString();
		int net_balance = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		int net_debits = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		int points_balance = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int total_credits = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int total_debits = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		Assert.assertEquals(banked_rewards, actualPoints, "Banked rewards doesn't match");
		Assert.assertEquals(membership_level, dataSet.get("membership_level"),
				"Membership Level rewards doesn't match");
		Assert.assertEquals(membership_level_id, dataSet.get("membership_level_id"),
				"Membership Level ID rewards doesn't match");
		Assert.assertEquals(net_balance, actualPoints, "Net Balance doesn't match");
		Assert.assertEquals(net_debits, expected_net_debit, "Net Debits doesn't match");
		Assert.assertEquals(points_balance, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(total_credits, amt, "Total Credits doesn't match");
		Assert.assertEquals(total_debits, expected_net_debit, "Total Debits doesn't match");

		logger.info("Response of POS Checkin API has been validated");
		utils.logPass("Response of POS Checkin API has been validated");
	}

	@Test(description = "OLF-T2059 [New ML redemption mark is less than old] Verify user balance in different Auth API's when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2059_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;

		long phone = (long) (Math.random() * Math.pow(10, 10));

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		logger.info("Auth User Balance is successful");
		utils.logit("Auth User Balance is successful");
		String membership_level = authUserBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name").toString();
		String membership_level_id = authUserBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_id").toString();
		int points_balance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemable_points = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(membership_level, dataSet.get("membership_level"), "Membership Level doesn't match");
		Assert.assertEquals(points_balance, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(membership_level_id, dataSet.get("membership_level_id"),
				"Membership Level ID doesn't match");
		Assert.assertEquals(redeemable_points, actualPoints, "Redeemable points doesn't match");

		logger.info(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in users balance api");
		utils.logit(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in users balance api");

		// Auth Api Fetch checkin balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Auth Api Fetch account balance");
		logger.info("Auth Api Fetch account balance is successful");
		utils.logPass("Auth Api Fetch account balance is successful");

		String banked_rewards = fetchAccountBalResponse.jsonPath().get("banked_rewards").toString();
		String membership_level1 = fetchAccountBalResponse.jsonPath().get("membership_level").toString();
		String membership_level_id1 = fetchAccountBalResponse.jsonPath().get("membership_level_id").toString();
		int net_balance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		int net_debits = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("net_debits").toString().replace(".0", ""));
		int points_balance1 = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int total_credits = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int total_debits = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));

		Assert.assertEquals(banked_rewards, dataSet.get("banked_rewards"), "Banked rewards doesn't match");
		Assert.assertEquals(membership_level1, dataSet.get("membership_level"),
				"Membership Level rewards doesn't match");
		Assert.assertEquals(membership_level_id1, dataSet.get("membership_level_id"),
				"Membership Level ID rewards doesn't match");
		Assert.assertEquals(net_balance, actualPoints, "Net Balance doesn't match");
		Assert.assertEquals(net_debits, expected_net_debit, "Net Debits doesn't match");
		Assert.assertEquals(points_balance1, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(total_credits, amt, "Total Credits doesn't match");
		Assert.assertEquals(total_debits, expected_net_debit, "Total Debits doesn't match");

		logger.info("Response of checkin balance API has been validated");
		utils.logPass("Response of checkin balance API has been validated");

	}

	@Test(description = "OLF-T2058 [New ML redemption mark is less than old] Verify user balance in different V1 API's when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2058_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		String points_earned1 = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString().replace(".0", "");
		int pointsEarned1 = Integer.parseInt(points_earned1);
		Assert.assertEquals(pointsEarned1, points_earned, "points balance is not equal to " + points_earned);
		logger.info("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");
		utils.logPass("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");

		int points_spent1 = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		Assert.assertEquals(points_spent1, points_spent, "total credits is not equal to 0");
		logger.info("Verified that total credits is not equal to 0 for Api V1 checkins");
		utils.logPass("Verified that total credits is not equal to 0 for Api V1 checkins");

		int points_available = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0", ""));
		Assert.assertEquals(points_available, actualPoints, "Available points doesn't match");
		logger.info("Available points matched for Api V1 checkins");
		utils.logPass("Available points matched for Api V1 checkins");

	}

	@Test(description = "OLF-T2057 [New ML redemption mark is less than old] Verify user balance in different API2 API's when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2057_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		String points_earned1 = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString().replace(".0", "");
		int pointsEarned1 = Integer.parseInt(points_earned1);
		Assert.assertEquals(pointsEarned1, points_earned, "points balance is not equal to " + points_earned);
		logger.info("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");
		utils.logPass("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");

		int points_spent1 = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		Assert.assertEquals(points_spent1, points_spent, "total credits is not equal to 0");
		logger.info("Verified that total credits is not equal to 0 for Api V1 checkins");
		utils.logPass("Verified that total credits is not equal to 0 for Api V1 checkins");

		int points_available = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0", ""));
		Assert.assertEquals(points_available, actualPoints, "Available points doesn't match");
		logger.info("Available points matched for Api V1 checkins");
		utils.logPass("Available points matched for Api V1 checkins");

		// Fetch user balance(api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("Api2 Fetch user balance is successful");
		utils.logPass("Api2 Fetch user balance is successful");
		String membership_level = userBalanceResponse.jsonPath().get("account_balance.current_membership_level_name")
				.toString();
		String membership_level_id = userBalanceResponse.jsonPath().get("account_balance.current_membership_level_id")
				.toString();
		int points_balance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemable_points = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(membership_level, dataSet.get("membership_level"), "Membership Level doesn't match");
		Assert.assertEquals(points_balance, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(membership_level_id, dataSet.get("membership_level_id"),
				"Membership Level ID doesn't match");
		Assert.assertEquals(redeemable_points, actualPoints, "Redeemable points doesn't match");

		logger.info(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in mobile users balance api");
		utils.logit(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in mobile users balance api");

		// Mobile Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		logger.info("Mobile Account Balance is successful");
		utils.logit("Mobile Account Balance is successful");

		String membership_level1 = userBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_name").toString();
		String membership_level_id1 = userBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_id").toString();
		int points_balance1 = Integer.parseInt(userBalanceResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int redeemable_points1 = Integer.parseInt(userBalanceResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(membership_level1, dataSet.get("membership_level"), "Membership Level doesn't match");
		Assert.assertEquals(points_balance1, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(membership_level_id1, dataSet.get("membership_level_id"),
				"Membership Level ID doesn't match");
		Assert.assertEquals(redeemable_points1, actualPoints, "Redeemable points doesn't match");

		logger.info(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in Mobile Account Balance");
		utils.logit(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated Mobile Account Balance");
	}

	@Test(description = "OLF-T2056 [New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2056_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		// Fetch user balance(api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("Api2 Fetch user balance is successful");
		utils.logPass("Api2 Fetch user balance is successful");
		String membership_level = userBalanceResponse.jsonPath().get("account_balance.current_membership_level_name")
				.toString();
		String membership_level_id = userBalanceResponse.jsonPath().get("account_balance.current_membership_level_id")
				.toString();
		int points_balance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemable_points = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(membership_level, dataSet.get("membership_level"), "Membership Level doesn't match");
		Assert.assertEquals(points_balance, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(membership_level_id, dataSet.get("membership_level_id"),
				"Membership Level ID doesn't match");
		Assert.assertEquals(redeemable_points, actualPoints, "Redeemable points doesn't match");

		logger.info(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in mobile users balance api");
		utils.logit(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in mobile users balance api");

		// Call user Checkins Balance api of mobile v1
		Response Api1MobileCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client,
				secret);
		Assert.assertEquals(Api1MobileCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api mobile Checkins Balance");
		logger.info("Checkins Balance api of mobile is successful");
		utils.logit("Checkins Balance api of mobile is successful");
		String membership_level1 = userBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_name").toString();
		String membership_level_id1 = userBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_id").toString();
		int points_balance1 = Integer.parseInt(userBalanceResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int redeemable_points1 = Integer.parseInt(userBalanceResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(membership_level1, dataSet.get("membership_level"), "Membership Level doesn't match");
		Assert.assertEquals(points_balance1, actualPoints, "Points Balance doesn't match");
		Assert.assertEquals(membership_level_id1, dataSet.get("membership_level_id"),
				"Membership Level ID doesn't match");
		Assert.assertEquals(redeemable_points1, actualPoints, "Redeemable points doesn't match");

		logger.info(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated in Mobile checkins Balance");
		utils.logit(
				"current_membership_level_id, points_balance, current_membership_level_name, redeemable_points have been validated Mobile checkins Balance");

		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		logger.info("Secure Api mobile checkin is successful");
		utils.logit(" Secure Api mobile checkin is successful");
		String points_earned1 = Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString().replace(".0", "");
		int pointsEarned1 = Integer.parseInt(points_earned1);
		Assert.assertEquals(pointsEarned1, points_earned, "points balance is not equal to " + points_earned);
		logger.info("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");
		utils.logPass("Verified that points balance is not equal to " + points_earned + " for Api V1 checkins");

		int points_spent1 = Integer
				.parseInt(Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		Assert.assertEquals(points_spent1, points_spent, "total credits is not equal to 0");
		logger.info("Verified that total credits is not equal to 0 for Api V1 checkins");
		utils.logPass("Verified that total credits is not equal to 0 for Api V1 checkins");

		int points_available = Integer
				.parseInt(Api1FetchCheckinResp.jsonPath().get("[0].points_available").toString().replace(".0", ""));
		Assert.assertEquals(points_available, actualPoints, "Available points doesn't match");
		logger.info("Available points matched for Api V1 checkins");
		utils.logPass("Available points matched for Api V1 checkins");
	}

	@Test(description = "OLF-T2055 [New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2055_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();
		String description;

		int k = 0;
		// User Account History
		Response accountHistoryResponse2 = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse2.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj2 = accountHistoryResponse2.jsonPath().getList("description");
		for (int i = 0; i < obj2.size(); i++) {
			description = accountHistoryResponse2.jsonPath().getString("[" + i + "].description");
			if (description.contains(dataSet.get("redeemableName"))) {
				i = k;
				break;
			}
		}

		String eventValue = accountHistoryResponse2.jsonPath().get("[" + k + "].event_value").toString();
		Assert.assertEquals(eventValue, "+Item", "banking event in account history is not created");
		logger.info("verified that banking event in account history created");
		utils.logPass("verified that banking event in account history created");
	}

	@Test(description = "OLF-T2054 [New ML redemption mark is less than old] Verify user's timeline / pie chart when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2054_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		String redeemablepieChartPoints = pageObj.guestTimelinePage().getRedeemablePieChartDetails();
		Assert.assertEquals(actualredeemablePoints, actualPoints, "User balance is not correct on timeline");
		Assert.assertEquals(redeemablepieChartPoints, String.valueOf(actualPoints),
				"Redeemable pie chart points balance is not correct on timeline");
		logger.info("Verified that User balance is correct on both timeline and on redeemable pie chart");
		utils.logPass("Verified that User balance is correct on both timeline and on redeemable pie chart :"
						+ actualredeemablePoints);
	}

	@Test(description = "OLF-T2053 [New ML redemption mark is less than old] Verify user's print timeline when business configuration has membership level Redemption Mark configured [User case 2].")
	public void T2053_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "707";
		String previousMembershipId = "706";

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

		// edit membership levels redemption mark - 100
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

		// Do checkin of 200
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, "200");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "200" + " points");
		utils.logPass("POS checkin is successful for " + "200" + " points");

		// Do checkin of 80
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, "80");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "80" + " points");
		utils.logPass("POS checkin is successful for " + "80" + " points");

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark1"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "3");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + "3" + " points");
		utils.logPass("POS checkin is successful for " + "3" + " points");
		int amt = (Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"))
				+ Integer.parseInt(dataSet.get("points")));
		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "pointsSpentAmount",
				5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int expected_net_debit = amt - actualPoints;
		long phone = (long) (Math.random() * Math.pow(10, 10));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// Check print timeline for user
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains(points_expired + ".0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains(actualPoints + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + actualPoints),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
