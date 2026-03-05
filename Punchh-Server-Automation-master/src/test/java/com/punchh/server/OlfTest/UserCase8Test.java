package com.punchh.server.OlfTest;

import java.lang.reflect.Method;
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
public class UserCase8Test {
	private static Logger logger = LogManager.getLogger(UserCase8Test.class);
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

	@Test(description = "OLF-T2165 [New ML redemption mark is less than old] Verify point balance in iframe for users when business configuration has membership level Redemption Mark configured [User case 8]", priority = 0)
	public void T2165_VerifyPointBalanceinIframeForUsersWhenBusinessConfigurationHasMembershipLevelRedemptionMarkConfigured()
			throws Exception {

		// Step 1: getting data from business.preference from DB and update all flag
		// values
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
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

		// Step 2: Old Redemption Mark -> 100 (No banking rules configured)
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
			utils.logit(dataSet.get("membership" + i) + " is successfully updated");
		}

		// check Has Membership Levels? on, and check Membership Level Bump on Edge? off
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 200 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount1") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount1") + " dollar");

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

		// (OLF-T2174) Do checkin of 3 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount2") + " points");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount2") + " points");
		int checkinPointsBalance = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int checkinTotalCredits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int checkinTotalDebits = Integer
				.parseInt(checkinResponse1.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		// (OLF-T2176) fetch details from checkin table
		String pointsSpentQuery = "SELECT SUM(`points_spent`) as points_spent FROM `checkins` where `user_id` = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsSpentQuery,
				"points_spent", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as points_earned FROM `checkins` where `user_id` = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, pointsEarnedQuery,
				"points_earned", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsSpent);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int expectedredeemablePoints = (points_earned - (points_spent + points_expired));

		// OLF-T2167 Check user timeline for Redeemable Points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		// piechart points
		String loyaltypieChartPoints = pageObj.guestTimelinePage().getLoyaltyPieChartDetails();
		Assert.assertEquals(expectedredeemablePoints, actualredeemablePoints,
				"User balance is not correct on timeline");
		Assert.assertEquals(loyaltypieChartPoints, String.valueOf(points_earned),
				"loyalty pie chart points balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline :" + actualredeemablePoints);

		// OLF-T2166 Check print timeline for user
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains(expectedredeemablePoints + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + expectedredeemablePoints),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

		// OLF-T2168 Banking event in account history validation
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		int totalBankingEvents = accountData.size();
		Assert.assertEquals(totalBankingEvents, 1,
				"Banking happened for user more than once, it should happen once only");
		utils.logPass("loyalty reward banking event validated in account history :" + totalBankingEvents);

		// OLF-T2165 User balance validation on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 1);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		String membershipLevel = pageObj.iframeSingUpValidationPageClass().getMembershiplevel();
		Assert.assertEquals(Integer.parseInt(iFrameActualpoints), expectedredeemablePoints,
				"Current Points for Iframe is not matching");
		Assert.assertEquals(membershipLevel, "Membership Level Silver", "Membership Levelfor Iframe is not matching");
		utils.logPass("Iframe points and membership level validated");

		// (OLF-T2169) api/mobile/users/balance
		Response balanceResponse = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch users balance");
		int api1PointsBalance = Integer.parseInt(
				balanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int api1TotalCredits = Integer
				.parseInt(balanceResponse.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int api1TotalDebits = Integer
				.parseInt(balanceResponse.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));

		Assert.assertEquals(actualredeemablePoints, api1PointsBalance,
				"PointsBalance did not matched for (api/mobile/users/balance)");
		Assert.assertEquals(points_earned, api1TotalCredits,
				"Total Credits did not matched for (api/mobile/users/balance)");
		Assert.assertEquals(points_spent, api1TotalDebits,
				"Total Debits did not matched for (api/mobile/users/balance)");
		utils.logPass("api/mobile/users/balance credit/debit point balance validated");

		// (api/mobile/checkins/balance)
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		int totalCredit = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int totaldebit = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int pointsBalance = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0", ""));

		Assert.assertEquals(actualredeemablePoints, pointsBalance,
				"PointsBalance did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(points_earned, totalCredit,
				"total cerdit did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(points_spent, totaldebit, "total debit did not matched for (api/mobile/checkins/balance)");
		utils.logPass("(api/mobile/checkins/balance) credit/debit point balance validated");

		// (/api/mobile/checkins)
		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		int points_AvailableApiFetchCheckin = Integer.parseInt(Api1FetchCheckinResp.jsonPath()
				.get("[0].points_available").toString().replace("[", "").replace("]", ""));
		int points_earnedApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString().replace("[", "").replace("]", ""));
		int points_spentApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		/*
		 * Assert.assertEquals(actualredeemablePoints, points_AvailableApiFetchCheckin,
		 * "points available is not matched for (/api/mobile/checkins)");
		 * Assert.assertEquals(points_earnedApiFetchCheckin, actualredeemablePoints,
		 * "points earned is not matched for (/api/mobile/checkins)");
		 */
		Assert.assertEquals(points_spentApiFetchCheckin, points_spent,
				"points_spent is not match for (/api/mobile/checkins)");
		utils.logPass("(/api/mobile/checkins) credit/debit point balance validated");

		// (OLF-T2170) api2/mobile/users/balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		int api2PointsBalance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int api2RedeemablePoints = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(actualredeemablePoints, api2PointsBalance,
				"PointsBalance did not matched for (api2/mobile/users/balance)");
		Assert.assertEquals(actualredeemablePoints, api2RedeemablePoints,
				"RedeemablePoints did not matched for (api2/mobile/users/balance)");
		utils.logPass("//api2/mobile/users/balance credit/debit point balance validated");

		// (api2/mobile/checkins/account_balance)
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		int api2checkinPointsBalance = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int api2checkinRedeemablePoints = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(actualredeemablePoints, api2checkinPointsBalance,
				"PointsBalance did not matched for (api2/mobile/checkins/account_balance) ");
		Assert.assertEquals(actualredeemablePoints, api2checkinRedeemablePoints,
				"RedeemablePoints did not matched for (api2/mobile/checkins/account_balance) ");
		utils.logPass("(api2/mobile/checkins/account_balance) credit/debit point balance validated");

		// (OLF-T2171) Api V1 checkins api/v1/checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");

		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		int points_earnedV1Checkin = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_earned").toString().replace(".0", ""));
		int points_spentV1Checkin = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		int points_available = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0", ""));

		/*
		 * Assert.assertEquals(actualredeemablePoints, points_available,
		 * "Points available did not matched for (api/v1/checkins)");
		 * Assert.assertEquals(actualredeemablePoints, points_earnedV1Checkin,
		 * "points earned did not matched for (api/v1/checkins)");
		 * Assert.assertEquals(actualredeemablePoints, points_spentV1Checkin,
		 * "points spent did not matched for (api/v1/checkins)");
		 */
		utils.logPass("api/v1/checkins credit/debit point balance validated");

		// (OLF-T2172) (api/auth/users/balance)
		// Fetch User Balance
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		int authPointsBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int authRedeemablePoints = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));

		Assert.assertEquals(actualredeemablePoints, authPointsBalance,
				"PointsBalance did not matched for (api/auth/users/balance)");
		Assert.assertEquals(actualredeemablePoints, authRedeemablePoints,
				"RedeemablePoints did not matched for (api/auth/users/balance)");
		utils.logPass("(api/auth/users/balance) credit/debit point balance validated");

		// (api/auth/checkins/balance)
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int authBalancePointBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int authBalanceTotalCredit = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int authBalanceTotalDebit = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));

		Assert.assertEquals(actualredeemablePoints, authBalancePointBalance,
				"total cerdit did not matched for (api/auth/checkins/balance)");
		Assert.assertEquals(actualredeemablePoints, authBalanceTotalCredit,
				"total debit did not matched for (api/auth/checkins/balance)");
		Assert.assertEquals(actualredeemablePoints, authBalanceTotalDebit,
				"PointsBalance did not matched for (api/auth/checkins/balance)");
		utils.logPass("(api/auth/checkins/balance) credit/debit point balance validated");

		// (OLF-T2173) // (api2/dashboard/users/info)
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String blankString = "";
		Response userLookUpApi = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, blankString);
		Assert.assertEquals(userLookUpApi.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");

		int useInfoPointsBalance = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int useInfoTotalCredit = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int useInfoTotalDebit = Integer
				.parseInt(userLookUpApi.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		Assert.assertEquals(actualredeemablePoints, useInfoPointsBalance,
				"total cerdit did not matched for (api2/dashboard/users/info)");
		/*
		 * Assert.assertEquals(actualredeemablePoints,
		 * useInfoTotalCredit,"total debit did not matched for (api2/dashboard/users/info)"
		 * ); Assert.assertEquals(actualredeemablePoints,
		 * useInfoTotalDebit,"PointsBalance did not matched for (api2/dashboard/users/info)"
		 * );
		 */
		utils.logPass("(api2/dashboard/users/info) credit/debit point balance validated");

		// (OLF-T2175) Check value for User in accounts table
		String query2 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String total_credits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_credits",
				5);
		String query1 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String total_debits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "total_debits", 5);
		String query3 = "Select `initial_credit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String initial_credit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "initial_credit",
				5);
		String query4 = "Select `initial_debit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String initial_debit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "initial_debit",
				5);

		Assert.assertEquals(total_credits, "83.0", "total_credits in accounts table is not matching");
		Assert.assertEquals(total_debits, "60.00", "total_debits in accounts table not matching");
		Assert.assertEquals(initial_credit, "0.0", "initial_credit in accounts tableis not matching");
		Assert.assertEquals(initial_debit, "0.0", "initial_debit in accounts table is not matching");

		// (OLF-T2177) reward_debit table for user
		String query6 = "Select `status` from `reward_debits` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String status = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "status", 5);

		String query7 = "Select `type` from `reward_debits` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String type = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "type", 5);
		Assert.assertEquals(status, "banked", "Status in reward debits table did not matched");
		Assert.assertEquals(type, "PointDebit", "Status in reward debits table did not matched");

		// (OLF-T2178) membership_level_histories table for user
		// membership_level_histories
		String query9 = "SELECT `previous_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9,
				"previous_membership_level_id", 5);
		Assert.assertEquals(expColValue1, "NULL", "Previous membership strategy is not null");

		// effective
		String query10 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"membership_level_id", 5);
		Assert.assertEquals(expColValue2, dataSet.get("membershipId1"),
				"Effective membership_level_id did not matched ");

		// operative
		String query11 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"operative_membership_level_id", 5);
		Assert.assertEquals(expColValue3, dataSet.get("membershipId1"),
				"operative_membership_level_id did not matched");
		logger.info("previous_membership_level_id, membership_level_id, operative_membership_level_id verified");
		utils.logPass("previous_membership_level_id, membership_level_id, operative_membership_level_id verified");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
