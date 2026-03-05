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

public class BusinessNewRedemptionMarkE2ETest {
	private static Logger logger = LogManager.getLogger(BusinessNewRedemptionMarkE2ETest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl, client, secret, locationKey, apiKey, locationName;
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
		apiKey = "6J8cAY_aJqSKxvUWjy-k";
		locationKey = "cc33b01d7f794e13a2d9ae1d3901af8e";
		locationName = "Location with Redemption 2.0 disable";
	}

	@Test(description = "OLF-T2035 E2E Case [Business updated with new redemption mark less than old redemption mark] Verify banking logic for user having more points than new redemption mark, on checkin reaches the threshold will get loyatly reward", priority = 0)
	public void T2035_BusinessNewRedemptionMarkE2ETest() throws Exception {

		// getting data from business.preference from DB
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
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));

		// Checking and Marking "enable_points_spent_backfill" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark1"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// uncheck -> Enable Banking Rules?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Loyalty Goal Completion");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Banking Rules?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

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

		// send reward points to user here points = 80
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark2"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

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
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		utils.logit("total_debits column in checkins table is equal to expected value " + pointsSpent);

		String query5 = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"pointsEarnedAmount", 5);
		logger.info("points_earned column in checkins table is equal to expected value " + pointsEarned);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		int points_earned = Integer.parseInt(pointsEarned);
		int points_spent = Integer.parseInt(pointsSpent);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Step- 3 User gets the loyalty reward on banking
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, client, secret);
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not match with Auth List Available Reward ");
		logger.info("Auth List Available Reward  is successful");
		utils.logit("Auth List Available Reward  is successful");
		String redeemableId = rewardResponse.jsonPath().get("[0].redeemable_id").toString();
		Assert.assertEquals(redeemableId, dataSet.get("redeemable_id"), "Redeemable id is not matching");
		logger.info("Verified that User gets the loyalty reward on banking");
		utils.logPass("Verified that User gets the loyalty reward on banking");

		// Step -4 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		logger.info("Verified that User balance is correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// Step -5 User balance is correct on print timeline
		int count = pageObj.guestTimelinePage().getRedeemablePointOnPrintTimeline();
		Assert.assertEquals(count, actualPoints, "User balance is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline");

		// Step-6 Banking event in account history created
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

		// Step-7 points_spent updated correctly in checkins table
		String query1 = "Select `points_spent` from `checkins` where `id` = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "points_spent", 5); // dbUtils.getValueFromColumn(query3,
																															// "expire_item_type");
		Assert.assertEquals(expColValue1, "60", "Entry is not created in points_spent table in DB");

		// Step-8 Check total_credits/total_debits in accounts table

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits", 5);
		Assert.assertEquals(expColValue2, "60.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0"); // where to fetch
		// total_debits for comparison
		logger.info("total_debits column in accounts table is equal to expected value i.e. 60");
		utils.logPass("total_debits column in accounts table is equal to expected value i.e. 60");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				5);
		int expColValue3No = Integer.parseInt(expColValue3.replace(".0", ""));
		Assert.assertEquals(expColValue3No, points_earned,
				"total_credits column in accounts table is not equal to expected value i.e. " + expColValue3No);
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);
		utils.logPass("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);

		// Step-9 Check PointDebit in reward_debits table
		String query7 = "Select `status` from `reward_debits` where `user_id` ='" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "status", 5);
		Assert.assertEquals(expColValue7, "banked",
				"status column in reward_debits table is not equal to expected value i.e. banked");
		logger.info("status column in reward_debits table is equal to expected value i.e. banked");
		utils.logPass("status column in reward_debits table is equal to expected value i.e. banked");

		// Step -10 Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		logger.info("Secure Account Balance is successful");
		utils.logit("Secure Account Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalance, actualPoints,
				"points balance for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalCredits, points_earned,
				"total cerdit for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalDebits, points_spent, "total debit for api v1 mobile users balance is not matching"); // where
		// to
		// fetch
		logger.info(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e. " + totalDebits);
		utils.logPass(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e " + totalDebits);
		logger.info("Total Debits for api v1 mobile users balance is matching i.e. 0");
		utils.logPass("Total Debits for api v1 mobile users balance is matching i.e. 0");

		// Step-11 Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		utils.logit("API2 User Balance is successful");
		int pointsBalanceUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalanceUser, actualPoints,
				"points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(redeemablePointsUser, actualPoints,
				"redeemable points for api2 fetch User Balance is not matching");
		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. "
				+ pointsBalanceUser);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + pointsBalanceUser);

		// Step-12 Check user balance in api2/mobile/checkins/account_balance API
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
		Assert.assertEquals(pointsBalanceAccount, actualPoints,
				"points balance for api2 fetch Account balance is not matching");
		Assert.assertEquals(redeemablePointsAccount, actualPoints,
				"redeemable points for api2 fetch Account balance is not matching");
		logger.info(
				"Redeemable Points and Point Balance for api2 fetch Account balance is matching i.e. " + actualPoints);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch Account balance is matching i.e " + actualPoints);

		// Step-13 User balance is correct on iframe
		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		int points = Integer.parseInt(pageObj.iframeSingUpValidationPageClass().getAccountBalance());
		Assert.assertEquals(points, actualPoints, "Current Points for Iframe is not matching");
		logger.info("Current Points for Iframe is matching i.e. " + points);
		utils.logPass("Current Points for Iframe is matching i.e. " + points);

	}

	@Test(description = "OLF-T1994 [Business having high redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark do some checkin and he didn't reach the threshold and will not get loyatly reward ")
	public void T1994_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 90
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		int pointPresent = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(amountCheckin, pointPresent, "User balance is not correct on timeline");
		logger.info("Verified that User balance is correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();

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

		Assert.assertEquals(k, 0, "reward is credited to user account (Account History)");
		logger.info("verified that it should not get any reward as user didn't reach the current threshold");
		utils.logPass("verified that it should not get any reward as user didn't reach the current threshold");
	}

	@Test(description = "OLF-T1992 [Business having high redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark do some checkin to make him reach  the threshold and will get loyatly reward")
	public void T1992_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark1"));
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

		// send reward points to user here points = 80
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark2"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		int RedeemptionMark2 = Integer.parseInt(dataSet.get("RedeemptionMark2"));
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMark2 > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

		// Do checkin of 40
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();

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

