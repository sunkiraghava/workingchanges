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

public class AuthApiNegativeTest {
	static Logger logger = LogManager.getLogger(AuthApiNegativeTest.class);
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
	private String env;
	private Utilities utils;
	String blankSpace = "";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		authHeaders = new AuthHeaders();
		utils = new Utilities();
		apiUtils = new ApiUtils();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3036 Negative Scenarios Of Auth Login with email and password Api", groups = "api", priority = 0)
	public void T3036_verifyNegativeScenariosOfAuthLogInApi() {
		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		// login via auth API using Invalid user email
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.jsonPath().getString("error"),
				"Incorrect information submitted. Please retry.");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Auth API user login Incorrect information");
		boolean isLogInInvalidUserEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, loginResponse.asString());
		Assert.assertTrue(isLogInInvalidUserEmailSchemaValidated, "Auth API User Log-in Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth API user login Incorrect information is unsuccessful (expected)");

		// login via auth API using Invalid user Password
		Response loginResponse1 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("invalidPassword"));
		Assert.assertEquals(loginResponse.jsonPath().getString("error"),
				"Incorrect information submitted. Please retry.");
		Assert.assertEquals(loginResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Auth API user login Incorrect information");
		boolean isLogInInvalidUserPasswordSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, loginResponse1.asString());
		Assert.assertTrue(isLogInInvalidUserPasswordSchemaValidated, "Auth API User Log-in Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth API user login Incorrect information is unsuccessful (expected)");

		// login via auth API using Invalid user client
		Response loginResponse2 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("invalidClient"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API user login with Invalid client");
		boolean isLogInInvalidSignatureSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, loginResponse2.asString());
		Assert.assertTrue(isLogInInvalidSignatureSchemaValidated, "Auth API User Log-in Schema Validation failed");
		Assert.assertEquals(loginResponse2.jsonPath().get("[0]"), "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API user login with Invalid client is unsuccessful (expected)");

		// login via auth API using Invalid user secret
		Response loginResponse3 = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API user login with Invalid secret");
		Assert.assertEquals(loginResponse3.jsonPath().get("[0]"), "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API user login with Invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3037 Negative Scenarios Of Auth Api Fetch Account Balance of User", groups = "api", priority = 1)
	public void T3037_verifyNegativeScenariosOfAuthApiFetchAccountBalanceOfUser() {
		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Fetch Account Balance of User with invalid authToken
		String invalidAuthToken = CreateDateTime.getTimeDateString();
		Response fetchAccountBalResponse = pageObj.endpoints().authApiFetchAccountBalance(invalidAuthToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchAccountBalResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(fetchAccountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Auth API Fetch Account Balance of User with Invalid authentication token");
		boolean isFetchAccountBalanceInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchAccountBalResponse.asString());
		Assert.assertTrue(isFetchAccountBalanceInvalidAuthTokenSchemaValidated,
				"Auth API Fetch Account Balance of User Schema Validation failed");
		TestListeners.extentTest.get().pass(
				"Auth API Fetch Account Balance of User with Invalid authentication token is unsuccessful (expected)");

		// Fetch Account Balance of User with invalid Client
		Response fetchAccountBalResponse1 = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(fetchAccountBalResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchAccountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API Fetch Account Balance of User with Invalid Client");
		boolean isFetchAccountBalanceInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchAccountBalResponse1.asString());
		Assert.assertTrue(isFetchAccountBalanceInvalidSignatureSchemaValidated,
				"Auth API Fetch Account Balance of User Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth API Fetch Account Balance of User with Invalid Client is unsuccessful (expected)");

		// Fetch Account Balance of User with invalid Secret
		Response fetchAccountBalResponse2 = pageObj.endpoints().authApiFetchAccountBalance(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(fetchAccountBalResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchAccountBalResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API Fetch Account Balance of User with Invalid Secret");
		TestListeners.extentTest.get()
				.pass("Auth API Fetch Account Balance of User with Invalid Secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3038 Negative Scenarios Of Auth Api Signup with email and password", groups = "api", priority = 2)
	public void T3038_verifyNegativeScenariosOfAuthApiSignup() {
		// Signup with email without any domain or Invalid email
		String userEmail1 = CreateDateTime.getTimeDateString();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.jsonPath().getString("errors"), "[email:[is invalid]]");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid email");
		boolean isSignUpInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authEmailErrorsSchema, signUpResponse.asString());
		Assert.assertTrue(isSignUpInvalidEmailSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid email is unsuccessful (expected)");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// SignUp
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse9 = pageObj.endpoints().authApiSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for Auth API User SignUp with Invalid email");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid email is unsuccessful (expected)");

		// Signup with already user user email
		Response signUpResponse10 = pageObj.endpoints().authApiSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse10.jsonPath().getString("errors"), "[email:[has already been taken]]");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid email");
		boolean isSignUpUsedEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authEmailErrorsSchema, signUpResponse10.asString());
		Assert.assertTrue(isSignUpUsedEmailSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid email is unsuccessful (expected)");
		// Signup with invalid client
		Response signUpResponse1 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("invalidClient"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for Auth API User SignUp with Invalid client");
		boolean isSignUpInvalidSignatureSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, signUpResponse1.asString());
		Assert.assertTrue(isSignUpInvalidSignatureSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid client is unsuccessful (expected)");

		// Signup with invalid secret
		Response signUpResponse2 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(signUpResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API User SignUp with Invalid client");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid client is unsuccessful (expected)");

		String last_name = "last_name" + CreateDateTime.getTimeDateString();
		String first_name = "first_name" + CreateDateTime.getTimeDateString();
		// Signup with blank password
		Response signUpResponse3 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), blankSpace, blankSpace, dataSet.get("birthday"), dataSet.get("anniversary"),
				dataSet.get("signup_channel"), dataSet.get("invite_code"), first_name, last_name);
		/*
		 * Assert.assertEquals(signUpResponse3.jsonPath().getString("errors"),
		 * "[password:[can't be blank, is too short (minimum is 6 characters)]]");
		 */
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid email");
		boolean isSignUpBlankPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authBlankPasswordErrorsSchema, signUpResponse3.asString());
		Assert.assertTrue(isSignUpBlankPasswordSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid email is unsuccessful (expected)");

		// Signup with password less than 6 characters
		Response signUpResponse4 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("minPassword"), dataSet.get("minPassword"), dataSet.get("birthday"),
				blankSpace, dataSet.get("signup_channel"), dataSet.get("invite_code"), first_name, last_name);
		/*
		 * Assert.assertEquals(signUpResponse4.jsonPath().getString("errors"),
		 * "[password:[is too short (minimum is 6 characters)]]");
		 */
		Assert.assertEquals(signUpResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with password less than 6 characters");
		boolean isSignUpMinPasswordSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema3, signUpResponse4.asString());
		Assert.assertTrue(isSignUpMinPasswordSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth Api User SignUp with password less than 6 characters is unsuccessful (expected)");

		// Signup with mismatching password confirmation
		Response signUpResponse5 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("password"), dataSet.get("invalidPassword"), dataSet.get("birthday"),
				dataSet.get("anniversary"), dataSet.get("signup_channel"), dataSet.get("invite_code"), first_name,
				last_name);
		// Assert.assertTrue(signUpResponse5.jsonPath().getString("errors").contains("doesn't
		// match Password"));
		Assert.assertEquals(signUpResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with mismatching password confirmation");
		boolean isSignUpMismatchPasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMismatchPasswordErrorsSchema, signUpResponse5.asString());
		Assert.assertTrue(isSignUpMismatchPasswordSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth Api User SignUp with mismatching password confirmation is unsuccessful (expected)");

		// Signup with Invalid Birthday
		Response signUpResponse6 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("password"), dataSet.get("password"), dataSet.get("invalidBirthday"),
				dataSet.get("anniversary"), dataSet.get("signup_channel"), dataSet.get("invite_code"), first_name,
				last_name);
		/*
		 * Assert.assertEquals(signUpResponse6.jsonPath().getString("errors"),
		 * "[birthday:[is not a date]]");
		 */
		Assert.assertEquals(signUpResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid Birthday");
		boolean isSignUpInvalidBirthdaySchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authBirthdayErrorsSchema, signUpResponse6.asString());
		Assert.assertTrue(isSignUpInvalidBirthdaySchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid Birthday is unsuccessful (expected)");

		// Signup with Invalid SignUp Channel
		Response signUpResponse7 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("password"), dataSet.get("password"), dataSet.get("birthday"),
				dataSet.get("anniversary"), dataSet.get("invalidSignup_channel"), dataSet.get("invite_code"),
				first_name, last_name);
		/*
		 * Assert.assertEquals(signUpResponse7.jsonPath().getString("errors"),
		 * "[signup_channel:[is not included in the list]]");
		 */
		Assert.assertEquals(signUpResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid SignUp Channel");
		boolean isSignUpInvalidSignUpChannelSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authSignUpChannelErrorsSchema, signUpResponse7.asString());
		Assert.assertTrue(isSignUpInvalidSignUpChannelSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth Api User SignUp with Invalid SignUp Channel is unsuccessful (expected)");

		// Signup with Invalid Invite Code
		Response signUpResponse8 = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("password"), dataSet.get("password"), dataSet.get("birthday"),
				dataSet.get("anniversary"), dataSet.get("signup_channel"), dataSet.get("invalidInvite_code"),
				first_name, last_name);
		Assert.assertEquals(signUpResponse8.jsonPath().getString("errors"),
				"[base:[Please check your invite code and try again.]]");
		Assert.assertEquals(signUpResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API User SignUp with Invalid Invite Code");
		boolean isSignUpInvalidInviteCodeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema3, signUpResponse8.asString());
		Assert.assertTrue(isSignUpInvalidInviteCodeSchemaValidated, "Auth API User SignUp Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth Api User SignUp with Invalid Invite Code is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3039 Negative Scenarios Of Auth Api List Available rewards of a given user", groups = "api", priority = 3)
	public void T3039_verifyNegativeScenariosOfAuthApiListAvailableRewardsOfAGivenUser() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// List Available rewards of a given user with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response listAvailableRewardsResponse = pageObj.endpoints().authListAvailableRewards(authToken1,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(listAvailableRewardsResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(listAvailableRewardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Auth API User List Available rewards of a given user with invalid Authentication Token");
		boolean isListAvailableRewardsInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, listAvailableRewardsResponse.asString());
		Assert.assertTrue(isListAvailableRewardsInvalidAuthTokenSchemaValidated,
				"Auth API List Available rewards Schema Validation failed");
		TestListeners.extentTest.get().pass(
				"Auth Api User SignUp List Available rewards of a given user with invalid Authentication Token is unsuccessful (expected)");

		// List Available rewards of a given user with invalid client
		Response listAvailableRewardsResponse1 = pageObj.endpoints().authListAvailableRewards(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(listAvailableRewardsResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(listAvailableRewardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API List Available rewards of a given user with invalid client");
		boolean isListAvailableRewardsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, listAvailableRewardsResponse1.asString());
		Assert.assertTrue(isListAvailableRewardsInvalidSignatureSchemaValidated,
				"Auth API List Available rewards Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth Api List Available rewards of a given user with invalid client is unsuccessful (expected)");

		// List Available rewards of a given user with invalid secret
		Response listAvailableRewardsResponse2 = pageObj.endpoints().authListAvailableRewards(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(listAvailableRewardsResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(listAvailableRewardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API List Available rewards of a given user with invalid secret");
		TestListeners.extentTest.get()
				.pass("Auth Api List Available rewards of a given user with invalid secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3040: Negative scenarios of Auth API Applicable Offers and Fetch Redemption Code (Redemptions 1.0)", groups = "api", priority = 4)
	public void T3040_verifyNegativeScenariosOfAuthApiFetchRedemptionCode() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");

		// Applicable Offers with invalid Authentication Token
		TestListeners.extentTest.get().info("== Applicable Offers with invalid Authentication Token ==");
		logger.info("== Applicable Offers with invalid Authentication Token ==");
		Response applicableOffersInvalidAuthResponse = pageObj.endpoints()
				.authApplicableOffersNew(dataSet.get("client"), dataSet.get("secret"), "101", "101", "1", "web");
		Assert.assertEquals(applicableOffersInvalidAuthResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApplicableOffersInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, applicableOffersInvalidAuthResponse.asString());
		Assert.assertTrue(isApplicableOffersInvalidAuthSchemaValidated,
				"Auth API Applicable Offers with invalid Auth Token Schema Validation failed");
		String applicableOffersInvalidAuthMsg = applicableOffersInvalidAuthResponse.jsonPath().getString("error");
		Assert.assertEquals(applicableOffersInvalidAuthMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get().pass("Auth API Applicable Offers with invalid Auth Token is unsuccessful");
		logger.info("Auth API Applicable Offers with invalid Auth Token is unsuccessful");

		// Applicable Offers with invalid signature
		logger.info("Applicable Offers with invalid signature");
		Response applicableOffersInvalidSignatureResponse = pageObj.endpoints()
				.authApplicableOffersNew(dataSet.get("client"), "1", "101", "101", authToken, "web");
		Assert.assertEquals(applicableOffersInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isApplicableOffersInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, applicableOffersInvalidSignatureResponse.asString());
		Assert.assertTrue(isApplicableOffersInvalidSignatureSchemaValidated,
				"Auth API Applicable Offers with invalid signature Schema Validation failed");
		String applicableOffersInvalidSignatureMsg = applicableOffersInvalidSignatureResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(applicableOffersInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API Applicable Offers with invalid signature is unsuccessful");
		logger.info("Auth API Applicable Offers with invalid signature is unsuccessful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		String BlankSpace = "";
		// Fetch Redemption Code with invalid Client
		Response fetchRedemptionCodeResponse = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), dataSet.get("location_id"), BlankSpace, reward_id);
		Assert.assertEquals(fetchRedemptionCodeResponse.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch Redemption Code with Invalid Client ");
		boolean isFetchRedemptionCodeInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchRedemptionCodeResponse.asString());
		Assert.assertTrue(isFetchRedemptionCodeInvalidSignatureSchemaValidated,
				"Auth API Fetch Redemption Code Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch Redemption Code with Invalid Client is unsuccessful (expected)");

		// Fetch Redemption Code with invalid Secret
		Response fetchRedemptionCodeResponse1 = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"), dataSet.get("location_id"), BlankSpace, reward_id);
		Assert.assertEquals(fetchRedemptionCodeResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchRedemptionCodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch Redemption Code with Invalid Secret ");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch Redemption Code with Invalid Secret is unsuccessful (expected)");

		// Fetch Redemption Code with invalid Authentication Token
		String invalidAuthToken = CreateDateTime.getTimeDateString();
		Response fetchRedemptionCodeResponse4 = pageObj.endpoints().authApiFetchRedemptionCode(invalidAuthToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"), BlankSpace, reward_id);
		Assert.assertEquals(fetchRedemptionCodeResponse4.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(fetchRedemptionCodeResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Fetch Redemption Code with Invalid Authentication Token ");
		boolean isFetchRedemptionCodeInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchRedemptionCodeResponse4.asString());
		Assert.assertTrue(isFetchRedemptionCodeInvalidAuthTokenSchemaValidated,
				"Auth API Fetch Redemption Code Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch Redemption Code with Invalid Authentication Token is unsuccessful (expected)");

		// Fetch Redemption Code for Invalid reward id
		Response fetchRedemptionCodeResponse2 = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"), BlankSpace,
				dataSet.get("invalidReward_id)"));
		Assert.assertEquals(fetchRedemptionCodeResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Fetch Redemption Code with Invalid reward id ");
		boolean isFetchRedemptionCodeInvalidRewardIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchRedemptionCodeResponse2.asString());
		Assert.assertTrue(isFetchRedemptionCodeInvalidRewardIdSchemaValidated,
				"Auth API Fetch Redemption Code Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch Redemption Code with Invalid reward id is unsuccessful (expected)");

		// Fetch Redemption Code for Invalid redeemed_points
		Response fetchRedemptionCodeResponse3 = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"),
				dataSet.get("invalidRedeemed_points"), BlankSpace);
		Assert.assertEquals(fetchRedemptionCodeResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Fetch Redemption Code with Invalid redeemed_points");
		boolean isFetchRedemptionCodeInvalidRedeemedPointsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchRedemptionCodeResponse3.asString());
		Assert.assertTrue(isFetchRedemptionCodeInvalidRedeemedPointsSchemaValidated,
				"Auth API Fetch Redemption Code Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch Redemption Code with Invalid redeemed_points is unsuccessful (expected)");

//		// Fetch Redemption Code for Invalid Location_id
//		Response fetchRedemptionCodeResponse5 = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
//				dataSet.get("client"), dataSet.get("secret"), dataSet.get("invalidLocation_id"),
//				dataSet.get("redeemed_points"), BlankSpace);
//		Assert.assertEquals(fetchRedemptionCodeResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, 
//				"Status code 201 did not matched for auth Api Fetch Redemption Code with Invalid Location_id");
//		TestListeners.extentTest.get()
//				.pass("auth Api Fetch Redemption Code with Invalid Location_id is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3041 Negative Scenarios Of Auth Api Forgot Password", groups = "api", priority = 5)
	public void T3041_verifyNegativeScenariosOfAuthApiForgotPassword() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

//		// forgot password with invalid user
//		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
//		Response forgotPasswordResponse = pageObj.endpoints().authApiForgotPassword(userEmail1, dataSet.get("client"),
//				dataSet.get("secret"));
//		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Fetch Redemption Code with Invalid redeemed_points");
//		TestListeners.extentTest.get()
//				.pass("auth Api Fetch Redemption Code with Invalid redeemed_points is unsuccessful (expected)");

		// forgot password with invalid client
		Response forgotPasswordResponse1 = pageObj.endpoints().authApiForgotPassword(userEmail,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(forgotPasswordResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 422 did not matched for auth Api forgot password with invalid client");
		boolean isForgotPasswordInvalidSigantureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, forgotPasswordResponse1.asString());
		Assert.assertTrue(isForgotPasswordInvalidSigantureSchemaValidated,
				"Auth API Forgot Password Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api forgot password with invalid client is unsuccessful (expected)");

		// forgot password with invalid secret
		Response forgotPasswordResponse2 = pageObj.endpoints().authApiForgotPassword(userEmail, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(forgotPasswordResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(forgotPasswordResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 422 did not matched for auth Api forgot password with invalid secret");
		TestListeners.extentTest.get().pass("auth Api forgot password with invalid secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3042 Negative Scenarios Of Auth Api fetch Updated User Info", groups = "api", priority = 6)
	public void T3042_verifyNegativeScenariosOfAuthApfetchUpdatedUserInfo() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Verify updated user info with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response fetchUserResponse = pageObj.endpoints().authApiFetchUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken1);
		Assert.assertEquals(fetchUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Verify updated user info with invalid Authentication Token");
		TestListeners.extentTest.get()
				.pass("auth Api Verify updated user info with invalid Authentication Token is unsuccessful (expected)");

		// Verify updated user info with invalid client
		Response fetchUserResponse1 = pageObj.endpoints().authApiFetchUserInfo(dataSet.get("invalidClient"),
				dataSet.get("secret"), authToken);
		Assert.assertEquals(fetchUserResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Auth API Fetch User Info with invalid client");
		boolean isFetchUserInfoInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchUserResponse1.asString());
		Assert.assertTrue(isFetchUserInfoInvalidSignatureSchemaValidated,
				"Auth API Fetch User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Auth API Fetch User Info with invalid client is unsuccessful (expected)");

		// Verify updated user info with invalid secret
		Response fetchUserResponse2 = pageObj.endpoints().authApiFetchUserInfo(dataSet.get("client"),
				dataSet.get("invalidSecret"), authToken);
		Assert.assertEquals(fetchUserResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Verify updated user info with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api Verify updated user info with invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3054 Negative Scenarios Of Auth Api Verify updated user info", groups = "api", priority = 7)
	public void T3054_verifyNegativeScenariosOfAuthApiVerifyUpdatedUserInfo() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String blankSpace = "";

		// Update userInfo with no email
		Response updateUserResponse = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken, blankSpace, dataSet.get("updateFName"), dataSet.get("updateLName"));
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Update userInfo with no email");
		boolean isUpdateUserInfoNoEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateUserResponse.asString());
		Assert.assertTrue(isUpdateUserInfoNoEmailSchemaValidated, "Auth API Update User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api Update userInfo with no email is unsuccessful (expected)");

		// Update userInfo with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response updateUserResponse1 = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken1, userEmail, dataSet.get("updateFName"), dataSet.get("updateLName"));
		Assert.assertEquals(updateUserResponse1.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(updateUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Update userInfo with invalid Authentication Token");
		boolean isUpdateUserInfoInvalidAuthTokenSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, updateUserResponse1.asString());
		Assert.assertTrue(isUpdateUserInfoInvalidAuthTokenSchemaValidated,
				"Auth API Update User Info Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Update userInfo with invalid Authentication Token is unsuccessful (expected)");

		// Update userInfo with invalid client
		Response updateUserResponse2 = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("invalidClient"),
				dataSet.get("secret"), authToken1, userEmail, dataSet.get("updateFName"), dataSet.get("updateLName"));
		Assert.assertEquals(updateUserResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(updateUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Update userInfo with invalid client");
		boolean isUpdateUserInfoInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateUserResponse2.asString());
		Assert.assertTrue(isUpdateUserInfoInvalidSignatureSchemaValidated,
				"Auth API Update User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api Update userInfo with invalid client is unsuccessful (expected)");

		// Update userInfo with invalid secret
		Response updateUserResponse3 = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("invalidSecret"), authToken1, userEmail, dataSet.get("updateFName"),
				dataSet.get("updateLName"));
		Assert.assertEquals(updateUserResponse3.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(updateUserResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Update userInfo with invalid secret");
		TestListeners.extentTest.get().pass("auth Api Update userInfo with invalid secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3056 Negative Scenarios Of Auth Api Account History", groups = "api", priority = 8)
	public void T3056_verifyNegativeScenariosOfAuthApiAccountHistory() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// get account history with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api get account history  with invalid Authentication Token");
		boolean isAccountHistoryInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, accountHistoryResponse.asString());
		Assert.assertTrue(isAccountHistoryInvalidAuthTokenSchemaValidated,
				"Auth API Account History Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Apiget account history with invalid Authentication Token is unsuccessful (expected)");

		// get account history with invalid client
		Response accountHistoryResponse1 = pageObj.endpoints().authApiAccountHistory(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api get account history with invalid client");
		boolean isAccountHistoryInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, accountHistoryResponse1.asString());
		Assert.assertTrue(isAccountHistoryInvalidSignatureSchemaValidated,
				"Auth API Account History Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api get account history  with invalid client is unsuccessful (expected)");

		// get account history with invalid secret
		Response accountHistoryResponse2 = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(accountHistoryResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(accountHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api get account history with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api get account history  with invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3057 Negative Scenarios Of Auth Api Change Password", groups = "api", priority = 9)
	public void T3057_verifyNegativeScenariosOfAuthApiChangePassword() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Change Password with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response changePasswordResponse = pageObj.endpoints().authApiChangePassword(authToken1, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("newPassword"));
		Assert.assertEquals(changePasswordResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(changePasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Change Password with invalid Authentication Token");
		boolean isChangePasswordInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, changePasswordResponse.asString());
		Assert.assertTrue(isChangePasswordInvalidAuthTokenSchemaValidated,
				"Auth API Change Password Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Change Password with invalid Authentication Token is unsuccessful (expected)");

		// Change Password with invalid client
		Response changePasswordResponse1 = pageObj.endpoints().authApiChangePassword(authToken,
				dataSet.get("invaliClient"), dataSet.get("secret"), dataSet.get("newPassword"));
		Assert.assertEquals(changePasswordResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(changePasswordResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Change Password with invalid client");
		boolean isChangePasswordInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, changePasswordResponse1.asString());
		Assert.assertTrue(isChangePasswordInvalidSignatureSchemaValidated,
				"Auth API Change Password Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api Change Password with invalid client is unsuccessful (expected)");

		// Change Password with invalid secret
		Response changePasswordResponse2 = pageObj.endpoints().authApiChangePassword(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"), dataSet.get("newPassword"));
		Assert.assertEquals(changePasswordResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(changePasswordResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Change Password with invalid secret");
		TestListeners.extentTest.get().pass("auth Api Change Password with invalid secret is unsuccessful (expected)");

		// Change Password with same password
		Response changePasswordResponse3 = pageObj.endpoints().authApiChangePassword(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("Password"));
		Assert.assertEquals(changePasswordResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for auth Api Change Password with same password");
		boolean isChangePasswordSamePasswordSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, changePasswordResponse3.asString());
		Assert.assertTrue(isChangePasswordSamePasswordSchemaValidated,
				"Auth API Change Password Schema Validation failed");
		String changePasswordNotNewMsg = changePasswordResponse3.jsonPath().getString("[0]");
		Assert.assertEquals(changePasswordNotNewMsg, "Password need to be new and unused.");
		TestListeners.extentTest.get().pass("auth Api Change Password with same password is unsuccessful (expected)");

		// Change Password with less than 6 characters
		Response changePasswordResponse4 = pageObj.endpoints().authApiChangePassword(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("minPassword"));
		Assert.assertEquals(changePasswordResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for auth Api Change Password with less than 6 character");
		boolean isChangePasswordLessThan6CharsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, changePasswordResponse4.asString());
		Assert.assertTrue(isChangePasswordLessThan6CharsSchemaValidated,
				"Auth API Change Password Schema Validation failed");
		String changePasswordTooShortMsg = changePasswordResponse4.jsonPath().getString("[0]");
		Assert.assertEquals(changePasswordTooShortMsg, "Password is too short (minimum is 6 characters)");
		TestListeners.extentTest.get()
				.pass("auth Api Change Password with less than 6 character is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3058 Negative Scenarios Of Auth Api Get reset_password_token of the user", groups = "api", priority = 10)
	public void T3058_verifyNegativeScenariosOfAuthApiGetResetPasswordTokenOfTheUser() {
		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		// Get reset_password_token of the user with invalid user email
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response resetPasswordTokenOfTheUserResponse = pageObj.endpoints()
				.authApiResetPasswordTokenOfTheUser(dataSet.get("client"), dataSet.get("secret"), userEmail1);
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth API Get reset_password_token of the user with invalid user email");
		boolean isResetPasswordTokenInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, resetPasswordTokenOfTheUserResponse.asString());
		Assert.assertTrue(isResetPasswordTokenInvalidEmailSchemaValidated,
				"Auth API Get reset password token Schema Validation failed");
		String resetPasswordTokenInvalidEmailMsg = resetPasswordTokenOfTheUserResponse.jsonPath().getString("[0]");
		Assert.assertEquals(resetPasswordTokenInvalidEmailMsg, "Guest does not exist");
		TestListeners.extentTest.get().pass(
				"Auth Api Get reset_password_token of the user with invalid user email is unsuccessful (expected)");

		// Get reset_password_token of the user with invalid Client
		Response resetPasswordTokenOfTheUserResponse1 = pageObj.endpoints()
				.authApiResetPasswordTokenOfTheUser(dataSet.get("invalidClient"), dataSet.get("secret"), userEmail);
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API Get reset_password_token of the user with invalid Client");
		boolean isResetPasswordTokenInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, resetPasswordTokenOfTheUserResponse1.asString());
		Assert.assertTrue(isResetPasswordTokenInvalidSignatureSchemaValidated,
				"Auth API Get reset password token Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Auth Api Get reset_password_token of the user with invalid Client is unsuccessful (expected)");

		// Get reset_password_token of the user with invalid Secret
		Response resetPasswordTokenOfTheUserResponse2 = pageObj.endpoints()
				.authApiResetPasswordTokenOfTheUser(dataSet.get("client"), dataSet.get("invalidSecret"), userEmail);
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Auth API Get reset_password_token of the user with invalid Secret");
		TestListeners.extentTest.get()
				.pass("Auth Api Get reset_password_token of the user with invalid Secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3059 Negative Scenarios Of Auth Api Estimate Loyalty Points Earning", groups = "api", priority = 11)
	public void T3059_verifyNegativeScenariosOfAuthApiEstimateLoyaltyPointsEarning() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Estimate Loyalty Points Earning with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response estimateLoyaltyPointsEarningResponse = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
				authToken1, dataSet.get("client"), dataSet.get("secret"), dataSet.get("subtotal_amount"));
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Estimate Loyalty Points Earning with invalid Authentication Token");
		boolean isEstimateLoyaltyPointsEarningInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, estimateLoyaltyPointsEarningResponse.asString());
		Assert.assertTrue(isEstimateLoyaltyPointsEarningInvalidTokenSchemaValidated,
				"Auth API Estimate Loyalty Points Earning Schema Validation failed");
		TestListeners.extentTest.get().pass(
				"auth Api Estimate Loyalty Points Earning with invalid Authentication Token is unsuccessful (expected)");

		// Estimate Loyalty Points Earning with invalid client
		Response estimateLoyaltyPointsEarningResponse1 = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
				authToken, dataSet.get("invalidClient"), dataSet.get("secret"), dataSet.get("subtotal_amount"));
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Estimate Loyalty Points Earning with invalid client");
		boolean isEstimateLoyaltyPointsEarningInvalidSignatureSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema,
						estimateLoyaltyPointsEarningResponse1.asString());
		Assert.assertTrue(isEstimateLoyaltyPointsEarningInvalidSignatureSchemaValidated,
				"Auth API Estimate Loyalty Points Earning Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Estimate Loyalty Points Earning with invalid client is unsuccessful (expected)");

		// Estimate Loyalty Points Earning with invalid secret
		Response estimateLoyaltyPointsEarningResponse2 = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
				authToken, dataSet.get("client"), dataSet.get("invalidSecret"), dataSet.get("subtotal_amount"));
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Estimate Loyalty Points Earning with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api Estimate Loyalty Points Earning with invalid secret is unsuccessful (expected)");

//		// Estimate Loyalty Points Earning with invalid subtotal_amount
//		Response estimateLoyaltyPointsEarningResponse3 = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
//				authToken, dataSet.get("client"), dataSet.get("secret"), dataSet.get("invalidSubtotal_amount"));
//		Assert.assertEquals(estimateLoyaltyPointsEarningResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Estimate Loyalty Points Earning with invalid subtotal_amount");
//		TestListeners.extentTest.get().pass(
//				"auth Api Estimate Loyalty Points Earning with invalid subtotal_amount is unsuccessful (expected)");

//		// Estimate Loyalty Points Earning with zero subtotal_amount
//		Response estimateLoyaltyPointsEarningResponse4 = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
//				authToken, dataSet.get("client"), dataSet.get("secret"), dataSet.get("zeroSubtotal_amount"));
//		Assert.assertEquals(estimateLoyaltyPointsEarningResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Estimate Loyalty Points Earning with zero subtotal_amount");
//		TestListeners.extentTest.get()
//				.pass("auth Api Estimate Loyalty Points Earning with zero subtotal_amount is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3060 Negative Scenarios Of Auth Api Fetch User Balance", groups = "api", priority = 12)
	public void T3060_verifyNegativeScenariosOfAuthApiFetchUserBalance() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Fetch User Balance with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response userBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userBalanceResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Fetch User Balance with invalid Authentication Token");
		boolean isFetchUserBalanceInvalidAuthTokenSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, userBalanceResponse.asString());
		Assert.assertTrue(isFetchUserBalanceInvalidAuthTokenSchemaValidated,
				"Auth API Fetch User Balance Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch User Balance with invalid Authentication Token is unsuccessful (expected)");

		// Fetch User Balance with invalid client
		Response userBalanceResponse1 = pageObj.endpoints().authApiFetchUserBalance(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(userBalanceResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch User Balance with invalid client");
		boolean isFetchUserBalanceInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userBalanceResponse1.asString());
		Assert.assertTrue(isFetchUserBalanceInvalidSignatureSchemaValidated,
				"Auth API Fetch User Balance Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch User Balance with invalid client is unsuccessful (expected)");

		// Fetch User Balance with invalid secret
		Response userBalanceResponse2 = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(userBalanceResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userBalanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch User Balance with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch User Balance with invalid secret is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3062 Negative Scenarios Of Auth Api List All Deals", groups = "api", priority = 13)
	public void T3062_verifyNegativeScenariosOfAuthApiListAllDeals() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// list all deals with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(listAuthDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api list all deals with invalid Authentication Token");
		boolean isListAuthDealsInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, listAuthDealsResponse.asString());
		Assert.assertTrue(isListAuthDealsInvalidAuthTokenSchemaValidated,
				"Auth API List All Deals Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api list all deals with invalid Authentication Token is unsuccessful (expected)");

		// list all deals with invalid client
		Response listAuthDealsResponse1 = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("invalidClient"),
				dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(listAuthDealsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api list all deals with invalid client");
		boolean isListAuthDealsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, listAuthDealsResponse1.asString());
		Assert.assertTrue(isListAuthDealsInvalidSignatureSchemaValidated,
				"Auth API List All Deals Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api list all deals with invalid client is unsuccessful (expected)");

		// list all deals with invalid secret
		Response listAuthDealsResponse2 = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(listAuthDealsResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(listAuthDealsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api list all deals with secret");
		TestListeners.extentTest.get().pass("auth Api list all deals with invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3063 Negative Scenarios Of Auth Api Get Deal details and Save Selected Deals", groups = "api", priority = 14)
	public void T3063_verifyNegativeScenariosOfAuthApiSaveSelectedDeals() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 list all deals");
		String redeemable_uuid = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		// Get the deal detail with invalid redeemable uuid
		TestListeners.extentTest.get().info("== Get the deal detail with invalid redeemable uuid ==");
		logger.info("== Get the deal detail with invalid redeemable uuid ==");
		Response getDealDetailInvalidRedeemableUuidResponse = pageObj.endpoints().authApiGetTheDealDetail(authToken,
				dataSet.get("client"), dataSet.get("secret"), "1");
		Assert.assertEquals(getDealDetailInvalidRedeemableUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthGetDealDetailInvalidRedeemableUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				getDealDetailInvalidRedeemableUuidResponse.asString());
		String getDealDetailInvalidRedeemableUuidMsg = getDealDetailInvalidRedeemableUuidResponse.jsonPath()
				.getString("error.message");
		Assert.assertEquals(getDealDetailInvalidRedeemableUuidMsg, "Deal not found.");
		Assert.assertTrue(isAuthGetDealDetailInvalidRedeemableUuidSchemaValidated,
				"Auth API Get the deal detail with invalid redeemable uuid schema validation failed");
		TestListeners.extentTest.get()
				.pass("Auth API Get the deal detail with invalid redeemable uuid is unsuccessful");
		logger.info("Auth API Get the deal detail with invalid redeemable uuid is unsuccessful");

		// Get the deal detail with invalid Authentication Token
		TestListeners.extentTest.get().info("== Get the deal detail with invalid Authentication Token ==");
		logger.info("== Get the deal detail with invalid Authentication Token ==");
		Response getDealDetailInvalidAuthTokenResponse = pageObj.endpoints().authApiGetTheDealDetail("1",
				dataSet.get("client"), dataSet.get("secret"), redeemable_uuid);
		Assert.assertEquals(getDealDetailInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isGetDealDetailInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getDealDetailInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isGetDealDetailInvalidAuthTokenSchemaValidated,
				"Auth API Get the deal detail with invalid Authentication Token schema validation failed");
		String getDealDetailInvalidAuthTokenMsg = getDealDetailInvalidAuthTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(getDealDetailInvalidAuthTokenMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Auth API Get the deal detail with invalid Authentication Token is unsuccessful");
		logger.info("Auth API Get the deal detail with invalid Authentication Token is unsuccessful");

		// Get the deal detail with invalid signature
		TestListeners.extentTest.get().info("== Get the deal detail with invalid signature ==");
		logger.info("== Get the deal detail with invalid signature ==");
		Response getDealDetailInvalidSignatureResponse = pageObj.endpoints().authApiGetTheDealDetail(authToken, "1",
				dataSet.get("secret"), redeemable_uuid);
		Assert.assertEquals(getDealDetailInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isGetDealDetailInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getDealDetailInvalidSignatureResponse.asString());
		Assert.assertTrue(isGetDealDetailInvalidSignatureSchemaValidated,
				"Auth API Get the deal detail with invalid signature schema validation failed");
		String getDealDetailInvalidSignatureMsg = getDealDetailInvalidSignatureResponse.jsonPath().getString("[0]");
		Assert.assertEquals(getDealDetailInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API Get the deal detail with invalid signature is unsuccessful");
		logger.info("Auth API Get the deal detail with invalid signature is unsuccessful");

		// Save Selected Deals with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response postAuthResponse = pageObj.endpoints().authPostDeals(authToken1, dataSet.get("client"),
				dataSet.get("secret"), redeemable_uuid);
		Assert.assertEquals(postAuthResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(postAuthResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Save Selected Deals with invalid Authentication Token");
		boolean isSaveDealsInvalidAuthTokenSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, postAuthResponse.asString());
		Assert.assertTrue(isSaveDealsInvalidAuthTokenSchemaValidated,
				"Auth API Save Selected Deals Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Save Selected Deals with invalid Authentication Token is unsuccessful (expected)");

		// Save Selected Deals with invalid client
		Response postAuthResponse1 = pageObj.endpoints().authPostDeals(authToken, dataSet.get("invalidClient"),
				dataSet.get("secret"), redeemable_uuid);
		Assert.assertEquals(postAuthResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(postAuthResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Save Selected Deals with invalid client");
		boolean isSaveDealsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postAuthResponse1.asString());
		Assert.assertTrue(isSaveDealsInvalidSignatureSchemaValidated,
				"Auth API Save Selected Deals Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Save Selected Deals with invalid client is unsuccessful (expected)");

		// Save Selected Deals with invalid secret
		Response postAuthResponse2 = pageObj.endpoints().authPostDeals(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"), redeemable_uuid);
		Assert.assertEquals(postAuthResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(postAuthResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Save Selected Deals with secret");
		TestListeners.extentTest.get()
				.pass("auth Api Save Selected Deals with invalid secret is unsuccessful (expected)");

		// Save Selected Deals with invalid used Deal RedeemableUuid
		String invalidRedeemable_uuid = CreateDateTime.getTimeDateString();
		Response postAuthResponse3 = pageObj.endpoints().authPostDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"), invalidRedeemable_uuid);
		Assert.assertEquals(postAuthResponse3.jsonPath().getString("error"),
				"[message:Deal not found., code:not_found]");
		Assert.assertEquals(postAuthResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth Api Save Selected Deals with secret");
		boolean isSaveDealsInvalidRedeemableUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, postAuthResponse3.asString());
		Assert.assertTrue(isSaveDealsInvalidRedeemableUuidSchemaValidated,
				"Auth API Save Selected Deals Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Save Selected Deals with invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3064 Negative Scenarios Of Auth Api Estimate Points Earning", groups = "api", priority = 15)
	public void T3064_verifyNegativeScenariosOfAuthApiEstimatePointsEarning() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Estimate Points Earning with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response estimatePointsEarningResponse = pageObj.endpoints().authApiEstimatePointsEarning(authToken1,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("receipt_amount"),
				dataSet.get("subtotal_amount"), dataSet.get("item_amount"));
		Assert.assertEquals(estimatePointsEarningResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(estimatePointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Estimate Points Earning with invalid Authentication Token");
		boolean isEstimatePointsEarningInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, estimatePointsEarningResponse.asString());
		Assert.assertTrue(isEstimatePointsEarningInvalidAuthTokenSchemaValidated,
				"Auth API Estimate Points Earning Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Estimate Points Earning with invalid Authentication Token is unsuccessful (expected)");

		// Estimate Points Earning with invalid client
		Response estimatePointsEarningResponse1 = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), dataSet.get("receipt_amount"),
				dataSet.get("subtotal_amount"), dataSet.get("item_amount"));
		Assert.assertEquals(estimatePointsEarningResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(estimatePointsEarningResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Estimate Points Earning with invalid client");
		boolean isEstimatePointsEarningInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, estimatePointsEarningResponse1.asString());
		Assert.assertTrue(isEstimatePointsEarningInvalidSignatureSchemaValidated,
				"Auth API Estimate Points Earning Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Estimate Points Earning with invalid client is unsuccessful (expected)");

		// Estimate Points Earning with invalid secret
		Response estimatePointsEarningResponse2 = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"), dataSet.get("receipt_amount"),
				dataSet.get("subtotal_amount"), dataSet.get("item_amount"));
		Assert.assertEquals(estimatePointsEarningResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(estimatePointsEarningResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Estimate Points Earning with invalid client");
		TestListeners.extentTest.get()
				.pass("auth Api Estimate Points Earning with invalid client is unsuccessful (expected)");

//		// Estimate Points Earning with invalid receipt_amount
//		Response estimatePointsEarningResponse3 = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
//				dataSet.get("client"), dataSet.get("secret"), dataSet.get("invalidReceipt_amount"),
//				dataSet.get("subtotal_amount"), dataSet.get("item_amount"));
//		Assert.assertEquals(estimatePointsEarningResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Estimate Points Earning with invalid receipt_amount");
//		TestListeners.extentTest.get()
//				.pass("auth Api Estimate Points Earning with invalid receipt_amount is unsuccessful (expected)");
//
//		// Estimate Points Earning with invalid subtotal_amount
//		Response estimatePointsEarningResponse4 = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
//				dataSet.get("client"), dataSet.get("secret"), dataSet.get("receipt_amount"),
//				dataSet.get("invalidSubtotal_amount"), dataSet.get("item_amount"));
//		Assert.assertEquals(estimatePointsEarningResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Estimate Points Earning with invalid subtotal_amount");
//		TestListeners.extentTest.get()
//				.pass("auth Api Estimate Points Earning with invalid subtotal_amount is unsuccessful (expected)");
//
//		// Estimate Points Earning with invalid item_amount
//		Response estimatePointsEarningResponse5 = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
//				dataSet.get("client"), dataSet.get("secret"), dataSet.get("receipt_amount"),
//				dataSet.get("subtotal_amount"), dataSet.get("invalidItem_amount"));
//		Assert.assertEquals(estimatePointsEarningResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
//				"Status code 200 did not matched for auth Api Estimate Points Earning with invalid item_amount");
//		TestListeners.extentTest.get()
//				.pass("auth Api Estimate Points Earning with invalid item_amount is unsuccessful (expected)");

	}

	@Test(description = "SQ-T3065 Negative Scenarios Of Auth Api Balance Timelines", groups = "api", priority = 16)
	public void T3065_verifyNegativeScenariosOfAuthApiBalanceTimelines() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Balance Timelines with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response balanceTimelinesResponse = pageObj.endpoints().authApiBalanceTimelines(authToken1,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(balanceTimelinesResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(balanceTimelinesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Balance Timelines with invalid Authentication Token");
		boolean isBalanceTimelinesInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, balanceTimelinesResponse.asString());
		Assert.assertTrue(isBalanceTimelinesInvalidAuthTokenSchemaValidated,
				"Auth API Balance Timelines Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Balance Timelines with invalid Authentication Token is unsuccessful (expected)");

		// Balance Timelines with invalid client
		Response balanceTimelinesResponse1 = pageObj.endpoints().authApiBalanceTimelines(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(balanceTimelinesResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(balanceTimelinesResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Balance Timelines with invalid client");
		boolean isBalanceTimelinesInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, balanceTimelinesResponse1.asString());
		Assert.assertTrue(isBalanceTimelinesInvalidSignatureSchemaValidated,
				"Auth API Balance Timelines Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Balance Timelines with invalid client is unsuccessful (expected)");

		// Balance Timelines with invalid secret
		Response balanceTimelinesResponse2 = pageObj.endpoints().authApiBalanceTimelines(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(balanceTimelinesResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(balanceTimelinesResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Balance Timelines with secret");
		TestListeners.extentTest.get()
				.pass("auth Api Balance Timelines with invalid secret is unsuccessful (expected)");
	}

	@Test(description = "SQ-T3066 Negative Scenarios Of Auth Api User Enrollment", groups = "api", priority = 17)
	public void T3066_verifyNegativeScenariosOfAuthApiUserEnrollment() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// User Enrollment with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response userEnrollmentResponse = pageObj.endpoints().authApiUserEnrollment(authToken1, dataSet.get("client"),
				dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userEnrollmentResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(userEnrollmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api User Enrollment with invalid Authentication Token");
		boolean isUserEnrollmentInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userEnrollmentResponse.asString());
		Assert.assertTrue(isUserEnrollmentInvalidAuthTokenSchemaValidated,
				"Auth API User Enrollment Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api User Enrollment with invalid Authentication Token is unsuccessful (expected)");

		// User Enrollment with invalid Client
		Response userEnrollmentResponse1 = pageObj.endpoints().authApiUserEnrollment(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userEnrollmentResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userEnrollmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api User Enrollment with invalid Client");
		boolean isUserEnrollmentInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userEnrollmentResponse1.asString());
		Assert.assertTrue(isUserEnrollmentInvalidSignatureSchemaValidated,
				"Auth API User Enrollment Schema Validation failed");
		TestListeners.extentTest.get().pass("auth Api User Enrollment with invalid Client is unsuccessful (expected)");

		// User Enrollment with invalid secret
		Response userEnrollmentResponse2 = pageObj.endpoints().authApiUserEnrollment(authToken, dataSet.get("client"),
				dataSet.get("invalidSecret"), social_cause_id);
		Assert.assertEquals(userEnrollmentResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userEnrollmentResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api User Enrollment with invalid secret");
		TestListeners.extentTest.get().pass("auth Api User Enrollment with invalid secret is unsuccessful (expected)");

		// User Enrollment with invalid social_cause_id
		String invalidSocial_cause_id = CreateDateTime.getTimeDateString();
		Response userEnrollmentResponse3 = pageObj.endpoints().authApiUserEnrollment(authToken, dataSet.get("client"),
				dataSet.get("secret"), invalidSocial_cause_id);
		Assert.assertEquals(userEnrollmentResponse3.jsonPath().getString("error"),
				"[message:Feature does not exists., code:feature_not_found]");
		Assert.assertEquals(userEnrollmentResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for auth Api User Enrollment with invalid social_cause_id");
		boolean isUserEnrollmentInvalidSocialCauseIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, userEnrollmentResponse3.asString());
		Assert.assertTrue(isUserEnrollmentInvalidSocialCauseIdSchemaValidated,
				"Auth API User Enrollment Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api User Enrollment with invalid social_cause_id is unsuccessful (expected)");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

	}

	@Test(description = "SQ-T3067 Negative Scenarios Of Auth Api User Disenrollment", groups = "api", priority = 18)
	public void T3067_verifyNegativeScenariosOfAuthApiUserDisenrollment() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// User Disenrollment with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response userDisenrollmentResponse = pageObj.endpoints().authApiUserDisenrollment(authToken1,
				dataSet.get("client"), dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userDisenrollmentResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(userDisenrollmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api User Disenrollment with invalid Authentication Token");
		boolean isUserDisenrollmentInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, userDisenrollmentResponse.asString());
		Assert.assertTrue(isUserDisenrollmentInvalidAuthTokenSchemaValidated,
				"Auth API User Disenrollment Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api User Disenrollment with invalid Authentication Token is unsuccessful (expected)");

		// User Disenrollment with invalid Client
		Response userDisenrollmentResponse1 = pageObj.endpoints().authApiUserDisenrollment(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userDisenrollmentResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userDisenrollmentResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api User Disenrollment with invalid Client");
		boolean isUserDisenrollmentInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userDisenrollmentResponse1.asString());
		Assert.assertTrue(isUserDisenrollmentInvalidSignatureSchemaValidated,
				"Auth API User Disenrollment Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api User Disenrollment with invalid Client is unsuccessful (expected)");

		// User Disenrollment with invalid secret
		Response userDisenrollmentResponse2 = pageObj.endpoints().authApiUserDisenrollment(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"), social_cause_id);
		Assert.assertEquals(userDisenrollmentResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(userDisenrollmentResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api User Disenrollment with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api User Disenrollment with invalid secret is unsuccessful (expected)");

		// User Disenrollment with invalid social_cause_id
		String invalidSocial_cause_id = CreateDateTime.getTimeDateString();
		Response userDisenrollmentResponse3 = pageObj.endpoints().authApiUserDisenrollment(authToken,
				dataSet.get("client"), dataSet.get("secret"), invalidSocial_cause_id);
		Assert.assertEquals(userDisenrollmentResponse3.jsonPath().getString("error"),
				"[message:Feature does not exists., code:feature_not_found]");
		Assert.assertEquals(userDisenrollmentResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for auth Api User Disenrollment with invalid social_cause_id");
		boolean isUserDisenrollmentInvalidSocialCauseIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, userDisenrollmentResponse3.asString());
		Assert.assertTrue(isUserDisenrollmentInvalidSocialCauseIdSchemaValidated,
				"Auth API User Disenrollment Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api User Disenrollment with invalid social_cause_id is unsuccessful (expected)");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

	}

	@Test(description = "SQ-T3068 Negative Scenarios Of Auth Api Fetch available offers of the user", groups = "api", priority = 19)
	public void T3068_verifyNegativeScenariosOfAuthApiFetchAvailableOffersOfTheUser() {

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Fetch available offers of the user with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response fetchAvailableOffersOfTheUserResponse = pageObj.endpoints()
				.authApiFetchAvailableOffersOfTheUser(authToken1, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api Fetch available offers of the user with invalid Authentication Token");
		boolean isFetchAvailableOffersInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchAvailableOffersOfTheUserResponse.asString());
		Assert.assertTrue(isFetchAvailableOffersInvalidAuthTokenSchemaValidated,
				"Auth API Fetch available offers Schema Validation failed");
		TestListeners.extentTest.get().pass(
				"auth Api Fetch available offers of the user with invalid Authentication Token is unsuccessful (expected)");

		// Fetch available offers of the user with invalid client
		Response fetchAvailableOffersOfTheUserResponse1 = pageObj.endpoints()
				.authApiFetchAvailableOffersOfTheUser(authToken, dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse1.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch available offers of the user with invalid client");
		boolean isFetchAvailableOffersInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchAvailableOffersOfTheUserResponse1.asString());
		Assert.assertTrue(isFetchAvailableOffersInvalidSignatureSchemaValidated,
				"Auth API Fetch available offers Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch available offers of the user with invalid client is unsuccessful (expected)");

		// Fetch available offers of the user with invalid secret
		Response fetchAvailableOffersOfTheUserResponse2 = pageObj.endpoints()
				.authApiFetchAvailableOffersOfTheUser(authToken, dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api Fetch available offers of the user with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api Fetch available offers of the user with invalid secret is unsuccessful (expected)");

	}

	@Test(description = "Verify Auth API Negative Scenarios:- SQ-T5335: Ordering Meta; SQ-T5337: Subscription Meta; "
			+ "SQ-T5341: Cancel Subscription; SQ-T5574: Fetch Active Purchasable Subscription Plans and Subscription Plans for a User", groups = "api")
	public void verifyAuthMetaApiNegative() {

		String invalidValue = "abc123";

		// Auth API User Sign-up
		TestListeners.extentTest.get().info("== Auth API User Sign-up ==");
		logger.info("== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Auth API User Sign-up");
		String accessToken = signUpResponse.jsonPath().get("access_token");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		TestListeners.extentTest.get().pass("Auth API User Sign-up call is successful");
		logger.info("Auth API User Sign-up call is successful");

		// Auth API Ordering Meta with invalid filter
		TestListeners.extentTest.get().info("== Auth API Ordering Meta with invalid filter ==");
		logger.info("== Auth API Ordering Meta with invalid filter ==");
		Response orderingMetaInvalidFilterResponse = pageObj.endpoints().authApiOrderingMeta("", dataSet.get("client"),
				dataSet.get("secret"), accessToken);
		Assert.assertEquals(orderingMetaInvalidFilterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Auth API Ordering Meta with invalid filter");
		boolean isOrderingMetaInvalidFilterSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, orderingMetaInvalidFilterResponse.asString());
		Assert.assertTrue(isOrderingMetaInvalidFilterSchemaValidated,
				"Auth API Ordering Meta Schema Validation failed");
		String orderingMetaInvalidFilterMsg = orderingMetaInvalidFilterResponse.jsonPath().get("error").toString();
		Assert.assertEquals(orderingMetaInvalidFilterMsg, "Required parameter missing or the value is empty: filter",
				"Message did not match.");
		TestListeners.extentTest.get().pass("Auth API Ordering Meta call with invalid filter is unsuccessful");
		logger.info("Auth API Ordering Meta call with invalid filter is unsuccessful");

		// Auth API Ordering Meta with invalid client
		TestListeners.extentTest.get().info("== Auth API Ordering Meta with invalid client ==");
		logger.info("== Auth API Ordering Meta with invalid client ==");
		Response orderingMetaInvalidClientResponse = pageObj.endpoints().authApiOrderingMeta("base_redeemable",
				invalidValue, dataSet.get("secret"), accessToken);
		Assert.assertEquals(orderingMetaInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Auth API Ordering Meta with invalid client");
		boolean isOrderingMetaInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, orderingMetaInvalidClientResponse.asString());
		Assert.assertTrue(isOrderingMetaInvalidSignatureSchemaValidated,
				"Auth API Ordering Meta Schema Validation failed");
		String orderingMetaInvalidClientMsg = orderingMetaInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(orderingMetaInvalidClientMsg, "Invalid Signature", "Message did not match.");
		TestListeners.extentTest.get().pass("Auth API Ordering Meta call with invalid client is unsuccessful");
		logger.info("Auth API Ordering Meta call with invalid client is unsuccessful");

		// Auth API Ordering Meta with invalid secret
		TestListeners.extentTest.get().info("== Auth API Ordering Meta with invalid secret ==");
		logger.info("== Auth API Ordering Meta with invalid secret ==");
		Response orderingMetaInvalidSecretResponse = pageObj.endpoints().authApiOrderingMeta("base_redeemable",
				dataSet.get("client"), invalidValue, accessToken);
		Assert.assertEquals(orderingMetaInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Auth API Ordering Meta with invalid secret");
		String orderingMetaInvalidSecretMsg = orderingMetaInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(orderingMetaInvalidSecretMsg, "Invalid Signature", "Message did not match.");
		TestListeners.extentTest.get().pass("Auth API Ordering Meta call with invalid secret is unsuccessful");
		logger.info("Auth API Ordering Meta call with invalid secret is unsuccessful");

		if (!(env.equalsIgnoreCase("qa"))) {
			// Auth API Subscription Meta with feature turned off (using different business)
			TestListeners.extentTest.get().info("== Auth API Subscription Meta with feature turned off ==");
			logger.info("== Auth API Subscription Meta with feature turned off ==");
			Response subscriptionMetaFeatureOffResponse = pageObj.endpoints()
					.authSubscriptionMeta(dataSet.get("client1"), dataSet.get("secret1"));
			Assert.assertEquals(subscriptionMetaFeatureOffResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
					"Status code 422 did not match for Auth API Subscription Meta with feature turned off");
			boolean isSubscriptionMetaFeatureOffSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.authSubscriptionErrorSchema, subscriptionMetaFeatureOffResponse.asString());
			Assert.assertTrue(isSubscriptionMetaFeatureOffSchemaValidated,
					"Auth API Subscription Meta Schema Validation failed");
			String subscriptionMetaFeatureOffMsg = subscriptionMetaFeatureOffResponse.jsonPath().get("error.message[0]")
					.toString();
			String featureOffMsg = "Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.";
			Assert.assertEquals(subscriptionMetaFeatureOffMsg, featureOffMsg, "Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Subscription Meta call with feature turned off is unsuccessful");
			logger.info("Auth API Subscription Meta call with feature turned off is unsuccessful");

			// Auth API Subscription Meta with invalid client
			TestListeners.extentTest.get().info("== Auth API Subscription Meta with invalid client ==");
			logger.info("== Auth API Subscription Meta with invalid client ==");
			Response subscriptionMetaInvalidClientResponse = pageObj.endpoints().authSubscriptionMeta(invalidValue,
					dataSet.get("secret"));
			Assert.assertEquals(subscriptionMetaInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
					"Status code 412 did not match for Auth API Subscription Meta with invalid client");
			boolean isSubscriptionMetaInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
					ApiResponseJsonSchema.apiStringArraySchema, subscriptionMetaInvalidClientResponse.asString());
			Assert.assertTrue(isSubscriptionMetaInvalidSignatureSchemaValidated,
					"Auth API Subscription Meta Schema Validation failed");
			String subscriptionMetaInvalidClientMsg = subscriptionMetaInvalidClientResponse.jsonPath().get("[0]")
					.toString();
			Assert.assertEquals(subscriptionMetaInvalidClientMsg, "Invalid Signature", "Message did not match.");
			TestListeners.extentTest.get().pass("Auth API Subscription Meta call with invalid client is unsuccessful");
			logger.info("Auth API Subscription Meta call with invalid client is unsuccessful");

			// Auth API Subscription Meta with invalid secret
			TestListeners.extentTest.get().info("== Auth API Subscription Meta with invalid secret ==");
			logger.info("== Auth API Subscription Meta with invalid secret ==");
			Response subscriptionMetaInvalidSecretResponse = pageObj.endpoints()
					.authSubscriptionMeta(dataSet.get("client"), invalidValue);
			Assert.assertEquals(subscriptionMetaInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
					"Status code 412 did not match for Auth API Subscription Meta with invalid secret");
			String subscriptionMetaInvalidSecretMsg = subscriptionMetaInvalidSecretResponse.jsonPath().get("[0]")
					.toString();
			Assert.assertEquals(subscriptionMetaInvalidSecretMsg, "Invalid Signature", "Message did not match.");
			TestListeners.extentTest.get().pass("Auth API Subscription Meta call with invalid secret is unsuccessful");
			logger.info("Auth API Subscription Meta call with invalid secret is unsuccessful");

			// Auth API Subscription Meta with valid details
			TestListeners.extentTest.get().info("== Auth API Subscription Meta with valid details ==");
			logger.info("== Auth API Subscription Meta with valid details ==");
			Response subscriptionMetaResponse = pageObj.endpoints().authSubscriptionMeta(dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(subscriptionMetaResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match for Auth API Subscription Meta with valid details");
			String cancellationReasonId = utils.getJsonReponseKeyValueFromJsonArray(subscriptionMetaResponse,
					"subscription_cancellation_reasons", "cancellation_reason_id", "38");
			TestListeners.extentTest.get().pass("Auth API Subscription Meta call with valid details is successful");
			logger.info("Auth API Subscription Meta call with valid details is successful");

			// Auth API Fetch Subscription Plans for User with invalid signature
			TestListeners.extentTest.get().info("== Auth Fetch Subscription Plans for User with invalid signature ==");
			logger.info("== Auth Fetch Subscription Plans for User with invalid signature ==");
			Response fetchUserSubscriptionPlansInvalidSignatureResponse = pageObj.endpoints()
					.AuthUserSubscriptionWithPastSubscription(accessToken, invalidValue, dataSet.get("secret"),
							"past_subscriptions");
			Assert.assertEquals(fetchUserSubscriptionPlansInvalidSignatureResponse.statusCode(), 412);
			boolean isFetchUserSubscriptionPlansInvalidSignatureResponseSchemaValidated = Utilities
					.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema,
							fetchUserSubscriptionPlansInvalidSignatureResponse.asString());
			Assert.assertTrue(isFetchUserSubscriptionPlansInvalidSignatureResponseSchemaValidated,
					"Auth Fetch Subscription Plans for User schema validation failed");
			String fetchUserSubscriptionPlansInvalidSignatureMsg = fetchUserSubscriptionPlansInvalidSignatureResponse
					.jsonPath().getString("[0]");
			Assert.assertEquals(fetchUserSubscriptionPlansInvalidSignatureMsg, "Invalid Signature");
			TestListeners.extentTest.get()
					.pass("Auth Fetch Subscription Plans for User call with invalid signature is unsuccessful");
			logger.info("Auth Fetch Subscription Plans for User call with invalid signature is unsuccessful");

			// Auth API Fetch Subscription Plans for User with invalid filter
			TestListeners.extentTest.get().info("== Auth Fetch Subscription Plans for User with invalid filter ==");
			logger.info("== Auth Fetch Subscription Plans for User with invalid filter ==");
			Response fetchUserSubscriptionPlansInvalidFilterResponse = pageObj.endpoints()
					.AuthUserSubscriptionWithPastSubscription(accessToken, dataSet.get("client"), dataSet.get("secret"),
							"");
			Assert.assertEquals(fetchUserSubscriptionPlansInvalidFilterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
			boolean isFetchUserSubscriptionPlansInvalidFilterResponseSchemaValidated = Utilities
					.validateJsonAgainstSchema(ApiResponseJsonSchema.authSubscriptionErrorSchema,
							fetchUserSubscriptionPlansInvalidFilterResponse.asString());
			Assert.assertTrue(isFetchUserSubscriptionPlansInvalidFilterResponseSchemaValidated,
					"Auth Fetch Subscription Plans for User schema validation failed");
			String fetchUserSubscriptionPlansInvalidFilterMsg = fetchUserSubscriptionPlansInvalidFilterResponse
					.jsonPath().getString("error.message[0]");
			Assert.assertEquals(fetchUserSubscriptionPlansInvalidFilterMsg,
					"Possible values are active, expired, cancelled.");
			TestListeners.extentTest.get()
					.pass("Auth Fetch Subscription Plans for User call with invalid filter is unsuccessful");
			logger.info("Auth Fetch Subscription Plans for User call with invalid filter is unsuccessful");

			// Auth API Fetch Active Purchasable Subscription Plans
			TestListeners.extentTest.get()
					.info("== Auth API Fetch Active Purchasable Subscription Plans with invalid signature ==");
			logger.info("== Auth API Fetch Active Purchasable Subscription Plans with invalid signature ==");
			Response activePurchasableSubscriptionPlansInvalidSignatureResponse = pageObj.endpoints()
					.authFetchActivePurchasableSubscriptionPlan("", dataSet.get("secret"));
			Assert.assertEquals(activePurchasableSubscriptionPlansInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
			boolean isActivePurchasableSubscriptionPlansInvalidSignatureSchemaValidated = Utilities
					.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema,
							activePurchasableSubscriptionPlansInvalidSignatureResponse.asString());
			Assert.assertTrue(isActivePurchasableSubscriptionPlansInvalidSignatureSchemaValidated,
					"Auth API Fetch Active Purchasable Subscription Plans Schema Validation failed");
			String activePurchasableSubscriptionPlansInvalidSignatureMsg = activePurchasableSubscriptionPlansInvalidSignatureResponse
					.jsonPath().getString("[0]");
			Assert.assertEquals(activePurchasableSubscriptionPlansInvalidSignatureMsg, "Invalid Signature");
			TestListeners.extentTest.get().pass(
					"Auth API Fetch Active Purchasable Subscription Plans call with invalid signature is unsuccessful");
			logger.info(
					"Auth API Fetch Active Purchasable Subscription Plans call with invalid signature is unsuccessful");

			// Auth API Fetch Active Purchasable Subscription Plans with feature turned off
			// (using different business)
			TestListeners.extentTest.get()
					.info("== Auth API Fetch Active Purchasable Subscription Plans with feature turned off ==");
			logger.info("== Auth API Fetch Active Purchasable Subscription Plans with feature turned off ==");
			Response activePurchasableSubscriptionPlansFeatureOffResponse = pageObj.endpoints()
					.authFetchActivePurchasableSubscriptionPlan(dataSet.get("client1"), dataSet.get("secret1"));
			Assert.assertEquals(activePurchasableSubscriptionPlansFeatureOffResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
			boolean isActivePurchasableSubscriptionPlansFeatureOffSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.authSubscriptionErrorSchema,
					activePurchasableSubscriptionPlansFeatureOffResponse.asString());
			Assert.assertTrue(isActivePurchasableSubscriptionPlansFeatureOffSchemaValidated,
					"Auth API Fetch Active Purchasable Subscription Plans Schema Validation failed");
			String activePurchasableSubscriptionPlansFeatureOffMsg = activePurchasableSubscriptionPlansFeatureOffResponse
					.jsonPath().getString("error.message[0]");
			Assert.assertEquals(activePurchasableSubscriptionPlansFeatureOffMsg,
					"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
			TestListeners.extentTest.get().pass(
					"Auth API Fetch Active Purchasable Subscription Plans call with feature turned off is unsuccessful");
			logger.info(
					"Auth API Fetch Active Purchasable Subscription Plans call with feature turned off is unsuccessful");

			// Auth API Purchase Subscription
			TestListeners.extentTest.get().info("== Auth API Purchase Subscription ==");
			logger.info("== Auth API Purchase Subscription ==");
			String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
			String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
			String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
			Response purchaseSubscriptionResponse = pageObj.endpoints().authApiSubscriptionPurchase(authToken,
					dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice,
					startDateTime, endDateTime, "false");
			Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
					"Status code 201 did not match for Auth API Purchase Subscription");
			String subscriptionId = purchaseSubscriptionResponse.jsonPath().get("subscription_id").toString();
			TestListeners.extentTest.get().pass("Auth API Purchase Subscription call is successful");
			logger.info("Auth API Purchase Subscription call is successful");

			// Auth API Cancel Subscription with invalid cancellation reason ID
			TestListeners.extentTest.get()
					.info("== Auth API Cancel Subscription with invalid cancellation reason ID ==");
			logger.info("== Auth API Cancel Subscription with invalid cancellation reason ID ==");
			Response cancelSubscriptionInvalidReasonIdResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), dataSet.get("secret"), accessToken, subscriptionId, "Price too High.", " ",
					"soft_cancelled");
			Assert.assertEquals(cancelSubscriptionInvalidReasonIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
					"Status code 400 did not match for Auth API Cancel Subscription with invalid cancellation reason ID");
			boolean isCancelSubscriptionInvalidReasonIdSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.apiErrorObjectSchema, cancelSubscriptionInvalidReasonIdResponse.asString());
			Assert.assertTrue(isCancelSubscriptionInvalidReasonIdSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionInvalidReasonIdMsg = cancelSubscriptionInvalidReasonIdResponse.jsonPath()
					.get("error").toString();
			Assert.assertEquals(cancelSubscriptionInvalidReasonIdMsg,
					"Required parameter missing or the value is empty: cancellation_reason_id",
					"Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call with invalid cancellation reason ID is unsuccessful");
			logger.info("Auth API Cancel Subscription call with invalid cancellation reason ID is unsuccessful");

			// Auth API Cancel Subscription with invalid user access token
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription with invalid user access token ==");
			logger.info("== Auth API Cancel Subscription with invalid user access token ==");
			Response cancelSubscriptionInvalidAccessTokenResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), dataSet.get("secret"), invalidValue, subscriptionId, "Price too High.",
					cancellationReasonId, "soft_cancelled");
			Assert.assertEquals(cancelSubscriptionInvalidAccessTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
					"Status code 401 did not match for Auth API Cancel Subscription with invalid user access token");
			boolean isCancelSubscriptionInvalidAccessTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.apiErrorObjectSchema,
					cancelSubscriptionInvalidAccessTokenResponse.asString());
			Assert.assertTrue(isCancelSubscriptionInvalidAccessTokenSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionInvalidAccessTokenMsg = cancelSubscriptionInvalidAccessTokenResponse.jsonPath()
					.get("error").toString();
			Assert.assertEquals(cancelSubscriptionInvalidAccessTokenMsg,
					"You need to sign in or sign up before continuing.", "Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call with invalid user access token is unsuccessful");
			logger.info("Auth API Cancel Subscription call with invalid user access token is unsuccessful");

			// Auth API Cancel Subscription with invalid client
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription with invalid client ==");
			logger.info("== Auth API Cancel Subscription with invalid client ==");
			Response cancelSubscriptionInvalidClientResponse = pageObj.endpoints().authSubscriptionCancel(invalidValue,
					dataSet.get("secret"), accessToken, subscriptionId, "Price too High.", cancellationReasonId,
					"soft_cancelled");
			Assert.assertEquals(cancelSubscriptionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
					"Status code 412 did not match for Auth API Cancel Subscription with invalid client");
			boolean isCancelSubscriptionInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
					ApiResponseJsonSchema.apiStringArraySchema, cancelSubscriptionInvalidClientResponse.asString());
			Assert.assertTrue(isCancelSubscriptionInvalidSignatureSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionInvalidClientMsg = cancelSubscriptionInvalidClientResponse.jsonPath().get("[0]")
					.toString();
			Assert.assertEquals(cancelSubscriptionInvalidClientMsg, "Invalid Signature", "Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call with invalid client is unsuccessful");
			logger.info("Auth API Cancel Subscription call with invalid client is unsuccessful");

			// Auth API Cancel Subscription with invalid secret
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription with invalid secret ==");
			logger.info("== Auth API Cancel Subscription with invalid secret ==");
			Response cancelSubscriptionInvalidSecretResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), invalidValue, accessToken, subscriptionId, "Price too High.",
					cancellationReasonId, "soft_cancelled");
			Assert.assertEquals(cancelSubscriptionInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
					"Status code 412 did not match for Auth API Cancel Subscription with invalid secret");
			String cancelSubscriptionInvalidSecretMsg = cancelSubscriptionInvalidSecretResponse.jsonPath().get("[0]")
					.toString();
			Assert.assertEquals(cancelSubscriptionInvalidSecretMsg, "Invalid Signature", "Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call with invalid secret is unsuccessful");
			logger.info("Auth API Cancel Subscription call with invalid secret is unsuccessful");

			// Auth API Cancel Subscription with invalid subscription ID
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription with invalid subscription ID ==");
			logger.info("== Auth API Cancel Subscription with invalid subscription ID ==");
			Response cancelSubscriptionInvalidSubscriptionIdResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), dataSet.get("secret"), accessToken, "\"\"", "Price too High.",
					cancellationReasonId, "soft_cancelled");
			Assert.assertEquals(cancelSubscriptionInvalidSubscriptionIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
					"Status code 422 did not match for Auth API Cancel Subscription with invalid subscription ID");
			boolean isCancelSubscriptionInvalidSubscriptionIdSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.authSubscriptionErrorSchema,
					cancelSubscriptionInvalidSubscriptionIdResponse.asString());
			Assert.assertTrue(isCancelSubscriptionInvalidSubscriptionIdSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionInvalidSubscriptionIdMsg = cancelSubscriptionInvalidSubscriptionIdResponse
					.jsonPath().get("error.message[0]").toString();
			Assert.assertEquals(cancelSubscriptionInvalidSubscriptionIdMsg, "Invalid User Subscription.",
					"Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call with invalid subscription ID is unsuccessful");
			logger.info("Auth API Cancel Subscription call with invalid subscription ID is unsuccessful");

			// Auth API Cancel Subscription with valid details
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription with valid details ==");
			logger.info("== Auth API Cancel Subscription with valid details ==");
			Response cancelSubscriptionResponse = pageObj.endpoints().authSubscriptionCancel(dataSet.get("client"),
					dataSet.get("secret"), accessToken, subscriptionId, "Price too High.", cancellationReasonId,
					"soft_cancelled");
			Assert.assertEquals(cancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match for Auth API Cancel Subscription");
			String cancelSubscriptionMsg = cancelSubscriptionResponse.jsonPath().get("[0]").toString();
			Assert.assertEquals(cancelSubscriptionMsg, "Subscription auto renewal canceled.", "Message did not match.");
			TestListeners.extentTest.get().pass("Auth API Cancel Subscription call with valid details is successful");
			logger.info("Auth API Cancel Subscription call with valid details is successful");

			// Auth API Cancel Subscription that is already cancelled
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription that is already cancelled ==");
			logger.info("== Auth API Cancel Subscription that is already cancelled ==");
			Response cancelSubscriptionAlreadyCancelledResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), dataSet.get("secret"), accessToken, subscriptionId, "Price too High.",
					cancellationReasonId, "soft_cancelled");
			Assert.assertEquals(cancelSubscriptionAlreadyCancelledResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
					"Status code 422 did not match for Auth API Cancel Subscription that is already cancelled");
			boolean isCancelSubscriptionAlreadyCancelledSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.authSubscriptionErrorSchema,
					cancelSubscriptionAlreadyCancelledResponse.asString());
			Assert.assertTrue(isCancelSubscriptionAlreadyCancelledSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionAlreadyCancelledMsg = cancelSubscriptionAlreadyCancelledResponse.jsonPath()
					.get("error.message[0]").toString();
			Assert.assertEquals(cancelSubscriptionAlreadyCancelledMsg, "Subscription is already canceled.",
					"Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call that is already cancelled is unsuccessful");
			logger.info("Auth API Cancel Subscription call that is already cancelled is unsuccessful");

			// Auth API Purchase Subscription #2 at an expired date
			TestListeners.extentTest.get().info("== Auth API Purchase Subscription #2 at an expired date ==");
			logger.info("== Auth API Purchase Subscription #2 at an expired date ==");
			String spPrice3 = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
			String startDateTime3 = CreateDateTime.getYesterdayDays(1) + " 01:00:00";
			String endDateTime3 = CreateDateTime.getYesterdaysDate() + " 10:00 PM";
			Response purchaseSubscriptionResponse3 = pageObj.endpoints().authApiSubscriptionPurchase(authToken,
					dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice3,
					startDateTime3, endDateTime3, "false");
			Assert.assertEquals(purchaseSubscriptionResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
					"Status code 201 did not match for Auth API Purchase Subscription at an expired date");
			String subscriptionId3 = purchaseSubscriptionResponse3.jsonPath().get("subscription_id").toString();
			TestListeners.extentTest.get()
					.pass("Auth API Purchase Subscription call #2 at an expired date is successful");
			logger.info("Auth API Purchase Subscription call #2 at an expired date is successful");

			// Auth API Cancel Subscription that is already expired
			TestListeners.extentTest.get().info("== Auth API Cancel Subscription that is already expired ==");
			logger.info("== Auth API Cancel Subscription that is already expired ==");
			Response cancelSubscriptionAlreadyExpiredResponse = pageObj.endpoints().authSubscriptionCancel(
					dataSet.get("client"), dataSet.get("secret"), accessToken, subscriptionId3, "Price too High.",
					cancellationReasonId, "soft_cancelled");
			Assert.assertEquals(cancelSubscriptionAlreadyExpiredResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
					"Status code 422 did not match for Auth API Cancel Subscription that is already expired");
			boolean isCancelSubscriptionAlreadyExpiredSchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.authSubscriptionErrorSchema,
					cancelSubscriptionAlreadyExpiredResponse.asString());
			Assert.assertTrue(isCancelSubscriptionAlreadyExpiredSchemaValidated,
					"Auth API Cancel Subscription Schema Validation failed");
			String cancelSubscriptionAlreadyExpiredMsg = cancelSubscriptionAlreadyExpiredResponse.jsonPath()
					.get("error.message[0]").toString();
			Assert.assertEquals(cancelSubscriptionAlreadyExpiredMsg, "The subscription has been expired/canceled.",
					"Message did not match.");
			TestListeners.extentTest.get()
					.pass("Auth API Cancel Subscription call that is already expired is unsuccessful");
			logger.info("Auth API Cancel Subscription call that is already expired is unsuccessful");
		}

	}

	@Test(description = "SQ-T5725: Verify Negative Scenarios of Auth Api Create Checkin against Receipt, Fetch Checkin with External Uid, "
			+ "Void Loyalty Checkin, Create Online Redemption", groups = "api")
	public void T5725_verifyAuthCheckinNegative() {
		// User Sign-up
		TestListeners.extentTest.get().info("== Auth API User Sign-up ==");
		logger.info("== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		TestListeners.extentTest.get().pass("Auth API User Sign-up call is successful");
		logger.info("Auth API User Sign-up call is successful");

		// Checkin Against Receipt Schema
		TestListeners.extentTest.get().info("== Auth API Checkin Against Receipt ==");
		logger.info("== Auth API Checkin Against Receipt ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken, amount,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String externalUid = checkinResponse.jsonPath().get("checkin.external_uid").toString();
		TestListeners.extentTest.get().pass("Auth API Checkin Against Receipt is successful");
		logger.info("Auth API Checkin Against Receipt is successful");

		// Negative case: Checkin Against Receipt with missing amount
		TestListeners.extentTest.get().info("== Auth API Checkin Against Receipt with missing amount ==");
		logger.info("== Auth API Checkin Against Receipt with missing amount ==");
		Response checkinMissingAmountResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken, "",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isAuthCheckinMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidDataErrorSchema, checkinMissingAmountResponse.asString());
		Assert.assertTrue(isAuthCheckinMissingAmountSchemaValidated,
				"Auth API Checkin Against Receipt with missing amount Schema Validation failed");
		String checkinMissingAmountMsg = checkinMissingAmountResponse.jsonPath().get("error").toString();
		Assert.assertEquals(checkinMissingAmountMsg, "data was not valid JSON");
		TestListeners.extentTest.get().pass("Auth API Checkin Against Receipt with missing amount is unsuccessful");
		logger.info("Auth API Checkin Against Receipt with missing amount is unsuccessful");

		// Negative case: Checkin Against Receipt with invalid amount
		TestListeners.extentTest.get().info("== Auth API Checkin Against Receipt with invalid amount ==");
		logger.info("== Auth API Checkin Against Receipt with invalid amount ==");
		Response checkinInvalidAmountResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken,
				"0", dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinInvalidAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthCheckinInvalidAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, checkinInvalidAmountResponse.asString());
		Assert.assertTrue(isAuthCheckinInvalidAmountSchemaValidated,
				"Auth API Checkin Against Receipt with invalid amount Schema Validation failed");
		String checkinInvalidAmountMsg = checkinInvalidAmountResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(checkinInvalidAmountMsg, "The minimum checkin amount is $2.00");
		TestListeners.extentTest.get().pass("Auth API Checkin Against Receipt with invalid amount is unsuccessful");
		logger.info("Auth API Checkin Against Receipt with invalid amount is unsuccessful");

		// Negative case: Checkin Against Receipt with invalid auth token
		TestListeners.extentTest.get().info("== Auth API Checkin Against Receipt with invalid auth token ==");
		logger.info("== Auth API Checkin Against Receipt with invalid auth token ==");
		Response checkinInvalidAuthTokenResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept("1",
				amount, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthCheckinInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isAuthCheckinInvalidAuthTokenSchemaValidated,
				"Auth API Checkin Against Receipt with invalid auth token Schema Validation failed");
		String checkinInvalidAuthTokenMsg = checkinInvalidAuthTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(checkinInvalidAuthTokenMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get().pass("Auth API Checkin Against Receipt with invalid auth token is unsuccessful");
		logger.info("Auth API Checkin Against Receipt with invalid auth token is unsuccessful");

		// Negative case: Checkin Against Receipt with invalid signature
		TestListeners.extentTest.get().info("== Auth API Checkin Against Receipt with invalid signature ==");
		logger.info("== Auth API Checkin Against Receipt with invalid signature ==");
		Response checkinInvalidSignatureResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken,
				amount, "1", dataSet.get("secret"));
		Assert.assertEquals(checkinInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isAuthCheckinInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, checkinInvalidSignatureResponse.asString());
		Assert.assertTrue(isAuthCheckinInvalidSignatureSchemaValidated,
				"Auth API Checkin Against Receipt with invalid signature Schema Validation failed");
		String checkinInvalidSignatureMsg = checkinInvalidSignatureResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(checkinInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API Checkin Against Receipt with invalid signature is unsuccessful");
		logger.info("Auth API Checkin Against Receipt with invalid signature is unsuccessful");

		// Negative case: Fetch a Checkin with missing external_uid
		TestListeners.extentTest.get().info("== Fetch a Checkin with missing external uid ==");
		logger.info("== Fetch a Checkin with missing external uid ==");
		Response fetchCheckinMissingExternalUidResponse = pageObj.endpoints()
				.authApiFetchACheckinByExternal_uid(authToken, dataSet.get("client"), dataSet.get("secret"), "");
		Assert.assertEquals(fetchCheckinMissingExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isAuthFetchCheckinMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchCheckinMissingExternalUidResponse.asString());
		Assert.assertTrue(isAuthFetchCheckinMissingExternalUidSchemaValidated,
				"Auth Fetch Checkin with missing external uid schema validation failed");
		String fetchCheckinMissingExternalUidMsg = fetchCheckinMissingExternalUidResponse.jsonPath().get("error");
		Assert.assertEquals(fetchCheckinMissingExternalUidMsg,
				"Required parameter missing or the value is empty: external_uid");
		TestListeners.extentTest.get().pass("Auth API Fetch a Checkin with missing external uid is unsuccessful");
		logger.info("Auth API Fetch a Checkin with missing external uid is unsuccessful");

		// Negative case: Fetch a Checkin with invalid external_uid
		TestListeners.extentTest.get().info("== Fetch a Checkin with invalid external uid ==");
		logger.info("== Fetch a Checkin with invalid external uid ==");
		Response fetchCheckinInvalidExternalUidResponse = pageObj.endpoints()
				.authApiFetchACheckinByExternal_uid(authToken, dataSet.get("client"), dataSet.get("secret"), "1");
		Assert.assertEquals(fetchCheckinInvalidExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_EXPECTATION_FAILED);
		boolean isAuthFetchCheckinInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				fetchCheckinInvalidExternalUidResponse.asString());
		Assert.assertTrue(isAuthFetchCheckinInvalidExternalUidSchemaValidated,
				"Auth Fetch Checkin with invalid external uid schema validation failed");
		String fetchCheckinInvalidExternalUidMsg = fetchCheckinInvalidExternalUidResponse.jsonPath()
				.get("error.message");
		Assert.assertEquals(fetchCheckinInvalidExternalUidMsg, "Checkin associated with external_uid 1 not found.");
		TestListeners.extentTest.get().pass("Auth API Fetch a Checkin with invalid external uid is unsuccessful");
		logger.info("Auth API Fetch a Checkin with invalid external uid is unsuccessful");

		// Negative case: Fetch a Checkin with invalid signature
		TestListeners.extentTest.get().info("== Fetch a Checkin with invalid signature ==");
		logger.info("== Fetch a Checkin with invalid signature ==");
		Response fetchCheckinInvalidSignatureResponse = pageObj.endpoints()
				.authApiFetchACheckinByExternal_uid(authToken, "1", dataSet.get("secret"), externalUid);
		Assert.assertEquals(fetchCheckinInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isAuthFetchCheckinInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchCheckinInvalidSignatureResponse.asString());
		Assert.assertTrue(isAuthFetchCheckinInvalidSignatureSchemaValidated,
				"Auth Fetch Checkin with invalid signature schema validation failed");
		String fetchCheckinInvalidSignatureMsg = fetchCheckinInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(fetchCheckinInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API Fetch a Checkin with invalid signature is unsuccessful");
		logger.info("Auth API Fetch a Checkin with invalid signature is unsuccessful");

		// Negative case: Fetch a Checkin with invalid auth token
		TestListeners.extentTest.get().info("== Fetch a Checkin with invalid auth token ==");
		logger.info("== Fetch a Checkin with invalid auth token ==");
		Response fetchCheckinInvalidAuthTokenResponse = pageObj.endpoints().authApiFetchACheckinByExternal_uid("1",
				dataSet.get("client"), dataSet.get("secret"), externalUid);
		Assert.assertEquals(fetchCheckinInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthFetchCheckinInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchCheckinInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isAuthFetchCheckinInvalidAuthTokenSchemaValidated,
				"Auth Fetch Checkin with invalid auth token schema validation failed");
		String fetchCheckinInvalidAuthTokenMsg = fetchCheckinInvalidAuthTokenResponse.jsonPath().get("error");
		Assert.assertEquals(fetchCheckinInvalidAuthTokenMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get().pass("Auth API Fetch a Checkin with invalid auth token is unsuccessful");
		logger.info("Auth API Fetch a Checkin with invalid auth token is unsuccessful");

		// Negative case: Online void checkin with already committed checkin
		TestListeners.extentTest.get().info("== Online void checkin with already committed checkin ==");
		logger.info("== Online void checkin with already committed checkin ==");
		Response voidCheckinCommittedResponse = pageObj.endpoints().authOnlineVoidCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), externalUid);
		Assert.assertEquals(voidCheckinCommittedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_GONE);
		boolean isAuthVoidCheckinCommittedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, voidCheckinCommittedResponse.asString());
		Assert.assertTrue(isAuthVoidCheckinCommittedSchemaValidated,
				"Auth API void checkin with already committed checkin schema validation failed");
		String voidCheckinCommittedMsgCode = voidCheckinCommittedResponse.jsonPath().get("error.code");
		Assert.assertEquals(voidCheckinCommittedMsgCode, "commited");
		TestListeners.extentTest.get().pass("Auth API void checkin with already committed checkin is unsuccessful");
		logger.info("Auth API void checkin with already committed checkin is unsuccessful");

		// Negative case: Online void checkin with invalid external_uid
		TestListeners.extentTest.get().info("== Online void checkin with invalid external_uid ==");
		logger.info("== Online void checkin with invalid external_uid ==");
		Response voidCheckinInvalidExternalUidResponse = pageObj.endpoints().authOnlineVoidCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), "1");
		Assert.assertEquals(voidCheckinInvalidExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_EXPECTATION_FAILED);
		boolean isAuthVoidCheckinInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				voidCheckinInvalidExternalUidResponse.asString());
		Assert.assertTrue(isAuthVoidCheckinInvalidExternalUidSchemaValidated,
				"Auth API void checkin with invalid external_uid schema validation failed");
		String voidCheckinInvalidExternalUidMsg = voidCheckinInvalidExternalUidResponse.jsonPath().get("error.message");
		Assert.assertEquals(voidCheckinInvalidExternalUidMsg, "Checkin associated with external_uid 1 not found.");
		TestListeners.extentTest.get().pass("Auth API void checkin with invalid external_uid is unsuccessful");
		logger.info("Auth API void checkin with invalid external_uid is unsuccessful");

		// Negative case: Online void checkin with missing external_uid
		TestListeners.extentTest.get().info("== Online void checkin with missing external_uid ==");
		logger.info("== Online void checkin with missing external_uid ==");
		Response voidCheckinMissingExternalUidResponse = pageObj.endpoints().authOnlineVoidCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), "");
		Assert.assertEquals(voidCheckinMissingExternalUidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isAuthVoidCheckinMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, voidCheckinMissingExternalUidResponse.asString());
		Assert.assertTrue(isAuthVoidCheckinMissingExternalUidSchemaValidated,
				"Auth API void checkin with missing external_uid schema validation failed");
		String voidCheckinMissingExternalUidMsg = voidCheckinMissingExternalUidResponse.jsonPath().get("error");
		Assert.assertEquals(voidCheckinMissingExternalUidMsg,
				"Required parameter missing or the value is empty: external_uid");
		TestListeners.extentTest.get().pass("Auth API void checkin with missing external_uid is unsuccessful");
		logger.info("Auth API void checkin with missing external_uid is unsuccessful");

		// Negative case: Online void checkin with invalid signature
		TestListeners.extentTest.get().info("== Online void checkin with invalid signature ==");
		logger.info("== Online void checkin with invalid signature ==");
		Response voidCheckinInvalidSignatureResponse = pageObj.endpoints().authOnlineVoidCheckin(authToken, "1",
				dataSet.get("secret"), externalUid);
		Assert.assertEquals(voidCheckinInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isAuthVoidCheckinInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, voidCheckinInvalidSignatureResponse.asString());
		Assert.assertTrue(isAuthVoidCheckinInvalidSignatureSchemaValidated,
				"Auth API void checkin with invalid signature schema validation failed");
		String voidCheckinInvalidSignatureMsg = voidCheckinInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(voidCheckinInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API void checkin with invalid signature is unsuccessful");
		logger.info("Auth API void checkin with invalid signature is unsuccessful");

		// Negative case: Online void checkin with invalid auth token
		TestListeners.extentTest.get().info("== Online void checkin with invalid auth token ==");
		logger.info("== Online void checkin with invalid auth token ==");
		Response voidCheckinInvalidAuthTokenResponse = pageObj.endpoints().authOnlineVoidCheckin("1",
				dataSet.get("client"), dataSet.get("secret"), externalUid);
		Assert.assertEquals(voidCheckinInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthVoidCheckinInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, voidCheckinInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isAuthVoidCheckinInvalidAuthTokenSchemaValidated,
				"Auth API void checkin with invalid auth token schema validation failed");
		String voidCheckinInvalidAuthTokenMsg = voidCheckinInvalidAuthTokenResponse.jsonPath().get("error");
		Assert.assertEquals(voidCheckinInvalidAuthTokenMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Negative Case: Auth API void checkin with invalid auth token is unsuccessful");
		logger.info("Negative Case: Auth API void checkin with invalid auth token is unsuccessful");

		// Negative case: Create Online Redemption with invalid auth token
		TestListeners.extentTest.get().info("== Auth API Create Online Redemption with invalid auth token ==");
		logger.info("== Auth API Create Online Redemption with invalid auth token ==");
		Response redemptionInvalidAuthTokenResponse = pageObj.endpoints().authOnlineBankCurrencyRedemption("1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemptionInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isRedemptionInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isRedemptionInvalidAuthTokenSchemaValidated,
				"Negative Case: Auth API Create Online Redemption with invalid auth token schema validation failed");
		String redemptionInvalidAuthTokenMsg = redemptionInvalidAuthTokenResponse.jsonPath().get("error");
		Assert.assertEquals(redemptionInvalidAuthTokenMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Negative Case: Auth API Create Online Redemption with invalid auth token is unsuccessful");
		logger.info("Negative Case: Auth API Create Online Redemption with invalid auth token is unsuccessful");

		// Negative case: Create Online Redemption with invalid signature
		TestListeners.extentTest.get().info("== Auth API Create Online Redemption with invalid signature ==");
		logger.info("== Auth API Create Online Redemption with invalid signature ==");
		Response redemptionInvalidSignatureResponse = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken,
				"1", dataSet.get("secret"));
		Assert.assertEquals(redemptionInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isRedemptionInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionInvalidSignatureResponse.asString());
		Assert.assertTrue(isRedemptionInvalidSignatureSchemaValidated,
				"Negative Case: Auth API Create Online Redemption with invalid signature schema validation failed");
		String redemptionInvalidSignatureMsg = redemptionInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(redemptionInvalidSignatureMsg, "Invalid Signature");
		TestListeners.extentTest.get()
				.pass("Negative Case: Auth API Create Online Redemption with invalid signature is unsuccessful");
		logger.info("Negative Case: Auth API Create Online Redemption with invalid signature is unsuccessful");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
