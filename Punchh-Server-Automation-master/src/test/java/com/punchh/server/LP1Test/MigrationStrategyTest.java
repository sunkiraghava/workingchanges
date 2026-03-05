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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MigrationStrategyTest {
	private static Logger logger = LogManager.getLogger(MigrationStrategyTest.class);
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
		prop = Utilities.loadPropertiesFile("config.properties");
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

	@Test(description = "SQ-T4460 Verify new migration strategy is showing in UI for point unlock business || "
			+ "SQ-T4461	Verify business migration user with new migration strategy(Consider only initial points in earning) in point unlock business.", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4460_migrationStrategy() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessNewNavFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessNewNavFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Consider Only Initial Points For Tier Migration", 3);
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
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "500", "900");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
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

		String query2 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "loyalty_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "900.0", "Gifted points is not 900.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 900.0 for Migrated user");

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
		Assert.assertEquals(expColValue7, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Bronze Level for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "100");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query8 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "loyalty_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "100.0", "Initial points is not 100.0 for Migrated user");
		utils.logPass("Verified that Initial points is 100.0 for Migrated user");

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
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String membershipLvlpts = userBalanceResponse.jsonPath()
				.get("account_balance.membership_level_qualification_points").toString();
		Assert.assertEquals(membershipLvlpts, "1000.0", "membership_level_qualification_points didn't match");
		utils.logPass("membership_level_qualification_points match which is " + membershipLvlpts);

	}

	@Test(description = "SQ-T4463 Verify account reset with new migration strategy(Consider only initial points in earning) in point unlock business.", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4463_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Consider Only Initial Points For Tier Migration", 3);
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
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
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

		String query2 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "loyalty_points",
				25); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, "0.0", "Initial points is not 0.0 for Migrated user");
		utils.logPass("Verified that Initial points is 0.0 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "900.0", "Gifted points is not 900.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 900.0 for Migrated user");

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
		Assert.assertEquals(expColValue7, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Bronze Level for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "4200");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query8 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "loyalty_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "4200.0", "Initial points is not 4200.0 for Migrated user");
		utils.logPass("Verified that Initial points is 4200.0 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertTrue((expColValue9.contains("5100.0") || expColValue9.contains("5105.0")),
				"Gifted points is not 5100.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 5100.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue10.contains("4700.0") || expColValue10.contains("4705.0")),
				"total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue11.contains("4700.0") || expColValue11.contains("4705.0")),
				"total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that membership_levels is Gold Level for Migrated user");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Gold Level for Migrated user");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String query14 = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue14, membershipId3,
				"Effective membership strategy is not " + membershipId3 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId3 + " for Migrated user");

		String query15 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query15,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue15, membershipId3,
				"Operative membership strategy is not " + membershipId3 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId3 + " for Migrated user");

		String query16 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue16.contains("900.0") || expColValue16.contains("905.0")),
				"Initial points is not 900.0 for Migrated user");
		utils.logPass("Verified that Initial points is 900.0 for Migrated user");

		String query17 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query17, "gift_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue17.contains("5100.0") || expColValue17.contains("5105.0")),
				"Gifted points is not 0.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 0.0 for Migrated user");

		String query18 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue18 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query18, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue18.contains("4700.0") || expColValue18.contains("4705.0")),
				"total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query19 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue19 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query19,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue19.contains("4700.0") || expColValue19.contains("4705.0")),
				"total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query20 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue20 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query20,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue20, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that membership_levels is Gold Level for Migrated user");

		String query21 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue21 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query21,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue21, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Gold Level for Migrated user");

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
		Assert.assertTrue((redeemablePoints.contains("4700.0") || redeemablePoints.contains("4705.0")),
				"Redeemable points is not equal to 4700.0");
		utils.logPass("Redeemable points is equal to 4700.0");

	}

	@Test(description = "SQ-T4483 Verify previous membership level updated in accounts table for new user with consider only initial points for tier migration strategy || "
			+ "SQ-T4484	Verify membership_level_qualification_points key value is showing in users and checkin balance apis in case of consider only initial points migration strategy", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4483_migrationStrategy() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Consider Only Initial Points For Tier Migration", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		// Navigate to cockpit -> Misc. Config. -> Enable API v1?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_api_v1", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query7 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue7, "Bronze Level", "membership_levels is not Bronze Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Bronze Level for Migrated user");

		// Api V1 user balance
		String password = Utilities.getApiConfigProperty("password");
		Response userBalanceApiV1Response = pageObj.endpoints().userBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(userBalanceApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 users balance");
		boolean isApi1UserBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1UserBalanceSchema, userBalanceApiV1Response.asString());
		Assert.assertTrue(isApi1UserBalanceSchemaValidated, "API1 User Balance Schema Validation failed");
		utils.logit("Api V1 user balance is successful");
		String memLvlQualPt = userBalanceApiV1Response.jsonPath()
				.get("account_balance.membership_level_qualification_points").toString();
		Assert.assertEquals(memLvlQualPt, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for Api V1 user balance");

		// Api V1 checkins Balance
		Response checkinsBalanceApiV1Response = pageObj.endpoints().checkinsBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsBalanceApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 checkins Balance");
		boolean isApi1CheckinsBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1CheckinsBalanceSchema, checkinsBalanceApiV1Response.asString());
		Assert.assertTrue(isApi1CheckinsBalanceSchemaValidated, "API1 Checkins Balance Schema Validation failed");
		utils.logit("Api V1 checkins Balance is successful");
		String memLvlQualPt1 = checkinsBalanceApiV1Response.jsonPath().get("membership_level_qualification_points")
				.toString();
		Assert.assertEquals(memLvlQualPt1, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for Api V1 checkins balance");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure api mobile users balance");
		utils.logit("Secure Account Balance is successful");
		String memLvlQualPt2 = balance_Response.jsonPath().get("account_balance.membership_level_qualification_points")
				.toString();
		Assert.assertEquals(memLvlQualPt2, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for user balance api of mobile");

		// Call user Checkins Balance api of mobile v1
		Response Api1MobileCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(Api1MobileCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api mobile Checkins Balance");
		utils.logit("Checkins Balance api of mobile is successful");
		String memLvlQualPt3 = Api1MobileCheckinsBalanceResponse.jsonPath().get("membership_level_qualification_points")
				.toString();
		Assert.assertEquals(memLvlQualPt3, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for user Checkins Balance api");

		// Mobile Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String memLvlQualPt4 = userBalanceResponse.jsonPath()
				.get("account_balance.membership_level_qualification_points").toString();
		Assert.assertEquals(memLvlQualPt4, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for Mobile Fetch user balance");

		// Mobile Fetch checkin account balance
		Response Api2CheckinAccountBalanceResponse = pageObj.endpoints()
				.Api2CheckinAccountBalance(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, Api2CheckinAccountBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch checkin account balance is successful");
		String memLvlQualPt5 = Api2CheckinAccountBalanceResponse.jsonPath()
				.get("account_balance_details.membership_level_qualification_points").toString();
		Assert.assertEquals(memLvlQualPt5, "0.0", "membership level qualification points is not equal");
		utils.logPass(
				"Verified that membership level qualification points is equal for Mobile Fetch checkin account balance");

		// Auth Api Fetch user balance
		Response authApiUserBalanceResponse = pageObj.endpoints().authApiUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, authApiUserBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Auth Api Fetch user balance is successful");
		String memLvlQualPt6 = authApiUserBalanceResponse.jsonPath()
				.get("account_balance.membership_level_qualification_points").toString();
		Assert.assertEquals(memLvlQualPt6, "0.0", "membership level qualification points is not equal");
		utils.logPass("Verified that membership level qualification points is equal for Auth Api Fetch user balance");

		// Auth Api Fetch account balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		utils.logPass("Auth Api Fetch account balance is successful");
		String memLvlQualPt7 = fetchAccountBalResponse.jsonPath().get("membership_level_qualification_points")
				.toString();
		Assert.assertEquals(memLvlQualPt7, "0.0", "membership level qualification points is not equal");
		utils.logPass(
				"Verified that membership level qualification points is equal for Auth Api Fetch account balance");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "5500");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Gold Level for Migrated user");

	}

	@Test(description = "SQ-T4462 Verify normal user journey with new migration strategy(Consider only initial points in earning) in point unlock business.", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4462_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String membershipId1 = dataSet.get("membership1");
//		String membershipId2 = dataSet.get("membership2");
//		String membershipId3 = dataSet.get("membership3");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
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

		String query2 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "loyalty_points",
				2); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, null, "Initial points is not null for Migrated user");
		utils.logPass("Verified that Initial points is null for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "0.0", "Gifted points is not 0.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 0.0 for Migrated user");

		String query4 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "total_credits",
				2); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue4, null, "total_credits is not null for Migrated user");
		utils.logPass("Verified that total_credits is null for Migrated user");

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

		String query8 = "SELECT `loyalty_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8, "loyalty_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, "1000.0", "Initial points is not 1000.0 for Migrated user");
		utils.logPass("Verified that Initial points is 1000.0 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
		Assert.assertTrue((expColValue9.contains("1000.0") || expColValue9.contains("1002.0")),
				"Gifted points is not 1000.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 1000.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue10.contains("1000.0") || expColValue10.contains("1002.0")),
				"total_credits is not 1000.0 for Migrated user");
		utils.logPass("Verified that total_credits is 1000.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue11.contains("1000.0") || expColValue11.contains("1002.0")),
				"total_lifetime_points is not 1000.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 1000.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Silver Level", "membership_levels is not Bronze Level for Migrated user");
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
		Assert.assertTrue((redeemablePoints.contains("1000.0") || redeemablePoints.contains("1002.0")),
				"Redeemable points is not equal to 1000.0");
		utils.logPass("Redeemable points is equal to 1000.0");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T4464 Verify guest profile segment users count with membership tier attribute in all three types of strategies || "
			+ "SQ-T4473	Verify previous membership level updated in accounts table for migrated user with exclude strategy", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T4464_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 3);
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

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().authApiSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken1 = signUpResponse1.jsonPath().get("authentication_token");
		String token1 = signUpResponse1.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID1 = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().authApiSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken2 = signUpResponse2.jsonPath().get("authentication_token");
		String token2 = signUpResponse2.jsonPath().get("access_token").toString();
		String userID2 = signUpResponse2.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().authApiSignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken3 = signUpResponse3.jsonPath().get("authentication_token");
		String token3 = signUpResponse3.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID3 = signUpResponse3.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID1
				+ "';";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue, membershipId1,
				"Effective membership strategy is not " + membershipId1 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " for Migrated user");

		String query1 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID1 + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId1,
				"Operative membership strategy is not " + membershipId1 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId1 + " for Migrated user");

		// do a checkin on the user
		Response resp = pageObj.endpoints().posCheckin(userEmail2, dataSet.get("locationkey"), "5050");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String query14 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID2
				+ "';";
		pageObj.singletonDBUtilsObj();
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue14, membershipId1,
				"Effective membership strategy is not " + membershipId1 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " for Migrated user");

		String query15 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID2 + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query15,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue15, membershipId3,
				"Operative membership strategy is not " + membershipId3 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId3 + " for Migrated user");

		// do a checkin on the user
		Response resp2 = pageObj.endpoints().posCheckin(userEmail3, dataSet.get("locationkey"), "5050");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query16 = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID3 + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue16, membershipId3,
				"Effective membership strategy is not " + membershipId3 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId3 + " for Migrated user");

		String query17 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId1 + "' AND `user_id` = '" + userID3 + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query17,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue17, membershipId3,
				"Operative membership strategy is not " + membershipId3 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId3 + " for Migrated user");

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Consider Only Initial Points For Tier Migration", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag3 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag3, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		// user in Segment
		Response userInSegmentResp = pageObj.endpoints().userInSegment(userEmail1, dataSet.get("apiKey"),
				dataSet.get("segmentID1"));
		Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Bronze Level");

		// user in Segment
		Response userInSegmentResp3 = pageObj.endpoints().userInSegment(userEmail2, dataSet.get("apiKey"),
				dataSet.get("segmentID1"));
		Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Bronze Level");

		// user in Segment
		Response userInSegmentResp1 = pageObj.endpoints().userInSegment(userEmail3, dataSet.get("apiKey"),
				dataSet.get("segmentID2"));
		Assert.assertEquals(userInSegmentResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail2 + " is present in Segment Membership Silver Level");

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag4 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag4, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		// user in Segment
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail1, dataSet.get("apiKey"),
				dataSet.get("segmentID1"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Bronze Level");

		// user in Segment
		Response userInSegmentResp4 = pageObj.endpoints().userInSegment(userEmail2, dataSet.get("apiKey"),
				dataSet.get("segmentID1"));
		Assert.assertEquals(userInSegmentResp4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Bronze Level");

		// user in Segment
		Response userInSegmentResp5 = pageObj.endpoints().userInSegment(userEmail3, dataSet.get("apiKey"),
				dataSet.get("segmentID2"));
		Assert.assertEquals(userInSegmentResp5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail2 + " is present in Segment Membership Silver Level");

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Exclude Modulus Of Initial Points From Earning", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag5 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag5, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		// user in Segment
		Response userInSegmentResp6 = pageObj.endpoints().userInSegment(userEmail1, dataSet.get("apiKey"),
				dataSet.get("segmentID1"));
		Assert.assertEquals(userInSegmentResp6.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Bronze Level");

		// user in Segment
		Response userInSegmentResp7 = pageObj.endpoints().userInSegment(userEmail2, dataSet.get("apiKey"),
				dataSet.get("segmentID3"));
		Assert.assertEquals(userInSegmentResp7.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail1 + " is present in Segment Membership Gold Level");

		// user in Segment
		Response userInSegmentResp8 = pageObj.endpoints().userInSegment(userEmail3, dataSet.get("apiKey"),
				dataSet.get("segmentID3"));
		Assert.assertEquals(userInSegmentResp8.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail2 + " is present in Segment Membership Gold Level");
		String result = userInSegmentResp8.jsonPath().get("result").toString();

		String query23 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query23);
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query23);
//		Assert.assertEquals(rs1, 1, "Update query to make changes in initial_points_migration_strategy is not working ");
		utils.logit("Update query to make changes in initial_points_migration_strategy is executed properly");

	}

	@Test(description = "SQ-T4467 Verify business migration user with include initial points migration strategy in point unlock business. || "
			+ "SQ-T4476 (1.0) Verify previous membership level updated in accounts table for new user with include strategy ", priority = 6)
	@Owner(name = "Hardik Bhardwaj")
	public void T4467_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 3);
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
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
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
		Assert.assertEquals(expColValue2, "900.0", "Initial points is not 900 for Migrated user");
		utils.logPass("Verified that Initial points is 900 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "1400.0", "Gifted points is not 1400.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 1400.0 for Migrated user");

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
		Assert.assertEquals(expColValue6, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that membership_levels is Silver Level for Migrated user");

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
		Assert.assertEquals(expColValue8, "900.0", "Initial points is not 900 for Migrated user");
		utils.logPass("Verified that Initial points is 900 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue9, "1500.0", "Gifted points is not 1500.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 1500.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, "600.0", "total_credits is not 600 for Migrated user");
		utils.logPass("Verified that total_credits is 600 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue11, "600.0", "total_lifetime_points is not 600 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 600 for Migrated user");

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
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String redeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(redeemablePoints, "1500.0", "Redeemable points is not equal to 1500.0");
		utils.logPass("Redeemable points is equal to 1500.0");

		/*---SQ-T4476----*/

		// user sign-up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpRes = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String user_id = Integer.toString(signUpRes.jsonPath().get("user_id"));

		String query50 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + user_id + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue50 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query50,
				"previous_membership_level", 10);
		Assert.assertEquals(expColValue50, "Bronze Level", "membership_levels is not Bronze Level for new user");
		utils.logPass("Verified that previous_membership_level is Bronze Level for new user");

		// user check-in
		Response resp50 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "6000");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp50.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.logPass("POS checkin is successfull");

		String query51 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + user_id + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue51 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query51,
				"previous_membership_level", 10);
		Assert.assertEquals(expColValue51, "Gold Level", "membership_levels is not Gold Level for new user");
		utils.logPass("Verified that previous_membership_level Updated and is Gold Level for new user");

	}

	@Test(description = "SQ-T4486 Verify membership_level_qualification_points key value is not showing in users and checkin balance apis in case of include migration strategy", priority = 7)
	@Owner(name = "Hardik Bhardwaj")
	public void T4486_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 3);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");
		utils.logit("Business was successfully updated.");

		// Navigate to cockpit -> Misc. Config. -> Enable API v1?
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_api_v1", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
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

		// Fetch checkin account balance
		Response Api2CheckinAccountBalanceResponse = pageObj.endpoints()
				.Api2CheckinAccountBalance(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, Api2CheckinAccountBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch checkin account balance is successful");
		String memLvlQualPt5 = Api2CheckinAccountBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt5, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Mobile Fetch checkin account balance api");

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
		Assert.assertEquals(memLvlQualPt6, null, "membership level qualification points is visible");
		utils.logPass(
				"Verified that membership level qualification points is not visible for Auth Api Fetch account balance");
	}

	@Test(description = "SQ-T4468 Verify normal user journey with include initial points migration strategy in point unlock business.", priority = 8)
	@Owner(name = "Hardik Bhardwaj")
	public void T4468_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");

		String query = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
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
		String expColValue4 = DBUtils.executeQueryAndGetColumnValue(env, query4, "total_credits"); // dbUtils.getValueFromColumn(query2,
																											// "membership_level_id");
		Assert.assertEquals(expColValue4, null, "total_credits is not NULL for Migrated user");
		utils.logPass("Verified that total_credits is NULL for Migrated user");

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
		Assert.assertEquals(expColValue8, "0.0", "Initial points is not 900 for Migrated user");
		utils.logPass("Verified that Initial points is 900 for Migrated user");

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
		Assert.assertEquals(expColValue11, "1000.0", "total_lifetime_points is not 1000 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 1000 for Migrated user");

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
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");
		String redeemablePoints = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		Assert.assertEquals(redeemablePoints, "1000.0", "Redeemable points is not equal to 1000");
		utils.logPass("Redeemable points is equal to 1000");
	}

	@Test(description = "SQ-T4469 Verify account reset with include initial points migration strategy in point unlock business. || "
			+ "SQ-T4478	Verify previous membership level updated in accounts table with guest inactivity", priority = 9)
	@Owner(name = "Hardik Bhardwaj")
	public void T4469_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String membershipId1 = dataSet.get("membership1");
		String membershipId2 = dataSet.get("membership2");
		String membershipId3 = dataSet.get("membership3");

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
				utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		utils.logit(query22);
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query22);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Guest -> Migration Tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Migration");
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 3);
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
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// User SignUp using API
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
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
		Assert.assertEquals(expColValue2, "900.0", "Initial points is not 900 for Migrated user");
		utils.logPass("Verified that Initial points is 900 for Migrated user");

		String query3 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue3, "1400.0", "Gifted points is not 1400.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 1400.0 for Migrated user");

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
		Assert.assertEquals(expColValue6, "Silver Level", "membership_levels is not Silver Level for Migrated user");
		utils.logPass("Verified that membership_levels is Silver Level for Migrated user");

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
		Assert.assertEquals(expColValue8, "900.0", "Initial points is not 900 for Migrated user");
		utils.logPass("Verified that Initial points is 900 for Migrated user");

		String query9 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertTrue((expColValue9.contains("5600.0") || expColValue9.contains("5605.0")),
				"Gifted points is not 5600.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 5600.0 for Migrated user");

		String query10 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue10.contains("4700.0") || expColValue10.contains("4705.0")),
				"total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query11 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue11.contains("4700.0") || expColValue11.contains("4705.0")),
				"total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query12 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query12,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue12, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that membership_levels is Gold Level for Migrated user");

		String query13 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue13, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Gold Level for Migrated user");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		String query14 = "SELECT `membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId2 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query14,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue14, membershipId3,
				"Effective membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Effective membership strategy is " + membershipId3 + " for Migrated user");

		String query15 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `previous_membership_level_id` = '"
				+ membershipId2 + "' AND `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query15,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue15, membershipId3,
				"Operative membership strategy is not " + membershipId2 + " for Migrated user");
		utils.logPass("Verified that Operative membership strategy is " + membershipId3 + " for Migrated user");

		String query16 = "SELECT `initial_credit` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query16, "initial_credit",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue16, "900.0", "Initial points is not 900.0 for Migrated user");
		utils.logPass("Verified that Initial points is 900.0 for Migrated user");

		String query17 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query17, "gift_points",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue17.contains("5600.0") || expColValue17.contains("5605.0")),
				"Gifted points is not 5600.0 for Migrated user");
		utils.logPass("Verified that Gifted points is 5600.0 for Migrated user");

		String query18 = "SELECT `total_credits` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue18 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query18, "total_credits",
				10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue18.contains("4700.0") || expColValue18.contains("4705.0")),
				"total_credits is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_credits is 4700.0 for Migrated user");

		String query19 = "SELECT `total_lifetime_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue19 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query19,
				"total_lifetime_points", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertTrue((expColValue19.contains("4700.0") || expColValue19.contains("4705.0")),
				"total_lifetime_points is not 4700.0 for Migrated user");
		utils.logPass("Verified that total_lifetime_points is 4700.0 for Migrated user");

		String query20 = "SELECT `membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue20 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query20,
				"membership_level", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue20, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that membership_levels is Gold Level for Migrated user");

		String query21 = "SELECT `previous_membership_level` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue21 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query21,
				"previous_membership_level", 10); // dbUtils.previous_membership_level(query2, "membership_level_id");
		Assert.assertEquals(expColValue21, "Gold Level", "membership_levels is not Gold Level for Migrated user");
		utils.logPass("Verified that previous_membership_level is Gold Level for Migrated user");

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
		Assert.assertTrue((redeemablePoints.contains("5600.0") || redeemablePoints.contains("5605.0")),
				"Redeemable points is not equal to 5600.0");
		utils.logPass("Redeemable points is equal to 5600.0");

	}

	@Test(description = "SQ-T4493 Verify the API's v1, v1 secure and v2 for gift_points column for the enable_decoupling_api_usage feature", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4493_enableDecouplingApiUsage() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), blankString,
				blankString, blankString, "200");
		utils.logit("Send point to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// Secure Api User Membership Level
		Response Api1UserMembershipLevelResp = pageObj.endpoints().Api1UserMembershipLevel(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(Api1UserMembershipLevelResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Secure Api User Membership Level");
		utils.logit("Secure Api User Membership Level is successful");
		String membershipLvlPt = Api1UserMembershipLevelResp.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt, "200",
				"membership level qualification points in not equal to expected value in Secure Api User Membership Level");
		utils.logPass("Verified that in Secure Api User Membership Level membership_level_qualification_points "
				+ membershipLvlPt);

		// Api V1 user membership level
		String password = Utilities.getApiConfigProperty("password");
		Response userMembershipLevelsApiV1Response = pageObj.endpoints().userMembershipLevelsApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(userMembershipLevelsApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 users balance");
		boolean isApiV1UserMembershipLevelSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1MembershipLevelSchema, userMembershipLevelsApiV1Response.asString());
		Assert.assertTrue(isApiV1UserMembershipLevelSchemaValidated,
				"Api V1 user Membership level Schema Validation failed");
		utils.logit("Api V1 user membership level is successful");
		String membershipLvlPt1 = userMembershipLevelsApiV1Response.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt1, "200",
				"membership level qualification points in not equal to expected value in Api V1 user membership level");
		utils.logPass("Verified that in Api V1 user membership level membership_level_qualification_points "
				+ membershipLvlPt1);

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logit("Api2 Fetch user membership level is successful");
		String membershipLvlPt2 = userMembershipLevelsApiV1Response.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt2, "200",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt2);

		String query2 = "SELECT `gift_points` FROM `accounts` WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "gift_points", 10); // dbUtils.getValueFromColumn(query2,
																															// "membership_level_id");
		Assert.assertEquals(expColValue2, "200.0", "gift points is not 200.0 for Migrated user");
		utils.logPass("Verified that gift points is 200.0 for Migrated user");

		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
