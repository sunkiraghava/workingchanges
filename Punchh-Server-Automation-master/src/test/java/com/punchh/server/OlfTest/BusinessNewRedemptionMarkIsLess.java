package com.punchh.server.OlfTest;

import java.lang.reflect.Method;
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
public class BusinessNewRedemptionMarkIsLess {
	private static Logger logger = LogManager.getLogger(BusinessNewRedemptionMarkIsLess.class);
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

	@SuppressWarnings("static-access")
	@Test(description = "Verify user details in checkins table for user having less  points than the new redemption mark", priority = 0)
	public void T1937_BusinessNewRedemptionMarkLess() throws Exception {

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

		// Checking and Marking "enable_staged_rewards" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag4"));

		// Checking and Marking "enable_points_spent_backfill" ->
		// true in business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "false", dataSet.get("dbFlag5"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redemption mark -> 120
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
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		// String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		// String authToken =
		// signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// send reward points to user here points = 60
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", "", "",
				dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send points to the user successfully");
		TestListeners.extentTest.get().info("Send points to the user successfully");

		// update redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark2"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		TestListeners.extentTest.get().pass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Check point_spent in checkins table
		String query4 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"pointsSpentAmount", 5);
		logger.info("points_spent column in checkins table is equal to expected value " + pointsSpent);
		TestListeners.extentTest.get()
				.info("total_debits column in checkins table is equal to expected value " + pointsSpent);
		int points_spent = Integer.parseInt(pointsSpent);
		Assert.assertEquals(points_spent, 0, "point_spent is 0 as points is less than new redemption mark");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
