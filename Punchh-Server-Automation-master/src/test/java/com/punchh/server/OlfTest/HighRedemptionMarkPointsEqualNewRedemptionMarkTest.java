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

public class HighRedemptionMarkPointsEqualNewRedemptionMarkTest {
	private static Logger logger = LogManager.getLogger(HighRedemptionMarkPointsEqualNewRedemptionMarkTest.class);
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

//Amit
	@Test(description = "OLF-T1977 [Business having high redemption mark than previous redemption mark] Verify user balance in API's (api2/mobile/users/balance) for user having exact points which is equal to new redemption mark", priority = 0)
	public void T1977_BusinessHighRedemptionMarkUserHavingPointsEqualToNewRedemptionMark() throws Exception {

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

		// update redeemption mark -> 100 old redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// uncheck Has Membership Levels? and uncheck Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 140 points, Points equal to new mark(high redeemption mark)
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int poscheckinPointsBalance = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int poscheckinTotalCredit = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int poscheckinTotalDebit = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		// update redeemption mark -> 140 high redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// OLF-T1977 Verify user balance in API's (api2/mobile/users/balance) for user
		// having exact points which is equal to new redemption mark
		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		int api2PointsBalance = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int api2RedeemablePoints = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(actualPoints, api2PointsBalance,
				"PointsBalance did not matched for (api2/mobile/users/balance)");
		Assert.assertEquals(actualPoints, api2RedeemablePoints,
				"RedeemablePoints did not matched for (api2/mobile/users/balance)");

		// OLF-T1978 Verify user balance in API's (api/auth/users/balance) for user
		// having exact points which is equal to new redemption mark
		// Fetch User Balance
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		int authPointsBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int authRedeemablePoints = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(actualPoints, authPointsBalance,
				"PointsBalance did not matched for (api/auth/users/balance)");
		Assert.assertEquals(actualPoints, authRedeemablePoints,
				"RedeemablePoints did not matched for (api/auth/users/balance)");

		// OLF-T1979 Verify user balance in API's (api/mobile/checkins/balance) for user
		// having exact points which is equal to new redemption mark
		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		int totalCredit = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int totaldebit = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		int pointsBalance = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		Assert.assertEquals(totalCredit, RedeemptionMarkNew,
				"total cerdit did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(totaldebit, RedeemptionMarkOld,
				"total debit did not matched for (api/mobile/checkins/balance)");
		Assert.assertEquals(actualPoints, pointsBalance,
				"PointsBalance did not matched for (api/mobile/checkins/balance)");

		// OLF-T1980 Verify user balance in API's (api2/mobile/checkins/account_balance)
		// for user having exact points which is equal to new redemption mark
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		int api2checkinPointsBalance = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.points_balance").toString().replace(".0", ""));
		int api2checkinRedeemablePoints = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(actualPoints, api2checkinPointsBalance,
				"PointsBalance did not matched for (api2/mobile/checkins/account_balance) ");
		Assert.assertEquals(actualPoints, api2checkinRedeemablePoints,
				"RedeemablePoints did not matched for (api2/mobile/checkins/account_balance) ");

		// OLF-T1981 Verify user balance in API's (api/auth/checkins/balance) for user
		// having exact points which is equal to new redemption mark
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int authBalancePointBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("points_balance").toString().replace(".0", ""));
		int authBalanceTotalCredit = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		int authBalanceTotalDebit = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));

		Assert.assertEquals(actualPoints, authBalancePointBalance,
				"total cerdit did not matched for (api/auth/checkins/balance)");
		Assert.assertEquals(authBalanceTotalCredit, RedeemptionMarkNew,
				"total debit did not matched for (api/auth/checkins/balance)");
		Assert.assertEquals(authBalanceTotalDebit, RedeemptionMarkOld,
				"PointsBalance did not matched for (api/auth/checkins/balance)");

		// OLF-T1982 Verify user balance in API's (api2/dashboard/users/info) for user
		// having exact points which is equal to new redemption mark
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

		Assert.assertEquals(actualPoints, useInfoPointsBalance,
				"total cerdit did not matched for (api2/dashboard/users/info)");
		Assert.assertEquals(useInfoTotalCredit, RedeemptionMarkNew,
				"total debit did not matched for (api2/dashboard/users/info)");
		Assert.assertEquals(useInfoTotalDebit, RedeemptionMarkOld,
				"PointsBalance did not matched for (api2/dashboard/users/info)");

		// OLF-T1983 Verify user balance in API's (api/pos/checkins) for user having
		// exact points which is equal to new redemption mark
		Assert.assertEquals(points_earned, poscheckinPointsBalance,
				"total cerdit did not matched for (api/pos/checkins)");
		Assert.assertEquals(points_earned, poscheckinTotalCredit, "total debit did not matched for (api/pos/checkins)");
		Assert.assertEquals(poscheckinTotalDebit, 0, "PointsBalance did not matched for (api/pos/checkins)");

		// OLF-T1984 Verify user balance in API's (/api/mobile/checkins) for user having
		// exact points which is equal to new redemption mark
		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		logger.info("Secure Api mobile checkin is successful");
		utils.logit(" Secure Api mobile checkin is successful");
		int points_earnedApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString().replace("[", "").replace("]", ""));
		int points_spentApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_earnedApiFetchCheckin, RedeemptionMarkNew,
				"points earned is not matched for (/api/mobile/checkins)");
		Assert.assertEquals(points_spentApiFetchCheckin, RedeemptionMarkOld,
				"points_spent is not match for (/api/mobile/checkins)");

	}

	@Test(description = "OLF-T1968 [Business having high redemption mark than previous redemption mark] Verify user balance on iframe for user having exact points which is equal to new redemption mark"
			+ "OLF-T1971 [Business having high redemption mark than previous redemption mark] Verify user balance on print timeline for user having exact points which is equal to new redemption mark", priority = 1)
	public void T1968_verifyUserBalanceOnIframeForUserHavingExactPointsWhichIsEqualToNewRedemptionMark()
			throws Exception {

		// Step:2 login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100 old redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// uncheck Has Membership Levels? and uncheck Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 80 points, less then to old redemption mark
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		/*
		 * int poscheckinPointsBalance =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.points_balance").
		 * toString().replace(".0", "")); int poscheckinTotalCredit =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.total_credits").
		 * toString().replace(".0", "")); int poscheckinTotalDebit =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.total_debits").
		 * toString().replace(".0", ""));
		 */

		// update redeemption mark -> 160 high redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin again of 80 points, threshold should reach new redemption mark
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("redeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("redeemptionMarkNew"));

		// Step -4 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(actualPoints, actualredeemablePoints, "User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline");

		// part of 1971
		// Step -5 User balance is correct on printtimeline
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienActualPoints = pageObj.guestTimelinePage().getPageContentsPrintTimeline();

		Assert.assertTrue(printTimelienActualPoints.contains(actualPoints + ".0 Redeemed"),
				"Redeemed point did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains(actualPoints + " Redeemable"),
				"Redeemable point did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains("Redeemable Points, \n" + actualPoints),
				"Redeemable Points did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points did not matched on print time line");

		// Step-6 Banking event in account history validation (only 1 banking)
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		Assert.assertTrue(accountData.get(0).contains("You were gifted: Base Redeemable (Loyalty Reward)"),
				"Banking did not happened");
		Assert.assertTrue(accountData.size() == 1, "Banking happened more then once");

		// Step-13 User balance is correct on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		Assert.assertEquals(actualPoints, Integer.parseInt(iFrameActualpoints),
				"Current Points for Iframe is not matching");
		logger.info("Iframe points validated");
		utils.logPass("Iframe points validated");

	}

	@Test(description = "OLF-T1970 [Business having high redemption mark than previous redemption mark] Verify user balance on iframe for user having less points than new redemption"
			+ "OLF-T1972 [Business having high redemption mark than previous redemption mark] Verify user balance on print timeline for user having less points than new redemption mark ", priority = 2)
	public void T1970_verifyUserBalanceOnIframeForUserHavingLessPointsThanNewRedemptionMark() throws Exception {

		// Step:2 login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100 old redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// uncheck Has Membership Levels? and uncheck Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// Do checkin of 60 points, less then to old redemption mark
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		/*
		 * int poscheckinPointsBalance =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.points_balance").
		 * toString().replace(".0", "")); int poscheckinTotalCredit =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.total_credits").
		 * toString().replace(".0", "")); int poscheckinTotalDebit =
		 * Integer.parseInt(checkinResponse.jsonPath().get("balance.total_debits").
		 * toString().replace(".0", ""));
		 */

		// update redeemption mark -> 140 high redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin again of 60 points, threshold should not reach to new redemption
		// mark
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in accounts table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("redeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("redeemptionMarkNew"));

		// Step -4 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		int actualredeemablePoints = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(actualPoints, actualredeemablePoints, "User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline");

		// part of 1971
		// Step -5 User balance is correct on printtimeline
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienActualPoints = pageObj.guestTimelinePage().getPageContentsPrintTimeline();

		Assert.assertTrue(printTimelienActualPoints.contains("0.0 Redeemed"),
				"Redeemed point did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains(actualPoints + " Redeemable"),
				"Redeemable point did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains("Redeemable Points, \n" + actualPoints),
				"Redeemable Points did not matched on print time line");
		Assert.assertTrue(printTimelienActualPoints.contains("(" + points_earned + " Paradise Points)"),
				"Paradise Points did not matched on print time line");

		// Step-6 Banking event in account history validation (only 1 banking)
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		Assert.assertTrue(accountData.size() == 0,
				"Banking happened. it should not happen as threshhold did not reached");

		// Step-13 User balance is correct on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		Assert.assertEquals(actualPoints, Integer.parseInt(iFrameActualpoints),
				"Current Points for Iframe is not matching");
		logger.info("Iframe points validated");
		utils.logPass("Iframe points validated");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
