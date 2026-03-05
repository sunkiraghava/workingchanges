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

import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.apiConfig.AuthHeaders;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class BusinessEarningTypeAPITest {
	static Logger logger = LogManager.getLogger(BusinessEarningTypeAPITest.class);
	public WebDriver driver;
	AuthHeaders authHeaders;
	ApiUtils apiUtils;
	String userEmail;
	Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	String blankSpace = "";
	private String env;
	private Utilities utils;
	private static Map<String, String> dataSet;
	// String adminAuthorization = "19Ez5ypiztii6J6we3J1"; // pp Super Admin

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		authHeaders = new AuthHeaders();
		apiUtils = new ApiUtils();
		utils = new Utilities();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3017 verifying auth online Point Conversion API", groups = "api", priority = 0)
	public void verifyPointConversionAPI() {
		// User SignUp from Auth Api
		String BlankSpace = "";
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String userId = signUpResponse.jsonPath().get("id").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userId,
				dataSet.get("adminAuthorization"), "", "", "", dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Point Conversion API
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("Point Conversion API hit count is : " + attempts);
				logger.info("Point Conversion API hit count is : " + attempts);
				Response pointConversionAPIResponse = pageObj.endpoints().authApiPointConversionAPI(authToken,
						dataSet.get("client"), dataSet.get("secret"), dataSet.get("conversion_rule_id"),
						dataSet.get("converted_value"), dataSet.get("source_value"), BlankSpace);
				int statusCode = pointConversionAPIResponse.getStatusCode();
				if (statusCode == 200) {
					boolean isAuthPointConversionSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.apiStringArraySchema, pointConversionAPIResponse.asString());
					Assert.assertTrue(isAuthPointConversionSchemaValidated,
							"Auth Point Conversion API schema validation failed");
					TestListeners.extentTest.get().pass("Auth Point Conversion API is successful");
					logger.info("Auth Point Conversion API is successful");
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}

	}

	@Test(description = "SQ-T2364 verifying auth online reward redemption", groups = "api", priority = 1)
	public void verifyAuthOnlineRewardRedemption() throws InterruptedException {
		logger.info("==  ==");
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		// Checkin via auth API
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Thread.sleep(60000);
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(fetchAccountBalResponse, "Auth fetch account balance API");
		Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthAccountBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchAccountBalanceSchema, fetchAccountBalResponse.asString());
		Assert.assertTrue(isAuthAccountBalanceSchemaValidated, "Auth fetch account balance schema validation failed");
		if (!(env.equalsIgnoreCase("qa"))) {
		String reward_id = fetchAccountBalResponse.jsonPath().get("rewards[0].id").toString();
//		int i=0;
//		String reward_id = null ;
//		Response fetchAccountBalResponse = null;
//		while(reward_id==null && i<=5) {
//			Thread.sleep(1000);
//			 fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(authToken,
//					dataSet.get("client"), dataSet.get("secret"));
//			 Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//				apiUtils.verifyResponse(fetchAccountBalResponse, "Auth fetch account balance API");
//			 reward_id = fetchAccountBalResponse.jsonPath().get("rewards[0].id").toString();
//			 System.out.println(i+"  reward_id"+reward_id);
//			 i++;
//		}

//		System.out.println(fetchAccountBalResponse.prettyPrint());
		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, reward_id,
				dataSet.get("secret"), dataSet.get("client"));
		apiUtils.verifyResponse(resp, "Auth online order redemption");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		}
	}

	@Test(description = "SQ-T2381 Verify point conversion and user account history", groups = "api", priority = 2)
	public void verify_Point_Conversion_User_account_history() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward to user 200 Points

		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", "", "", "100");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

		// Point conversion
		Response pointsResponse = pageObj.endpoints().Api2PointConversion(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 point conversion");
		boolean isPointConversionSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, pointsResponse.asString());
		Assert.assertTrue(isPointConversionSchemaValidated, "API v2 Point conversion Schema Validation failed");

		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Account History");
		boolean isAccountHistorySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2AccountHistorySchema, accountHistoryResponse.asString());
		Assert.assertTrue(isAccountHistorySchemaValidated, "API v2 User Account History Schema Validation failed");

	}

	@Test(description = "SQ-T2367 verifying auth online redeemable redemption", groups = "api", priority = 3)
	public void verifyAuthOnlineRedeemableRedemption() {
		logger.info("== Verifying auth online Bank Currency redemption ==");
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		// Checkin via auth API
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().authOnlineRedeemableRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"), "1001", dataSet.get("redeemable_id"));
		apiUtils.verifyResponse(resp, "Auth online order bank currency redemption");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
	}

	@Test(description = "SQ-T3061 Negative Scenarios Of Auth Api Point Conversion API", groups = "api", priority = 4)
	public void T3061_verifyNegativeScenariosOfAuthApiPointConversionAPI() throws InterruptedException {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String userId = signUpResponse.jsonPath().get("id").toString();

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userId,
				dataSet.get("adminAuthorization"), "", "", "", dataSet.get("points"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Point Conversion API with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response pointConversionAPIResponse = pageObj.endpoints().authApiPointConversionAPI(authToken1,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("conversion_rule_id"),
				dataSet.get("converted_value"), dataSet.get("source_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(pointConversionAPIResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Point Conversion API with invalid Authentication Token");
		boolean isPointConversionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, pointConversionAPIResponse.asString());
		Assert.assertTrue(isPointConversionInvalidTokenSchemaValidated,
				"Auth Point Conversion API schema validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Point Conversion API with invalid Authentication Token is unsuccessful (expected)");

		// Point Conversion API with invalid client
		Response pointConversionAPIResponse1 = pageObj.endpoints().authApiPointConversionAPI(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), dataSet.get("conversion_rule_id"),
				dataSet.get("converted_value"), dataSet.get("source_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(pointConversionAPIResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Point Conversion API");
		boolean isPointConversionInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, pointConversionAPIResponse1.asString());
		Assert.assertTrue(isPointConversionInvalidSignatureSchemaValidated,
				"Auth Point Conversion API schema validation failed");
		TestListeners.extentTest.get().pass("auth Api Point Conversion API is successful");

		// Point Conversion API with invalid Secret
		Response pointConversionAPIResponse2 = pageObj.endpoints().authApiPointConversionAPI(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"), dataSet.get("conversion_rule_id"),
				dataSet.get("converted_value"), dataSet.get("source_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(pointConversionAPIResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Point Conversion API with invalid Secret");
		TestListeners.extentTest.get()
				.pass("auth Api Point Conversion API with invalid Secret is unsuccessful (expected)");

		// Point Conversion API invalid conversion rule id
		Response pointConversionAPIResponse3 = pageObj.endpoints().authApiPointConversionAPI(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("invalidConversion_rule_id"),
				dataSet.get("converted_value"), dataSet.get("source_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse3.jsonPath().getString("error"),
				"[[message:Invalid conversion rule., code:invalid_rule]]");
		Assert.assertEquals(pointConversionAPIResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for auth Api Point Conversion API with invalid invalid conversion rule id");
		boolean isPointConversionInvalidConversionRuleSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPointConversionErrorSchema, pointConversionAPIResponse3.asString());
		Assert.assertTrue(isPointConversionInvalidConversionRuleSchemaValidated,
				"Auth Point Conversion API schema validation failed");
		TestListeners.extentTest.get().pass(
				"auth Api Point Conversion API with invalid invalid conversion rule id is unsuccessful (expected)");

		// Point Conversion API with invalid converted value
		Response pointConversionAPIResponse4 = pageObj.endpoints().authApiPointConversionAPI(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("conversion_rule_id"),
				dataSet.get("invalidConverted_value"), dataSet.get("source_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse4.jsonPath().getString("error"),
				"[[message:Conversion ratio is invalid., code:invalid_ratio]]");
		Assert.assertEquals(pointConversionAPIResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Point Conversion APIwith invalid converted value");
		boolean isPointConversionInvalidConvertedValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPointConversionErrorSchema, pointConversionAPIResponse4.asString());
		Assert.assertTrue(isPointConversionInvalidConvertedValueSchemaValidated,
				"Auth Point Conversion API schema validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Point Conversion API with invalid converted value is unsuccessful (expected)");

		// Point Conversion API with invalid Source value
		Response pointConversionAPIResponse5 = pageObj.endpoints().authApiPointConversionAPI(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("conversion_rule_id"),
				dataSet.get("converted_value"), dataSet.get("invalidSource_value"), blankSpace);
		Assert.assertEquals(pointConversionAPIResponse5.jsonPath().getString("error"),
				"[[message:Conversion ratio is invalid., code:invalid_ratio]]");
		Assert.assertEquals(pointConversionAPIResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Point Conversion API with invalid Source value");
		boolean isPointConversionInvalidSourceValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPointConversionErrorSchema, pointConversionAPIResponse5.asString());
		Assert.assertTrue(isPointConversionInvalidSourceValueSchemaValidated,
				"Auth Point Conversion API schema validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Point Conversion API with invalid Source value is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3097 Verify Api2 Notifications API negative flow", groups = "api", priority = 5)
	public void Api2NotificationsNegativeApi() throws InterruptedException {
		String invalidtoken = "1";
		String invalidmessage_id = "";
		String invalidnotification_id = "";

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Fetch User Notifications with invalid client
		Response fetchNotificationsResponse1 = pageObj.endpoints().Api2FetchNotifications(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchNotificationsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch notifications");
		boolean isFetchNotificationsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchNotificationsResponse1.asString());
		Assert.assertTrue(isFetchNotificationsInvalidClientSchemaValidated,
				"API v2 Fetch User Notifications schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch User Notifications with invalid secret
		Response fetchNotificationsResponse2 = pageObj.endpoints().Api2FetchNotifications(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchNotificationsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch notifications");
		boolean isFetchNotificationsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchNotificationsResponse2.asString());
		Assert.assertTrue(isFetchNotificationsInvalidSecretSchemaValidated,
				"API v2 Fetch User Notifications schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch User Notifications with invalid token
		Response fetchNotificationsResponse3 = pageObj.endpoints().Api2FetchNotifications(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchNotificationsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch notifications");
		boolean isFetchNotificationsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchNotificationsResponse3.asString());
		Assert.assertTrue(isFetchNotificationsInvalidTokenSchemaValidated,
				"API v2 Fetch User Notifications schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch User Notifications
		utils.longWaitInSeconds(60);
		Response fetchNotificationsResponse = pageObj.endpoints().Api2FetchNotifications(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchNotificationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch notifications");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");
		String notification_id = fetchNotificationsResponse.jsonPath().get("[0].id").toString();
		logger.info("Response time in milliseconds is :" + fetchNotificationsResponse.getTime());

		// Delete User Notification Response with invalid client
		Response deleteNotificationsResponse = pageObj.endpoints()
				.Api2DeletehNotifications(dataSet.get("invalidclient"), dataSet.get("secret"), token, notification_id);
		Assert.assertEquals(deleteNotificationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 delete notifications");
		boolean isDeleteNotificationsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, deleteNotificationsResponse.asString());
		Assert.assertTrue(isDeleteNotificationsInvalidClientSchemaValidated,
				"API v2 Delete User Notification schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful ");

		// Delete User Notification Response with invalid secret
		Response deleteNotificationsResponse1 = pageObj.endpoints().Api2DeletehNotifications(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, notification_id);
		Assert.assertEquals(deleteNotificationsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 delete notifications");
		boolean isDeleteNotificationsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, deleteNotificationsResponse1.asString());
		Assert.assertTrue(isDeleteNotificationsInvalidSecretSchemaValidated,
				"API v2 Delete User Notification schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful ");

		// Delete User Notification Response with invalid token
		Response deleteNotificationsResponse2 = pageObj.endpoints().Api2DeletehNotifications(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, notification_id);
		Assert.assertEquals(deleteNotificationsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 delete notifications");
		boolean isDeleteNotificationsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, deleteNotificationsResponse2.asString());
		Assert.assertTrue(isDeleteNotificationsInvalidTokenSchemaValidated,
				"API v2 Delete User Notification schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful ");

		// Delete User Notification Response with invalid notification id
		Response deleteNotificationsResponse3 = pageObj.endpoints().Api2DeletehNotifications(dataSet.get("client"),
				dataSet.get("secret"), token, invalidnotification_id);
		Assert.assertEquals(deleteNotificationsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 delete notifications");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful ");

		// Fetch Messages
		String message_id = "";
		// boolean status = false;
		int counter = 0;
		while (counter < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + counter);
				logger.info("API hit count is : " + counter);
				utils.longwait(5000);
				Response fetchMessagesResponse = pageObj.endpoints().Api2FetchMessages(dataSet.get("client"),
						dataSet.get("secret"), token);
				message_id = fetchMessagesResponse.jsonPath().get("messages[0].message_id").toString();

				if (message_id != null) {
					// status = true;
					Assert.assertEquals(fetchMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 Fetch Messages");
					TestListeners.extentTest.get().pass("Api2 Fetch Messages successful ");
					break;
				}
			} catch (Exception e) {

			}
			counter++;
		}

		// Fetch Messages with invalid client
		Response fetchMessagesResponse1 = pageObj.endpoints().Api2FetchMessages(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchMessagesResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch Messages");
		boolean isFetchMessagesInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchMessagesResponse1.asString());
		Assert.assertTrue(isFetchMessagesInvalidClientSchemaValidated,
				"API v2 Fetch Messages schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch Messages with invalid secret
		Response fetchMessagesResponse2 = pageObj.endpoints().Api2FetchMessages(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchMessagesResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch Messages");
		boolean isFetchMessagesInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchMessagesResponse2.asString());
		Assert.assertTrue(isFetchMessagesInvalidSecretSchemaValidated,
				"API v2 Fetch Messages schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch Messages with invalid token
		Response fetchMessagesResponse3 = pageObj.endpoints().Api2FetchMessages(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchMessagesResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch Messages");
		boolean isFetchMessagesInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchMessagesResponse3.asString());
		Assert.assertTrue(isFetchMessagesInvalidTokenSchemaValidated, "API v2 Fetch Messages schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Mark message read with invalid client
		Response markMessagesReadResponse = pageObj.endpoints().Api2MarkMessagesRead(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(markMessagesReadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Mark Messages Read");
		boolean isMarkMessagesReadInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, markMessagesReadResponse.asString());
		Assert.assertTrue(isMarkMessagesReadInvalidClientSchemaValidated,
				"API v2 Mark Messages Read schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Mark Messages Read unsuccessful ");

		// Mark message read with invalid secret
		Response markMessagesReadResponse1 = pageObj.endpoints().Api2MarkMessagesRead(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(markMessagesReadResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Mark Messages Read");
		boolean isMarkMessagesReadInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, markMessagesReadResponse1.asString());
		Assert.assertTrue(isMarkMessagesReadInvalidSecretSchemaValidated,
				"API v2 Mark Messages Read schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Mark Messages Read unsuccessful ");

		// Mark message read with invalid token
		Response markMessagesReadResponse2 = pageObj.endpoints().Api2MarkMessagesRead(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(markMessagesReadResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Mark Messages Read");
		boolean isMarkMessagesReadInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, markMessagesReadResponse2.asString());
		Assert.assertTrue(isMarkMessagesReadInvalidTokenSchemaValidated,
				"API v2 Mark Messages Read schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Mark Messages Read unsuccessful ");

		// delete message with invalid client
		Response deleteMessagesResponse = pageObj.endpoints().Api2DeleteMessages(dataSet.get("invalidclient"),
				message_id, dataSet.get("secret"), token);
		Assert.assertEquals(deleteMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Delete Messages");
		boolean isDeleteMessagesInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, deleteMessagesResponse.asString());
		Assert.assertTrue(isDeleteMessagesInvalidClientSchemaValidated,
				"API v2 Delete Message schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete Messages unsuccessful");

		// delete message with invalid secret
		Response deleteMessagesResponse1 = pageObj.endpoints().Api2DeleteMessages(dataSet.get("client"), message_id,
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(deleteMessagesResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Delete Messages");
		boolean isDeleteMessagesInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, deleteMessagesResponse1.asString());
		Assert.assertTrue(isDeleteMessagesInvalidSecretSchemaValidated,
				"API v2 Delete Message schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete Messages unsuccessful");

		// delete message with invalid token
		Response deleteMessagesResponse2 = pageObj.endpoints().Api2DeleteMessages(dataSet.get("client"), message_id,
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(deleteMessagesResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Delete Messages");
		boolean isDeleteMessagesInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, deleteMessagesResponse2.asString());
		Assert.assertTrue(isDeleteMessagesInvalidTokenSchemaValidated,
				"API v2 Delete Message schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete Messages unsuccessful");

		// delete message with invalid message_id
		Response deleteMessagesResponse3 = pageObj.endpoints().Api2DeleteMessages(dataSet.get("client"),
				invalidmessage_id, dataSet.get("secret"), token);
		Assert.assertEquals(deleteMessagesResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 Delete Messages");
		TestListeners.extentTest.get().pass("Api2 Delete Messages unsuccessful");

		// Generate OTP token with invalid client
		Response getOTPTokenResponse = pageObj.endpoints().Api2GenerateOtpToken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(getOTPTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Generate Otp Token");
		boolean isGenerateOtpTokenInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, getOTPTokenResponse.asString());
		Assert.assertTrue(isGenerateOtpTokenInvalidClientSchemaValidated,
				"API v2 Generate OTP Token schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Otp Token is unsuccessful");

		// Generate OTP token with invalid secret
		Response getOTPTokenResponse1 = pageObj.endpoints().Api2GenerateOtpToken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(getOTPTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Generate Otp Token");
		boolean isGenerateOtpTokenInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, getOTPTokenResponse1.asString());
		Assert.assertTrue(isGenerateOtpTokenInvalidSecretSchemaValidated,
				"API v2 Generate OTP Token schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Otp Token is unsuccessful");

		// Generate OTP token with invalid token
		Response getOTPTokenResponse2 = pageObj.endpoints().Api2GenerateOtpToken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(getOTPTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Generate Otp Token");
		boolean isGenerateOtpTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, getOTPTokenResponse2.asString());
		Assert.assertTrue(isGenerateOtpTokenInvalidTokenSchemaValidated,
				"API v2 Generate OTP Token schema validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Otp Token is unsuccessful");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}