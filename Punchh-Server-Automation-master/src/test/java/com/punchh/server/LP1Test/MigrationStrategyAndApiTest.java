package com.punchh.server.LP1Test;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MigrationStrategyAndApiTest {
	private static Logger logger = LogManager.getLogger(MigrationStrategyAndApiTest.class);
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

	// shashank
	@Test(description = "SQ-T4465	Verify new migration strategy should not be shown in UI for other than point unlock business"
			+ "SQ-T4466 Verify the hint set below the initial points migration strategy field for all types of businesses", priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T4465_VerifyMigrationStrategyOptions() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Migration");
		boolean isValueExist = pageObj.cockpitGuestPage()
				.verifyInitialPointsMigrationStrategyOptionsValue(dataSet.get("expectedValue"));
		Assert.assertFalse(isValueExist, dataSet.get("expectedValue")
				+ "   value should not be visible for this business " + dataSet.get("slug"));

		logger.info("Verified that " + dataSet.get("expectedValue") + "  value is not visible for the business"
				+ dataSet.get("slug"));
		TestListeners.extentTest.get().pass("Verified that " + dataSet.get("expectedValue")
				+ "  value is not visible for the business" + dataSet.get("slug"));

		boolean hintIsExist_1 = pageObj.cockpitGuestPage()
				.verifyInitialPointsMigrationStrategy(dataSet.get("hintMessage_1"));

		Assert.assertTrue(hintIsExist_1, dataSet.get("hintMessage_1") + " hint message is not visible ");

		logger.info(dataSet.get("hintMessage_1") + " hint message is visible ");
		TestListeners.extentTest.get().pass(dataSet.get("hintMessage_1") + " hint message is visible ");

		boolean hintIsExist_2 = pageObj.cockpitGuestPage()
				.verifyInitialPointsMigrationStrategy(dataSet.get("hintMessage_2"));

		Assert.assertTrue(hintIsExist_2, dataSet.get("hintMessage_2") + " hint message is not visible ");

		logger.info(dataSet.get("hintMessage_2") + " hint message is visible ");
		TestListeners.extentTest.get().pass(dataSet.get("hintMessage_2") + " hint message is visible ");

		boolean hintIsExist_3 = pageObj.cockpitGuestPage()
				.verifyInitialPointsMigrationStrategy(dataSet.get("hintMessage_3"));

		Assert.assertTrue(hintIsExist_3, dataSet.get("hintMessage_3") + " hint message is not visible ");

		logger.info(dataSet.get("hintMessage_3") + " hint message is visible ");
		TestListeners.extentTest.get().pass(dataSet.get("hintMessage_3") + " hint message is visible ");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T4487 Verify membership_level_qualification_points key value is not showing in users and checkin balance apis in any business types except point unlock", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4487_migrationStrategy() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		String query22 = "UPDATE businesses SET preferences = REPLACE(preferences, ':initial_points_migration_strategy: exclude_initial_points', ':initial_points_migration_strategy: include_initial_points') WHERE preferences LIKE '%:initial_points_migration_strategy: exclude_initial_points%'AND id= "
				+ b_id + ";";
		logger.info(query22);
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
		pageObj.cockpitGuestPage().accountReEvaluationStrategy("Include Modulus Of Initial Points In Earning", 2);
		pageObj.cockpitGuestPage().clickUpdateBtn();
		boolean flag2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Business was successfully updated.");
		Assert.assertTrue(flag2, "Business configuration was not updated.");
		logger.info("Business was successfully updated.");
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
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// Api V1 user balance
		String password = Utilities.getApiConfigProperty("password");
		Response userBalanceApiV1Response = pageObj.endpoints().userBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(userBalanceApiV1Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 users balance");
		logger.info("Api V1 user balance is successful");
		utils.logit("Api V1 user balance is successful");
		String memLvlQualPt = userBalanceApiV1Response.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt, null, "membership level qualification points is visible");
		logger.info("Verified that membership level qualification points is not visible for Api V1 user balance");
		TestListeners.extentTest.get()
				.pass("Verified that membership level qualification points is not visible for Api V1 user balance");

		// Api V1 checkins Balance
		Response checkinsBalanceApiV1Response = pageObj.endpoints().checkinsBalanceApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsBalanceApiV1Response.getStatusCode(), 200,
				"Status code 200 did not matched for api v1 checkins Balance");
		logger.info("Api V1 checkins Balance is successful");
		utils.logit("Api V1 checkins Balance is successful");
		String memLvlQualPt1 = checkinsBalanceApiV1Response.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt1, null, "membership level qualification points is visible");
		logger.info("Verified that membership level qualification points is not visible for Api V1 checkins Balance");
		TestListeners.extentTest.get()
				.pass("Verified that membership level qualification points is not visible for Api V1 checkins Balance");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), 200,
				"Status code 200 did not matched for Secure api mobile users balance");
		logger.info("Secure Account Balance is successful");
		utils.logit("Secure Account Balance is successful");
		String memLvlQualPt2 = balance_Response.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt2, null, "membership level qualification points is visible");
		logger.info(
				"Verified that membership level qualification points is not visible for user balance api of mobile");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for user balance api of mobile");

		// Call user Checkins Balance api of mobile v1
		Response Api1MobileCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(Api1MobileCheckinsBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api mobile Checkins Balance");
		logger.info("Checkins Balance api of mobile is successful");
		utils.logit("Checkins Balance api of mobile is successful");
		String memLvlQualPt3 = Api1MobileCheckinsBalanceResponse.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt3, null, "membership level qualification points is visible");
		logger.info("Verified that membership level qualification points is not visible for user Checkins Balance api");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for user Checkins Balance api");

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("Api2 Fetch user balance is successful");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is successful");
		String memLvlQualPt4 = userBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt4, null, "membership level qualification points is visible");
		logger.info(
				"Verified that membership level qualification points is not visible for Mobile Fetch user balance api");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for Mobile Fetch user balance api");

		// Fetch checkin account balance
		Response Api2CheckinAccountBalanceResponse = pageObj.endpoints()
				.Api2CheckinAccountBalance(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(200, Api2CheckinAccountBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("Api2 Fetch checkin account balance is successful");
		TestListeners.extentTest.get().pass("Api2 Fetch checkin account balance is successful");
		String memLvlQualPt5 = Api2CheckinAccountBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt5, null, "membership level qualification points is visible");
		logger.info(
				"Verified that membership level qualification points is not visible for Mobile Fetch checkin account balance api");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for Mobile Fetch checkin account balance api");

		// Auth Api Fetch user balance
		Response authApiUserBalanceResponse = pageObj.endpoints().authApiUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(200, authApiUserBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		logger.info("Auth Api Fetch user balance is successful");
		TestListeners.extentTest.get().pass("Auth Api Fetch user balance is successful");
		String memLvlQualPt6 = authApiUserBalanceResponse.jsonPath()
				.getString("account_balance.membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt6, null, "membership level qualification points is visible");
		logger.info(
				"Verified that membership level qualification points is not visible for Auth Api Fetch user balance");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for Auth Api Fetch user balance");

		// Auth Api Fetch account balance
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, fetchAccountBalResponse.getStatusCode(),
				"Status code 200 did not matched for Auth Api Fetch account balance");
		logger.info("Auth Api Fetch account balance is successful");
		TestListeners.extentTest.get().pass("Auth Api Fetch account balance is successful");
		String memLvlQualPt7 = fetchAccountBalResponse.jsonPath().getString("membership_level_qualification_points");
		Assert.assertEquals(memLvlQualPt6, null, "membership level qualification points is visible");
		logger.info(
				"Verified that membership level qualification points is not visible for Auth Api Fetch account balancei");
		TestListeners.extentTest.get().pass(
				"Verified that membership level qualification points is not visible for Auth Api Fetch account balance");
	}

	@Test(description = "SQ-T4712 Verify that the Loyalty Rewards are successfully banked based on the Operative Tier of guests when the rewards_based_on_operative_tier flag is enabled.", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4712_rewardsBasedOnOperativeTier() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		// enable flag from the db
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("label" + i));
			pageObj.settingsPage().clearBankedRedeemable();
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList1.get(i) + " is visible");
			counter++;
		}
		Assert.assertEquals(counter, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList3.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList3.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList3.get(i) + " is visible");
			counter3++;
		}
		Assert.assertEquals(counter3, Integer.parseInt(dataSet.get("expectedSize3")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize3"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// DB query for membership_level_histories updation
		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		utils.logit("membership_level_histories table updated successfully.");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// pos checkin -- 4
		Response resp4 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount4"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// pos checkin -- 5
		Response resp5 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount5"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp5.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int counter5 = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList1.get(i) + " is visible");
			counter5++;
		}
		Assert.assertEquals(counter5, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("label" + i));
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
		}

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "uncheck");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// enable flag from the db
		String query2 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue2, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");
	}

	//{bronze : 0-200, silver: 201-1000, gold :1000-1200}
	@Test(description = "SQ-T4713 Verify that the Loyalty Rewards are not impacted when the rewards_based_on_operative_tier flag is disabled (default behavior).", priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T4713_rewardsBasedOnOperativeTier() throws Exception {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		utils.logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		utils.logit("went_live value is updated to false");

		// enable flag from the db
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("label" + i));
			pageObj.settingsPage().clearBankedRedeemable();
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList1.get(i) + " is visible");
			counter++;
		}
		Assert.assertEquals(counter, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList3.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList3.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList3.get(i) + " is visible");
			counter3++;
		}
		Assert.assertEquals(counter3, Integer.parseInt(dataSet.get("expectedSize3")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize3"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		TestListeners.extentTest.get().pass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// DB query for membership_level_histories updation
		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		String query5 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		utils.logit("membership_level_histories table updated successfully.");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// pos checkin -- 4
		Response resp4 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount4"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// pos checkin -- 5
		Response resp5 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount5"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp5.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int counter5 = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			TestListeners.extentTest.get().pass("Verified point range " + rangeList1.get(i) + " is visible");
			counter5++;
		}
		Assert.assertEquals(counter5, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("label" + i));
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("label" + i) + " is successfully updated ");
		}

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "uncheck");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T4345 [Points Unlock Redeemable]Verify v2 user profile api optimisation", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T4345_profileApiOptimisation() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("Api2 user signup is successful");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// fetch user profile detals
		Response fetchUserResponse = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(fetchUserResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("Api2 user fetch user profile detals is successful");
		TestListeners.extentTest.get().pass("Api2 user fetch user profile detals is successful");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}