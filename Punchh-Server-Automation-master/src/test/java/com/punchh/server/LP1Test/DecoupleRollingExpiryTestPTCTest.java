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
public class DecoupleRollingExpiryTestPTCTest {
	static Logger logger = LogManager.getLogger(DecoupleRollingExpiryTestPTCTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

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

	// Rakhi
	@SuppressWarnings("unused")
	@Test(description = "SQ-T5402 Verify annually on membership level bump anniversary decoupling strategy where level bump when run 2 years expiry", priority = 0, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5402_MembershipLevelBumpAnniversaryDecouplingStrategy() throws Exception {

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

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		String membershipId1 = dataSet.get("tier1");
		String membershipId2 = dataSet.get("tier2");
		String membershipId3 = dataSet.get("tier3");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		// pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		String newcreated_at = CreateDateTime.previousYearDateAndTime(2);
		// DB query for user updation
		String query1 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logPass("Users table updated successfully.");

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for 10 dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// DB query for accounts updation
		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		utils.logPass("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logPass("checkins table updated successfully.");

		// effective
		String query4 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " ");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		String membershipLvlPt1 = userMembershipLevelMobileApiResponse.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt1, "10",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt1);

		// Do checkin of 140 after 2 days of user creation
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "140");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for 140 dollar");

		// Fetch Checkins
		Response fetchCheckinResponse1 = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id2 = fetchCheckinResponse1.jsonPath().get("[1].checkin_id").toString();

		String newcreated_at1 = CreateDateTime.getPastYearsWithFutureDate(2, 2);

		// DB query for accounts updation
		String query5 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at1 + "' , `updated_at` = '"
				+ newcreated_at1 + "' WHERE `user_id` = '" + userID + "';";
		int rs3 = DBUtils.executeUpdateQuery(env, query5);
		Assert.assertEquals(rs3, 1, "accounts table query is not working");
		utils.logPass("Accounts table updated successfully.");

		// DB query for checkins updation
		String query6 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at1 + "' , `updated_at` = '"
				+ newcreated_at1 + "' WHERE id = '" + checkin_id2 + "';";
		int rs4 = DBUtils.executeUpdateQuery(env, query6);
		Assert.assertEquals(rs4, 1, "checkins table query is not working");
		utils.logPass("checkins table updated successfully.");

		// effective
		String query7 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, membershipId2, "Effective membership strategy is not " + membershipId2 + "");
		utils.logPass("Verified that Effective membership strategy is " + membershipId2 + " ");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse1 = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		String membershipLvlPt2 = userMembershipLevelMobileApiResponse1.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt2, "150",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt2);

		String newcreated_at3 = CreateDateTime.getCurrentDate();
		// DB query for membership_level_histories update
		String query15 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at3
				+ "' WHERE `user_id` = '" + userID + "' AND `scheduled_expiry_on` IS NOT NULL;";
		utils.longWaitInSeconds(3);
		int rs7 = DBUtils.executeUpdateQuery(env, query15); // stmt.executeUpdate(query2);
		// Assert.assertEquals(rs7, 1, "membership_level_histories table query is not
		// working");
		utils.logPass("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(8);
		String query9 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		// pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9,
				"business_id", 100);
		Assert.assertEquals(expColValue9, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		utils.logPass("Entry is created in expiry_events table in DB : " + expColValue9);

		// effective
		String query8 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"membership_level_id", 10);
		Assert.assertEquals(expColValue7, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " ");

		// operative
		String query10 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by previous_membership_level_id desc limit 1;";
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, membershipId2, "Operative membership strategy is not " + membershipId2 + "");
		utils.logPass("Verified that Operative membership strategy is " + membershipId2 + "");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse2 = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		String membershipLvlPt3 = userMembershipLevelMobileApiResponse2.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt3, "0",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt3);

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		utils.logPass("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "5.0", "user balance is not equal to expected value i.e. 5");
		utils.logPass("user balance is equal to expected value i.e. 5");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		utils.logPass("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "50",
				"user balance in account history is not equal to expected value i.e. 50");
		utils.logPass("user balance in account history is equal to expected value i.e. 50");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T5401 Verify annually on membership level bump anniversary decoupling strategy for 1st year of expiry(New user)", groups = {
			"unstable", "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5401_MembershipLevelBumpAnniversaryDecouplingStrategy() throws Exception {

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

		String membershipId1 = dataSet.get("tier1");
		String membershipId2 = dataSet.get("tier2");
		// String membershipId3 = dataSet.get("tier3");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "ON");
		pageObj.earningPage().decoupledMembershipLevelEvaluationStrategy(
				"Annually On Membership Bump Anniversary (Points do not expire)");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);
		// DB query for user updation
		String query1 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		utils.logPass("Users table updated successfully.");

		// Do checkin of 10
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for 10 dollar");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// DB query for accounts updation
		String query2 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		utils.logPass("Accounts table updated successfully.");

		// DB query for checkins updation
		String query3 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		utils.logPass("checkins table updated successfully.");

		// effective
		String query4 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue1, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " ");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		String membershipLvlPt1 = userMembershipLevelMobileApiResponse.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt1, "10",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt1);

		// Do checkin of 140 after 2 days of user creation
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "140");
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for 140 dollar");
		utils.longWaitInSeconds(12);

		// Fetch Checkins
		Response fetchCheckinResponse1 = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id2 = fetchCheckinResponse1.jsonPath().get("[1].checkin_id").toString();

		String newcreated_at1 = CreateDateTime.getPastYearsWithFutureDate(1, 2);
		// DB query for accounts updation
		String query5 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at1 + "' , `updated_at` = '"
				+ newcreated_at1 + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs3 = DBUtils.executeUpdateQuery(env, query5);
		Assert.assertEquals(rs3, 1, "accounts table query is not working");
		utils.logPass("Accounts table updated successfully.");

		// DB query for checkins updation
		String query6 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at1 + "' , `updated_at` = '"
				+ newcreated_at1 + "' WHERE id = '" + checkin_id2 + "';";
		pageObj.singletonDBUtilsObj();
		int rs4 = DBUtils.executeUpdateQuery(env, query6);
		Assert.assertEquals(rs4, 1, "checkins table query is not working");
		utils.logPass("checkins table updated successfully.");

		// effective
		String query7 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		utils.logPass("Verified that Effective membership strategy is " + membershipId2 + " ");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse1 = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		utils.longWaitInSeconds(4);
		String membershipLvlPt2 = userMembershipLevelMobileApiResponse1.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt2, "150",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt2);

		String newcreated_at3 = CreateDateTime.getCurrentDate();
		// DB query for accounts updation
		String query15 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at3
				+ "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs7 = DBUtils.executeUpdateQuery(env, query15); // stmt.executeUpdate(query2);
		utils.logPass("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(8);
		String query9 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "business_id",
				130);
		Assert.assertEquals(expColValue9, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		utils.logPass("Entry is created in expiry_events table in DB");

		// effective
		String query8 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"membership_level_id", 10);
		Assert.assertEquals(expColValue7, membershipId1, "Effective membership strategy is not " + membershipId1 + "");
		utils.logPass("Verified that Effective membership strategy is " + membershipId1 + " ");

		// operative
		String query10 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		utils.logPass("Verified that Operative membership strategy is " + membershipId2 + "");

		// Mobile Fetch user membership level
		Response userMembershipLevelMobileApiResponse2 = pageObj.endpoints()
				.userMembershipLevelMobileApi(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(userMembershipLevelMobileApiResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user membership level is successful");
		String membershipLvlPt3 = userMembershipLevelMobileApiResponse2.jsonPath()
				.getString("membership_level_qualification_points");
		Assert.assertEquals(membershipLvlPt3, "0",
				"membership level qualification points in not equal to expected value in Mobile Fetch user membership level");
		utils.logPass("Verified that in Mobile Fetch user membership level membership_level_qualification_points "
				+ membershipLvlPt3);

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		utils.logPass("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "5.0", "user balance is not equal to expected value i.e. 5");
		utils.logPass("user balance is equal to expected value i.e. 5");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		utils.logPass("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "50",
				"user balance in account histroy is not equal to expected value i.e. 50");
		utils.logPass("user balance in account histroy is equal to expected value i.e. 50");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
