package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ApiV1Test {
	static Logger logger = LogManager.getLogger(ApiV1Test.class);
	private Properties prop;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String run = "api";
	private String env;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
		pageObj = new PageObj();
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
	}

	@Test(description = "SQ-T5688: Verify API V1 for User Signup, Login, Checkin, User Balance, Rich Messages", groups = "api", priority = 0)
	public void T5688_verifyApiV1UserSignupLogin() {

		// Api V1 User Sign-up
		pageObj.utils().logit("info", "== API v1 User signup ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response guestSignupResponse = pageObj.endpoints().apiV1UserSignup(userEmail, dataSet.get("punchhAppKey"));
		Assert.assertEquals(guestSignupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 user signup");
		boolean isApi1UserSignupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1UserSignupLoginSchema, guestSignupResponse.asString());
		Assert.assertTrue(isApi1UserSignupSchemaValidated, "API1 User Signup Schema Validation failed");
		String signupEmail = guestSignupResponse.jsonPath().getString("email");
		Assert.assertNotNull(signupEmail, "Email should not be null in signup response");
		Assert.assertEquals(signupEmail, userEmail.toLowerCase(), "Email should match");
		String signupUserId = guestSignupResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		String password = prop.getProperty("password");
		pageObj.utils().logit("pass", "API v1 user signup is successful. User ID: " + signupUserId + ", Email: " + signupEmail);

		// Api V1 User Login
		pageObj.utils().logit("info", "== API v1 User login ==");
		Response guestLoginresponse = pageObj.endpoints().apiV1UserLogin(userEmail, password,
				dataSet.get("punchhAppKey"), dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(guestLoginresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 user login");
		boolean isApi1UserLoginSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1UserSignupLoginSchema, guestLoginresponse.asString());
		Assert.assertTrue(isApi1UserLoginSchemaValidated, "API1 User Login Schema Validation failed");
		String loginEmail = guestLoginresponse.jsonPath().getString("email");
		Assert.assertEquals(loginEmail, userEmail.toLowerCase(), "Email should match");
		String loginUserId = guestLoginresponse.jsonPath().getString("user_id");
		Assert.assertEquals(loginUserId, signupUserId, "Login user ID should match signup user ID");
		pageObj.utils().logit("pass", "API v1 user login is successful. User ID: " + loginUserId);

		// POS Checkin
		pageObj.utils().logit("info", "== POS Checkin ==");
		Response posCheckinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(posCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS checkin");
		pageObj.utils().logit("pass", "POS checkin is successful");

		// API v1 Checkin
		pageObj.utils().logit("info", "== API v1 Checkin ==");
		Response apiV1CheckinResponse = pageObj.endpoints().checkinsApiV1(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(apiV1CheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 checkin");
		boolean isApi1CheckinSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiV1CheckinSchema, apiV1CheckinResponse.asString());
		Assert.assertTrue(isApi1CheckinSchemaValidated, "API1 Checkin Schema Validation failed");
		int checkinCount = apiV1CheckinResponse.jsonPath().getList("$").size();
		Assert.assertTrue(checkinCount > 0, "Checkin count should be greater than 0");
		String pointsEarnedActual = apiV1CheckinResponse.jsonPath().getString("[0].points_earned");
		Assert.assertNotNull(pointsEarnedActual, "Points earned should not be null");
		Assert.assertEquals(pointsEarnedActual, dataSet.get("amount"), "Points earned is not as expected");
		long checkinId = apiV1CheckinResponse.jsonPath().getLong("[0].checkin_id");
		Assert.assertTrue(checkinId > 0, "Checkin ID should be greater than 0");
		pageObj.utils().logit("pass", "API v1 Checkin is successful. Checkin ID: " + checkinId + ", Points Earned: " + pointsEarnedActual);

		// API V1 User Balance
		pageObj.utils().logit("info", "== API v1 User Balance ==");
		Response guestBalanceResponse = pageObj.endpoints().apiV1UserBalance(userEmail, password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(guestBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user balance");
		pageObj.utils().logit("pass", "API v1 user balance is successful. Response: " + guestBalanceResponse.asString());

		// API V1 Get Rich messages
		pageObj.utils().logit("info", "== API v1 Get Rich Messages ==");
		String messageId = null;
		int counter = 0;
		while (counter < 3) {
			try {
				pageObj.utils().logit("info", "API hit count is : " + counter);
				Response getRichMessagesResponse = pageObj.endpoints().apiV1GetRichMessages(userEmail, password,
						dataSet.get("punchhAppKey"));
				messageId = getRichMessagesResponse.jsonPath().getString("messages[0].message_id");
				if (messageId != null) {
					Assert.assertEquals(getRichMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
					boolean isApi1FetchRichMessagesSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api1FetchMessagesSchema, getRichMessagesResponse.asString());
					Assert.assertTrue(isApi1FetchRichMessagesSchemaValidated,
							"API1 Fetch Rich Messages Schema Validation failed");
					pageObj.utils().logit("pass", "API v1 Rich Message is fetched successfully with id: " + messageId);
					break;
				}
			} catch (Exception e) {
				pageObj.utils().logit("info", "Retry attempt " + counter + " failed: " + e.getMessage());
			}
			counter++;
			utils.longWaitInSeconds(5); // API takes 5-10s to get messageId in dry run
		}
		
		// Ensure rich message was actually returned after all retries
		Assert.assertNotNull(messageId, "Rich message was not returned after " + counter + " retry attempts");
	}

	@Test(description = "SQ-T5687: Verify API V1 Negative Scenarios for User Signup, Login, Checkin, User Balance, Checkin balance, "
			+ "Rich messages, Redemptions, Offers, User Membership Level", groups = "api")
	public void T5687_verifyApiV1Negative() {

		String incorrectInfoMsg = "Incorrect information submitted. Please retry.";
		String notSignedInMsg = "You need to sign in or sign up before continuing.";

		// API V1 User Sign-up
		pageObj.utils().logit("info", "== API v1 User signup with valid data ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response guestSignupResponse = pageObj.endpoints().apiV1UserSignup(userEmail, dataSet.get("punchhAppKey"));
		Assert.assertEquals(guestSignupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 user signup");
		boolean isApi1UserSignupSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1UserSignupLoginSchema, guestSignupResponse.asString());
		Assert.assertTrue(isApi1UserSignupSchemaValidated, "API v1 User Signup Schema Validation failed");
		String email = guestSignupResponse.jsonPath().getString("email");
		String userId = guestSignupResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(email, "Email should not be null");
		Assert.assertNotNull(userId, "User ID should not be null");
		Assert.assertEquals(email, userEmail.toLowerCase(), "Email should match");
		String password = prop.getProperty("password");
		pageObj.utils().logit("pass", "API v1 user signup is successful. User ID: " + userId + ", Email: " + email);

		// API V1 User Sign-up with invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 User signup with invalid Punchh App Key ==");
		Response userSignupInvalidAppKeyResponse = pageObj.endpoints().apiV1UserSignup(userEmail, "1");
		Assert.assertEquals(userSignupInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key");
		pageObj.utils().logit("pass", "API v1 user signup with invalid Punchh App Key is unsuccessful. Status: " + userSignupInvalidAppKeyResponse.getStatusCode());

		// API V1 User Sign-up with invalid Email
		pageObj.utils().logit("info", "== API v1 User signup with invalid Email ==");
		Response userSignupInvalidEmailResponse = pageObj.endpoints().apiV1UserSignup("1", dataSet.get("punchhAppKey"));
		Assert.assertEquals(userSignupInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for invalid email");
		boolean isApi1UserSignupInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, userSignupInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1UserSignupInvalidEmailSchemaValidated,
				"API v1 User Signup with Invalid Email Schema Validation failed");
		String userSignupInvalidEmailMsg = userSignupInvalidEmailResponse.jsonPath().getString("errors.email[0]");
		Assert.assertNotNull(userSignupInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(userSignupInvalidEmailMsg, "is invalid", "Message does not match");
		pageObj.utils().logit("pass", "API v1 user signup with invalid Email is unsuccessful. Error: " + userSignupInvalidEmailMsg);

		// API V1 User Sign-up with Missing Email
		pageObj.utils().logit("info", "== API v1 User signup with Missing Email ==");
		Response userSignupMissingEmailResponse = pageObj.endpoints().apiV1UserSignup("", dataSet.get("punchhAppKey"));
		Assert.assertEquals(userSignupMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for missing email");
		boolean isApi1UserSignupMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, userSignupMissingEmailResponse.asString());
		Assert.assertTrue(isApi1UserSignupMissingEmailSchemaValidated,
				"API v1 User Signup with Missing Email Schema Validation failed");
		String userSignupMissingEmailMsg = userSignupMissingEmailResponse.jsonPath().getString("errors.email[0]");
		Assert.assertNotNull(userSignupMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(userSignupMissingEmailMsg, "can't be blank", "Message does not match");
		pageObj.utils().logit("pass", "API v1 user signup with Missing Email is unsuccessful. Error: " + userSignupMissingEmailMsg);

		// API V1 User Sign-up with Existing Email
		pageObj.utils().logit("info", "== API v1 User signup with Existing Email ==");
		Response userSignupExistingEmailResponse = pageObj.endpoints().apiV1UserSignup(userEmail,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(userSignupExistingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for existing email");
		boolean isApi1UserSignupExistingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EmailErrorSchema, userSignupExistingEmailResponse.asString());
		Assert.assertTrue(isApi1UserSignupExistingEmailSchemaValidated,
				"API v1 User Signup with Existing Email Schema Validation failed");
		String userSignupExistingEmailMsg = userSignupExistingEmailResponse.jsonPath().getString("errors.email[0]");
		Assert.assertNotNull(userSignupExistingEmailMsg, "Error message should not be null");
		Assert.assertEquals(userSignupExistingEmailMsg, "has already been taken", "Message does not match");
		pageObj.utils().logit("pass", "API v1 user signup with Existing Email is unsuccessful. Error: " + userSignupExistingEmailMsg);

		// API V1 User Login with Invalid Email
		pageObj.utils().logit("info", "== API v1 User login with Invalid Email ==");
		Response userLoginInvalidEmailResponse = pageObj.endpoints().apiV1UserLogin("1", password,
				dataSet.get("punchhAppKey"), dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(userLoginInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email login");
		boolean isApi1UserLoginInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userLoginInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1UserLoginInvalidEmailSchemaValidated,
				"API v1 User Login with Invalid Email Schema Validation failed");
		String userLoginInvalidEmailMsg = userLoginInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(userLoginInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(userLoginInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 user login with Invalid Email is unsuccessful. Error: " + userLoginInvalidEmailMsg);

		// API V1 User Login with Missing Email
		pageObj.utils().logit("info", "== API v1 User login with Missing Email ==");
		Response userLoginMissingEmailResponse = pageObj.endpoints().apiV1UserLogin("", password,
				dataSet.get("punchhAppKey"), dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(userLoginMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email login");
		boolean isApi1UserLoginMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userLoginMissingEmailResponse.asString());
		Assert.assertTrue(isApi1UserLoginMissingEmailSchemaValidated,
				"API v1 User Login with Missing Email Schema Validation failed");
		String userLoginMissingEmailMsg = userLoginMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(userLoginMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(userLoginMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 user login with Missing Email is unsuccessful. Error: " + userLoginMissingEmailMsg);

		// API V1 User Login with Invalid Password
		pageObj.utils().logit("info", "== API v1 User login with Invalid Password ==");
		Response userLoginInvalidPasswordResponse = pageObj.endpoints().apiV1UserLogin(userEmail, "1",
				dataSet.get("punchhAppKey"), dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(userLoginInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password login");
		boolean isApi1UserLoginInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userLoginInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1UserLoginInvalidPasswordSchemaValidated,
				"API v1 User Login with Invalid Password Schema Validation failed");
		String userLoginInvalidPasswordMsg = userLoginInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(userLoginInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(userLoginInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 user login with Invalid Password is unsuccessful. Error: " + userLoginInvalidPasswordMsg);

		// API V1 User Login with Missing Password
		pageObj.utils().logit("info", "== API v1 User login with Missing Password ==");
		Response userLoginMissingPasswordResponse = pageObj.endpoints().apiV1UserLogin(userEmail, "",
				dataSet.get("punchhAppKey"), dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(userLoginMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password login");
		boolean isApi1UserLoginMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userLoginMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1UserLoginMissingPasswordSchemaValidated,
				"API v1 User Login with Missing Password Schema Validation failed");
		String userLoginMissingPasswordMsg = userLoginMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(userLoginMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(userLoginMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 user login with Missing Password is unsuccessful. Error: " + userLoginMissingPasswordMsg);

		// API V1 User Login with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 User login with Invalid Punchh App Key ==");
		Response userLoginInvalidAppKeyResponse = pageObj.endpoints().apiV1UserLogin(userEmail, password, "1",
				dataSet.get("punchhAppDeviceid"));
		Assert.assertEquals(userLoginInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key login");
		pageObj.utils().logit("pass", "API v1 user login with Invalid Punchh App Key is unsuccessful. Status: " + userLoginInvalidAppKeyResponse.getStatusCode());

		// API V1 Checkin with Invalid Email
		pageObj.utils().logit("info", "== API v1 Checkin with Invalid Email ==");
		Response checkinInvalidEmailResponse = pageObj.endpoints().checkinsApiV1("1", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email checkin");
		boolean isApi1CheckinInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1CheckinInvalidEmailSchemaValidated,
				"API v1 Checkin with Invalid Email Schema Validation failed");
		String checkinInvalidEmailMsg = checkinInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(checkinInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin with Invalid Email is unsuccessful. Error: " + checkinInvalidEmailMsg);

		// API V1 Checkin with Missing Email
		pageObj.utils().logit("info", "== API v1 Checkin with Missing Email ==");
		Response checkinMissingEmailResponse = pageObj.endpoints().checkinsApiV1("", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email checkin");
		boolean isApi1CheckinMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinMissingEmailResponse.asString());
		Assert.assertTrue(isApi1CheckinMissingEmailSchemaValidated,
				"API v1 Checkin with Missing Email Schema Validation failed");
		String checkinMissingEmailMsg = checkinMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(checkinMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin with Missing Email is unsuccessful. Error: " + checkinMissingEmailMsg);

		// API V1 Checkin with Invalid Password
		pageObj.utils().logit("info", "== API v1 Checkin with Invalid Password ==");
		Response checkinInvalidPasswordResponse = pageObj.endpoints().checkinsApiV1(userEmail, "1",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password checkin");
		boolean isApi1CheckinInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1CheckinInvalidPasswordSchemaValidated,
				"API v1 Checkin with Invalid Password Schema Validation failed");
		String checkinInvalidPasswordMsg = checkinInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(checkinInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin with Invalid Password is unsuccessful. Error: " + checkinInvalidPasswordMsg);

		// API V1 Checkin with Missing Password
		pageObj.utils().logit("info", "== API v1 Checkin with Missing Password ==");
		Response checkinMissingPasswordResponse = pageObj.endpoints().checkinsApiV1(userEmail, "",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password checkin");
		boolean isApi1CheckinMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1CheckinMissingPasswordSchemaValidated,
				"API v1 Checkin with Missing Password Schema Validation failed");
		String checkinMissingPasswordMsg = checkinMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(checkinMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin with Missing Password is unsuccessful. Error: " + checkinMissingPasswordMsg);

		// API V1 Checkin with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 Checkin with Invalid Punchh App Key ==");
		Response checkinInvalidAppKeyResponse = pageObj.endpoints().checkinsApiV1(userEmail, password, "1");
		Assert.assertEquals(checkinInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key checkin");
		pageObj.utils().logit("pass", "API v1 checkin with Invalid Punchh App Key is unsuccessful. Status: " + checkinInvalidAppKeyResponse.getStatusCode());

		// API V1 User Balance with Invalid Email
		pageObj.utils().logit("info", "== API v1 User Balance with Invalid Email ==");
		Response balanceInvalidEmailResponse = pageObj.endpoints().apiV1UserBalance("1", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(balanceInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email balance");
		boolean isApi1BalanceInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, balanceInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1BalanceInvalidEmailSchemaValidated,
				"API v1 Balance with Invalid Email Schema Validation failed");
		String balanceInvalidEmailMsg = balanceInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(balanceInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(balanceInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 balance with Invalid Email is unsuccessful. Error: " + balanceInvalidEmailMsg);

		// API V1 User Balance with Missing Email
		pageObj.utils().logit("info", "== API v1 User Balance with Missing Email ==");
		Response balanceMissingEmailResponse = pageObj.endpoints().apiV1UserBalance("", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(balanceMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email balance");
		boolean isApi1BalanceMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, balanceMissingEmailResponse.asString());
		Assert.assertTrue(isApi1BalanceMissingEmailSchemaValidated,
				"API v1 Balance with Missing Email Schema Validation failed");
		String balanceMissingEmailMsg = balanceMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(balanceMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(balanceMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 balance with Missing Email is unsuccessful. Error: " + balanceMissingEmailMsg);

		// API V1 User Balance with Invalid Password
		pageObj.utils().logit("info", "== API v1 User Balance with Invalid Password ==");
		Response balanceInvalidPasswordResponse = pageObj.endpoints().apiV1UserBalance(userEmail, "1",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(balanceInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password balance");
		boolean isApi1BalanceInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, balanceInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1BalanceInvalidPasswordSchemaValidated,
				"API v1 Balance with Invalid Password Schema Validation failed");
		String balanceInvalidPasswordMsg = balanceInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(balanceInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(balanceInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 balance with Invalid Password is unsuccessful. Error: " + balanceInvalidPasswordMsg);

		// API V1 User Balance with Missing Password
		pageObj.utils().logit("info", "== API v1 User Balance with Missing Password ==");
		Response balanceMissingPasswordResponse = pageObj.endpoints().apiV1UserBalance(userEmail, "",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(balanceMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password balance");
		boolean isApi1BalanceMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, balanceMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1BalanceMissingPasswordSchemaValidated,
				"API v1 Balance with Missing Password Schema Validation failed");
		String balanceMissingPasswordMsg = balanceMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(balanceMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(balanceMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 balance with Missing Password is unsuccessful. Error: " + balanceMissingPasswordMsg);

		// API V1 User Balance with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 User Balance with Invalid Punchh App Key ==");
		Response balanceInvalidAppKeyResponse = pageObj.endpoints().apiV1UserBalance(userEmail, password, "1");
		Assert.assertEquals(balanceInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key balance");
		pageObj.utils().logit("pass", "API v1 balance with Invalid Punchh App Key is unsuccessful. Status: " + balanceInvalidAppKeyResponse.getStatusCode());

		// API V1 Checkin Balance with Invalid Email
		pageObj.utils().logit("info", "== API v1 Checkin Balance with Invalid Email ==");
		Response checkinsBalanceApiV1Response = pageObj.endpoints().checkinsBalanceApiV1("1", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinsBalanceApiV1Response.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email checkin balance");
		boolean isApi1CheckinBalanceInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinsBalanceApiV1Response.asString());
		Assert.assertTrue(isApi1CheckinBalanceInvalidEmailSchemaValidated,
				"API v1 Checkin Balance with Invalid Email Schema Validation failed");
		String checkinBalanceInvalidEmailMsg = checkinsBalanceApiV1Response.jsonPath().getString("error");
		Assert.assertNotNull(checkinBalanceInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(checkinBalanceInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin balance with Invalid Email is unsuccessful. Error: " + checkinBalanceInvalidEmailMsg);

		// API V1 Checkin Balance with Missing Email
		pageObj.utils().logit("info", "== API v1 Checkin Balance with Missing Email ==");
		Response checkinBalanceMissingEmailResponse = pageObj.endpoints().checkinsBalanceApiV1("", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinBalanceMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email checkin balance");
		boolean isApi1CheckinBalanceMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinBalanceMissingEmailResponse.asString());
		Assert.assertTrue(isApi1CheckinBalanceMissingEmailSchemaValidated,
				"API v1 Checkin Balance with Missing Email Schema Validation failed");
		String checkinBalanceMissingEmailMsg = checkinBalanceMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinBalanceMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(checkinBalanceMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin balance with Missing Email is unsuccessful. Error: " + checkinBalanceMissingEmailMsg);

		// API V1 Checkin Balance with Invalid Password
		pageObj.utils().logit("info", "== API v1 Checkin Balance with Invalid Password ==");
		Response checkinBalanceInvalidPasswordResponse = pageObj.endpoints().checkinsBalanceApiV1(userEmail, "1",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinBalanceInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password checkin balance");
		boolean isApi1CheckinBalanceInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinBalanceInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1CheckinBalanceInvalidPasswordSchemaValidated,
				"API v1 Checkin Balance with Invalid Password Schema Validation failed");
		String checkinBalanceInvalidPasswordMsg = checkinBalanceInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinBalanceInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(checkinBalanceInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin balance with Invalid Password is unsuccessful. Error: " + checkinBalanceInvalidPasswordMsg);

		// API V1 Checkin Balance with Missing Password
		pageObj.utils().logit("info", "== API v1 Checkin Balance with Missing Password ==");
		Response checkinBalanceMissingPasswordResponse = pageObj.endpoints().checkinsBalanceApiV1(userEmail, "",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(checkinBalanceMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password checkin balance");
		boolean isApi1CheckinBalanceMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinBalanceMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1CheckinBalanceMissingPasswordSchemaValidated,
				"API v1 Checkin Balance with Missing Password Schema Validation failed");
		String checkinBalanceMissingPasswordMsg = checkinBalanceMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(checkinBalanceMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(checkinBalanceMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 checkin balance with Missing Password is unsuccessful. Error: " + checkinBalanceMissingPasswordMsg);

		// API V1 Checkin Balance with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 Checkin Balance with Invalid Punchh App Key ==");
		Response checkinBalanceInvalidAppKeyResponse = pageObj.endpoints().checkinsBalanceApiV1(userEmail, password,
				"1");
		Assert.assertEquals(checkinBalanceInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key checkin balance");
		pageObj.utils().logit("pass", "API v1 checkin balance with Invalid Punchh App Key is unsuccessful. Status: " + checkinBalanceInvalidAppKeyResponse.getStatusCode());

		// API V1 Get Rich Messages with Invalid Email
		pageObj.utils().logit("info", "== API v1 Get Rich Messages with Invalid Email ==");
		Response getRichMessagesInvalidEmailResponse = pageObj.endpoints().apiV1GetRichMessages("1", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(getRichMessagesInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email rich messages");
		boolean isApi1GetRichMessagesInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getRichMessagesInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1GetRichMessagesInvalidEmailSchemaValidated,
				"API v1 Get Rich Messages with Invalid Email Schema Validation failed");
		String getRichMessagesInvalidEmailMsg = getRichMessagesInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(getRichMessagesInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(getRichMessagesInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Get Rich Messages with Invalid Email is unsuccessful. Error: " + getRichMessagesInvalidEmailMsg);

		// API V1 Get Rich Messages with Missing Email
		pageObj.utils().logit("info", "== API v1 Get Rich Messages with Missing Email ==");
		Response getRichMessagesMissingEmailResponse = pageObj.endpoints().apiV1GetRichMessages("", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(getRichMessagesMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email rich messages");
		boolean isApi1GetRichMessagesMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getRichMessagesMissingEmailResponse.asString());
		Assert.assertTrue(isApi1GetRichMessagesMissingEmailSchemaValidated,
				"API v1 Get Rich Messages with Missing Email Schema Validation failed");
		String getRichMessagesMissingEmailMsg = getRichMessagesMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(getRichMessagesMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(getRichMessagesMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Get Rich Messages with Missing Email is unsuccessful. Error: " + getRichMessagesMissingEmailMsg);

		// API V1 Get Rich Messages with Invalid Password
		pageObj.utils().logit("info", "== API v1 Get Rich Messages with Invalid Password ==");
		Response getRichMessagesInvalidPasswordResponse = pageObj.endpoints().apiV1GetRichMessages(userEmail, "1",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(getRichMessagesInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password rich messages");
		boolean isApi1GetRichMessagesInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getRichMessagesInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1GetRichMessagesInvalidPasswordSchemaValidated,
				"API v1 Get Rich Messages with Invalid Password Schema Validation failed");
		String getRichMessagesInvalidPasswordMsg = getRichMessagesInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(getRichMessagesInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(getRichMessagesInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Get Rich Messages with Invalid Password is unsuccessful. Error: " + getRichMessagesInvalidPasswordMsg);

		// API V1 Get Rich Messages with Missing Password
		pageObj.utils().logit("info", "== API v1 Get Rich Messages with Missing Password ==");
		Response getRichMessagesMissingPasswordResponse = pageObj.endpoints().apiV1GetRichMessages(userEmail, "",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(getRichMessagesMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password rich messages");
		boolean isApi1GetRichMessagesMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getRichMessagesMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1GetRichMessagesMissingPasswordSchemaValidated,
				"API v1 Get Rich Messages with Missing Password Schema Validation failed");
		String getRichMessagesMissingPasswordMsg = getRichMessagesMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(getRichMessagesMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(getRichMessagesMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Get Rich Messages with Missing Password is unsuccessful. Error: " + getRichMessagesMissingPasswordMsg);

		// API V1 Get Rich Messages with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API v1 Get Rich Messages with Invalid Punchh App Key ==");
		Response getRichMessagesInvalidAppKeyResponse = pageObj.endpoints().apiV1GetRichMessages(userEmail, password,
				"1");
		Assert.assertEquals(getRichMessagesInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key rich messages");
		pageObj.utils().logit("pass", "API v1 Get Rich Messages with Invalid Punchh App Key is unsuccessful. Status: " + getRichMessagesInvalidAppKeyResponse.getStatusCode());

		// Platform Functions API Purchase Subscription
		pageObj.utils().logit("info", "== Platform Functions API Purchase Subscription ==");
		String purchasePrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		Response purchaseSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionPurchase(dataSet.get("apiKey"),
				dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"), dataSet.get("secret"), purchasePrice,
				endDateTime, "false", userId);
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for purchase subscription");
		String subscriptionId = purchaseSubscriptionResponse.jsonPath().getString("subscription_id");
		Assert.assertNotNull(subscriptionId, "Subscription ID should not be null");
		pageObj.utils().logit("pass", "Platform Functions API Purchase Subscription is successful with Subscription ID: " + subscriptionId);

		// API V1 Redemption with Invalid Email
		pageObj.utils().logit("info", "== API V1 Redemption with Invalid Email ==");
		Response redemptionInvalidEmailResponse = pageObj.endpoints().apiV1Redemption("1", subscriptionId,
				dataSet.get("punchhAppKey"), password);
		Assert.assertEquals(redemptionInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email redemption");
		boolean isApi1RedemptionInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1RedemptionInvalidEmailSchemaValidated,
				"API v1 Redemption with Invalid Email Schema Validation failed");
		String redemptionInvalidEmailMsg = redemptionInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(redemptionInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Invalid Email is unsuccessful. Error: " + redemptionInvalidEmailMsg);

		// API V1 Redemption with Missing Email
		pageObj.utils().logit("info", "== API V1 Redemption with Missing Email ==");
		Response redemptionMissingEmailResponse = pageObj.endpoints().apiV1Redemption("", subscriptionId,
				dataSet.get("punchhAppKey"), password);
		Assert.assertEquals(redemptionMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email redemption");
		boolean isApi1RedemptionMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionMissingEmailResponse.asString());
		Assert.assertTrue(isApi1RedemptionMissingEmailSchemaValidated,
				"API v1 Redemption with Missing Email Schema Validation failed");
		String redemptionMissingEmailMsg = redemptionMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(redemptionMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Missing Email is unsuccessful. Error: " + redemptionMissingEmailMsg);

		// API V1 Redemption with Invalid Password
		pageObj.utils().logit("info", "== API V1 Redemption with Invalid Password ==");
		Response redemptionInvalidPasswordResponse = pageObj.endpoints().apiV1Redemption(email, subscriptionId,
				dataSet.get("punchhAppKey"), "1");
		Assert.assertEquals(redemptionInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password redemption");
		boolean isApi1RedemptionInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionInvalidPasswordResponse.asString());
		Assert.assertTrue(isApi1RedemptionInvalidPasswordSchemaValidated,
				"API v1 Redemption with Invalid Password Schema Validation failed");
		String redemptionInvalidPasswordMsg = redemptionInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(redemptionInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Invalid Password is unsuccessful. Error: " + redemptionInvalidPasswordMsg);

		// API V1 Redemption with Missing Password
		pageObj.utils().logit("info", "== API V1 Redemption with Missing Password ==");
		Response redemptionMissingPasswordResponse = pageObj.endpoints().apiV1Redemption(email, subscriptionId,
				dataSet.get("punchhAppKey"), "");
		Assert.assertEquals(redemptionMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password redemption");
		boolean isApi1RedemptionMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionMissingPasswordResponse.asString());
		Assert.assertTrue(isApi1RedemptionMissingPasswordSchemaValidated,
				"API v1 Redemption with Missing Password Schema Validation failed");
		String redemptionMissingPasswordMsg = redemptionMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(redemptionMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Missing Password is unsuccessful. Error: " + redemptionMissingPasswordMsg);

		// API V1 Redemption with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API V1 Redemption with Invalid Punchh App Key ==");
		Response redemptionInvalidAppKeyResponse = pageObj.endpoints().apiV1Redemption(email, subscriptionId, "1",
				password);
		Assert.assertEquals(redemptionInvalidAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key redemption");
		pageObj.utils().logit("pass", "API v1 Redemption with Invalid Punchh App Key is unsuccessful. Status: " + redemptionInvalidAppKeyResponse.getStatusCode());

		// API V1 Redemption with Invalid Subscription ID
		pageObj.utils().logit("info", "== API V1 Redemption with Invalid Subscription ID ==");
		Response redemptionInvalidSubscriptionIDResponse = pageObj.endpoints().apiV1Redemption(email, "1",
				dataSet.get("punchhAppKey"), password);
		Assert.assertEquals(redemptionInvalidSubscriptionIDResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for invalid subscription ID redemption");
		boolean isApi1RedemptionInvalidSubscriptionIDSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionInvalidSubscriptionIDResponse.asString());
		Assert.assertTrue(isApi1RedemptionInvalidSubscriptionIDSchemaValidated,
				"API v1 Redemption with Invalid Subscription ID Schema Validation failed");
		String redemptionInvalidSubscriptionIDMsg = redemptionInvalidSubscriptionIDResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionInvalidSubscriptionIDMsg, "Error message should not be null");
		Assert.assertEquals(redemptionInvalidSubscriptionIDMsg, "Invalid User Subscription.", "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Invalid Subscription ID is unsuccessful. Error: " + redemptionInvalidSubscriptionIDMsg);

		// API V1 Redemption with Missing Subscription ID
		pageObj.utils().logit("info", "== API V1 Redemption with Missing Subscription ID ==");
		Response redemptionMissingSubscriptionIDResponse = pageObj.endpoints().apiV1Redemption(email, "",
				dataSet.get("punchhAppKey"), password);
		Assert.assertEquals(redemptionMissingSubscriptionIDResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for missing subscription ID redemption");
		boolean isApi1RedemptionMissingSubscriptionIDSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionMissingSubscriptionIDResponse.asString());
		Assert.assertTrue(isApi1RedemptionMissingSubscriptionIDSchemaValidated,
				"API v1 Redemption with Missing Subscription ID Schema Validation failed");
		String redemptionMissingSubscriptionIDMsg = redemptionMissingSubscriptionIDResponse.jsonPath().getString("error");
		Assert.assertNotNull(redemptionMissingSubscriptionIDMsg, "Error message should not be null");
		Assert.assertEquals(redemptionMissingSubscriptionIDMsg,
				"Required parameter missing or the value is empty: subscription_id", "Message does not match");
		pageObj.utils().logit("pass", "API v1 Redemption with Missing Subscription ID is unsuccessful. Error: " + redemptionMissingSubscriptionIDMsg);

		// API V1 Offers with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API V1 Offers with Invalid Punchh App Key ==");
		Response offerInvalidPunchhAppKeyResponse = pageObj.endpoints().apiV1UserOffers("1", userEmail, password);
		Assert.assertEquals(offerInvalidPunchhAppKeyResponse.statusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key offers");
		pageObj.utils().logit("pass", "API v1 Offers with Invalid Punchh App Key is unsuccessful. Status: " + offerInvalidPunchhAppKeyResponse.statusCode());

		// API V1 Offers with Invalid Email
		pageObj.utils().logit("info", "== API V1 Offers with Invalid Email ==");
		Response offerInvalidEmailResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"), "1",
				password);
		Assert.assertEquals(offerInvalidEmailResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email offers");
		boolean isOfferInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, offerInvalidEmailResponse.asString());
		Assert.assertTrue(isOfferInvalidEmailSchemaValidated,
				"API v1 Offers with Invalid Email Schema Validation failed");
		String offerInvalidEmailMsg = offerInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(offerInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(offerInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Offers with Invalid Email is unsuccessful. Error: " + offerInvalidEmailMsg);

		// API V1 Offers with Missing Email
		pageObj.utils().logit("info", "== API V1 Offers with Missing Email ==");
		Response offerMissingEmailResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"), "",
				password);
		Assert.assertEquals(offerMissingEmailResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email offers");
		boolean isOfferMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, offerMissingEmailResponse.asString());
		Assert.assertTrue(isOfferMissingEmailSchemaValidated,
				"API v1 Offers with Missing Email Schema Validation failed");
		String offerMissingEmailMsg = offerMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(offerMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(offerMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Offers with Missing Email is unsuccessful. Error: " + offerMissingEmailMsg);

		// API V1 Offers with Invalid Password
		pageObj.utils().logit("info", "== API V1 Offers with Invalid Password ==");
		Response offerInvalidPasswordResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"),
				userEmail, "1");
		Assert.assertEquals(offerInvalidPasswordResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password offers");
		boolean isOfferInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, offerInvalidPasswordResponse.asString());
		Assert.assertTrue(isOfferInvalidPasswordSchemaValidated,
				"API v1 Offers with Invalid Password Schema Validation failed");
		String offerInvalidPasswordMsg = offerInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(offerInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(offerInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Offers with Invalid Password is unsuccessful. Error: " + offerInvalidPasswordMsg);

		// API V1 Offers with Missing Password
		pageObj.utils().logit("info", "== API V1 Offers with Missing Password ==");
		Response offerMissingPasswordResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"),
				userEmail, "");
		Assert.assertEquals(offerMissingPasswordResponse.statusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password offers");
		boolean isOfferMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, offerMissingPasswordResponse.asString());
		Assert.assertTrue(isOfferMissingPasswordSchemaValidated,
				"API v1 Offers with Missing Password Schema Validation failed");
		String offerMissingPasswordMsg = offerMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(offerMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(offerMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 Offers with Missing Password is unsuccessful. Error: " + offerMissingPasswordMsg);

		// API V1 User Membership Level with Invalid Punchh App Key
		pageObj.utils().logit("info", "== API V1 User Membership Level with Invalid Punchh App Key ==");
		Response membershipLevelInvalidPunchhAppKeyResponse = pageObj.endpoints().userMembershipLevelsApiV1(userEmail,
				password, "1");
		Assert.assertEquals(membershipLevelInvalidPunchhAppKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for invalid app key membership level");
		pageObj.utils().logit("pass", "API v1 User Membership Level with Invalid Punchh App Key is unsuccessful. Status: " + membershipLevelInvalidPunchhAppKeyResponse.getStatusCode());

		// API V1 User Membership Level with Invalid Email
		pageObj.utils().logit("info", "== API V1 User Membership Level with Invalid Email ==");
		Response membershipLevelInvalidEmailResponse = pageObj.endpoints().userMembershipLevelsApiV1("1", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(membershipLevelInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid email membership level");
		boolean isMembershipLevelInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, membershipLevelInvalidEmailResponse.asString());
		Assert.assertTrue(isMembershipLevelInvalidEmailSchemaValidated,
				"API v1 User Membership Level with Invalid Email Schema Validation failed");
		String membershipLevelInvalidEmailMsg = membershipLevelInvalidEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(membershipLevelInvalidEmailMsg, "Error message should not be null");
		Assert.assertEquals(membershipLevelInvalidEmailMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 User Membership Level with Invalid Email is unsuccessful. Error: " + membershipLevelInvalidEmailMsg);

		// API V1 User Membership Level with Missing Email
		pageObj.utils().logit("info", "== API V1 User Membership Level with Missing Email ==");
		Response membershipLevelMissingEmailResponse = pageObj.endpoints().userMembershipLevelsApiV1("", password,
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(membershipLevelMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing email membership level");
		boolean isMembershipLevelMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, membershipLevelMissingEmailResponse.asString());
		Assert.assertTrue(isMembershipLevelMissingEmailSchemaValidated,
				"API v1 User Membership Level with Missing Email Schema Validation failed");
		String membershipLevelMissingEmailMsg = membershipLevelMissingEmailResponse.jsonPath().getString("error");
		Assert.assertNotNull(membershipLevelMissingEmailMsg, "Error message should not be null");
		Assert.assertEquals(membershipLevelMissingEmailMsg, notSignedInMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 User Membership Level with Missing Email is unsuccessful. Error: " + membershipLevelMissingEmailMsg);

		// API V1 User Membership Level with Invalid Password
		pageObj.utils().logit("info", "== API V1 User Membership Level with Invalid Password ==");
		Response membershipLevelInvalidPasswordResponse = pageObj.endpoints().userMembershipLevelsApiV1(userEmail, "1",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(membershipLevelInvalidPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for invalid password membership level");
		boolean isMembershipLevelInvalidPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, membershipLevelInvalidPasswordResponse.asString());
		Assert.assertTrue(isMembershipLevelInvalidPasswordSchemaValidated,
				"API v1 User Membership Level with Invalid Password Schema Validation failed");
		String membershipLevelInvalidPasswordMsg = membershipLevelInvalidPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(membershipLevelInvalidPasswordMsg, "Error message should not be null");
		Assert.assertEquals(membershipLevelInvalidPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 User Membership Level with Invalid Password is unsuccessful. Error: " + membershipLevelInvalidPasswordMsg);

		// API V1 User Membership Level with Missing Password
		pageObj.utils().logit("info", "== API V1 User Membership Level with Missing Password ==");
		Response membershipLevelMissingPasswordResponse = pageObj.endpoints().userMembershipLevelsApiV1(userEmail, "",
				dataSet.get("punchhAppKey"));
		Assert.assertEquals(membershipLevelMissingPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for missing password membership level");
		boolean isMembershipLevelMissingPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, membershipLevelMissingPasswordResponse.asString());
		Assert.assertTrue(isMembershipLevelMissingPasswordSchemaValidated,
				"API v1 User Membership Level with Missing Password Schema Validation failed");
		String membershipLevelMissingPasswordMsg = membershipLevelMissingPasswordResponse.jsonPath().getString("error");
		Assert.assertNotNull(membershipLevelMissingPasswordMsg, "Error message should not be null");
		Assert.assertEquals(membershipLevelMissingPasswordMsg, incorrectInfoMsg, "Message does not match");
		pageObj.utils().logit("pass", "API v1 User Membership Level with Missing Password is unsuccessful. Error: " + membershipLevelMissingPasswordMsg);

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