//	@Test(description = "OLF-T1987 [Business having high redemption mark than previous redemption mark] Verify user balance in API's (api/v1/checkins?transaction_id=transaction_id/transactions)  for user having exact points which is equal to new redemption mark")
	public void T1987_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark1"));
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

		// Do checkin of 120
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

//		/api/v1/checkins?transaction_id

	}

	@Test(description = "OLF-T1985 [Business having high redemption mark than previous redemption mark] Verify user balance in API's (api/v1/checkins)  for user having exact points which is equal to new redemption mark || "
			+ "OLF-T1966 [Business having high redemption mark than previous redemption mark] Verify user details in accounts table for user having exact points which is equal to  new redemption mark")
	public void T1985_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// Do checkin of 120
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));
		int point = Integer.parseInt(dataSet.get("amount"));

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		String points_earned = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString().replace(".0", "");
		int pointsEarned = Integer.parseInt(points_earned);
		Assert.assertEquals(pointsEarned, RedeemptionMarkNew, "points balance is not equal to " + RedeemptionMarkNew);
		logger.info("Verified that points balance is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");
		utils.logPass("Verified that points balance is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");

		int points_spent = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		Assert.assertEquals(points_spent, RedeemptionMarkOld, "total credits is not equal to 0");
		logger.info("Verified that total credits is not equal to 0 for Api V1 checkins");
		utils.logPass("Verified that total credits is not equal to 0 for Api V1 checkins");

		int points_available = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0", ""));
		Assert.assertEquals(points_available, (RedeemptionMarkNew - RedeemptionMarkOld),
				"total debits is not equal to 20");
		logger.info("Verified that total debits is not equal to 20 for Api V1 checkins");
		utils.logPass("Verified that total debits is not equal to 20 for Api V1 checkins");

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits", 5);
		Assert.assertEquals(expColValue2, "100.00",
				"total_debits column in accounts table is not equal to expected value i.e. 100"); // where to fetch
		// total_debits for comparison
		logger.info("total_debits column in accounts table is equal to expected value i.e. 100");
		utils.logPass("total_debits column in accounts table is equal to expected value i.e. 100");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				5);
		int expColValue3No = Integer.parseInt(expColValue3.replace(".0", ""));
		Assert.assertEquals(expColValue3No, point,
				"total_credits column in accounts table is not equal to expected value i.e. " + expColValue3No);
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);
		utils.logPass("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);

		String query4 = "Select `initial_debit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "initial_debit",
				5);
		Assert.assertEquals(expColValue4, "0.0",
				"initial_debit column in accounts table is not equal to expected value i.e. 0");
		logger.info("initial_debit column in accounts table is equal to expected value i.e. 0");
		utils.logPass("initial_debit column in accounts table is equal to expected value i.e. 0");

		String query5 = "Select `initial_credit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "initial_credit",
				5);
		Assert.assertEquals(expColValue5, "0.0",
				"initial_credit column in accounts table is not equal to expected value i.e. 0"); // where to fetch
		// total_debits for comparison
		logger.info("initial_credit column in accounts table is equal to expected value i.e. 0");
		utils.logPass("initial_credit column in accounts table is equal to expected value i.e. 0");

	}

	@Test(description = "OLF-T1974 [Business having high redemption mark than previous redemption mark] Verify user balance in API's  for user having less points than new redemption mark")
	public void T1974_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 40
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 60
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));
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

		int pointBalancePosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		int pointsEarnedsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("points_earned").toString().replace(".0", ""));

		Assert.assertEquals(pointBalancePosCheckin, amt, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, 0, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(pointsEarnedsPosCheckin, Integer.parseInt(dataSet.get("amount")),
				"points_earned for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and points_earned  for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and points_earned  for Pos Checkin is matching the expected result");

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		logger.info("Secure Account Balance is successful");
		utils.logit("Secure Account Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalance, actualPoints,
				"points balance for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalCredits, amt, "total cerdit for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalDebits, 0, "total debit for api v1 mobile users balance is not matching"); // where to
																											// fetch
		logger.info(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e. " + totalDebits);
		utils.logPass(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e " + totalDebits);
		logger.info("Total Debits for api v1 mobile users balance is matching i.e. 0");
		utils.logPass("Total Debits for api v1 mobile users balance is matching i.e. 0");

		// Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		utils.logit("API2 User Balance is successful");
		int pointsBalanceUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalanceUser, actualPoints,
				"points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(redeemablePointsUser, actualPoints,
				"redeemable points for api2 fetch User Balance is not matching");
		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. "
				+ pointsBalanceUser);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + pointsBalanceUser);

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		logger.info("Auth User Balance is successful");
		utils.logit("Auth User Balance is successful");
		int pointBalanceAccountBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointBalanceAccountBalance, actualPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance, actualPoints,
				"redeemable Points Account Balance is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");

		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		utils.logPass("API v1 Fetch User Checkins Balance for user  is successful");
		logger.info("API v1 Fetch User Checkins Balance for user  is successful");

		String point_Balance = userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0",
				"");
		int pointBalance = Integer.parseInt(point_Balance);
		Assert.assertEquals(pointBalance, actualPoints, "points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for secure checkins balance");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for secure checkins balance");

		int total_credits = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_credits, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for secure checkins balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for secure checkins balance");

		int total_debits = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debits, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for secure checkins balance");
		utils.logPass("Verified that total debits is equal to 120 for secure checkins balance");

		// Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		utils.logPass("api2 account balance  is successful");
		logger.info("api2 account balance is successful");

		String point_BalanceAccountBalance = accountBalResponse.jsonPath().get("account_balance_details.points_balance")
				.toString().replace(".0", "");
		int pointBalanceAccountBalance3 = Integer.parseInt(point_BalanceAccountBalance);
		Assert.assertEquals(pointBalanceAccountBalance3, actualPoints,
				"points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for Mobile Account Balance");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for Mobile Account Balance");

		int redeemable_pointsAccountBalance = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(redeemable_pointsAccountBalance, actualPoints, "redeemable_points is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Mobile Account Balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Mobile Account Balance");

		// Auth Api Fetch checkin balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		logger.info("Auth Api Fetch account balance is successful");
		utils.logPass("Auth Api Fetch account balance is successful");
		String point_BalanceAuthCheckinsBalance = fetchAccountBalResponse.jsonPath().get("points_balance").toString()
				.replace(".0", "");
		int pointBalanceAuthCheckinsBalance = Integer.parseInt(point_BalanceAuthCheckinsBalance);
		Assert.assertEquals(pointBalanceAuthCheckinsBalance, actualPoints,
				"points balance is not equal to " + actualPoints);
		logger.info(
				"Verified that points balance is not equal to " + actualPoints + " for Auth Api Fetch checkin balance");
		utils.logPass(
				"Verified that points balance is not equal to " + actualPoints + " for Auth Api Fetch checkin balance");

		int total_creditsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsAuthCheckinsBalance, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Auth Api Fetch checkin balance");

		int total_debitsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsAuthCheckinsBalance, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for Auth Api Fetch checkin balance");
		utils.logPass("Verified that total debits is equal to 120 for Auth Api Fetch checkin balance");

		long phone = (long) (Math.random() * Math.pow(10, 10));
		// hit api api2/dashboard/users/info
		// Email only
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, blankString);
		Assert.assertEquals(userLookUpApi2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		String point_BalanceUsersInfo = userLookUpApi2.jsonPath().get("balance.points_balance").toString().replace(".0",
				"");
		int pointBalanceUsersInfo = Integer.parseInt(point_BalanceUsersInfo);
		Assert.assertEquals(pointBalanceUsersInfo, actualPoints, "points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");

		int total_creditsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsUsersInfo, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Dashboard user info");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Dashboard user info");

		int total_debitsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsUsersInfo, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for Dashboard user info");
		utils.logPass("Verified that total debits is equal to 120 for Dashboard user info");

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
		logger.info("Verified that points earned is not equal to " + amt + " for Api V1 checkins");
		utils.logPass("Verified that points earned is not equal to " + amt + " for Api V1 checkins");

		int points_spentV1Checkins = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentV1Checkins1 = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentV1Checkins + points_spentV1Checkins1, 0, "points_spent is not equal to " + amt);
		logger.info("Verified that points_spent is not equal to " + amt + " for Api V1 checkins");
		utils.logPass("Verified that points_spent is not equal to " + amt + " for Api V1 checkins");

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

		int points_spentApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentApiFetchCheckin1 = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentApiFetchCheckin + points_spentApiFetchCheckin1, 0,
				"points_spent is not equal to " + amt);
		logger.info("Verified that points_spent is not equal to " + amt + " for Secure Api Fetch Checkin");
		utils.logPass("Verified that points_spent is not equal to " + amt + " for Secure Api Fetch Checkin");

		// need to check step 11 and 12

	}

	@Test(description = "OLF-T1973 [Business having high redemption mark than previous redemption mark] Verify user balance in API's(api/mobile/users/balance)  for user having exact points which is equal to new redemption mark")
	public void T1973_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// Do checkin of 120
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile users balance");
		logger.info("Secure users Balance is successful");
		utils.logit("Secure users Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalance, actualPoints,
				"points balance for Secure Api mobile users balance is not matching");
		Assert.assertEquals(totalCredits, RedeemptionMarkNew,
				"total cerdit for Secure Api mobile users balance is not matching");
		Assert.assertEquals(totalDebits, RedeemptionMarkOld,
				"total debit for Secure Api mobile users balance is not matching"); // where to fetch
		logger.info("total credits and Point Balance for Secure Api mobile users balance is matching i.e. "
				+ RedeemptionMarkNew);
		utils.logPass("total credits and Point Balance for Secure Api mobile users balance is matching i.e "
						+ RedeemptionMarkNew);
		logger.info("Point Balance for Secure Api mobile users balance is matching i.e. " + actualPoints);
		utils.logPass("Point Balance for Secure Api mobile users balance is matching i.e " + actualPoints);
		logger.info("Total Debits for Secure Api users balance is matching i.e. " + RedeemptionMarkOld);
		utils.logPass("Total Debits for Secure Api users balance is matching i.e. " + RedeemptionMarkOld);

	}

	@Test(description = "OLF-T1967 [Business having high redemption mark than previous redemption mark] Verify points_debits in reward_debit table for user having exact points which is equal to new redemption mark ")
	public void T1967_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// Do checkin of 120
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		String query7 = "Select `status` from `reward_debits` where `user_id` ='" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "status", 5);
		Assert.assertEquals(expColValue7, "banked",
				"status column in reward_debits table is not equal to expected value i.e. banked");
		logger.info("status column in reward_debits table is equal to expected value i.e. banked");
		utils.logPass("status column in reward_debits table is equal to expected value i.e. banked");

	}

	@Test(description = "OLF-T1956 [Business having less redemption mark than previous redemption mark] Verify user balance in API's (api/mobile/checkins)  for user having exact points which is equal to new redemption mark || "
			+ "OLF-T1955 [Business having less redemption mark than previous redemption mark] Verify user balance in API's (api/pos/checkins)  for user having exact points which is equal to new redemption mark || "
			+ "OLF-T1945 [Business having less redemption mark than previous redemption mark] Verify user balance in API (api/mobile/users/balance) for user having exact points which is equal to new redemption mark ||"
			+ "OLF-T1949 [Business having less redemption mark than previous redemption mark] Verify user balance in API's (api2/mobile/users/balance)  for user having exact points which is equal to new redemption mark "
			+ "OLF-T1957 [Business having less redemption mark than previous redemption mark] Verify user balance in API's (api/v1/checkins)  for user having exact points which is equal to new redemption mark")
	public void T1956_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// Do checkin of 100
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));
		int amt = Integer.parseInt(dataSet.get("amount"));

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

		int pointBalancePosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));

		Assert.assertEquals(pointBalancePosCheckin, amt, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, 0, "total_debits for Pos Checkin is not matching");

		logger.info("points balance, total_credits and total_debits for Pos Checkin is matching the expected result");
		utils.logPass("points balance, total_credits and total_debits for Pos Checkin is matching the expected result");

		// Secure Api Fetch Checkin
		Response Api1FetchCheckinResp = pageObj.endpoints().mobAPi1FetchCheckin(token, client, secret);
		Assert.assertEquals(Api1FetchCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile checkin");
		logger.info("Secure Api mobile checkin is successful");
		utils.logit(" Secure Api mobile checkin is successful");

		String points_earnedApiFetchCheckin = Api1FetchCheckinResp.jsonPath().get("[0].points_earned").toString()
				.replace("[", "").replace("]", "");
		int pointsEarnedApiFetchCheckin = Integer.parseInt(points_earnedApiFetchCheckin);
		Assert.assertEquals(pointsEarnedApiFetchCheckin, RedeemptionMarkNew,
				"points earned is not equal to " + RedeemptionMarkNew);
		logger.info(
				"Verified that points earned is not equal to " + RedeemptionMarkNew + " for Secure Api Fetch Checkin");
		utils.logPass(
				"Verified that points earned is not equal to " + RedeemptionMarkNew + " for Secure Api Fetch Checkin");

		int points_spentApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentApiFetchCheckin, 0, "points_spent is not equal to 0");
		logger.info("Verified that points_spent is not equal to 0 for Secure Api Fetch Checkin");
		utils.logPass("Verified that points_spent is not equal to 0 for Secure Api Fetch Checkin");

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api mobile users balance");
		logger.info("Secure users Balance is successful");
		utils.logit("Secure users Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalance, actualPoints,
				"points balance for Secure Api mobile users balance is not matching");
		Assert.assertEquals(totalCredits, RedeemptionMarkNew,
				"total cerdit for Secure Api mobile users balance is not matching");
		Assert.assertEquals(totalDebits, 0, "total debit for Secure Api mobile users balance is not matching"); // where
																												// to
																												// fetch
		logger.info("total credits and Point Balance for Secure Api mobile users balance is matching i.e. "
				+ RedeemptionMarkNew);
		utils.logPass("total credits and Point Balance for Secure Api mobile users balance is matching i.e "
						+ RedeemptionMarkNew);
		logger.info("Point Balance for Secure Api mobile users balance is matching i.e. " + actualPoints);
		utils.logPass("Point Balance for Secure Api mobile users balance is matching i.e " + actualPoints);
		logger.info("Total Debits for Secure Api users balance is matching i.e. 0");
		utils.logPass("Total Debits for Secure Api users balance is matching i.e. 0");

		// Api V1 checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		String points_earnedV1Checkin = checkinsApiV1Response.jsonPath().get("[0].points_earned").toString()
				.replace(".0", "");
		int pointsEarnedV1Checkin = Integer.parseInt(points_earnedV1Checkin);
		Assert.assertEquals(pointsEarnedV1Checkin, RedeemptionMarkNew,
				"points_earned is not equal to " + RedeemptionMarkNew);
		logger.info("Verified that points_earnede is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");
		utils.logPass("Verified that points_earned is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");

		int points_spentV1Checkin = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace(".0", ""));
		Assert.assertEquals(points_spentV1Checkin, 0, "points_spent is not equal to " + RedeemptionMarkNew);
		logger.info("Verified that points_spent is not equal to 0 for Api V1 checkins");
		utils.logPass("Verified that points_spent is not equal to 0 for Api V1 checkins");

		int points_available = Integer
				.parseInt(checkinsApiV1Response.jsonPath().get("[0].points_available").toString().replace(".0", ""));
		Assert.assertEquals(points_available, RedeemptionMarkNew,
				"points_available is not equal to " + RedeemptionMarkNew);
		logger.info("Verified that points_available is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");
		utils.logPass("Verified that points_available is not equal to " + RedeemptionMarkNew + " for Api V1 checkins");

		// Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		utils.logit("API2 User Balance is successful");
		int pointsBalanceUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalanceUser, actualPoints,
				"points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(redeemablePointsUser, actualPoints,
				"redeemable points for api2 fetch User Balance is not matching");
		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. "
				+ pointsBalanceUser);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + pointsBalanceUser);

	}

	@Test(description = "OLF-T1961 [Business having high redemption mark than previous redemption mark] Verify user balance on user timeline for user with exact points i.e.equal to new redemption mark gets reward only when user reach the threshold & will get 2 rewards in the account histo")
	public void T1961_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 80
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

		// Do checkin of 40
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();

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

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount1 = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag1 = false;
		if (RedeemptionMarkNew != redeemablePointCount) {
			RedeemptionMarkFlag1 = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag1, "user have less points than new redemption mark in the business");
		logger.info("Verified that user not having less points than new redemption mark in the business");
		utils.logPass("Verified that user not having less points than new redemption mark in the business");

		Assert.assertEquals(redeemablePointCount1, 0,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance i.e 0");
		utils.logPass("Verified that In user timeline it should show current points as his balance i.e. 0");

	}

	@Test(description = "OLF-T1962 [Business having higher redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark")
	public void T1962_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 90
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

	}

	@Test(description = "OLF-T2036 [Business having higher redemption mark than previous redemption mark] Verify user balance on user timeline for user with exact points i.e.equal to new redemption mark ,refresh the account and check user is getting reward or not")
	public void T2036_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// Do checkin of 120
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = RedeemptionMarkNew - RedeemptionMarkOld;
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Timeline");
		// verify redeemable point in guest timeline
		int redeemablePointCount1 = pageObj.guestTimelinePage().getRedeemablePointCount();

		boolean RedeemptionMarkFlag1 = false;
		if (RedeemptionMarkNew > redeemablePointCount1) {
			RedeemptionMarkFlag1 = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag1, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		Assert.assertEquals(redeemablePointCount1, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");
	}

	@Test(description = "OLF-T2037 [Business having less redemption mark than previous redemption mark]Verify user balance on user timeline for user with exact points i.e.equal to new redemption mark ,refresh the account and check user is getting reward or not.")
	public void T2037_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// Do checkin of 100
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew == redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user have less points than new redemption mark in the business");
		logger.info("Verified that user having exact point equal to new redemption mark in the business");
		utils.logPass("Verified that user having exact point equal to redemption mark in the business");

		int point = RedeemptionMarkNew;
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Edit Profile");
		pageObj.guestTimelinePage().refreshAccount();
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Timeline");
		// verify redeemable point in guest timeline
		int redeemablePointCount1 = pageObj.guestTimelinePage().getRedeemablePointCount();

		boolean RedeemptionMarkFlag1 = false;
		if (RedeemptionMarkNew == redeemablePointCount1) {
			RedeemptionMarkFlag1 = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag1, "user have less points than new redemption mark in the business");
		logger.info("Verified that user having exact point equal to new redemption mark in the business");
		utils.logPass("Verified that user having exact point equal to new redemption mark in the business");

		Assert.assertEquals(redeemablePointCount1, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");
	}

	@Test(description = "OLF-T1993 [Business having less redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark do some checkin and he didn't reach the threshold and will not get loyatly reward")
	public void T1993_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// send reward points to user here points = 60
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		pageObj.guestTimelinePage().refreshTimeline();

		// verify redeemable point in guest timeline
		int redeemablePointCount1 = pageObj.guestTimelinePage().getRedeemablePointCount();

		boolean RedeemptionMarkFlag1 = false;
		if (RedeemptionMarkNew > redeemablePointCount1) {
			RedeemptionMarkFlag1 = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag1, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		point = (Integer.parseInt(dataSet.get("points")) + Integer.parseInt(dataSet.get("amount")));
		Assert.assertEquals(redeemablePointCount1, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();

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

		Assert.assertEquals(k, 0, "reward is credited to user account (Account History)");
		logger.info("verified that it should not get any reward as user didn't reach the current threshold");
		utils.logPass("verified that it should not get any reward as user didn't reach the current threshold");
	}

	@Test(description = "OLF-T1991 [Business having less redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark do some checkin to make him reach  the threshold and will get loyatly reward")
	public void T1991_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// send reward points to user here points = 60
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("points"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

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

		Assert.assertEquals(j, 0, "reward should be credited in the account history");
		logger.info("verified that No reward should be credited in the account history");
		utils.logPass("verified that No reward should be credited in the account history");

		// Do checkin of 40
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		utils.refreshPage();
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount1 = pageObj.guestTimelinePage().getRedeemablePointCount();

		boolean RedeemptionMarkFlag1 = false;
		if (RedeemptionMarkNew > redeemablePointCount1) {
			RedeemptionMarkFlag1 = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag1, "user not have less points than new redemption mark in the business");
		logger.info("Verified that user having less points than new redemption mark in the business");
		utils.logPass("Verified that user having less points than new redemption mark in the business");

		point = RedeemptionMarkNew
				- (Integer.parseInt(dataSet.get("points")) + Integer.parseInt(dataSet.get("amount")));
		Assert.assertEquals(redeemablePointCount1, point,
				"In user timeline it is not showing current points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		// Banking event in account history created
		List<Object> obj2 = new ArrayList<Object>();

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

	@Test(description = "OLF-T1946 [Business having less redemption mark than previous redemption mark] Verify user balance in API's  for user having less points than new redemption mark")
	public void T1946_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 30
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 50
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int amt = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));
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

		int pointBalancePosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.points_balance").toString().replace(".0", ""));
		int totalCreditsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		int totalDebitsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		int pointsEarnedsPosCheckin = Integer
				.parseInt(checkinResponse.jsonPath().get("points_earned").toString().replace(".0", ""));

		Assert.assertEquals(pointBalancePosCheckin, amt, "points balance for Pos Checkin is not matching");
		Assert.assertEquals(totalCreditsPosCheckin, amt, "total_credits for Pos Checkin is not matching");
		Assert.assertEquals(totalDebitsPosCheckin, 0, "total_debits for Pos Checkin is not matching");
		Assert.assertEquals(pointsEarnedsPosCheckin, Integer.parseInt(dataSet.get("amount")),
				"points_earned for Pos Checkin is not matching");

		logger.info(
				"points balance, total_credits, total_debits and points_earned  for Pos Checkin is matching the expected result");
		utils.logPass(
				"points balance, total_credits, total_debits and points_earned  for Pos Checkin is matching the expected result");

		// Check user balance in api/mobile/users/balance API
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		logger.info("Secure Account Balance is successful");
		utils.logit("Secure Account Balance is successful");
		int pointsBalance = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int totalCredits = Integer.parseInt(
				balance_Response.jsonPath().get("account_balance.total_credits").toString().replace(".0", ""));
		int totalDebits = Integer
				.parseInt(balance_Response.jsonPath().get("account_balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalance, actualPoints,
				"points balance for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalCredits, amt, "total cerdit for api v1 mobile users balance is not matching");
		Assert.assertEquals(totalDebits, 0, "total debit for api v1 mobile users balance is not matching"); // where to
																											// fetch
		logger.info(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e. " + totalDebits);
		utils.logPass(
				"Redeemable Points and Point Balance for api v1 mobile users balance is matching i.e " + totalDebits);
		logger.info("Total Debits for api v1 mobile users balance is matching i.e. 0");
		utils.logPass("Total Debits for api v1 mobile users balance is matching i.e. 0");

		// Check user balance in api2/mobile/users/balance API
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("API2 User Balance is successful");
		utils.logit("API2 User Balance is successful");
		int pointsBalanceUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsUser = Integer.parseInt(
				userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointsBalanceUser, actualPoints,
				"points balance for api2 fetch User Balance is not matching");
		Assert.assertEquals(redeemablePointsUser, actualPoints,
				"redeemable points for api2 fetch User Balance is not matching");
		logger.info("Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e. "
				+ pointsBalanceUser);
		utils.logPass(
				"Redeemable Points and Point Balance for api2 fetch User Balance is matching i.e " + pointsBalanceUser);

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		logger.info("Auth User Balance is successful");
		utils.logit("Auth User Balance is successful");
		int pointBalanceAccountBalance = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString().replace(".0", ""));
		int redeemablePointsAccountBalance = Integer.parseInt(authUserBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(pointBalanceAccountBalance, actualPoints, "point Balance Account Balance is not matching");
		Assert.assertEquals(redeemablePointsAccountBalance, actualPoints,
				"redeemable Points Account Balance is not matching");
		logger.info("point Balance and redeemable Points Account Balance is matching the expected result");
		utils.logPass("point Balance and redeemable Points Account Balance is matching the expected result");

		// Fetch User Checkins Balance for user
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token, client, secret);
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		utils.logPass("API v1 Fetch User Checkins Balance for user  is successful");
		logger.info("API v1 Fetch User Checkins Balance for user  is successful");

		String point_Balance = userCheckinsBalanceResponse.jsonPath().get("points_balance").toString().replace(".0",
				"");
		int pointBalance = Integer.parseInt(point_Balance);
		Assert.assertEquals(pointBalance, actualPoints, "points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for secure checkins balance");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for secure checkins balance");

		int total_credits = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_credits, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for secure checkins balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for secure checkins balance");

		int total_debits = Integer
				.parseInt(userCheckinsBalanceResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debits, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for secure checkins balance");
		utils.logPass("Verified that total debits is equal to 120 for secure checkins balance");

		// Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(client, secret, token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		utils.logPass("api2 account balance  is successful");
		logger.info("api2 account balance is successful");

		String point_BalanceAccountBalance = accountBalResponse.jsonPath().get("account_balance_details.points_balance")
				.toString().replace(".0", "");
		int pointBalanceAccountBalance3 = Integer.parseInt(point_BalanceAccountBalance);
		Assert.assertEquals(pointBalanceAccountBalance3, actualPoints,
				"points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for Mobile Account Balance");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for Mobile Account Balance");

		int redeemable_pointsAccountBalance = Integer.parseInt(accountBalResponse.jsonPath()
				.get("account_balance_details.redeemable_points").toString().replace(".0", ""));
		Assert.assertEquals(redeemable_pointsAccountBalance, actualPoints, "redeemable_points is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Mobile Account Balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Mobile Account Balance");

		// Auth Api Fetch checkin balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken, client, secret);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		logger.info("Auth Api Fetch account balance is successful");
		utils.logPass("Auth Api Fetch account balance is successful");
		String point_BalanceAuthCheckinsBalance = fetchAccountBalResponse.jsonPath().get("points_balance").toString()
				.replace(".0", "");
		int pointBalanceAuthCheckinsBalance = Integer.parseInt(point_BalanceAuthCheckinsBalance);
		Assert.assertEquals(pointBalanceAuthCheckinsBalance, actualPoints,
				"points balance is not equal to " + actualPoints);
		logger.info(
				"Verified that points balance is not equal to " + actualPoints + " for Auth Api Fetch checkin balance");
		utils.logPass(
				"Verified that points balance is not equal to " + actualPoints + " for Auth Api Fetch checkin balance");

		int total_creditsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsAuthCheckinsBalance, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Auth Api Fetch checkin balance");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Auth Api Fetch checkin balance");

		int total_debitsAuthCheckinsBalance = Integer
				.parseInt(fetchAccountBalResponse.jsonPath().get("total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsAuthCheckinsBalance, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for Auth Api Fetch checkin balance");
		utils.logPass("Verified that total debits is equal to 120 for Auth Api Fetch checkin balance");

		long phone = (long) (Math.random() * Math.pow(10, 10));
		// hit api api2/dashboard/users/info
		// Email only
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, phone, blankString);
		Assert.assertEquals(userLookUpApi2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		String point_BalanceUsersInfo = userLookUpApi2.jsonPath().get("balance.points_balance").toString().replace(".0",
				"");
		int pointBalanceUsersInfo = Integer.parseInt(point_BalanceUsersInfo);
		Assert.assertEquals(pointBalanceUsersInfo, actualPoints, "points balance is not equal to " + actualPoints);
		logger.info("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");
		utils.logPass("Verified that points balance is not equal to " + actualPoints + " for Dashboard user info");

		int total_creditsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_credits").toString().replace(".0", ""));
		Assert.assertEquals(total_creditsUsersInfo, amt, "total credits is not equal to " + amt);
		logger.info("Verified that total credits is not equal to " + amt + " for Dashboard user info");
		utils.logPass("Verified that total credits is not equal to " + amt + " for Dashboard user info");

		int total_debitsUsersInfo = Integer
				.parseInt(userLookUpApi2.jsonPath().get("balance.total_debits").toString().replace(".0", ""));
		Assert.assertEquals(total_debitsUsersInfo, 0, "total debits is not equal to 120");
		logger.info("Verified that total debits is equal to 120 for Dashboard user info");
		utils.logPass("Verified that total debits is equal to 120 for Dashboard user info");

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
		logger.info("Verified that points earned is not equal to " + amt + " for Api V1 checkins");
		utils.logPass("Verified that points earned is not equal to " + amt + " for Api V1 checkins");

		int points_spentV1Checkins = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentV1Checkins1 = Integer.parseInt(
				checkinsApiV1Response.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentV1Checkins + points_spentV1Checkins1, 0, "points_spent is not equal to " + amt);
		logger.info("Verified that points_spent is not equal to " + amt + " for Api V1 checkins");
		utils.logPass("Verified that points_spent is not equal to " + amt + " for Api V1 checkins");

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

		int points_spentApiFetchCheckin = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[0].points_spent").toString().replace("[", "").replace("]", ""));
		int points_spentApiFetchCheckin1 = Integer.parseInt(
				Api1FetchCheckinResp.jsonPath().get("[1].points_spent").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(points_spentApiFetchCheckin + points_spentApiFetchCheckin1, 0,
				"points_spent is not equal to " + amt);
		logger.info("Verified that points_spent is not equal to " + amt + " for Secure Api Fetch Checkin");
		utils.logPass("Verified that points_spent is not equal to " + amt + " for Secure Api Fetch Checkin");

		// need to check step 11 and 12

	}

	@Test(description = "OLF-T1944 [Business having less redemption mark than previous redemption mark] Verify user balance on print timeline for user having less points than new redemption mark || "
			+ "OLF-T1942 [Business having less redemption mark than previous redemption mark] Verify user balance on iframe for user having less points than new redemption mark || "
			+ "OLF-T1935 [Business having less redemption mark than previous redemption mark] Verify user balance on user timeline for user having less points than new redemption mark")
	public void T1944_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew > redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user have less points than new redemption mark in the business");
		logger.info("Verified that user having exact point equal to new redemption mark in the business");
		utils.logPass("Verified that user having exact point equal to redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("amount"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("" + point + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + "" + point + ""),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + point + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

		// User balance is correct on iframe
		// Login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		int iFramePoints = Integer.parseInt(pageObj.iframeSingUpValidationPageClass().getAccountBalance());
		Assert.assertEquals(iFramePoints, point, "Current Points for Iframe is not matching");
		logger.info("Current Points for Iframe is matching i.e. " + iFramePoints);
		utils.logPass("Current Points for Iframe is matching i.e. " + iFramePoints);
	}

	@Test(description = "OLF-T1943 [Business having less redemption mark than previous redemption mark] Verify user balance on print timeline for user having exact points which is equal to new redemption mark || "
			+ "OLF-T1941 [Business having less redemption mark than previous redemption mark] Verify user balance on iframe for user having exact points which is equal to new redemption mark")
	public void T1943_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 120
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

		// Do checkin of 80
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		int RedeemptionMarkOld = Integer.parseInt(dataSet.get("RedeemptionMarkOld"));
		int RedeemptionMarkNew = Integer.parseInt(dataSet.get("RedeemptionMarkNew"));

		// User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int redeemablePointCount = pageObj.guestTimelinePage().getRedeemablePointCount();
		boolean RedeemptionMarkFlag = false;
		if (RedeemptionMarkNew == redeemablePointCount) {
			RedeemptionMarkFlag = true;
		}
		Assert.assertTrue(RedeemptionMarkFlag, "user have less points than new redemption mark in the business");
		logger.info("Verified that user having exact point equal to new redemption mark in the business");
		utils.logPass("Verified that user having exact point equal to redemption mark in the business");

		int point = Integer.parseInt(dataSet.get("amount"));
		Assert.assertEquals(redeemablePointCount, point,
				"In user timeline it is not showing correct points as his balance");
		logger.info("Verified that In user timeline it should show current points as his balance");
		utils.logPass("Verified that In user timeline it should show current points as his balance");

		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("" + point + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + "" + point + ""),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + point + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

		// User balance is correct on iframe
		// Login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 2);
		int iFramePoints = Integer.parseInt(pageObj.iframeSingUpValidationPageClass().getAccountBalance());
		Assert.assertEquals(iFramePoints, point, "Current Points for Iframe is not matching");
		logger.info("Current Points for Iframe is matching i.e. " + iFramePoints);
		utils.logPass("Current Points for Iframe is matching i.e. " + iFramePoints);
	}

	@Test(description = "OLF-T1965 [Business having high redemption mark than previous redemption mark] Verify user details in accounts table for user having less points than the new redemption mark")
	public void T1965_BusinessNewRedemptionMarkTest() throws Exception {

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
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

		// send reward points to user here points = 40
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		utils.logit("Send points to the user successfully");

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 60
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		int point = (Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("points")));

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits", 5);
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0"); // where to fetch
		// total_debits for comparison
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		utils.logPass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				5);
		int expColValue3No = Integer.parseInt(expColValue3.replace(".0", ""));
		Assert.assertEquals(expColValue3No, point,
				"total_credits column in accounts table is not equal to expected value i.e. " + expColValue3No);
		logger.info("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);
		utils.logPass("total_credits column in accounts table is equal to expected value i.e. " + expColValue3No);

		String query4 = "Select `initial_debit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "initial_debit",
				5);
		Assert.assertEquals(expColValue4, "0.0",
				"initial_debit column in accounts table is not equal to expected value i.e. 0");
		logger.info("initial_debit column in accounts table is equal to expected value i.e. 0");
		utils.logPass("initial_debit column in accounts table is equal to expected value i.e. 0");

		String query5 = "Select `initial_credit` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "initial_credit",
				5);
		Assert.assertEquals(expColValue5, "0.0",
				"initial_credit column in accounts table is not equal to expected value i.e. 0"); // where to fetch
		// total_debits for comparison
		logger.info("initial_credit column in accounts table is equal to expected value i.e. 0");
		utils.logPass("initial_credit column in accounts table is equal to expected value i.e. 0");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
