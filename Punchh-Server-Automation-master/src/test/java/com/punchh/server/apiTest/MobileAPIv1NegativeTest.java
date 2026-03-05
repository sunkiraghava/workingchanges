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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileAPIv1NegativeTest {

	private static Logger logger = LogManager.getLogger(MobileAPIv1NegativeTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private Utilities utils;
	private static Map<String, String> dataSet;
	private String invalidValue, invalidEmail;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		invalidValue = "$@&12345";
		invalidEmail = "punchh" + CreateDateTime.getTimeDateString() + "$@&com";
	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T4943: Signup; SQ-T4956: Login; SQ-T4959: Update Guest details; "
			+ "SQ-T4960: Update User; SQ-T4965: Forgot Password; SQ-T4945: Messages; SQ-T4947: Feedback; SQ-T4949: Braintree token; "
			+ "SQ-T4967: Create Passcode; SQ-T4969: Update Passcode; SQ-T4963: Logout", groups = "api", priority = 0)
	public void verifyMobileAPIv1UserSignupNegative() {

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String fName = signUpResponse.jsonPath().get("first_name").toString();
		String lName = signUpResponse.jsonPath().get("last_name").toString();
		String signUpEmail = signUpResponse.jsonPath().getString("email");
		Assert.assertEquals(signUpEmail, userEmail, "email did not match");
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// User sign-up using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up using invalid client ==");
		logger.info("== Mobile API v1: User sign-up using invalid client ==");
		Response signUpInvalidClientResponse = pageObj.endpoints().Api1UserSignUp(userEmail, invalidValue,
				dataSet.get("secret"));
		Assert.assertEquals(signUpInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user signup");
		boolean isApi1SignUpInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, signUpInvalidClientResponse.asString());
		Assert.assertTrue(isApi1SignUpInvalidSignatureSchemaValidated, "API1 User Signup Schema Validation failed");
		String signUpInvalidClientMsg = signUpInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(signUpInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 user signup is unsuccessful because of invalid client");
		logger.info("API v1 user signup is unsuccessful because of invalid client");

		// User sign-up using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up using invalid secret ==");
		logger.info("== Mobile API v1: User sign-up using invalid secret ==");
		Response signUpInvalidSecretResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				invalidValue);
		Assert.assertEquals(signUpInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user signup");
		String signUpInvalidSecretMsg = signUpInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(signUpInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 user signup is unsuccessful because of invalid secret");
		logger.info("API v1 user signup is unsuccessful because of invalid secret");

		// User sign-up using invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up using invalid email ==");
		logger.info("== Mobile API v1: User sign-up using invalid email ==");
		Response signUpInvalidEmailResponse = pageObj.endpoints().Api1UserSignUp(invalidEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 user signup");
		boolean isApi1SignUpInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiEmailObjectSchema, signUpInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1SignUpInvalidEmailSchemaValidated, "API1 User Signup Schema Validation failed");
		String signUpInvalidEmailMsg = signUpInvalidEmailResponse.jsonPath().get("email[0]").toString();
		Assert.assertEquals(signUpInvalidEmailMsg, "Email is invalid", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 user signup is unsuccessful because of invalid email");
		logger.info("API v1 user signup is unsuccessful because of invalid email");

		// User sign-up with missing email
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up with missing email ==");
		logger.info("== Mobile API v1: User sign-up with missing email ==");
		Response signUpMissingEmailResponse = pageObj.endpoints().Api1UserSignUp("", dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 user signup");
		boolean isApi1SignUpMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, signUpMissingEmailResponse.asString());
		Assert.assertTrue(isApi1SignUpMissingEmailSchemaValidated, "API1 User Signup Schema Validation failed");
		String signUpMissingEmailMsg = signUpMissingEmailResponse.jsonPath().get("error").toString();
		Assert.assertEquals(signUpMissingEmailMsg, "Required parameter missing or the value is empty: email",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 user signup is unsuccessful because of missing email");
		logger.info("API v1 user signup is unsuccessful because of missing email");

		// User login using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: User login using invalid client ==");
		logger.info("== Mobile API v1: User login using invalid client ==");
		Response loginInvalidClientResponse = pageObj.endpoints().Api1UserLogin(userEmail, invalidValue,
				dataSet.get("secret"));
		Assert.assertEquals(loginInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user login");
		boolean isApi1LogInInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, loginInvalidClientResponse.asString());
		Assert.assertTrue(isApi1LogInInvalidSignatureSchemaValidated, "API1 User Login Schema Validation failed");
		String loginInvalidClientMsg = loginInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(loginInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user login is unsuccessful because of invalid client");
		logger.info("Api v1 user login is unsuccessful because of invalid client");

		// User login using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: User login using invalid secret ==");
		logger.info("== Mobile API v1: User login using invalid secret ==");
		Response loginInvalidSecretResponse = pageObj.endpoints().Api1UserLogin(userEmail, dataSet.get("client"),
				invalidValue);
		Assert.assertEquals(loginInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user login");
		String loginInvalidSecretMsg = loginInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(loginInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user login is unsuccessful because of invalid secret");
		logger.info("Api v1 user login is unsuccessful because of invalid secret");

		// User login using invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: User login using invalid email ==");
		logger.info("== Mobile API v1: User login using invalid email ==");
		Response loginInvalidEmailResponse = pageObj.endpoints().Api1UserLogin(invalidEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 user login");
		boolean isApi1LogInInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiBaseObjectSchema, loginInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1LogInInvalidEmailSchemaValidated, "API1 User Login Schema Validation failed");
		String loginInvalidEmailMsg = loginInvalidEmailResponse.jsonPath().get("base[0]").toString();
		Assert.assertEquals(loginInvalidEmailMsg, "Incorrect information submitted. Please retry.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user login is unsuccessful because of invalid email");
		logger.info("Api v1 user login is unsuccessful because of invalid email");

		// User login with missing email
		TestListeners.extentTest.get().info("== Mobile API v1: User login with missing email ==");
		logger.info("== Mobile API v1: User login with missing email ==");
		Response loginMissingEmailResponse = pageObj.endpoints().Api1UserLogin("", dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 user login");
		boolean isApi1LogInMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, loginMissingEmailResponse.asString());
		Assert.assertTrue(isApi1LogInMissingEmailSchemaValidated, "API1 User Login Schema Validation failed");
		String loginMissingEmailMsg = loginMissingEmailResponse.jsonPath().get("error").toString();
		Assert.assertEquals(loginMissingEmailMsg, "Required parameter missing or the value is empty: email",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user login is unsuccessful because of missing email");
		logger.info("Api v1 user login is unsuccessful because of missing email");

		// Update guest details (/api/mobile/customers/) using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid client ==");
		logger.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid client ==");
		Response updateGuestInvalidClientResponse = pageObj.endpoints().Api1MobileUpdateGuestDetails("New" + fName,
				"New" + lName, "New" + dataSet.get("passcode"), invalidValue, dataSet.get("secret"), token,
				"New" + userEmail);
		Assert.assertEquals(updateGuestInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 guest details update");
		boolean isApi1UpdateDetailsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateGuestInvalidClientResponse.asString());
		Assert.assertTrue(isApi1UpdateDetailsInvalidSignatureSchemaValidated,
				"API1 Guest details update Schema Validation failed");
		String updateGuestInvalidClientMsg = updateGuestInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateGuestInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid client");
		logger.info("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid client");

		// Update guest details (/api/mobile/customers/) using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid secret ==");
		logger.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid secret ==");
		Response updateGuestInvalidSecretResponse = pageObj.endpoints().Api1MobileUpdateGuestDetails("New" + fName,
				"New" + lName, "New" + dataSet.get("passcode"), dataSet.get("client"), invalidValue, token,
				"New" + userEmail);
		Assert.assertEquals(updateGuestInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 guest details update");
		String updateGuestInvalidSecretMsg = updateGuestInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateGuestInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid secret");
		logger.info("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid secret");

		// Update guest details (/api/mobile/customers/) using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid token ==");
		logger.info("== Mobile API v1: Update guest details (/api/mobile/customers/) using invalid token ==");
		Response updateGuestInvalidTokenResponse = pageObj.endpoints().Api1MobileUpdateGuestDetails("New" + fName,
				"New" + lName, "New" + dataSet.get("passcode"), dataSet.get("client"), dataSet.get("secret"),
				invalidValue, "New" + userEmail);
		Assert.assertEquals(updateGuestInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 guest details update");
		boolean isApi1UpdateGuestInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateGuestInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1UpdateGuestInvalidTokenSchemaValidated,
				"API1 Update Guest details Schema Validation failed");
		String updateGuestInvalidTokenMsg = updateGuestInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(updateGuestInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid token");
		logger.info("Api v1 Update guest details (/api/mobile/customers/) is unsuccessful because of invalid token");

		// Update user (/api/mobile/users/) using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid client ==");
		logger.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid client ==");
		Response updateUserInvalidClientResponse = pageObj.endpoints().api1UpdateUser(dataSet.get("signupChannel"),
				invalidValue, dataSet.get("secret"), token);
		Assert.assertEquals(updateUserInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update user");
		boolean isApi1UpdateUserInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateUserInvalidClientResponse.asString());
		Assert.assertTrue(isApi1UpdateUserInvalidSignatureSchemaValidated, "API1 Update user Schema Validation failed");
		String updateUserInvalidClientMsg = updateUserInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateUserInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid client");
		logger.info("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid client");

		// Update user (/api/mobile/users/) using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid secret ==");
		logger.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid secret ==");
		Response updateUserInvalidSecretResponse = pageObj.endpoints().api1UpdateUser(dataSet.get("signupChannel"),
				dataSet.get("client"), invalidValue, token);
		Assert.assertEquals(updateUserInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update user");
		String updateUserInvalidSecretMsg = updateUserInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateUserInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid secret");
		logger.info("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid secret");

		// Update user (/api/mobile/users/) using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid token ==");
		logger.info("== Mobile API v1: Update user (/api/mobile/users/) using invalid token ==");
		Response updateUserInvalidTokenResponse = pageObj.endpoints().api1UpdateUser(dataSet.get("signupChannel"),
				dataSet.get("client"), dataSet.get("secret"), invalidValue);
		Assert.assertEquals(updateUserInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Update user");
		TestListeners.extentTest.get()
				.pass("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid token");
		logger.info("Api v1 Update user (/api/mobile/users/) is unsuccessful because of invalid token");

		// Forgot password using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Forgot password using invalid client ==");
		logger.info("== Mobile API v1: Forgot password using invalid client ==");
		Response forgotPasswordInvalidClientResponse = pageObj.endpoints().Api1MobileForgotPassword(userEmail,
				invalidValue, dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Forgot Password");
		boolean isApi1ForgotPasswordInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, forgotPasswordInvalidClientResponse.asString());
		Assert.assertTrue(isApi1ForgotPasswordInvalidSignatureSchemaValidated,
				"API1 Forgot password Schema Validation failed");
		String forgotPwdInvalidClientMsg = forgotPasswordInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(forgotPwdInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 forgot password is unsuccessful because of invalid client");
		logger.info("Api v1 forgot password is unsuccessful because of invalid client");

		// Forgot password using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Forgot password using invalid secret ==");
		logger.info("== Mobile API v1: Forgot password using invalid secret ==");
		Response forgotPasswordInvalidSecretResponse = pageObj.endpoints().Api1MobileForgotPassword(userEmail,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(forgotPasswordInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Forgot Password");
		String forgotPwdInvalidSecretMsg = forgotPasswordInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(forgotPwdInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 forgot password is unsuccessful because of invalid secret");
		logger.info("Api v1 forgot password is unsuccessful because of invalid secret");

		// Create Guest feedback using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Create Guest feedback using invalid client ==");
		logger.info("== Mobile API v1: Create Guest feedback using invalid client ==");
		Response feedbackInvalidClientResponse = pageObj.endpoints().api1CreateFeedback(dataSet.get("feedbackRating"),
				dataSet.get("feedbackMessage"), invalidValue, dataSet.get("secret"), token);
		Assert.assertEquals(feedbackInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Create Guest feedback");
		boolean isApi1CreateGuestFeedbackInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, feedbackInvalidClientResponse.asString());
		Assert.assertTrue(isApi1CreateGuestFeedbackInvalidSignatureSchemaValidated,
				"API1 Create Guest feedback Schema Validation failed");
		String feedbackInvalidClientMsg = feedbackInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(feedbackInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Guest feedback is unsuccessful because of invalid client");
		logger.info("API v1 Create Guest feedback is unsuccessful because of invalid client");

		// Create Guest feedback using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Create Guest feedback using invalid secret ==");
		logger.info("== Mobile API v1: Create Guest feedback using invalid secret ==");
		Response feedbackInvalidSecretResponse = pageObj.endpoints().api1CreateFeedback(dataSet.get("feedbackRating"),
				dataSet.get("feedbackMessage"), dataSet.get("client"), invalidValue, token);
		Assert.assertEquals(feedbackInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Create Guest feedback");
		String feedbackInvalidSecretMsg = feedbackInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(feedbackInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Guest feedback is unsuccessful because of invalid secret");
		logger.info("API v1 Create Guest feedback is unsuccessful because of invalid secret");

		// Create Guest feedback using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Create Guest feedback using invalid token ==");
		logger.info("== Mobile API v1: Create Guest feedback using invalid token ==");
		Response feedbackInvalidTokenResponse = pageObj.endpoints().api1CreateFeedback(dataSet.get("feedbackRating"),
				dataSet.get("feedbackMessage"), dataSet.get("client"), dataSet.get("secret"), invalidValue);
		Assert.assertEquals(feedbackInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Create Guest feedback");
		boolean isApi1CreateGuestFeedbackInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, feedbackInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1CreateGuestFeedbackInvalidTokenSchemaValidated,
				"API1 Create Guest feedback Schema Validation failed");
		String feedbackInvalidTokenMsg = feedbackInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(feedbackInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Guest feedback is unsuccessful because of invalid token");
		logger.info("API v1 Create Guest feedback is unsuccessful because of invalid token");

		// Create Guest feedback with missing feedback message and rating
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Create Guest feedback with missing feedback message and rating ==");
		logger.info("== Mobile API v1: Create Guest feedback with missing feedback message and rating ==");
		Response feedbackMissingParametersResponse = pageObj.endpoints().api1CreateFeedback("\"\"", "",
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(feedbackMissingParametersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Create Guest feedback");
		boolean isApi1CreateFeedbackMissingParamsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, feedbackMissingParametersResponse.asString());
		Assert.assertTrue(isApi1CreateFeedbackMissingParamsSchemaValidated,
				"API1 Create Guest feedback Schema Validation failed");
		String feedbackMissingParametersMsg = feedbackMissingParametersResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(feedbackMissingParametersMsg, "Message can't be blank", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Create Guest feedback is unsuccessful because of missing feedback message and rating");
		logger.info("API v1 Create Guest feedback is unsuccessful because of missing feedback message and rating");

		// Fetch Rich messages using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Rich messages using invalid client ==");
		logger.info("== Mobile API v1: Fetch Rich messages using invalid client ==");
		Response fetchMessagesInvalidClientResponse = pageObj.endpoints().api1FetchMessages(invalidValue,
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchMessagesInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Messages");
		boolean isApi1FetchRichMessagesInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchMessagesInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchRichMessagesInvalidSignatureSchemaValidated,
				"API1 Fetch Rich messages Schema Validation failed");
		String fetchMessagesInvalidClientMsg = fetchMessagesInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(fetchMessagesInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch Messages is unsuccessful because of invalid client");
		logger.info("Api v1 Fetch Messages is unsuccessful because of invalid client");

		// Fetch Rich messages using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Rich messages using invalid secret ==");
		logger.info("== Mobile API v1: Fetch Rich messages using invalid secret ==");
		Response fetchMessagesInvalidSecretResponse = pageObj.endpoints().api1FetchMessages(dataSet.get("client"),
				invalidValue, token);
		Assert.assertEquals(fetchMessagesInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Messages");
		String fetchMessagesInvalidSecretMsg = fetchMessagesInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(fetchMessagesInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch Messages is unsuccessful because of invalid secret");
		logger.info("Api v1 Fetch Messages is unsuccessful because of invalid secret");

		// Fetch Rich messages using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Rich messages using invalid token ==");
		logger.info("== Mobile API v1: Fetch Rich messages using invalid token ==");
		Response fetchMessagesInvalidTokenResponse = pageObj.endpoints().api1FetchMessages(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(fetchMessagesInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch Messages");
		boolean isApi1FetchRichMessagesInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchMessagesInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchRichMessagesInvalidTokenSchemaValidated,
				"API1 Fetch Rich messages Schema Validation failed");
		String fetchMessagesInvalidTokenMsg = fetchMessagesInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(fetchMessagesInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch Messages is unsuccessful because of invalid token");
		logger.info("Api v1 Fetch Messages is unsuccessful because of invalid token");

		// Get Braintree token using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Get Braintree token using invalid client ==");
		logger.info("== Mobile API v1: Get Braintree token using invalid client ==");
		Response braintreeTokenInvalidClientResponse = pageObj.endpoints().api1BraintreeToken(invalidValue,
				dataSet.get("secret"), token);
		Assert.assertEquals(braintreeTokenInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Braintree Token");
		boolean isApi1GetBraintreeTokenInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, braintreeTokenInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GetBraintreeTokenInvalidSignatureSchemaValidated,
				"API1 Get Braintree token Schema Validation failed");
		String braintreeTokenInvalidClientMsg = braintreeTokenInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(braintreeTokenInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Get Braintree Token is unsuccessful because of invalid client");
		logger.info("API v1 Get Braintree Token is unsuccessful because of invalid client");

		// Get Braintree token using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Get Braintree token using invalid secret ==");
		logger.info("== Mobile API v1: Get Braintree token using invalid secret ==");
		Response braintreeTokenInvalidSecretResponse = pageObj.endpoints().api1BraintreeToken(dataSet.get("client"),
				invalidValue, token);
		Assert.assertEquals(braintreeTokenInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Braintree Token");
		String braintreeTokenInvalidSecretMsg = braintreeTokenInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(braintreeTokenInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Get Braintree Token is unsuccessful because of invalid secret");
		logger.info("API v1 Get Braintree Token is unsuccessful because of invalid secret");

		// Get Braintree token using invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Get Braintree token using invalid user access token ==");
		logger.info("== Mobile API v1: Get Braintree token using invalid user access token ==");
		Response braintreeTokenInvalidUserTokenResponse = pageObj.endpoints().api1BraintreeToken(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(braintreeTokenInvalidUserTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Get Braintree Token");
		boolean isApi1GetBraintreeTokenInvalidUserTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, braintreeTokenInvalidUserTokenResponse.asString());
		Assert.assertTrue(isApi1GetBraintreeTokenInvalidUserTokenSchemaValidated,
				"API1 Get Braintree token Schema Validation failed");
		String braintreeTokenInvalidUserTokenMsg = braintreeTokenInvalidUserTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(braintreeTokenInvalidUserTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Get Braintree Token is unsuccessful because of invalid user access token");
		logger.info("API v1 Get Braintree Token is unsuccessful because of invalid user access token");

		// Create Passcode
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode ==");
		logger.info("== Mobile API v1: Create Passcode ==");
		Response createPasscodeResponse = pageObj.endpoints().api1CreatePasscode(dataSet.get("passcode"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 Create Passcode");
		String createPasscodeMsg = createPasscodeResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createPasscodeMsg, "Passcode has been successfully created.", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Passcode is successful");
		logger.info("API v1 Create Passcode is successful");

		// Create Passcode for already configured User
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode for already configured User ==");
		logger.info("== Mobile API v1: Create Passcode for already configured User==");
		Response createPasscodeAlreadyConfiguredResponse = pageObj.endpoints()
				.api1CreatePasscode(dataSet.get("passcode"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createPasscodeAlreadyConfiguredResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Create Passcode");
		boolean isApi1CreatePasscodeAlreadyConfiguredSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiPasscodeObjectSchema, createPasscodeAlreadyConfiguredResponse.asString());
		Assert.assertTrue(isApi1CreatePasscodeAlreadyConfiguredSchemaValidated,
				"API1 Create Passcode Schema Validation failed");
		String createPasscodeAlreadyConfiguredMsg = createPasscodeAlreadyConfiguredResponse.jsonPath()
				.get("passcode[0]").toString();
		Assert.assertEquals(createPasscodeAlreadyConfiguredMsg, "already configured for this user.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Create Passcode is unsuccessful because of being already configured");
		logger.info("API v1 Create Passcode is unsuccessful because of being already configured");

		// Create Passcode with missing passcode parameter
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode with missing passcode parameter ==");
		logger.info("== Mobile API v1: Create Passcode with missing passcode parameter ==");
		Response createPasscodeMissingParameterResponse = pageObj.endpoints().api1CreatePasscode("",
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createPasscodeMissingParameterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Create Passcode");
		boolean isApi1CreatePasscodeMissingParameterSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createPasscodeMissingParameterResponse.asString());
		Assert.assertTrue(isApi1CreatePasscodeMissingParameterSchemaValidated,
				"API1 Create Passcode Schema Validation failed");
		String createPasscodeMissingParameterMsg = createPasscodeMissingParameterResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(createPasscodeMissingParameterMsg,
				"Required parameter missing or the value is empty: passcode", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Create Passcode is unsuccessful because of missing passcode parameter");
		logger.info("API v1 Create Passcode is unsuccessful because of missing passcode parameter");

		// Create Passcode using invalid client˙
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode using invalid client ==");
		logger.info("== Mobile API v1: Create Passcode using invalid client ==");
		Response createPasscodeInvalidClientResponse = pageObj.endpoints().api1CreatePasscode(dataSet.get("passcode"),
				invalidValue, dataSet.get("secret"), token);
		Assert.assertEquals(createPasscodeInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Create Passcode");
		boolean isApi1CreatePasscodeInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createPasscodeInvalidClientResponse.asString());
		Assert.assertTrue(isApi1CreatePasscodeInvalidSignatureSchemaValidated,
				"API1 Create Passcode Schema Validation failed");
		String createPasscodeInvalidClientMsg = createPasscodeInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createPasscodeInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Passcode is unsuccessful because of invalid client");
		logger.info("API v1 Create Passcode is unsuccessful because of invalid client");

		// Create Passcode using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode using invalid secret ==");
		logger.info("== Mobile API v1: Create Passcode using invalid secret ==");
		Response createPasscodeInvalidSecretResponse = pageObj.endpoints().api1CreatePasscode(dataSet.get("passcode"),
				dataSet.get("client"), invalidValue, token);
		Assert.assertEquals(createPasscodeInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Create Passcode");
		String createPasscodeInvalidSecretMsg = createPasscodeInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createPasscodeInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Passcode is unsuccessful because of invalid secret");
		logger.info("API v1 Create Passcode is unsuccessful because of invalid secret");

		// Create Passcode using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Create Passcode using invalid token ==");
		logger.info("== Mobile API v1: Create Passcode using invalid token ==");
		Response createPasscodeInvalidTokenResponse = pageObj.endpoints().api1CreatePasscode(dataSet.get("passcode"),
				dataSet.get("client"), dataSet.get("secret"), invalidValue);
		Assert.assertEquals(createPasscodeInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Create Passcode");
		String createPasscodeInvalidTokenMsg = createPasscodeInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createPasscodeInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Create Passcode is unsuccessful because of invalid token");
		logger.info("API v1 Create Passcode is unsuccessful because of invalid token");

		// Update Passcode using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Update Passcode using invalid client ==");
		logger.info("== Mobile API v1: Update Passcode using invalid client ==");
		Response updatePasscodeInvalidClientResponse = pageObj.endpoints().api1UpdatePasscode(invalidValue,
				dataSet.get("secret"), token);
		Assert.assertEquals(updatePasscodeInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update Passcode");
		String updatePasscodeInvalidClientMsg = updatePasscodeInvalidClientResponse.jsonPath().get("[0]").toString();
		boolean isApi1UpdatePasscodeInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updatePasscodeInvalidClientResponse.asString());
		Assert.assertTrue(isApi1UpdatePasscodeInvalidSignatureSchemaValidated,
				"API1 Update Passcode Schema Validation failed");
		Assert.assertEquals(updatePasscodeInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update Passcode is unsuccessful because of invalid client");
		logger.info("API v1 Update Passcode is unsuccessful because of invalid client");

		// Update Passcode using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Update Passcode using invalid secret ==");
		logger.info("== Mobile API v1: Update Passcode using invalid secret ==");
		Response updatePasscodeInvalidSecretResponse = pageObj.endpoints().api1UpdatePasscode(dataSet.get("client"),
				invalidValue, token);
		Assert.assertEquals(updatePasscodeInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update Passcode");
		String updatePasscodeInvalidSecretMsg = updatePasscodeInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updatePasscodeInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update Passcode is unsuccessful because of invalid secret");
		logger.info("API v1 Update Passcode is unsuccessful because of invalid secret");

		// Update Passcode using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Update Passcode using invalid token ==");
		logger.info("== Mobile API v1: Update Passcode using invalid token ==");
		Response updatePasscodeInvalidTokenResponse = pageObj.endpoints().api1UpdatePasscode(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(updatePasscodeInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Update Passcode");
		boolean isApi1UpdatePasscodeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updatePasscodeInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1UpdatePasscodeInvalidTokenSchemaValidated,
				"API1 Update Passcode Schema Validation failed");
		String updatePasscodeInvalidTokenMsg = updatePasscodeInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(updatePasscodeInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update Passcode is unsuccessful because of invalid token");
		logger.info("API v1 Update Passcode is unsuccessful because of invalid token");

		// User logout using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: User logout using invalid client ==");
		logger.info("== Mobile API v1: User logout using invalid client ==");
		Response logoutInvalidClientResponse = pageObj.endpoints().api1UserLogout(invalidValue, dataSet.get("secret"),
				token);
		Assert.assertEquals(logoutInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user logout");
		boolean isApi1UserLogoutInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, logoutInvalidClientResponse.asString());
		Assert.assertTrue(isApi1UserLogoutInvalidSignatureSchemaValidated, "API1 User logout Schema Validation failed");
		String logoutInvalidClientMsg = logoutInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(logoutInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user logout is unsuccessful because of invalid client");
		logger.info("Api v1 user logout is unsuccessful because of invalid client");

		// User logout using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: User logout using invalid secret ==");
		logger.info("== Mobile API v1: User logout using invalid secret ==");
		Response logoutInvalidSecretResponse = pageObj.endpoints().api1UserLogout(dataSet.get("client"), invalidValue,
				token);
		Assert.assertEquals(logoutInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 user logout");
		String logoutInvalidSecretMsg = logoutInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(logoutInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user logout is unsuccessful because of invalid secret");
		logger.info("Api v1 user logout is unsuccessful because of invalid secret");

		// User logout using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: User logout using invalid token ==");
		logger.info("== Mobile API v1: User logout using invalid token ==");
		Response logoutInvalidTokenResponse = pageObj.endpoints().api1UserLogout(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(logoutInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 user logout");
		boolean isApi1UserLogoutInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, logoutInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1UserLogoutInvalidTokenSchemaValidated, "API1 User logout Schema Validation failed");
		String logoutInvalidTokenMsg = logoutInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(logoutInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 user logout is unsuccessful because of invalid token");
		logger.info("Api v1 user logout is unsuccessful because of invalid token");
	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T4982: Generate Redemption; SQ-T4984: Cancel Redemption; SQ-T4988: Fetch Notifications", groups = "api", priority = 1)
	public void verifyAPIv1RedemptionsNegative() {

		String invalidId = "1234";

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// Generate Redemption code with not enough reward balance available
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Generate Redemption code with not enough reward balance available ==");
		logger.info("== Mobile API v1: Generate Redemption code with not enough reward balance available ==");
		Response redemptionLowBalanceResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token,
				dataSet.get("redeemableId"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemptionLowBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Generate Redemption code with redeemable_id");
		boolean isApi1GenerateRedemptionLowBalanceSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionLowBalanceResponse.asString());
		Assert.assertTrue(isApi1GenerateRedemptionLowBalanceSchemaValidated,
				"API1 Generate Redemption code Schema Validation failed");
		String actualLowBalanceMsg = redemptionLowBalanceResponse.jsonPath().getString("[0]");
		String expectedLowBalanceMsg = "Not enough reward balance available to redeem. Current Balance of $0.00 is less than "
				+ dataSet.get("rewardAmount") + " requested";
		Assert.assertEquals(actualLowBalanceMsg.toLowerCase(), expectedLowBalanceMsg.toLowerCase(),
				"Message did not match");
		TestListeners.extentTest.get().pass(
				"API v1 Generate Redemption code with redeemable_id is unsuccessful because of not having enough reward balance");
		logger.info(
				"API v1 Generate Redemption code with redeemable_id is unsuccessful because of not having enough reward balance");

		// API2 Send reward amount to user
		TestListeners.extentTest.get().info("== Mobile API v2: Send Reward amount to user ==");
		logger.info("== Mobile API v2: Send Reward amount to user ==");
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		TestListeners.extentTest.get().pass("API2 send reward amount to user is successful");
		logger.info("API2 send reward amount to user is successful");

		// Generate Redemption code with redeemable_id using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid client ==");
		logger.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid client ==");
		Response redemptionInvalidClientResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token,
				dataSet.get("redeemableId"), invalidValue, dataSet.get("secret"));
		Assert.assertEquals(redemptionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Generate Redemption code with redeemable_id");
		boolean isApi1GenerateRedemptionInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GenerateRedemptionInvalidSignatureSchemaValidated,
				"API1 Generate Redemption code Schema Validation failed");
		String redemptionInvalidClientMsg = redemptionInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(redemptionInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid client");
		logger.info("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid client");

		// Generate Redemption code with redeemable_id using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid secret ==");
		logger.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid secret ==");
		Response redemptionInvalidSecretResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token,
				dataSet.get("redeemableId"), dataSet.get("client"), invalidValue);
		Assert.assertEquals(redemptionInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Generate Redemption code with redeemable_id");
		String redemptionInvalidSecretMsg = redemptionInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(redemptionInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid secret");
		logger.info("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid secret");

		// Generate Redemption code with redeemable_id using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid token ==");
		logger.info("== Mobile API v1: Generate Redemption code with redeemable_id using invalid token ==");
		Response redemptionInvalidTokenResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(invalidValue,
				dataSet.get("redeemableId"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemptionInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Generate Redemption code with redeemable_id");
		boolean isApi1GenerateRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, redemptionInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GenerateRedemptionInvalidTokenSchemaValidated,
				"API1 Generate Redemption code Schema Validation failed");
		String redemptionInvalidTokenMsg = redemptionInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(redemptionInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid token");
		logger.info("API v1 Generate Redemption code with redeemable_id is unsuccessful because of invalid token");

		// Generate Redemption with redeemed points
		TestListeners.extentTest.get().info("== Mobile API v1: Generate Redemption with redeemed points ==");
		logger.info("== Mobile API v1: Generate Redemption with redeemed points ==");
		Response generateRedemptionResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "2",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(generateRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 Generate Redemption with redeemed points");
		String redemptionId = generateRedemptionResponse.jsonPath().get("id").toString();
		Assert.assertNotNull(redemptionId, "Redemption Id is null");
		TestListeners.extentTest.get().pass(
				"API v1 Generate Redemption with redeemed points is successful for redemption id: " + redemptionId);
		logger.info("API v1 Generate Redemption with redeemed points is successful for redemption id: " + redemptionId);

		// Cancel Redemption using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption using invalid client ==");
		logger.info("== Mobile API v1: Cancel Redemption using invalid client ==");
		Response cancelRedemptionInvalidClientResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, token,
				invalidValue, dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, cancelRedemptionInvalidClientResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionInvalidSignatureSchemaValidated,
				"API1 Cancel Redemption Schema Validation failed");
		String cancelRedemptionInvalidClientMsg = cancelRedemptionInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(cancelRedemptionInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Cancel Redemption is unsuccessful because of invalid client");
		logger.info("API v1 Cancel Redemption is unsuccessful because of invalid client");

		// Cancel Redemption using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption using invalid secret ==");
		logger.info("== Mobile API v1: Cancel Redemption using invalid secret ==");
		Response cancelRedemptionInvalidSecretResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, token,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(cancelRedemptionInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Cancel Redemption");
		String cancelRedemptionInvalidSecretMsg = cancelRedemptionInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(cancelRedemptionInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Cancel Redemption is unsuccessful because of invalid secret");
		logger.info("API v1 Cancel Redemption is unsuccessful because of invalid secret");

		// Cancel Redemption using invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption using invalid user access token ==");
		logger.info("== Mobile API v1: Cancel Redemption using invalid user access token ==");
		Response cancelRedemptionInvalidTokenResponse = pageObj.endpoints().api1CancelRedemption(redemptionId,
				invalidValue, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, cancelRedemptionInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionInvalidTokenSchemaValidated,
				"API1 Cancel Redemption Schema Validation failed");
		String cancelRedemptionInvalidTokenMsg = cancelRedemptionInvalidTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(cancelRedemptionInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Cancel Redemption is unsuccessful because of invalid user access token");
		logger.info("API v1 Cancel Redemption is unsuccessful because of invalid user access token");

		// Cancel Redemption using invalid redemption id
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption using invalid redemption id ==");
		logger.info("== Mobile API v1: Cancel Redemption using invalid redemption id ==");
		Response cancelRedemptionInvalidRedemptionIdResponse = pageObj.endpoints().api1CancelRedemption(invalidId,
				token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionInvalidRedemptionIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionInvalidRedemptionIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, cancelRedemptionInvalidRedemptionIdResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionInvalidRedemptionIdSchemaValidated,
				"API1 Cancel Redemption Schema Validation failed");
		String cancelRedemptionInvalidRedemptionIdMsg = cancelRedemptionInvalidRedemptionIdResponse.jsonPath()
				.get("errors").toString();
		Assert.assertEquals(cancelRedemptionInvalidRedemptionIdMsg,
				"This Redemption is an invalid redemption or It is not for this guest", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Cancel Redemption is unsuccessful because of invalid redemption id with message: "
						+ cancelRedemptionInvalidRedemptionIdMsg);
		logger.info("API v1 Cancel Redemption is unsuccessful because of invalid redemption id");

		// Cancel Redemption using valid redemption id
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption using valid redemption id ==");
		logger.info("== Mobile API v1: Cancel Redemption using valid redemption id ==");
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Cancel Redemption");
		String cancelRedemptionMsg = cancelRedemptionResponse.jsonPath().get("message").toString();
		Assert.assertEquals(cancelRedemptionMsg, "Redemption successfully cancelled.", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Cancel Redemption is successful for redemption id: " + redemptionId
				+ " with message: " + cancelRedemptionMsg);
		logger.info("API v1 Cancel Redemption is successful for redemption id: " + redemptionId + " with message: "
				+ cancelRedemptionMsg);

		// Cancel Redemption using already cancelled redemption id
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Cancel Redemption using already cancelled redemption id ==");
		logger.info("== Mobile API v1: Cancel Redemption using already cancelled redemption id ==");
		Response cancelRedemptionCancelledIdResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionCancelledIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionCancelledIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, cancelRedemptionCancelledIdResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionCancelledIdSchemaValidated,
				"API1 Cancel Redemption Schema Validation failed");
		String cancelRedemptionCancelledIdMsg = cancelRedemptionCancelledIdResponse.jsonPath().get("errors").toString();
		Assert.assertEquals(cancelRedemptionCancelledIdMsg,
				"Redemption cancellation failed. This redemption has already been cancelled.", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Cancel Redemption is unsuccessful for redemption id " + redemptionId
				+ " with message: " + cancelRedemptionCancelledIdMsg);
		logger.info("API v1 Cancel Redemption is unsuccessful for redemption id " + redemptionId + " with message: "
				+ cancelRedemptionCancelledIdMsg);

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// API2 Send reward amount to user
		TestListeners.extentTest.get().info("== Mobile API v2: Send Reward amount to user ==");
		logger.info("== Mobile API v2: Send Reward amount to user ==");
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		TestListeners.extentTest.get().pass("API2 send reward amount to user is successful");
		logger.info("API2 send reward amount to user is successful");

		// Generate Redemption #2
		TestListeners.extentTest.get().info("== Mobile API v1: Generate Redemption #2 ==");
		logger.info("== Mobile API v1: Generate Redemption #2 ==");
		Response generateRedemptionResponse2 = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token1, "2",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(generateRedemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 Generate Redemption with redeemed points"
						+ generateRedemptionResponse2.asString());
		String redemptionCode2 = generateRedemptionResponse2.jsonPath().get("internal_tracking_code").toString();
		String redemptionId2 = generateRedemptionResponse2.jsonPath().get("id").toString();
		Assert.assertNotNull(redemptionId2, "Redemption Id is null");
		TestListeners.extentTest.get()
				.pass("API v1 Generate Redemption is successful for Redemption #2 having id: " + redemptionId2);
		logger.info("API v1 Generate Redemption is successful for Redemption #2 having id: " + redemptionId2);

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		// Perform POS Redemption of Redemption #2
		TestListeners.extentTest.get().info("== POS API: Perform POS Redemption of Redemption #2  ==");
		logger.info("== POS API: Perform POS Redemption of Redemption #2  ==");
		Response performRedemptionResponse = pageObj.endpoints().posRedemptionOfCode(userEmail1, date, redemptionCode2,
				key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(performRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS Perform redemption API");
		String performRedemptionSuccessMsg = performRedemptionResponse.jsonPath().get("status").toString();
		String posRedemptionId = performRedemptionResponse.jsonPath().get("redemption_id").toString();
		Assert.assertTrue(performRedemptionSuccessMsg.contains("Please HONOR it."),
				"Failed to call POS Perform redemption API");
		TestListeners.extentTest.get().pass("POS Perform Redemption is successful for redemption #2 having id: "
				+ posRedemptionId + " with message: " + performRedemptionSuccessMsg);
		logger.info("POS Perform Redemption is successful for redemption #2 having id: " + posRedemptionId
				+ " with message: " + performRedemptionSuccessMsg);

		// Cancel Redemption #2 (Already redeemed code)
		TestListeners.extentTest.get().info("== Mobile API v1: Cancel Redemption #2 (Already redeemed code) ==");
		logger.info("== Mobile API v1: Cancel Redemption #2 (Already redeemed code) ==");
		Response cancelRedemptionAlreadyRedeemedResponse = pageObj.endpoints().api1CancelRedemption(posRedemptionId,
				token1, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionAlreadyRedeemedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionAlreadyRedeemedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, cancelRedemptionAlreadyRedeemedResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionAlreadyRedeemedSchemaValidated,
				"API1 Cancel Redemption Schema Validation");
		String cancelRedemptionAlreadyRedeemedMsg = cancelRedemptionAlreadyRedeemedResponse.jsonPath().get("errors")
				.toString();
		Assert.assertEquals(cancelRedemptionAlreadyRedeemedMsg,
				"This Redemption cannot be cancelled because the Redemption code has been honoured / redeemed",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Cancel Redemption call is unsuccessful for redemption #2 having id: " + posRedemptionId
						+ " with message: " + cancelRedemptionAlreadyRedeemedMsg);
		logger.info("API v1 Cancel Redemption call is unsuccessful for redemption #2 having id: " + posRedemptionId
				+ " with message: " + cancelRedemptionAlreadyRedeemedMsg);

		// Fetch User Notifications using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Notifications using invalid client ==");
		logger.info("== Mobile API v1: Fetch User Notifications using invalid client ==");
		Response fetchNotificationsInvalidClientResponse = pageObj.endpoints().api1FetchMessages(invalidValue,
				dataSet.get("secret"), token1);
		Assert.assertEquals(fetchNotificationsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch User Notifications");
		boolean isApi1FetchNotificationsInvalidClientSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchNotificationsInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchNotificationsInvalidClientSchemaValidated,
				"API1 Fetch User Notifications Schema Validation failed");
		String fetchNotificationsInvalidClientMsg = fetchNotificationsInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(fetchNotificationsInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Fetch User Notifications is unsuccessful because of invalid client");
		logger.info("Api v1 Fetch User Notifications is unsuccessful because of invalid client");

		// Fetch User Notifications using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Notifications using invalid secret ==");
		logger.info("== Mobile API v1: Fetch User Notifications using invalid secret ==");
		Response fetchNotificationsInvalidSecretResponse = pageObj.endpoints().api1FetchMessages(dataSet.get("client"),
				invalidValue, token1);
		Assert.assertEquals(fetchNotificationsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch User Notifications");
		String fetchNotificationsInvalidSecretMsg = fetchNotificationsInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(fetchNotificationsInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Fetch User Notifications is unsuccessful because of invalid secret");
		logger.info("Api v1 Fetch User Notifications is unsuccessful because of invalid secret");

		// Fetch User Notifications using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Notifications using invalid token ==");
		logger.info("== Mobile API v1: Fetch User Notifications using invalid token ==");
		Response fetchNotificationsInvalidTokenResponse = pageObj.endpoints().api1FetchMessages(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(fetchNotificationsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch User Notifications");
		boolean isApi1FetchNotificationsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchNotificationsInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchNotificationsInvalidTokenSchemaValidated,
				"API1 Fetch User Notifications Schema Validation failed");
		String fetchNotificationsInvalidTokenMsg = fetchNotificationsInvalidTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(fetchNotificationsInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch User Notifications is unsuccessful because of invalid token");
		logger.info("Api v1 Fetch User Notifications is unsuccessful because of invalid token");

	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T4970: Reward Transfer; SQ-T4979: Currency Transfer; SQ-T4972: Account History; "
			+ "SQ-T4975: User Balance; SQ-T4977: Checkins Balance; SQ-T4990: Fetch Offers", groups = "api", priority = 2)
	public void verifyAPIv1CheckinsNegative() throws InterruptedException {

		// User sign-up for user #1
		TestListeners.extentTest.get().info("== Mobile API v1: User #1 sign-up ==");
		logger.info("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// User sign-up for user #2
		TestListeners.extentTest.get().info("== Mobile API v1: User #2 sign-up ==");
		logger.info("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		TestListeners.extentTest.get().pass("API v1 user #2 signup is successful");
		logger.info("API v1 user #2 signup is successful");

		// Currency Transfer to other user with not enough reward balance available
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user with not enough reward balance available ==");
		logger.info("== Mobile API v1: Currency Transfer to other user with not enough reward balance available ==");
		Response currencyTransferResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferLowBalanceSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferLowBalanceSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String actualNotEnoughBalanceMsg = currencyTransferResponse.jsonPath().getString("[0]");
		String expectedNotEnoughBalanceMsg = "Not enough reward balance available to redeem. Current Balance of $0.00 is less than $"
				+ dataSet.get("amount") + " requested";
		Assert.assertEquals(actualNotEnoughBalanceMsg.toLowerCase(), expectedNotEnoughBalanceMsg.toLowerCase(),
				"Message did not match");
		TestListeners.extentTest.get().pass(
				"API v1 Currency Transfer to other user is unsuccessful because of not enough reward balance available");
		logger.info(
				"API v1 Currency Transfer to other user is unsuccessful because of not enough reward balance available");

		// Send reward amount to user #1
		TestListeners.extentTest.get().info("== Platform Functions: Send reward amount to user #1 ==");
		logger.info("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableID"), "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		TestListeners.extentTest.get().pass("Send reward amount to user #1 is successful");
		logger.info("Send reward amount to user #1 is successful");

		// Get Reward Id for user #1
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user #1 ==");
		logger.info("== Auth API: Get Reward Id for user #1 ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableID"));
		TestListeners.extentTest.get().info("Reward Id for user #1 is fetched: " + rewardId);
		logger.info("Reward Id for user #1 is fetched: " + rewardId);

		// Reward Transfer to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to user #2 ==");
		logger.info("== Mobile API v1: Reward Transfer to user #2 ==");
		Response rewardsTransferResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(dataSet.get("client"),
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Reward Transfer to other user");
		String actualRewardTransferMsg = rewardsTransferResponse.jsonPath().getString("[0]");
		String expectedRewardTransferMsg = dataSet.get("rewardName") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualRewardTransferMsg.toLowerCase(), expectedRewardTransferMsg.toLowerCase(),
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Reward Transfer to user #2 is successful");
		logger.info("Api v1 Reward Transfer to user #2 is successful");

		// Reward Transfer to other user using existing/invalid reward id
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Reward Transfer to other user using existing/invalid reward id ==");
		logger.info("== Mobile API v1: Reward Transfer to other user using existing/invalid reward id ==");
		Response rewardsTransferInvalidRewardIdResponse = pageObj.endpoints()
				.Api1GiftRewardToOtherUser(dataSet.get("client"), dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferInvalidRewardIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferInvalidRewardIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferInvalidRewardIdResponse.asString());
		Assert.assertTrue(isApi1RewardTransferInvalidRewardIdSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferInvalidRewardIdMsg = rewardsTransferInvalidRewardIdResponse.jsonPath().getString("[0]");
		Assert.assertEquals(rewardsTransferInvalidRewardIdMsg, "Reward doesn't exist or has already been redeemed.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Reward Transfer to user is unsuccessful because of invalid reward id");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of invalid reward id");

		// Reward Transfer to other user with missing reward id
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Reward Transfer to other user with missing reward id ==");
		logger.info("== Mobile API v1: Reward Transfer to other user with missing reward id ==");
		Response rewardsTransferMissingRewardIdResponse = pageObj.endpoints()
				.Api1GiftRewardToOtherUser(dataSet.get("client"), dataSet.get("secret"), token1, userEmail2, "");
		Assert.assertEquals(rewardsTransferMissingRewardIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferMissingRewardIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, rewardsTransferMissingRewardIdResponse.asString());
		Assert.assertTrue(isApi1RewardTransferMissingRewardIdSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferMissingRewardIdMsg = rewardsTransferMissingRewardIdResponse.jsonPath().getString("error");
		Assert.assertEquals(rewardsTransferMissingRewardIdMsg,
				"Required parameter missing or the value is empty: reward_to_transfer", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Reward Transfer to user is unsuccessful because of missing reward id");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of missing reward id");

		// Reward Transfer to other user using invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to other user with invalid email ==");
		logger.info("== Mobile API v1: Reward Transfer to other user with invalid email ==");
		Response rewardsTransferInvalidEmailResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(
				dataSet.get("client"), dataSet.get("secret"), token1, invalidEmail, rewardId);
		Assert.assertEquals(rewardsTransferInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1RewardTransferInvalidEmailSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferInvalidEmailMsg = rewardsTransferInvalidEmailResponse.jsonPath().getString("[0]");
		Assert.assertEquals(rewardsTransferInvalidEmailMsg,
				"Transfer cannot be processed as the email id doesn’t exist in our loyalty system.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Reward Transfer to user is unsuccessful because of invalid email");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of invalid email");

		// Reward Transfer to other user using missing email
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to other user with missing email ==");
		logger.info("== Mobile API v1: Reward Transfer to other user with missing email ==");
		Response rewardsTransferMissingEmailResponse = pageObj.endpoints()
				.Api1GiftRewardToOtherUser(dataSet.get("client"), dataSet.get("secret"), token1, "", rewardId);
		Assert.assertEquals(rewardsTransferMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, rewardsTransferMissingEmailResponse.asString());
		Assert.assertTrue(isApi1RewardTransferMissingEmailSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferMissingEmailMsg = rewardsTransferMissingEmailResponse.jsonPath().getString("error");
		Assert.assertEquals(rewardsTransferMissingEmailMsg,
				"Required parameter missing or the value is empty: recipient_email", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Reward Transfer to user is unsuccessful because of missing email");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of missing email");

		// Reward Transfer to other user using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to other user using invalid client ==");
		logger.info("== Mobile API v1: Reward Transfer to other user using invalid client ==");
		Response rewardsTransferInvalidClientResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(invalidValue,
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferInvalidClientResponse.asString());
		Assert.assertTrue(isApi1RewardTransferInvalidSignatureSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferInvalidClientMsg = rewardsTransferInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(rewardsTransferInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Reward Transfer to user is unsuccessful because of invalid client");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of invalid client");

		// Reward Transfer to other user using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to other user using invalid secret ==");
		logger.info("== Mobile API v1: Reward Transfer to other user using invalid secret ==");
		Response rewardsTransferInvalidSecretResponse = pageObj.endpoints()
				.Api1GiftRewardToOtherUser(dataSet.get("client"), invalidValue, token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Reward Transfer to other user");
		String rewardsTransferInvalidSecretMsg = rewardsTransferInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(rewardsTransferInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Reward Transfer to user is unsuccessful because of invalid secret");
		logger.info("Api v1 Reward Transfer to user is unsuccessful because of invalid secret");

		// Reward Transfer to other user using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Reward Transfer to other user using invalid token ==");
		logger.info("== Mobile API v1: Reward Transfer to other user using invalid token ==");
		Response rewardsTransferInvalidTokenResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(
				dataSet.get("client"), dataSet.get("secret"), invalidValue, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, rewardsTransferInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1RewardTransferInvalidTokenSchemaValidated,
				"API1 Reward Transfer Schema Validation failed");
		String rewardsTransferInvalidTokenMsg = rewardsTransferInvalidTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(rewardsTransferInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Reward Transfer to other user is unsuccessful because of invalid token");
		logger.info("Api v1 Reward Transfer to other user is unsuccessful because of invalid token");

		// Currency Transfer to other user using invalid recipient email
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user using invalid recipient email ==");
		logger.info("== Mobile API v1: Currency Transfer to other user using invalid recipient email ==");
		Response currencyTransferInvalidEmailResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(
				invalidEmail, dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferInvalidEmailSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferInvalidEmailMsg = currencyTransferInvalidEmailResponse.jsonPath().getString("[0]");
		Assert.assertEquals(currencyTransferInvalidEmailMsg,
				"Transfer cannot be processed as the email id doesn’t exist in our loyalty system.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of invalid recipient email");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of invalid recipient email");

		// Currency Transfer to other user with missing recipient email
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user with missing recipient email ==");
		logger.info("== Mobile API v1: Currency Transfer to other user with missing recipient email ==");
		Response currencyTransferMissingEmailResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser("",
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, currencyTransferMissingEmailResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferMissingEmailSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferMissingEmailMsg = currencyTransferMissingEmailResponse.jsonPath().getString("error");
		Assert.assertEquals(currencyTransferMissingEmailMsg,
				"Required parameter missing or the value is empty: recipient_email", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of missing recipient email");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of missing recipient email");

		// Currency Transfer to other user using invalid currency amount
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user using invalid currency amount ==");
		logger.info("== Mobile API v1: Currency Transfer to other user using invalid currency amount ==");
		Response currencyTransferInvalidAmountResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				invalidValue, dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferInvalidAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferInvalidAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferInvalidAmountResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferInvalidAmountSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferInvalidAmountMsg = currencyTransferInvalidAmountResponse.jsonPath().getString("[0]");
		Assert.assertEquals(currencyTransferInvalidAmountMsg, "Invalid transfer amount", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of invalid currency amount");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of invalid currency amount");

		// Currency Transfer to other user with missing currency amount
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user with missing currency amount ==");
		logger.info("== Mobile API v1: Currency Transfer to other user with missing currency amount ==");
		Response currencyTransferMissingAmountResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				"", dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, currencyTransferMissingAmountResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferMissingAmountSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferMissingAmountMsg = currencyTransferMissingAmountResponse.jsonPath().getString("error");
		Assert.assertEquals(currencyTransferMissingAmountMsg,
				"Required parameter missing or the value is empty: amount_to_transfer", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of missing currency amount");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of missing currency amount");

		// Currency Transfer to other user using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user using invalid client ==");
		logger.info("== Mobile API v1: Currency Transfer to other user using invalid client ==");
		Response currencyTransferInvalidClientResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), invalidValue, dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferInvalidClientResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferInvalidSignatureSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferInvalidClientMsg = currencyTransferInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(currencyTransferInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of invalid client");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of invalid client");

		// Currency Transfer to other user using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Currency Transfer to other user using invalid secret ==");
		logger.info("== Mobile API v1: Currency Transfer to other user using invalid secret ==");
		Response currencyTransferInvalidSecretResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), invalidValue, token1);
		Assert.assertEquals(currencyTransferInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Currency Transfer to other user");
		String currencyTransferInvalidSecretMsg = currencyTransferInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(currencyTransferInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of invalid secret");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of invalid secret");

		// Currency Transfer to other user using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Currency Transfer to other user using invalid token ==");
		logger.info("== Mobile API v1: Currency Transfer to other user using invalid token ==");
		Response currencyTransferInvalidTokenResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), invalidValue);
		Assert.assertEquals(currencyTransferInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, currencyTransferInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferInvalidTokenSchemaValidated,
				"API1 Currency Transfer Schema Validation failed");
		String currencyTransferInvalidTokenMsg = currencyTransferInvalidTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(currencyTransferInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Currency Transfer to other user is unsuccessful because of invalid token");
		logger.info("API v1 Currency Transfer to other user is unsuccessful because of invalid token");

		// Fetch User Offers using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Offers using invalid client ==");
		logger.info("== Mobile API v1: Fetch User Offers using invalid client ==");
		Response fetchUserOffersInvalidClientResponse = pageObj.endpoints().api1FetchUserOffers(invalidValue,
				dataSet.get("secret"), token2);
		Assert.assertEquals(fetchUserOffersInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch User Offers");
		boolean isApi1FetchUserOffersInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, fetchUserOffersInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchUserOffersInvalidSignatureSchemaValidated,
				"API1 Fetch User Offers Schema Validation failed");
		String fetchUserOffersInvalidClientMsg = fetchUserOffersInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(fetchUserOffersInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch User Offers is unsuccessful because of invalid client");
		logger.info("API v1 Fetch User Offers is unsuccessful because of invalid client");

		// Fetch User Offers using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Offers using invalid secret ==");
		logger.info("== Mobile API v1: Fetch User Offers using invalid secret ==");
		Response fetchUserOffersInvalidSecretResponse = pageObj.endpoints().api1FetchUserOffers(dataSet.get("client"),
				invalidValue, token2);
		Assert.assertEquals(fetchUserOffersInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch User Offers");
		String fetchUserOffersInvalidSecretMsg = fetchUserOffersInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(fetchUserOffersInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch User Offers is unsuccessful because of invalid secret");
		logger.info("API v1 Fetch User Offers is unsuccessful because of invalid secret");

		// Fetch User Offers using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Offers using invalid token ==");
		logger.info("== Mobile API v1: Fetch User Offers using invalid token ==");
		Response fetchUserOffersInvalidTokenResponse = pageObj.endpoints().api1FetchUserOffers(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(fetchUserOffersInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch User Offers");
		boolean isApi1FetchUserOffersInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, fetchUserOffersInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchUserOffersInvalidTokenSchemaValidated,
				"API1 Fetch User Offers Schema Validation failed");
		String fetchUserOffersInvalidTokenMsg = fetchUserOffersInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(fetchUserOffersInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch User Offers is unsuccessful because of invalid token");
		logger.info("API v1 Fetch User Offers is unsuccessful because of invalid token");

		// Fetch User Account history (/api/mobile/accounts) using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid client ==");
		logger.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid client ==");
		Response accountsInvalidClientResponse = pageObj.endpoints().Api1MobileAccounts(token2, invalidValue,
				dataSet.get("secret"));
		Assert.assertEquals(accountsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for api v1 Fetch user account history");
		boolean isApi1FetchUserAccountsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, accountsInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchUserAccountsInvalidSignatureSchemaValidated,
				"API1 Fetch User account history Schema Validation failed");
		String accountsInvalidClientMsg = accountsInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(accountsInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Fetch User account history is unsuccessful because of invalid client");
		logger.info("Api v1 Fetch User account history is unsuccessful because of invalid client");

		// Fetch User Account history (/api/mobile/accounts) using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid secret ==");
		logger.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid secret ==");
		Response accountsInvalidSecretResponse = pageObj.endpoints().Api1MobileAccounts(token2, dataSet.get("client"),
				invalidValue);
		Assert.assertEquals(accountsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for api v1 Fetch user account history");
		String accountsInvalidSecretMsg = accountsInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(accountsInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Fetch User account history is unsuccessful because of invalid secret");
		logger.info("Api v1 Fetch User account history is unsuccessful because of invalid secret");

		// Fetch User Account history (/api/mobile/accounts) using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid token ==");
		logger.info("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) using invalid token ==");
		Response accountsInvalidTokenResponse = pageObj.endpoints().Api1MobileAccounts(invalidValue,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(accountsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for api v1 Fetch user account history");
		boolean isApi1FetchUserAccountsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, accountsInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchUserAccountsInvalidTokenSchemaValidated,
				"API1 Fetch User account history Schema Validation failed");
		String accountsInvalidTokenMsg = accountsInvalidTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(accountsInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("Api v1 Fetch User account history is unsuccessful because of invalid token");
		logger.info("Api v1 Fetch User account history is unsuccessful because of invalid token");

		// Fetch User balance (/api/mobile/users/balance) using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid client ==");
		logger.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid client ==");
		Response userBalanceInvalidClientResponse = pageObj.endpoints().Api1MobileUsersbalance(token2, invalidValue,
				dataSet.get("secret"));
		Assert.assertEquals(userBalanceInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch users balance");
		boolean isApi1FetchUserBalanceInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userBalanceInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchUserBalanceInvalidSignatureSchemaValidated,
				"API1 Fetch User balance Schema Validation failed");
		String userBalanceInvalidClientMsg = userBalanceInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(userBalanceInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch User balance is unsuccessful because of invalid client");
		logger.info("Api v1 Fetch User balance is unsuccessful because of invalid client");

		// Fetch User balance (/api/mobile/users/balance) using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid secret ==");
		logger.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid secret ==");
		Response userBalanceInvalidSecretResponse = pageObj.endpoints().Api1MobileUsersbalance(token2,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(userBalanceInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch users balance");
		String userBalanceInvalidSecretMsg = userBalanceInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(userBalanceInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Fetch User balance is unsuccessful because of invalid secret");
		logger.info("Api v1 Fetch User balance is unsuccessful because of invalid secret");

		// Fetch User balance (/api/mobile/users/balance) using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid token ==");
		logger.info("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) using invalid token ==");
		Response userBalanceInvalidTokenResponse = pageObj.endpoints().Api1MobileUsersbalance(invalidValue,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(userBalanceInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch users balance");
		TestListeners.extentTest.get().pass("Api v1 Fetch User balance is unsuccessful because of invalid token");
		logger.info("Api v1 Fetch User balance is unsuccessful because of invalid token");

		// Fetch User Checkins Balance using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Checkins Balance using invalid client ==");
		logger.info("== Mobile API v1: Fetch User Checkins Balance using invalid client ==");
		Response checkinsBalanceInvalidClientResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token2,
				invalidValue, dataSet.get("secret"));
		Assert.assertEquals(checkinsBalanceInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch user Checkins Balance");
		boolean isApi1FetchUserCheckinsBalanceInvalidSignatureSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema,
						checkinsBalanceInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchUserCheckinsBalanceInvalidSignatureSchemaValidated,
				"API1 Fetch User Checkins Balance Schema Validation failed");
		String checkinsBalanceInvalidClientMsg = checkinsBalanceInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(checkinsBalanceInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid client");
		logger.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid client");

		// Fetch User Checkins Balance using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Checkins Balance using invalid secret ==");
		logger.info("== Mobile API v1: Fetch User Checkins Balance using invalid secret ==");
		Response checkinsBalanceInvalidSecretResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token2,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(checkinsBalanceInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch user Checkins Balance");
		String checkinsBalanceInvalidSecretMsg = checkinsBalanceInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(checkinsBalanceInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid secret");
		logger.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid secret");

		// Fetch User Checkins Balance using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch User Checkins Balance using invalid token ==");
		logger.info("== Mobile API v1: Fetch User Checkins Balance using invalid token ==");
		Response checkinsBalanceInvalidTokenResponse = pageObj.endpoints().Api1MobileCheckinsBalance(invalidValue,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinsBalanceInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch user Checkins Balance");
		boolean isApi1FetchUserCheckinsBalanceInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, checkinsBalanceInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchUserCheckinsBalanceInvalidTokenSchemaValidated,
				"API1 Fetch User Checkins Balance Schema Validation failed");
		String checkinsBalanceInvalidTokenMsg = checkinsBalanceInvalidTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(checkinsBalanceInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid token");
		logger.info("API v1 Fetch User Checkins Balance is unsuccessful because of invalid token");
	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T4986: Version Notes; SQ-T4951: Meta API; SQ-T4992: Migration User Look-up; "
			+ "SQ-T4994: Generate OTP token; SQ-T5003: Beacon Entry; SQ-T5005: Beacon Exit", groups = "api", priority = 3)
	public void verifyAPIv1VersionNotesNegative() {

		// Create Business Migration User
		TestListeners.extentTest.get().info("== Platform Functions: Create Business Migration User ==");
		logger.info("== Platform Functions: Create Business Migration User ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("apiKey"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Create Business Migration User API");
		String card_number = createMigrationUserResponse.jsonPath().get("original_membership_no");
		String email = createMigrationUserResponse.jsonPath().get("email").toString();
		TestListeners.extentTest.get().pass("Create Business Migration User call is successful");
		logger.info("Create Business Migration User call is successful");

		// Migration user look-up with invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: Migration user look-up with invalid email ==");
		logger.info("== Mobile API v1: Migration user look-up with invalid email ==");
		Response migrationLookupInvalidEmailResponse = pageObj.endpoints().api1UserMigrationLookup(card_number,
				invalidEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Migration user look-up");
		boolean isApi1MigrationLookupInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, migrationLookupInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1MigrationLookupInvalidEmailSchemaValidated,
				"API1 Migration user look-up Schema Validation failed");
		String migrationLookupInvalidEmailMsg = migrationLookupInvalidEmailResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(migrationLookupInvalidEmailMsg, "Incorrect information submitted. Please retry.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Migration user look-up call is unsuccessful because of invalid email");
		logger.info("Migration user look-up call is unsuccessful because of invalid email");

		// Migration user look-up with missing email
		TestListeners.extentTest.get().info("== Mobile API v1: Migration user look-up with missing email ==");
		logger.info("== Mobile API v1: Migration user look-up with missing email ==");
		Response migrationLookupMissingEmailResponse = pageObj.endpoints().api1UserMigrationLookup(card_number, "",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Migration user look-up");
		String migrationLookupMissingEmailMsg = migrationLookupMissingEmailResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(migrationLookupMissingEmailMsg, "Incorrect information submitted. Please retry.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Migration user look-up call is unsuccessful because of missing email");
		logger.info("Migration user look-up call is unsuccessful because of missing email");

		// Migration user look-up using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Migration user look-up using invalid client ==");
		logger.info("== Mobile API v1: Migration user look-up using invalid client ==");
		Response migrationLookupInvalidClientResponse = pageObj.endpoints().api1UserMigrationLookup(card_number, email,
				invalidValue, dataSet.get("secret"));
		Assert.assertEquals(migrationLookupInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Migration user look-up");
		boolean isApi1MigrationLookupInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, migrationLookupInvalidClientResponse.asString());
		Assert.assertTrue(isApi1MigrationLookupInvalidSignatureSchemaValidated,
				"API1 Migration user look-up Schema Validation failed");
		String migrationLookupInvalidClientMsg = migrationLookupInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(migrationLookupInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Migration user look-up call is unsuccessful because of invalid client");
		logger.info("Migration user look-up call is unsuccessful because of invalid client");

		// Migration user look-up using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Migration user look-up using invalid secret ==");
		logger.info("== Mobile API v1: Migration user look-up using invalid secret ==");
		Response migrationLookupInvalidSecretResponse = pageObj.endpoints().api1UserMigrationLookup(card_number, email,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(migrationLookupInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Migration user look-up");
		String migrationLookupInvalidSecretMsg = migrationLookupInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(migrationLookupInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Migration user look-up call is unsuccessful because of invalid secret");
		logger.info("Migration user look-up call is unsuccessful because of invalid secret");

		// Meta API using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Meta API using invalid client ==");
		logger.info("== Mobile API v1: Meta API using invalid client ==");
		Response metaApiInvalidClientResponse = pageObj.endpoints().Api1Cards(invalidValue, dataSet.get("secret"));
		Assert.assertEquals(metaApiInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Api v1 Meta API");
		boolean isApi1MetaApiInvalidSignatureSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1MetaApiCardsSchema, metaApiInvalidClientResponse.asString());
		Assert.assertTrue(isApi1MetaApiInvalidSignatureSchemaValidated, "API1 Meta API Schema Validation failed");
		String metaApiInvalidClientMsg = metaApiInvalidClientResponse.jsonPath().get("cards[0]").toString();
		Assert.assertEquals(metaApiInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Meta API is unsuccessful because of invalid client");
		logger.info("Api v1 Meta API is unsuccessful because of invalid client");

		// Meta API using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Meta API using invalid secret ==");
		logger.info("== Mobile API v1: Meta API using invalid secret ==");
		Response metaApiInvalidSecretResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), invalidValue);
		Assert.assertEquals(metaApiInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Api v1 Meta API");
		String metaApiInvalidSecretMsg = metaApiInvalidSecretResponse.jsonPath().get("cards[0]").toString();
		Assert.assertEquals(metaApiInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Api v1 Meta API is unsuccessful because of invalid secret");
		logger.info("Api v1 Meta API is unsuccessful because of invalid secret");

		// Version Notes with missing version
		TestListeners.extentTest.get().info("== Mobile API v1: Version Notes with missing version ==");
		logger.info("== Mobile API v1: Version Notes with missing version ==");
		Response versionNotesMissingVersionResponse = pageObj.endpoints().api1VersionNotes("", dataSet.get("os"),
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingVersionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isApi1VersionNotesMissingVersionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingVersionSchema,
				versionNotesMissingVersionResponse.asString());
		Assert.assertTrue(isApi1VersionNotesMissingVersionSchemaValidated,
				"API1 Version Notes Schema Validation failed");
		String versionNotesMissingVersionMsg = versionNotesMissingVersionResponse.jsonPath().get("errors.version")
				.toString();
		Assert.assertEquals(versionNotesMissingVersionMsg, "Required parameter missing or the value is empty.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Version Notes call is unsuccessful because of missing version");
		logger.info("Version Notes call is unsuccessful because of missing version");

		// Version Notes with missing OS
		TestListeners.extentTest.get().info("== Mobile API v1: Version Notes with missing OS ==");
		logger.info("== Mobile API v1: Version Notes with missing OS ==");
		Response versionNotesMissingOsResponse = pageObj.endpoints().api1VersionNotes(dataSet.get("version"), "",
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingOsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isApi1VersionNotesMissingOsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingOsSchema, versionNotesMissingOsResponse.asString());
		Assert.assertTrue(isApi1VersionNotesMissingOsSchemaValidated, "API1 Version Notes Schema Validation failed");
		String versionNotesMissingOsMsg = versionNotesMissingOsResponse.jsonPath().get("errors.os").toString();
		Assert.assertEquals(versionNotesMissingOsMsg, "Required parameter missing or the value is empty.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Version Notes call is unsuccessful because of missing OS");
		logger.info("Version Notes call is unsuccessful because of missing OS");

		// Version Notes using invalid OS
		TestListeners.extentTest.get().info("== Mobile API v1: Version Notes using invalid OS ==");
		logger.info("== Mobile API v1: Version Notes using invalid OS ==");
		Response versionNotesInvalidOsResponse = pageObj.endpoints().api1VersionNotes(dataSet.get("version"),
				dataSet.get("model"), dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesInvalidOsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isApi1VersionNotesInvalidOsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesInvalidOsSchema, versionNotesInvalidOsResponse.asString());
		Assert.assertTrue(isApi1VersionNotesInvalidOsSchemaValidated, "API1 Version Notes Schema Validation failed");
		String versionNotesInvalidOsMsg = versionNotesInvalidOsResponse.jsonPath().get("os").toString();
		Assert.assertEquals(versionNotesInvalidOsMsg, "Operating System should be either android or ios.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Version Notes call is unsuccessful because of invalid OS");
		logger.info("Version Notes call is unsuccessful because of invalid OS");

		// Version Notes with missing model
		TestListeners.extentTest.get().info("== Mobile API v1: Version Notes with missing model ==");
		logger.info("== Mobile API v1: Version Notes with missing model ==");
		Response versionNotesMissingModelResponse = pageObj.endpoints().api1VersionNotes(dataSet.get("version"),
				dataSet.get("os"), "", dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingModelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isApi1VersionNotesMissingModelSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingModelSchema, versionNotesMissingModelResponse.asString());
		Assert.assertTrue(isApi1VersionNotesMissingModelSchemaValidated, "API1 Version Notes Schema Validation failed");
		String versionNotesMissingModelMsg = versionNotesMissingModelResponse.jsonPath().get("errors.model").toString();
		Assert.assertEquals(versionNotesMissingModelMsg, "Required parameter missing or the value is empty.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Version Notes call is unsuccessful because of missing model");
		logger.info("Version Notes call is unsuccessful because of missing model");

		// Version Notes with missing client
		TestListeners.extentTest.get().info("== Mobile API v1: Version Notes with missing client ==");
		logger.info("== Mobile API v1: Version Notes with missing client ==");
		Response versionNotesMissingClientResponse = pageObj.endpoints().api1VersionNotes(dataSet.get("version"),
				dataSet.get("os"), dataSet.get("model"), "");
		Assert.assertEquals(versionNotesMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isApi1VersionNotesMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingClientSchema,
				versionNotesMissingClientResponse.asString());
		Assert.assertTrue(isApi1VersionNotesMissingClientSchemaValidated,
				"API1 Version Notes Schema Validation failed");
		String versionNotesMissingClientMsg = versionNotesMissingClientResponse.jsonPath().get("errors.client")
				.toString();
		Assert.assertEquals(versionNotesMissingClientMsg, "Required parameter missing or the value is empty.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Version Notes call is unsuccessful because of missing client");
		logger.info("Version Notes call is unsuccessful because of missing client");

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// Generate OTP Token using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Generate OTP Token using invalid client ==");
		logger.info("== Mobile API v1: Generate OTP Token using invalid client ==");
		Response generateOtpTokenInvalidClientResponse = pageObj.endpoints().api1GenerateOtpToken(invalidValue,
				dataSet.get("secret"), token);
		Assert.assertEquals(generateOtpTokenInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Generate OTP Token");
		boolean isApi1GenerateOtpTokenInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, generateOtpTokenInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GenerateOtpTokenInvalidSignatureSchemaValidated,
				"API1 Generate OTP Token Schema Validation failed");
		String generateOtpTokenInvalidClientMsg = generateOtpTokenInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(generateOtpTokenInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Generate OTP Token is unsuccessful because of invalid client");
		logger.info("Generate OTP Token is unsuccessful because of invalid client");

		// Generate OTP Token using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Generate OTP Token using invalid secret ==");
		logger.info("== Mobile API v1: Generate OTP Token using invalid secret ==");
		Response generateOtpTokenInvalidSecretResponse = pageObj.endpoints().api1GenerateOtpToken(dataSet.get("client"),
				invalidValue, token);
		Assert.assertEquals(generateOtpTokenInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Generate OTP Token");
		String generateOtpTokenInvalidSecretMsg = generateOtpTokenInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(generateOtpTokenInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Generate OTP Token is unsuccessful because of invalid secret");
		logger.info("Generate OTP Token is unsuccessful because of invalid secret");

		// Generate OTP Token using invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Generate OTP Token using invalid user access token ==");
		logger.info("== Mobile API v1: Generate OTP Token using invalid user access token ==");
		Response generateOtpTokenInvalidTokenResponse = pageObj.endpoints().api1GenerateOtpToken(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(generateOtpTokenInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for Generate OTP Token");
		boolean isApi1GenerateOtpTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, generateOtpTokenInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GenerateOtpTokenInvalidTokenSchemaValidated,
				"API1 Generate OTP Token Schema Validation failed");
		String generateOtpTokenInvalidTokenMsg = generateOtpTokenInvalidTokenResponse.jsonPath().getString("error");
		Assert.assertEquals(generateOtpTokenInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("Generate OTP Token is unsuccessful because of invalid user access token");
		logger.info("Generate OTP Token is unsuccessful because of invalid user access token");

		// Beacon Entry using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Beacon Entry using invalid client ==");
		logger.info("== Mobile API v1: Beacon Entry using invalid client ==");
		Response beaconEntryInvalidClientResponse = pageObj.endpoints().api1BeaconEntry(invalidValue,
				dataSet.get("secret"), token, dataSet.get("beaconEntryIDs"));
		Assert.assertEquals(beaconEntryInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Beacon Entry");
		boolean isApi1BeaconEntryInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, beaconEntryInvalidClientResponse.asString());
		Assert.assertTrue(isApi1BeaconEntryInvalidSignatureSchemaValidated,
				"API1 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidClientMsg = beaconEntryInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(beaconEntryInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Beacon Entry is unsuccessful because of invalid client");
		logger.info("Beacon Entry is unsuccessful because of invalid client");

		// Beacon Entry using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Beacon Entry using invalid secret ==");
		logger.info("== Mobile API v1: Beacon Entry using invalid secret ==");
		Response beaconEntryInvalidSecretResponse = pageObj.endpoints().api1BeaconEntry(dataSet.get("client"),
				invalidValue, token, dataSet.get("beaconEntryIDs"));
		Assert.assertEquals(beaconEntryInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Beacon Entry");
		String beaconEntryInvalidSecretMsg = beaconEntryInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(beaconEntryInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Beacon Entry is unsuccessful because of invalid secret");
		logger.info("Beacon Entry is unsuccessful because of invalid secret");

		// Beacon Exit using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Beacon Exit using invalid client ==");
		logger.info("== Mobile API v1: Beacon Exit using invalid client ==");
		Response beaconExitInvalidClientResponse = pageObj.endpoints().api1BeaconExit(invalidValue,
				dataSet.get("secret"), token, dataSet.get("beaconExitIDs"));
		Assert.assertEquals(beaconExitInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Beacon Exit");
		boolean isApi1BeaconExitInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, beaconExitInvalidClientResponse.asString());
		Assert.assertTrue(isApi1BeaconExitInvalidSignatureSchemaValidated, "API1 Beacon Exit Schema Validation failed");
		String beaconExitInvalidClientMsg = beaconExitInvalidClientResponse.jsonPath().getString("[0]");
		Assert.assertEquals(beaconExitInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Beacon Exit is unsuccessful because of invalid client");
		logger.info("Beacon Exit is unsuccessful because of invalid client");

		// Beacon Exit using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Beacon Exit using invalid secret ==");
		logger.info("== Mobile API v1: Beacon Exit using invalid secret ==");
		Response beaconExitInvalidSecretResponse = pageObj.endpoints().api1BeaconExit(dataSet.get("client"),
				invalidValue, token, dataSet.get("beaconExitIDs"));
		Assert.assertEquals(beaconExitInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for Beacon Exit");
		String beaconExitInvalidSecretMsg = beaconExitInvalidSecretResponse.jsonPath().getString("[0]");
		Assert.assertEquals(beaconExitInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("Beacon Exit is unsuccessful because of invalid secret");
		logger.info("Beacon Exit is unsuccessful because of invalid secret");

	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T5007: Gaming Achievements; SQ-T5024: Get Scratch Board", groups = "api", priority = 4)
	public void verifyAPIv1GamingAchievementsNegative() {

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// Gaming Achievements using missing parameter: kind
		TestListeners.extentTest.get().info("== Mobile API v1: Gaming Achievements using missing parameter: kind ==");
		logger.info("== Mobile API v1: Gaming Achievements using missing parameter: kind ==");
		Response gamingAchievementsMissingKindParamResponse = pageObj.endpoints().APi1GamingAchievements(
				dataSet.get("client"), dataSet.get("secret"), token, "", dataSet.get("level"), dataSet.get("score"),
				dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsMissingKindParamResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Gaming Achievements");
		TestListeners.extentTest.get()
				.pass("API v1 Gaming Achievements is unsuccessful because of missing parameter: kind");
		logger.info("API v1 Gaming Achievements is unsuccessful because of missing parameter: kind");

		// Gaming Achievements using missing parameter: level
		TestListeners.extentTest.get().info("== Mobile API v1: Gaming Achievements using missing parameter: level ==");
		logger.info("== Mobile API v1: Gaming Achievements using missing parameter: level ==");
		Response gamingAchievementsMissingLevelParamResponse = pageObj.endpoints().APi1GamingAchievements(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kind"), "", dataSet.get("score"),
				dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsMissingLevelParamResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Gaming Achievements");
		TestListeners.extentTest.get()
				.pass("API v1 Gaming Achievements is unsuccessful because of missing parameter: level");
		logger.info("API v1 Gaming Achievements is unsuccessful because of missing parameter: level");

		// Gaming Achievements using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Gaming Achievements using invalid client ==");
		logger.info("== Mobile API v1: Gaming Achievements using invalid client ==");
		Response gamingAchievementsInvalidClientResponse = pageObj.endpoints().APi1GamingAchievements(invalidValue,
				dataSet.get("secret"), token, dataSet.get("kind"), dataSet.get("level"), dataSet.get("score"),
				dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Gaming Achievements");
		boolean isApi1GamingAchievementsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, gamingAchievementsInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GamingAchievementsInvalidSignatureSchemaValidated,
				"API1 Gaming Achievements Schema Validation failed");
		String gamingAchievementsInvalidClientMsg = gamingAchievementsInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(gamingAchievementsInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Gaming Achievements is unsuccessful because of invalid client");
		logger.info("API v1 Gaming Achievements is unsuccessful because of invalid client");

		// Gaming Achievements using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Gaming Achievements using invalid secret ==");
		logger.info("== Mobile API v1: Gaming Achievements using invalid secret ==");
		Response gamingAchievementsInvalidSecretResponse = pageObj.endpoints().APi1GamingAchievements(
				dataSet.get("client"), invalidValue, token, dataSet.get("kind"), dataSet.get("level"),
				dataSet.get("score"), dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Gaming Achievements");
		String gamingAchievementsInvalidSecretMsg = gamingAchievementsInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(gamingAchievementsInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Gaming Achievements is unsuccessful because of invalid secret");
		logger.info("API v1 Gaming Achievements is unsuccessful because of invalid secret");

		// Gaming Achievements using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Gaming Achievements using invalid token ==");
		logger.info("== Mobile API v1: Gaming Achievements using invalid token ==");
		Response gamingAchievementsInvalidTokenResponse = pageObj.endpoints().APi1GamingAchievements(
				dataSet.get("client"), dataSet.get("secret"), invalidValue, dataSet.get("kind"), dataSet.get("level"),
				dataSet.get("score"), dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Gaming Achievements");
		boolean isApi1GamingAchievementsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, gamingAchievementsInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GamingAchievementsInvalidTokenSchemaValidated,
				"API1 Gaming Achievements Schema Validation failed");
		String gamingAchievementsInvalidTokenMsg = gamingAchievementsInvalidTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(gamingAchievementsInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Gaming Achievements is unsuccessful because of invalid token");
		logger.info("API v1 Gaming Achievements is unsuccessful because of invalid token");

		// Get Scratch Board using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Get Scratch Board using invalid client ==");
		logger.info("== Mobile API v1: Get Scratch Board using invalid client ==");
		Response getScratchBoardInvalidClientResponse = pageObj.endpoints().api1GetScratchBoard(invalidValue,
				dataSet.get("secret"), token);
		Assert.assertEquals(getScratchBoardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Scratch Board");
		boolean isApi1GetScratchBoardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getScratchBoardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GetScratchBoardInvalidSignatureSchemaValidated,
				"API1 Get Scratch Board Schema Validation failed");
		String getScratchBoardInvalidClientMsg = getScratchBoardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getScratchBoardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Get Scratch Board is unsuccessful because of invalid client");
		logger.info("API v1 Get Scratch Board is unsuccessful because of invalid client");

		// Get Scratch Board using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Get Scratch Board using invalid secret ==");
		logger.info("== Mobile API v1: Get Scratch Board using invalid secret ==");
		Response getScratchBoardInvalidSecretResponse = pageObj.endpoints().api1GetScratchBoard(dataSet.get("client"),
				invalidValue, token);
		Assert.assertEquals(getScratchBoardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Get Scratch Board");
		String getScratchBoardInvalidSecretMsg = getScratchBoardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getScratchBoardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Get Scratch Board is unsuccessful because of invalid secret");
		logger.info("API v1 Get Scratch Board is unsuccessful because of invalid secret");

		// Get Scratch Board using invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Get Scratch Board using invalid user access token ==");
		logger.info("== Mobile API v1: Get Scratch Board using invalid user access token ==");
		Response getScratchBoardInvalidTokenResponse = pageObj.endpoints().api1GetScratchBoard(dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(getScratchBoardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Get Scratch Board");
		boolean isApi1GetScratchBoardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getScratchBoardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GetScratchBoardInvalidTokenSchemaValidated,
				"API1 Get Scratch Board Schema Validation failed");
		String getScratchBoardInvalidTokenMsg = getScratchBoardInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(getScratchBoardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Get Scratch Board is unsuccessful because of invalid user access token");
		logger.info("API v1 Get Scratch Board is unsuccessful because of invalid user access token");

	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T5011: Transfer Loyalty Points to User; SQ-T5082: Import Gift Card", groups = "api", priority = 5)
	public void verifyAPIv1TransferLoyaltyPointsToUserNegative() {

		// User sign-up for user #1
		TestListeners.extentTest.get().info("== Mobile API v1: User #1 sign-up ==");
		logger.info("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful with user id: " + userID1);
		logger.info("API v1 user #1 signup is successful with user id: " + userID1);

		// User sign-up for user #2
		TestListeners.extentTest.get().info("== Mobile API v1: User #2 sign-up ==");
		logger.info("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID2 = signUpResponse2.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user #2 signup is successful with user id: " + userID2);
		logger.info("API v1 user #2 signup is successful with user id: " + userID2);

		// Loyalty points transfer to user #2 when not having enough balance
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 when not having enough balance ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 when not having enough balance ==");
		Response transferPointsLowBalanceResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsLowBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsLowBalanceSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsLowBalanceResponse.asString());
		Assert.assertTrue(isApi1TransferPointsLowBalanceSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String actualTransferPointsLowBalanceMsg = transferPointsLowBalanceResponse.jsonPath().get("[0]").toString();
		String expectedTransferPointsLowBalanceMsg = "Not enough reward balance available to redeem.";
		Assert.assertTrue(actualTransferPointsLowBalanceMsg.contains(expectedTransferPointsLowBalanceMsg),
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of not having enough balance");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of not having enough balance");

		// Send reward amount to user #1
		TestListeners.extentTest.get().info("== Platform Functions: Send reward amount to user #1 ==");
		logger.info("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "",
				"", "", dataSet.get("amount"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		TestListeners.extentTest.get().pass("Send reward amount to user #1 is successful");
		logger.info("Send reward amount to user #1 is successful");

		// Loyalty points transfer to user #2 using invalid email
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid email ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid points ==");
		Response transferPointsInvalidEmailResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(invalidEmail,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1TransferPointsInvalidEmailSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsInvalidEmailMsg = transferPointsInvalidEmailResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(transferPointsInvalidEmailMsg,
				"Transfer cannot be processed as the email id doesn’t exist in our loyalty system.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid email");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid email");

		// Loyalty points transfer to user #2 with missing email
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 with missing email ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 with missing email ==");
		Response transferPointsMissingEmailResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser("",
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, transferPointsMissingEmailResponse.asString());
		Assert.assertTrue(isApi1TransferPointsMissingEmailSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsMissingEmailMsg = transferPointsMissingEmailResponse.jsonPath().get("error").toString();
		Assert.assertEquals(transferPointsMissingEmailMsg,
				"Required parameter missing or the value is empty: recipient_email", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of missing email");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of missing email");

		// Loyalty points transfer to user #2 using invalid points
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid points ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid points ==");
		Response transferPointsInvalidPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				invalidValue, dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsInvalidPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsInvalidPointsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsInvalidPointsResponse.asString());
		Assert.assertTrue(isApi1TransferPointsInvalidPointsSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsInvalidPointsMsg = transferPointsInvalidPointsResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(transferPointsInvalidPointsMsg, "Invalid transfer points", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid points");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid points");

		// Loyalty points transfer to user #2 with missing points
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 with missing points ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 with missing points ==");
		Response transferPointsMissingPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				"", dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsMissingPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsMissingPointsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, transferPointsMissingPointsResponse.asString());
		Assert.assertTrue(isApi1TransferPointsMissingPointsSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsMissingPointsMsg = transferPointsMissingPointsResponse.jsonPath().get("error").toString();
		Assert.assertEquals(transferPointsMissingPointsMsg,
				"Required parameter missing or the value is empty: points_to_transfer", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of missing points");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of missing points");

		// Loyalty points transfer to user #2 using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid client ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid client ==");
		Response transferPointsInvalidClientResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("amount"), invalidValue, dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsInvalidClientResponse.asString());
		Assert.assertTrue(isApi1TransferPointsInvalidSignatureSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsInvalidClientMsg = transferPointsInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(transferPointsInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid client");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid client");

		// Loyalty points transfer to user #2 using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid secret ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid secret ==");
		Response transferPointsInvalidSecretResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), invalidValue, token1);
		Assert.assertEquals(transferPointsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 transfer loyalty points to other user");
		String transferPointsInvalidSecretMsg = transferPointsInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(transferPointsInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid secret");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid secret");

		// Loyalty points transfer to user #2 using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid token ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 using invalid token ==");
		Response transferPointsInvalidTokenResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), invalidValue);
		Assert.assertEquals(transferPointsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, transferPointsInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1TransferPointsInvalidTokenSchemaValidated,
				"API1 Transfer Loyalty Points Schema Validation failed");
		String transferPointsInvalidTokenMsg = transferPointsInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(transferPointsInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid token");
		logger.info("API v1 Loyalty points transfer to user #2 is unsuccessful because of invalid token");

		// Import Gift Card with missing design_id
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card with missing design_id ==");
		logger.info("== Mobile API v1: Import Gift Card with missing design_id ==");
		Response importGiftCardMissingDesignIdResponse = pageObj.endpoints().api1ImportGiftCard("",
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardMissingDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardMissingDesignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, importGiftCardMissingDesignIdResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardMissingDesignIdSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardMissingDesignIdMsg = importGiftCardMissingDesignIdResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(importGiftCardMissingDesignIdMsg,
				"Required parameter missing or the value is empty: design_id", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of missing design_id");
		logger.info("API v1 Import Gift Card call is unsuccessful because of missing design_id");

		// Import Gift Card with missing card_number
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card with missing card_number ==");
		logger.info("== Mobile API v1: Import Gift Card with missing card_number ==");
		Response importGiftCardMissingCardNumberResponse = pageObj.endpoints().api1ImportGiftCard(
				dataSet.get("designId"), "", dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardMissingCardNumberResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardMissingCardNumberSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, importGiftCardMissingCardNumberResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardMissingCardNumberSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardMissingCardNumberMsg = importGiftCardMissingCardNumberResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(importGiftCardMissingCardNumberMsg,
				"Required parameter missing or the value is empty: card_number", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of missing card_number");
		logger.info("API v1 Import Gift Card call is unsuccessful because of missing card_number");

		// Import Gift Card using invalid design_id
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using invalid design_id ==");
		logger.info("== Mobile API v1: Import Gift Card using invalid design_id ==");
		Response importGiftCardInvalidDesignIdResponse = pageObj.endpoints().api1ImportGiftCard(invalidValue,
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardInvalidDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardInvalidDesignIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, importGiftCardInvalidDesignIdResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardInvalidDesignIdSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardInvalidDesignIdMsg = importGiftCardInvalidDesignIdResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(importGiftCardInvalidDesignIdMsg,
				"Card design unavailable! Please restart application to refresh gift card design data.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using invalid design_id");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using invalid design_id");

		// Import Gift Card using invalid card_number
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using invalid card_number ==");
		logger.info("== Mobile API v1: Import Gift Card using invalid card_number ==");
		Response importGiftCardInvalidCardNumberResponse = pageObj.endpoints().api1ImportGiftCard(
				dataSet.get("designId"), invalidValue, dataSet.get("epin"), dataSet.get("client"),
				dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardInvalidCardNumberResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardInvalidCardNumberSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, importGiftCardInvalidCardNumberResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardInvalidCardNumberSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardInvalidCardNumberMsg = importGiftCardInvalidCardNumberResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(importGiftCardInvalidCardNumberMsg, "Card no or epin is not valid.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using invalid card_number");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using invalid card_number");

		// Import Gift Card using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using invalid client ==");
		logger.info("== Mobile API v1: Import Gift Card using invalid client ==");
		Response importGiftCardInvalidClientResponse = pageObj.endpoints().api1ImportGiftCard(dataSet.get("designId"),
				dataSet.get("cardNumber"), dataSet.get("epin"), invalidValue, dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, importGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardInvalidSignatureSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardInvalidClientMsg = importGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(importGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using invalid client");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using invalid client");

		// Import Gift Card using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using invalid secret ==");
		logger.info("== Mobile API v1: Import Gift Card using invalid secret ==");
		Response importGiftCardInvalidSecretResponse = pageObj.endpoints().api1ImportGiftCard(dataSet.get("designId"),
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), invalidValue, token1);
		Assert.assertEquals(importGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Import Gift Card call");
		String importGiftCardInvalidSecretMsg = importGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(importGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using invalid secret");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using invalid secret");

		// Import Gift Card using invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using invalid user access token ==");
		logger.info("== Mobile API v1: Import Gift Card using invalid user access token ==");
		Response importGiftCardInvalidUserTokenResponse = pageObj.endpoints().api1ImportGiftCard(
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"),
				dataSet.get("secret"), invalidValue);
		Assert.assertEquals(importGiftCardInvalidUserTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardInvalidUserTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, importGiftCardInvalidUserTokenResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardInvalidUserTokenSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		String importGiftCardInvalidUserTokenMsg = importGiftCardInvalidUserTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(importGiftCardInvalidUserTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using invalid user access token");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using invalid user access token");

		// Mobile API v1: Import Gift Card using already used card number
		TestListeners.extentTest.get().info("== Mobile API v1: Import Gift Card using already used card number ==");
		logger.info("== Mobile API v1: Import Gift Card using already used card number ==");
		Response importGiftCardUsedCardResponse = pageObj.endpoints().api1ImportGiftCard(dataSet.get("designId"),
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(importGiftCardUsedCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardUsedCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, importGiftCardUsedCardResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardUsedCardSchemaValidated,
				"API1 Import Gift Card Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("API v1 Import Gift Card call is unsuccessful because of using already used card number");
		logger.info("API v1 Import Gift Card call is unsuccessful because of using already used card number");

	}

	@Test(description = "Verify Mobile API v1 Negative Scenarios:- SQ-T5009: Social Cause Campaign", groups = "api", priority = 6)
	public void verifyAPIv1SocialCauseCampaignNegative() {

		// Create Social Cause Campaigns
		TestListeners.extentTest.get().info("== Platform Functions: Create Social Cause Campaign ==");
		logger.info("== Platform Functions: Create Social Cause Campaign ==");
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("apiKey"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions API Create Social Cause Campaign");
		String socialCauseId = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();
		TestListeners.extentTest.get().pass("Platform Functions API Create Social Cause Campaigns is successful");
		logger.info("Platform Functions API Create Social Cause Campaigns is successful");

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// Social Cause Create Donation when not enough reward balance is available
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation when not enough reward balance is available ==");
		logger.info("== Mobile API v1: Social Cause Create Donation when not enough reward balance is available ==");
		Response createDonationNotEnoughBalanceResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"),
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationNotEnoughBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationNotEnoughBalanceSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationNotEnoughBalanceResponse.asString());
		Assert.assertTrue(isApi1CreateDonationNotEnoughBalanceSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String expectedNotEnoughBalanceMessage = "Not enough reward balance available to redeem. Current Balance of $0.00 is less than $"
				+ dataSet.get("itemToDonate") + " requested";
		String actualNotEnoughBalanceMessage = createDonationNotEnoughBalanceResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(actualNotEnoughBalanceMessage, expectedNotEnoughBalanceMessage,
				"Not Enough Balance Message did not match");
		TestListeners.extentTest.get().pass(
				"API v1 Social Cause Create Donation is unsuccessful because of not having enough reward balance");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of not having enough reward balance");

		// Send reward amount to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward amount to user ==");
		logger.info("== Platform Functions: Send reward amount to user ==");
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send reward amount to user");
		TestListeners.extentTest.get().pass("Send reward amount to user is successful");
		logger.info("Send reward amount to user is successful");

		// Social Cause Create Donation using invalid social_cause_id
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation using invalid social_cause_id ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid social_cause_id ==");
		Response createDonationInvalidSocialCauseIdResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, invalidValue, dataSet.get("donationType"),
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationInvalidSocialCauseIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationInvalidSocialCauseIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationInvalidSocialCauseIdResponse.asString());
		Assert.assertTrue(isApi1CreateDonationInvalidSocialCauseIdSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationInvalidSocialCauseIdMsg = createDonationInvalidSocialCauseIdResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(createDonationInvalidSocialCauseIdMsg, "Social Cause Campaign not found.",
				"Invalid social_cause_id Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid social_cause_id");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid social_cause_id");

		// Social Cause Create Donation with missing social_cause_id
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation with missing social_cause_id ==");
		logger.info("== Mobile API v1: Social Cause Create Donation with missing social_cause_id ==");
		Response createDonationMissingSocialCauseIdResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, "", dataSet.get("donationType"),
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationMissingSocialCauseIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationMissingSocialCauseIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createDonationMissingSocialCauseIdResponse.asString());
		Assert.assertTrue(isApi1CreateDonationMissingSocialCauseIdSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationMissingSocialCauseIdMsg = createDonationMissingSocialCauseIdResponse.jsonPath()
				.get("error").toString();
		Assert.assertEquals(createDonationMissingSocialCauseIdMsg,
				"Required parameter missing or the value is empty: social_cause_id",
				"Missing social_cause_id Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of missing social_cause_id");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of missing social_cause_id");

		// Social Cause Create Donation using invalid donation_type
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation using invalid donation_type ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid donation_type ==");
		Response createDonationInvalidDonationTypeResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, socialCauseId, invalidValue,
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationInvalidDonationTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationInvalidDonationTypeSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationInvalidDonationTypeResponse.asString());
		Assert.assertTrue(isApi1CreateDonationInvalidDonationTypeSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationInvalidDonationTypeMsg = createDonationInvalidDonationTypeResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(createDonationInvalidDonationTypeMsg,
				"Invalid donation_type! Supported donation types are card, redeemable, reward, currency.",
				"Invalid donation_type Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid donation_type");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid donation_type");

		// Social Cause Create Donation with missing donation_type
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation with missing donation_type ==");
		logger.info("== Mobile API v1: Social Cause Create Donation with missing donation_type ==");
		Response createDonationMissingDonationTypeResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, socialCauseId, "", dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationMissingDonationTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationMissingDonationTypeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createDonationMissingDonationTypeResponse.asString());
		Assert.assertTrue(isApi1CreateDonationMissingDonationTypeSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationMissingDonationTypeMsg = createDonationMissingDonationTypeResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(createDonationMissingDonationTypeMsg,
				"Required parameter missing or the value is empty: donation_type",
				"Missing donation_type Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of missing donation_type");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of missing donation_type");

		// Social Cause Create Donation using invalid item_to_donate
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation using invalid item_to_donate ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid item_to_donate ==");
		Response createDonationInvalidItemToDonateResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"), "0");
		Assert.assertEquals(createDonationInvalidItemToDonateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationInvalidItemToDonateSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationInvalidItemToDonateResponse.asString());
		Assert.assertTrue(isApi1CreateDonationInvalidItemToDonateSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationInvalidItemToDonateMsg = createDonationInvalidItemToDonateResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(createDonationInvalidItemToDonateMsg, "Invalid transfer amount",
				"Invalid item_to_donate Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid item_to_donate");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid item_to_donate");

		// Social Cause Create Donation with missing item_to_donate
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation with missing item_to_donate ==");
		logger.info("== Mobile API v1: Social Cause Create Donation with missing item_to_donate ==");
		Response createDonationMissingItemToDonateResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"),
				"\"\"");
		Assert.assertEquals(createDonationMissingItemToDonateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationMissingItemToDonateSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createDonationMissingItemToDonateResponse.asString());
		Assert.assertTrue(isApi1CreateDonationMissingItemToDonateSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationMissingItemToDonateMsg = createDonationMissingItemToDonateResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(createDonationMissingItemToDonateMsg,
				"Required parameter missing or the value is empty: item_to_donate",
				"Missing item_to_donate Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of missing item_to_donate");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of missing item_to_donate");

		// Social Cause Create Donation using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Social Cause Create Donation using invalid client ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid client ==");
		Response createDonationInvalidClientResponse = pageObj.endpoints().api1SocialCauseDonation(invalidValue,
				dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"), dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationInvalidClientResponse.asString());
		Assert.assertTrue(isApi1CreateDonationInvalidSignatureSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationInvalidClientMsg = createDonationInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createDonationInvalidClientMsg, "Invalid Signature",
				"Invalid client Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid client");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid client");

		// Social Cause Create Donation using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Social Cause Create Donation using invalid secret ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid secret ==");
		Response createDonationInvalidSecretResponse = pageObj.endpoints().api1SocialCauseDonation(
				dataSet.get("client"), invalidValue, token, socialCauseId, dataSet.get("donationType"),
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Social Cause Create Donation");
		String createDonationInvalidSecretMsg = createDonationInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createDonationInvalidSecretMsg, "Invalid Signature",
				"Invalid secret Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid secret");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid secret");

		// Social Cause Create Donation using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Social Cause Create Donation using invalid token ==");
		logger.info("== Mobile API v1: Social Cause Create Donation using invalid token ==");
		Response createDonationInvalidTokenResponse = pageObj.endpoints().api1SocialCauseDonation(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, socialCauseId, dataSet.get("donationType"),
				dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createDonationInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1CreateDonationInvalidTokenSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationInvalidTokenMsg = createDonationInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(createDonationInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Invalid token Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because of using invalid token");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because of using invalid token");

		// Deactivate Social Cause Campaign
		TestListeners.extentTest.get().info("== Platform Functions: Deactivate Social Cause Campaign ==");
		logger.info("== Platform Functions: Deactivate Social Cause Campaign ==");
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(socialCauseId,
				dataSet.get("apiKey"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("Platform Functions API Deactivate Social Cause Campaign is successful");
		logger.info("Platform Functions API Deactivate Social Cause Campaign is successful");

		// Social Cause Create Donation when the campaign was deactivated
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Social Cause Create Donation when the campaign was deactivated ==");
		logger.info("== Mobile API v1: Social Cause Create Donation when the campaign was deactivated ==");
		Response createDonationDeactivatedResponse = pageObj.endpoints().api1SocialCauseDonation(dataSet.get("client"),
				dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"), dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationDeactivatedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Social Cause Create Donation");
		boolean isApi1CreateDonationDeactivatedSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationDeactivatedResponse.asString());
		Assert.assertTrue(isApi1CreateDonationDeactivatedSchemaValidated,
				"API1 Social Cause Create Donation Schema Validation failed");
		String createDonationDeactivatedMsg = createDonationDeactivatedResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(createDonationDeactivatedMsg,
				"You can’t donate to this Social Cause as the related campaign has expired or has been deactivated by the Business.",
				"Social Cause Campaign Deactivated Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Social Cause Create Donation is unsuccessful because the campaign was deactivated");
		logger.info("API v1 Social Cause Create Donation is unsuccessful because the campaign was deactivated");

	}

	@Test(description = "Verify Mobile API v1 Gift Card Negative Scenarios:- SQ-T5013: Purchase; SQ-T5015: Update; SQ-T5017: Reload; SQ-T5026: Fetch Balance; "
			+ "SQ-T5046: POST Tip; SQ-T5048: GET Tip; SQ-T5059: Gift; SQ-T5072: Share; SQ-T5078: Transfer; SQ-T5080: Fetch Transaction History", groups = "api", priority = 7)
	public void verifyAPIv1GiftCardNegative() {

		String invalidId = "abcd-01234";
		String giftCardInvalidEmailMsg = "There is no guest associated with this email. Please ask them to sign up with "
				+ invalidEmail + ".";
		String giftCardBalanceNotUpdatedMsg = "Oops! Your Gift Card Balance is not updated. Please Pull Down to refresh the Gift Card Balance before you transfer the amount/card.";

		// User sign-up
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up ==");
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("API v1 user signup is successful");
		logger.info("API v1 user signup is successful");

		// Purchase Gift Card
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card ==");
		logger.info("== Mobile API v1: Purchase Gift Card ==");
		String giftCardUuid = "";
		int counter = 0;
		while (counter < 10) {
			try {
				TestListeners.extentTest.get().info("API v1 Purchase gift card hit count is: " + counter);
				logger.info("API v1 Purchase gift card hit count is: " + counter);
				Response purchaseGiftCardResponse = pageObj.endpoints().api1PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("designId"),
						dataSet.get("transactionToken"), dataSet.get("expDate"));
				TestListeners.extentTest.get()
						.info("API v1 Purchase gift card call response is: " + purchaseGiftCardResponse.asString());
				giftCardUuid = purchaseGiftCardResponse.jsonPath().get("uuid").toString();
				if (giftCardUuid != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 purchase gift card");
					TestListeners.extentTest.get().pass(
							"API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid);
					logger.info("API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longwait(5000);
		}

		// Purchase Gift Card with invalid design_id
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid design_id ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid design_id ==");
		Response purchaseGiftCardInvalidDesignIdResponse = pageObj.endpoints().api1PurchaseGiftCard(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), token, invalidValue,
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardInvalidDesignIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, purchaseGiftCardInvalidDesignIdResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardInvalidDesignIdSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardInvalidDesignIdMsg = purchaseGiftCardInvalidDesignIdResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(purchaseGiftCardInvalidDesignIdMsg,
				"Card design unavailable! Please restart application to refresh gift card design data.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of invalid design_id");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid design_id");

		// Purchase Gift Card with missing design_id
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with missing design_id ==");
		logger.info("== Mobile API v1: Purchase Gift Card with missing design_id ==");
		Response purchaseGiftCardMissingDesignIdResponse = pageObj.endpoints().api1PurchaseGiftCard(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), token, "",
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardMissingDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardMissingDesignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, purchaseGiftCardMissingDesignIdResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardMissingDesignIdSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardMissingDesignIdMsg = purchaseGiftCardMissingDesignIdResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(purchaseGiftCardMissingDesignIdMsg,
				"Required parameter missing or the value is empty: design_id", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of missing design_id");
		logger.info("API v1 purchase gift card is unsuccessful because of missing design_id");

		// Purchase Gift Card with invalid amount
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid amount ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid amount ==");
		Response purchaseGiftCardInvalidAmountResponse = pageObj.endpoints().api1PurchaseGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), invalidValue, token, dataSet.get("designId"),
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardInvalidAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, purchaseGiftCardInvalidAmountResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardInvalidAmountSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardInvalidAmountMsg = purchaseGiftCardInvalidAmountResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(purchaseGiftCardInvalidAmountMsg, "Transaction amount can not be less than $10.00.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of invalid amount");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid amount");

		// Purchase Gift Card with missing amount
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with missing amount ==");
		logger.info("== Mobile API v1: Purchase Gift Card with missing amount ==");
		Response purchaseGiftCardMissingAmountResponse = pageObj.endpoints().api1PurchaseGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), "", token, dataSet.get("designId"),
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, purchaseGiftCardMissingAmountResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardMissingAmountSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardMissingAmountMsg = purchaseGiftCardMissingAmountResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(purchaseGiftCardMissingAmountMsg,
				"Required parameter missing or the value is empty: amount", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of missing amount");
		logger.info("API v1 purchase gift card is unsuccessful because of missing amount");

		// Purchase Gift Card with invalid transaction_token
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid transaction_token ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid transaction_token ==");
		Response purchaseGiftCardInvalidTransactionTokenResponse = pageObj.endpoints()
				.api1PurchaseGiftCardBySendingAmount(dataSet.get("client"), dataSet.get("secret"),
						dataSet.get("amount"), token, dataSet.get("designId"), invalidValue, dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardInvalidTransactionTokenSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, purchaseGiftCardInvalidTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardInvalidTransactionTokenSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardInvalidTransactionTokenMsg = purchaseGiftCardInvalidTransactionTokenResponse.jsonPath()
				.get("[0]").toString();
		Assert.assertEquals(purchaseGiftCardInvalidTransactionTokenMsg, "Unknown or expired payment_method_nonce.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 purchase gift card is unsuccessful because of invalid transaction_token");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid transaction_token");

		// Purchase Gift Card with missing transaction_token
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with missing transaction_token ==");
		logger.info("== Mobile API v1: Purchase Gift Card with missing transaction_token ==");
		Response purchaseGiftCardMissingTransactionTokenResponse = pageObj.endpoints()
				.api1PurchaseGiftCardBySendingAmount(dataSet.get("client"), dataSet.get("secret"),
						dataSet.get("amount"), token, dataSet.get("designId"), "", dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardMissingTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardMissingTransactionTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, purchaseGiftCardMissingTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardMissingTransactionTokenSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardMissingTransactionTokenMsg = purchaseGiftCardMissingTransactionTokenResponse.jsonPath()
				.get("error").toString();
		Assert.assertEquals(purchaseGiftCardMissingTransactionTokenMsg,
				"Required parameter missing or the value is empty: transaction_token", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 purchase gift card is unsuccessful because of missing transaction_token");
		logger.info("API v1 purchase gift card is unsuccessful because of missing transaction_token");

		// Purchase Gift Card with invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid client ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid client ==");
		Response purchaseGiftCardInvalidClientResponse = pageObj.endpoints().api1PurchaseGiftCardBySendingAmount(
				invalidValue, dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("designId"),
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, purchaseGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardInvalidSignatureSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardInvalidClientMsg = purchaseGiftCardInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(purchaseGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of invalid client");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid client");

		// Purchase Gift Card with invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid secret ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid secret ==");
		Response purchaseGiftCardInvalidSecretResponse = pageObj.endpoints().api1PurchaseGiftCardBySendingAmount(
				dataSet.get("client"), invalidValue, dataSet.get("amount"), token, dataSet.get("designId"),
				dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 purchase gift card");
		String purchaseGiftCardInvalidSecretMsg = purchaseGiftCardInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(purchaseGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 purchase gift card is unsuccessful because of invalid secret");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid secret");

		// Purchase Gift Card with invalid user access token
		TestListeners.extentTest.get().info("== Mobile API v1: Purchase Gift Card with invalid user access token ==");
		logger.info("== Mobile API v1: Purchase Gift Card with invalid user access token ==");
		Response purchaseGiftCardInvalidTokenResponse = pageObj.endpoints().api1PurchaseGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), invalidValue,
				dataSet.get("designId"), dataSet.get("transactionToken"), dataSet.get("expDate"));
		Assert.assertEquals(purchaseGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 purchase gift card");
		boolean isApi1PurchaseGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, purchaseGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1PurchaseGiftCardInvalidTokenSchemaValidated,
				"API1 Purchase Gift Card Schema Validation failed");
		String purchaseGiftCardInvalidTokenMsg = purchaseGiftCardInvalidTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(purchaseGiftCardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 purchase gift card is unsuccessful because of invalid user access token");
		logger.info("API v1 purchase gift card is unsuccessful because of invalid user access token");

		// Update Gift Card using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Update Gift Card using invalid client ==");
		logger.info("== Mobile API v1: Update Gift Card using invalid client ==");
		Response updateGiftCardInvalidClientResponse = pageObj.endpoints().api1UpdateGiftCard(invalidValue,
				dataSet.get("secret"), token, dataSet.get("preferred"), giftCardUuid);
		Assert.assertEquals(updateGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update gift card");
		boolean isApi1UpdateGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1UpdateGiftCardInvalidSignatureSchemaValidated,
				"API1 Update Gift Card Schema Validation failed");
		String updateGiftCardInvalidClientMsg = updateGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update gift card is unsuccessful because of invalid client");
		logger.info("API v1 Update gift card is unsuccessful because of invalid client");

		// Update Gift Card using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Update Gift Card using invalid secret ==");
		logger.info("== Mobile API v1: Update Gift Card using invalid secret ==");
		Response updateGiftCardInvalidSecretResponse = pageObj.endpoints().api1UpdateGiftCard(dataSet.get("client"),
				invalidValue, token, dataSet.get("preferred"), giftCardUuid);
		Assert.assertEquals(updateGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Update gift card");
		String updateGiftCardInvalidSecretMsg = updateGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update gift card is unsuccessful because of invalid secret");
		logger.info("API v1 Update gift card is unsuccessful because of invalid secret");

		// Update Gift Card using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Update Gift Card using invalid token ==");
		logger.info("== Mobile API v1: Update Gift Card using invalid token ==");
		Response updateGiftCardInvalidTokenResponse = pageObj.endpoints().api1UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, dataSet.get("preferred"), giftCardUuid);
		Assert.assertEquals(updateGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Update gift card");
		boolean isApi1UpdateGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, updateGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1UpdateGiftCardInvalidTokenSchemaValidated,
				"API1 Update Gift Card Schema Validation failed");
		String updateGiftCardInvalidTokenMsg = updateGiftCardInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(updateGiftCardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Update gift card is unsuccessful because of invalid token");
		logger.info("API v1 Update gift card is unsuccessful because of invalid token");

		// Update Gift Card using invalid gift card uuid
		TestListeners.extentTest.get().info("== Mobile API v1: Update Gift Card using invalid gift card uuid ==");
		logger.info("== Mobile API v1: Update Gift Card using invalid gift card uuid ==");
		Response updateGiftCardInvalidUuidResponse = pageObj.endpoints().api1UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("preferred"), invalidId);
		Assert.assertEquals(updateGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Update gift card");
		boolean isApi1UpdateGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updateGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1UpdateGiftCardInvalidUuidSchemaValidated,
				"API1 Update Gift Card Schema Validation failed");
		String updateGiftCardInvalidUuidMsg = updateGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(updateGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Update gift card is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Update gift card is unsuccessful because of invalid gift card uuid");

		// Reload Gift Card using invalid amount
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid amount ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid amount ==");
		Response reloadGiftCardInvalidAmountResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), token, invalidValue, dataSet.get("designId"),
				dataSet.get("transactionToken"), giftCardUuid);
		Assert.assertEquals(reloadGiftCardInvalidAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardInvalidAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, reloadGiftCardInvalidAmountResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardInvalidAmountSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardInvalidAmountMsg = reloadGiftCardInvalidAmountResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(reloadGiftCardInvalidAmountMsg, "Transaction amount can not be less than $10.00.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Reload gift card is unsuccessful because of invalid amount");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid amount");

		// Reload Gift Card using missing amount
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card with missing amount ==");
		logger.info("== Mobile API v1: Reload Gift Card with missing amount ==");
		Response reloadGiftCardMissingAmountResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), token, "", dataSet.get("designId"),
				dataSet.get("transactionToken"), giftCardUuid);
		Assert.assertEquals(reloadGiftCardMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reloadGiftCardMissingAmountResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardMissingAmountSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardMissingAmountMsg = reloadGiftCardMissingAmountResponse.jsonPath().get("error").toString();
		Assert.assertEquals(reloadGiftCardMissingAmountMsg, "Required parameter missing or the value is empty: amount",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Reload gift card is unsuccessful because of missing amount");
		logger.info("API v1 Reload gift card is unsuccessful because of missing amount");

		// Reload Gift Card using invalid transaction token
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid transaction token ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid transaction token ==");
		Response reloadGiftCardInvalidTransactionTokenResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("amount"), dataSet.get("designId"),
				invalidValue, giftCardUuid);
		Assert.assertEquals(reloadGiftCardInvalidTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardInvalidTransactionTokenSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, reloadGiftCardInvalidTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardInvalidTransactionTokenSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardInvalidTransactionTokenMsg = reloadGiftCardInvalidTransactionTokenResponse.jsonPath()
				.get("[0]").toString();
		Assert.assertEquals(reloadGiftCardInvalidTransactionTokenMsg, "Unknown or expired payment_method_nonce.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Reload gift card is unsuccessful because of invalid transaction token");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid transaction token");

		// Reload Gift Card using missing transaction token
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card with missing transaction token ==");
		logger.info("== Mobile API v1: Reload Gift Card with missing transaction token ==");
		Response reloadGiftCardMissingTransactionTokenResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("amount"), dataSet.get("designId"), "",
				giftCardUuid);
		Assert.assertEquals(reloadGiftCardMissingTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardMissingTransactionTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reloadGiftCardMissingTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardMissingTransactionTokenSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardMissingTransactionTokenMsg = reloadGiftCardMissingTransactionTokenResponse.jsonPath()
				.get("error").toString();
		Assert.assertEquals(reloadGiftCardMissingTransactionTokenMsg,
				"Required parameter missing or the value is empty: transaction_token", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Reload gift card is unsuccessful because of missing transaction token");
		logger.info("API v1 Reload gift card is unsuccessful because of missing transaction token");

		// Reload Gift Card using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid client ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid client ==");
		Response reloadGiftCardInvalidClientResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				invalidValue, dataSet.get("secret"), token, dataSet.get("amount"), dataSet.get("designId"),
				dataSet.get("transactionToken"), giftCardUuid);
		Assert.assertEquals(reloadGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, reloadGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardInvalidSignatureSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardInvalidClientMsg = reloadGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(reloadGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Reload gift card is unsuccessful because of invalid client");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid client");

		// Reload Gift Card using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid secret ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid secret ==");
		Response reloadGiftCardInvalidSecretResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), invalidValue, token, dataSet.get("amount"), dataSet.get("designId"),
				dataSet.get("transactionToken"), giftCardUuid);
		Assert.assertEquals(reloadGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Reload gift card");
		String reloadGiftCardInvalidSecretMsg = reloadGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(reloadGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Reload gift card is unsuccessful because of invalid secret");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid secret");

		// Reload Gift Card using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid token ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid token ==");
		Response reloadGiftCardInvalidTokenResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), invalidValue, dataSet.get("amount"),
				dataSet.get("designId"), dataSet.get("transactionToken"), giftCardUuid);
		Assert.assertEquals(reloadGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reloadGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardInvalidTokenSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardInvalidTokenMsg = reloadGiftCardInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(reloadGiftCardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Reload gift card is unsuccessful because of invalid token");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid token");

		// Reload Gift Card using invalid gift card uuid
		TestListeners.extentTest.get().info("== Mobile API v1: Reload Gift Card using invalid gift card uuid ==");
		logger.info("== Mobile API v1: Reload Gift Card using invalid gift card uuid ==");
		Response reloadGiftCardInvalidUuidResponse = pageObj.endpoints().api1ReloadGiftCardBySendingAmount(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("amount"), dataSet.get("designId"),
				dataSet.get("transactionToken"), invalidId);
		Assert.assertEquals(reloadGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Reload gift card");
		boolean isApi1ReloadGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, reloadGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1ReloadGiftCardInvalidUuidSchemaValidated,
				"API1 Reload Gift Card Schema Validation failed");
		String reloadGiftCardInvalidUuidMsg = reloadGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(reloadGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Reload gift card is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Reload gift card is unsuccessful because of invalid gift card uuid");

		// Fetch Gift Card Balance using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Gift Card Balance using invalid client ==");
		logger.info("== Mobile API v1: Fetch Gift Card Balance using invalid client ==");
		Response giftCardBalanceInvalidClientResponse = pageObj.endpoints().api1FetchGiftCardBalance(invalidValue,
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(giftCardBalanceInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Gift Card Balance");
		boolean isApi1FetchGiftCardBalanceInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftCardBalanceInvalidClientResponse.asString());
		Assert.assertTrue(isApi1FetchGiftCardBalanceInvalidSignatureSchemaValidated,
				"API1 Fetch Gift Card Balance Schema Validation failed");
		String giftCardBalanceInvalidClientMsg = giftCardBalanceInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardBalanceInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch Gift Card Balance is unsuccessful because of invalid client");
		logger.info("API v1 Fetch Gift Card Balance is unsuccessful because of invalid client");

		// Fetch Gift Card Balance using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Gift Card Balance using invalid secret ==");
		logger.info("== Mobile API v1: Fetch Gift Card Balance using invalid secret ==");
		Response giftCardBalanceInvalidSecretResponse = pageObj.endpoints()
				.api1FetchGiftCardBalance(dataSet.get("client"), invalidValue, token, giftCardUuid);
		Assert.assertEquals(giftCardBalanceInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Gift Card Balance");
		String giftCardBalanceInvalidSecretMsg = giftCardBalanceInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardBalanceInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch Gift Card Balance is unsuccessful because of invalid secret");
		logger.info("API v1 Fetch Gift Card Balance is unsuccessful because of invalid secret");

		// Fetch Gift Card Balance using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: Fetch Gift Card Balance using invalid token ==");
		logger.info("== Mobile API v1: Fetch Gift Card Balance using invalid token ==");
		Response giftCardBalanceInvalidTokenResponse = pageObj.endpoints()
				.api1FetchGiftCardBalance(dataSet.get("client"), dataSet.get("secret"), invalidValue, giftCardUuid);
		Assert.assertEquals(giftCardBalanceInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch Gift Card Balance");
		boolean isApi1FetchGiftCardBalanceInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftCardBalanceInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1FetchGiftCardBalanceInvalidTokenSchemaValidated,
				"API1 Fetch Gift Card Balance Schema Validation failed");
		String giftCardBalanceInvalidTokenMsg = giftCardBalanceInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(giftCardBalanceInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Fetch Gift Card Balance is unsuccessful because of invalid token");
		logger.info("API v1 Fetch Gift Card Balance is unsuccessful because of invalid token");

		// Fetch Gift Card Balance using invalid gift card uuid
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch Gift Card Balance using invalid gift card uuid ==");
		logger.info("== Mobile API v1: Fetch Gift Card Balance using invalid gift card uuid ==");
		Response giftCardBalanceInvalidUuidResponse = pageObj.endpoints()
				.api1FetchGiftCardBalance(dataSet.get("client"), dataSet.get("secret"), token, invalidId);
		Assert.assertEquals(giftCardBalanceInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Fetch Gift Card Balance");
		boolean isApi1FetchGiftCardBalanceInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftCardBalanceInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1FetchGiftCardBalanceInvalidUuidSchemaValidated,
				"API1 Fetch Gift Card Balance Schema Validation failed");
		String giftCardBalanceInvalidUuidMsg = giftCardBalanceInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardBalanceInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Fetch Gift Card Balance is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Fetch Gift Card Balance is unsuccessful because of invalid gift card uuid");

		// Create Loyalty Checkin by Receipt Image
		TestListeners.extentTest.get().info("== Mobile API2: Create Loyalty Checkin by Receipt Image ==");
		logger.info("== Mobile API2: Create Loyalty Checkin by Receipt Image ==");
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("locationId"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 Loyalty Checkin by Receipt Image");
		String checkinId = receiptCheckinResponse.jsonPath().getString("checkin_id").toString();
		TestListeners.extentTest.get()
				.pass("API2 Create Loyalty Checkin by Receipt Image is successful with checkin id: " + checkinId);
		logger.info("API2 Create Loyalty Checkin by Receipt Image is successful with checkin id: " + checkinId);

		// POST Tip via Gift Card using invalid checkin_id
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid checkin_id ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid checkin_id ==");
		Response postTipGiftCardInvalidCheckinIdResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, invalidId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardInvalidCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardInvalidCheckinIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postTipGiftCardInvalidCheckinIdResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardInvalidCheckinIdSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardInvalidCheckinIdMsg = postTipGiftCardInvalidCheckinIdResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(postTipGiftCardInvalidCheckinIdMsg, "This checkin ID is not valid/not found",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid checkin_id");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid checkin_id");

		// POST Tip via Gift Card with missing checkin_id
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card with missing checkin_id ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card with missing checkin_id ==");
		Response postTipGiftCardMissingCheckinIdResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, "", giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardMissingCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardMissingCheckinIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, postTipGiftCardMissingCheckinIdResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardMissingCheckinIdSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardMissingCheckinIdMsg = postTipGiftCardMissingCheckinIdResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(postTipGiftCardMissingCheckinIdMsg,
				"Required parameter missing or the value is empty: checkin_id", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 POST Tip via Gift Card is unsuccessful because of missing checkin_id");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of missing checkin_id");

		// POST Tip via Gift Card using invalid tip
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid tip ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid tip ==");
		Response postTipGiftCardInvalidTipResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, "0", "POST");
		Assert.assertEquals(postTipGiftCardInvalidTipResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardInvalidTipSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postTipGiftCardInvalidTipResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardInvalidTipSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardInvalidTipMsg = postTipGiftCardInvalidTipResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(postTipGiftCardInvalidTipMsg, "Requested amount must be greater than 0",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid tip");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid tip");

		// POST Tip via Gift Card with missing tip
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card with missing tip ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card with missing tip ==");
		Response postTipGiftCardMissingTipResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, "", "POST");
		Assert.assertEquals(postTipGiftCardMissingTipResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardMissingTipSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, postTipGiftCardMissingTipResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardMissingTipSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardMissingTipMsg = postTipGiftCardMissingTipResponse.jsonPath().get("error").toString();
		Assert.assertEquals(postTipGiftCardMissingTipMsg, "Required parameter missing or the value is empty: tip",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is unsuccessful because of missing tip");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of missing tip");

		// POST Tip via Gift Card using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid client ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid client ==");
		Response postTipGiftCardInvalidClientResponse = pageObj.endpoints().api1GiftCardTip(invalidValue,
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postTipGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardInvalidSignatureSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardInvalidClientMsg = postTipGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(postTipGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid client");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid client");

		// POST Tip via Gift Card using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid secret ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid secret ==");
		Response postTipGiftCardInvalidSecretResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				invalidValue, token, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 POST Tip via Gift Card");
		String postTipGiftCardInvalidSecretMsg = postTipGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(postTipGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid secret");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid secret");

		// POST Tip via Gift Card using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid token ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid token ==");
		Response postTipGiftCardInvalidTokenResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, postTipGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardInvalidTokenSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardInvalidTokenMsg = postTipGiftCardInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(postTipGiftCardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid token");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid token");

		// POST Tip via Gift Card using invalid gift card uuid
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card using invalid gift card uuid ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card using invalid gift card uuid ==");
		Response postTipGiftCardInvalidUuidResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, invalidId, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postTipGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardInvalidUuidSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardInvalidUuidMsg = postTipGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(postTipGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 POST Tip via Gift Card is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of invalid gift card uuid");

		// GET Tip via Gift Card when POST tip was not performed
		TestListeners.extentTest.get()
				.info("== Mobile API v1: GET Tip via Gift Card when POST tip was not performed ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card when POST tip was not performed ==");
		Response getTipGiftCardWithoutPostResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardWithoutPostResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v1 GET Tip via Gift Card");
		TestListeners.extentTest.get().pass(
				"API v1 GET Tip via Gift Card is unsuccessful because a successful POST tip call is not yet performed");
		logger.info(
				"API v1 GET Tip via Gift Card is unsuccessful because a successful POST tip call is not yet performed");

		// POST Tip via Gift Card
		TestListeners.extentTest.get().info("== Mobile API v1: POST Tip via Gift Card ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card ==");
		Response postTipGiftCardResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 POST Tip via Gift Card");
		String approvedAmount = postTipGiftCardResponse.jsonPath().get("approved_amount").toString();
		Assert.assertEquals(approvedAmount, dataSet.get("tip"), "Approved amount did not match");
		TestListeners.extentTest.get().pass("API v1 POST Tip via Gift Card is successful");
		logger.info("API v1 POST Tip via Gift Card is successful");

		// POST Tip via Gift Card when checkin has already been tipped
		TestListeners.extentTest.get()
				.info("== Mobile API v1: POST Tip via Gift Card when checkin has already been tipped ==");
		logger.info("== Mobile API v1: POST Tip via Gift Card when checkin has already been tipped ==");
		Response postTipGiftCardAlreadyTippedResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardAlreadyTippedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardAlreadyTippedSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, postTipGiftCardAlreadyTippedResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardAlreadyTippedSchemaValidated,
				"API1 POST Tip via Gift Card Schema Validation failed");
		String postTipGiftCardAlreadyTippedMsg = postTipGiftCardAlreadyTippedResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(postTipGiftCardAlreadyTippedMsg, "Checkin has been tipped.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 POST Tip via Gift Card is unsuccessful because of checkin being already tipped");
		logger.info("API v1 POST Tip via Gift Card is unsuccessful because of checkin being already tipped");

		// GET Tip via Gift Card using invalid checkin_id
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card using invalid checkin_id ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card using invalid checkin_id ==");
		Response getTipGiftCardInvalidCheckinIdResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, invalidId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardInvalidCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardInvalidCheckinIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getTipGiftCardInvalidCheckinIdResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardInvalidCheckinIdSchemaValidated,
				"API1 GET Tip via Gift Card Schema Validation failed");
		String getTipGiftCardInvalidCheckinIdMsg = getTipGiftCardInvalidCheckinIdResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(getTipGiftCardInvalidCheckinIdMsg, "This checkin ID is not valid/not found",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 GET Tip via Gift Card is unsuccessful because of invalid checkin_id");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of invalid checkin_id");

		// GET Tip via Gift Card with missing checkin_id
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card with missing checkin_id ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card with missing checkin_id ==");
		Response getTipGiftCardMissingCheckinIdResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, "", giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardMissingCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardMissingCheckinIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getTipGiftCardMissingCheckinIdResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardMissingCheckinIdSchemaValidated,
				"API1 GET Tip via Gift Card Schema Validation failed");
		String getTipGiftCardMissingCheckinIdMsg = getTipGiftCardMissingCheckinIdResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(getTipGiftCardMissingCheckinIdMsg,
				"Required parameter missing or the value is empty: checkin_id", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 GET Tip via Gift Card is unsuccessful because of missing checkin_id");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of missing checkin_id");

		// GET Tip via Gift Card using invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card using invalid client ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card using invalid client ==");
		Response getTipGiftCardInvalidClientResponse = pageObj.endpoints().api1GiftCardTip(invalidValue,
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getTipGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardInvalidSignatureSchemaValidated,
				"API1 GET Tip via Gift Card Schema Validation failed");
		String getTipGiftCardInvalidClientMsg = getTipGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getTipGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 GET Tip via Gift Card is unsuccessful because of invalid client");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of invalid client");

		// GET Tip via Gift Card using invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card using invalid secret ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card using invalid secret ==");
		Response getTipGiftCardInvalidSecretResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				invalidValue, token, checkinId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 GET Tip via Gift Card");
		String getTipGiftCardInvalidSecretMsg = getTipGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getTipGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get().pass("API v1 GET Tip via Gift Card is unsuccessful because of invalid secret");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of invalid secret");

		// GET Tip via Gift Card using invalid token
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card using invalid token ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card using invalid token ==");
		Response getTipGiftCardInvalidTokenResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, checkinId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getTipGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardInvalidTokenSchemaValidated,
				"API1 GET Tip via Gift Card Schema Validation failed");
		String getTipGiftCardInvalidTokenMsg = getTipGiftCardInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(getTipGiftCardInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 GET Tip via Gift Card is unsuccessful because of invalid token");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of invalid token");

		// GET Tip via Gift Card using invalid gift card uuid
		TestListeners.extentTest.get().info("== Mobile API v1: GET Tip via Gift Card using invalid gift card uuid ==");
		logger.info("== Mobile API v1: GET Tip via Gift Card using invalid gift card uuid ==");
		Response getTipGiftCardInvalidUuidResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, invalidId, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, getTipGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardInvalidUuidSchemaValidated,
				"API1 GET Tip via Gift Card Schema Validation failed");
		String getTipGiftCardInvalidUuidMsg = getTipGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(getTipGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 GET Tip via Gift Card is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 GET Tip via Gift Card is unsuccessful because of invalid gift card uuid");

		// User sign-up #2
		TestListeners.extentTest.get().info("== Mobile API v1: User sign-up #2 ==");
		logger.info("== Mobile API v1: User sign-up #2 ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		TestListeners.extentTest.get().pass("API v1 user signup #2 is successful");
		logger.info("API v1 user signup #2 is successful");

		// Gift a Gift Card with missing amount to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with missing amount to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with missing amount to user #2 ==");
		Response giftaCardMissingAmountResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "", dataSet.get("designId"), dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftaCardMissingAmountResponse.asString());
		Assert.assertTrue(isApi1GiftaCardMissingAmountSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardMissingAmountMsg = giftaCardMissingAmountResponse.jsonPath().get("error").toString();
		Assert.assertEquals(giftaCardMissingAmountMsg, "Required parameter missing or the value is empty: amount",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing amount");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing amount");

		// Gift a Gift Card with less than minimum amount to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Gift a Gift Card with less than minimum amount to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with less than minimum amount to user #2 ==");
		Response giftaCardLowAmountResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "1", dataSet.get("designId"),
				dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardLowAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardLowAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftaCardLowAmountResponse.asString());
		Assert.assertTrue(isApi1GiftaCardLowAmountSchemaValidated, "API1 Gift a Gift Card Schema Validation failed");
		String giftaCardLowAmountMsg = giftaCardLowAmountResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftaCardLowAmountMsg, "Transaction amount can not be less than $10.00.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of having less than minimum amount");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of having less than minimum amount");

		// Gift a Gift Card with more than maximum amount to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Gift a Gift Card with more than maximum amount to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with more than maximum amount to user #2 ==");
		Response giftaCardHighAmountResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "1000", dataSet.get("designId"),
				dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardHighAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardHighAmountSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftaCardHighAmountResponse.asString());
		Assert.assertTrue(isApi1GiftaCardHighAmountSchemaValidated, "API1 Gift a Gift Card Schema Validation failed");
		String giftaCardHighAmountMsg = giftaCardHighAmountResponse.jsonPath().get("[0]").toString();
		String expectedGiftCardMaxLimitMsg = "Maximum balance exceeded. Payment would increase card balance over limit";
		Assert.assertTrue(giftaCardHighAmountMsg.contains(expectedGiftCardMaxLimitMsg), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of having more than maximum amount");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of having more than maximum amount");

		// Gift a Gift Card with missing design_id to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with missing design_id to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with missing design_id to user #2 ==");
		Response giftaCardMissingDesignIdResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "12", "", dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardMissingDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardMissingDesignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftaCardMissingDesignIdResponse.asString());
		Assert.assertTrue(isApi1GiftaCardMissingDesignIdSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardMissingDesignIdMsg = giftaCardMissingDesignIdResponse.jsonPath().get("error").toString();
		Assert.assertEquals(giftaCardMissingDesignIdMsg, "Required parameter missing or the value is empty: design_id",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing design_id");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing design_id");

		// Gift a Gift Card with invalid design_id to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with invalid design_id to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid design_id to user #2 to user #2 ==");
		Response giftaCardInvalidDesignIdResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "12", invalidValue, dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardInvalidDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardInvalidDesignIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftaCardInvalidDesignIdResponse.asString());
		Assert.assertTrue(isApi1GiftaCardInvalidDesignIdSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardInvalidDesignIdMsg = giftaCardInvalidDesignIdResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftaCardInvalidDesignIdMsg,
				"Card design unavailable! Please restart application to refresh gift card design data.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid design_id");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid design_id");

		// Gift a Gift Card with missing transaction_token to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Gift a Gift Card with missing transaction_token to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with missing transaction_token to user #2 ==");
		Response giftaCardMissingTransactionTokenResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "12", dataSet.get("designId"), "");
		Assert.assertEquals(giftaCardMissingTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardMissingTransactionTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftaCardMissingTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1GiftaCardMissingTransactionTokenSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardMissingTransactionTokenMsg = giftaCardMissingTransactionTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(giftaCardMissingTransactionTokenMsg,
				"Required parameter missing or the value is empty: transaction_token", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing transaction_token");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing transaction_token");

		// Gift a Gift Card with invalid transaction_token to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Gift a Gift Card with invalid transaction_token to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid transaction_token to user #2 ==");
		Response giftaCardInvalidTransactionTokenResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, userEmail2, "12", dataSet.get("designId"), invalidValue);
		Assert.assertEquals(giftaCardInvalidTransactionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardInvalidTransactionTokenSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftaCardInvalidTransactionTokenResponse.asString());
		Assert.assertTrue(isApi1GiftaCardInvalidTransactionTokenSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardInvalidTransactionTokenMsg = giftaCardInvalidTransactionTokenResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(giftaCardInvalidTransactionTokenMsg, "Unknown or expired payment_method_nonce.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid transaction_token");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid transaction_token");

		// Gift a Gift Card with missing email to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with missing email to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with missing email to user #2 ==");
		Response giftaCardMissingEmailResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), token, "", "12", dataSet.get("designId"), dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftaCardMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, giftaCardMissingEmailResponse.asString());
		Assert.assertTrue(isApi1GiftaCardMissingEmailSchemaValidated, "API1 Gift a Gift Card Schema Validation failed");
		String giftaCardMissingEmailMsg = giftaCardMissingEmailResponse.jsonPath().get("errors[0]").toString();
		Assert.assertEquals(giftaCardMissingEmailMsg,
				"Required parameters - email, phone and fb_uid are missing or the values are empty.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing email");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of missing email");

		// Gift a Gift Card with invalid email to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with invalid email to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid email to user #2 ==");
		String actualGiftaCardInvalidEmailMsg = "";
		int count = 0;
		while (count < 10) {
			try {
				TestListeners.extentTest.get().info("API v1 Gift a gift card hit count is: " + count);
				logger.info("API v1 Gift a gift card hit count is: " + count);
				Response giftaCardInvalidEmailResponse = pageObj.endpoints().api1GiftaCardWithRandomAmount(
						dataSet.get("client"), dataSet.get("secret"), token, invalidEmail, dataSet.get("designId"),
						dataSet.get("transactionToken"));
				TestListeners.extentTest.get()
						.info("API v1 Gift a gift card call response is: " + giftaCardInvalidEmailResponse.asString());
				actualGiftaCardInvalidEmailMsg = giftaCardInvalidEmailResponse.jsonPath().get("[0]").toString();
				if (actualGiftaCardInvalidEmailMsg.equals(giftCardInvalidEmailMsg)) {
					Assert.assertEquals(giftaCardInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
							"Status code 202 did not match for API v1 Gift a Gift Card call with invalid email.");
					boolean isApi1GiftaCardInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.apiStringArraySchema, giftaCardInvalidEmailResponse.asString());
					Assert.assertTrue(isApi1GiftaCardInvalidEmailSchemaValidated,
							"API1 Gift a Gift Card Schema Validation failed");
					TestListeners.extentTest.get()
							.pass("API v1 Gift a gift card to user #2 is unsuccessful because of invalid email.");
					logger.info("API v1 Gift a gift card to user #2 is unsuccessful because of invalid email.");
					break;
				}
			} catch (Exception e) {

			}
			count++;
			utils.longWaitInSeconds(5);
		}

		// Gift a Gift Card with invalid client to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with invalid client to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid client to user #2 ==");
		Response giftaCardInvalidClientResponse = pageObj.endpoints().api1GiftaCard(invalidValue, dataSet.get("secret"),
				token, userEmail2, "12", dataSet.get("designId"), dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftCardGiftInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftaCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GiftCardGiftInvalidSignatureSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardInvalidClientMsg = giftaCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftaCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid client");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid client");

		// Gift a Gift Card with invalid secret to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Gift a Gift Card with invalid secret to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid secret to user #2 ==");
		Response giftaCardInvalidSecretResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"), invalidValue,
				token, userEmail2, "12", dataSet.get("designId"), dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Gift a Gift Card");
		String giftaCardInvalidSecretMsg = giftaCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftaCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid secret");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid secret");

		// Gift a Gift Card with invalid user access token to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Gift a Gift Card with invalid user access token to user #2 ==");
		logger.info("== Mobile API v1: Gift a Gift Card with invalid user access token to user #2 ==");
		Response giftaCardInvalidUserTokenResponse = pageObj.endpoints().api1GiftaCard(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, userEmail2, "12", dataSet.get("designId"),
				dataSet.get("transactionToken"));
		Assert.assertEquals(giftaCardInvalidUserTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Gift a Gift Card");
		boolean isApi1GiftCardGiftInvalidUserTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftaCardInvalidUserTokenResponse.asString());
		Assert.assertTrue(isApi1GiftCardGiftInvalidUserTokenSchemaValidated,
				"API1 Gift a Gift Card Schema Validation failed");
		String giftaCardInvalidUserTokenMsg = giftaCardInvalidUserTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(giftaCardInvalidUserTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid user access token");
		logger.info("API v1 Gift a Gift Card to user #2 is unsuccessful because of invalid user access token");

		// Share Gift Card to user #2 using missing email
		TestListeners.extentTest.get().info("== Mobile API v1: Share Gift Card to user #2 using missing email ==");
		logger.info("== Mobile API v1: Share Gift Card to user #2 using missing email ==");
		Response shareGiftCardMissingEmailResponse = pageObj.endpoints().Api1ShareGiftCard("", dataSet.get("client"),
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Share gift card");
		boolean isApi1ShareGiftCardMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, shareGiftCardMissingEmailResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardMissingEmailSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String shareGiftCardMissingEmailMsg = shareGiftCardMissingEmailResponse.jsonPath().get("errors[0]").toString();
		Assert.assertEquals(shareGiftCardMissingEmailMsg,
				"Required parameters - email, phone and fb_uid are missing or the values are empty.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of missing email");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of missing email");

		// Share Gift Card to user #2 using invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: Share Gift Card to user #2 using invalid email ==");
		logger.info("== Mobile API v1: Share Gift Card to user #2 using invalid email ==");
		Response shareGiftCardInvalidEmailResponse = pageObj.endpoints().Api1ShareGiftCard(invalidEmail,
				dataSet.get("client"), dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Share gift card");
		boolean isApi1ShareGiftCardInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardInvalidEmailSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String actualShareGiftCardInvalidEmailMsg = shareGiftCardInvalidEmailResponse.jsonPath().get("[0]").toString();
		// Assert.assertEquals(actualShareGiftCardInvalidEmailMsg,
		// giftCardInvalidEmailMsg, "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of invalid email");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of invalid email");

		// Share Gift Card with invalid gift card uuid to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Share Gift Card with invalid gift card uuid to user #2 ==");
		logger.info("== Mobile API v1: Share Gift Card with invalid gift card uuid to user #2 ==");
		Response shareGiftCardInvalidUuidResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), token, invalidId);
		Assert.assertEquals(shareGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Share Gift Card");
		boolean isApi1ShareGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardInvalidUuidSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String shareGiftCardInvalidUuidMsg = shareGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(shareGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of invalid gift card uuid");

		// Share Gift Card with invalid client to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Share Gift Card with invalid client to user #2 ==");
		logger.info("== Mobile API v1: Share Gift Card with invalid client to user #2 ==");
		Response shareGiftCardInvalidClientResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2, invalidValue,
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Share Gift Card");
		boolean isApi1ShareGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardInvalidSignatureSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String shareGiftCardInvalidClientMsg = shareGiftCardInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(shareGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of invalid client");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of invalid client");

		// Share Gift Card with invalid secret to user #2
		TestListeners.extentTest.get().info("== Mobile API v1: Share Gift Card with invalid secret to user #2 ==");
		logger.info("== Mobile API v1: Share Gift Card with invalid secret to user #2 ==");
		Response shareGiftCardInvalidSecretResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2,
				dataSet.get("client"), invalidValue, token, giftCardUuid);
		Assert.assertEquals(shareGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Share Gift Card");
		String shareGiftCardInvalidSecretMsg = shareGiftCardInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(shareGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of invalid secret");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of invalid secret");

		// Share Gift Card with invalid user access token to user #2
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Share Gift Card with invalid user access token to user #2 ==");
		logger.info("== Mobile API v1: Share Gift Card with invalid user access token to user #2 ==");
		Response shareGiftCardInvalidUserTokenResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), invalidValue, giftCardUuid);
		Assert.assertEquals(shareGiftCardInvalidUserTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Share Gift Card");
		boolean isApi1ShareGiftCardInvalidUserTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, shareGiftCardInvalidUserTokenResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardInvalidUserTokenSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String shareGiftCardInvalidUserTokenMsg = shareGiftCardInvalidUserTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(shareGiftCardInvalidUserTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Share Gift Card to user #2 is unsuccessful because of invalid user access token");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of invalid user access token");

		// Share Gift Card to user #2 using valid data
		TestListeners.extentTest.get().info("== Mobile API v1: Share Gift Card to user #2 using valid data ==");
		logger.info("== Mobile API v1: Share Gift Card to user #2 using valid data ==");
		Response shareGiftCardResp = pageObj.endpoints().Api1ShareGiftCard(userEmail2, dataSet.get("client"),
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Share gift card");
		String actualShareGiftCardSuccessMsg = shareGiftCardResp.jsonPath().get("[0]").toString();
		String expectedShareGiftCardSuccessMsg = "Gift Card shared with " + fName2 + " " + lName2 + " successfully.";
		Assert.assertEquals(actualShareGiftCardSuccessMsg.toLowerCase(), expectedShareGiftCardSuccessMsg.toLowerCase(),
				"Message did not match");
		TestListeners.extentTest.get().pass("API v1 Share Gift Card to user #2 is successful");
		logger.info("API v1 Share Gift Card to user #2 is successful");

		// Share Gift Card to user #2 using already shared gift card uuid
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Share Gift Card to user #2 using already shared gift card uuid ==");
		logger.info("== Mobile API v1: Share Gift Card to user #2 using already shared gift card uuid ==");
		Response shareGiftCardAlreadySharedResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardAlreadySharedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Share gift card");
		boolean isApi1ShareGiftCardAlreadySharedSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardAlreadySharedResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardAlreadySharedSchemaValidated,
				"API1 Share Gift Card Schema Validation failed");
		String actualShareGiftCardAlreadySharedMsg = shareGiftCardAlreadySharedResponse.jsonPath().get("[0]")
				.toString();
		String expectedShareGiftCardAlreadySharedMsg = "You're already sharing this card with " + fName2 + " " + lName2
				+ ".";
		Assert.assertEquals(actualShareGiftCardAlreadySharedMsg.toLowerCase(),
				expectedShareGiftCardAlreadySharedMsg.toLowerCase(), "Message did not match");
		TestListeners.extentTest.get().pass(
				"API v1 Share Gift Card to user #2 is unsuccessful because of using already shared gift card uuid");
		logger.info("API v1 Share Gift Card to user #2 is unsuccessful because of using already shared gift card uuid");

		// Purchase Gift Card #2 (new gift card to transfer to user #2)
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Purchase Gift Card #2 (new gift card to transfer to user #2) ==");
		logger.info("== Mobile API v1: Purchase Gift Card #2 (new gift card to transfer to user #2) ==");
		String giftCardUuid2 = "";
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("API v1 Purchase gift card hit count is: " + attempts);
				logger.info("API v1 Purchase gift card hit count is: " + attempts);
				Response purchaseGiftCardResponse = pageObj.endpoints().api1PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("designId"),
						dataSet.get("transactionToken"), dataSet.get("expDate"));
				TestListeners.extentTest.get()
						.info("API v1 Purchase gift card call response is: " + purchaseGiftCardResponse.asString());
				giftCardUuid2 = purchaseGiftCardResponse.jsonPath().get("uuid").toString();
				if (giftCardUuid2 != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 purchase gift card");
					TestListeners.extentTest.get().pass(
							"API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid2);
					logger.info(
							"API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid2);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
			utils.longwait(5000);
		}

		// Transfer Gift Card #2 to user #2 with missing amount
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with missing amount ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with missing amount ==");
		Response transferGiftCardMissingAmountResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), "\"\"", token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardMissingAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardMissingAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, transferGiftCardMissingAmountResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardMissingAmountSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardMissingAmountMsg = transferGiftCardMissingAmountResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(transferGiftCardMissingAmountMsg,
				"Required parameter missing or the value is empty: amount", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of missing amount");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of missing amount");

		// Transfer Gift Card #2 to user #2 with missing email
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with missing email ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with missing email ==");
		Response transferGiftCardMissingEmailResponse = pageObj.endpoints().Api1TransferGiftCard("",
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardMissingEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardMissingEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, transferGiftCardMissingEmailResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardMissingEmailSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardMissingEmailMsg = transferGiftCardMissingEmailResponse.jsonPath().get("errors[0]")
				.toString();
		Assert.assertEquals(transferGiftCardMissingEmailMsg,
				"Required parameters - email, phone and fb_uid are missing or the values are empty.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of missing email");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of missing email");

		// Transfer Gift Card #2 to user #2 with invalid email
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with invalid email ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid email ==");
		Response transferGiftCardInvalidEmailResponse = pageObj.endpoints().Api1TransferGiftCard(invalidEmail,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferGiftCardInvalidEmailResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardInvalidEmailSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardInvalidEmailMsg = transferGiftCardInvalidEmailResponse.jsonPath().get("[0]").toString();
		// Assert.assertEquals(transferGiftCardInvalidEmailMsg, giftCardInvalidEmailMsg,
		// "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid email");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid email");

		// Transfer Gift Card #2 to user #2 with invalid gift card uuid
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid gift card uuid ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid gift card uuid ==");
		Response transferGiftCardInvalidUuidResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), token, invalidId);
		Assert.assertEquals(transferGiftCardInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferGiftCardInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardInvalidUuidSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardInvalidUuidMsg = transferGiftCardInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(transferGiftCardInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid gift card uuid");

		// Transfer Gift Card #2 to user #2 with large amount
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with large amount ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with large amount ==");
		Response transferGiftCardLargeAmountResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), "1000", token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardLargeAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardLargeAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, transferGiftCardLargeAmountResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardLargeAmountSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardLargeAmountMsg = transferGiftCardLargeAmountResponse.jsonPath().get("errors.base[0]")
				.toString();
		Assert.assertEquals(transferGiftCardLargeAmountMsg, giftCardBalanceNotUpdatedMsg, "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of large amount");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of large amount");

		// Transfer Gift Card #2 to user #2 using valid data
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 using valid data ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 using valid data ==");
		Response transferGiftCardResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2, dataSet.get("client"),
				dataSet.get("secret"), "7", token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Transfer gift card due to "
						+ transferGiftCardResponse.asString());
		String actualTransferGiftCardSuccessMsg = transferGiftCardResponse.jsonPath().get("[0]").toString();
		String expectedTransferGiftCardSuccessMsg = "You have successfully transferred $7.00 to " + fName2 + " "
				+ lName2 + ".";
		Assert.assertEquals(actualTransferGiftCardSuccessMsg.toLowerCase(),
				expectedTransferGiftCardSuccessMsg.toLowerCase(), "Message did not match");
		TestListeners.extentTest.get().pass("API v1 Transfer Gift Card to user #2 is successful");
		logger.info("API v1 Transfer Gift Card to user #2 is successful");

		// Transfer Gift Card #2 to user #2 using already transferred gift card uuid
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Transfer Gift Card to user #2 using already transferred gift card uuid ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 using already transferred gift card uuid ==");
		Response transferGiftCardAlreadyTransferredResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), "40", token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardAlreadyTransferredResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardAlreadyTransferredSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, transferGiftCardAlreadyTransferredResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardAlreadyTransferredSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardAlreadyTransferredMsg = transferGiftCardAlreadyTransferredResponse.jsonPath()
				.get("errors.base[0]").toString();
		Assert.assertEquals(transferGiftCardAlreadyTransferredMsg, giftCardBalanceNotUpdatedMsg,
				"Message did not match");
		TestListeners.extentTest.get().pass(
				"API v1 Transfer Gift Card to user #2 is unsuccessful because of using already transferred gift card uuid");
		logger.info(
				"API v1 Transfer Gift Card to user #2 is unsuccessful because of using already transferred gift card uuid");

		// Transfer Gift Card #2 to user #2 with invalid client
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with invalid client ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid client ==");
		Response transferGiftCardInvalidClientResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				invalidValue, dataSet.get("secret"), dataSet.get("amount"), token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardInvalidSignatureSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardInvalidClientMsg = transferGiftCardInvalidClientResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(transferGiftCardInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid client");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid client");

		// Transfer Gift Card #2 to user #2 with invalid secret
		TestListeners.extentTest.get().info("== Mobile API v1: Transfer Gift Card to user #2 with invalid secret ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid secret ==");
		Response transferGiftCardInvalidSecretResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), invalidValue, dataSet.get("amount"), token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Transfer gift card");
		String transferGiftCardInvalidSecretMsg = transferGiftCardInvalidSecretResponse.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(transferGiftCardInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid secret");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid secret");

		// Transfer Gift Card #2 to user #2 with invalid user access token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid user access token ==");
		logger.info("== Mobile API v1: Transfer Gift Card to user #2 with invalid user access token ==");
		Response transferGiftCardInvalidUserTokenResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("amount"), invalidValue, giftCardUuid2);
		Assert.assertEquals(transferGiftCardInvalidUserTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Transfer gift card");
		boolean isApi1TransferGiftCardInvalidUserTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, transferGiftCardInvalidUserTokenResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardInvalidUserTokenSchemaValidated,
				"API1 Transfer Gift Card Schema Validation failed");
		String transferGiftCardInvalidUserTokenMsg = transferGiftCardInvalidUserTokenResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(transferGiftCardInvalidUserTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid user access token");
		logger.info("API v1 Transfer Gift Card to user #2 is unsuccessful because of invalid user access token");

		// Fetch Gift Card Transaction History using invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid client ==");
		logger.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid client ==");
		Response giftCardHistoryInvalidClientResponse = pageObj.endpoints().api1GiftCardTransactionHistory(invalidValue,
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(giftCardHistoryInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Gift Card Transaction History");
		boolean isApi1GiftCardHistoryInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftCardHistoryInvalidClientResponse.asString());
		Assert.assertTrue(isApi1GiftCardHistoryInvalidSignatureSchemaValidated,
				"API1 Fetch Gift Card Transaction History Schema Validation failed");
		String giftCardHistoryInvalidClientMsg = giftCardHistoryInvalidClientResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardHistoryInvalidClientMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid client");
		logger.info("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid client");

		// Fetch Gift Card Transaction History using invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid secret ==");
		logger.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid secret ==");
		Response giftCardHistoryInvalidSecretResponse = pageObj.endpoints()
				.api1GiftCardTransactionHistory(dataSet.get("client"), invalidValue, token, giftCardUuid);
		Assert.assertEquals(giftCardHistoryInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v1 Fetch Gift Card Transaction History");
		String giftCardHistoryInvalidSecretMsg = giftCardHistoryInvalidSecretResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardHistoryInvalidSecretMsg, "Invalid Signature", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid secret");
		logger.info("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid secret");

		// Fetch Gift Card Transaction History using invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid token ==");
		logger.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid token ==");
		Response giftCardHistoryInvalidTokenResponse = pageObj.endpoints().api1GiftCardTransactionHistory(
				dataSet.get("client"), dataSet.get("secret"), invalidValue, giftCardUuid);
		Assert.assertEquals(giftCardHistoryInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v1 Fetch Gift Card Transaction History");
		boolean isApi1GiftCardHistoryInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, giftCardHistoryInvalidTokenResponse.asString());
		Assert.assertTrue(isApi1GiftCardHistoryInvalidTokenSchemaValidated,
				"API1 Fetch Gift Card Transaction History Schema Validation failed");
		String giftCardHistoryInvalidTokenMsg = giftCardHistoryInvalidTokenResponse.jsonPath().get("error").toString();
		Assert.assertEquals(giftCardHistoryInvalidTokenMsg, "You need to sign in or sign up before continuing.",
				"Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid token");
		logger.info("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid token");

		// Fetch Gift Card Transaction History using invalid gift card uuid
		TestListeners.extentTest.get()
				.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid gift card uuid ==");
		logger.info("== Mobile API v1: Fetch Gift Card Transaction History using invalid gift card uuid ==");
		Response giftCardHistoryInvalidUuidResponse = pageObj.endpoints()
				.api1GiftCardTransactionHistory(dataSet.get("client"), dataSet.get("secret"), token, invalidId);
		Assert.assertEquals(giftCardHistoryInvalidUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 Fetch Gift Card Transaction History");
		boolean isApi1GiftCardHistoryInvalidUuidSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftCardHistoryInvalidUuidResponse.asString());
		Assert.assertTrue(isApi1GiftCardHistoryInvalidUuidSchemaValidated,
				"API1 Fetch Gift Card Transaction History Schema Validation failed");
		String giftCardHistoryInvalidUuidMsg = giftCardHistoryInvalidUuidResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(giftCardHistoryInvalidUuidMsg, "Gift Card not found.", "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid gift card uuid");
		logger.info("API v1 Fetch Gift Card Transaction History is unsuccessful because of invalid gift card uuid");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
