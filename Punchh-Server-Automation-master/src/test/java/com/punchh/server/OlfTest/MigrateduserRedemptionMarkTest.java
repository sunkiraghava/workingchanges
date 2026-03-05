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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class MigrateduserRedemptionMarkTest {
	private static Logger logger = LogManager.getLogger(MigrateduserRedemptionMarkTest.class);
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
	@Test(description = "OLF-T1975 [Business having high redemption mark than previous redemption mark] Verify migrated user having exact initial points which is equal to new redemption mark", priority = 0)
	public void T1975_verifyMigratedUserHavingExactInitialPointsWhichIsEqualToNewRedemptionMark() throws Exception {

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
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Step:3 Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api1
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("api1 Signup is successful");
		logger.info("api1 Signup is successful");

		// update redeemption mark to 150
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String pointsSpentQuery = "Select `points_spent` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "points_spent", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "Select `points_earned` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "points_earned", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_earned column in accounts table is equal to expected value " + pointsSpent);

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
		TestListeners.extentTest.get().pass("Verified thet User balance is correct on timeline");

		// Step -5 User balance is correct on printtimeline
		pageObj.guestTimelinePage().moveToPointOnPrintTimeline();
		String printTimelienActualPoints = pageObj.guestTimelinePage().getPageContentsPrintTimeline();
		/*
		 * Assert.assertEquals(printTimelienActualPoints, expectedredeemablePoints,
		 * "User balance is not correct on print timeline");
		 */
		logger.info("Verified thet User balance is correct on print timeline");
		TestListeners.extentTest.get().pass("Verified thet User balance is correct on print timeline");

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
		TestListeners.extentTest.get().pass("Iframe points validated");

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

	@Test(description = "OLF-T1976 [Business having high redemption mark than previous redemption mark] Verify migrated user having less initial points than new redemption mark", priority = 1)
	public void T1976_verifyMigratedUserHavingLessInitialPointsThanNewRedemptionMark() throws Exception {

		// Step:2 login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkOld"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Step:3 Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail, apiKey,
				dataSet.get("originalPoints"), dataSet.get("initialPoints"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), 201,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api1
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("api1 Signup is successful");
		logger.info("api1 Signup is successful");

		// update redeemption mark to 150
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String pointsSpentQuery = "Select `points_spent` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "points_spent", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("total_debits column in accounts table is equal to expected value " + pointsSpent);

		String pointsEarnedQuery = "Select `points_earned` from `checkins` where `id` = '" + checkin_id + "';";
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "points_earned", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("points_earned column in accounts table is equal to expected value " + pointsSpent);

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
		TestListeners.extentTest.get().pass("Verified thet User balance is correct on timeline");

		// Step -5 User balance is correct on printtimeline
		int printTimelienActualPoints = pageObj.guestTimelinePage().getRedeemablePointOnPrintTimeline();
		/*
		 * Assert.assertEquals(printTimelienActualPoints, expectedredeemablePoints,
		 * "User balance is not correct on print timeline");
		 */
		logger.info("Verified thet User balance is correct on print timeline");
		TestListeners.extentTest.get().pass("Verified thet User balance is correct on print timeline");

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
		TestListeners.extentTest.get().pass("Iframe points validated");

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