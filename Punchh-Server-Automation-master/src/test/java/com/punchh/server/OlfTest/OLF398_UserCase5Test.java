package com.punchh.server.OlfTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
public class OLF398_UserCase5Test {
	private static Logger logger = LogManager.getLogger(OLF398_UserCase5Test.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl, client, secret, apiKey, locationKey;
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
		apiKey = "6J8cAY_aJqSKxvUWjy-k";
		locationKey = "cc33b01d7f794e13a2d9ae1d3901af8e";
	}

//	Author :- Hardik Bhardwaj

	@Test(description = "OLF-T2106 [New ML redemption mark is less than old] Verify point balance in iframe for users when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2106_VerifyPointBalanceinIframe() throws Exception {

		// getting data from business.preference from DB and update all flag values
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

		// Checking and Marking "enable_staged_rewards" ->
		// false in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));

		// Checking and Marking "enable_points_spent_backfill" ->
		// false in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}
		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// User balance validation on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 1);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		String membershipLevel = pageObj.iframeSingUpValidationPageClass().getMembershiplevel();
		Assert.assertEquals(Integer.parseInt(iFrameActualpoints), Integer.parseInt(dataSet.get("amount")),
				"Current Points for Iframe is not matching");
		Assert.assertEquals(membershipLevel, "Membership Level Silver", "Membership Levelfor Iframe is not matching");
		utils.logPass("Iframe points and membership level validated");

	}

	@Test(description = "OLF-T2107 [New ML redemption mark is less than old] Verify user's print timeline when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2107_VerifyPrintTimeLine() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int point = Integer.parseInt(dataSet.get("amount"));
		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("" + point + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + "" + point + ""),
				"Redeemable Points is not correct on print timeline");
		Assert.assertTrue(
				printTimelienPageContentes
						.contains("(" + Integer.parseInt(dataSet.get("originalPoints")) + " Migrated Paradise Points)"),
				"Migrated Paradise Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + point + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

	}

	@Test(description = "OLF-T2108 [New ML redemption mark is less than old] Verify user's timeline / pie chart when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2108_VerifyTimeLine() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int point = Integer.parseInt(dataSet.get("amount"));
		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		String loyaltypieChartPoints = pageObj.guestTimelinePage().getLoyaltyPieChartDetails();
		Assert.assertEquals(loyaltypieChartPoints, actualPoints,
				"loyalty pie chart points balance is not correct on timeline");
		logger.info("Verified that User balance is correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

	}

	@Test(description = "OLF-T2109 [New ML redemption mark is less than old] Verify user's account history when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2109_VerifyAccoutHistory() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int point = Integer.parseInt(dataSet.get("amount"));

		// Banking event in account history created
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

	@Test(description = "OLF-T2110 [New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2110_VerifySecureApi() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 mobile users balance");
		logger.info("Secure Account Balance is successful");
		TestListeners.extentTest.get().info("Secure Account Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		int bankedRewards = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.banked_rewards").toString().replace(".00", ""));
		String membershipLevel = balance_Response.jsonPath().get("account_balance.membership_level").toString();
		String membershipLevelId = balance_Response.jsonPath().get("account_balance.membership_level_id").toString();
		int netBalance = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.net_balance").toString().replace(".0", ""));
		int netDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.net_debits").toString().replace(".0", ""));

		Assert.assertEquals(pointsBalance, initialPoints,
				"points balance for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalCredits, totalPoints, "total cerdit for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalDebits, totalPoints, "total debit for api v1 mobile users balance is not matching");
		Assert.assertEquals(bankedRewards, initialPoints,
				"banked Rewards for api v1 mobile users balance is not matching");
		Assert.assertEquals(membershipLevel, dataSet.get("membership1"),
				"membership Level for api v1 mobile users balance is not matching");
		Assert.assertEquals(membershipLevelId, dataSet.get("membershipId1"),
				"membership Level Id for api v1 mobile users balance is not matching");
		Assert.assertEquals(netBalance, initialPoints, "net Balance for api v1 mobile users balance is not matching");
		Assert.assertEquals(netDebits, totalPoints, "net Debits for api v1 mobile users balance is not matching");

		logger.info("points balance, banked rewards and net balance for api v1 mobile users balance is matching i.e. "
				+ initialPoints);
		utils.logPass("points balance, banked rewards and net balance for api v1 mobile users balance is matching i.e "
						+ totalDebits);
		logger.info("Total Debits, total cerdit and net Debits for api v1 mobile users balance is matching i.e. "
				+ totalPoints);
		utils.logPass("Total Debits, total cerdit and net Debits for api v1 mobile users balance is matching i.e. "
						+ totalPoints);
		logger.info("membership Level Id for api v1 mobile users balance is matching i.e. " + membershipLevelId);
		utils.logPass("membership Level Id for api v1 mobile users balance is matching i.e. " + membershipLevelId);
		logger.info("membership Level for api v1 mobile users balance is matching i.e. " + membershipLevel);
		utils.logPass("membership Level for api v1 mobile users balance is matching i.e. " + membershipLevel);

		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		utils.logPass("API v1 Fetch User Checkins Balance for user  is successful");
		logger.info("API v1 Fetch User Checkins Balance for user  is successful");

		int pointsBalance1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int totalCredits1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int totalDebits1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int bankedRewards1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		String membershipLevel1 = userCheckinsBalanceResponse.jsonPath().get("membership_level").toString();
		String membershipLevelId1 = userCheckinsBalanceResponse.jsonPath().get("membership_level_id").toString();
		int netBalance1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		int netDebits1 = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("net_debits").toString().replace(".0", ""));

