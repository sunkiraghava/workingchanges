package com.punchh.server.apiTest;

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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LoyaltyDashboardApiTest {

	private static Logger logger = LogManager.getLogger(MobileApiTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;
	String userEmail, b_id;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

//Author : Hardik
	@SuppressWarnings("static-access")
//	This is incomplete because we are not able to give cookies
//	@Test(description = "SQ-T5406 Validate that the redirection issues due to cross-session problems occur with flag OFF and logs appear in sidekiq", groups = "Regression", priority = 0)
	public void T5406_crossSessionDashboardApiFlagOff() throws Exception {
		String b_id1 = dataSet.get("business_id");
		String b_id2 = dataSet.get("business_id2");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id1 + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag1"), b_id1);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag2"), b_id1);
		Assert.assertTrue(flag1, dataSet.get("dbFlag2") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag2") + " value is updated to true");

		String query2 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id2 + "'";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "preferences");

		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue2, "false",
				dataSet.get("dbFlag1"), b_id2);
		Assert.assertTrue(flag2, dataSet.get("dbFlag1") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to false");

		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue2, "true",
				dataSet.get("dbFlag2"), b_id2);
		Assert.assertTrue(flag3, dataSet.get("dbFlag2") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag2") + " value is updated to true");

		// creating BMU
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail1,
				dataSet.get("apiKey"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions Create Business Migration User API");
		utils.logPass("Platform Functions Create Business Migration User API call is successful");
		String cookies = (createMigrationUserResponse.getDetailedCookies()).toString();

		String userEmail4 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse4 = pageObj.endpoints().createBusinessMigrationUserCookies(userEmail4,
				dataSet.get("apiKey2"), cookies);
		Assert.assertEquals(createMigrationUserResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions Create Business Migration User API");
		utils.logPass("Platform Functions Create Business Migration User API call is successful");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5405 Validate that the redirection issues due to cross-session problems no longer occur with flag ON and no logs appear in sidekiq", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5405_crossSessionDashboardApiFlagOn() throws Exception {
		String b_id1 = dataSet.get("business_id");
		String b_id2 = dataSet.get("business_id2");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id1 + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				dataSet.get("dbFlag1"), b_id1);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				dataSet.get("dbFlag2"), b_id1);
		Assert.assertTrue(flag1, dataSet.get("dbFlag2") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag2") + " value is updated to false");

		String query2 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id2 + "'";
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "preferences");

		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue2, "true",
				dataSet.get("dbFlag1"), b_id2);
		Assert.assertTrue(flag2, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag1") + " value is updated to true");

		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue2, "false",
				dataSet.get("dbFlag2"), b_id2);
		Assert.assertTrue(flag3, dataSet.get("dbFlag2") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag2") + " value is updated to false");

		// creating BMU
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail1,
				dataSet.get("apiKey"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions Create Business Migration User API");
		utils.logPass("Platform Functions Create Business Migration User API call is successful");

		String userEmail4 = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse4 = pageObj.endpoints().createBusinessMigrationUse(userEmail4,
				dataSet.get("apiKey2"));
		Assert.assertEquals(createMigrationUserResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions Create Business Migration User API");
		utils.logit("Platform Functions Create Business Migration User API call is successful");
		utils.logPass("No HTML error appear because of cross sessions and these apis run successfully");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5928 Verify if the lock is released immediately after processing a transaction.", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5928_VerifyLockReleased() throws Exception {
		b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");
		utils.logit("Send Points to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"100");
		utils.logit("Send Points to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// perform redemption first time
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "5927 ");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass(
				"Verified able to redeem the redeemable having the redeemable id - " + dataSet.get("redeemable_id"));
		utils.logPass("Verified that Redemption should be done successfully for first redemption");

		// perform redemption second time
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_ID"), "5927 ");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass(
				"Verified able to redeem the redeemable having the redeemable id - " + dataSet.get("redeemable_ID"));
		utils.logPass(
				"Redemption should be done successfully as previous lock will get release immediately once request is processed");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value false
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5927 Verify the current locking mechanism duration.", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5927_VerifyLockReleased() throws Exception {
		b_id = dataSet.get("business_id");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value false
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		String auth_token = signUpResponse.jsonPath().get("authentication_token").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");
		utils.logit("Send Points to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"100");
		utils.logit("Send Points to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// perform redemption first time
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedeem = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "5927 ");
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass(
				"Verified able to redeem the redeemable having the redeemable id - " + dataSet.get("redeemable_id"));
		utils.logPass("Verified that Redemption should be done successfully for first redemption");

		// perform redemption second time
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_ID"), "5927 ");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "api status code did not match");
		Assert.assertEquals(posRedeem1.jsonPath().getString("[0]"),
				"Another transaction is currently accessing the same code. Please try after some time.");
		utils.logPass(
				"Verified error of POS Redemption Of Redeemable Another transaction is currently accessing the same code. Please try after some time");

		// Perform redemption of reward through api (/api/mobile/redemptions)

		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token,
				dataSet.get("redeemable_ID"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api1 create redemption using redeemable_id");
		Assert.assertEquals(posRedeem1.jsonPath().getString("[0]"),
				"Another transaction is currently accessing the same code. Please try after some time.");
		utils.logPass(
				"Verified error of Secure Api Redemption Of Redeemable Another transaction is currently accessing the same code. Please try after some time");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"80");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		utils.longWaitInSeconds(6);
		// Create Redemption using redeemable id
		Response redeemableRedemptionResp1 = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("redeemable_ID"),
				dataSet.get("locationId"));
		Assert.assertEquals(redeemableRedemptionResp1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using redeemable_id");

		// Create Redemption using redeemable id
		Response redeemableRedemptionResp = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("redeemable_ID"),
				dataSet.get("locationId"));
		Assert.assertEquals(redeemableRedemptionResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create redemption using redeemable_id");
		Assert.assertEquals(redeemableRedemptionResp.jsonPath().getString("[0]"),
				"Another transaction is currently accessing the same code. Please try after some time.");
		utils.logPass(
				"Verified error of Mobile Api Redemption Of Redeemable Another transaction is currently accessing the same code. Please try after some time");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().authOnlineRedeemableRedemption(auth_token, dataSet.get("client"),
				dataSet.get("secret"), "5927", dataSet.get("redeemable_ID"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create redemption using redeemable_id");
		Assert.assertEquals(resp.jsonPath().getString("[0]"),
				"Another transaction is currently accessing the same code. Please try after some time.");
		utils.logPass(
				"Verified error of Auth Api Redemption Of Redeemable Another transaction is currently accessing the same code. Please try after some time");

		// Perform redemption of reward through api api/pos/redemptions/possible
		Response possibleRedemptionRespo = pageObj.endpoints().posPossibleRedemptionOfRedeemable(userEmail,
				dataSet.get("redeemable_ID"), dataSet.get("locationkey"), "5927");
		Assert.assertEquals(possibleRedemptionRespo.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified that Possible redemption of points is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"80");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// Perform redemption of reward through api api/pos/redemptions/possible
		Response possibleRedemptionRespo1 = pageObj.endpoints().posPossibleRedemptionOfRedeemable(userEmail,
				dataSet.get("redeemable_ID"), dataSet.get("locationkey"), "5927");
		Assert.assertEquals(possibleRedemptionRespo1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass(
				"Verified that Possible Redemption should be done successfully and no error should appear. Lock mechanism not work in possible api");

	}

	@Test(description = "SQ-T5739: Verify Auth API Update Loyalty Check-in", priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T5739_verifyAuthApiUpdateLoyaltyCheckin() {

		// User Sign-up
		utils.logit("== User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		utils.logPass("Auth API Sign-up is successful");

		// Create Loyalty Check-in
		utils.logit("== Create Loyalty Check-in ==");
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, dataSet.get("amount"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String createCheckinPendingPoints = checkinResponse.jsonPath().get("checkin.pending_points").toString();
		String createCheckinEarnedPoints = checkinResponse.jsonPath().get("checkin.points_earned").toString();
		Assert.assertEquals(createCheckinPendingPoints, "300", "Pending points did not match");
		Assert.assertEquals(createCheckinEarnedPoints, "0", "Earned points did not match");
		String externalUid = checkinResponse.jsonPath().get("checkin.external_uid").toString();
		utils.logPass("Auth API Create Loyalty Check-in is successful");

		// Update Loyalty Check-in using valid data
		utils.logit("== Update Loyalty Check-in using valid data ==");
		Response updateCheckinResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken, dataSet.get("client"),
				dataSet.get("secret"), externalUid, "committed");
		Assert.assertEquals(updateCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("Auth API Update Loyalty Check-in using valid data is successful");

		// Fetch the updated Check-in by External_uid
		utils.logit("== Fetch a Check-in by External_uid ==");
		Response fetchCheckinResponse = pageObj.endpoints().authApiFetchACheckinByExternal_uid(authToken,
				dataSet.get("client"), dataSet.get("secret"), externalUid);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String updatedCheckinPendingPoints = fetchCheckinResponse.jsonPath().get("pending_points").toString();
		String updatedCheckinEarnedPoints = fetchCheckinResponse.jsonPath().get("points_earned").toString();
		Assert.assertEquals(updatedCheckinPendingPoints, "0", "Pending points did not match");
		Assert.assertEquals(updatedCheckinEarnedPoints, "300", "Earned points did not match");
		utils.logPass("Auth API Fetch a Check-in by external uid is successful");

		// Negative case: Update Loyalty Check-in with already committed external_uid
		utils.logit("== Negative case: Update Loyalty Check-in with already committed external_uid ==");
		Response updateCheckinAlreadyCommittedResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), externalUid, "committed");
		Assert.assertEquals(updateCheckinAlreadyCommittedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_GONE);
		boolean isUpdateCheckinAlreadyCommittedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				updateCheckinAlreadyCommittedResponse.asString());
		Assert.assertTrue(isUpdateCheckinAlreadyCommittedSchemaValidated,
				"Auth API Update Loyalty Check-in with already committed external_uid Schema Validation failed");
		String updateCheckinAlreadyCommittedErrorCode = updateCheckinAlreadyCommittedResponse.jsonPath()
				.get("error.code");
		Assert.assertEquals(updateCheckinAlreadyCommittedErrorCode, "commited");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to already committed external_uid");

		// Negative case: Update Loyalty Check-in with invalid auth token
		utils.logit("== Negative case: Update Loyalty Check-in with invalid auth token ==");
		Response updateCheckinInvalidAuthTokenResponse = pageObj.endpoints().authUpdateLoyaltyCheckin("1",
				dataSet.get("client"), dataSet.get("secret"), externalUid, "committed");
		Assert.assertEquals(updateCheckinInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isUpdateCheckinInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateCheckinInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isUpdateCheckinInvalidAuthTokenSchemaValidated,
				"Auth API Update Loyalty Check-in with invalid auth token Schema Validation failed");
		String updateCheckinInvalidAuthTokenErrorMsg = updateCheckinInvalidAuthTokenResponse.jsonPath().get("error");
		Assert.assertEquals(updateCheckinInvalidAuthTokenErrorMsg, "You need to sign in or sign up before continuing.");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to invalid auth token");

		// Negative case: Update Loyalty Check-in with invalid signature
		utils.logit("== Negative case: Update Loyalty Check-in with invalid signature ==");
		Response updateCheckinInvalidSignatureResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken,
				dataSet.get("client"), "1", externalUid, "committed");
		Assert.assertEquals(updateCheckinInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isUpdateCheckinInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateCheckinInvalidSignatureResponse.asString());
		Assert.assertTrue(isUpdateCheckinInvalidSignatureSchemaValidated,
				"Auth API Update Loyalty Check-in with invalid signature Schema Validation failed");
		String updateCheckinInvalidSignatureErrorMsg = updateCheckinInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(updateCheckinInvalidSignatureErrorMsg, "Invalid Signature");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to invalid signature");

		// Negative case: Update Loyalty Check-in with invalid external_uid
		utils.logit("== Negative case: Update Loyalty Check-in with invalid external_uid ==");
		Response updateCheckinInvalidExternalUidResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), "1", "committed");
		Assert.assertEquals(updateCheckinInvalidExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_EXPECTATION_FAILED);
		boolean isUpdateCheckinInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				updateCheckinInvalidExternalUidResponse.asString());
		Assert.assertTrue(isUpdateCheckinInvalidExternalUidSchemaValidated,
				"Auth API Update Loyalty Check-in with invalid external_uid Schema Validation failed");
		String updateCheckinInvalidExternalUidErrorCode = updateCheckinInvalidExternalUidResponse.jsonPath()
				.get("error.code");
		Assert.assertEquals(updateCheckinInvalidExternalUidErrorCode, "not_found");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to invalid external_uid");

		// Negative case: Update Loyalty Check-in with missing external_uid
		utils.logit("== Negative case: Update Loyalty Check-in with missing external_uid ==");
		Response updateCheckinMissingExternalUidResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), "", "committed");
		Assert.assertEquals(updateCheckinMissingExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isUpdateCheckinMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateCheckinMissingExternalUidResponse.asString());
		Assert.assertTrue(isUpdateCheckinMissingExternalUidSchemaValidated,
				"Auth API Update Loyalty Check-in with missing external_uid Schema Validation failed");
		String updateCheckinMissingExternalUidErrorMsg = updateCheckinMissingExternalUidResponse.jsonPath()
				.get("error");
		Assert.assertEquals(updateCheckinMissingExternalUidErrorMsg,
				"Required parameter missing or the value is empty: external_uid");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to missing external_uid");

		// Negative case: Update Loyalty Check-in with missing state
		utils.logit("== Negative case: Update Loyalty Check-in with missing state ==");
		Response updateCheckinMissingStateResponse = pageObj.endpoints().authUpdateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), externalUid, "");
		Assert.assertEquals(updateCheckinMissingStateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isUpdateCheckinMissingStateSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, updateCheckinMissingStateResponse.asString());
		Assert.assertTrue(isUpdateCheckinMissingStateSchemaValidated,
				"Auth API Update Loyalty Check-in with missing state Schema Validation failed");
		String updateCheckinMissingStateErrorCode = updateCheckinMissingStateResponse.jsonPath().get("error.code");
		Assert.assertEquals(updateCheckinMissingStateErrorCode, "missing_data");
		utils.logPass("Auth API Update Loyalty Check-in is unsuccessful due to missing state");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

}
