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

public class VerifyRedemptionMarkHigherThanPreviousMark {
	private static Logger logger = LogManager.getLogger(VerifyRedemptionMarkHigherThanPreviousMark.class);
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
		apiKey = "dmazbKvNDZoAj9KrcCgz";
		locationKey = "cc33b01d7f794e13a2d9ae1d3901af8e";
		locationName = "Location with Redemption 2.0 disable";
	}

	@Test(description = "[Business having higher redemption mark than previous redemption mark]Verify user balance on user timeline for user with exact points i.e.equal to new redemption mark gets reward only when user reach the threshold & will get 2 rewards in the account history", priority = 0)
	public void T1961_VerifyRedemptionMarkHigherThanPreviousMark() throws Exception {

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

		// login to instance -> here instance is https://divya.punchh.io
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark -> 100
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkOld"));
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

		// update redeemption mark -> 120
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMarkNew"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Do checkin of 160
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey, dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		logger.info("POS checkin is successful for " + dataSet.get("amount") + " dollar");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(client, secret, token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String query4 = "Select `points_spent` from `checkins` where `id` = '" + checkin_id + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"points_spent", 5);
		logger.info("points_spent column in accounts table is equal to expected value " + expColValue4);
		utils.logit("total_debits column in accounts table is equal to expected value " + expColValue4);

		String query5 = "Select `points_earned` from `checkins` where `id` = '" + checkin_id + "';";
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"points_earned", 5);
		logger.info("points_earned column in accounts table is equal to expected value " + expColValue4);
		utils.logit("points_earned column in accounts table is equal to expected value " + expColValue4);

		int points_earned = Integer.parseInt(expColValue5);
		int points_spent = Integer.parseInt(expColValue4);
		int points_expired = 0; // utill we find a way to fetch it
		int actualPoints = (points_earned - (points_spent + points_expired));

		// Step- 3 User gets the loyalty reward on banking
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, client, secret);
		Assert.assertEquals(rewardResponse.getStatusCode(), 200,
				"Status code 200 did not match with Auth List Available Reward");
		logger.info("Auth List Available Reward is successful");
		utils.logit("Auth List Available Reward is successful");

		// Extract the rewards list and validate the count
		List<Map<String, Object>> rewardsList = rewardResponse.jsonPath().getList("");
		Assert.assertNotNull(rewardsList, "Rewards list is null");
		Assert.assertEquals(rewardsList.size(), 2, "Expected 2 rewards, but found " + rewardsList.size());
		logger.info("Verified that the user has received 2 rewards");

		// Validate details of both rewards
		String redeemableId1 = rewardsList.get(0).get("redeemable_id").toString();
		String redeemableId2 = rewardsList.get(1).get("redeemable_id").toString();

		Assert.assertEquals(redeemableId1, dataSet.get("redeemable_id_1"), "First redeemable ID mismatch: expected "
				+ dataSet.get("redeemable_id_1") + ", but got " + redeemableId1);
		Assert.assertEquals(redeemableId2, dataSet.get("redeemable_id_2"), "Second redeemable ID mismatch: expected "
				+ dataSet.get("redeemable_id_2") + ", but got " + redeemableId2);

		logger.info("Verified both rewards' redeemable IDs match expected values");
		utils.logPass("Verified both rewards' redeemable IDs match expected values");

		// Step -4 User balance is correct on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.waitTillPagePaceDone();
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		logger.info("Verified thet User balance is correct on timeline");
		utils.logPass("Verified thet User balance is correct on timeline");

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
		Assert.assertEquals(eventValue, "+Item", "reward is not reverted back to user account (Account History)");
		logger.info("verified that banking event in account history created");
		utils.logPass("verified that banking event in account history created");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
