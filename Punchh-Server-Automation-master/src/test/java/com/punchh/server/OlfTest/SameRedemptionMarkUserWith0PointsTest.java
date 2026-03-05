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

public class SameRedemptionMarkUserWith0PointsTest {
	private static Logger logger = LogManager.getLogger(SameRedemptionMarkUserWith0PointsTest.class);
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

// Amit
	@Test(description = "OLF-T1926,OLF-T1929,OLF-T1930 [Business having same redemption mark] Verify user balance on user timeline for user having 0 points", priority = 0)
	public void T1926_verifyUserBalanceOnUserTimelineForUserHaving0Points() throws Exception {

		// Step 1: getting data from business.preference from DB
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
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));

		// Checking and Marking "enable_points_spent_backfill" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// Step:2 login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Signup using mobile api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("api1 Signup is successful");
		logger.info("api1 Signup is successful");

		// Step -2 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(actualredeemablePoints, 0, "User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline :" + actualredeemablePoints);

		// OLF-T1929 [Business having same redemption mark] Verify user balance on print
		// time line
		// for user having 0 points
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("0 Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + "0"),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(0 Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

		// Banking event in account history validation
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		Assert.assertTrue(accountData.size() == 0, "Banking happened for user having 0 points, it should not happen");

		// OLF-T1930 [Business having same redemption mark] Verify user balance on
		// iframe for user having 0 point
		// User balance is correct on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		Assert.assertEquals(iFrameActualpoints, "0", "Current Points for Iframe is not matching");
		logger.info("Iframe points validated");
		utils.logPass("Iframe points validated");

		// OLF-T1928 [Business having same redemption mark] Verify user details in
		// accounts table for user having 0 points
		String query2 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String total_credits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"total_credits", 5);
		String query1 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String total_debits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"total_debits", 5);
		String query3 = "Select `initial_credit` from `accounts` where `user_id` = '" + userID + "';";
		String initial_credit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"initial_credit", 5);
		String query4 = "Select `initial_debit` from `accounts` where `user_id` = '" + userID + "';";
		String initial_debit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"initial_debit", 5);

		Assert.assertEquals(total_credits, null, "total_credits in accounts table is not matching");
		Assert.assertEquals(total_debits, null, "total_debits in accounts table not matching");
		Assert.assertEquals(initial_credit, "0.0", "initial_credit in accounts tableis not matching");
		Assert.assertEquals(initial_debit, "0.0", "initial_debit in accounts table is not matching");

		// OLF-T1931 [Business having same redemption mark] Verify points_debits in
		// reward_debit table for user having 0 points
		String query6 = "Select `status` from `reward_debits` where `user_id` = '" + userID + "';";
		String status = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "status",
				5);
		Assert.assertEquals(status, "", "Status in reward debits table did not matched");

		// OLF-T1927 [Business having same redemption mark] Verify user details in
		// checkins table for user having 0 points
		String pointsEarnedQuery = "Select `points_earned` from `checkins` where `user_id` = '" + userID + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "points_earned", 5);
		Assert.assertEquals(pointsEarned, "", "pointsEarned in accounts table did not matched");

		// OLF-T1932 [Business having same redemption mark] Verify user balance in API's
		// for user having 0 points

		// api/mobile/users/balance
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
		Assert.assertEquals(actualredeemablePoints, api1TotalCredits,
				"Total Credits did not matched for (api/mobile/users/balance)");
		Assert.assertEquals(actualredeemablePoints, api1TotalDebits,
				"Total Debits did not matched for (api/mobile/users/balance)");

		// api2/mobile/users/balance
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

		// (api/auth/users/balance)
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
		Assert.assertEquals(actualredeemablePoints, totalCredit,
				"total cerdit did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(actualredeemablePoints, totaldebit,
				"total debit did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(actualredeemablePoints, pointsBalance,
				"PointsBalance did not matched for (api/mobile/checkins/balance)");

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

		// (api2/dashboard/users/info)
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
		Assert.assertEquals(actualredeemablePoints, useInfoTotalCredit,
				"total debit did not matched for (api2/dashboard/users/info)");
		Assert.assertEquals(actualredeemablePoints, useInfoTotalDebit,
				"PointsBalance did not matched for (api2/dashboard/users/info)");

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

		// (/api/mobile/checkins)
		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		/*
		 * int points_earnedApiFetchCheckin = Integer.parseInt(
		 * Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString().replace(
		 * "[", "").replace("]", "")); int points_spentApiFetchCheckin =
		 * Integer.parseInt(
		 * Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace(
		 * "[", "").replace("]", "")); Assert.assertEquals(points_earnedApiFetchCheckin,
		 * actualredeemablePoints,
		 * "points earned is not matched for (/api/mobile/checkins)");
		 * Assert.assertEquals(points_spentApiFetchCheckin, actualredeemablePoints,
		 * "points_spent is not match for (/api/mobile/checkins)");
		 */

		// Api V1 checkins api/v1/checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		/*
		 * logger.info("Api V1 checkins Balance is successful");
		 * utils.logit("Api V1 checkins is successful"); int
		 * points_earnedV1Checkin =
		 * Integer.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_earned").
		 * toString().replace(".0", "")); int points_spentV1Checkin =
		 * Integer.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").
		 * toString().replace(".0", "")); int points_available =
		 * Integer.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available")
		 * .toString().replace(".0", "")); Assert.assertEquals(actualredeemablePoints,
		 * points_earnedV1Checkin,
		 * "points earned did not matched for (api/v1/checkins)");
		 * Assert.assertEquals(actualredeemablePoints, points_spentV1Checkin,
		 * "points spent did not matched for (api/v1/checkins)");
		 * Assert.assertEquals(actualredeemablePoints, points_available,
		 * "Points available did not matched for (api/v1/checkins)");
		 */
	}

	@Test(description = "OLF-T1933 [Business having same redemption mark] Verify migrated user having initial points", priority = 1)
	public void T1933_verifyMigratedUserHavingInitialPoints() throws Exception {

		// Step:2 login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Step:3 Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api1
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("api1 Signup is successful");
		logger.info("api1 Signup is successful");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// fetch details from checkin table
		String pointsSpentQuery = "Select `points_spent` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "points_spent", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "Select `points_earned` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "points_earned", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsSpent);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int expectedredeemablePoints = (points_earned - (points_spent + points_expired));

		// Step -4 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();

		/*
		 * Assert.assertEquals(actualredeemablePoints, expectedredeemablePoints,
		 * "User balance is not correct on timeline");
		 */
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline");

		// Step -5 User balance is correct on printtimeline
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienActualPoints = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		/*
		 * Assert.assertEquals(printTimelienActualPoints, expectedredeemablePoints,
		 * "User balance is not correct on print timeline");
		 */
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline");

		// Step-6 Banking event in account history validation
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		Assert.assertTrue(accountData.get(0).contains("You were gifted: Base Redeemable (Loyalty Reward)"),
				"Banking did not happened");
		/*
		 * Assert.assertTrue(accountData.size()==1, "Banking happened more then once");
		 */

		// Step-13 User balance is correct on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		/*
		 * Assert.assertEquals(iFrameActualpoints, expectedredeemablePoints,
		 * "Current Points for Iframe is not matching");
		 */
		logger.info("Iframe points validated");
		utils.logPass("Iframe points validated");

		// Step-8 Check total_credits/total_debits/initial_credits/initial_debits in
		// accounts table

		String query2 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String total_credits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"total_credits", 5);
		String query1 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String total_debits = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"total_debits", 5);
		String query3 = "Select `initial_credit` from `accounts` where `user_id` = '" + userID + "';";
		String initial_credit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"initial_credit", 5);
		String query4 = "Select `initial_debit` from `accounts` where `user_id` = '" + userID + "';";
		String initial_debit = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"initial_debit", 5);

		// reward debit table
		String query5 = "Select `type` from `reward_debits` where `user_id` = '" + userID + "';";
		String type = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "type", 5);
		String query6 = "Select `status` from `reward_debits` where `user_id` = '" + userID + "';";
		String status = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "status",
				5);
		String query7 = "Select `honored_reward_value` from `reward_debits` where `user_id` = '" + userID + "';";
		String honoredRewardValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"honored_reward_value", 5);

		/*
		 * Assert.assertEquals(total_credits, expectedredeemablePoints,
		 * "Current Points for Iframe is not matching");
		 * Assert.assertEquals(total_debits, "0",
		 * "Current Points for Iframe is not matching");
		 * Assert.assertEquals(initial_credit, expectedredeemablePoints,
		 * "Current Points for Iframe is not matching");
		 * Assert.assertEquals(initial_debit, "0",
		 * "Current Points for Iframe is not matching"); // checkin table points spent
		 * points earned Assert.assertEquals(pointsSpent, "0",
		 * "Current Points for Iframe is not matching");
		 * Assert.assertEquals(pointsEarned, "0",
		 * "Current Points for Iframe is not matching"); // reward debit table need to
		 * added Assert.assertEquals(type, "0",
		 * "Current Points for Iframe is not matching"); Assert.assertEquals(status,
		 * "0", "Current Points for Iframe is not matching");
		 * Assert.assertEquals(honoredRewardValue, "0",
		 * "Current Points for Iframe is not matching");
		 */
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
