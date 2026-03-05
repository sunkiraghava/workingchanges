package com.punchh.server.LP1Test;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MigrationStrategyExcludeCategoryTest {
	private static Logger logger = LogManager.getLogger(MigrationStrategyExcludeCategoryTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String externalUID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4470 Verify business migration user with exclude initial points migration strategy in point unlock business.", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4470_migrationStrategy() throws Exception {

		String b_id = dataSet.get("business_id");
		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Exclude Modulus Of Initial Points From Earning", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "500", "900");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue, membershipId2,
				"Effective membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId2 + " for Migrated user");

		String query1 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId2,
				"Operative membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId2 + " for Migrated user");

		String query2 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "500.0", "Gifted points is not 500.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 500.0 for Migrated user");

		String query4 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue4, "500.0", "total_credits is not 500.0 for Migrated user");
		utils.logPass("Verified that total_credits is 500.0 for Migrated user");

		String query5 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue5, "500.0", "total_lifetime_points is not 500.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 500.0 for Migrated user");

		String query6 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "membership_level",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue6, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that membership_levels is Bronze Level for Migrated user");

		String query7 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue7, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "100");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query8 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue9, "600.0", "Gifted points is not 600.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 600.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, "600.0", "total_credits is not 600.0 for Migrated user");
		utils.logPass("Verified that total_credits is 600.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue11, "600.0", "total_lifetime_points is not 600.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 600.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that membership_levels is Bronze Level for Migrated user");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		utils.logit("Auth API Account history response is successfull");

		// verify step-7

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String redeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(redeemablePoints, "600.0", "Redeemable points is not equal to 600.0");
		utils.logPass("Redeemable points is equal to 600.0");

	}

	@Test(description = "SQ-T4471 Verify normal user journey with exclude initial points migration strategy in point unlock business. || "
			+ "SQ-T4485	Verify membership_level_qualification_points key value is not showing in users and checkin balance apis in case of exclude migration strategy", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4471_migrationStrategy() throws Exception {

		String b_id = dataSet.get("business_id");
		String membershipId1 = dataSet.get("membership1");
//		String membershipId2 = dataSet.get("membership2");
//		String membershipId3 = dataSet.get("membership3");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
//		pageObj.dashboardpage().navigateToTabs("Migration");
//		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Exclude Modulus Of Initial Points From Earning", 3);
//		pageObj.cockpitGuestPage().clickUpdateBtn();
//		boolean flag2 = pageObj.guestTimelinePage()
//				.successOrErrorConfirmationMessage("Business was successfully updated.");
//		Assert.assertTrue(flag2, "Business configuration was not updated.");
//		logger.info("Business was successfully updated.");
//		TestListeners.extentTest.get().info("Business was successfully updated.");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		// Api V1 user balance
		String password = Utilities.getApiConfigProperty("password");
		Response userBalanceApiV1Response = pageObj.endpoints().userBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(userBalanceApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 users balance");
		utils.logit("Api V1 user balance is successful");
		String memLvlQualPt = userBalanceApiV1Response.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt, null, "membership level qualification points is visible");
		utils.logPass("Verified that membership level qualification points is not visible for Api V1 user balance");

		// Api V1 checkins Balance
		Response checkinsBalanceApiV1Response = pageObj.endpoints().checkinsBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsBalanceApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		utils.logit("Api V1 checkins Balance is successful");
		String memLvlQualPt1 = checkinsBalanceApiV1Response.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt1, null, "membership level qualification points is visible");
		utils.logPass("Verified that membership level qualification points is not visible for Api V1 checkins Balance");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure api mobile users balance");
		utils.logit("Secure Account Balance is successful");
		String memLvlQualPt2 = balance_Response.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt2, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for user balance api of mobile");

		// Call user Checkins Balance api of mobile v1
		Response Api1MobileCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(Api1MobileCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api mobile Checkins Balance");
		utils.logit("Checkins Balance api of mobile is successful");
		String memLvlQualPt3 = Api1MobileCheckinsBalanceResponse.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt3, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for user Checkins Balance api");

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String memLvlQualPt4 = userBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt4, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Mobile Fetch user balance api");

		// Mobile Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		utils.logit("Mobile Account Balance is successful");
		String memLvlQualPt5 = userBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt5, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Mobile Account Balance api");

		// Auth Api Fetch user balance
		Response authApiUserBalanceResponse = pageObj.endpoints().authApiUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, authApiUserBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Auth Api Fetch user balance is successful");
		String memLvlQualPt6 = authApiUserBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt6, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Auth Api Fetch user balance");

		// Auth Api Fetch account balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		utils.logPass("Auth Api Fetch account balance is successful");
		String memLvlQualPt7 = fetchAccountBalResponse.jsonPath().getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt7, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Auth Api Fetch account balance");

		String query = "SELECT `membership_level_id` from `membership_level_histories` WHERE `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue, membershipId1,
				"Effective membership strategy is not " + membershipId1 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " for Migrated user");

		String query1 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId1,
				"Operative membership strategy is not " + membershipId1 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId1 + " for Migrated user");

		String query2 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "0.0", "Gifted points is not 0.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 0.0 for Migrated user");

		String query4 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "total_credits",
				1); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue4, null, "total_credits is not 0.0 for Migrated user");
		utils.logPass("Verified that total_credits is 0.0 for Migrated user");

		String query5 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue5, "0.0", "total_lifetime_points is not 0.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 0.0 for Migrated user");

		String query6 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "membership_level",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue6, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that membership_levels is Bronze Level for Migrated user");

		String query7 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue7, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Bronze Level for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "1000");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query8 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue9, "1000.0", "Gifted points is not 1000.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 1000.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, "1000.0", "total_credits is not 1000.0 for Migrated user");
		utils.logPass("Verified that total_credits is 1000.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue11, "1000.0", "total_lifetime_points is not 1000.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 1000.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that membership_levels is Silver Level for Migrated user");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		utils.logit("Auth API Account history response is successfull");

		// verify step-7
		// Fetch user balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String redeemablePoints = userBalanceResponse1.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(redeemablePoints, "1000.0", "Redeemable points is not equal to 1000.0");
		utils.logPass("Redeemable points is equal to 1000.0");

		String query14 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query14);
		pageObj.singletonDBUtilsObj();
		@SuppressWarnings("unused")
		int rs = DBUtils.executeUpdateQuery(env, query14);
		utils.logit("Update query to make changes in initial_points_migration_strategy is executed properly");
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T4472 Verify account reset with exclude initial points migration strategy in point unlock business.", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4472_migrationStrategy() throws Exception {

		String b_id = dataSet.get("business_id");
		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		// String membershipId3 = dataSet.get("membership3");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Exclude Modulus Of Initial Points From Earning", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "500", "900");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue, membershipId2,
				"Effective membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId2 + " for Migrated user");

		String query1 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId2,
				"Operative membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId2 + " for Migrated user");

		String query2 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "500.0", "Gifted points is not 500.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 500.0 for Migrated user");

		String query4 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue4, "500.0", "total_credits is not 500.0 for Migrated user");
		utils.logPass("Verified that total_credits is 500.0 for Migrated user");

		String query5 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue5, "500.0", "total_lifetime_points is not 500.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 500.0 for Migrated user");

		String query6 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "membership_level",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue6, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that membership_levels is Bronze Level for Migrated user");

		String query7 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue7, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "4200");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query8 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue9, "4700.0", "Gifted points is not 4700.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 4700.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, "4700.0", "total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue11, "4700.0", "total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that membership_levels is Silver Level for Migrated user");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String query14 = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue14, membershipId2,
				"Effective membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " for Migrated user");

		String query15 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query15,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue15, membershipId2,
				"Operative membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId2 + " for Migrated user");

		String query16 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue16, "0.0", "Initial points is not 0 for Migrated user");
		utils.logPass("Verified that Initial points is 0 for Migrated user");

		String query17 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query17, "gift_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue17, "4700.0", "Gifted points is not 4700.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 4700.0 for Migrated user");

		String query18 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue18 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query18, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue18, "4700.0", "total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query19 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue19 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query19,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue19, "4700.0", "total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query20 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue20 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query20,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue20, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that membership_levels is Silver Level for Migrated user");

		String query21 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue21 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query21,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue21, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Silver Level for Migrated user");

		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		utils.logit("Auth API Account history response is successfull");

		// verify step-7

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String redeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(redeemablePoints, "4700.0", "Redeemable points is not equal to 4700.0");
		utils.logPass("Redeemable points is equal to 4700.0");

		String query23 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query23);
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query23);
		Assert.assertEquals(rs1, 1,
				"Update query to make changes in initial_points_migration_strategy is not working ");
		utils.logit("Update query to make changes in initial_points_migration_strategy is executed properly");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}