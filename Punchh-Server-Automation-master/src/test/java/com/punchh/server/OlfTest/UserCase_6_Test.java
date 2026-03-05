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
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class UserCase_6_Test {
	private static Logger logger = LogManager.getLogger(UserCase_6_Test.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl, client, secret, apiKey, locationKey;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	private SeleniumUtilities seleniumutilities;

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
		seleniumutilities = new SeleniumUtilities(driver);
	}

	@Test(description = "OLF-T2120	[New ML redemption mark is less than old] Verify point balance in iframe for users when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2121	[New ML redemption mark is less than old] Verify user's print timeline when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2122	[New ML redemption mark is less than old] Verify user's timeline / pie chart when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2123	[New ML redemption mark is less than old] Verify user's account history when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2124	[New ML redemption mark is less than old] Verify user balance in different secure API's when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2125	[New ML redemption mark is less than old] Verify user balance in different API2 API's when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2126	[New ML redemption mark is less than old] Verify user balance in different V1 API's when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2127	[New ML redemption mark is less than old] Verify user balance in different Auth API's when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2128	[New ML redemption mark is less than old] Verify user balance in dashboard API when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2129	[New ML redemption mark is less than old] Verify user balance in pos/checkin API when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2130	[New ML redemption mark is less than old] Verify user details in accounts table when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2131	[New ML redemption mark is less than old] Verify user details in checkins table when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2132	[New ML redemption mark is less than old] Verify PointDebits in reward_debits table when business configuration has membership level Redemption Mark configured [User case 6]\n"
			+ "OLF-T2133	[New ML redemption mark is less than old] Verify user details in membership_level_histories table when business configuration has membership level Redemption Mark configured [User case 6]")
	public void T2120_VerifyNewMLRedemptionMarkLessThanOld() throws Exception {

		// getting data from business.preference from DB
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String membershipId1 = "706";
		String expMembershipLevel = "Membership Level Silver";
		int checkinAmount = Utilities.getRandomNoFromRange(61, 100);
		double checkinAmountDouble = Double.parseDouble(checkinAmount + "");
		double redemptionMarkExp = Double.parseDouble(dataSet.get("RedemptionMark"));
		int membershipId1_Int = Integer.parseInt(membershipId1);

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

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check Has Membership Levels? and check Membership Level Bump on Edge?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// edit membership levels redemption mark - 60
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			utils.longWaitInSeconds(2);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark"));
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

		// Do checkin in rage 61-100
		// OLF-T2129 [New ML redemption mark is less than old] Verify user balance in
		// pos/checkin API when business configuration has membership level Redemption
		// Mark configured [User case 6]
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, locationKey, checkinAmount + "");
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + checkinAmount + " points");
		utils.logPass("POS checkin is successful for " + checkinAmount + " points");

		String actualMemberShipLevelAPi = checkinResponse2.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(actualMemberShipLevelAPi, expMembershipLevel,
				"Membership level Name in dashboard API is not matching");

		int actualMembershipLevelID_POSApi = checkinResponse2.jsonPath().getInt("balance.membership_level_id");
		Assert.assertEquals(actualMembershipLevelID_POSApi, Integer.parseInt(membershipId1),
				"Membership level ID in dashboard API is not matching");
		logger.info("Verified membership_level_id is matched in POS Checkin API response "
				+ actualMembershipLevelID_POSApi);
		utils.logPass("Verified membership_level_id is matched in POS Checkin API response "
				+ actualMembershipLevelID_POSApi);

		double net_balanceActual_POSApi = checkinResponse2.jsonPath().getDouble("balance.net_balance");
		Assert.assertEquals(net_balanceActual_POSApi, checkinAmountDouble,
				"Net balance in dashboard API is not matching");
		logger.info("Verified Net balance is matched in POS Checkin API response " + net_balanceActual_POSApi);
		utils.logPass("Verified Net balance is matched in POS Checkin API response " + net_balanceActual_POSApi);

		double total_creditsActual_POSApi = checkinResponse2.jsonPath().getDouble("balance.total_credits");
		Assert.assertEquals(total_creditsActual_POSApi, checkinAmountDouble,
				"total_credits balance in dashboard API is not matching");
		logger.info("Verified total_credits is matched in POS Checkin API response " + total_creditsActual_POSApi);
		utils.logPass("Verified total_credits is matched in POS Checkin API response " + total_creditsActual_POSApi);

		double net_debits_POSApi = checkinResponse2.jsonPath().getDouble("balance.net_debits");
		Assert.assertEquals(net_debits_POSApi, 0.0, "net_debits balance in dashboard API is not matching");
		logger.info("Verified net_debits is matched in POS Checkin API response " + net_debits_POSApi);
		utils.logPass("Verified net_debits is matched in POS Checkin API response " + net_debits_POSApi);

		double points_balance_POSApi = checkinResponse2.jsonPath().getDouble("balance.points_balance");
		Assert.assertEquals(points_balance_POSApi, checkinAmount,
				"points_balance balance in dashboard API is not matching");

		logger.info("Verified points_balance is matched in POS Checkin API response " + points_balance_POSApi);
		utils.logPass("Verified points_balance is matched in POS Checkin API response " + points_balance_POSApi);

		double total_credits_POSApi = checkinResponse2.jsonPath().getDouble("balance.total_credits");
		Assert.assertEquals(total_credits_POSApi, checkinAmount,
				"total_credits balance in dashboard API is not matching");
		logger.info("Verified total_credits is matched in POS Checkin API response " + total_credits_POSApi);
		utils.logPass("Verified total_credits is matched in POS Checkin API response " + total_credits_POSApi);

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query1 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "pointsSpentAmount",
				5);
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
		double expectedAvailabeBalancePoints = Double
				.parseDouble((points_earned - (points_spent + points_expired)) + "");
		int pointsRemaining = (int) expectedAvailabeBalancePoints;

		// Check user timeline for Redeemable Points
		// OLF-T2122 [New ML redemption mark is less than old] Verify user's timeline /
		// pie chart when business configuration has membership level Redemption Mark
		// configured [User case 6]
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		double actualredeemablePoints = Double.parseDouble(pageObj.guestTimelinePage().getRedeemablePointCount() + "");
		int loyaltypieChartPoints = Integer.parseInt(pageObj.guestTimelinePage().getLoyaltyPieChartDetails());
		Assert.assertEquals(actualredeemablePoints, expectedAvailabeBalancePoints,
				"User balance is not correct on timeline");
		Assert.assertEquals(loyaltypieChartPoints, checkinAmount,
				checkinAmount + " checkin loyalty pie chart points balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline :" + actualredeemablePoints);

		// Banking event in account history validation
		// OLF-T2123 [New ML redemption mark is less than old] Verify user's account
		// history when business configuration has membership level Redemption Mark
		// configured [User case 6]
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> accountData = pageObj.accounthistoryPage().getAccountDetailsforItemEarned("(Loyalty Reward)");
		int totalBankingEvents = accountData.size();
		Assert.assertEquals(totalBankingEvents, 1,
				"Banking happened for user more than once, it should happen once only");
		utils.logPass("loyalty reward banking event validated in account history :" + totalBankingEvents);

		utils.longwait(3);
		pageObj.guestTimelinePage().navigateToTabs("Timeline");
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienPageContentes = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		Assert.assertTrue(printTimelienPageContentes.contains("0.0 Redeemed"),
				"Redeemed is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("" + pointsRemaining + " Redeemable"),
				"Redeemable is not correct on print timeline");
		Assert.assertTrue(printTimelienPageContentes.contains("Redeemable Points, \n" + "" + pointsRemaining + ""),
				"Redeemable Points is not correct on print timeline");
		// Paradise is configurable from cockpit>dashboard>mic>currency
		Assert.assertTrue(printTimelienPageContentes.contains("(" + checkinAmount + " Paradise Points)"),
				"Paradise Points is not correct on print timeline");
		logger.info("Verified thet User balance is correct on print timeline");
		utils.logPass("Verified thet User balance is correct on print timeline page");

		seleniumutilities.closeAllChildWindowAndSwitchToParentWindow();
		// OLF-T2133 [New ML redemption mark is less than old] Verify user details in
		// membership_level_histories table when business configuration has membership
		// level Redemption Mark configured [User case 6]
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
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2,
														// "membership_level_id");
		Assert.assertEquals(expColValue3, membershipId1, "Operative membership strategy is not " + membershipId1 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId1 + "");
		utils.logPass("Verified that Operative membership strategy is " + membershipId1 + "");

		String previous_membership_level_id_Query = "SELECT `previous_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "'order by previous_membership_level_id desc;";
		String expColValue22 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				previous_membership_level_id_Query, "previous_membership_level_id", 10); // dbUtils.getValueFromColumn(query2,
																							// "membership_level_id");
		Assert.assertEquals(expColValue22, null, "Effective membership strategy is not " + membershipId1 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " ");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + "");

		// User balance validation on iframe
		// login through iframe

		// OLF-T2120 [New ML redemption mark is less than old] Verify point balance in
		// iframe for users when business configuration has membership level Redemption
		// Mark configured [User case 6]

		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + "/whitelabel/" + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLoginToCheckReactivation(userEmail, prop.getProperty("iFramePassword"), 1);
		String iFrameActualpoints = pageObj.iframeSingUpValidationPageClass().getAccountBalance();
		String membershipLevel = pageObj.iframeSingUpValidationPageClass().getMembershiplevel();
		Assert.assertEquals(Double.parseDouble(iFrameActualpoints), expectedAvailabeBalancePoints,
				iFrameActualpoints + " Current Points for Iframe is not matching");
		Assert.assertEquals(membershipLevel, expMembershipLevel, "Membership Level Silver for Iframe is not matching");
		utils.logPass("Iframe points and membership level validated");

		// OLF-T2128 [New ML redemption mark is less than old] Verify user balance in
		// dashboard API when business configuration has membership level Redemption
		// Mark configured [User case 6]
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, apiKey, 123456789, "");
		Assert.assertEquals(userLookUpApi2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("using User Look Up (email only) is successful");

		String actualMemberShipLevelAPi1 = userLookUpApi2.jsonPath().get("balance.membership_level").toString();
		Assert.assertEquals(actualMemberShipLevelAPi1, expMembershipLevel,
				"Membership level in dashboard API is not matching");

		int actualMembershipLevelIDAPI = userLookUpApi2.jsonPath().getInt("balance.membership_level_id");
		Assert.assertEquals(actualMembershipLevelIDAPI, Integer.parseInt(membershipId1),
				"Membership level ID in dashboard API is not matching");
		logger.info(
				"Verified membership_level_id is matched in User balance API response " + actualMembershipLevelIDAPI);
		utils.logPass(
				"Verified membership_level_id is matched in User balance API response " + actualMembershipLevelIDAPI);

		double net_balanceActualAPI = userLookUpApi2.jsonPath().getDouble("balance.net_balance");
		Assert.assertEquals(net_balanceActualAPI, expectedAvailabeBalancePoints,
				"Net balance in dashboard API is not matching");
		logger.info("Verified net_balance is matched in User balance API response " + net_balanceActualAPI);
		utils.logPass("Verified net_balance is matched in User balance API response " + net_balanceActualAPI);

		double total_creditsActualAPI = userLookUpApi2.jsonPath().getDouble("balance.total_credits");
		Assert.assertEquals(total_creditsActualAPI, checkinAmount, "Net balance in dashboard API is not matching");
		logger.info("Verified total_credits is matched in User balance API response " + total_creditsActualAPI);
		utils.logPass("Verified total_credits is matched in User balance API response " + total_creditsActualAPI);

		// OLF-T2130 [New ML redemption mark is less than old] Verify user details in
		// accounts table when business configuration has membership level Redemption
		// Mark configured [User case 6]

		String query6 = "SELECT `membership_level` from `accounts` where `user_id` = '" + userID + "';";
		String actMembershipLevel_DB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"membership_level", 10);
		Assert.assertEquals(actMembershipLevel_DB, expMembershipLevel,
				expMembershipLevel + " Membership level in accounts table is not matching");

		logger.info("Verified Membership level is matched in accounts table " + actMembershipLevel_DB);
		utils.logPass("Verified Membership level is matched in accounts table " + actMembershipLevel_DB);

		String query7 = "SELECT `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		String actTotalCredits_DB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"total_credits", 10);
		double actTotalsCredits_DB = Double.parseDouble(actTotalCredits_DB);
		Assert.assertEquals(actTotalsCredits_DB, checkinAmountDouble,
				actTotalsCredits_DB + " Total Credits in accounts table is not matching");
		logger.info("Verified total_credits is matched in accounts table " + actTotalsCredits_DB);
		utils.logPass("Verified total_credits is matched in accounts table " + actTotalsCredits_DB);

		String query8 = "SELECT `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		String actTotalDebits_DB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"total_debits", 10);
		double actTotalDebits_DB1 = Double.parseDouble(actTotalDebits_DB);

		Assert.assertEquals(actTotalDebits_DB1, redemptionMarkExp,
				actTotalDebits_DB1 + " Total Debits in accounts table is not matching");
		logger.info("Verified total_debits is matched in accounts table " + actTotalDebits_DB1);
		utils.logPass("Verified total_debits is matched in accounts table " + actTotalDebits_DB1);

		// OLF-T2131 [New ML redemption mark is less than old] Verify user details in
		// checkins table when business configuration has membership level Redemption
		// Mark configured [User case 6]
		String query9 = "SELECT `points_earned` from `checkins` where `user_id` = '" + userID + "';";
		String actPointsEarnedDB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9,
				"points_earned", 10);
		double actPointsEarned_DB = Double.parseDouble(actPointsEarnedDB);

		Assert.assertEquals(actPointsEarned_DB, checkinAmountDouble,
				actPointsEarned_DB + " points earned  in checkins table is not matching");
		logger.info("Verified points earned  is matched in checkins table " + actPointsEarned_DB);
		utils.logPass("Verified points earned  is matched in checkins table " + actPointsEarned_DB);

		String query10 = "SELECT `points_spent` from `checkins` where `user_id` = '" + userID + "';";
		String actPointsSpentsDB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"points_spent", 10);
		double actPointsSpents_DB = Double.parseDouble(actPointsSpentsDB);

		Assert.assertEquals(actPointsSpents_DB, redemptionMarkExp,
				actPointsSpents_DB + " points spent  in checkins table is not matching");
		logger.info("Verified points spent  is matched in checkins table " + actPointsSpents_DB);
		utils.logPass("Verified points spent  is matched in checkins table " + actPointsSpents_DB);

		String query11 = "Select `status` from `reward_debits` where `user_id` = '" + userID + "';";
		String status = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11, "status", 5);

		String query12 = "Select `type` from `reward_debits` where `user_id` = '" + userID + "';";
		String type = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12, "type", 5);
		Assert.assertEquals(status, "banked", "Status in reward debits table did not matched");
		Assert.assertEquals(type, "PointDebit", "Status in reward debits table did not matched");

		String query13 = "Select `honored_reward_value` from `reward_debits` where `user_id` = '" + userID + "';";
		String honored_reward_value = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				query13, "honored_reward_value", 5);
		Assert.assertEquals(Double.parseDouble(honored_reward_value), redemptionMarkExp,
				"Status in reward debits table did not matched");

		// OLF-T2127 [New ML redemption mark is less than old] Verify user balance in
		// different Auth API's when business configuration has membership level
		// Redemption Mark configured [User case 6]
		// (api/auth/users/balance)
		// Fetch User Balance
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, client, secret);
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		double authPointsBalance = Double
				.parseDouble(authUserBalanceResponse.jsonPath().get("account_balance.points_balance").toString());
		Assert.assertEquals(authPointsBalance, expectedAvailabeBalancePoints,
				expectedAvailabeBalancePoints + " points_balance in auth/users/balance API is not matching");

		double authRedeemablePoints = Double
				.parseDouble(authUserBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString());
		Assert.assertEquals(authRedeemablePoints, expectedAvailabeBalancePoints,
				expectedAvailabeBalancePoints + " redeemable_points  in auth/users/balance API is not matching");

		int authRedeemableCurrent_membership_level_id = Integer.parseInt(
				authUserBalanceResponse.jsonPath().get("account_balance.current_membership_level_id").toString());
		Assert.assertEquals(authRedeemableCurrent_membership_level_id, membershipId1_Int,
				membershipId1_Int + " membership level ID in auth/users/balance API is not matching");

		String authMembershipLevel = authUserBalanceResponse.jsonPath()
				.getString("account_balance.current_membership_level_name").replace("[", "").replace("]", "");
		Assert.assertEquals(authMembershipLevel, expMembershipLevel,
				expMembershipLevel + " membership level in auth/users/balance API is not matching");

		// OLF-T2124 [New ML redemption mark is less than old] Verify user balance in
		// different secure API's when business configuration has membership level
		// Redemption Mark configured [User case 6]
		// api/mobile/users/balance
		Response balanceResponse = pageObj.endpoints().Api1MobileUsersbalance(token, client, secret);
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch users balance");
		double api1PointsBalance = Double.parseDouble(balanceResponse.jsonPath().get("account_balance.points_balance")
				.toString().replace("[", "").replace("]", ""));
		double api1TotalCredits = Double.parseDouble(balanceResponse.jsonPath().get("account_balance.total_credits")
				.toString().replace("[", "").replace("]", ""));
		double api1TotalDebits = Double.parseDouble(balanceResponse.jsonPath().get("account_balance.total_debits")
				.toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(api1PointsBalance, expectedAvailabeBalancePoints,
				"PointsBalance did not matched for (api/mobile/users/balance)");
		Assert.assertEquals(api1TotalCredits, checkinAmountDouble,
				"TotalCredits did not matched for (api/mobile/users/balance)");

		Assert.assertEquals(api1TotalDebits, redemptionMarkExp,
				"TotalCredits did not matched for (api/mobile/users/balance)");

		// OLF-T2125 [New ML redemption mark is less than old] Verify user balance in
		// different API2 API's when business configuration has membership level
		// Redemption Mark configured [User case 6]
		// api2/mobile/users/balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");

		System.out.println("userBalanceResponse-- " + userBalanceResponse.asPrettyString());
		double api2PointsBalance = Double.parseDouble(userBalanceResponse.jsonPath()
				.get("account_balance.points_balance").toString().replace("[", "").replace("]", ""));
		double api2RedeemablePoints = Double.parseDouble(userBalanceResponse.jsonPath()
				.get("account_balance.redeemable_points").toString().replace("[", "").replace("]", ""));
		Assert.assertEquals(api2PointsBalance, expectedAvailabeBalancePoints,
				"PointsBalance did not matched for (api/mobile/users/balance)");
		Assert.assertEquals(api2RedeemablePoints, expectedAvailabeBalancePoints,
				"TotalCredits did not matched for (api/mobile/users/balance)");

		utils.logPass("//api2/mobile/users/balance credit/debit point balance validated");

		// OLF-T2126 [New ML redemption mark is less than old] Verify user balance in
		// different V1 API's when business configuration has membership level
		// Redemption Mark configured [User case 6]
		// Api V1 checkins api/v1/checkins
		Response checkinsApiV1Response = pageObj.endpoints().checkinsApiV1(userEmail,
				Utilities.getApiConfigProperty("password"), dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");

		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins is successful");
		double points_earnedV1Checkin = Double.parseDouble(
				checkinsApiV1Response.jsonPath().getString("[0].points_earned").replace("[", "").replace("]", ""));
		double points_spentV1Checkin = Double.parseDouble(
				checkinsApiV1Response.jsonPath().getString("[0].points_spent").replace("[", "").replace("]", ""));
		double points_available = Double.parseDouble(
				checkinsApiV1Response.jsonPath().getString("[0].points_available").replace("[", "").replace("]", ""));

		Assert.assertEquals(points_earnedV1Checkin, checkinAmountDouble,
				checkinAmountDouble + " Points earned did not matched for (api/v1/checkins)");
		Assert.assertEquals(points_spentV1Checkin, redemptionMarkExp,
				redemptionMarkExp + " Points Spents did not matched for (api/v1/checkins)");
		Assert.assertEquals(points_available, expectedAvailabeBalancePoints,
				expectedAvailabeBalancePoints + " Points avaiable did not matched for (api/v1/checkins)");
		utils.logPass(
				"Verified  points_earned /points_spent/ points_available successfully in Api V1 checkins api/v1/checkins response");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
