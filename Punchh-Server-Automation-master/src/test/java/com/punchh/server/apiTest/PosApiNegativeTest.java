package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PosApiNegativeTest {
	static Logger logger = LogManager.getLogger(PosApiNegativeTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	Properties uiProp;
	String email = "AutoApiTemp@punchh.com";
	Properties prop;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	String adminAuthorization = "bJzb3avjxPs9PP2xW8kf";// pp
	private String env;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		apiUtils = new ApiUtils();
		utils = new Utilities();
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3079 POS API verify location configuration negative flow", priority = 0)
	public void T2903_verifyLocationConfigurationNegativeFlow() {
		logger.info("== POS API: verify location configuration with invalid Location Key ==");
		Response posLocConfigResponse = pageObj.endpoints().posLocationConfig(dataSet.get("invalidlocationKey"));
		Assert.assertEquals(posLocConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code not matched as 401 for invalid location key");
		boolean isPosLocationConfigurationSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, posLocConfigResponse.asString());
		Assert.assertTrue(isPosLocationConfigurationSchemaValidated,
				"POS Location configuration Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified location configuration with invalid location key");
	}

	@Test(description = "SQ-T3076 POS API validation for Singup, Checkin, Redemption, void redemption negative flow", priority = 1)
	public void T2369_verifyCreateUserCreateCheckinNegativeFlow() {
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		String invaliduserEmail = "testabc";

		System.out.print("Reading data from file ==" + dataSet.get("file"));
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// create a user with invalid location key
		logger.info("== POS API: verify create user with invalid Location Key ==");
		Response response1 = pageObj.endpoints().posSignUp(userEmail, dataSet.get("invalidlocationKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "status code not matched as 401 for invalid location key");
		boolean isPosUserSignUpInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response1.asString());
		Assert.assertTrue(isPosUserSignUpInvalidLocationKeySchemaValidated,
				"POS User Sign-up Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified Create User with invalid location key");

		// create a user with valid location key and invalid email.
		logger.info("== POS API: verify create user with invalid email ==");
		Response response3 = pageObj.endpoints().posSignUp(invaliduserEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "status code not matched as 422 for invalid email id");
		boolean isPosUserSignUpInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response3.asString());
		Assert.assertTrue(isPosUserSignUpInvalidEmailSchemaValidated, "POS User Sign-up Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified Create User with valid location key and invalid email");

		// *****create checkin*****
		// create checkin with invalid location key
		logger.info("== POS API: verify create checkin with invalid Location Key ==");
		Response response4 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("invalidlocationKey"));
		Assert.assertEquals(response4.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "status code not matched as 401 for invalid location key");
		boolean isPosCheckinInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response4.asString());
		Assert.assertTrue(isPosCheckinInvalidLocationKeySchemaValidated, "POS Checkin Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified Create checkin with invalid location key");

		// create checkin with invalid email
		logger.info("== POS API: verify create checkin with invalid Email ==");
		Response response5 = pageObj.endpoints().posCheckin(invaliduserEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response5.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "status code not matched as 404 for invalid Email");
		boolean isPosCheckinInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response5.asString());
		Assert.assertTrue(isPosCheckinInvalidEmailSchemaValidated, "POS Checkin Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified Create checkin with invalid Email");

		// *******Create Redemption*******
		// create redemption with invalid location key
		logger.info("== POS API: verify create redemption with invalid location key ==");
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("invalidlocationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for pos possible redemption api");
		boolean isPosCreateRedemptionInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, respo.asString());
		Assert.assertTrue(isPosCreateRedemptionInvalidLocationKeySchemaValidated,
				"POS Create Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified create redemption with invalid location key");

		// create redemption with invalid email
		logger.info("== POS API: verify create redemption with invalid email ==");
		Response respo1 = pageObj.endpoints().posRedemptionOfAmount(invaliduserEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for pos possible redemption api");
		boolean isPosCreateRedemptionInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, respo1.asString());
		Assert.assertTrue(isPosCreateRedemptionInvalidEmailSchemaValidated,
				"POS Create Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified create redemption with invalid email");

		// ********Possible redemptions********
		// pos possible redemption with invalid location key
		logger.info("== POS API: verify possible redemptions with invalid location key ==");
		Response respo2 = pageObj.endpoints().posPossibleRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), "Token token=" + dataSet.get("invalidlocationKey"));
		Assert.assertEquals(respo2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for pos possible redemption api");
		boolean isPosPossibleRedemptionInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, respo2.asString());
		Assert.assertTrue(isPosPossibleRedemptionInvalidLocationKeySchemaValidated,
				"POS Possible Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified possible redemption with invalid location key");

		// pos possible redemption with invalid email
		logger.info("== POS API: verify Possible redemptions with invalid Email ==");
		Response respo3 = pageObj.endpoints().posPossibleRedemptionOfAmount(invaliduserEmail, date, key, txn,
				dataSet.get("redeemAmount"), "Token token=" + dataSet.get("locationKey"));
		Assert.assertEquals(respo3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for pos possible redemption api");
		boolean isPosPossibleRedemptionInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, respo3.asString());
		Assert.assertTrue(isPosPossibleRedemptionInvalidEmailSchemaValidated,
				"POS Possible Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified possible redemption with invalid email");

		// ******* void redemption ***********

		// pos checkin with valid data
		Response response6 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response6, "POS checkin");
		Assert.assertEquals(response6.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response6.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// get user balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		// String bankedRewardBefore =
		// balanceResponse.jsonPath().get("balance.banked_rewards");

		// pos create redemption with valid data
		Response respo4 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(respo4.jsonPath().get("status").toString().contains("Please HONOR it."),
				"Error in verfiying create redemption api response status");
		int redemption_id = respo4.jsonPath().get("redemption_id");
		int invalidredemption_id = 12;

		// Void redemption using API with invalid redemption_id
		logger.info("== POS API: verify void redemptions with invalid rdemption id ==");
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(invalidredemption_id),
				dataSet.get("locationKey"));
		voidResponse.then().log().all();
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		boolean isPosVoidRedemptionInvalidRedemptionIdSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema2, voidResponse.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidRedemptionIdSchemaValidated,
				"POS Void Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified void redemption with invalid redemption id");

		// Void redemption using API with invalid location key
		logger.info("== POS API: verify void redemptions with invalid location key ==");
		Response voidResponse1 = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id),
				dataSet.get("invalidlocationKey"));
		voidResponse1.then().log().all();
		Assert.assertEquals(voidResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for pos redemption api");
		boolean isPosVoidRedemptionInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, voidResponse1.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidLocationKeySchemaValidated,
				"POS Void Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified void redemption with invalid location key");

		// void redemption using API with invalid email
		logger.info("== POS API: verify void redemptions with invalid email ==");
		Response voidResponse2 = pageObj.endpoints().posVoidRedemption(invaliduserEmail,
				Integer.toString(redemption_id), dataSet.get("locationKey"));
		voidResponse2.then().log().all();
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for pos redemption api");
		boolean isPosVoidRedemptionInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, voidResponse2.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidEmailSchemaValidated,
				"POS Void Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified void redemption with invalid email");
	}

	@Test(description = "SQ-T3073 [POS API] verify active redemption negative flow", priority = 2)
	public void verifyPosApiActiveRedemptionNegativeFlow() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userID,
				dataSet.get("adminAuthorization"), dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1.5",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code => " + redemption_Code);

		// call pos active redemptions api with invalid location key
		logger.info("== POS API: verify active redemptions with invalid location key ==");
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("invalidlocationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isPosActiveRedemptionInvalidLocationKeySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, activeRedemptionResponse.asString());
		Assert.assertTrue(isPosActiveRedemptionInvalidLocationKeySchemaValidated,
				"POS Active Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified pos active redemptions with invalid location key");

		// call pos active redemptions api with invalid email
		logger.info("== POS API: verify active redemptions with invalid email ==");
		String invaliduserEmail = "testabc";
		Response activeRedemptionResponse1 = pageObj.endpoints().posActiveRedemptions(invaliduserEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND);
		boolean isPosActiveRedemptionInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, activeRedemptionResponse1.asString());
		Assert.assertTrue(isPosActiveRedemptionInvalidEmailSchemaValidated,
				"POS Active Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified pos active redemptions with invalid email");
	}

	@Test(description = "SQ-T3075 POS API multiple void redemption negative flow", priority = 3)
	public void verifyPosApiVoidMultipleRedemptionNegative() {
		ArrayList<String> redemptionIdList = new ArrayList<String>();
		logger.info("== POS user signup and test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// get user balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		// String bankedRewardBefore =
		// balanceResponse.jsonPath().get("balance.banked_rewards");
		// POS redemption API
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		apiUtils.verifyResponse(response, "Redemption successful");
		// int temp1=respo.jsonPath().get("redemption_id");
		redemptionIdList.add(Integer.toString(respo.jsonPath().get("redemption_id")));
		Utilities.longWait(5000);
		// POS redemption API
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:55:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo1 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo1.jsonPath().get("redemption_id")));
		Utilities.longWait(5000);
		// POS redemption API
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:54:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo2 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo2.jsonPath().get("redemption_id")));
		// POS redemption API
		Utilities.longWait(5000);
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:59:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo3 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo3.jsonPath().get("redemption_id")));

		// Void multiple redemption using API invalid location key
		logger.info("== POS API: verify multiple redemptions with invalid location key ==");
		Response voidResponse = pageObj.endpoints().posVoidMultipleRedemption(userEmail, redemptionIdList,
				dataSet.get("invalidlocationKey"));
		voidResponse.then().log().all();
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for pos multiple redemption api");
		boolean isPosVoidMultipleRedemptionInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, voidResponse.asString());
		Assert.assertTrue(isPosVoidMultipleRedemptionInvalidLocationKeySchemaValidated,
				"POS Void Multiple Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified multiple redemption with invalid location key");

		// Void multiple redemption using API invalid email
		String invaliduserEmail = "testabc";
		logger.info("== POS API: verify multiple redemptions with invalid email ==");
		Response voidResponse1 = pageObj.endpoints().posVoidMultipleRedemption(invaliduserEmail, redemptionIdList,
				dataSet.get("locationKey"));
		voidResponse1.then().log().all();
		Assert.assertEquals(voidResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for pos multiple redemption api");
		boolean isPosVoidMultipleRedemptionInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, voidResponse1.asString());
		Assert.assertTrue(isPosVoidMultipleRedemptionInvalidEmailSchemaValidated,
				"POS Void Multiple Redemption Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified multiple redemption with invalid email");

	}

	@Test(description = "SQ-T3078 POS API verify program meta API negative flow", priority = 4)
	public void verifyProgramMetaNegative() {
		logger.info("===Program meta API===");
		Response response2 = pageObj.endpoints().posProgramMeta(dataSet.get("invalidlocationKey"));
		response2.then().log().all();
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for pos program meta api");
		boolean isPosProgramMetaSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response2.asString());
		Assert.assertTrue(isPosProgramMetaSchemaValidated, "POS Program Meta Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified program meta with invalid location key");
	}

	@Test(description = "SQ-T3074 POS API verify applicable offers API negative flow", priority = 5)
	public void verifyApplicableOffersNegative() {
		String invaliduserEmail = "testabc";
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
//		String token = signUpResponse.jsonPath().get("access_token.token").toString();
//		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// applicable offers with invalid location key
		logger.info("===Applicable offers API with invalid location key===");
		Response applicableresponse = pageObj.endpoints().posApplicableOffer(userEmail, dataSet.get("amount"),
				dataSet.get("invalidlocationKey"));
		applicableresponse.then().log().all();
		Assert.assertEquals(applicableresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for pos program meta api");
		boolean isPosApplicableOffersSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, applicableresponse.asString());
		Assert.assertTrue(isPosApplicableOffersSchemaValidated, "POS Applicable Offers Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified program meta with invalid location key");

		// applicable offers with invalid email
		logger.info("===Applicable offers API with invalid email===");
		Response response = pageObj.endpoints().posApplicableOffer(invaliduserEmail, dataSet.get("amount"),
				dataSet.get("locationKey"));
		response.then().log().all();
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Status code 404 did not matched for pos program meta api");
		boolean isPosApplicableOffersSchemaValidated1 = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response.asString());
		Assert.assertTrue(isPosApplicableOffersSchemaValidated1, "POS Applicable Offers Schema Validation failed");
		TestListeners.extentTest.get().pass("Verified program meta with invalid email");

	}

	@Test(description = "SQ-T3077 POS API Verify User Lookup and Fetch Balance API negative flow", priority = 6)
	public void verifyUserLookupFetchBalanceNegative() throws InterruptedException {
		String invaliduserEmail = "testabc";

		// POS Signup
		TestListeners.extentTest.get().info("== POS API: User Sign-up ==");
		logger.info("== POS API: User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signupResponse = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(signupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API User Sign-up");
		String signupEmail = signupResponse.jsonPath().get("email").toString();
		Assert.assertEquals(signupEmail, userEmail.toLowerCase());
		TestListeners.extentTest.get().pass("POS API User Sign-up call is successful");
		logger.info("POS API User Sign-up call is successful");

		// POS Checkin
		TestListeners.extentTest.get().info("== POS API: Checkin ==");
		logger.info("== POS API: Checkin ==");
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS API Checkin");
		String checkinEmail = checkinResponse.jsonPath().get("email").toString();
		Assert.assertEquals(checkinEmail, userEmail.toLowerCase());
		TestListeners.extentTest.get().pass("POS API Checkin call is successful");
		logger.info("POS API Checkin call is successful");

		// POS User Look-up (/api/pos/users/find) with invalid email
		TestListeners.extentTest.get().info("== POS API: User Look-up (/api/pos/users/find) with invalid email ==");
		logger.info("== POS API: User Look-up (/api/pos/users/find) with invalid email ==");
		Response posUserLookupInvalidEmailResponse = pageObj.endpoints().userLookupPosApi("email", invaliduserEmail,
				dataSet.get("locationKey"), "");
		Assert.assertEquals(posUserLookupInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS user lookUp with invalid email");
		boolean isPosUserLookupInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, posUserLookupInvalidEmailResponse.asString());
		Assert.assertTrue(isPosUserLookupInvalidEmailSchemaValidated, "POS User Look-up Schema Validation failed");
		String posUserLookupInvalidEmailMsg = posUserLookupInvalidEmailResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(posUserLookupInvalidEmailMsg, "User not found", "Message did not match.");
		TestListeners.extentTest.get().pass("POS API User look-up call is unsuccessful with invalid email");
		logger.info("POS API User look-up call is unsuccessful with invalid email");

		// POS User Look-up (/api/pos/users/find) with invalid location key
		TestListeners.extentTest.get()
				.info("== POS API: User Look-up (/api/pos/users/find) with invalid location key ==");
		logger.info("== POS API: User Look-up (/api/pos/users/find) with invalid location key ==");
		Response posUserLookupInvalidLocationKeyResponse = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("invalidlocationKey"), "");
		Assert.assertEquals(posUserLookupInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS user lookUp with invalid location key");
		boolean isPosUserLookupInvalidLocationKeySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, posUserLookupInvalidLocationKeyResponse.asString());
		Assert.assertTrue(isPosUserLookupInvalidLocationKeySchemaValidated,
				"POS User Look-up Schema Validation failed");
		String posUserLookupInvalidLocationKeyMsg = posUserLookupInvalidLocationKeyResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(posUserLookupInvalidLocationKeyMsg, "Invalid LocationKey", "Message did not match.");
		TestListeners.extentTest.get().pass("POS API User look-up call is unsuccessful with invalid location key");
		logger.info("POS API User look-up call is unsuccessful with invalid location key");

		// POS User Look-up (/api/pos/users/find) with missing lookup_value
		TestListeners.extentTest.get()
				.info("== POS API: User Look-up (/api/pos/users/find) with missing lookup_value ==");
		logger.info("== POS API: User Look-up (/api/pos/users/find) with missing lookup_value ==");
		Response posUserLookupMissingLookupValueResponse = pageObj.endpoints().userLookupPosApi("email", "",
				dataSet.get("locationKey"), "");
		Assert.assertEquals(posUserLookupMissingLookupValueResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for POS user lookUp with missing lookup_value");
		boolean isPosUserLookupMissingLookupValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, posUserLookupMissingLookupValueResponse.asString());
		Assert.assertTrue(isPosUserLookupMissingLookupValueSchemaValidated,
				"POS User Look-up Schema Validation failed");
		String posUserLookupMissingLookupValueMsg = posUserLookupMissingLookupValueResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(posUserLookupMissingLookupValueMsg,
				"Required parameter missing or the value is empty: lookup_value", "Message did not match.");
		TestListeners.extentTest.get().pass("POS API User look-up call is unsuccessful with missing lookup_value");
		logger.info("POS API User look-up call is unsuccessful with missing lookup_value");

		// POS User Look-up (/api/pos/users/find) with missing lookup_field
		TestListeners.extentTest.get()
				.info("== POS API: User Look-up (/api/pos/users/find) with missing lookup_field ==");
		logger.info("== POS API: User Look-up (/api/pos/users/find) with missing lookup_field ==");
		Response posUserLookupMissingLookupFieldResponse = pageObj.endpoints().userLookupPosApi("", userEmail,
				dataSet.get("locationKey"), "");
		Assert.assertEquals(posUserLookupMissingLookupFieldResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for POS user lookUp with missing lookup_field");
		boolean isPosUserLookupMissingLookupFieldSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, posUserLookupMissingLookupFieldResponse.asString());
		Assert.assertTrue(isPosUserLookupMissingLookupFieldSchemaValidated,
				"POS User Look-up Schema Validation failed");
		String posUserLookupMissingLookupFieldMsg = posUserLookupMissingLookupFieldResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(posUserLookupMissingLookupFieldMsg,
				"Required parameter missing or the value is empty: lookup_field", "Message did not match.");
		TestListeners.extentTest.get().pass("POS API User look-up call is unsuccessful with missing lookup_field");
		logger.info("POS API User look-up call is unsuccessful with missing lookup_field");

		// POS Get User Balance with invalid location key
		TestListeners.extentTest.get().info("== POS API: Get User Balance with invalid location key ==");
		logger.info("== POS API: Get User Balance with invalid location key ==");
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail,
				dataSet.get("invalidlocationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Get User Balance with invalid location key");
		boolean isPosUserBalanceInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, balanceResponse.asString());
		Assert.assertTrue(isPosUserBalanceInvalidLocationKeySchemaValidated,
				"POS User Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("POS API Get User Balance call is unsuccessful with invalid location key");
		logger.info("POS API Get User Balance call is unsuccessful with invalid location key");

		// POS Get user balance with invalid email
		TestListeners.extentTest.get().info("== POS API: Get User Balance with invalid email ==");
		logger.info("== POS API: Get User Balance with invalid email ==");
		Response balanceResponse1 = pageObj.endpoints().posUserLookupFetchBalance(invaliduserEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS Get User Balance with invalid email");
		boolean isPosUserBalanceInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, balanceResponse1.asString());
		Assert.assertTrue(isPosUserBalanceInvalidEmailSchemaValidated, "POS User Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("POS API Get User Balance call is unsuccessful with invalid email");
		logger.info("POS API Get User Balance call is unsuccessful with invalid email");
	}

	// @Test(description = "SQ-T4747 Verify POS Payments Negative flow", priority =
	// 7)//Heartland service issue
	public void verifyPOSPaymentsNegative() {
		// User Sign-up
		logger.info("== API v1: User Sign-up positive flow ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		TestListeners.extentTest.get().info(userEmail + " Api1 user signup is successful");

		// Generate Single Scan code
		logger.info("== Heartland Payment token: Generate Single Scan Code positive flow ==");
		Response singleTokenResponse1 = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartlandPublicKey"));
		Assert.assertEquals(singleTokenResponse1.statusCode(), 201,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken1 = singleTokenResponse1.jsonPath().getString("token_value");
		TestListeners.extentTest.get().info("Heartland Single token is generated: " + finalSingleScanToken1);

		Response singleScanTokenResponse = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("paymentType"), dataSet.get("transactionToken"),
				finalSingleScanToken1);
		Assert.assertEquals(singleScanTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"singleScanTokenResponse API response status code not matched");
		String single_scan_code = singleScanTokenResponse.jsonPath().getString("single_scan_code").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().info("single_scan_code is generated: " + single_scan_code);

		// Verify Create Payment using invalid single scan code
		logger.info("== POS API: Create Payment using invalid single scan code ==");
		Response posPaymentInvalidSingleScanCodeResponse = pageObj.endpoints().POSPayment("singleScan", "",
				dataSet.get("invalidSingleScanCode"), dataSet.get("paymentType"), dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentInvalidSingleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS Create Payment.");
		TestListeners.extentTest.get()
				.pass("POS Create Payment call is unsuccessful because of invalid single scan code");

		// Verify Create Payment using invalid payment type
		logger.info("== POS API: Create Payment using invalid payment type ==");
		Response posPaymentInvalidPaymentTypeResponse = pageObj.endpoints().POSPayment("singleScan", "",
				single_scan_code, dataSet.get("invalidPaymentType"), dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentInvalidPaymentTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for POS Create Payment.");
		TestListeners.extentTest.get().pass("POS Create Payment call is unsuccessful because of invalid payment type");

		// Verify Create Payment using invalid location key
		logger.info("== POS API: Create Payment using invalid location key ==");
		Response posPaymentInvalidLocationKeyResponse = pageObj.endpoints().POSPayment("singleScan", "",
				single_scan_code, dataSet.get("paymentType"), dataSet.get("invalidLocationKey"));
		Assert.assertEquals(posPaymentInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Create Payment.");
		TestListeners.extentTest.get().pass("POS Create Payment call is unsuccessful because of invalid location key");

		// Create Payment
		logger.info("== POS API: Create Payment positive flow ==");
		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", "", single_scan_code,
				dataSet.get("paymentType"), dataSet.get("locationKey"));
		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		String status = posPaymentResponse.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Create Payment status code not matched.");
		Assert.assertEquals(status, "success", "payment_type is not matched.");
		TestListeners.extentTest.get().pass("POS Create Payment call is successful.");

		// Verify Get Payment Status using invalid location key
		logger.info("== POS API: Get Payment Status using invalid location key ==");
		Response posPaymentStatusInvalidLocationKeyResponse = pageObj.endpoints().POSPaymentStatus(payment_reference_id,
				dataSet.get("invalidLocationKey"));
		Assert.assertEquals(posPaymentStatusInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Get Payment Status.");
		TestListeners.extentTest.get()
				.pass("POS Get Payment Status call is unsuccessful because of invalid location key");

		// Verify Update Payment using invalid payment reference id
		logger.info("== POS API: Update Payment using invalid payment reference id ==");
		Response posPaymentUpdateInvalidPaymentRefIdResponse = pageObj.endpoints().POSPaymentPUT(
				dataSet.get("invalidPaymentReferenceId"), dataSet.get("locationKey"), dataSet.get("statusToUpdate"));
		Assert.assertEquals(posPaymentUpdateInvalidPaymentRefIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS Update Payment.");
		TestListeners.extentTest.get()
				.pass("POS Update Payment call is unsuccessful because of invalid payment reference id");

		// Verify Update Payment using invalid location key
		logger.info("== POS API: Update Payment using invalid location key ==");
		Response posPaymentUpdateInvalidLocationKeyResponse = pageObj.endpoints().POSPaymentPUT(payment_reference_id,
				dataSet.get("invalidLocationKey"), dataSet.get("statusToUpdate"));
		Assert.assertEquals(posPaymentUpdateInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Update Payment.");
		TestListeners.extentTest.get().pass("POS Update Payment call is unsuccessful because of invalid location key");

		// Verify Update Payment using invalid status
		logger.info("== POS API: Update Payment using invalid status ==");
		Response posPaymentUpdateInvalidStatusResponse = pageObj.endpoints().POSPaymentPUT(payment_reference_id,
				dataSet.get("locationKey"), dataSet.get("invalidStatus"));
		Assert.assertEquals(posPaymentUpdateInvalidStatusResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for POS Update Payment.");
		TestListeners.extentTest.get().pass("POS Update Payment call is unsuccessful because of invalid status");

		// Verify Refund Payment using invalid payment reference id
		logger.info("== POS API: Refund Payment using invalid payment reference id ==");
		Response posPaymentRefundInvalidPaymentRefIdResponse = pageObj.endpoints()
				.POSPaymentRefund(dataSet.get("invalidPaymentReferenceId"), dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentRefundInvalidPaymentRefIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS Refund Payment.");
		TestListeners.extentTest.get()
				.pass("POS Refund Payment call is unsuccessful because of invalid payment reference id");

		// Verify Refund Payment using invalid location key
		logger.info("== POS API: Refund Payment using invalid location key ==");
		Response posPaymentRefundInvalidLocationKeyResponse = pageObj.endpoints().POSPaymentRefund(payment_reference_id,
				dataSet.get("invalidLocationKey"));
		Assert.assertEquals(posPaymentRefundInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Refund Payment.");
		TestListeners.extentTest.get().pass("POS Refund Payment call is unsuccessful because of invalid location key");

		// Verify Cancel Payment using invalid payment reference id
		logger.info("== POS API: Cancel Payment using invalid payment reference id ==");
		Response posPaymentCancelInvalidPaymentRefIdResponse = pageObj.endpoints()
				.posPaymentCancel(dataSet.get("invalidPaymentReferenceId"), dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentCancelInvalidPaymentRefIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS Cancel Payment.");
		TestListeners.extentTest.get()
				.pass("POS Cancel Payment call is unsuccessful because of invalid payment reference id");

		// Verify Cancel Payment using invalid location key
		logger.info("== POS API: Cancel Payment using invalid location key ==");
		Response posPaymentCancelInvalidLocationKeyResponse = pageObj.endpoints().posPaymentCancel(payment_reference_id,
				dataSet.get("invalidLocationKey"));
		Assert.assertEquals(posPaymentCancelInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for POS Cancel Payment.");
		TestListeners.extentTest.get().pass("POS Cancel Payment call is unsuccessful because of invalid location key");
	}

	@Test(description = "Verify POS API Negative Scenarios:- SQ-T5302: Unlock Discount Basket (Redemptions 2.0); SQ-T5560: Fetch Account Balance", groups = "api")
	public void verifyPOSUnlockDiscountBasketNegative() throws Exception {

		/*
		 * Pre-conditions:
		 * 
		 * SQ-T5302: To check for POS Unlock Discount Basket with unsupported business
		 * (autoone), ensure that under Cockpit > Redemptions > Multiple Redemptions >
		 * "Enable Reward Locking" is unchecked.
		 * 
		 * To check for POS Unlock Discount Basket with supported business (autonine),
		 * ensure that under Cockpit > Redemptions > Multiple Redemptions >
		 * "Enable Auto-redemption" and "Enable Reward Locking" are checked. Location
		 * "Automation- Redemption 2.0" should have the
		 * "Allow Location for Multiple Redemption" checked.
		 * 
		 */

		String invalidValue = "123abc";

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Send reward to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// POS Add Discount to Basket
		TestListeners.extentTest.get().info("== POS API: Add Discount to Basket ==");
		logger.info("== POS API: Add Discount to Basket ==");
		String externalUid = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response addDiscountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUid);
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Add Discount to Basket call.");
		String discountId = utils.getJsonReponseKeyValueFromJsonArray(addDiscountBasketResponse,
				"discount_basket_items", "discount_id", rewardId);
		Assert.assertEquals(discountId, rewardId, "ID did not match.");
		TestListeners.extentTest.get().pass("POS API Add Discount to Basket call is successful");
		logger.info("POS API Add Discount to Basket call is successful");

		// POS Unlock Discount Basket with invalid User ID
		TestListeners.extentTest.get().info("== POS API: Unlock Discount Basket with invalid User ID ==");
		logger.info("== POS API: Unlock Discount Basket with invalid User ID ==");
		Response basketUnlockInvalidUserIdResponse = pageObj.endpoints()
				.discountUnlockPOSAPI(dataSet.get("locationKey"), invalidValue, externalUid);
		Assert.assertEquals(basketUnlockInvalidUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS API Unlock Discount Basket with invalid User ID.");
		boolean isBasketUnlockInvalidUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, basketUnlockInvalidUserIdResponse.asString());
		Assert.assertTrue(isBasketUnlockInvalidUserIdSchemaValidated,
				"POS Unlock Discount Basket Schema Validation failed");
		String basketUnlockInvalidUserIdResponseMsg = basketUnlockInvalidUserIdResponse.jsonPath().getString("error");
		Assert.assertEquals(basketUnlockInvalidUserIdResponseMsg, "User not found.", "Message did not match.");
		TestListeners.extentTest.get().pass("POS API Unlock Discount Basket call with invalid User ID is unsuccessful");
		logger.info("POS API Unlock Discount Basket call with invalid User ID is unsuccessful");

		// POS Unlock Discount Basket with invalid External UID
		TestListeners.extentTest.get().info("== POS API: Unlock Discount Basket with invalid External UID ==");
		logger.info("== POS API: Unlock Discount Basket with invalid External UID ==");
		Response basketUnlockInvalidExternalUidResponse = pageObj.endpoints()
				.discountUnlockPOSAPI(dataSet.get("locationKey"), userId, invalidValue);
		Assert.assertEquals(basketUnlockInvalidExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for POS API Unlock Discount Basket with invalid External UID.");
		boolean isBasketUnlockInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, basketUnlockInvalidExternalUidResponse.asString());
		Assert.assertTrue(isBasketUnlockInvalidExternalUidSchemaValidated,
				"POS Unlock Discount Basket Schema Validation failed");
		String basketUnlockInvalidExternalUidResponseMsg = basketUnlockInvalidExternalUidResponse.jsonPath()
				.getString("error");
		Assert.assertEquals(basketUnlockInvalidExternalUidResponseMsg, "Unable to find valid discount basket.",
				"Message did not match.");
		TestListeners.extentTest.get()
				.pass("POS API Unlock Discount Basket call with invalid External UID is unsuccessful");
		logger.info("POS API Unlock Discount Basket call with invalid External UID is unsuccessful");

		// POS Process Batch Redemption
		TestListeners.extentTest.get().info("== POS API: Process Batch Redemption ==");
		logger.info("== POS API: Process Batch Redemption ==");
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationKey"), userId, "30", "1", "12003", externalUid);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		TestListeners.extentTest.get().pass("POS API Process Batch Redemption call is successful");
		logger.info("POS API Process Batch Redemption call is successful");

		// POS Unlock Discount Basket for already processed redemption
		TestListeners.extentTest.get().info("== POS API: Unlock Discount Basket for already processed redemption ==");
		logger.info("== POS API: Unlock Discount Basket that for already processed redemption ==");
		Response basketUnlockResponse = pageObj.endpoints().discountUnlockPOSAPI(dataSet.get("locationKey"), userId,
				externalUid);
		Assert.assertEquals(basketUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for POS API Unlock Discount Basket for already processed redemption.");
		boolean isBasketUnlockAlreadyProcessedSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, basketUnlockResponse.asString());
		Assert.assertTrue(isBasketUnlockAlreadyProcessedSchemaValidated,
				"POS Unlock Discount Basket Schema Validation failed");
		String basketUnlockResponseMsg = basketUnlockResponse.jsonPath().getString("error");
		Assert.assertEquals(basketUnlockResponseMsg, "User does not have an active basket.", "Message did not match.");
		TestListeners.extentTest.get()
				.pass("POS API Unlock Discount Basket call for already processed redemption is unsuccessful");
		logger.info("POS API Unlock Discount Basket call for already processed redemption is unsuccessful");

		// Verifying POS Unlock Discount Basket with unsupported business. Cockpit >
		// Redemptions > Multiple Redemptions > Enable Reward Locking is unchecked.
		TestListeners.extentTest.get()
				.info("Verifying POS Unlock Discount Basket with unsupported business: " + dataSet.get("slug1")
						+ ". Cockpit > Redemptions > Multiple Redemptions > Enable Reward Locking is unchecked.");
		logger.info("Verifying POS Unlock Discount Basket with unsupported business: " + dataSet.get("slug1")
				+ ". Cockpit > Redemptions > Multiple Redemptions > Enable Reward Locking is unchecked.");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client1"),
				dataSet.get("secret1"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Send reward to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId1, dataSet.get("apiKey1"), "",
				dataSet.get("redeemableId1"), "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client1"), dataSet.get("secret1"),
				dataSet.get("redeemableId1"));
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId1);
		logger.info("Reward Id for user is fetched: " + rewardId1);

		// POS Add Discount to Basket
		TestListeners.extentTest.get().info("== POS API: Add Discount to Basket ==");
		logger.info("== POS API: Add Discount to Basket ==");
		String externalUid1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response addDiscountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey1"), userId1, "reward", rewardId1, externalUid1);
		Assert.assertEquals(addDiscountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Add Discount to Basket call.");
		String discountId1 = utils.getJsonReponseKeyValueFromJsonArray(addDiscountBasketResponse1,
				"discount_basket_items", "discount_id", rewardId1);
		Assert.assertEquals(discountId1, rewardId1, "ID did not match.");
		TestListeners.extentTest.get().pass("POS API Add Discount to Basket call is successful");
		logger.info("POS API Add Discount to Basket call is successful");

		// POS Unlock Discount Basket with invalid business configuration
		TestListeners.extentTest.get()
				.info("== POS API: Unlock Discount Basket with invalid business configuration ==");
		logger.info("== POS API: Unlock Discount Basket with invalid business configuration ==");
		Response basketUnlockInvalidBusinessConfig = pageObj.endpoints()
				.discountUnlockPOSAPI(dataSet.get("locationKey1"), userId1, externalUid1);
		Assert.assertEquals(basketUnlockInvalidBusinessConfig.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for POS API Unlock Discount Basket with invalid business configuration.");
		boolean isBasketUnlockInvalidBusinessConfigSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, basketUnlockInvalidBusinessConfig.asString());
		Assert.assertTrue(isBasketUnlockInvalidBusinessConfigSchemaValidated,
				"POS Unlock Discount Basket Schema Validation failed");
		String basketUnlockInvalidBusinessConfigMsg = basketUnlockInvalidBusinessConfig.jsonPath().getString("error");
		Assert.assertEquals(basketUnlockInvalidBusinessConfigMsg,
				"Invalid Business Configuration. Please connect with your Customer Success representative for resolution of the issue.",
				"Message did not match.");
		TestListeners.extentTest.get()
				.pass("POS API Unlock Discount Basket call with invalid business configuration is unsuccessful");
		logger.info("POS API Unlock Discount Basket call with invalid business configuration is unsuccessful");

		// POS Fetch Account Balance with invalid user Id
		TestListeners.extentTest.get().info("== POS API: Fetch Account Balance with invalid user Id ==");
		logger.info("== POS API: Fetch Account Balance with invalid user Id ==");
		Response fetchAccountBalanceInvalidUserIdResponse = pageObj.endpoints().posFetchAccountBalance(invalidValue,
				"subscription", dataSet.get("locationKey1"));
		Assert.assertEquals(fetchAccountBalanceInvalidUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND);
		boolean isFetchAccountBalanceInvalidUserIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchAccountBalanceInvalidUserIdResponse.asString());
		Assert.assertTrue(isFetchAccountBalanceInvalidUserIdSchemaValidated,
				"POS Fetch Account Balance Schema Validation failed");
		String fetchAccountBalanceInvalidUserIdMsg = fetchAccountBalanceInvalidUserIdResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(fetchAccountBalanceInvalidUserIdMsg, "User not found");
		TestListeners.extentTest.get().info("POS API Fetch Account Balance call with invalid user Id is unsuccessful");
		logger.info("POS API Fetch Account Balance call with invalid user Id is unsuccessful");

		// POS Fetch Account Balance with missing user Id
		TestListeners.extentTest.get().info("== POS API: Fetch Account Balance with missing user Id ==");
		logger.info("== POS API: Fetch Account Balance with missing user Id ==");
		Response fetchAccountBalanceMissingUserIdResponse = pageObj.endpoints().posFetchAccountBalance("",
				"subscription", dataSet.get("locationKey1"));
		Assert.assertEquals(fetchAccountBalanceMissingUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isFetchAccountBalanceMissingUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchAccountBalanceMissingUserIdResponse.asString());
		Assert.assertTrue(isFetchAccountBalanceMissingUserIdSchemaValidated,
				"POS Fetch Account Balance Schema Validation failed");
		String fetchAccountBalanceMissingUserIdMsg = fetchAccountBalanceMissingUserIdResponse.jsonPath()
				.getString("error");
		Assert.assertEquals(fetchAccountBalanceMissingUserIdMsg,
				"Required parameter missing or the value is empty: user_id");
		TestListeners.extentTest.get().info("POS API Fetch Account Balance call with missing user Id is unsuccessful");
		logger.info("POS API Fetch Account Balance call with missing user Id is unsuccessful");

		// POS Fetch Account Balance with invalid location key
		TestListeners.extentTest.get().info("== POS API: Fetch Account Balance with invalid location key ==");
		logger.info("== POS API: Fetch Account Balance with invalid location key ==");
		Response fetchAccountBalanceInvalidLocationKeyResponse = pageObj.endpoints().posFetchAccountBalance(userId1,
				"subscription", invalidValue);
		Assert.assertEquals(fetchAccountBalanceInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isFetchAccountBalanceInvalidLocationKeySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchAccountBalanceInvalidLocationKeyResponse.asString());
		Assert.assertTrue(isFetchAccountBalanceInvalidLocationKeySchemaValidated,
				"POS Fetch Account Balance Schema Validation failed");
		String fetchAccountBalanceInvalidLocationKeyMsg = fetchAccountBalanceInvalidLocationKeyResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(fetchAccountBalanceInvalidLocationKeyMsg, "Invalid LocationKey");
		TestListeners.extentTest.get()
				.info("POS API Fetch Account Balance call with invalid location key is unsuccessful");
		logger.info("POS API Fetch Account Balance call with invalid location key is unsuccessful");

	}
	
	//akansha jain
	@Test(description = "SQ-T7218 Verify pos user search API default behavior (ApiCompleteAccount) when parametrization is enabled.", groups = "api", priority = 4)
	@Owner(name = "Akansha Jain")
	public void SQ_7218_verifyPOSUsersSearchWithInvalidDetails() throws Exception {
			
		// DB - update preference column in business table
		// updating enable_api_parameterization to true
		String b_id = dataSet.get("business_id");
		String query = "SELECT preferences FROM businesses WHERE id = '" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = false;
		try {
				flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		} catch (Exception e) {
				e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");
			
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		
		// get pos user search api with invalid single scan code
		logger.info("===pos users search API===");
		Response usersearchresponse = pageObj.endpoints().posUserLookupSingleScanToken(dataSet.get("invalidSingleScanToken"), dataSet.get("location"));
				usersearchresponse.then().log().all();
		Assert.assertEquals(usersearchresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "Status code 404 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified pos user search api with parametrization ON and invalid single scan code");

		// get pos user search api with invalid location key
		logger.info("===pos users search API===");
		Response usersearchresponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("invalidLocation"));
		usersearchresponse2.then().log().all();
		Assert.assertEquals(usersearchresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified pos user search api with parametrization ON and invalid location key");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
