package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RollingCheckinSumTest {
	static Logger logger = LogManager.getLogger(RollingCheckinSumTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	String BlankSpace = "";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6221 Verify Points spent update for PUR redemption || "
			+ "SQ-T6222 Verify Points spent update for PUR force redemption || "
			+ "SQ-T6227 Verify API api/pos/redemptions PUR redemption || "
			+ "SQ-T6230 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR || "
			+ "SQ-T2182, Verify Guest timeline and Account history in point unlock business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6221_rollingCheckinSumPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS CHeckin - 30 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// check redeemable get Unlocked after checkin
		boolean redeemable_1_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_10Points"));
		boolean redeemable_2_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_30Points"));
		Assert.assertTrue(redeemable_1_Unlocked,
				dataSet.get("redeemable_name_10Points") + " redeemable is not unlocked");
		Assert.assertTrue(redeemable_2_Unlocked,
				dataSet.get("redeemable_name_30Points") + " redeemable is not unlocked");
		utils.logPass("Verified that checkin of 30 Points should be successfull and redeemable should get unlocked");

		int actualPoints = (Integer.parseInt(dataSet.get("amount")) * 3);
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of first checkin
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, actualPoints + " points earned ", "description");
		Assert.assertEquals(checkinStatus, true, "Checkin is not logged from Account History");
		utils.logPass("Checkin is logged from Account History");

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue3, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue4, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after first redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		// check account history entry of first redemption
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");
		utils.longWaitInSeconds(6);
		// second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue5, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue6, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after Second redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		utils.longWaitInSeconds(6);
		// force redemption of 6 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "6");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 6 points is successful");

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue7, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue8, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("6.0 redeemable"), "Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6231 Verify Points spent update for PUR redemption with flag OFF || "
			+ "SQ-T6232 Verify Points spent update for PUR force redemption with Flag OFF || "
			+ "SQ-T6237 Verify API api/pos/redemptions PUR redemption with Flag OFF || "
			+ "SQ-T6240 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR with Flag OFF", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T6231_rollingCheckinSumPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS CHeckin - 30 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// check redeemable get Unlocked after checkin
		boolean redeemable_1_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_10Points"));
		boolean redeemable_2_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_30Points"));
		Assert.assertTrue(redeemable_1_Unlocked,
				dataSet.get("redeemable_name_10Points") + " redeemable is not unlocked");
		Assert.assertTrue(redeemable_2_Unlocked,
				dataSet.get("redeemable_name_30Points") + " redeemable is not unlocked");
		utils.logPass("Verified that checkin of 30 Points should be successfull and redeemable should get unlocked");

		int actualPoints = (Integer.parseInt(dataSet.get("amount")) * 3);
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of first checkin
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, actualPoints + " points earned ", "description");
		Assert.assertEquals(checkinStatus, true, "Checkin is not logged from Account History");
		utils.logPass("Checkin is logged from Account History");

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue3, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue4, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after first redemption, No data should get updated in account summaries table if account summary flag is off");

		// check account history entry of first redemption
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");

		// second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue5, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue6, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after Second redemption, No data should get updated in account_summaries table if account summary flag is off");

		utils.longWaitInSeconds(6);
		// force redemption of 6 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "6");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 6 points is successful");

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue7, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue8, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after force redemption, No data should get updated in account_summaries table if account summary flag is off");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("6.0 redeemable"), "Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		Assert.assertNotEquals(pointsSpent, "0", "Checkins table is not updating");
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6223 Verify Points spent update for PTC redemption || "
			+ "SQ-T6224 Verify Points spent update for PTC force redemption || "
			+ "SQ-T6229 Verify API api/auth/redemptions PTC redemption || "
			+ "SQ-T6230 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6223_rollingCheckinSumPTC() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("redemptionMark")) / 10);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemableRewardCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redemptionMark") + " points converted ", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History");

		utils.longWaitInSeconds(5);

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue3, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue4, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after first checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a checkin is successfully completed.");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue5, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue6, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after Second checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a checkin is successfully completed.");

		// check account history entry of Second checkin
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("amount2") + " points earned ", "description");
		Assert.assertEquals(redemptionStatus, true, "Second checkin is not logged from Account History");
		utils.logPass("Second checkin is logged from Account History");

		utils.longWaitInSeconds(6);
		// force redemption of 6 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "2", "points");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 2 points is successful");

		utils.longWaitInSeconds(5);

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue7, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue8, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("2.0 redeemable"), "Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		utils.longWaitInSeconds(5);

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue9, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue10, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after currency redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a currency redemption is successfully completed.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6235 Verify Points spent update for PTC redemption with Flag OFF || "
			+ "SQ-T6236 Verify Points spent update for PTC force redemption with Flag OFF || "
			+ "SQ-T6239 Verify API api/auth/redemptions PTC redemption with Flag OFF || "
			+ "SQ-T6240 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6235_rollingCheckinSumPTC() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("redemptionMark")) / 10);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemableRewardCount();
		Assert.assertEquals(amountCheckin, actualPoints, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redemptionMark") + " points converted ", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue3, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue4, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after first checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a checkin is successfully completed.");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue5, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue6, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after Second checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a checkin is successfully completed.");

		// check account history entry of Second checkin
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("amount2") + " points earned ", "description");
		Assert.assertEquals(redemptionStatus, true, "Second checkin is not logged from Account History");
		utils.logPass("Second checkin is logged from Account History");

		utils.longWaitInSeconds(6);
		// force redemption of 6 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "2", "points");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 2 points is successful");

		utils.longWaitInSeconds(5);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue7, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue8, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().getExpiredRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("2.0 expired") || amount.contains("2.0 redeemable"),
				"Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		utils.longWaitInSeconds(5);

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue9, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue10, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after currency redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a currency redemption is successfully completed.");

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		Assert.assertNotEquals(pointsSpent, "0", "Checkins table is not updating");
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6226 Verify Points spent update for PTR redemption || "
			+ "SQ-T6225 Verify Points spent update for PTR force redemption || "
			+ "SQ-T6228 Verify API api/auth/redemptions PTR redemption || "
			+ "SQ-T6230 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR || "
			+ "SQ-T2267, Verify Guest timeline and Account history in point to reward business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6226_rollingCheckinSumPTR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("amount")) - Integer.parseInt(dataSet.get("redemptionMark")));

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(((amountCheckin == actualPoints) || (amountCheckin == 105)),
				"User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "You were gifted: Base Redeemable (Loyalty Reward)", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History and user get Base Redeemable");

		utils.longWaitInSeconds(5);

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue3, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue4, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after first checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a checkin is successfully completed.");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue5, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue6, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after Second checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a checkin is successfully completed.");

		// check account history entry of Second checkin
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("amount2") + " points earned ", "description");
		Assert.assertEquals(redemptionStatus, true, "Second checkin is not logged from Account History");
		utils.logPass("Second checkin is logged from Account History");

		// force redemption of 5 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 5 points is successful");

		utils.longWaitInSeconds(5);

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue7, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue8, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("5.0 redeemable"), "Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		utils.longWaitInSeconds(5);

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue9, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue10, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after currency redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a currency redemption is successfully completed.");

		// check account history entry of redemption
		boolean redemptionStatus1 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name"), "description");
		Assert.assertEquals(redemptionStatus1, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6233 Verify Points spent update for PTR redemption with flag OFF || "
			+ "SQ-T6234 Verify Points spent update for PTR force redemption with flag OFF || "
			+ "SQ-T6238 Verify API api/auth/redemptions PTR redemption with Flag OFF || "
			+ "SQ-T6230 Verify Account history & timeline after every event like redemption , force redemption  for businesses PUR, PTC , PTR", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6233_rollingCheckinSumPTR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("amount")) - Integer.parseInt(dataSet.get("redemptionMark")));

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(((amountCheckin == actualPoints) || (amountCheckin == 105)),
				"User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "You were gifted: Base Redeemable (Loyalty Reward)", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History and user get Base Redeemable");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue3, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue4, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after first checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a checkin is successfully completed.");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(5);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table
		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue5, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue6, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after Second checkin, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a checkin is successfully completed.");

		// check account history entry of Second checkin
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("amount2") + " points earned ", "description");
		Assert.assertEquals(redemptionStatus, true, "Second checkin is not logged from Account History");
		utils.logPass("Second checkin is logged from Account History");

		// force redemption of 5 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"unbanked_points_redemption", "requested_punches", "5", "points");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 5 points is successful");

		utils.longWaitInSeconds(5);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue7, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue8, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		utils.longWaitInSeconds(5);

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue9, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue10, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after currency redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ BlankSpace + " points only when a currency redemption is successfully completed.");

		// check account history entry of redemption
		boolean redemptionStatus1 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name"), "description");
		Assert.assertEquals(redemptionStatus1, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");

		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		Assert.assertNotEquals(pointsSpent, "0", "Checkins table is not updating");
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6243 Verify account_summaries is updated on void redemption || "
			+ "SQ-T6244 Validate checkins.points_spent is updated after void || "
			+ "SQ-T6246 Verify partial point spend and void behaviour || "
			+ "SQ-T6245 Verify  account_summaries reflects multiple voids accurately || "
			+ "SQ-T6249 Validate checkins.points_spent and account_summaries is updated after void force redemption", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6243_rollingCheckinSumVoidPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS Checkin - 14 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// POS Checkin - 18 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

//		int actualPoints = ((Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("amount1"))) * 3);

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));
		int redemption_id = posRedeem.jsonPath().get("redemption_id");

		// sum of points spent in checkins table
		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue3, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue4, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after first redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		// check account history entry of first redemption
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id),
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, voidResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		utils.logPass("Void redemption API is successful for " + redemption_id);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);
		Assert.assertEquals(pointsSpent, "0",
				"points_spent column in checkins table is not equal to expected value i.e. 0");

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue5, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue5, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. 0");

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue6, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue6, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 0");
		utils.logPass(
				"verified that after void redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a void redemption is successfully completed.");

		// check account history entry of first void redemption
		boolean redemptionStatus1 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus1, false,
				"Redeemable redemption is logged in Account History after void redemption");
		utils.logPass("Redeemable redemption is not logged in Account History after void redemption");

		utils.longWaitInSeconds(6);
		// second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));
		int redemption_id2 = posRedeem2.jsonPath().get("redemption_id");

		utils.longWaitInSeconds(6);
		// third redemption of redeemable unlocked by 30 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem3 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_30Points"), "");
		Assert.assertEquals(posRedeem3.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_30Points"));
		int redemption_id3 = posRedeem3.jsonPath().get("redemption_id");

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue7, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue8, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after Second and third redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		// check account history entry of Second redemption
		boolean redemptionStatus2 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus2, true, "Second Redeemable redemption is not logged from Account History");
		utils.logPass("Second Redeemable redemption is logged from Account History");

		// check account history entry of third redemption
		boolean redemptionStatus3 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_30Points"), "description");
		Assert.assertEquals(redemptionStatus3, true, "Third Redeemable redemption is not logged from Account History");
		utils.logPass("Third Redeemable redemption is logged from Account History");

		// Void redemption using API
		Response voidResponse2 = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id2),
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, voidResponse2.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		utils.logPass("Void redemption API is successful for " + redemption_id2);

		// Void redemption using API
		Response voidResponse3 = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id3),
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, voidResponse3.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		utils.logPass("Void redemption API is successful for " + redemption_id3);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);
		Assert.assertEquals(pointsSpent, "0",
				"points_spent column in checkins table is not equal to expected value i.e. 0");

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue9, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue9, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. 0");

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue10, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue10, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 0");

		utils.logPass(
				"verified that after Second void redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a Second void redemption is successfully completed.");

		// check account history entry of second void redemption
		boolean redemptionStatus4 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus4, false,
				"Redeemable redemption is logged in Account History after void redemption");
		utils.logPass("Redeemable redemption is not logged in Account History after void redemption");

		// check account history entry of second void redemption
		boolean redemptionStatus5 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_30Points"), "description");
		Assert.assertEquals(redemptionStatus5, false,
				"Redeemable redemption is logged in Account History after void redemption");
		utils.logPass("Redeemable redemption is not logged in Account History after void redemption");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// force redemption of 2 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "2");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logPass("Force redemption of 2 points is successful");
		int redemption_id4 = forceRedeem_Response.jsonPath().get("redemption_id");

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue11, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue12, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that after force redemption, The columns non_transaction_spent_points and non_transaction_unexpired_spent_points in the account_summary table must be updated with "
						+ pointsSpent + " points only when a redemption is successfully completed.");

		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("2.0 redeemable"), "Force Redeemed amount did not matched");
		utils.logPass("Verified that Forced Redemption should be successfull logged on user timeline");

		// Void redemption using API
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id4),
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, voidResponse4.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		utils.logPass("Void redemption API is successful for " + redemption_id4);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);
		Assert.assertEquals(pointsSpent, "2",
				"points_spent column in checkins table is not equal to expected value i.e. 2");

		String expColValue13 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue13, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue13, "2",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. 2");

		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue14, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		Assert.assertEquals(expColValue14, "2",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. 2");
		utils.logPass(
				"Checked that the corresponding checkin’s points_spent is not getting reverted and force-redeemed points from the account_summaries should not get reverted");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6247 Verify Account summary should remain unchanged when feature flag is OFF || "
			+ "SQ-T6248 Verify Normal redemption and void flow should continue working", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6247_rollingCheckinSumVoidPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS Checkin - 14 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// POS Checkin - 18 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount1"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

//		int actualPoints = ((Integer.parseInt(dataSet.get("amount")) + Integer.parseInt(dataSet.get("amount1"))) * 3);

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));
		int redemption_id = posRedeem.jsonPath().get("redemption_id");

		// sum of points spent in checkins table
		String query2 = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '" + userID
				+ "';";
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		Assert.assertNotEquals(pointsSpent, "0", "Checkins table is not updating");
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue3, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue4, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after first redemption, No data should get updated in account summaries table if account summary flag is off");

		// check account history entry of first redemption
		boolean redemptionStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_name_10Points"), "description");
		Assert.assertEquals(redemptionStatus, true, "Redeemable redemption is not logged from Account History");
		utils.logPass("Redeemable redemption is logged from Account History");

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id),
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, voidResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		utils.logPass("Void redemption API is successful for " + redemption_id);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);
		Assert.assertEquals(pointsSpent, "0",
				"points_spent column in checkins table is not equal to expected value i.e. 0");

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue5, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue6, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that after void first redemption, No update should occur to account_summaries table and Only checkins.points_spent should be affected");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6329 Verify that a new entry is created in the account summaries table upon user registration with default values when enable_checkin_rolling_sum flag is enabled at the business level.", groups = {
			"nonNightly" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6329_rollingCheckinUserCreateFlagEnable() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// STEP-1
		// User SignUp using eclub API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response eClubGuestUploadResponse = pageObj.endpoints().eClubGuestUpload(userEmail, dataSet.get("store_number"),
				dataSet.get("apiKey"));
		Assert.assertEquals(eClubGuestUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		utils.logPass("PLATFORM FUNCTIONS API EClub Guest Upload is successful");

		// Fetching user id for users table
		String query2 = "Select `id` from `users` WHERE `email` = \"" + userEmail + "\" ;";
		String userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue3, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue4, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass(
				"verified that Entry should created in the account summaries table for api2/dashboard/eclub_guests");

		// STEP-4
		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String iFrameEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();

		// Fetching user id for users table
		String query5 = "Select `id` from `users` WHERE `email` = \"" + iFrameEmail + "\" ;";
		userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue5, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue6, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for iFrame Signup");

		// STEP-5
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		userID = resp.jsonPath().get("id").toString();

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue7, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue8, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for pos signup api");

		// STEP-7
		// User SignUp using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue9, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue10, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for Mobile signup api");

		// STEP-8
		// user sign-up via Secure API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpRes = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		userID = signUpRes.jsonPath().get("user_id").toString();

		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue11, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue12, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for Secure signup api");

		// STEP-10
//		// Wifi user signup Test
//		userEmail = pageObj.iframeSingUpPage().generateEmail();
//		Response wifiSignUpResponse = pageObj.endpoints().wifiUserSignUp(userEmail, dataSet.get("client"),
//				dataSet.get("secret"), dataSet.get("locationId"));
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, wifiSignUpResponse.getStatusCode());
//		logger.info("Wifi user signup is successful");
//		TestListeners.extentTest.get().pass("Wifi user signup is successful");

		// Fetching user id for users table
		String query13 = "Select `id` from `users` WHERE `email` = \"" + userEmail + "\" ;";
		userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue14, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue15, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for Wifi user Signup");

		// STEP-13
		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "0", "0");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api1
		Response signUpResponseMigrationUser = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseMigrationUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		userID = signUpResponseMigrationUser.jsonPath().get("id").toString();
		utils.logPass("api1 Signup is successful");

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue16, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue17, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for Migration user Signup");

		// STEP-14
		// User SignUp using Auth API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseAuth = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseAuth.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		userID = signUpResponseAuth.jsonPath().get("user_id").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue18 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 5);
		Assert.assertEquals(expColValue18, "0",
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");

		String expColValue19 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(expColValue19, "0",
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry should created in the account summaries table for Migration user Signup");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6330 Verify that a new entry has not created in the account summaries table upon user registration with default values when enable_checkin_rolling_sum flag is disabled at the business level.", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6330_rollingCheckinUserCreateFlagDisable() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// STEP-1
		// User SignUp using eclub API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response eClubGuestUploadResponse = pageObj.endpoints().eClubGuestUpload(userEmail, dataSet.get("store_number"),
				dataSet.get("apiKey"));
		Assert.assertEquals(eClubGuestUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API EClub Guest Upload");
		utils.logPass("PLATFORM FUNCTIONS API EClub Guest Upload is successful");

		// Fetching user id for users table
		String query2 = "Select `id` from `users` WHERE `email` = \"" + userEmail + "\" ;";
		String userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String query3 = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '" + userID
				+ "';";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue3, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String query4 = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue4, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that Entry is not created in the account summaries table for api2/dashboard/eclub_guests");

		// STEP-4
		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String iFrameEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();

		// Fetching user id for users table
		String query5 = "Select `id` from `users` WHERE `email` = \"" + iFrameEmail + "\" ;";
		userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue5 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue5, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue6, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ "0");
		utils.logPass("verified that Entry is not created in the account summaries table for iFrame Signup");

		// STEP-5
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		userID = resp.jsonPath().get("id").toString();

		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue7, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue8, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for pos signup api");

		// STEP-7
		// User SignUp using Mobile API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue9, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue10, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for Mobile signup api");

		// STEP-8
		// user sign-up via Secure API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpRes = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		userID = signUpRes.jsonPath().get("user_id").toString();

		String expColValue11 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue11, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue12 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue12, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for Secure signup api");

		// STEP-10
//		// Wifi user signup Test
//		userEmail = pageObj.iframeSingUpPage().generateEmail();
//		Response wifiSignUpResponse = pageObj.endpoints().wifiUserSignUp(userEmail, dataSet.get("client"),
//				dataSet.get("secret"), dataSet.get("locationId"));
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, wifiSignUpResponse.getStatusCode());
//		logger.info("Wifi user signup is successful");
//		TestListeners.extentTest.get().pass("Wifi user signup is successful");

		// Fetching user id for users table
		String query13 = "Select `id` from `users` WHERE `email` = \"" + userEmail + "\" ;";
		userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query13, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue14 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue14, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue15 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue15, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for Wifi user Signup");

		// STEP-13
		// Create Business Migration User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail,
				dataSet.get("apiKey"), "0", "0");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Business Migration User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// Signup using mobile api1
		Response signUpResponseMigrationUser = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseMigrationUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		userID = signUpResponseMigrationUser.jsonPath().get("id").toString();
		utils.logPass("api1 Signup is successful");

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue16 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue16, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue17 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue17, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for Migration user Signup");

		// STEP-14
		// User SignUp using Auth API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseAuth = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseAuth.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		userID = signUpResponseAuth.jsonPath().get("user_id").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String expColValue18 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"non_transaction_spent_points", 2);
		Assert.assertEquals(expColValue18, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String expColValue19 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(expColValue19, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that Entry is not created in the account summaries table for Migration user Signup");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6380 Verify  Account Summary Updates on Transactions (Existing User)(enable_account_summary) for PUR business || "
			+ "SQ-T6369 Verify  No Change on Existing User After Enabling Flag(enable_account_summary) for PUR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6380_rollingCheckinSumPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS CHeckin - 30 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// POS CHeckin - 30 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// check redeemable get Unlocked after checkin
		boolean redeemable_1_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_10Points"));
		boolean redeemable_2_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_30Points"));
		Assert.assertTrue(redeemable_1_Unlocked,
				dataSet.get("redeemable_name_10Points") + " redeemable is not unlocked");
		Assert.assertTrue(redeemable_2_Unlocked,
				dataSet.get("redeemable_name_30Points") + " redeemable is not unlocked");
		utils.logPass("Verified that checkin of 30 Points should be successfull and redeemable should get unlocked");

		int actualPoints = (Integer.parseInt(dataSet.get("amount")) * 3);
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(amountCheckin >= 50 && amountCheckin <= 80, "User balance is not correct on timeline");
		//Assert.assertEquals(amountCheckin, 65, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of first checkin
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, actualPoints + " points earned ", "description");
		Assert.assertEquals(checkinStatus, true, "Checkin is not logged from Account History");
		utils.logPass("Checkin is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6372 Verify Account Summary Behaviour for New User(enable_account_summary is ON) for PUR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6372_rollingCheckinSumNewUserPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS CHeckin - 30 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		// POS CHeckin - 30 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 5);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6377 Verify Feature Flag OFF Behaviour(enable_account_summary is off) for PUR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6377_rollingCheckinSumOFFPUR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		logger.info("enable_new_sidenav_header_and_footer value is updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// POS CHeckin - 30 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// first redemption of redeemable unlocked by 10 points
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		// POS CHeckin - 30 points
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Second redemption of redeemable unlocked by 10 points
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id_10Points"), "");
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id - "
				+ dataSet.get("redeemable_id_10Points"));

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 5);

		Assert.assertEquals(accountSummaryLoyaltyPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table to NULL");
		utils.logit("loyalty_points column in account_summaries table to NULL");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 2);

		Assert.assertEquals(accountSummaryLoyaltyCountValue, BlankSpace,
				"loyalty_count column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("loyalty_points column in account_summaries table " + BlankSpace);

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 2);

		Assert.assertEquals(accountsPendingPointsValue, "0.0",
				"pending_points column in accounts table is not equal to 0");
		utils.logit("pending_points column in accounts table is equal to 0");

		Assert.assertEquals(accountSummaryPendingPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("pending_points column in accounts table is equal to " + BlankSpace);

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 2);
		Assert.assertEquals(nonTransactionSpentPointsValue, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to NULL");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 2);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that non_transaction_total_points in account_summary table is equal to NULL");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying non_transaction_total_points in account_summaries table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to NULL");
		utils.logPass("verified that non_transaction_total_points column in account_summaries table is equal to NULL");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// check redeemable get Unlocked after checkin
		boolean redeemable_1_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_10Points"));
		boolean redeemable_2_Unlocked = pageObj.guestTimelinePage()
				.verifyRedeemableUnlockedPUR(dataSet.get("redeemable_name_30Points"));
		Assert.assertTrue(redeemable_1_Unlocked,
				dataSet.get("redeemable_name_10Points") + " redeemable is not unlocked");
		Assert.assertTrue(redeemable_2_Unlocked,
				dataSet.get("redeemable_name_30Points") + " redeemable is not unlocked");
		utils.logPass("Verified that checkin of 30 Points should be successfull and redeemable should get unlocked");

		int actualPoints = (Integer.parseInt(dataSet.get("amount")) * 3);
		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(amountCheckin >= 50 && amountCheckin <= 80, "User balance is not correct on timeline");
	//	Assert.assertEquals(amountCheckin, 65, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of first checkin
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, actualPoints + " points earned ", "description");
		Assert.assertEquals(checkinStatus, true, "Checkin is not logged from Account History");
		utils.logPass("Checkin is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6378 Verify  Account Summary Updates on Transactions (Existing User)(enable_account_summary) for PTC business || "
			+ "SQ-T6371 Verify  No Change on Existing User After Enabling Flag(enable_account_summary) for PTC business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6378_rollingCheckinSumPTC() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount2") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp1 = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp1.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemableRewardCount();
		Assert.assertEquals(amountCheckin, 16, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redemptionMark") + " points converted ", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6374 Verify Account Summary Behaviour for New User(enable_account_summary is ON) for PTC business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6374_rollingCheckinSumNewUserPTC() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount2") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp1 = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp1.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemableRewardCount();
		Assert.assertEquals(amountCheckin, 16, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redemptionMark") + " points converted ", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6375 Verify Feature Flag OFF Behaviour(enable_account_summary is off) for PTC business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6375_rollingCheckinSumOFFPTC() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		/// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS Checkin of " + dataSet.get("amount2") + " amount is successful");

		utils.longWaitInSeconds(6);
		// Create Online Redemption
		utils.logit("== Auth API Create Online Redemption ==");
		Response resp1 = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp1.jsonPath().get("status").toString().contains("Please HONOR it."));
		utils.logPass("Auth API Create Online Redemption call is successful");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), "205",
				"loyalty_points column in accounts table is not equal to 205");
		utils.logit("loyalty_points column in accounts table is equal to 205");

		Assert.assertEquals(accountSummaryLoyaltyPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table to NULL");
		utils.logit("loyalty_points column in account_summaries table to NULL");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 2);

		Assert.assertEquals(accountsLoyaltyCountValue, "2", "loyalty_count column in accounts table is not equal to 2");
		utils.logit("loyalty_count column in accounts table is equal to 2");

		Assert.assertEquals(accountSummaryLoyaltyCountValue, BlankSpace,
				"loyalty_count column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("loyalty_points column in account_summaries table " + BlankSpace);

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 2);

		Assert.assertEquals(accountsPendingPointsValue, "0.0",
				"pending_points column in accounts table is not equal to 0");
		utils.logit("pending_points column in accounts table is equal to 0");

		Assert.assertEquals(accountSummaryPendingPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("pending_points column in accounts table is equal to " + BlankSpace);

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 2);
		Assert.assertEquals(nonTransactionSpentPointsValue, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to NULL");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 2);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that non_transaction_total_points in account_summary table is equal to NULL");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying non_transaction_total_points in account_summaries table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to NULL");
		utils.logPass("verified that non_transaction_total_points column in account_summaries table is equal to NULL");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemableRewardCount();
		Assert.assertEquals(amountCheckin, 16, "User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redemptionMark") + " points converted ", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6379 Verify  Account Summary Updates on Transactions (Existing User)(enable_account_summary) for PTR business || "
			+ "SQ-T6370 Verify  No Change on Existing User After Enabling Flag(enable_account_summary) for PTR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6379_rollingCheckinSumPTR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("amount")) - Integer.parseInt(dataSet.get("redemptionMark")));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(6);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId2, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp2 = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId2,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(((amountCheckin == actualPoints) || (amountCheckin == 405)),
				"User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "You were gifted: Base Redeemable (Loyalty Reward)", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History and user get Base Redeemable");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6373 Verify Account Summary Behaviour for New User(enable_account_summary is ON) for PTR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6373_rollingCheckinSumNewUserPTR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("amount")) - Integer.parseInt(dataSet.get("redemptionMark")));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(6);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId2, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp2 = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId2,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), accountSummaryLoyaltyPointsValue,
				"loyalty_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 5);

		Assert.assertEquals(accountsLoyaltyCountValue, accountSummaryLoyaltyCountValue,
				"loyalty_count column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"loyalty_count column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 5);

		Assert.assertEquals((accountsPendingPointsValue.replace(".0", "")), accountSummaryPendingPointsValue,
				"pending_points column in accounts table is not equal to loyalty_points column in account_summaries table");
		utils.logit(
				"pending_points column in accounts table is equal to loyalty_points column in account_summaries table");

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 5);
		Assert.assertEquals(nonTransactionSpentPointsValue, pointsSpent,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 5);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, pointsSpent,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsSpent);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to sum of points_spent in checkins table");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 5);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, pointsEarned,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ pointsEarned);
		utils.logPass(
				"verified that non_transaction_total_points in account_summary table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying total_lifetime_points in accounts table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, (totalLifetimePointsValue.replace(".0", "")),
				"total_lifetime_points column in account table is not equal to non_transaction_total_points column in account_summaries table");
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to non_transaction_total_points column in account_summaries table");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(((amountCheckin == actualPoints) || (amountCheckin == 405)),
				"User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "You were gifted: Base Redeemable (Loyalty Reward)", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History and user get Base Redeemable");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6376 Verify Feature Flag OFF Behaviour(enable_account_summary is off) for PTR business", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6376_rollingCheckinSumOFFPTR() throws Exception {

		String b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful");

		// First POS Checkin - 105 points
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		int actualPoints = (Integer.parseInt(dataSet.get("amount")) - Integer.parseInt(dataSet.get("redemptionMark")));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// Second POS Checkin - 100 points
		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount2"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.longWaitInSeconds(6);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("redeemable_id"), "", "100");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId2, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp2 = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId2,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		String accountsLoyaltyPointsQuery = "Select `loyalty_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryLoyaltyPointsQuery = "Select `loyalty_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsLoyaltyCountQuery = "Select `total_visits` from `accounts` WHERE `user_id` = '" + userID + "';";
		String accountSummaryLoyaltyCountQuery = "Select `loyalty_count` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String accountsPendingPointsQuery = "Select `pending_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";
		String accountSummaryPendingPointsQuery = "Select `pending_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String pointsSpentQuery = "SELECT SUM(`points_spent`) as pointsSpentAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String pointsEarnedQuery = "SELECT SUM(`points_earned`) as pointsEarnedAmount FROM `checkins` where `user_id` = '"
				+ userID + "';";

		String nonTransactionSpentPointsQuery = "Select `non_transaction_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";
		String nonTransactionUnexpiredSpentPointsQuery = "Select `non_transaction_unexpired_spent_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String nonTransactionTotalPointsQuery = "Select `non_transaction_total_points` from `account_summaries` WHERE `user_id` = '"
				+ userID + "';";

		String totalLifetimePointsQuery = "Select `total_lifetime_points` from `accounts` WHERE `user_id` = '" + userID
				+ "';";

		// verifying loyalty_points in accounts and account_summaries table
		String accountsLoyaltyPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyPointsQuery, "loyalty_points", 5);
		String accountSummaryLoyaltyPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyPointsQuery, "loyalty_points", 2);

		Assert.assertEquals((accountsLoyaltyPointsValue.replace(".0", "")), "205",
				"loyalty_points column in accounts table is not equal to 205");
		utils.logit("loyalty_points column in accounts table is equal to 205");

		Assert.assertEquals(accountSummaryLoyaltyPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table to NULL");
		utils.logit("loyalty_points column in account_summaries table to NULL");

		// verifying loyalty_count in accounts and account_summaries table
		String accountsLoyaltyCountValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsLoyaltyCountQuery, "total_visits", 5);
		String accountSummaryLoyaltyCountValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryLoyaltyCountQuery, "loyalty_count", 2);

		Assert.assertEquals(accountsLoyaltyCountValue, "2", "loyalty_count column in accounts table is not equal to 2");
		utils.logit("loyalty_count column in accounts table is equal to 2");

		Assert.assertEquals(accountSummaryLoyaltyCountValue, BlankSpace,
				"loyalty_count column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("loyalty_points column in account_summaries table " + BlankSpace);

		// verifying pending_points in accounts and account_summaries table
		String accountsPendingPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				accountsPendingPointsQuery, "pending_points", 5);
		String accountSummaryPendingPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, accountSummaryPendingPointsQuery, "pending_points", 2);

		Assert.assertEquals(accountsPendingPointsValue, "0.0",
				"pending_points column in accounts table is not equal to 0");
		utils.logit("pending_points column in accounts table is equal to 0");

		Assert.assertEquals(accountSummaryPendingPointsValue, BlankSpace,
				"loyalty_points column in account_summaries table is not equal to " + BlankSpace);
		utils.logit("pending_points column in accounts table is equal to " + BlankSpace);

		// verifying point spent sum in checkin table
		String pointsSpent = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsSpentQuery, "pointsSpentAmount", 5);
		utils.logit("points_spent column in checkins table is equal to expected value " + pointsSpent);

		// Check non_transaction_spent_points and non_transaction_unexpired_spent_points
		// in account_summaries table

		String nonTransactionSpentPointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(
				env, nonTransactionSpentPointsQuery, "non_transaction_spent_points", 2);
		Assert.assertEquals(nonTransactionSpentPointsValue, BlankSpace,
				"non_transaction_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);

		String nonTransactionUnexpiredSpentPointsValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionUnexpiredSpentPointsQuery,
						"non_transaction_unexpired_spent_points", 2);
		Assert.assertEquals(nonTransactionUnexpiredSpentPointsValue, BlankSpace,
				"non_transaction_unexpired_spent_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass(
				"verified that non_transaction_unexpired_spent_points and non_transaction_spent_points in account_summary table is equal to NULL");

		// verifying point earned sum in checkin table
		String pointsEarned = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				pointsEarnedQuery, "pointsEarnedAmount", 5);
		utils.logit("points_earned column in checkins table is equal to expected value " + pointsEarned);

		// Check non_transaction_total_points in account_summaries table
		String nonTransactionTotalPointsQueryValue = DBUtils
				.executeQueryAndGetColumnValuePollingUsed(env, nonTransactionTotalPointsQuery,
						"non_transaction_total_points", 2);
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to expected value i.e. "
						+ BlankSpace);
		utils.logPass("verified that non_transaction_total_points in account_summary table is equal to NULL");

		// verifying total_lifetime_points in accounts table
		String totalLifetimePointsValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env,
				totalLifetimePointsQuery, "total_lifetime_points", 5);

		Assert.assertEquals((totalLifetimePointsValue.replace(".0", "")), pointsEarned,
				"total_lifetime_points column in account table is not equal to expected value i.e. " + pointsEarned);
		utils.logPass(
				"verified that total_lifetime_points in account table is equal to sum of points_earned in checkins table");

		// verifying non_transaction_total_points in account_summaries table
		Assert.assertEquals(nonTransactionTotalPointsQueryValue, BlankSpace,
				"non_transaction_total_points column in account_summaries table is not equal to NULL");
		utils.logPass("verified that non_transaction_total_points column in account_summaries table is equal to NULL");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable point in guest timeline
		int amountCheckin = pageObj.guestTimelinePage().getRedeemablePointCount();
		Assert.assertTrue(((amountCheckin == actualPoints) || (amountCheckin == 405)),
				"User balance is not correct on timeline");
		utils.logPass("Verified that User balance is correct on timeline");

		// check account history entry of Redemption by Redemption Mark
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "You were gifted: Base Redeemable (Loyalty Reward)", "description");
		Assert.assertEquals(checkinStatus, true, "Redemption by Redemption Mark is not logged from Account History");
		utils.logPass("Redemption by Redemption Mark is logged from Account History and user get Base Redeemable");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
