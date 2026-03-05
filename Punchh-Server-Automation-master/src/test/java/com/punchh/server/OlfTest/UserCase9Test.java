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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class UserCase9Test {
	private static Logger logger = LogManager.getLogger(UserCase9Test.class);
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
		client = "Z8v1Q-EYPuMjZXffOaA9Jc0y7Fbw2hx1ITsXQsL6tAw";
		secret = "zdJKoyKLx5iiRKDF4d2ULmcspxTjkF6eDy1anN75fZM";
		apiKey = "NPRCDe-tyrwjxvjhsn6x";
		locationKey = "dd33b01d7f794e13a2d9ae1d3901af8e";
	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2182 [New ML redemption mark is less than old] Verify point balance in iframe for users when business configuration has membership level Redemption Mark configured")
	public void T2182_verifyPointBalanceIframe() throws Exception {

		// Get data from business.preference table, update all flag values
		String businessId = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		// Check and update "enable_banking_based_on_unified_balance" to true
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, businessId, env, "true", dataSet.get("dbFlag1"));

		// Check and update "track_points_spent" to true
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, businessId, env, "true", dataSet.get("dbFlag2"));

		// Check and update "enable_transaction_based_display_account_history" to true
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, businessId, env, "true", dataSet.get("dbFlag3"));

		// Check and update "enable_staged_rewards" to false
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, businessId, env, "false", dataSet.get("dbFlag4"));

		// Check and update "enable_points_spent_backfill" to false
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, businessId, env, "false", dataSet.get("dbFlag5"));

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// === OLF-T2182 ===

		// User Points and Membership validation on iFrame
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 1);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		String membershipLevel = pageObj.iframeSingUpValidationPageClass().getMembershiplevel();
		Assert.assertEquals(Integer.parseInt(iFrameActualpoints), 0, "Current Points for iFrame is not matching");
		Assert.assertEquals(membershipLevel, dataSet.get("expectedMembership"),
				"Membership Level for iFrame is not matching");
		logger.info("iFrame points and membership level are validated");
		TestListeners.extentTest.get().pass("iFrame points and membership level are validated");

	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2183 [New ML redemption mark is less than old] Verify user's print timeline when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2184 [New ML redemption mark is less than old] Verify user's timeline / pie chart when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2185 [New ML redemption mark is less than old] Verify user's account history when business configuration has membership level Redemption Mark configured")
	public void T2183_verifyUserPrintTimeline() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from DB
		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2183 ===

		// Verify User balance is correct on print timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelinePageContents = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelinePageContents.contains(actualPoints + ".0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelinePageContents.contains("" + actualPoints + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelinePageContents.contains("Redeemable Points, \n" + "" + actualPoints + ""),
				"Redeemable Points is not correct on print timeline");

		// Paradise is configurable from Cockpit > Dashboard > Misc Config > Currency
		Assert.assertTrue(printTimelinePageContents.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified that User balance is correct on print timeline");
		TestListeners.extentTest.get().pass("Verified that User balance is correct on print timeline page");

		// === OLF-T2184 ===

		// Verify redeemable points on guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		String loyaltyPieChartPoints = pageObj.guestTimelinePage().getLoyaltyPieChartDetails();
		Assert.assertEquals(Integer.parseInt(loyaltyPieChartPoints), points_earned,
				"loyalty pie chart points balance is not correct on timeline");
		logger.info("Verified that User balance is correct on timeline");
		TestListeners.extentTest.get().pass("Verified that User balance is correct on timeline");

		// === OLF-T2185 ===

		// Banking event in account history created
		List<Object> obj = new ArrayList<Object>();
		String description;
		int j = 0;

		// User Account History using Mobile API2
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(client, secret, token);
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), 200);
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "].description");
			if (description.contains(dataSet.get("redeemableName"))) {
				i = j;
				break;
			}
		}

		String eventValue = accountHistoryResponse.jsonPath().get("[" + j + "].event_value").toString();
		Assert.assertEquals(eventValue, "+Item", "Banking event in account history is not created");
		logger.info("Verified that banking event in account history is created");
		TestListeners.extentTest.get().pass("Verified that banking event in account history is created");

	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2186 [New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured")
	public void T2186_verifySecureApis() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from DB
		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2186 ===

		// Hit api/mobile/users/balance and verify balance
		Response userBalanceApiResponse = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(userBalanceApiResponse.getStatusCode(), 200);
		logger.info("Mobile secure API Fetch User balance is successful");
		TestListeners.extentTest.get().info("Mobile secure API Fetch User balance is successful");

		int userBalanceApiPointsBalance = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int userBalanceApiTotalCredits = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int userBalanceApiTotalDebits = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		int userBalanceApiBankedRewards = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.banked_rewards").toString().replace(".00", ""));
		String userBalanceApiMembershipLevel = userBalanceApiResponse.jsonPath().get("account_balance.membership_level")
				.toString();
		String userBalanceApiMembershipLevelId = userBalanceApiResponse.jsonPath()
				.get("account_balance.membership_level_id").toString();
		int userBalanceApiNetBalance = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.net_balance").toString().replace(".0", ""));
		int userBalanceApiNetDebits = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.net_debits").toString().replace(".0", ""));

		Assert.assertEquals(userBalanceApiPointsBalance, actualPoints,
				"Points balance for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiTotalCredits, points_earned,
				"Total credit for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiTotalDebits, points_spent,
				"Total debit for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiBankedRewards, actualPoints,
				"Banked Rewards for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiMembershipLevel, dataSet.get("expectedMembership"),
				"Membership Level for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Membership Level Id for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiNetBalance, actualPoints,
				"Net Balance for secure API mobile users balance is not matching");
		Assert.assertEquals(userBalanceApiNetDebits, points_spent,
				"Net Debits for secure API mobile users balance is not matching");

		logger.info(
				"Points balance, banked rewards and net balance for secure API mobile users balance is matching i.e. "
						+ actualPoints);
		TestListeners.extentTest.get().pass(
				"Points balance, banked rewards and net balance for secure API mobile users balance is matching i.e "
						+ actualPoints);
		logger.info("Total Credits for secure API mobile users balance is matching i.e. " + points_earned);
		TestListeners.extentTest.get()
				.pass("Total Credits for secure API mobile users balance is matching i.e. " + points_earned);
		logger.info("Total Debits and net debits for secure API mobile users balance is matching i.e. " + points_spent);
		TestListeners.extentTest.get().pass(
				"Total Debits and net debits for secure API mobile users balance is matching i.e. " + points_spent);
		logger.info("Membership Level Id for secure API mobile users balance is matching i.e. "
				+ userBalanceApiMembershipLevelId);
		TestListeners.extentTest.get().pass("Membership Level Id for secure API mobile users balance is matching i.e. "
				+ userBalanceApiMembershipLevelId);
		logger.info("Membership Level for secure API mobile users balance is matching i.e. "
				+ userBalanceApiMembershipLevel);
		TestListeners.extentTest.get().pass("Membership Level for secure API mobile users balance is matching i.e. "
				+ userBalanceApiMembershipLevel);

		// Hit api/mobile/checkins/balance and verify balance
		Response checkinsBalanceApiResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(checkinsBalanceApiResponse.getStatusCode(), 200);
		logger.info("Mobile secure API Fetch User Checkins Balance is successful");
		TestListeners.extentTest.get().info("Mobile secure API Fetch User Checkins Balance is successful");

		int checkinsBalanceApiPointsBalance = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int checkinsBalanceApiTotalCredits = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int checkinsBalanceApiTotalDebits = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int checkinsBalanceApiBankedRewards = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		String checkinsBalanceApiMembershipLevel = checkinsBalanceApiResponse.jsonPath().get("membership_level")
				.toString();
		String checkinsBalanceApiMembershipLevelId = checkinsBalanceApiResponse.jsonPath().get("membership_level_id")
				.toString();
		int checkinsBalanceApiNetBalance = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		int checkinsBalanceApiNetDebits = Integer
				.parseInt(checkinsBalanceApiResponse.jsonPath().get("net_debits").toString().replace(".0", ""));

		Assert.assertEquals(checkinsBalanceApiPointsBalance, actualPoints,
				"Points balance for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiTotalCredits, points_earned,
				"Total credit for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiTotalDebits, points_spent,
				"Total debit for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiBankedRewards, actualPoints,
				"Banked Rewards for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiMembershipLevel, dataSet.get("expectedMembership"),
				"Membership Level for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Membership Level Id for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiNetBalance, actualPoints,
				"Net Balance for secure API user checkins balance is not matching");
		Assert.assertEquals(checkinsBalanceApiNetDebits, points_spent,
				"Net Debits for secure API user checkins balance is not matching");

		logger.info(
				"Points balance, banked rewards and net balance for secure API user checkins balance is matching i.e. "
						+ actualPoints);
		TestListeners.extentTest.get().pass(
				"points balance, banked rewards and net balance for secure API user checkins balance is matching i.e "
						+ actualPoints);
		logger.info("Total credits for secure API user checkins balance is matching i.e. " + points_earned);
		TestListeners.extentTest.get()
				.pass("Total credits for secure API user checkins balance is matching i.e. " + points_earned);
		logger.info(
				"Total Debits and net debits for secure API user checkins balance is matching i.e. " + points_spent);
		TestListeners.extentTest.get().pass(
				"Total Debits and net debits for secure API user checkins balance is matching i.e. " + points_spent);
		logger.info("Membership Level Id for secure API user checkins balance is matching i.e. "
				+ checkinsBalanceApiMembershipLevelId);
		TestListeners.extentTest.get().pass("Membership Level Id for secure API user checkins balance is matching i.e. "
				+ checkinsBalanceApiMembershipLevelId);
		logger.info("Membership Level for secure API user checkins balance is matching i.e. "
				+ checkinsBalanceApiMembershipLevel);
		TestListeners.extentTest.get().pass("Membership Level for secure API user checkins balance is matching i.e. "
				+ checkinsBalanceApiMembershipLevel);

		// Hit api/mobile/checkins and verify the response
		Response fetchCheckinResponse = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200);
		logger.info("Mobile secure API Fetch Checkins is successful");
		TestListeners.extentTest.get().info("Mobile secure API Fetch Checkins is successful");

		String fetchCheckinApiPointsEarned1 = fetchCheckinResponse.jsonPath().get("[0].points_earned").toString()
				.replace("[", "").replace("]", "");
		String fetchCheckinApiPointsEarned2 = fetchCheckinResponse.jsonPath().get("[1].points_earned").toString()
				.replace("[", "").replace("]", "");
		int fetchCheckinApiPointsEarnedTotal = Integer.parseInt(fetchCheckinApiPointsEarned1)
				+ Integer.parseInt(fetchCheckinApiPointsEarned2);
		Assert.assertEquals(fetchCheckinApiPointsEarnedTotal, points_earned,
				"Points earned is not equal to " + points_earned);
		logger.info("Verified that points_earned value is correct i.e. " + points_earned
				+ " in Mobile secure API Fetch Checkins response");
		TestListeners.extentTest.get().pass("Verified that points_earned value is correct i.e. " + points_earned
				+ " in Mobile secure API Fetch Checkins response");

		String fetchCheckinApiPointsSpent1 = fetchCheckinResponse.jsonPath().get("[0].points_spent").toString()
				.replace("[", "").replace("]", "");
		String fetchCheckinApiPointsSpent2 = fetchCheckinResponse.jsonPath().get("[1].points_spent").toString()
				.replace("[", "").replace("]", "");
		int fetchCheckinApiPointsSpentTotal = Integer.parseInt(fetchCheckinApiPointsSpent1)
				+ Integer.parseInt(fetchCheckinApiPointsSpent2);
		Assert.assertEquals(fetchCheckinApiPointsSpentTotal, points_spent,
				"Points spent is not equal to " + points_spent);
		logger.info("Verified that points_spent value is correct i.e. " + points_spent
				+ " in Mobile secure API Fetch Checkins response");
		TestListeners.extentTest.get().pass("Verified that points_spent value is correct i.e. " + points_spent
				+ " in Mobile secure API Fetch Checkins response");

		String fetchCheckinApiPointsAvailable1 = fetchCheckinResponse.jsonPath().get("[0].points_available").toString()
				.replace("[", "").replace("]", "");
		String fetchCheckinApiPointsAvailable2 = fetchCheckinResponse.jsonPath().get("[1].points_available").toString()
				.replace("[", "").replace("]", "");
		int fetchCheckinApiPointsAvailableTotal = Integer.parseInt(fetchCheckinApiPointsAvailable1)
				+ Integer.parseInt(fetchCheckinApiPointsAvailable2);
		int fetchCheckinApiPointsAvailableTotalExpected = fetchCheckinApiPointsEarnedTotal
				- fetchCheckinApiPointsSpentTotal;
		Assert.assertEquals(fetchCheckinApiPointsAvailableTotal, fetchCheckinApiPointsAvailableTotalExpected,
				"Points available is not equal to " + actualPoints);
		logger.info("Verified that points_available value is correct i.e. " + actualPoints
				+ " in Mobile secure API Fetch Checkins response");
		TestListeners.extentTest.get().pass("Verified that points_available value is correct i.e. " + actualPoints
				+ " in Mobile secure API Fetch Checkins response");

		// Hit api/mobile/checkins?transaction_id=transaction_id/transactions API
		// and verify the response ==> REMAINING
	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2187 [New ML redemption mark is less than old] Verify user balance in different API2 API's when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2188 [New ML redemption mark is less than old] Verify user balance in different V1 API's when business configuration has membership level Redemption Mark configured")
	public void T2187_verifyMobileApi2() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}
		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from DB
		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2187 ===

		// Hit api2/mobile/users/balance and verify balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200);
		logger.info("API2 User Balance is successful");
		TestListeners.extentTest.get().info("API2 User Balance is successful");

		int userBalanceApiPointsBalance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int userBalanceApiRedeemablePoints = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		String userBalanceApiMembershipLevelId = userBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_id").toString();
		String userBalanceApiMembershipLevelName = userBalanceResponse.jsonPath()
				.get("account_balance.current_membership_level_name").toString();
		Assert.assertEquals(userBalanceApiPointsBalance, actualPoints,
				"Points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(userBalanceApiRedeemablePoints, actualPoints,
				"Redeemable points for api2 fetch User Balance is not matching");
		Assert.assertEquals(userBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Current Membership Level Id for api2 fetch User Balance is not matching");
		Assert.assertEquals(userBalanceApiMembershipLevelName, dataSet.get("expectedMembership"),
				"Current Membership Level Name for api2 fetch User Balance is not matching");

		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. " + actualPoints);
		TestListeners.extentTest.get().pass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + actualPoints);
		logger.info(
				"Membership Level Id for api2 fetch User Balance is matching i.e. " + userBalanceApiMembershipLevelId);
		TestListeners.extentTest.get().pass(
				"Membership Level Id for api2 fetch User Balance is matching i.e. " + userBalanceApiMembershipLevelId);
		logger.info("Membership Level name for api2 fetch User Balance is matching i.e. "
				+ userBalanceApiMembershipLevelName);
		TestListeners.extentTest.get().pass("Membership Level name for api2 fetch User Balance is matching i.e. "
				+ userBalanceApiMembershipLevelName);

		// Hit api2/mobile/checkins/account_balance and verify balance
		Response checkinAccountBalanceResponse = pageObj.endpoints().Api2CheckinAccountBalance(client, secret, token);
		Assert.assertEquals(checkinAccountBalanceResponse.getStatusCode(), 200);
		logger.info("Api2 Fetch checkin account balance is successful");
		TestListeners.extentTest.get().info("Api2 Fetch checkin account balance is successful");

		int checkinAccountBalanceApiPointsBalance = Integer.parseInt(checkinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int checkinAccountBalanceApiRedeemablePoints = Integer.parseInt(checkinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		String checkinAccountBalanceApiMembershipLevelId = checkinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_id").toString();
		String checkinAccountBalanceApiMembershipLevelName = checkinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.current_membership_level_name").toString();
		Assert.assertEquals(checkinAccountBalanceApiPointsBalance, actualPoints,
				"Points balance for api2 fetch checkin Account balance is not matching");
		Assert.assertEquals(checkinAccountBalanceApiRedeemablePoints, actualPoints,
				"Redeemable points for api2 fetch checkin Account balance is not matching");
		Assert.assertEquals(checkinAccountBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Current Membership Level Id for api2 fetch checkin Account balance is not matching");
		Assert.assertEquals(checkinAccountBalanceApiMembershipLevelName, dataSet.get("expectedMembership"),
				"Current Membership Level Name for api2 fetch checkin Account balance is not matching");

		logger.info("Redeemable Points and Point Balance for api2 fetch checkin Account balance is matching i.e. "
				+ actualPoints);
		TestListeners.extentTest.get()
				.pass("Redeemable Points and Point Balance for api2 fetch checkin Account balance is matching i.e "
						+ actualPoints);
		logger.info("Membership Level Id for api2 fetch checkin Account Balance is matching i.e. "
				+ checkinAccountBalanceApiMembershipLevelId);
		TestListeners.extentTest.get()
				.pass("Membership Level Id for api2 fetch checkin Account Balance is matching i.e. "
						+ checkinAccountBalanceApiMembershipLevelId);
		logger.info("Membership Level Name for api2 fetch checkin Account Balance is matching i.e. "
				+ checkinAccountBalanceApiMembershipLevelName);
		TestListeners.extentTest.get()
				.pass("Membership Level Name for api2 fetch checkin Account Balance is matching i.e. "
						+ checkinAccountBalanceApiMembershipLevelName);

		// === OLF-T2188 ===

		// Hi api/v1/checkins and verify response
		Response checkinsV1ApiResponse = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsV1ApiResponse.getStatusCode(), 200);
		logger.info("Api V1 checkins Balance is successful");
		TestListeners.extentTest.get().info("Api V1 checkins is successful");

		String checkinsV1ApiPointsEarned1 = checkinsV1ApiResponse.jsonPath().get("[0].points_earned").toString()
				.replace(".0", "");
		String checkinsV1ApiPointsEarned2 = checkinsV1ApiResponse.jsonPath().get("[1].points_earned").toString()
				.replace(".0", "");
		int checkinsV1ApiPointsEarnedTotal = Integer.parseInt(checkinsV1ApiPointsEarned1)
				+ Integer.parseInt(checkinsV1ApiPointsEarned2);
		Assert.assertEquals(checkinsV1ApiPointsEarnedTotal, points_earned, "Points earned total is not matching");
		logger.info(
				"Verified that points_earned value is correct i.e. " + points_earned + " in V1 checkins Api response");
		TestListeners.extentTest.get().pass(
				"Verified that points_earned value is correct i.e. " + points_earned + " in V1 checkins Api response");

		String checkinsV1ApiPointsSpent1 = checkinsV1ApiResponse.jsonPath().get("[0].points_spent").toString()
				.replace(".0", "");
		String checkinsV1ApiPointsSpent2 = checkinsV1ApiResponse.jsonPath().get("[1].points_spent").toString()
				.replace(".0", "");
		int checkinsV1ApiPointsSpentTotal = Integer.parseInt(checkinsV1ApiPointsSpent1)
				+ Integer.parseInt(checkinsV1ApiPointsSpent2);
		Assert.assertEquals(checkinsV1ApiPointsSpentTotal, points_spent, "Points spent total is not matching");
		logger.info(
				"Verified that points_spent value is correct i.e. " + points_spent + " in V1 checkins Api response");
		TestListeners.extentTest.get().pass(
				"Verified that points_spent value is correct i.e. " + points_spent + " in V1 checkins Api response");

		String checkinsV1ApiPointsAvailable1 = checkinsV1ApiResponse.jsonPath().get("[0].points_available").toString()
				.replace(".0", "");
		String checkinsV1ApiPointsAvailable2 = checkinsV1ApiResponse.jsonPath().get("[1].points_available").toString()
				.replace(".0", "");
		int checkinsV1ApiPointsAvailableTotal = Integer.parseInt(checkinsV1ApiPointsAvailable1)
				+ Integer.parseInt(checkinsV1ApiPointsAvailable2);
		int checkinV1ApiPointsAvailableTotalExpected = checkinsV1ApiPointsEarnedTotal - checkinsV1ApiPointsSpentTotal;
		Assert.assertEquals(checkinsV1ApiPointsAvailableTotal, checkinV1ApiPointsAvailableTotalExpected,
				"Points available total is not matching");
		Assert.assertEquals(checkinsV1ApiPointsAvailableTotal, actualPoints, "Points available total is not matching");
		logger.info("Verified that points_available value is correct i.e. " + actualPoints
				+ " in V1 checkins Api response");
		TestListeners.extentTest.get().pass("Verified that points_available value is correct i.e. " + actualPoints
				+ " in V1 checkins Api response");

		// Hit api/v1/checkins?transaction_id=transaction_id/transactions API and verify
		// response (REMAINING)
	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2189 [New ML redemption mark is less than old] Verify user balance in different Auth API's when business configuration has membership level Redemption Mark configured")
	public void T2189_verifyAuthApi() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from DB
		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		TestListeners.extentTest.get()
				.info("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Hit api/auth/users/balance API and verify response
		Response userBalanceApiResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(userBalanceApiResponse.getStatusCode(), 200);
		logger.info("Auth User Balance is successful");
		TestListeners.extentTest.get().info("Auth User Balance is successful");

		int userBalanceApiPointsBalance = Integer.parseInt(
				userBalanceApiResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int userBalanceApiRedeemablePoints = Integer.parseInt(userBalanceApiResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		String userBalanceApiMembershipLevelId = userBalanceApiResponse.jsonPath()
				.get("account_balance.current_membership_level_id").toString();
		String userBalanceApiMembershipLevelName = userBalanceApiResponse.jsonPath()
				.get("account_balance.current_membership_level_name").toString();

		Assert.assertEquals(userBalanceApiPointsBalance, actualPoints, "Point Balance is not matching");
		Assert.assertEquals(userBalanceApiRedeemablePoints, actualPoints, "Redeemable Points is not matching");
		logger.info("Point Balance and Redeemable Points in Auth User balance Api are matching the expected result");
		TestListeners.extentTest.get()
				.pass("Point Balance and Redeemable Points in Auth User balance Api are matching the expected result");
		Assert.assertEquals(userBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Current membership level Id is not matching");
		Assert.assertEquals(userBalanceApiMembershipLevelName, dataSet.get("expectedMembership"),
				"Current membership level name is not matching");
		logger.info("Current membership level id and name are matching the expected result");
		TestListeners.extentTest.get().pass("Current membership level id and name are matching the expected result");

		// Hit api/auth/checkins/balance API and verify response
		Response checkinBalanceApiResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(checkinBalanceApiResponse.getStatusCode(), 200);
		logger.info("Auth Api Fetch checkins balance is successful");
		TestListeners.extentTest.get().pass("Auth Api Fetch checkins balance is successful");
		int checkinBalanceApiPointsBalance = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		Assert.assertEquals(checkinBalanceApiPointsBalance, actualPoints, "Points balance is not equal");
		logger.info("Verified that points balance is equal to " + actualPoints + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get().pass(
				"Verified that points balance is equal to " + actualPoints + " for Auth Api Fetch checkin balance");

		int checkinBalanceApiTotalCredits = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(checkinBalanceApiTotalCredits, points_earned, "Total credits is not equal");
		logger.info("Verified that total credits is equal to " + points_earned + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get().pass(
				"Verified that total credits is equal to " + points_earned + " for Auth Api Fetch checkin balance");

		int checkinBalanceApiTotalDebits = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(checkinBalanceApiTotalDebits, points_spent, "Total debits is not equal");
		logger.info("Verified that total debits is equal to " + points_spent + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get()
				.pass("Verified that total debits is equal to " + points_spent + " for Auth Api Fetch checkin balance");

		int checkinBalanceApiBankedRewards = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(checkinBalanceApiBankedRewards, actualPoints, "Banked rewards is not equal");
		logger.info("Verified that banked_rewards is equal to " + actualPoints + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get().pass(
				"Verified that banked_rewards is equal to " + actualPoints + " for Auth Api Fetch checkin balance");

		String checkinBalanceApiMembershipLevel = checkinBalanceApiResponse.jsonPath().get("membership_level")
				.toString();
		Assert.assertEquals(checkinBalanceApiMembershipLevel, dataSet.get("expectedMembership"),
				"Membership level is not equal");
		logger.info("Verified that membership_level is equal to " + dataSet.get("expectedMembership")
				+ " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get().pass("Verified that membership_level is equal to "
				+ dataSet.get("expectedMembership") + " for Auth Api Fetch checkin balance");

		String checkinBalanceApiMembershipLevelId = checkinBalanceApiResponse.jsonPath().get("membership_level_id")
				.toString();
		Assert.assertEquals(checkinBalanceApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Membership level id is not equal");
		logger.info("Verified that membership_level_id is equal to " + dataSet.get("expectedMembershipId")
				+ " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get().pass("Verified that membership_level_id is equal to "
				+ dataSet.get("expectedMembershipId") + " for Auth Api Fetch checkin balance");

		int checkinBalanceApiNetBalance = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("net_balance").toString().replace(".0", ""));
		Assert.assertEquals(checkinBalanceApiNetBalance, actualPoints, "Net balance is not equal");
		logger.info("Verified that net_balance is equal to " + actualPoints + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get()
				.pass("Verified that net_balance is equal to " + actualPoints + " for Auth Api Fetch checkin balance");

		int checkinBalanceApiNetDebits = Integer
				.parseInt(checkinBalanceApiResponse.jsonPath().get("net_debits").toString().replace(".0", ""));
		Assert.assertEquals(checkinBalanceApiNetDebits, points_spent, "Net debits is not equal");
		logger.info("Verified that net_debits is equal to " + points_spent + " for Auth Api Fetch checkin balance");
		TestListeners.extentTest.get()
				.pass("Verified that net_debits is equal to " + points_spent + " for Auth Api Fetch checkin balance");

	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2190 [New ML redemption mark is less than old] Verify user balance in dashboard API when business configuration has membership level Redemption Mark configured")
	public void T2190_verifyDashboardApi() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from checkins table
		String pointsSpentQuery = "SELECT SUM(`points_spent`) AS pointsSpentAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to " + pointsSpent);
		TestListeners.extentTest.get().info("points_spent column in checkins table is equal to " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) AS pointsEarnedAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to " + pointsEarned);
		TestListeners.extentTest.get().info("points_earned column in checkins table is equal to " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2190 ===

		// Hit api2/dashboard/users/info API and verify response
		long phone = (long) (Math.random() * Math.pow(10, 10));
		Response userInfoApiResponse = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, "");
		Assert.assertEquals(userInfoApiResponse.getStatusCode(), 200);
		logger.info("Dashboard API User Look Up (email only) is successful");
		TestListeners.extentTest.get().pass("Dashboard API User Look Up (email only) is successful");

		int userInfoApiPointsBalance = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		Assert.assertEquals(userInfoApiPointsBalance, actualPoints, "Points balance is not equal");
		logger.info("Verified that points balance is equal to " + actualPoints + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that points balance is equal to " + actualPoints + " for Dashboard user info");

		int userInfoApiTotalCredits = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(userInfoApiTotalCredits, points_earned, "Total credits is not equal");
		logger.info("Verified that total credits is equal to " + points_earned + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that total credits is equal to " + points_earned + " for Dashboard user info");

		int userInfoApiTotalDebits = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(userInfoApiTotalDebits, points_spent, "Total debits is not equal");
		logger.info("Verified that total debits is equal to " + points_spent + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that total debits is equal to " + points_spent + " for Dashboard user info");

		int userInfoApiBankedRewards = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(userInfoApiBankedRewards, actualPoints, "Banked rewards is not equal");
		logger.info("Verified that banked_rewards is equal to " + actualPoints + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that banked_rewards is equal to " + actualPoints + " for Dashboard user info");

		String userInfoApiMembershipLevel = userInfoApiResponse.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(userInfoApiMembershipLevel, dataSet.get("expectedMembership"),
				"Membership level is not equal");
		logger.info("Verified that membership_level is equal to " + dataSet.get("expectedMembership")
				+ " for Dashboard user info");
		TestListeners.extentTest.get().pass("Verified that membership_level is equal to "
				+ dataSet.get("expectedMembership") + " for Dashboard user info");

		String userInfoApiMembershipLevelId = userInfoApiResponse.jsonPath().get("balance.membership_level_id")
				.toString();
		Assert.assertEquals(userInfoApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Membership level id is not equal");
		logger.info("Verified that membership_level_id is equal to " + dataSet.get("expectedMembershipId")
				+ " for Dashboard user info");
		TestListeners.extentTest.get().pass("Verified that membership_level_id is equal to "
				+ dataSet.get("expectedMembershipId") + " for Dashboard user info");

		int userInfoApiNetBalance = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		Assert.assertEquals(userInfoApiNetBalance, actualPoints, "Net balance is not equal");
		logger.info("Verified that net_balance is equal to " + actualPoints + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that net_balance is equal to " + actualPoints + " for Dashboard user info");

		int userInfoApiNetDebits = Integer
				.parseInt(userInfoApiResponse.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		Assert.assertEquals(userInfoApiNetDebits, points_spent, "Net debits is not equal");
		logger.info("Verified that net_debits is equal to " + points_spent + " for Dashboard user info");
		TestListeners.extentTest.get()
				.pass("Verified that net_debits is equal to " + points_spent + " for Dashboard user info");

	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2191 [New ML redemption mark is less than old] Verify user balance in pos/checkin API when business configuration has membership level Redemption Mark configured")
	public void T2191_verifyPosApi() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// Get the user points balance from checkins table
		String pointsSpentQuery = "SELECT SUM(`points_spent`) AS pointsSpentAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to " + pointsSpent);
		TestListeners.extentTest.get().info("points_spent column in checkins table is equal to " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) AS pointsEarnedAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to " + pointsEarned);
		TestListeners.extentTest.get().info("points_earned column in checkins table is equal to " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2191 ===

		// Verify POS checkins API response
		int posCheckinApiPointsBalance = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		Assert.assertEquals(posCheckinApiPointsBalance, actualPoints, "Points balance is not equal");
		logger.info("Verified that points balance is equal to " + actualPoints + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that points balance is equal to " + actualPoints + " for Pos Checkin");

		int posCheckinApiTotalCredits = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(posCheckinApiTotalCredits, points_earned, "Total credits is not equal");
		logger.info("Verified that total credits is equal to " + points_earned + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that total credits is equal to " + points_earned + " for Pos Checkin");

		int posCheckinApiTotalDebits = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(posCheckinApiTotalDebits, points_spent, "Total debits is not equal");
		logger.info("Verified that total debits is equal to " + points_spent + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that total debits is equal to " + points_spent + " for Pos Checkin");

		int posCheckinApiBankedRewards = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.banked_rewards").toString().replace(".00", ""));
		Assert.assertEquals(posCheckinApiBankedRewards, actualPoints, "Banked rewards is not equal");
		logger.info("Verified that banked_rewards is equal to " + actualPoints + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that banked_rewards is equal to " + actualPoints + " for Pos Checkin");

		String posCheckinApiMembershipLevel = checkinResponse2.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(posCheckinApiMembershipLevel, dataSet.get("expectedMembership"),
				"Membership level is not equal");
		logger.info(
				"Verified that membership_level is equal to " + dataSet.get("expectedMembership") + " for Pos Checkin");
		TestListeners.extentTest.get().pass(
				"Verified that membership_level is equal to " + dataSet.get("expectedMembership") + " for Pos Checkin");

		String posCheckinApiMembershipLevelId = checkinResponse2.jsonPath().get("balance.membership_level_id")
				.toString();
		Assert.assertEquals(posCheckinApiMembershipLevelId, dataSet.get("expectedMembershipId"),
				"Membership level id is not equal");
		logger.info("Verified that membership_level_id is equal to " + dataSet.get("expectedMembershipId")
				+ " for Pos Checkin");
		TestListeners.extentTest.get().pass("Verified that membership_level_id is equal to "
				+ dataSet.get("expectedMembershipId") + " for Pos Checkin");

		int posCheckinApiNetBalance = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.net_balance").toString().replace(".0", ""));
		Assert.assertEquals(posCheckinApiNetBalance, actualPoints, "Net balance is not equal");
		logger.info("Verified that net_balance is equal to " + actualPoints + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that net_balance is equal to " + actualPoints + " for Pos Checkin");

		int posCheckinApiNetDebits = Integer
				.parseInt(checkinResponse2.jsonPath().get("balance.net_debits").toString().replace(".0", ""));
		Assert.assertEquals(posCheckinApiNetDebits, points_spent, "Net debits is not equal");
		logger.info("Verified that net_debits is equal to " + points_spent + " for Pos Checkin");
		TestListeners.extentTest.get()
				.pass("Verified that net_debits is equal to " + points_spent + " for Pos Checkin");

	}

	@SuppressWarnings("static-access")
	@Test(description = "OLF-T2192 [New ML redemption mark is less than old] Verify user details in accounts table when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2193 [New ML redemption mark is less than old] Verify user details in checkins table when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2194 [New ML redemption mark is less than old] Verify PointDebits in reward_debits table when business configuration has membership level Redemption Mark configured; "
			+ "OLF-T2195 [New ML redemption mark is less than old] Verify user details in membership_level_histories table when business configuration has membership level Redemption Mark configured")
	public void T2192_verifyUserDetailsTable() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Old Redemption Mark -> 100 (No banking rules configured)
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set Membership level minimum and maximum points (all levels) And
		// Old membership level redemption mark -> 100 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkOld"));
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get().info(dataSet.get("membership" + i) + " is successfully updated");
		}

		// Set "Has Membership Levels?" & "Membership Level Bump on Edge?" ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User Sign-up using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String userID = signUpResponse.jsonPath().get("id").toString();
		logger.info("User Sign-up is successful for user_id: " + userID);
		TestListeners.extentTest.get().info("User Sign-up is successful for user_id: " + userID);

		// Do check-in of 59 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount1") + " points");

		// New membership level redemption mark -> 60 (all levels)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMarkNew"));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
			TestListeners.extentTest.get()
					.info("This membership: " + dataSet.get("membership" + i) + " is successfully updated");
		}

		// Do check-in of 1 point
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), 200);
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount2") + " points");

		// === OLF-T2193 ===

		// checkins table validations
		String checkinsTablePointsEarnedQuery = "SELECT SUM(`points_earned`) AS pointsEarnedAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				checkinsTablePointsEarnedQuery, "pointsEarnedAmount", 5);
		int points_earned = Integer.parseInt(pointsEarned);
		int totalPoints = Integer.parseInt(dataSet.get("amount1")) + Integer.parseInt(dataSet.get("amount2"));
		Assert.assertEquals(points_earned, totalPoints, "points_earned value is not equal to total checkin points");
		logger.info("points_earned column in checkins table is equal to expected value " + totalPoints);
		TestListeners.extentTest.get()
				.pass("points_earned column in checkins table is equal to expected value " + totalPoints);

		String checkinsTablePointsSpentQuery = "SELECT SUM(`points_spent`) AS pointsSpentAmount FROM `checkins` WHERE `user_id` = '"
				+ userID + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				checkinsTablePointsSpentQuery, "pointsSpentAmount", 5);
		int spentPoints = Integer.parseInt(dataSet.get("redemptionMarkNew"));
		int points_spent = Integer.parseInt(pointsSpent);
		Assert.assertEquals(points_spent, spentPoints,
				"points_spent column in checkins table is not equal to the spent points");
		logger.info("points_spent column in checkins table is equal to expected value " + spentPoints);
		TestListeners.extentTest.get()
				.pass("points_spent column in checkins table is equal to expected value " + spentPoints);

		int points_expired = 0; // until we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// === OLF-T2192 ===

		// accounts table validations
		String accountsTableTotalDebitsQuery = "SELECT `total_debits` FROM `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountsTableTotalDebitsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountsTableTotalDebitsQuery, "total_debits", 5);
		int totalDebits = Integer.parseInt(accountsTableTotalDebitsValue.replace(".00", ""));
		Assert.assertEquals(totalDebits, spentPoints,
				"total_debits column in accounts table is not equal to the spent points");
		logger.info("total_debits column in accounts table is equal to expected value i.e. " + spentPoints);
		TestListeners.extentTest.get()
				.pass("total_debits column in accounts table is equal to expected value i.e. " + spentPoints);

		String accountsTableTotalCreditsQuery = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountsTableTotalCreditsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountsTableTotalCreditsQuery, "total_credits", 5);
		int totalCredits = Integer.parseInt(accountsTableTotalCreditsValue.replace(".0", ""));
		Assert.assertEquals(totalCredits, totalPoints,
				"total_credits column in accounts table is not equal to the total checkin points");
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + totalPoints);
		TestListeners.extentTest.get()
				.pass("total_credits column in accounts table is equal to expected value i.e. " + totalPoints);

		String accountsTableMembershipLevelQuery = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '"
				+ userID + "';";
		String accountsTableMembershipLevel = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, accountsTableMembershipLevelQuery, "membership_level", 5);
		Assert.assertEquals(accountsTableMembershipLevel, dataSet.get("expectedMembership"),
				"membership_level column in accounts table is not equal to expected value");
		logger.info("membership_level column in accounts table is equal to expected value i.e. "
				+ dataSet.get("expectedMembership"));
		TestListeners.extentTest.get().pass("membership_level column in accounts table is equal to expected value i.e. "
				+ dataSet.get("expectedMembership"));

		// === OLF-T2194 ===

		// reward_debits table validation
		String rewardsDebitsTableQuery = "SELECT `type` FROM `reward_debits` WHERE `user_id` ='" + userID + "';";
		String rewardsDebitsTableValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				rewardsDebitsTableQuery, "type", 5);
		Assert.assertEquals(rewardsDebitsTableValue, "PointDebit",
				"Type column in reward_debits table is not equal to 'PointDebit'");
		logger.info("Type column in reward_debits table is equal to 'PointDebit'");
		TestListeners.extentTest.get().pass("Type column in reward_debits table is equal to 'PointDebit'");

		// === OLF-T2195 ===

		// membership_level_histories table validations

		// effective
		String membershipLevelIdQuery1 = "SELECT `membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "';";
		String effectiveMembershipId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				membershipLevelIdQuery1, "membership_level_id", 10);
		Assert.assertEquals(effectiveMembershipId, dataSet.get("expectedMembershipId"),
				"Effective membership strategy is not as expected");
		logger.info("Verified that Effective membership strategy is " + dataSet.get("expectedMembershipId"));
		TestListeners.extentTest.get()
				.pass("Verified that Effective membership strategy is " + dataSet.get("expectedMembershipId"));

		// operative
		String membershipLevelIdQuery2 = "SELECT `operative_membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "';";
		String operativeMembershipId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				membershipLevelIdQuery2, "operative_membership_level_id", 10);
		Assert.assertEquals(operativeMembershipId, dataSet.get("expectedMembershipId"),
				"Operative membership strategy is not as expected");
		logger.info("Verified that Operative membership strategy is " + dataSet.get("expectedMembershipId"));
		TestListeners.extentTest.get()
				.pass("Verified that Operative membership strategy is " + dataSet.get("expectedMembershipId"));

		// previous membership level
		String membershipLevelIdQuery3 = "SELECT `previous_membership_level_id` FROM `membership_level_histories` WHERE `user_id` = '"
				+ userID + "';";
		String previousMembershipId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				membershipLevelIdQuery3, "previous_membership_level_id", 5);
		Assert.assertNull(previousMembershipId, "Previous_membership_level_id is not NULL");
		logger.info("Verified that previous_membership_level_id is NULL");
		TestListeners.extentTest.get().pass("Verified that previous_membership_level_id is NULL");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