		Assert.assertEquals(pointsBalance1, initialPoints, "points balance for api v1 mobile balance is not matching");
		Assert.assertEquals(totalCredits1, totalPoints, "total cerdit for api v1 mobile balance is not matching");
		Assert.assertEquals(totalDebits1, totalPoints, "total debit for api v1 mobile balance is not matching");
		Assert.assertEquals(bankedRewards1, initialPoints, "banked Rewards for api v1 mobile balance is not matching");
		Assert.assertEquals(membershipLevel1, dataSet.get("membership1"),
				"membership Level for api v1 mobile balance is not matching");
		Assert.assertEquals(membershipLevelId1, dataSet.get("membershipId1"),
				"membership Level Id for api v1 mobile balance is not matching");
		Assert.assertEquals(netBalance1, initialPoints, "net Balance for api v1 mobile balance is not matching");
		Assert.assertEquals(netDebits1, totalPoints, "net Debits for api v1 mobile balance is not matching");

		logger.info("points balance, banked rewards and net balance for api v1 mobile balance is matching i.e. "
				+ initialPoints);
		utils.logPass("points balance, banked rewards and net balance for api v1 mobile balance is matching i.e "
						+ totalDebits1);
		logger.info(
				"Total Debits, total cerdit and net Debits for api v1 mobile balance is matching i.e. " + totalPoints);
		utils.logPass(
				"Total Debits, total cerdit and net Debits for api v1 mobile balance is matching i.e. " + totalPoints);
		logger.info("membership Level Id for api v1 mobile balance is matching i.e. " + membershipLevelId1);
		utils.logPass("membership Level Id for api v1 mobile balance is matching i.e. " + membershipLevelId1);
		logger.info("membership Level for api v1 mobile balance is matching i.e. " + membershipLevel1);
		utils.logPass("membership Level for api v1 mobile balance is matching i.e. " + membershipLevel1);

		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), 200,
				"Status code 200 did not matched for Secure Api mobile checkin");
		logger.info("Secure Api mobile checkin is successful");
		TestListeners.extentTest.get().info(" Secure Api mobile checkin is successful");

		String points_earnedApiFetchCheckin = Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString()
				.replace("[", "").replace("]", "");
		String points_earnedApiFetchCheckin1 = Api1FetchCheckinResp.jsonPath().get("[1].points_earned").toString()
				.replace("[", "").replace("]", "");
		int pointsEarnedApiFetchCheckin = Integer.parseInt(points_earnedApiFetchCheckin)
				+ Integer.parseInt(points_earnedApiFetchCheckin1);
		Assert.assertEquals(pointsEarnedApiFetchCheckin, totalPoints, "points earned is not equal to " + totalPoints);
		logger.info("Verified that points earned is not equal to " + totalPoints + " for Secure Api Fetch Checkin");
		utils.logPass("Verified that points earned is not equal to " + totalPoints + " for Secure Api Fetch Checkin");

		String points_spentApiFetchCheckin = Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString()
				.replace("[", "").replace("]", "");
		String points_spentApiFetchCheckin1 = Api1FetchCheckinResp.jsonPath().get("[1].points_spent").toString()
				.replace("[", "").replace("]", "");
		int points_spentApiFetchCheckinTotal = Integer.parseInt(points_spentApiFetchCheckin)
				+ Integer.parseInt(points_spentApiFetchCheckin1);
		Assert.assertEquals(points_spentApiFetchCheckinTotal, totalPoints,
				"points_spent is not equal to " + totalPoints);
		logger.info("Verified that points_spent is not equal to " + totalPoints + " for Secure Api Fetch Checkin");
		utils.logPass("Verified that points_spent is not equal to " + totalPoints + " for Secure Api Fetch Checkin");

		String points_availableApiFetchCheckin = Api1FetchCheckinResp.jsonPath().get("[0].points_available").toString()
				.replace("[", "").replace("]", "");
		String points_availableApiFetchCheckin1 = Api1FetchCheckinResp.jsonPath().get("[1].points_available").toString()
				.replace("[", "").replace("]", "");
		int points_availableApiFetchCheckinTotal = Integer.parseInt(points_availableApiFetchCheckin)
				+ Integer.parseInt(points_availableApiFetchCheckin1);

		Assert.assertEquals(points_availableApiFetchCheckinTotal, 0, "points_spent is not equal to 0");
		logger.info("Verified that points_available is not equal to 0 for Secure Api Fetch Checkin");
		utils.logPass("Verified that points_available is not equal to 0 for Secure Api Fetch Checkin");

		// Step - 7 api/mobile/checkins?transaction_id=transaction_id/transactions API
		// (REMAINING)

	}

	@Test(description = "OLF-T2111 [New ML redemption mark is less than old] Verify user balance in different API2 API's when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2111_VerifyMobileApi() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		// Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		int pointsBalanceUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		String currentMembershipLevelId = userBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_id").toString();
		String currentMembershipLevelName = userBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name").toString();
		Assert.assertEquals(pointsBalanceUser, initialPoints,
				"points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(redeemablePointsUser, initialPoints,
				"redeemable points for api2 fetch User Balance is not matching");
		Assert.assertEquals(currentMembershipLevelId, dataSet.get("membershipId1"),
				"current Membership Level Id for api2 fetch User Balance is not matching");
		Assert.assertEquals(currentMembershipLevelName, dataSet.get("membership1"),
				"current Membership Level Name for api2 fetch User Balance is not matching");
		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. "
				+ pointsBalanceUser);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + pointsBalanceUser);
		logger.info("membership Level Id for api2 fetch User Balance is matching i.e. " + currentMembershipLevelId);
		utils.logPass("membership Level Id for api2 fetch User Balance is matching i.e. " + currentMembershipLevelId);
		logger.info("membership Level for api2 fetch User Balance is matching i.e. " + currentMembershipLevelName);
		utils.logPass("membership Level for api2 fetch User Balance is matching i.e. " + currentMembershipLevelName);

		// Check user balance in api2/mobile/checkins/account_balance API
		Response Api2CheckinAccountBalanceResponse = pageObj.endpoints().Api2CheckinAccountBalance(client, secret,
				token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, Api2CheckinAccountBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch Account balance");
		logger.info("Api2 Fetch checkin account balance is successful");
		utils.logPass("Api2 Fetch checkin account balance is successful");
		int pointsBalanceAccount = Integer.parseInt(Api2CheckinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccount = Integer.parseInt(Api2CheckinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		String currentMembershipLevelIdAcc = Api2CheckinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_id").toString();
		String currentMembershipLevelNameAcc = Api2CheckinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_name").toString();
		Assert.assertEquals(pointsBalanceAccount, initialPoints,
				"points balance for api2 fetch Account balance is not matching");
		Assert.assertEquals(redeemablePointsAccount, initialPoints,
				"redeemable points for api2 fetch Account balance is not matching");
		Assert.assertEquals(currentMembershipLevelIdAcc, dataSet.get("membershipId1"),
				"current Membership Level Id for api2 fetch Account balance is not matching");
		Assert.assertEquals(currentMembershipLevelNameAcc, dataSet.get("membership1"),
				"current Membership Level Name for api2 fetch Account balance is not matching");
		logger.info(
				"Redeemable Points and Point Balance for api2 fetch Account balance is matching i.e. " + initialPoints);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch Account balance is matching i.e " + initialPoints);

		logger.info("membership Level Id for api2 fetch Account Balance is matching i.e. " + currentMembershipLevelId);
		utils.logPass(
				"membership Level Id for api2 fetch Account Balance is matching i.e. " + currentMembershipLevelId);
		logger.info("membership Level for api2 fetch Account Balance is matching i.e. " + currentMembershipLevelName);
		utils.logPass("membership Level for api2 fetch Account Balance is matching i.e. " + currentMembershipLevelName);

	}

	@Test(description = "OLF-T2112 [New ML redemption mark is less than old] Verify user balance in different V1 API's when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2112_VerifyV1Api() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		TestListeners.extentTest.get().info("Api V1 checkins is successful");
		String pointsEarnedV1 = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString().replace(".0", "");
		String pointsEarnedV1checkin = checkinsApiV1Response.jsonPath().get("[1].points_earned").toString()
				.replace(".0", "");
		int pointsEarnedTotal = Integer.parseInt(pointsEarnedV1) + Integer.parseInt(pointsEarnedV1checkin);
		Assert.assertEquals(pointsEarnedTotal, totalPoints, "points balance is not equal to " + totalPoints);
		logger.info("Verified that points balance is not equal to " + totalPoints + " for Api V1 checkins");
		utils.logPass("Verified that points balance is not equal to " + totalPoints + " for Api V1 checkins");

		String pointsSpentV1 = checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", "");
		String pointsSpentV1checkin = checkinsApiV1Response.jsonPath().get("[1].points_spent").toString().replace(".0",
				"");
		int pointsSpentTotal = Integer.parseInt(pointsSpentV1) + Integer.parseInt(pointsSpentV1checkin);
		Assert.assertEquals(pointsSpentTotal, totalPoints, "total credits is not equal to 0");
		logger.info("Verified that total credits is not equal to " + totalPoints + " for Api V1 checkins");
		utils.logPass("Verified that total credits is not equal to " + totalPoints + " for Api V1 checkins");

		String pointsAvailableV1 = checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0",
				"");
		String pointsAvailableV1checkin = checkinsApiV1Response.jsonPath().get("[1].points_available").toString()
				.replace(".0", "");
		int pointsAvailableTotal = Integer.parseInt(pointsAvailableV1) + Integer.parseInt(pointsAvailableV1checkin);
		Assert.assertEquals(pointsAvailableTotal, actualPoints, "total debits is not equal to " + actualPoints);
		logger.info("Verified that total debits is not equal to " + actualPoints + " for Api V1 checkins");
		utils.logPass("Verified that total debits is not equal to " + actualPoints + " for Api V1 checkins");

		// Step - 5 api/mobile/checkins?transaction_id=transaction_id/transactions API
		// (REMAINING)
	}

	@Test(description = "OLF-T2113 [New ML redemption mark is less than old] Verify user balance in different Auth API's when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2113_VerifyAuthApi() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), 200, "Failed Auth API Account balance response");
		logger.info("Auth User Balance is successful");
		TestListeners.extentTest.get().info("Auth User Balance is successful");
		int pointBalanceAccountBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		String currentMembershipLevelIdAccountBalance = authUserBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_id").toString();
		String currentMembershipLevelNameAccountBalance = authUserBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name").toString();
		Assert.assertEquals(pointBalanceAccountBalance, initialPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance, initialPoints,
				"redeemable Points Account Balance is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");
		Assert.assertEquals(currentMembershipLevelIdAccountBalance, dataSet.get("membershipId1"),
				"current membership level id Account Balance is not matching");
		Assert.assertEquals(currentMembershipLevelNameAccountBalance, dataSet.get("membership1"),
				"current membership level name Account Balance is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass(
				"current membership level id and current membership level name Account Balance is matching the expected result");

		// Auth Api Fetch checkin balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		logger.info("Auth Api Fetch account balance is successful");
		utils.logPass("Auth Api Fetch account balance is successful");
		String point_BalanceAuthCheckinsBalance = fetchAccountBalResponse.jsonPath().get("points_balance").toString()
				.replace(".0", "");
		int pointBalanceAuthCheckinsBalance = Integer.parseInt(point_BalanceAuthCheckinsBalance);
		Assert.assertEquals(pointBalanceAuthCheckinsBalance, initialPoints,
				"points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + initialPoints
				+ " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that points balance is not equal to " + initialPoints
				+ " for Auth Api Fetch checkin balance");

		int total_creditsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsAuthCheckinsBalance, totalPoints,
				"total credits is not equal to " + totalPoints);
		logger.info(
				"Verified that total credits is not equal to " + totalPoints + " for Auth Api Fetch checkin balance");
		utils.logPass(
				"Verified that total credits is not equal to " + totalPoints + " for Auth Api Fetch checkin balance");

		int total_debitsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsAuthCheckinsBalance, totalPoints, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to " + totalPoints + " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that total debits is equal to " + totalPoints + " for Auth Api Fetch checkin balance");

		int banked_rewardsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(banked_rewardsAuthCheckinsBalance, initialPoints,
				"banked_rewards is not equal to " + initialPoints);
		logger.info(
				"Verified that banked_rewards is equal to " + initialPoints + " for Auth Api Fetch checkin balance");
		utils.logPass(
				"Verified that banked_rewards is equal to " + initialPoints + " for Auth Api Fetch checkin balance");

		String membership_levelAuthCheckinsBalance = fetchAccountBalResponse.jsonPath().get("membership_level")
				.toString();
		Assert.assertEquals(membership_levelAuthCheckinsBalance, dataSet.get("membership1"),
				"membership_level is not equal to " + dataSet.get("membership1"));
		logger.info("Verified that membership_level is equal to " + dataSet.get("membership1")
				+ " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that membership_level is equal to " + dataSet.get("membership1")
				+ " for Auth Api Fetch checkin balance");

		String membershipLevelIdAuthCheckinsBalance = fetchAccountBalResponse.jsonPath().get("membership_level_id")
				.toString();
		Assert.assertEquals(membershipLevelIdAuthCheckinsBalance, dataSet.get("membershipId1"),
				"membership_level_id is not equal to " + dataSet.get("membershipId1"));
		logger.info("Verified that membership_level_id is equal to " + dataSet.get("membershipId1")
				+ " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that membership_level_id is equal to "
				+ dataSet.get("membershipId1") + " for Auth Api Fetch checkin balance");

		int net_balanceAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		Assert.assertEquals(net_balanceAuthCheckinsBalance, initialPoints,
				"net_balance is not equal to " + initialPoints);
		logger.info("Verified that net_balance is equal to " + initialPoints + " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that net_balance is equal to " + initialPoints + " for Auth Api Fetch checkin balance");

		int net_debitsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("net_debits").toString().replace(".0", ""));
		Assert.assertEquals(net_debitsAuthCheckinsBalance, totalPoints, "net_debits is not equal to " + initialPoints);
		logger.info("Verified that net_debits is equal to " + totalPoints + " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that net_debits is equal to " + totalPoints + " for Auth Api Fetch checkin balance");

	}

	@Test(description = "OLF-T2114 [New ML redemption mark is less than old] Verify user balance in dashboard API when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2114_VerifyDashboardApi() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		long phone = (long) (Math.random() * Math.pow(10, 10));
		// hit api api2/dashboard/users/info
		// Email only
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, "");
		Assert.assertEquals(userLookUpApi2.getStatusCode(), 200,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		String point_BalanceUsersInfo = userLookUpApi2.jsonPath().get("balance.points_balance").toString().replace(".0",
				"");
		int pointBalanceUsersInfo = Integer.parseInt(point_BalanceUsersInfo);
		Assert.assertEquals(pointBalanceUsersInfo, initialPoints, "points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");

		int total_creditsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsUsersInfo, totalPoints, "total credits is not equal to " + totalPoints);
		logger.info("Verified that total credits is not equal to " + totalPoints + " for Dashboard user info");
		utils.logPass("Verified that total credits is not equal to " + totalPoints + " for Dashboard user info");

		int total_debitsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsUsersInfo, totalPoints, "total debits is not equal to " + totalPoints);
		logger.info("Verified that total debits is equal to " + totalPoints + " for Dashboard user info");
		utils.logPass("Verified that total debits is equal to " + totalPoints + " for Dashboard user info");

		int banked_rewardsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(banked_rewardsUsersInfo, actualPoints, "banked_rewards is not equal to " + actualPoints);
		logger.info("Verified that banked_rewards is equal to " + actualPoints + " for Dashboard user info");
		utils.logPass("Verified that banked_rewards is equal to " + actualPoints + " for Dashboard user info");

		String membership_levelUsersInfo = userLookUpApi2.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(membership_levelUsersInfo, dataSet.get("membership1"),
				"membership_level is not equal to " + dataSet.get("membership1"));
		logger.info("Verified that membership_level is equal to " + dataSet.get("membership1")
				+ " for Dashboard user info");
		utils.logPass("Verified that membership_level is equal to " + dataSet.get("membership1")
				+ " for Dashboard user info");

		String membershipLevelIdUsersInfo = userLookUpApi2.jsonPath().get("balance.membership_level_id").toString();
		Assert.assertEquals(membershipLevelIdUsersInfo, dataSet.get("membershipId1"),
				"membership_level_id is not equal to " + dataSet.get("membershipId1"));
		logger.info("Verified that membership_level_id is equal to " + dataSet.get("membershipId1")
				+ " for Dashboard user info");
		utils.logPass("Verified that membership_level_id is equal to "
				+ dataSet.get("membershipId1") + " for Dashboard user info");

		int net_balanceUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		Assert.assertEquals(net_balanceUsersInfo, initialPoints, "net_balance is not equal to " + initialPoints);
		logger.info("Verified that net_balance is equal to " + initialPoints + " for Dashboard user info");
		utils.logPass("Verified that net_balance is equal to " + initialPoints + " for Dashboard user info");

		int net_debitsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		Assert.assertEquals(net_debitsUsersInfo, totalPoints, "net_debits is not equal to " + initialPoints);
		logger.info("Verified that net_debits is equal to " + totalPoints + " for Dashboard user info");
		utils.logPass("Verified that net_debits is equal to " + totalPoints + " for Dashboard user info");

	}

	@Test(description = "OLF-T2115 [New ML redemption mark is less than old] Verify user balance in pos/checkin API when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2115_VerifyPosApi() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));
		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		utils.longWaitInSeconds(20);
		int pointBalancePosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		Assert.assertEquals(pointBalancePosCheckin, (initialPoints + totalPoints),
				"points balance is not equal to " + (initialPoints + totalPoints));
		logger.info(
				"Verified that points balance is not equal to " + (initialPoints + totalPoints) + " for Pos Checkin");
		utils.logPass(
				"Verified that points balance is not equal to " + (initialPoints + totalPoints) + " for Pos Checkin");

		int totalCreditsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(totalCreditsPosCheckin, totalPoints, "total credits is not equal to " + totalPoints);
		logger.info("Verified that total credits is not equal to " + totalPoints + " for Pos Checkin");
		utils.logPass("Verified that total credits is not equal to " + totalPoints + " for Pos Checkin");

		int totalDebitsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(totalDebitsPosCheckin, actualPoints, "total debits is not equal to " + actualPoints);
		logger.info("Verified that total debits is equal to " + actualPoints + " for Pos Checkin");
		utils.logPass("Verified that total debits is equal to " + actualPoints + " for Pos Checkin");

		int banked_rewardsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(banked_rewardsPosCheckin, actualPoints, "banked_rewards is not equal to " + actualPoints);
		logger.info("Verified that banked_rewards is equal to " + actualPoints + " for Pos Checkin");
		utils.logPass("Verified that banked_rewards is equal to " + actualPoints + " for Pos Checkin");

		String membership_levelPosCheckin = checkinResponse.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(membership_levelPosCheckin, dataSet.get("membership1"),
				"membership_level is not equal to " + dataSet.get("membership1"));
		logger.info("Verified that membership_level is equal to " + dataSet.get("membership1") + " for Pos Checkin");
		utils.logPass("Verified that membership_level is equal to " + dataSet.get("membership1") + " for Pos Checkin");

		String membershipLevelIdPosCheckin = checkinResponse.jsonPath().get("balance.membership_level_id").toString();
		Assert.assertEquals(membershipLevelIdPosCheckin, dataSet.get("membershipId1"),
				"membership_level_id is not equal to " + dataSet.get("membershipId1"));
		logger.info(
				"Verified that membership_level_id is equal to " + dataSet.get("membershipId1") + " for Pos Checkin");
		utils.logPass(
				"Verified that membership_level_id is equal to " + dataSet.get("membershipId1") + " for Pos Checkin");

		int net_balancePosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		Assert.assertEquals(net_balancePosCheckin, (initialPoints + totalPoints),
				"net_balance is not equal to " + (initialPoints + totalPoints));
		logger.info("Verified that net_balance is equal to " + (initialPoints + totalPoints) + " for Pos Checkin");
		utils.logPass("Verified that net_balance is equal to " + (initialPoints + totalPoints) + " for Pos Checkin");

		int net_debitsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		Assert.assertEquals(net_debitsPosCheckin, actualPoints, "net_debits is not equal to " + initialPoints);
		logger.info("Verified that net_debits is equal to " + actualPoints + " for Pos Checkin");
		utils.logPass("Verified that net_debits is equal to " + actualPoints + " for Pos Checkin");

	}

	@Test(description = "OLF-T2116 [[New ML redemption mark is less than old] Verify user details in accounts table when business configuration has membership level Redemption Mark configured [User case 5] || "
			+ "OLF-T2117 [New ML redemption mark is less than old] Verify user details in checkins table when business configuration has membership level Redemption Mark configured [User case 5] || "
			+ "OLF-T2118 [New ML redemption mark is less than old] Verify PointDebits in reward_debits table when business configuration has membership level Redemption Mark configured [User case 5] || "
			+ "OLF-T2119 [New ML redemption mark is less than old] Verify user details in membership_level_histories table when business configuration has membership level Redemption Mark configured [User case 5]")
	public void T2116_VerifyAccountsTable() throws Exception {
		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			utils.logit(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Do checkin of 3 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int checkinPoint = Integer.parseInt(dataSet.get("amount"));
		int originalPoints = Integer.parseInt(dataSet.get("originalPoints"));
		int initialPoints = Integer.parseInt(dataSet.get("initialPoints"));
		int totalPoints = checkinPoint + originalPoints;

		// checkins table
		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		int points_spent = Integer.parseInt(pointsSpent);
		Assert.assertEquals(points_spent, totalPoints,
				"points_spent column in checkins table is not equal to expected value i.e. " + totalPoints);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		int points_earned = Integer.parseInt(pointsEarned);
		Assert.assertEquals(points_earned, totalPoints,
				"points_earned column in checkins table is not equal to expected value i.e. " + totalPoints);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_expired = 0;
		int actualPoints = (points_earned - (points_spent + points_expired));

		// accounts table
		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"total_debits", 5);
		int totalDebits = Integer.parseInt(expColValue2.replace(".00", ""));
		Assert.assertEquals(totalDebits, totalPoints,
				"total_debits column in accounts table is not equal to expected value i.e. " + totalPoints);
		logger.info("total_debits column in accounts table is equal to expected value i.e. " + totalPoints);
		utils.logPass("total_debits column in accounts table is equal to expected value i.e. " + totalPoints);

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"total_credits", 5);
		int totalCredits = Integer.parseInt(expColValue3.replace(".0", ""));
		Assert.assertEquals(totalCredits, totalPoints,
				"total_credits column in accounts table is not equal to expected value i.e. " + totalCredits);
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + totalCredits);
		utils.logPass("total_credits column in accounts table is equal to expected value i.e. " + totalCredits);

		String query7 = "Select `membership_level` from `accounts` where `user_id` = '" + userID + "';";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level", 5);
		Assert.assertEquals(expColValue5, dataSet.get("membership1"),
				"membership_level column in accounts table is not equal to expected value i.e. "
						+ dataSet.get("membership1"));
		// total_debits
		logger.info("membership_level column in accounts table is equal to expected value i.e. "
				+ dataSet.get("membership1"));
		utils.logPass("membership_level column in accounts table is equal to expected value i.e. "
				+ dataSet.get("membership1"));

		// Check PointDebit in reward_debits table
		String query8 = "Select `type` from `reward_debits` where `user_id` ='" + userID + "';";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"type", 5);
		Assert.assertEquals(expColValue7, "PointDebit",
				"status column in reward_debits table is not equal to expected value i.e. banked");
		logger.info("status column in reward_debits table is equal to expected value i.e. banked");
		utils.logPass("status column in reward_debits table is equal to expected value i.e. banked");

		// effective
		String query9 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, dataSet.get("membershipId1"),
				"Effective membership strategy is not " + dataSet.get("membershipId1"));
		logger.info("Verified that Effective membership strategy is " + dataSet.get("membershipId1"));
		utils.logPass("Verified that Effective membership strategy is " + dataSet.get("membershipId1"));

		// operative
		String query10 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue4, dataSet.get("membershipId1"),
				"Operative membership strategy is not " + dataSet.get("membershipId1"));
		logger.info("Verified that Operative membership strategy is " + dataSet.get("membershipId1"));
		utils.logPass("Verified that Operative membership strategy is " + dataSet.get("membershipId1"));

		// previous membership level
		String query11 = "SELECT `previous_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "';";
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"previous_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue11, null, "previous_membership_level_id is not NULL");
		logger.info("Verified that previous_membership_level_id is NULL");
		utils.logPass("Verified that previous_membership_level_id is NULL");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
