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
public class MobileAPINegativeTest {
	private static Logger logger = LogManager.getLogger(MobileAPINegativeTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities();
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T3083 Verify Api2 user signup login and logout negative flow", groups = "api", priority = 0)
	public void verify_Api2_UserSignUp_Login_Logout_Negative() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String invalidemail = "abc";

		// user register/signup using API2 Signup with invalid client id
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for api2 signup");
		boolean isUserSignUpInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, signUpResponse.asString());
		Assert.assertTrue(isUserSignUpInvalidClientSchemaValidated, "API v2 User Sign-up Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user signup is unsuccessful because of invalid client id");

		// user register/signup using API2 Signup with invalid secret id
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for api2 signup");
		boolean isUserSignUpInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, signUpResponse1.asString());
		Assert.assertTrue(isUserSignUpInvalidSecretSchemaValidated, "API v2 User Sign-up Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user signup is unsuccessful because of invalid secret id");

		// user register/signup using API2 Signup with invalid email
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(invalidemail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api2 signup");
		boolean isUserSignUpInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2EmailErrorSchema, signUpResponse2.asString());
		Assert.assertTrue(isUserSignUpInvalidEmailSchemaValidated, "API v2 User Sign-up Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user signup is unsuccessful because of invalid mail");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful ");

		// User login using API2 Signin
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 login");
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user Login is successful ");

		// User login using invalid client id
		Response loginResponse1 = pageObj.endpoints().Api2Login(userEmail, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for api2 login");
		boolean isUserLoginInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, loginResponse1.asString());
		Assert.assertTrue(isUserLoginInvalidClientSchemaValidated, "API v2 User Login Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful because of invalit client id");

		// user login using invalid secret id
		Response loginResponse2 = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for api2 login");
		boolean isUserLoginInvalidSecretSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2InvalidSignatureSchema, loginResponse2.asString());
		Assert.assertTrue(isUserLoginInvalidSecretSchemaValidated, "API v2 User Login Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful ");

		// user login using invalid email
		Response loginResponse3 = pageObj.endpoints().Api2Login(invalidemail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api2 login");
		boolean isUserLoginInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2BaseErrorSchema, loginResponse3.asString());
		Assert.assertTrue(isUserLoginInvalidEmailSchemaValidated, "API v2 User Login Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful because of invalid mail");

		// User logout using invalid client id
		Response logoutResponse1 = pageObj.endpoints().Api2Logout(token, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		Assert.assertEquals(logoutResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for api2 logout");
		boolean isUserLogoutInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, logoutResponse1.asString());
		Assert.assertTrue(isUserLogoutInvalidClientSchemaValidated, "API v2 User Logout Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful because the client id ");

		// User logout using invalid secret id
		Response logoutResponse2 = pageObj.endpoints().Api2Logout(token, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(logoutResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for api2 logout");
		boolean isUserLogoutInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, logoutResponse2.asString());
		Assert.assertTrue(isUserLogoutInvalidSecretSchemaValidated, "API v2 User Logout Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful ");

		// User logout using invalid token
		String invalidtoken = "1";
		Response logoutResponse3 = pageObj.endpoints().Api2Logout(invalidtoken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(logoutResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api2 logout");
		boolean isUserLogoutInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2AccessTokenErrorSchema, logoutResponse3.asString());
		Assert.assertTrue(isUserLogoutInvalidTokenSchemaValidated, "API v2 User Logout Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful ");
	}

	@Test(description = "SQ-T3082 Verify fetch user info and update user profile negative flow; "
			+ "SQ-T4749 Verify API2 User Show negative scenarios; "
			+ "SQ-T4767 Verify API2 Fetch Rolling Points Expiry negative scenarios", groups = "api", priority = 1)
	public void verify_FetchUserinfo_UpdateUserProfileNegative() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String user_id = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// API2 User show using invalid token
		String invalidtoken = token + "0";
		Response userShowInvalidTokenResponse = pageObj.endpoints().api2UserShow(invalidtoken, user_id,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(userShowInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 user show");
		boolean isUserShowInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, userShowInvalidTokenResponse.asString());
		Assert.assertTrue(isUserShowInvalidTokenSchemaValidated, "API v2 User Show Schema Validation failed");
		TestListeners.extentTest.get().pass("API2 user show call is unsuccessful because of invalid token");

		// API2 User show using invalid user_id
		Response userShowInvalidUserIdResponse = pageObj.endpoints().api2UserShow(token, "", dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userShowInvalidUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API2 user show");
		TestListeners.extentTest.get().pass("API2 user show call is unsuccessful because of invalid user_id");

		// API2 User show using invalid client
		Response userShowInvalidClientResponse = pageObj.endpoints().api2UserShow(token, user_id,
				dataSet.get("invalidclient"), dataSet.get("secret"));
		Assert.assertEquals(userShowInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 user show");
		boolean isUserShowInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, userShowInvalidClientResponse.asString());
		Assert.assertTrue(isUserShowInvalidClientSchemaValidated, "API v2 User Show Schema Validation failed");
		TestListeners.extentTest.get().pass("API2 user show call is unsuccessful because of invalid client");

		// API2 User show using invalid secret
		Response userShowInvalidSecretResponse = pageObj.endpoints().api2UserShow(token, user_id, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(userShowInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API2 user show");
		boolean isUserShowInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, userShowInvalidSecretResponse.asString());
		Assert.assertTrue(isUserShowInvalidSecretSchemaValidated, "API v2 User Show Schema Validation failed");
		TestListeners.extentTest.get().pass("API2 user show call is unsuccessful because of invalid secret");

		// Fetch user information using invalid client
		Response userInfoResponse = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		Assert.assertEquals(userInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch user info");
		boolean isFetchUserInfoInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, userInfoResponse.asString());
		Assert.assertTrue(isFetchUserInfoInvalidClientSchemaValidated,
				"API v2 Fetch User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 fetch user information is unsuccessful because of invalid client");

		// Fetch user information using invalid secret
		Response userInfoResponse1 = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(userInfoResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch user info");
		boolean isFetchUserInfoInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, userInfoResponse1.asString());
		Assert.assertTrue(isFetchUserInfoInvalidSecretSchemaValidated,
				"API v2 Fetch User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 fetch user information is unsuccessful because of invalid secret");

		// Fetch user information using invalid token
		Response userInfoResponse2 = pageObj.endpoints().Api2FetchUserInfo(invalidtoken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userInfoResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch user info");
		boolean isFetchUserInfoInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, userInfoResponse2.asString());
		Assert.assertTrue(isFetchUserInfoInvalidTokenSchemaValidated,
				"API v2 Fetch User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 fetch user information is unsuccessful because of invalid token");

		// Verify Fetch Rolling Points Expiry on business where feature is disabled
		Response fetchExpiryPointsDisabledResponse = pageObj.endpoints()
				.Api2FetchUserExpiringPoints(dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(fetchExpiryPointsDisabledResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API2 Fetch Rolling Points Expiry");
		boolean isFetchExpiryPointsDisabledFeatureSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2DisabledFeatureErrorObjectSchema,
				fetchExpiryPointsDisabledResponse.asString());
		Assert.assertTrue(isFetchExpiryPointsDisabledFeatureSchemaValidated,
				"API v2 Fetch Rolling Points Expiry Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Rolling Points Expiry is unsuccessful because of business with disabled feature");

		// Verify Fetch Rolling Points Expiry using invalid client
		Response fetchExpiryPointsInvalidClientResponse = pageObj.endpoints()
				.Api2FetchUserExpiringPoints(dataSet.get("invalidclient"), dataSet.get("secret"), token);
		Assert.assertEquals(fetchExpiryPointsInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Fetch Rolling Points Expiry");
		boolean isFetchExpiryPointsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchExpiryPointsInvalidClientResponse.asString());
		Assert.assertTrue(isFetchExpiryPointsInvalidClientSchemaValidated,
				"API v2 Fetch Rolling Points Expiry Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Rolling Points Expiry is unsuccessful because of invalid client");

		// Verify Fetch Rolling Points Expiry using invalid secret
		Response fetchExpiryPointsInvalidSecretResponse = pageObj.endpoints()
				.Api2FetchUserExpiringPoints(dataSet.get("client"), dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchExpiryPointsInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API2 Fetch Rolling Points Expiry");
		boolean isFetchExpiryPointsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchExpiryPointsInvalidSecretResponse.asString());
		Assert.assertTrue(isFetchExpiryPointsInvalidSecretSchemaValidated,
				"API v2 Fetch Rolling Points Expiry Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Rolling Points Expiry is unsuccessful because of invalid secret");

		// Verify Fetch Rolling Points Expiry using invalid token
		Response fetchExpiryPointsInvalidTokenResponse = pageObj.endpoints()
				.Api2FetchUserExpiringPoints(dataSet.get("client"), dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchExpiryPointsInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Fetch Rolling Points Expiry");
		boolean isFetchExpiryPointsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchExpiryPointsInvalidTokenResponse.asString());
		Assert.assertTrue(isFetchExpiryPointsInvalidTokenSchemaValidated,
				"API v2 Fetch Rolling Points Expiry Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Rolling Points Expiry is unsuccessful because of invalid token");

		// Update user profile with invalid client
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("invalidclient"),
				userEmail, dataSet.get("secret"), token);
		Assert.assertEquals(updateUserInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 update user info");
		boolean isUpdateUserInfoInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, updateUserInfoResponse.asString());
		Assert.assertTrue(isUpdateUserInfoInvalidClientSchemaValidated,
				"API v2 Update User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful because of invalid client");

		// Update user profile with invalid secret
		Response updateUserInfoResponse1 = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(updateUserInfoResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 update user info");
		boolean isUpdateUserInfoInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, updateUserInfoResponse1.asString());
		Assert.assertTrue(isUpdateUserInfoInvalidSecretSchemaValidated,
				"API v2 Update User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful because of invalid secret");

		// Update user profile with invalid token
		Response updateUserInfoResponse2 = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(updateUserInfoResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 update user info");
		boolean isUpdateUserInfoInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, updateUserInfoResponse2.asString());
		Assert.assertTrue(isUpdateUserInfoInvalidTokenSchemaValidated,
				"API v2 Update User Info Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful because of invalid token");
	}

	@Test(description = "SQ-T3086 Verify create update and delete user relation negative flow", groups = "api", priority = 2)
	public void verify_Create_update_delete_User_Relation_Negative() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		int id = createUserRelationResponse.jsonPath().get("id");
		TestListeners.extentTest.get().pass("Api2 Create user relation is successful");

		// Create user relation with invalid client
		Response createUserRelationResponse0 = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 signup");
		boolean isCreateUserRelationInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, createUserRelationResponse0.asString());
		Assert.assertTrue(isCreateUserRelationInvalidClientSchemaValidated,
				"API v2 Create User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create user relation is unsuccessful because of invalid client id");

		// Create user relation with invalid secret
		Response createUserRelationResponse1 = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(createUserRelationResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 signup");
		boolean isCreateUserRelationInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, createUserRelationResponse1.asString());
		Assert.assertTrue(isCreateUserRelationInvalidSecretSchemaValidated,
				"API v2 Create User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create user relation is unsuccessful because of invalid secret id");

		// Create user relation with invalid token
		String invalidtoken = token + "0";
		Response createUserRelationResponse2 = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(createUserRelationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 signup");
		boolean isCreateUserRelationInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, createUserRelationResponse2.asString());
		Assert.assertTrue(isCreateUserRelationInvalidTokenSchemaValidated,
				"API v2 Create User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create user relation is unsuccessful because of invalid token id");

		// Update user relation with invalid client id
		Response updateUserRelationResponse = pageObj.endpoints().Api2UpdateUserrelation(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, id);
		Assert.assertEquals(updateUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 signup");
		boolean isUpdateUserRelationInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, updateUserRelationResponse.asString());
		Assert.assertTrue(isUpdateUserRelationInvalidClientSchemaValidated,
				"API v2 Update User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Update user relation is unsuccessful because of invalid token");

		// Update user relation with invalid secret id
		Response updateUserRelationResponse1 = pageObj.endpoints().Api2UpdateUserrelation(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, id);
		Assert.assertEquals(updateUserRelationResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 signup");
		boolean isUpdateUserRelationInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, updateUserRelationResponse1.asString());
		Assert.assertTrue(isUpdateUserRelationInvalidSecretSchemaValidated,
				"API v2 Update User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Update user relation is unsuccessful because of invalid token");

		// Update user relation with invalid token
		Response updateUserRelationResponse2 = pageObj.endpoints().Api2UpdateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, id);
		Assert.assertEquals(updateUserRelationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 signup");
		boolean isUpdateUserRelationInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, updateUserRelationResponse2.asString());
		Assert.assertTrue(isUpdateUserRelationInvalidTokenSchemaValidated,
				"API v2 Update User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Update user relation is unsuccessful because of invalid token");

		// Update user relation with invalid id
		int invalidid = 1;
		Response updateUserRelationResponse3 = pageObj.endpoints().Api2UpdateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token, invalidid);
		Assert.assertEquals(updateUserRelationResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 signup");
		boolean isUpdateUserRelationInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BaseErrorSchema, updateUserRelationResponse3.asString());
		Assert.assertTrue(isUpdateUserRelationInvalidIdSchemaValidated,
				"API v2 Update User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Update user relation is unsuccessful because of invalid token");

		// Delete user relation with invalid client id
		Response deleteUserRelationResponse = pageObj.endpoints().Api2DeleteUserRelation(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, Integer.toString(id));
		Assert.assertEquals(deleteUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 signup");
		boolean isDeleteUserRelationInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, deleteUserRelationResponse.asString());
		Assert.assertTrue(isDeleteUserRelationInvalidClientSchemaValidated,
				"API v2 Delete User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete user relation is unsuccessful because of invalid client");

		// Delete user relation with invalid secret id
		Response deleteUserRelationResponse1 = pageObj.endpoints().Api2DeleteUserRelation(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, Integer.toString(id));
		Assert.assertEquals(deleteUserRelationResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 signup");
		boolean isDeleteUserRelationInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, deleteUserRelationResponse1.asString());
		Assert.assertTrue(isDeleteUserRelationInvalidSecretSchemaValidated,
				"API v2 Delete User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete user relation is unsuccessful because of invalid secret");

		// Delete user relation with invalid token
		Response deleteUserRelationResponse2 = pageObj.endpoints().Api2DeleteUserRelation(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, Integer.toString(id));
		Assert.assertEquals(deleteUserRelationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 signup");
		boolean isDeleteUserRelationInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, deleteUserRelationResponse2.asString());
		Assert.assertTrue(isDeleteUserRelationInvalidTokenSchemaValidated,
				"API v2 Delete User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete user relation is unsuccessful because of invalid token");

		// Delete user relation with invalid id
		Response deleteUserRelationResponse3 = pageObj.endpoints().Api2DeleteUserRelation(dataSet.get("client"),
				dataSet.get("secret"), token, Integer.toString(invalidid));
		Assert.assertEquals(deleteUserRelationResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 signup");
		boolean isDeleteUserRelationInvalidIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BaseErrorSchema, deleteUserRelationResponse3.asString());
		Assert.assertTrue(isDeleteUserRelationInvalidIdSchemaValidated,
				"API v2 Delete User Relation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete user relation is unsuccessful because of invalid id");
	}

	@Test(description = "SQ-T3089 Verify asynchronous user update and get user session token negative flow", groups = "api", priority = 3)
	public void verify_Asynchronous_User_Update_Get_User_Session_Token_Negative() {
		String invalidtoken = "1";
		String invalidUserEmail = "abc";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

//		// Asynchronous User Update with invalid client id
//		Response updateUserInfoResponse = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("invalidclient"),
//				userEmail, dataSet.get("secret"), token);
//		Assert.assertEquals(updateUserInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
//				"Status code 401 did not matched for api2 asynchronous user update");
//		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful");

//		// Asynchronous User Update with invalid secret id
//		Response updateUserInfoResponse1 = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
//				userEmail, dataSet.get("invalidsecret"), token);
//		Assert.assertEquals(updateUserInfoResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, //				"Status code 412 did not matched for api2 asynchronous user update");
//		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful");

//		// Asynchronous User Update with invalid token
//		Response updateUserInfoResponse2 = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
//				userEmail, dataSet.get("secret"), invalidtoken);
//		Assert.assertEquals(updateUserInfoResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, //				"Status code 401 did not matched for api2 asynchronous user update");
//		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful");
//
//		// Asynchronous User Update with invalid email
//		Response updateUserInfoResponse3 = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
//				invalidUserEmail, dataSet.get("secret"), token);
//		Assert.assertEquals(updateUserInfoResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, //				"Status code 404 did not matched for api2 asynchronous user update");
//		TestListeners.extentTest.get().pass("Api2 update user information is unsuccessful");

		// Get User Session Token with invalid client
		Response sessionTokenResponse = pageObj.endpoints().Api2UserSessionToken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(sessionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 get user session token");
		boolean isSessionTokenInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, sessionTokenResponse.asString());
		Assert.assertTrue(isSessionTokenInvalidClientSchemaValidated,
				"API v2 Get User Session Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 get user session token is unsuccessful because of invalid client");

		// Get User Session Token with invalid secret
		Response sessionTokenResponse1 = pageObj.endpoints().Api2UserSessionToken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(sessionTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 get user session token");
		boolean isSessionTokenInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, sessionTokenResponse1.asString());
		Assert.assertTrue(isSessionTokenInvalidSecretSchemaValidated,
				"API v2 Get User Session Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 get user session token is unsuccessful because of invalid secret");

		// Get User Session Token with invalid token
		Response sessionTokenResponse2 = pageObj.endpoints().Api2UserSessionToken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(sessionTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 get user session token");
		boolean isSessionTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, sessionTokenResponse2.asString());
		Assert.assertTrue(isSessionTokenInvalidTokenSchemaValidated,
				"API v2 Get User Session Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 get user session token is unsuccessful because of invalid token");

		// Send Verification Email with invalid client id
		Response verificationEmailResponse = pageObj.endpoints().Api2SendVreificationEmail(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(verificationEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 412 did not matched for api2 get user session token");
		boolean isVerificationEmailInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, verificationEmailResponse.asString());
		Assert.assertTrue(isVerificationEmailInvalidClientSchemaValidated,
				"API v2 Send Verification Email Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Send Verification Email is unsuccessful because of invalid client");

		// Send Verification Email with invalid secret id
		Response verificationEmailResponse1 = pageObj.endpoints().Api2SendVreificationEmail(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(verificationEmailResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 get user session token");
		boolean isVerificationEmailInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, verificationEmailResponse1.asString());
		Assert.assertTrue(isVerificationEmailInvalidSecretSchemaValidated,
				"API v2 Send Verification Email Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Send Verification Email is unsuccessful because of invalid secret");

		// Send Verification Email with invalid token
		Response verificationEmailResponse2 = pageObj.endpoints().Api2SendVreificationEmail(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(verificationEmailResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 get user session token");
		boolean isVerificationEmailInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, verificationEmailResponse2.asString());
		Assert.assertTrue(isVerificationEmailInvalidTokenSchemaValidated,
				"API v2 Send Verification Email Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Send Verification Email is unsuccessful because of invalid token");
	}

	@Test(description = "SQ-T3084 Verify forgot password and fetch user balance negative flow", groups = "api", priority = 4)
	public void verify_forgot_password_and_fetch_user_balance_Negative() {
		String invalidUserEmail = "abc";
		String invalidtoken = "1";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Forgot password with invalid client id
		Response forgotPasswordResponse = pageObj.endpoints().Api2ForgotPassword(dataSet.get("invalidclient"),
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 forgot password");
		boolean isForgotPasswordInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, forgotPasswordResponse.asString());
		Assert.assertTrue(isForgotPasswordInvalidClientSchemaValidated,
				"API v2 Forgot Password Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Forgot password is unsuccessful because of invalid client");

		// Forgot password with invalid secret id
		Response forgotPasswordResponse1 = pageObj.endpoints().Api2ForgotPassword(dataSet.get("client"),
				dataSet.get("invalidsecret"), userEmail);
		Assert.assertEquals(forgotPasswordResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 forgot password");
		boolean isForgotPasswordInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, forgotPasswordResponse1.asString());
		Assert.assertTrue(isForgotPasswordInvalidSecretSchemaValidated,
				"API v2 Forgot Password Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Forgot password is unsuccessful because of invalid secret");

		// Forgot password with invalid mail
//		Response forgotPasswordResponse2 = pageObj.endpoints().Api2ForgotPassword(dataSet.get("client"),
//				dataSet.get("secret"), invalidUserEmail);
//		Assert.assertEquals(forgotPasswordResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, //				"Status code 404 did not matched for api2 forgot password");
//		TestListeners.extentTest.get().pass("Api2 Forgot password is unsuccessful");

		// Fetch user balance with invalid client id
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch user balance");
		boolean isUserBalanceInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, userBalanceResponse.asString());
		Assert.assertTrue(isUserBalanceInvalidClientSchemaValidated,
				"API v2 Fetch User Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is unsuccessful because of invalid client");

		// Fetch user balance with invalid secret id
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch user balance");
		boolean isUserBalanceInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, userBalanceResponse1.asString());
		Assert.assertTrue(isUserBalanceInvalidSecretSchemaValidated,
				"API v2 Fetch User Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is unsuccessful because of invalid secret");

		// Fetch user balance with invalid token
		Response userBalanceResponse2 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(userBalanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch user balance");
		boolean isUserBalanceInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, userBalanceResponse2.asString());
		Assert.assertTrue(isUserBalanceInvalidTokenSchemaValidated,
				"API v2 Fetch User Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is unsuccessful because of invalid token");

		// Balance Timeline with invalid client id
		Response balanceTimelineResponse = pageObj.endpoints().Api2BalanceTimeline(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(balanceTimelineResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 balance timeline");
		boolean isBalanceTimelineInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, balanceTimelineResponse.asString());
		Assert.assertTrue(isBalanceTimelineInvalidClientSchemaValidated,
				"API v2 Balance Timeline Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Balance Timeline is unsuccessful because of invalid client");

		// Balance Timeline with invalid secret id
		Response balanceTimelineResponse1 = pageObj.endpoints().Api2BalanceTimeline(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(balanceTimelineResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 balance timeline");
		boolean isBalanceTimelineInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, balanceTimelineResponse1.asString());
		Assert.assertTrue(isBalanceTimelineInvalidSecretSchemaValidated,
				"API v2 Balance Timeline Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Balance Timeline is unsuccessful because of invalid secret");

		// Balance Timeline with invalid token
		Response balanceTimelineResponse2 = pageObj.endpoints().Api2BalanceTimeline(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(balanceTimelineResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 balance timeline");
		boolean isBalanceTimelineInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, balanceTimelineResponse2.asString());
		Assert.assertTrue(isBalanceTimelineInvalidTokenSchemaValidated,
				"API v2 Balance Timeline Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Balance Timeline is unsuccessful because of invalid token");

		// Estimate Points Earning with invalid client
		Response pointsEarningResponse = pageObj.endpoints().Api2EstimatePointsEarning(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(pointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 estimate points earning");
		boolean isEstimatePointsEarningInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, pointsEarningResponse.asString());
		Assert.assertTrue(isEstimatePointsEarningInvalidClientSchemaValidated,
				"API v2 Estimate Points Earning Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2  Estimate Points Earning is unsuccessful because of invalid client");

		// Estimate Points Earning with invalid secret
		Response pointsEarningResponse1 = pageObj.endpoints().Api2EstimatePointsEarning(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(pointsEarningResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 estimate points earning");
		boolean isEstimatePointsEarningInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, pointsEarningResponse1.asString());
		Assert.assertTrue(isEstimatePointsEarningInvalidSecretSchemaValidated,
				"API v2 Estimate Points Earning Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2  Estimate Points Earning is unsuccessful because of invalid secret");

		// Estimate Points Earning with invalid token
		Response pointsEarningResponse2 = pageObj.endpoints().Api2EstimatePointsEarning(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(pointsEarningResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 estimate points earning");
		boolean isEstimatePointsEarningInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, pointsEarningResponse2.asString());
		Assert.assertTrue(isEstimatePointsEarningInvalidTokenSchemaValidated,
				"API v2 Estimate Points Earning Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2  Estimate Points Earning is unsuccessful because of invalid token");
	}

	@Test(description = "SQ-T3081 Verify Api2 old user signup login and logout negative flow", groups = "api", priority = 5)
	public void Api2OldUserLoginLogoutNegative() {
		String invalidtoken = "1";
		String unknownClientMsg = "Client ID is incorrect. Please check client param or contact us";
		String invalidSignatureMsg = "Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.";

		// User login using API2 Signin
		TestListeners.extentTest.get().info("== Mobile API2 login with existing user valid data ==");
		logger.info("== Mobile API2 login with existing user valid data ==");
		userEmail = "automation01@punchh.com";
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 login");
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user Login is successful ");
		logger.info("Api2 user login is successful");

		// Negative case: Existing User login using API2 with invalid client
		TestListeners.extentTest.get().info("== Mobile API2 existing user login with invalid client ==");
		logger.info(
				"== Mobile API2 existing user login with invalid client ==Mobile API2 existing user login with invalid client");
		Response loginResponse1 = pageObj.endpoints().Api2Login(userEmail, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		boolean isloginInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, loginResponse1.asString());
		Assert.assertTrue(isloginInvalidClientSchemaValidated,
				"API2 Existing User Login with invalid client Schema Validation failed");
		Assert.assertEquals(loginResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for api2 login");
		String loginInvalidClientMsg = loginResponse1.jsonPath().get("errors.unknown_client[0]").toString();
		Assert.assertEquals(loginInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful because of invalid client");
		logger.info("Api2 user login is unsuccessful because of invalid client");

		// Negative case: Existing User login using API2 with invalid secret
		TestListeners.extentTest.get().info("== Mobile API2 existing user login with invalid secret ==");
		logger.info("== Mobile API2 existing user login with invalid secret ==");
		Response loginResponse2 = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for api2 login");
		boolean isloginInvalidSecretSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2InvalidSignatureSchema, loginResponse2.asString());
		Assert.assertTrue(isloginInvalidSecretSchemaValidated,
				"API2 Existing User Login with invalid secret Schema Validation failed");
		String loginInvalidSecretMsg = loginResponse2.jsonPath().get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(loginInvalidSecretMsg, invalidSignatureMsg);
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful because of invalid secret");
		logger.info("Api2 user login is unsuccessful because of invalid secret");

		// Negative case: Existing User login using API2 with invalid email
		TestListeners.extentTest.get().info("== Mobile API2 existing user login with invalid email ==");
		logger.info("== Mobile API2 existing user login with invalid email ==");
		String invaliduserEmail = "automation";
		Response loginResponse3 = pageObj.endpoints().Api2Login(invaliduserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api2 login");
		boolean isloginInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema3, loginResponse3.asString());
		Assert.assertTrue(isloginInvalidEmailSchemaValidated,
				"API2 Existing User Login with invalid email Schema Validation failed");
		String loginInvalidEmailMsg = loginResponse3.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals(loginInvalidEmailMsg, "Incorrect information submitted. Please retry.");
		TestListeners.extentTest.get().pass("Api2 user Login is unsuccessful because of invalid mail");
		logger.info("Api2 user login is unsuccessful because of invalid email");

		// Negative case: Existing User logout using API2 with invalid client
		TestListeners.extentTest.get().info("== Mobile API2 existing user logout with invalid client ==");
		logger.info("== Mobile API2 existing user logout with invalid client ==");
		Response logoutResponse = pageObj.endpoints().Api2Logout(token, dataSet.get("invalidclient"),
				dataSet.get("secret"));
		Assert.assertEquals(logoutResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for api2 logout");
		boolean isLogoutInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, logoutResponse.asString());
		Assert.assertTrue(isLogoutInvalidClientSchemaValidated,
				"API2 Existing User Logout with invalid client Schema Validation failed");
		String logoutInvalidClientMsg = logoutResponse.jsonPath().get("errors.unknown_client[0]").toString();
		Assert.assertEquals(logoutInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful because of invalid client");
		logger.info("Api2 user logout is unsuccessful because of invalid client");

		// Negative case: Existing User logout using API2 with invalid secret
		TestListeners.extentTest.get().info("== Mobile API2 existing user logout with invalid secret ==");
		logger.info("== Mobile API2 existing user logout with invalid secret ==");
		Response logoutResponse1 = pageObj.endpoints().Api2Logout(token, dataSet.get("client"),
				dataSet.get("invalidsecret"));
		Assert.assertEquals(logoutResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED, "Status code 412 did not matched for api2 logout");
		boolean isLogoutInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, logoutResponse1.asString());
		Assert.assertTrue(isLogoutInvalidSecretSchemaValidated,
				"API2 Existing User Logout with invalid secret Schema Validation failed");
		String logoutInvalidSecretMsg = logoutResponse1.jsonPath().get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(logoutInvalidSecretMsg, invalidSignatureMsg);
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful because of invalid secret");
		logger.info("Api2 user logout is unsuccessful because of invalid secret");

		// Negative case: Existing User logout using API2 with invalid token
		TestListeners.extentTest.get().info("== Mobile API2 existing user logout with invalid token ==");
		logger.info("== Mobile API2 existing user logout with invalid token ==");
		Response logoutResponse2 = pageObj.endpoints().Api2Logout(invalidtoken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(logoutResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api2 logout");
		boolean isLogoutInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2AccessTokenErrorSchema, logoutResponse2.asString());
		Assert.assertTrue(isLogoutInvalidTokenSchemaValidated,
				"API2 Existing User Logout with invalid token Schema Validation failed");
		String logoutInvalidTokenMsg = logoutResponse2.jsonPath().get("errors.access_token").toString();
		Assert.assertEquals(logoutInvalidTokenMsg, "Invalid access_token or can't be blank");
		TestListeners.extentTest.get().pass("Api2 user Logout is unsuccessful because of invalid token");
		logger.info("Api2 user logout is unsuccessful because of invalid token");

	}

	@Test(description = "SQ-T3092 Verify list deals negative flow", groups = "api", priority = 6)
	public void verify_List_Deals_Negative() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

		// List all deals
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(listdealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals");
		String redeemableUUID = listdealsResponse.jsonPath().get("[0].redeemable_uuid").toString();

		// List all deals with invalid client
		Response listdealsResponsenegative = pageObj.endpoints().Api2ListAllDeals(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(listdealsResponsenegative.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 list all deals");
		boolean isListDealsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, listdealsResponsenegative.asString());
		Assert.assertTrue(isListDealsInvalidClientSchemaValidated, "API v2 List Deals Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 List all deals is unsuccessful because of invalid client");

		// List all deals with invalid secret
		Response listdealsResponse1 = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(listdealsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 list all deals");
		boolean isListDealsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, listdealsResponse1.asString());
		Assert.assertTrue(isListDealsInvalidSecretSchemaValidated, "API v2 List Deals Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 List all deals is unsuccessful because of invalid secret");

		// List all deals with invalid token
		String invalidtoken = "1";
		Response listdealsResponse2 = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				invalidtoken);
		Assert.assertEquals(listdealsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 list all deals");
		boolean isListDealsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, listdealsResponse2.asString());
		Assert.assertTrue(isListDealsInvalidTokenSchemaValidated, "API v2 List Deals Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 List all deals is unsuccessful because of invalid token");

		// Get details of deal with invalid client
		Response detailsDealsResponse = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, dataSet.get("redeemable_uuid"));
		Assert.assertEquals(detailsDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Get Deal Details");
		boolean isDealDetailsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, detailsDealsResponse.asString());
		Assert.assertTrue(isDealDetailsInvalidClientSchemaValidated,
				"API v2 Get Details of Deals Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get details of deal is unsuccessful because of invalid client");

		// Get details of deal with invalid secret
		Response detailsDealsResponse1 = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("redeemable_uuid"));
		Assert.assertEquals(detailsDealsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Get Deal Details");
		boolean isDealDetailsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, detailsDealsResponse1.asString());
		Assert.assertTrue(isDealDetailsInvalidSecretSchemaValidated,
				"API v2 Get Deal Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get details of deal is unsuccessful because of invalid secret");

		// Get details of deal with invalid token
		Response detailsDealsResponse2 = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, dataSet.get("redeemable_uuid"));
		Assert.assertEquals(detailsDealsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Get Deal Details");
		boolean isDealDetailsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, detailsDealsResponse2.asString());
		Assert.assertTrue(isDealDetailsInvalidTokenSchemaValidated, "API v2 Get Deal Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get details of deal is unsuccessful because of invalid token");

		// Get details of deal with invalid redeemable id
		Response detailsDealsResponse3 = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("client"),
				dataSet.get("secret"), token, "1");
		Assert.assertEquals(detailsDealsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v2 Get Deal Details");
		boolean isDealDetailsInvalidRedeemableIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, detailsDealsResponse3.asString());
		Assert.assertTrue(isDealDetailsInvalidRedeemableIdSchemaValidated,
				"API v2 Get Deal Details Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Api2 Get details of deal is unsuccessful because of invalid redeemable id");

		// Get details of deal
		Response detailsDealsResponse4 = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("client"),
				dataSet.get("secret"), token, redeemableUUID);
		String redeemable_uuid = detailsDealsResponse4.jsonPath().get("redeemable_uuid").toString();
		Assert.assertEquals(detailsDealsResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Get Deal Details");
		TestListeners.extentTest.get().pass("API v2 Get Deal Details call is successful");

		// Save selected deal with invalid client
		Response saveDealResponse = pageObj.endpoints().Api2SaveSelectedDeal(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, redeemable_uuid);
		Assert.assertEquals(saveDealResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 save selected deal");
		boolean isSaveDealInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, saveDealResponse.asString());
		Assert.assertTrue(isSaveDealInvalidClientSchemaValidated, "API v2 Save Selected Deal Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Save selected deal is unsuccessful because of invalid client");

		// Save selected deal with invalid secret
		Response saveDealResponse1 = pageObj.endpoints().Api2SaveSelectedDeal(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, redeemable_uuid);
		Assert.assertEquals(saveDealResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 save selected deal");
		boolean isSaveDealInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, saveDealResponse1.asString());
		Assert.assertTrue(isSaveDealInvalidSecretSchemaValidated, "API v2 Save Selected Deal Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Save selected deal is unsuccessful because of invalid secret");

		// Save selected deal with invalid token
		Response saveDealResponse2 = pageObj.endpoints().Api2SaveSelectedDeal(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, redeemable_uuid);
		Assert.assertEquals(saveDealResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 save selected deal");
		boolean isSaveDealInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, saveDealResponse2.asString());
		Assert.assertTrue(isSaveDealInvalidTokenSchemaValidated, "API v2 Save Selected Deal Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Save selected deal is unsuccessful because of invalid token");

		// Save selected deal with invalid id
		Response saveDealResponse3 = pageObj.endpoints().Api2SaveSelectedDeal(dataSet.get("client"),
				dataSet.get("secret"), token, "1");
		Assert.assertEquals(saveDealResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 save selected deal");
		boolean isSaveDealInvalidIdSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2NotFoundErrorSchema, saveDealResponse3.asString());
		Assert.assertTrue(isSaveDealInvalidIdSchemaValidated, "API v2 Save Selected Deal Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Save selected deal is unsuccessful because of invalid id ");

	}

	@Test(description = "SQ-T3085 Verify point conversion and user account history negative flow", groups = "api", priority = 7)
	public void verify_Point_Conversion_User_account_history_Negative() throws InterruptedException {
		String invalidtoken = "1";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Point conversion with invalid client
		Response pointsResponse = pageObj.endpoints().Api2PointConversion(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 point conversion");
		boolean isPointConversionInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, pointsResponse.asString());
		Assert.assertTrue(isPointConversionInvalidClientSchemaValidated,
				"API v2 Point Conversion Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 point conversion is unsuccessful because of invalid client id");

		// Point conversion with invalid secret
		Response pointsResponse1 = pageObj.endpoints().Api2PointConversion(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 point conversion");
		boolean isPointConversionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, pointsResponse1.asString());
		Assert.assertTrue(isPointConversionInvalidSecretSchemaValidated,
				"API v2 Point Conversion Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 point conversion is unsuccessful because of invalid secret id");

		// Point conversion with invalid token
		Response pointsResponse2 = pageObj.endpoints().Api2PointConversion(dataSet.get("client"), dataSet.get("secret"),
				invalidtoken, dataSet.get("conversionRuleId"));
		Assert.assertEquals(pointsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 point conversion");
		boolean isPointConversionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, pointsResponse2.asString());
		Assert.assertTrue(isPointConversionInvalidTokenSchemaValidated,
				"API v2 Point Conversion Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 point conversion is unsuccessful because of invalid token");

		// User Account History with invalid client
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 User Account History");
		boolean isAccountHistoryInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, accountHistoryResponse.asString());
		Assert.assertTrue(isAccountHistoryInvalidClientSchemaValidated,
				"API v2 User Account History Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user account history is unsuccessful because of invalid client");

		// User Account History with invalid secret
		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 User Account History");
		boolean isAccountHistoryInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, accountHistoryResponse1.asString());
		Assert.assertTrue(isAccountHistoryInvalidSecretSchemaValidated,
				"API v2 User Account History Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user account history is unsuccessful because of invalid secret");

		// User Account History with invalid token
		Response accountHistoryResponse2 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(accountHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 User Account History");
		boolean isAccountHistoryInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, accountHistoryResponse2.asString());
		Assert.assertTrue(isAccountHistoryInvalidTokenSchemaValidated,
				"API v2 User Account History Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 user account history is unsuccessful because of invalid token");

	}

	@Test(description = "SQ-T3090 Verify gifting loyalty reward to user negative flow", groups = "api", priority = 8)
	public void verify_Gifting_Loyalty_Reward_User_to_User_Negative() throws InterruptedException {
		String invalidtoken = "1";
		String invalidUserEmail = "a";
		String invalidreward_id = "1";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Gift loyalty reward to other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");

		// gifting loyalty reward to other user with invalid client
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("invalidclient"),
				dataSet.get("secret"), reward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Gift Loyalty Reward");
		boolean isGiftRewardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, giftRewardResponse.asString());
		Assert.assertTrue(isGiftRewardInvalidClientSchemaValidated,
				"API v2 Gift Loyalty Reward Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift loyalty reward is unsuccessful because of invalid client");

		// gifting loyalty reward to other user with invalid secret
		Response giftRewardResponse1 = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("invalidsecret"), reward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Gift Loyalty Reward");
		boolean isGiftRewardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, giftRewardResponse1.asString());
		Assert.assertTrue(isGiftRewardInvalidSecretSchemaValidated,
				"API v2 Gift Loyalty Reward Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift loyalty reward is unsuccessful because of invalid secret");

		// gifting loyalty reward to other user with invalid token
		Response giftRewardResponse2 = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, newUserEmail, invalidtoken);
		Assert.assertEquals(giftRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Gift Loyalty Reward");
		boolean isGiftRewardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, giftRewardResponse2.asString());
		Assert.assertTrue(isGiftRewardInvalidTokenSchemaValidated,
				"API v2 Gift Loyalty Reward Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift loyalty reward is unsuccessful because of invalid token");

		// gifting loyalty reward to other user with invalid mail
		Response giftRewardResponse3 = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, invalidUserEmail, token);
		Assert.assertEquals(giftRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Gift Loyalty Reward");
		boolean isGiftRewardInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema, giftRewardResponse3.asString());
		Assert.assertTrue(isGiftRewardInvalidEmailSchemaValidated,
				"API v2 Gift Loyalty Reward Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift loyalty reward is unsuccessful because of invalid mail");

		// gifting loyalty reward to other user with invalid reward id
		Response giftRewardResponse4 = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), invalidreward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Gift Loyalty Reward");
		boolean isGiftRewardInvalidRewardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, giftRewardResponse4.asString());
		Assert.assertTrue(isGiftRewardInvalidRewardSchemaValidated,
				"API v2 Gift Loyalty Reward Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift loyalty reward is unsuccessful because of invalid reward id");

	}

	@Test(description = "SQ-T3091 Verify gifting loyalty amount user to user negative flow", groups = "api", priority = 9)
	public void verify_Gifting_Loyalty_Amount_User_to_User_Negative() throws InterruptedException {
		String invalidtoken = "1";
		String invalidUserEmail = "a";
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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Gift loyalty reward to other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");

		// gift banked currency with invalid client
		Response giftRewardResponse = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("invalidclient"),
				dataSet.get("secret"), newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Gift Banked Currency");
		boolean isGiftCurrencyInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, giftRewardResponse.asString());
		Assert.assertTrue(isGiftCurrencyInvalidClientSchemaValidated,
				"API v2 Gift Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift banked currency is unsuccessful because of invalid client");

		// gift banked currency with invalid secret
		Response giftRewardResponse1 = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("invalidsecret"), newUserEmail, token);
		Assert.assertEquals(giftRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for Gift Banked Currency");
		boolean isGiftCurrencyInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, giftRewardResponse1.asString());
		Assert.assertTrue(isGiftCurrencyInvalidSecretSchemaValidated,
				"API v2 Gift Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift banked currency is unsuccessful because of invalid secret");

		// gift banked currency with invalid token
		Response giftRewardResponse2 = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("secret"), newUserEmail, invalidtoken);
		Assert.assertEquals(giftRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Gift Banked Currency");
		boolean isGiftCurrencyInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, giftRewardResponse2.asString());
		Assert.assertTrue(isGiftCurrencyInvalidTokenSchemaValidated,
				"API v2 Gift Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift banked currency is unsuccessful because of invalid token");

		// gift banked currency with invalid mail
		Response giftRewardResponse3 = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("secret"), invalidUserEmail, token);
		Assert.assertEquals(giftRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Gift Banked Currency");
		boolean isGiftCurrencyInvalidEmailSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema, giftRewardResponse3.asString());
		Assert.assertTrue(isGiftCurrencyInvalidEmailSchemaValidated,
				"API v2 Gift Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 gift banked currency is unsuccessful because of invalid mail");
	}

	@Test(description = "SQ-T3093 Verify redemption using redeemable negative flow", groups = "api", priority = 10)
	public void verify_Redemption_Using_Redeemable_Negative() {
		String invalidtoken = "1";
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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Create Redemption using "redeemable" (fetch redemption code) with invalid
		// client
		Response redeemableResponse = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using redeemable");
		boolean isCreateRedemptionInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, redeemableResponse.asString());
		Assert.assertTrue(isCreateRedemptionInvalidClientSchemaValidated,
				"API v2 Create Redemption using Redeemable Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using redeemable is successful");

		// Create Redemption using "redeemable" (fetch redemption code) with invalid
		// secret
		Response redeemableResponse1 = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 create redemption using redeemable");
		boolean isCreateRedemptionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, redeemableResponse1.asString());
		Assert.assertTrue(isCreateRedemptionInvalidSecretSchemaValidated,
				"API v2 Create Redemption using Redeemable Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using redeemable is unsuccessful");

		// Create Redemption using "redeemable" (fetch redemption code) with invalid
		// token
		Response redeemableResponse2 = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using redeemable");
		boolean isCreateRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, redeemableResponse2.asString());
		Assert.assertTrue(isCreateRedemptionInvalidTokenSchemaValidated,
				"API v2 Create Redemption using Redeemable Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using redeemable is unsuccessful");
	}

	@Test(description = "SQ-T3094 Verify redemption using visits negative flow", groups = "api", priority = 11)
	public void verify_Redemption_Using_Visits_Negative() {
		String invalidtoken = "1";
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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Create Redemption using "visits" (fetch redemption code) with invalid client
		Response visitsResponse = pageObj.endpoints().Api2RedemptionWithVisit(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(visitsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using visits");
		boolean isCreateRedemptionInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, visitsResponse.asString());
		Assert.assertTrue(isCreateRedemptionInvalidClientSchemaValidated,
				"API v2 Create Redemption using Visits Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using visits is unsuccessful");

		// Create Redemption using "visits" (fetch redemption code) 2ith invalid secret
		Response visitsResponse1 = pageObj.endpoints().Api2RedemptionWithVisit(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(visitsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 create redemption using visits");
		boolean isCreateRedemptionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, visitsResponse1.asString());
		Assert.assertTrue(isCreateRedemptionInvalidSecretSchemaValidated,
				"API v2 Create Redemption using Visits Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using visits is unsuccessful");

		// Create Redemption using "visits" (fetch redemption code) with invalid token
		Response visitsResponse2 = pageObj.endpoints().Api2RedemptionWithVisit(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(visitsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using visits");
		boolean isCreateRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, visitsResponse2.asString());
		Assert.assertTrue(isCreateRedemptionInvalidTokenSchemaValidated,
				"API v2 Create Redemption using Visits Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using visits is unsuccessful");
	}

	@Test(description = "SQ-T2377 Verify redemption using banked currency negative flow", groups = "api", priority = 12)
	public void verify_Redemption_Using_Banked_Currency_Negative() {
		String invalidtoken = "1";
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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Create Redemption using banked currency (fetch redemption code) with invalid
		// client
		Response visitsResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrency(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(visitsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using banked currency");
		boolean isCreateRedemptionInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, visitsResponse.asString());
		Assert.assertTrue(isCreateRedemptionInvalidClientSchemaValidated,
				"API v2 Create Redemption using Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using banked currency is unsuccessful");

		// Create Redemption using banked currency (fetch redemption code) with invalid
		// secret
		Response visitsResponse1 = pageObj.endpoints().Api2RedemptionWithBankedCurrency(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(visitsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 create redemption using banked currency");
		boolean isCreateRedemptionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, visitsResponse1.asString());
		Assert.assertTrue(isCreateRedemptionInvalidSecretSchemaValidated,
				"API v2 Create Redemption using Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using banked currency is unsuccessful");

		// Create Redemption using banked currency (fetch redemption code) with invalid
		// token
		Response visitsResponse2 = pageObj.endpoints().Api2RedemptionWithBankedCurrency(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(visitsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using banked currency");
		boolean isCreateRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, visitsResponse2.asString());
		Assert.assertTrue(isCreateRedemptionInvalidTokenSchemaValidated,
				"API v2 Create Redemption using Banked Currency Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using banked currency is unsuccessful");
	}

	@Test(description = "SQ-T3080 Verify redemption using reward_id negative flow", groups = "api", priority = 13)
	public void verify_Redemption_Using_Reward_id_Negative() {
		String invalidreward_id = "1";
		String invalidtoken = "1";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Create Redemption using "reward_id" (fetch redemption code) with invalid
		// client
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using reward_id");
		boolean isCreateRedemptionInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, redemptionResponse.asString());
		Assert.assertTrue(isCreateRedemptionInvalidClientSchemaValidated,
				"API v2 Create Redemption using Reward_id Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using reward_id is unsuccessful");

		// Create Redemption using "reward_id" (fetch redemption code) with invalid
		// secret
		Response redemptionResponse1 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, reward_id);
		Assert.assertEquals(redemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 create redemption using reward_id");
		boolean isCreateRedemptionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, redemptionResponse1.asString());
		Assert.assertTrue(isCreateRedemptionInvalidSecretSchemaValidated,
				"API v2 Create Redemption using Reward_id Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using reward_id is unsuccessful");

		// Create Redemption using "reward_id" (fetch redemption code) with invalid
		// token
		Response redemptionResponse2 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, reward_id);
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create redemption using reward_id");
		boolean isCreateRedemptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, redemptionResponse2.asString());
		Assert.assertTrue(isCreateRedemptionInvalidTokenSchemaValidated,
				"API v2 Create Redemption using Reward_id Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using reward_id is unsuccessful");

		// Create Redemption using "reward_id" (fetch redemption code) with invalid
		// reward id
		Response redemptionResponse3 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, invalidreward_id);
		Assert.assertEquals(redemptionResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create redemption using reward_id");
		boolean isCreateRedemptionInvalidRewardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionResponse3.asString());
		Assert.assertTrue(isCreateRedemptionInvalidRewardSchemaValidated,
				"API v2 Create Redemption using Reward_id Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Redemption using reward_id is unsuccessful");
	}

	@Test(description = "SQ-T2383 verify_Create_Checkins", groups = "api", priority = 14)
	public void verify_Create_Checkins_Negative() throws InterruptedException {
		String invalidtoken = "1";
		String invalidbarcode = "1";
		String invalidqrCode = "1";
		String invalidcheckin_id = "1@2";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

//		// Create Loyalty Checkin by Barcode
//		try {
//
//			driver = new BrowserUtilities().launchBrowser();
//			pageObj = new PageObj(driver);
//
//			pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("instanceUrl"));
//			pageObj.instanceDashboardPage().loginToInstance();
//			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//			// generateBarcode
//			pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
//			pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
//			String barcode = pageObj.instanceDashboardPage().captureBarcode();
//			logger.info(barcode);
//			driver.quit();
//			
//			//Loyalty Checkin by Barcode with invalid client
//			Response barcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("invalidclient"),
//					dataSet.get("secret"), token, barcode);
//			Assert.assertEquals(barcodeCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
//					"Status code 401 did not matched for api2 Loyalty Checkin by bar code");
//			
//			//Loyalty Checkin by Barcode with invalid secret
//			Response barcodeCheckinResponse1 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
//					dataSet.get("invalidsecret"), token, barcode);
//			Assert.assertEquals(barcodeCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
//					"Status code 412 did not matched for api2 Loyalty Checkin by bar code");
//			
//			//Loyalty Checkin by Barcode with invalid token
//			Response barcodeCheckinResponse2 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
//					dataSet.get("secret"), invalidtoken, barcode);
//			Assert.assertEquals(barcodeCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
//					"Status code 401 did not matched for api2 Loyalty Checkin by bar code");
//			
//			//Loyalty Checkin by Barcode with invalid barcode
//			Response barcodeCheckinResponse3 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
//					dataSet.get("secret"), token, invalidbarcode);
//			Assert.assertEquals(barcodeCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
//					"Status code 422 did not matched for api2 Loyalty Checkin by bar code");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// Create Loyalty Checkin by QR Code

		/*
		 * try {
		 * 
		 * driver = new BrowserUtilities().launchBrowser(); pageObj = new
		 * PageObj(driver);
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get(
		 * "instanceUrl")); pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * generateBarcode pageObj.menupage().navigateToSubMenuItem("Support",
		 * "Test Barcodes");
		 * pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		 * String qrCode = pageObj.instanceDashboardPage().captureBarcode();
		 * logger.info(qrCode);
		 * 
		 * //Loyalty Checkin by QR Code with invalid client Response
		 * qrcodeCheckinResponse =
		 * pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("invalidclient"),
		 * dataSet.get("secret"), token, qrCode);
		 * Assert.assertEquals(qrcodeCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
		 * "Status code 401 did not matched for api2 Loyalty Checkin by qr code");
		 * 
		 * driver = new BrowserUtilities().launchBrowser(); pageObj = new
		 * PageObj(driver);
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get(
		 * "instanceUrl")); pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * generateBarcode pageObj.menupage().navigateToSubMenuItem("Support",
		 * "Test Barcodes");
		 * pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		 * String qrCode1 = pageObj.instanceDashboardPage().captureBarcode();
		 * logger.info(qrCode1);
		 * 
		 * //Loyalty Checkin by QR Code with invalid secret Response
		 * qrcodeCheckinResponse1 =
		 * pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("client"),
		 * dataSet.get("invalidsecret"), token, qrCode1);
		 * Assert.assertEquals(qrcodeCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
		 * "Status code 412 did not matched for api2 Loyalty Checkin by qr code");
		 * 
		 * driver = new BrowserUtilities().launchBrowser(); pageObj = new
		 * PageObj(driver);
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get(
		 * "instanceUrl")); pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * generateBarcode pageObj.menupage().navigateToSubMenuItem("Support",
		 * "Test Barcodes");
		 * pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		 * String qrCode2 = pageObj.instanceDashboardPage().captureBarcode();
		 * logger.info(qrCode2); //Loyalty Checkin by QR Code with invalid token
		 * Response qrcodeCheckinResponse2 =
		 * pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("client"),
		 * dataSet.get("secret"), invalidtoken, qrCode2);
		 * Assert.assertEquals(qrcodeCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
		 * "Status code 401 did not matched for api2 Loyalty Checkin by qr code");
		 * 
		 * //Loyalty Checkin by QR Code with invalid qrcode Response
		 * qrcodeCheckinResponse3 =
		 * pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("client"),
		 * dataSet.get("secret"), token, invalidqrCode);
		 * Assert.assertEquals(qrcodeCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
		 * "Status code 422 did not matched for api2 Loyalty Checkin by qr code"); }
		 * catch (Exception e) { e.printStackTrace(); }
		 */

		// Create Loyalty Checkin by Receipt Image with invalid client
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(
				dataSet.get("invalidclient"), dataSet.get("secret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Loyalty Checkin by Receipt Image");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful");

		// Create Loyalty Checkin by Receipt Image with invalid secret
		Response receiptCheckinResponse1 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Loyalty Checkin by Receipt Image");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful");

		// Create Loyalty Checkin by Receipt Image with invalid token
		Response receiptCheckinResponse2 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Loyalty Checkin by Receipt Image");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful");

		// Create Loyalty Checkin by Receipt Image with invalid location id
		Response receiptCheckinResponse3 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("invalidlocationid"));
		Assert.assertEquals(receiptCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Loyalty Checkin by Receipt Image");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful");

		// Fetch Checkins
		/*
		 * Response fetchCheckinResponse =
		 * pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
		 * dataSet.get("secret"), token);
		 * pageObj.apiUtils().verifyResponse(fetchCheckinResponse,
		 * "Fetch checkin response");
		 * Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		 * "Status code 200 did not matched for api2 fetch checkin"); String checkin_id
		 * = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();
		 */

		// Fetch Checkins with invalid client
		Response fetchCheckinResponse1 = pageObj.endpoints().Api2FetchCheckin(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch checkin");
		boolean isFetchCheckinInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchCheckinResponse1.asString());
		Assert.assertTrue(isFetchCheckinInvalidClientSchemaValidated, "API v2 Fetch Checkins Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Checkins is unsuccessful");

		// Fetch Checkins with invalid secret
		Response fetchCheckinResponse2 = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch checkin");
		boolean isFetchCheckinInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchCheckinResponse2.asString());
		Assert.assertTrue(isFetchCheckinInvalidSecretSchemaValidated, "API v2 Fetch Checkins Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Checkins is unsuccessful");

		// Fetch Checkins with invalid token
		Response fetchCheckinResponse3 = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch checkin");
		boolean isFetchCheckinInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchCheckinResponse3.asString());
		Assert.assertTrue(isFetchCheckinInvalidTokenSchemaValidated, "API v2 Fetch Checkins Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Checkins is unsuccessful");

		// Account Balance with invalid client
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 account balance");
		boolean isAccountBalanceInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, accountBalResponse.asString());
		Assert.assertTrue(isAccountBalanceInvalidClientSchemaValidated,
				"API v2 Account Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Account Balance is unsuccessful");

		// Account Balance with invalid secret
		Response accountBalResponse1 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(accountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 account balance");
		boolean isAccountBalanceInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, accountBalResponse1.asString());
		Assert.assertTrue(isAccountBalanceInvalidSecretSchemaValidated,
				"API v2 Account Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Account Balance is unsuccessful");

		// Account Balance with invalid token
		Response accountBalResponse2 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(accountBalResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 account balance");
		boolean isAccountBalanceInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, accountBalResponse2.asString());
		Assert.assertTrue(isAccountBalanceInvalidTokenSchemaValidated,
				"API v2 Account Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Account Balance is unsuccessful");

		// Transaction Details with invalid client
		String checkin_id = "123456";
		Response txnDetailsResponse = pageObj.endpoints().Api2Trasactiondetails(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, checkin_id);
		Assert.assertEquals(txnDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 transaction details");
		boolean isTransactionDetailsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, txnDetailsResponse.asString());
		Assert.assertTrue(isTransactionDetailsInvalidClientSchemaValidated,
				"API v2 Transaction Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transaction Details is unsuccessful");

		// Transaction Details with invalid secret
		Response txnDetailsResponse1 = pageObj.endpoints().Api2Trasactiondetails(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, checkin_id);
		Assert.assertEquals(txnDetailsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 transaction details");
		boolean isTransactionDetailsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, txnDetailsResponse1.asString());
		Assert.assertTrue(isTransactionDetailsInvalidSecretSchemaValidated,
				"API v2 Transaction Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transaction Details is unsuccessful");

		// Transaction Details with invalid token
		Response txnDetailsResponse2 = pageObj.endpoints().Api2Trasactiondetails(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, checkin_id);
		Assert.assertEquals(txnDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 transaction details");
		boolean isTransactionDetailsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, txnDetailsResponse2.asString());
		Assert.assertTrue(isTransactionDetailsInvalidTokenSchemaValidated,
				"API v2 Transaction Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transaction Details is unsuccessful");

		// Transaction Details with invalid checkin id
		/*
		 * Response txnDetailsResponse3 =
		 * pageObj.endpoints().Api2Trasactiondetails(dataSet.get("client"),
		 * dataSet.get("secret"), token, invalidcheckin_id);
		 * Assert.assertEquals(txnDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
		 * "Status code 200 did not matched for api2 transaction balance");
		 * TestListeners.extentTest.get().
		 * pass("Api2 Transaction Details is unsuccessful");
		 */
	}

	@Test(description = "SQ-T3096 Verify Api2 giftcard api negative flow", groups = "api", priority = 15)
	public void Api2GiftCardApiNegative() throws InterruptedException {
		String invalidtoken = "1";
		String invaliduuid = "1";
		String invalidreciverUserEmail = "abc";
		String invalidcheckin_id = "1";
		String invaliduserEmail = "abc";
		String uuid = "";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + attempts);
				utils.longWait(5000);
				// Purchase Gift Card API2
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("design_id"));
				uuid = purchaseGiftCardResponse.jsonPath().getString("uuid").toString();
				int statusCode = purchaseGiftCardResponse.getStatusCode();
				if (statusCode == 200) {
					flag = true;
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not matched for api2 purchase gift card");
					TestListeners.extentTest.get().pass("Api2 Purchase Gift Card with valid details is successful ");
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}

		// Purchase Gift Card API2 with invalid client
		Response purchaseGiftCardResponse1 = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, dataSet.get("design_id"));
		Assert.assertEquals(purchaseGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 purchase gift card");
		boolean isPurchaseGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, purchaseGiftCardResponse1.asString());
		Assert.assertTrue(isPurchaseGiftCardInvalidClientSchemaValidated,
				"API v2 Purchase Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Purchase Gift Card with invalid client is unsuccessful ");

		// Purchase Gift Card API2 with invalid secret
		Response purchaseGiftCardResponse2 = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("design_id"));
		Assert.assertEquals(purchaseGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 purchase gift card");
		boolean isPurchaseGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, purchaseGiftCardResponse2.asString());
		Assert.assertTrue(isPurchaseGiftCardInvalidSecretSchemaValidated,
				"API v2 Purchase Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Purchase Gift Card with invalid secret is unsuccessful ");

		// Purchase Gift Card API2 with invalid token
		Response purchaseGiftCardResponse3 = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, dataSet.get("design_id"));
		Assert.assertEquals(purchaseGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 purchase gift card");
		boolean isPurchaseGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, purchaseGiftCardResponse3.asString());
		Assert.assertTrue(isPurchaseGiftCardInvalidTokenSchemaValidated,
				"API v2 Purchase Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Purchase Gift Card with invalid token is unsuccessful ");

		// Purchase Gift Card API2 with invalid design id
		Response purchaseGiftCardResponse4 = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("invaliddesign"));
		Assert.assertEquals(purchaseGiftCardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not matched for api2 purchase gift card");
		boolean isPurchaseGiftCardInvalidDesignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, purchaseGiftCardResponse4.asString());
		Assert.assertTrue(isPurchaseGiftCardInvalidDesignSchemaValidated,
				"API v2 Purchase Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Purchase Gift Card with invalid design id is unsuccessful ");

		// Update Gift Cards with invalid client
		Response updateGiftCardResponse = pageObj.endpoints().Api2UpdateGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(updateGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 update gift card");
		boolean isUpdateGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, updateGiftCardResponse.asString());
		Assert.assertTrue(isUpdateGiftCardInvalidClientSchemaValidated,
				"API v2 Update Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid client is unsuccessful ");

		// Update Gift Cards with invalid secret
		Response updateGiftCardResponse1 = pageObj.endpoints().Api2UpdateGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(updateGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 update gift card");
		boolean isUpdateGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, updateGiftCardResponse1.asString());
		Assert.assertTrue(isUpdateGiftCardInvalidSecretSchemaValidated,
				"API v2 Update Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid secret is unsuccessful ");

		// Update Gift Cards with invalid token
		Response updateGiftCardResponse2 = pageObj.endpoints().Api2UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid);
		Assert.assertEquals(updateGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 update gift card");
		boolean isUpdateGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, updateGiftCardResponse2.asString());
		Assert.assertTrue(isUpdateGiftCardInvalidTokenSchemaValidated,
				"API v2 Update Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid token is unsuccessful ");

		// Update Gift Cards with invalid uuid
		Response updateGiftCardResponse3 = pageObj.endpoints().Api2UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid);
		Assert.assertEquals(updateGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 update gift card");
		boolean isUpdateGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, updateGiftCardResponse3.asString());
		Assert.assertTrue(isUpdateGiftCardInvalidUuidSchemaValidated,
				"API v2 Update Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid uuid is unsuccessful ");

		// Reload Gift Card API2 with invalid client
		Response reloadGiftCardResponse = pageObj.endpoints().Api2ReloadGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(reloadGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 reload gift card");
		boolean isReloadGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, reloadGiftCardResponse.asString());
		Assert.assertTrue(isReloadGiftCardInvalidClientSchemaValidated,
				"API v2 Reload Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid client is unsuccessful ");

		// Reload Gift Card API2 with invalid secret
		Response reloadGiftCardResponse1 = pageObj.endpoints().Api2ReloadGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(reloadGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 reload gift card");
		boolean isReloadGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, reloadGiftCardResponse1.asString());
		Assert.assertTrue(isReloadGiftCardInvalidSecretSchemaValidated,
				"API v2 Reload Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid secret is unsuccessful ");

		// Reload Gift Card API2 with invalid token
		Response reloadGiftCardResponse2 = pageObj.endpoints().Api2ReloadGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid);
		Assert.assertEquals(reloadGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 reload gift card");
		boolean isReloadGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, reloadGiftCardResponse2.asString());
		Assert.assertTrue(isReloadGiftCardInvalidTokenSchemaValidated,
				"API v2 Reload Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid token is unsuccessful ");

		// Reload Gift Card API2 with invalid uuid
		Response reloadGiftCardResponse3 = pageObj.endpoints().Api2ReloadGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid);
		Assert.assertEquals(reloadGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not matched for api2 reload gift card");
		boolean isReloadGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, reloadGiftCardResponse3.asString());
		Assert.assertTrue(isReloadGiftCardInvalidUuidSchemaValidated,
				"API v2 Reload Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card with invalid uuid is unsuccessful ");

		// Fetch Gift Card with invalid client
		Response fetchGiftCardResponse = pageObj.endpoints().Api2FetchGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card");
		boolean isFetchGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchGiftCardResponse.asString());
		Assert.assertTrue(isFetchGiftCardInvalidClientSchemaValidated,
				"API v2 Fetch Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card with invalid client is unsuccessful ");

		// Fetch Gift Card with invalid secret
		Response fetchGiftCardResponse1 = pageObj.endpoints().Api2FetchGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch gift card");
		boolean isFetchGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchGiftCardResponse1.asString());
		Assert.assertTrue(isFetchGiftCardInvalidSecretSchemaValidated,
				"API v2 Fetch Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card with invalid secret is unsuccessful ");

		// Fetch Gift Card with invalid token
		Response fetchGiftCardResponse2 = pageObj.endpoints().Api2FetchGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card");
		boolean isFetchGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchGiftCardResponse2.asString());
		Assert.assertTrue(isFetchGiftCardInvalidTokenSchemaValidated,
				"API v2 Fetch Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card with invalid token is unsuccessful ");

		// Fetch Gift Card Balance with invalid client
		Response fetchGiftCardBalanceResponse = pageObj.endpoints()
				.Api2FetchGiftCardBalance(dataSet.get("invalidclient"), dataSet.get("secret"), token, uuid);
		Assert.assertEquals(fetchGiftCardBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card balance");
		boolean isFetchGiftCardBalanceInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchGiftCardBalanceResponse.asString());
		Assert.assertTrue(isFetchGiftCardBalanceInvalidClientSchemaValidated,
				"API v2 Fetch Gift Card Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful ");

		// Fetch Gift Card Balance with invalid secret
		Response fetchGiftCardBalanceResponse1 = pageObj.endpoints().Api2FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(fetchGiftCardBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch gift card balance");
		boolean isFetchGiftCardBalanceInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchGiftCardBalanceResponse1.asString());
		Assert.assertTrue(isFetchGiftCardBalanceInvalidSecretSchemaValidated,
				"API v2 Fetch Gift Card Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card Balance with invalid client is unsuccessful ");

		// Fetch Gift Card Balance with invalid token
		Response fetchGiftCardBalanceResponse2 = pageObj.endpoints().Api2FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid);
		Assert.assertEquals(fetchGiftCardBalanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card balance");
		boolean isFetchGiftCardBalanceInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchGiftCardBalanceResponse2.asString());
		Assert.assertTrue(isFetchGiftCardBalanceInvalidTokenSchemaValidated,
				"API v2 Fetch Gift Card Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card Balance with invalid token is unsuccessful ");

		// Fetch Gift Card Balance with invalid uuid
		Response fetchGiftCardBalanceResponse3 = pageObj.endpoints().Api2FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid);
		Assert.assertEquals(fetchGiftCardBalanceResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 fetch gift card balance");
		boolean isFetchGiftCardBalanceInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, fetchGiftCardBalanceResponse3.asString());
		Assert.assertTrue(isFetchGiftCardBalanceInvalidUuidSchemaValidated,
				"API v2 Fetch Gift Card Balance Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card Balance with invalid uuid is unsuccessful ");

		// Fetch Gift Card Transaction History with invalid client
		Response giftCardtransactionResponse = pageObj.endpoints()
				.Api2GiftCardTransactionHistory(dataSet.get("invalidclient"), dataSet.get("secret"), token, uuid);
		Assert.assertEquals(giftCardtransactionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card balance");
		boolean isGiftCardTransactionHistoryInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, giftCardtransactionResponse.asString());
		Assert.assertTrue(isGiftCardTransactionHistoryInvalidClientSchemaValidated,
				"API v2 Fetch Gift Card Transaction History Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Api2 Fetch Gift Card Transaction History with invalid client is unsuccessful ");

		// Fetch Gift Card Transaction History with invalid secret
		Response giftCardtransactionResponse1 = pageObj.endpoints()
				.Api2GiftCardTransactionHistory(dataSet.get("client"), dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(giftCardtransactionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch gift card balance");
		boolean isGiftCardTransactionHistoryInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, giftCardtransactionResponse1.asString());
		Assert.assertTrue(isGiftCardTransactionHistoryInvalidSecretSchemaValidated,
				"API v2 Fetch Gift Card Transaction History Schema Validation failed");
		TestListeners.extentTest.get()
				.pass("Api2 Fetch Gift Card Transaction History with invalid secret is unsuccessful ");

		// Fetch Gift Card Transaction History with invalid token
		Response giftCardtransactionResponse2 = pageObj.endpoints()
				.Api2GiftCardTransactionHistory(dataSet.get("client"), dataSet.get("secret"), invalidtoken, uuid);
		Assert.assertEquals(giftCardtransactionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch gift card balance");
		boolean isGiftCardTransactionHistoryInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, giftCardtransactionResponse2.asString());
		Assert.assertTrue(isGiftCardTransactionHistoryInvalidTokenSchemaValidated,
				"API v2 Fetch Gift Card Transaction History Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card Transaction History is unsuccessful ");

		// Fetch Gift Card Transaction History with invalid uuid
		Response giftCardtransactionResponse3 = pageObj.endpoints()
				.Api2GiftCardTransactionHistory(dataSet.get("client"), dataSet.get("secret"), token, invaliduuid);
		Assert.assertEquals(giftCardtransactionResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 fetch gift card balance");
		boolean isGiftCardTransactionHistoryInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, giftCardtransactionResponse3.asString());
		Assert.assertTrue(isGiftCardTransactionHistoryInvalidUuidSchemaValidated,
				"API v2 Fetch Gift Card Transaction History Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch Gift Card Transaction History is unsuccessful ");

		// User register/signup using API2 Signup
		String reciverUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpReciverResponse = pageObj.endpoints().Api2SignUp(reciverUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpReciverResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Transfer GiftCard Balance with invalid client
		Response transferGiftCardResponse = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, transferGiftCardResponse.asString());
		Assert.assertTrue(isTransferGiftCardInvalidClientSchemaValidated,
				"API v2 Transfer Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer Gift Card is unsuccessful ");

		// Transfer GiftCard Balance with invalid secret
		Response transferGiftCardResponse1 = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, transferGiftCardResponse1.asString());
		Assert.assertTrue(isTransferGiftCardInvalidSecretSchemaValidated,
				"API v2 Transfer Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer Gift Card is unsuccessful ");

		// Transfer GiftCard Balance with invalid token
		Response transferGiftCardResponse2 = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid, reciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, transferGiftCardResponse2.asString());
		Assert.assertTrue(isTransferGiftCardInvalidTokenSchemaValidated,
				"API v2 Transfer Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer Gift Card is unsuccessful ");

		// Transfer GiftCard Balance with invalid uuid
		Response transferGiftCardResponse3 = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid, reciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, transferGiftCardResponse3.asString());
		Assert.assertTrue(isTransferGiftCardInvalidUuidSchemaValidated,
				"API v2 Transfer Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer Gift Card is unsuccessful ");

		// Transfer GiftCard Balance with invalid reciever user mail
		Response transferGiftCardResponse4 = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, invalidreciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardInvalidReciverUserEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, transferGiftCardResponse4.asString());
		Assert.assertTrue(isTransferGiftCardInvalidReciverUserEmailSchemaValidated,
				"API v2 Transfer Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer Gift Card is unsuccessful ");

		// Share Gift Card with invalid client
		Response shareGiftCardResponse = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 share gift card ");
		boolean isShareGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, shareGiftCardResponse.asString());
		Assert.assertTrue(isShareGiftCardInvalidClientSchemaValidated,
				"API v2 Share Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Share Gift Card is unsuccessful ");

		// Share Gift Card with invalid secret
		Response shareGiftCardResponse1 = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 share gift card ");
		boolean isShareGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, shareGiftCardResponse1.asString());
		Assert.assertTrue(isShareGiftCardInvalidSecretSchemaValidated,
				"API v2 Share Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Share Gift Card is unsuccessful ");

		// Share Gift Card with invalid token
		Response shareGiftCardResponse2 = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid, reciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 share gift card ");
		boolean isShareGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, shareGiftCardResponse2.asString());
		Assert.assertTrue(isShareGiftCardInvalidTokenSchemaValidated,
				"API v2 Share Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Share Gift Card is unsuccessful ");

		// Share Gift Card with invalid uuid
		Response shareGiftCardResponse3 = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid, reciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not matched for api2 share gift card ");
		boolean isShareGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, shareGiftCardResponse3.asString());
		Assert.assertTrue(isShareGiftCardInvalidUuidSchemaValidated, "API v2 Share Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Share Gift Card is unsuccessful ");

		// Share Gift Card with invalid mail
		Response shareGiftCardResponse4 = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, invalidreciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 share gift card ");
		boolean isShareGiftCardInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, shareGiftCardResponse4.asString());
		Assert.assertTrue(isShareGiftCardInvalidEmailSchemaValidated,
				"API v2 Share Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Share Gift Card is unsuccessful ");

		// Create Loyalty Checkin by Receipt Image
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Loyalty Checkin by Receipt Image");
		String checkin_id = receiptCheckinResponse.jsonPath().getString("checkin_id");

		// Create Loyalty Checkin by Receipt Image with invalid client
		Response receiptCheckinResponse1 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(
				dataSet.get("invalidclient"), dataSet.get("secret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Loyalty Checkin by Receipt Image");
		boolean isReceiptCheckinInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, receiptCheckinResponse1.asString());
		Assert.assertTrue(isReceiptCheckinInvalidClientSchemaValidated,
				"API v2 Create Loyalty Checkin by Receipt Image Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful ");

		// Create Loyalty Checkin by Receipt Image with invalid secret
		Response receiptCheckinResponse2 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Loyalty Checkin by Receipt Image");
		boolean isReceiptCheckinInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, receiptCheckinResponse2.asString());
		Assert.assertTrue(isReceiptCheckinInvalidSecretSchemaValidated,
				"API v2 Create Loyalty Checkin by Receipt Image Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful ");

		// Create Loyalty Checkin by Receipt Image with invalid token
		Response receiptCheckinResponse3 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Loyalty Checkin by Receipt Image");
		boolean isReceiptCheckinInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, receiptCheckinResponse3.asString());
		Assert.assertTrue(isReceiptCheckinInvalidTokenSchemaValidated,
				"API v2 Create Loyalty Checkin by Receipt Image Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful ");

		// Create Loyalty Checkin by Receipt Image with invalid location id
		Response receiptCheckinResponse4 = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("invalidlocationid"));
		Assert.assertEquals(receiptCheckinResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Loyalty Checkin by Receipt Image");
		boolean isReceiptCheckinInvalidLocationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, receiptCheckinResponse4.asString());
		Assert.assertTrue(isReceiptCheckinInvalidLocationSchemaValidated,
				"API v2 Create Loyalty Checkin by Receipt Image Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Loyalty Checkin by Receipt Image is unsuccessful ");

		// Tip via giftcard with invalid client
		Response tipGiftCardResponse = pageObj.endpoints().Api2TipGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid, checkin_id);
		Assert.assertEquals(tipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, tipGiftCardResponse.asString());
		Assert.assertTrue(isTipGiftCardInvalidClientSchemaValidated,
				"API v2 Tip via Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Tip Via Gift Card is unsuccessful ");

		// Tip via giftcard with invalid secret
		Response tipGiftCardResponse1 = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid, checkin_id);
		Assert.assertEquals(tipGiftCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, tipGiftCardResponse1.asString());
		Assert.assertTrue(isTipGiftCardInvalidSecretSchemaValidated,
				"API v2 Tip via Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Tip Via Gift Card is unsuccessful ");

		// Tip via giftcard with invalid token
		Response tipGiftCardResponse2 = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid, checkin_id);
		Assert.assertEquals(tipGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, tipGiftCardResponse2.asString());
		Assert.assertTrue(isTipGiftCardInvalidTokenSchemaValidated,
				"API v2 Tip via Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Tip Via Gift Card is unsuccessful ");

		// Tip via giftcard with invalid uuid
		Response tipGiftCardResponse3 = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid, checkin_id);
		Assert.assertEquals(tipGiftCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GiftCardNotFoundSchema, tipGiftCardResponse3.asString());
		Assert.assertTrue(isTipGiftCardInvalidUuidSchemaValidated, "API v2 Tip via Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Tip Via Gift Card is unsuccessful ");

		// Tip via giftcard with invalid checkin id
		Response tipGiftCardResponse4 = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, invalidcheckin_id);
		Assert.assertEquals(tipGiftCardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardInvalidCheckinIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CheckinNotFoundSchema, tipGiftCardResponse4.asString());
		Assert.assertTrue(isTipGiftCardInvalidCheckinIdSchemaValidated,
				"API v2 Tip via Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Tip Via Gift Card is unsuccessful ");

		// Delete a card with invalid client
		Response deleteGiftCard = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(deleteGiftCard.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 delete a gift card");
		boolean isDeleteGiftCardInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, deleteGiftCard.asString());
		Assert.assertTrue(isDeleteGiftCardInvalidClientSchemaValidated,
				"API v2 Delete Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 delete Gift Card is unsuccessful ");

		// Delete a card with invalid secret
		Response deleteGiftCard1 = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(deleteGiftCard1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 delete a gift card");
		boolean isDeleteGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, deleteGiftCard1.asString());
		Assert.assertTrue(isDeleteGiftCardInvalidSecretSchemaValidated,
				"API v2 Delete Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 delete Gift Card is unsuccessful ");

		// Delete a card with invalid token
		Response deleteGiftCard2 = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				invalidtoken, uuid);
		Assert.assertEquals(deleteGiftCard2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 delete a gift card");
		boolean isDeleteGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, deleteGiftCard2.asString());
		Assert.assertTrue(isDeleteGiftCardInvalidTokenSchemaValidated,
				"API v2 Delete Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 delete Gift Card is unsuccessful ");

		// Delete a card with invalid uuid
		Response deleteGiftCard3 = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				token, invaliduuid);
		Assert.assertEquals(deleteGiftCard3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 delete a gift card");
		boolean isDeleteGiftCardInvalidUuidSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema2, deleteGiftCard3.asString());
		Assert.assertTrue(isDeleteGiftCardInvalidUuidSchemaValidated,
				"API v2 Delete Gift Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 delete Gift Card is unsuccessful ");

		// Gift a card with invalid client
		Response giftaCardResponse = pageObj.endpoints().Api2GiftaCard(userEmail, dataSet.get("invalidclient"),
				dataSet.get("secret"), token, dataSet.get("design_id"));
		Assert.assertEquals(giftaCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2  gift a card");
		boolean isGiftaCardInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, giftaCardResponse.asString());
		Assert.assertTrue(isGiftaCardInvalidClientSchemaValidated, "API v2 Gift a Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Gift a Card is unsuccessful ");

		// Gift a card with invalid secret
		Response giftaCardResponse1 = pageObj.endpoints().Api2GiftaCard(userEmail, dataSet.get("client"),
				dataSet.get("invalidsecret"), token, dataSet.get("design_id"));
		Assert.assertEquals(giftaCardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2  gift a card");
		boolean isGiftaCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, giftaCardResponse1.asString());
		Assert.assertTrue(isGiftaCardInvalidSecretSchemaValidated, "API v2 Gift a Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Gift a Card is unsuccessful ");

		// Gift a card with invalid token
		Response giftaCardResponse2 = pageObj.endpoints().Api2GiftaCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, dataSet.get("design_id"));
		Assert.assertEquals(giftaCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2  gift a card");
		boolean isGiftaCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, giftaCardResponse2.asString());
		Assert.assertTrue(isGiftaCardInvalidTokenSchemaValidated, "API v2 Gift a Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Gift a Card is unsuccessful ");

		// Gift a card with invalid design id
		Response giftaCardResponse3 = pageObj.endpoints().Api2GiftaCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("invaliddesign"));
		Assert.assertEquals(giftaCardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_ACCEPTABLE,
				"Status code 406 did not matched for api2  gift a card");
		boolean isGiftaCardInvalidDesignSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema2, giftaCardResponse3.asString());
		Assert.assertTrue(isGiftaCardInvalidDesignSchemaValidated, "API v2 Gift a Card Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Gift a Card is unsuccessful ");

		// Gift a card with invalid mail
		String actualGiftCardInvalidEmailMsg = "";
		String expectedGiftCardInvalidEmailMsg = "You have already invited " + invaliduserEmail
				+ ". Ask them to join the program. We will notify you when they join.";
		int count = 0;
		while (count < 10) {
			try {
				TestListeners.extentTest.get().info("API v2 Gift a gift card hit count is: " + count);
				logger.info("API v2 Gift a gift card hit count is: " + count);
				Response giftCardInvalidEmailResponse = pageObj.endpoints().api2GiftCardGiftedWithRandomAmount(
						dataSet.get("client"), dataSet.get("secret"), token, invaliduserEmail, dataSet.get("design_id"),
						"fake-valid-nonce", "Test Name", "825", "VISA");
				TestListeners.extentTest.get()
						.info("API v2 Gift a gift card call response is: " + giftCardInvalidEmailResponse.asString());
				actualGiftCardInvalidEmailMsg = giftCardInvalidEmailResponse.jsonPath().get("errors[0]").toString();
				if (actualGiftCardInvalidEmailMsg.equals(expectedGiftCardInvalidEmailMsg)) {
					Assert.assertEquals(giftCardInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
							"Status code 202 did not match for API v2 Gift a Gift Card call with invalid email.");
					boolean isGiftCardInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.apiErrorsObjectSchema2, giftCardInvalidEmailResponse.asString());
					Assert.assertTrue(isGiftCardInvalidEmailSchemaValidated,
							"API v2 Gift a Gift Card Schema Validation failed");
					TestListeners.extentTest.get()
							.pass("API v2 Gift a gift card is unsuccessful because of invalid user email.");
					logger.info("API v2 Gift a gift card is unsuccessful because of invalid user email.");
					break;
				}
			} catch (Exception e) {

			}
			count++;
			utils.longWaitInSeconds(5);
		}
	}

	@Test(description = "SQ-T3097 Verify Api2 Social Cause Campaign and Notifications API negative flow", groups = "api", priority = 16)
	public void Api2SocialCauseCampaignAndNotificationsNegativeApi() throws InterruptedException {
		String invalidtoken = "1";
		String invalidsocial_cause_id = "1";
		String invalidgetsocial_cause_id = "1";
		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Create Social Cause Campaigns with invalid admin authorization
		String campaignName1 = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse1 = pageObj.endpoints().cReateSocialcauseCampaign(campaignName1,
				dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is unsuccessful");

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

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is successful");
		String getsocial_cause_id = getsocialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Get Social Cause Campaigns with invalid client
		Response getsocialCauseCampaignResponse1 = pageObj.endpoints()
				.Api2SocialCauseCampaign(dataSet.get("invalidclient"), dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		boolean isSocialCauseCampaignInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, getsocialCauseCampaignResponse1.asString());
		Assert.assertTrue(isSocialCauseCampaignInvalidClientSchemaValidated,
				"API v2 Get Social Cause Campaign Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is unsuccessful");

		// Get Social Cause Campaigns with invalid secret
		Response getsocialCauseCampaignResponse2 = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		boolean isSocialCauseCampaignInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, getsocialCauseCampaignResponse2.asString());
		Assert.assertTrue(isSocialCauseCampaignInvalidSecretSchemaValidated,
				"API v2 Get Social Cause Campaign Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is unsuccessful");

		// Get Social Cause Campaigns with invalid token
		Response getsocialCauseCampaignResponse3 = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		boolean isSocialCauseCampaignInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, getsocialCauseCampaignResponse3.asString());
		Assert.assertTrue(isSocialCauseCampaignInvalidTokenSchemaValidated,
				"API v2 Get Social Cause Campaign Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is unsuccessful");

		// Create Donation with invalid client
		Response createDonationResponse = pageObj.endpoints().Api2CreateDonation(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Donation");
		boolean isCreateDonationInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, createDonationResponse.asString());
		Assert.assertTrue(isCreateDonationInvalidClientSchemaValidated,
				"API v2 Create Donation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Create Donation with invalid secret
		Response createDonationResponse1 = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, getsocial_cause_id);
		Assert.assertEquals(createDonationResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Create Donation");
		boolean isCreateDonationInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, createDonationResponse1.asString());
		Assert.assertTrue(isCreateDonationInvalidSecretSchemaValidated,
				"API v2 Create Donation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Create Donation with invalid token
		Response createDonationResponse2 = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, getsocial_cause_id);
		Assert.assertEquals(createDonationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Donation");
		boolean isCreateDonationInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, createDonationResponse2.asString());
		Assert.assertTrue(isCreateDonationInvalidTokenSchemaValidated,
				"API v2 Create Donation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Create Donation with invalid get social cause id
		Response createDonationResponse3 = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, invalidgetsocial_cause_id);
		Assert.assertEquals(createDonationResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 Create Donation");
		boolean isCreateDonationInvalidSocialCauseIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, createDonationResponse3.asString());
		Assert.assertTrue(isCreateDonationInvalidSocialCauseIdSchemaValidated,
				"API v2 Create Donation Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Social Cause Campaign Details with invalid client
		Response socialCauseCampaignDetailsResponse = pageObj.endpoints().Api2SocialCausecampaigndetails(
				dataSet.get("invalidclient"), dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Donation");
		boolean isSocialCauseCampaignDetailsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, socialCauseCampaignDetailsResponse.asString());
		Assert.assertTrue(isSocialCauseCampaignDetailsInvalidClientSchemaValidated,
				"API v2 Social Cause Campaign Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Social Cause Campaign Details with invalid secret
		Response socialCauseCampaignDetailsResponse1 = pageObj.endpoints().Api2SocialCausecampaigndetails(
				dataSet.get("client"), dataSet.get("invalidsecret"), token, getsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Create Donation");
		boolean isSocialCauseCampaignDetailsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, socialCauseCampaignDetailsResponse1.asString());
		Assert.assertTrue(isSocialCauseCampaignDetailsInvalidSecretSchemaValidated,
				"API v2 Social Cause Campaign Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Social Cause Campaign Details with invalid token
		Response socialCauseCampaignDetailsResponse2 = pageObj.endpoints().Api2SocialCausecampaigndetails(
				dataSet.get("client"), dataSet.get("secret"), invalidtoken, getsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Donation");
		boolean isSocialCauseCampaignDetailsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, socialCauseCampaignDetailsResponse2.asString());
		Assert.assertTrue(isSocialCauseCampaignDetailsInvalidTokenSchemaValidated,
				"API v2 Social Cause Campaign Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Social Cause Campaign Details with invalid get social cause id
		Response socialCauseCampaignDetailsResponse3 = pageObj.endpoints().Api2SocialCausecampaigndetails(
				dataSet.get("client"), dataSet.get("secret"), token, invalidgetsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 Create Donation");
		boolean isSocialCauseCampaignDetailsInvalidSocialCauseIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, socialCauseCampaignDetailsResponse3.asString());
		Assert.assertTrue(isSocialCauseCampaignDetailsInvalidSocialCauseIdSchemaValidated,
				"API v2 Social Cause Campaign Details Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Donation is unsuccessful");

		// Deactivate Social Cause Campaign with invalid social_cause_id
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(invalidsocial_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is unsuccessful");

		// Deactivate Social Cause Campaign with invalid authorization id
		Response deactivateSocialCampaignResponse1 = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("invalidAdminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is unsuccessful");

		// Create Feedback
		Response createfeedbackResponse = pageObj.endpoints().Api2CreateFeedback(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create feedback");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");
		String feedback_id = createfeedbackResponse.jsonPath().getString("feedback_id");

		// Create Feedback with invalid client
		Response createfeedbackResponse1 = pageObj.endpoints().Api2CreateFeedback(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 201 did not matched for api2 create feedback");
		boolean isCreateFeedbackInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, createfeedbackResponse1.asString());
		Assert.assertTrue(isCreateFeedbackInvalidClientSchemaValidated,
				"API v2 Create Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Create Feedback with invalid secret
		Response createfeedbackResponse2 = pageObj.endpoints().Api2CreateFeedback(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(createfeedbackResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 201 did not matched for api2 create feedback");
		boolean isCreateFeedbackInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, createfeedbackResponse2.asString());
		Assert.assertTrue(isCreateFeedbackInvalidSecretSchemaValidated,
				"API v2 Create Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Create Feedback with invalid token
		Response createfeedbackResponse3 = pageObj.endpoints().Api2CreateFeedback(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(createfeedbackResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 201 did not matched for api2 create feedback");
		boolean isCreateFeedbackInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, createfeedbackResponse3.asString());
		Assert.assertTrue(isCreateFeedbackInvalidTokenSchemaValidated,
				"API v2 Create Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Update Feedback with invalid client
		Response updatefeedbackResponse = pageObj.endpoints().Api2UpdateFeedback(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, feedback_id);
		Assert.assertEquals(updatefeedbackResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 201 did not matched for api2 update feedback");
		boolean isUpdateFeedbackInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, updatefeedbackResponse.asString());
		Assert.assertTrue(isUpdateFeedbackInvalidClientSchemaValidated,
				"API v2 Update Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Update Feedback with invalid secret
		Response updatefeedbackResponse1 = pageObj.endpoints().Api2UpdateFeedback(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, feedback_id);
		Assert.assertEquals(updatefeedbackResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 201 did not matched for api2 update feedback");
		boolean isUpdateFeedbackInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, updatefeedbackResponse1.asString());
		Assert.assertTrue(isUpdateFeedbackInvalidSecretSchemaValidated,
				"API v2 Update Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications successful ");

		// Update Feedback with invalid token
		Response updatefeedbackResponse2 = pageObj.endpoints().Api2UpdateFeedback(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, feedback_id);
		Assert.assertEquals(updatefeedbackResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 update feedback");
		boolean isUpdateFeedbackInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, updatefeedbackResponse2.asString());
		Assert.assertTrue(isUpdateFeedbackInvalidTokenSchemaValidated,
				"API v2 Update Feedback Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch Client Token invalid client
		Response fetchClientTokenResponse = pageObj.endpoints().Api2FetchClientToken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchClientTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch client token");
		boolean isFetchClientTokenInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, fetchClientTokenResponse.asString());
		Assert.assertTrue(isFetchClientTokenInvalidClientSchemaValidated,
				"API v2 Fetch Client Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch Client Token with invalid secret
		Response fetchClientTokenResponse1 = pageObj.endpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(fetchClientTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 fetch client token");
		boolean isFetchClientTokenInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, fetchClientTokenResponse1.asString());
		Assert.assertTrue(isFetchClientTokenInvalidSecretSchemaValidated,
				"API v2 Fetch Client Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

		// Fetch Client Token with invalid token
		Response fetchClientTokenResponse2 = pageObj.endpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(fetchClientTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 fetch client token");
		boolean isFetchClientTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, fetchClientTokenResponse2.asString());
		Assert.assertTrue(isFetchClientTokenInvalidTokenSchemaValidated,
				"API v2 Fetch Client Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Fetch User Notifications unsuccessful ");

	}

	@Test(description = "SQ-T3098 Verify Api2 Challenges_Passcodes And Invitations api negative flow", groups = "api", priority = 17)
	public void Api2ChallengesPasscodesAndInvitationsNegativeApi() throws InterruptedException {
		String passcode = "123456";
		String invalidtoken = "1";
		String invalidpasscode = "1";

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// List Challenges with invalid client
		Response listChallengesResponse = pageObj.endpoints().Api2ListChallenges(dataSet.get("invalidclient"),
				dataSet.get("secret"), "es");
		Assert.assertEquals(listChallengesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 List Challenges");
		boolean isListChallengesInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, listChallengesResponse.asString());
		Assert.assertTrue(isListChallengesInvalidClientSchemaValidated,
				"API v2 List Challenges Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 List Challenges with invalid client call is unsuccessful");

		// List Challenges with invalid secret
		Response listChallengesResponse1 = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"),
				dataSet.get("invalidsecret"), "es");
		Assert.assertEquals(listChallengesResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 List Challenges");
		boolean isListChallengesInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, listChallengesResponse1.asString());
		Assert.assertTrue(isListChallengesInvalidSecretSchemaValidated,
				"API v2 List Challenges Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 List Challenges with invalid secret call is unsuccessful");

		// Forgot Passcode with invalid client
		Response forgotPasscodeResponse = pageObj.endpoints().Api2ForgotPasscode(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(forgotPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Forgot Passcode");
		boolean isForgotPasscodeInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, forgotPasscodeResponse.asString());
		Assert.assertTrue(isForgotPasscodeInvalidClientSchemaValidated,
				"API v2 Forgot Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Forgot Passcode with invalid client call is unsuccessful");

		// Forgot Passcode with invalid secret
		Response forgotPasscodeResponse1 = pageObj.endpoints().Api2ForgotPasscode(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(forgotPasscodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Forgot Passcode");
		boolean isForgotPasscodeInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, forgotPasscodeResponse1.asString());
		Assert.assertTrue(isForgotPasscodeInvalidSecretSchemaValidated,
				"API v2 Forgot Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Forgot Passcode with invalid secret call is unsuccessful");

		// Forgot Passcode with invalid token
		Response forgotPasscodeResponse2 = pageObj.endpoints().Api2ForgotPasscode(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(forgotPasscodeResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Forgot Passcode");
		boolean isForgotPasscodeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, forgotPasscodeResponse2.asString());
		Assert.assertTrue(isForgotPasscodeInvalidTokenSchemaValidated,
				"API v2 Forgot Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Forgot Passcode with invalid token call is unsuccessful");

		// Create Passcode with invalid client
		Response createPasscodeResponse = pageObj.endpoints().Api2CreatePasscode(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, passcode);
		Assert.assertEquals(createPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create passcode");
		boolean isCreatePasscodeInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, createPasscodeResponse.asString());
		Assert.assertTrue(isCreatePasscodeInvalidClientSchemaValidated,
				"API v2 Create Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Passcode with invalid client call is unsuccessful");

		// Create Passcode with invalid secret
		Response createPasscodeResponse1 = pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, passcode);
		Assert.assertEquals(createPasscodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 create passcode");
		boolean isCreatePasscodeInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, createPasscodeResponse1.asString());
		Assert.assertTrue(isCreatePasscodeInvalidSecretSchemaValidated,
				"API v2 Create Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Passcode with invalid secret call is unsuccessful");

		// Create Passcode with invalid token
		Response createPasscodeResponse2 = pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, passcode);
		Assert.assertEquals(createPasscodeResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 create passcode");
		boolean isCreatePasscodeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, createPasscodeResponse2.asString());
		Assert.assertTrue(isCreatePasscodeInvalidTokenSchemaValidated,
				"API v2 Create Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Passcode with invalid token call is unsuccessful");

		// Create Passcode with invalid passcode
		Response createPasscodeResponse3 = pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"),
				dataSet.get("secret"), token, invalidpasscode);
		Assert.assertEquals(createPasscodeResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create passcode");
		boolean isCreatePasscodeInvalidPasscodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2PasscodeErrorSchema, createPasscodeResponse3.asString());
		Assert.assertTrue(isCreatePasscodeInvalidPasscodeSchemaValidated,
				"API v2 Create Passcode Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Passcode with invalid passcode call is unsuccessful");
	}

	@Test(description = "SQ-T3098 Verify Api2 Challenges_Passcodes And Invitations api negative flow", groups = "api", priority = 18)
	public void Api2EpinClaimTokenInvitationsNegativeApi() throws InterruptedException {

		String passcode = "123456";
		String invalidtoken = "1";
		String invalidpasscode = "1";
		String invaliduuid = "1";
		String invalidinvitation_id = "1";
		String invalidclaim_token = "1";
		String invalidtokenNew = "1";

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Purchase Gift Card API2
		String uuid = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + attempts);
				// utils.longwait(5000);
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("design_id"));
				Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api2 purchase gift card");
				TestListeners.extentTest.get()
						.pass("Api2 Purchase Gift Card is successful " + purchaseGiftCardResponse.asString());
				uuid = purchaseGiftCardResponse.jsonPath().getString("uuid").toString();
				if (uuid != null) {
					flag = true;
					TestListeners.extentTest.get().info("uuid fetched successfully :" + uuid);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}
		// Generate Epin with invalid client
		Response generateEpinResponse = pageObj.endpoints().Api2GenerateEpin(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, passcode, uuid);
		Assert.assertEquals(generateEpinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Generate Epin");
		boolean isGenerateEpinInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, generateEpinResponse.asString());
		Assert.assertTrue(isGenerateEpinInvalidClientSchemaValidated, "API v2 Generate Epin Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Epin with invalid client is unsuccessful ");

		// Generate Epin with invalid secret
		Response generateEpinResponse1 = pageObj.endpoints().Api2GenerateEpin(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, passcode, uuid);
		Assert.assertEquals(generateEpinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Generate Epin");
		boolean isGenerateEpinInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, generateEpinResponse1.asString());
		Assert.assertTrue(isGenerateEpinInvalidSecretSchemaValidated, "API v2 Generate Epin Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Epin with invalid secret is unsuccessful ");

		// Generate Epin with invalid token
		Response generateEpinResponse2 = pageObj.endpoints().Api2GenerateEpin(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, passcode, uuid);
		Assert.assertEquals(generateEpinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Generate Epin");
		boolean isGenerateEpinInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, generateEpinResponse2.asString());
		Assert.assertTrue(isGenerateEpinInvalidTokenSchemaValidated, "API v2 Generate Epin Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Epin with invalid token is unsuccessful ");

		// Generate Epin with invalid passcode
		Response generateEpinResponse3 = pageObj.endpoints().Api2GenerateEpin(dataSet.get("client"),
				dataSet.get("secret"), token, invalidpasscode, uuid);
		Assert.assertEquals(generateEpinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v2 Generate Epin");
		boolean isGenerateEpinInvalidPasscodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, generateEpinResponse3.asString());
		Assert.assertTrue(isGenerateEpinInvalidPasscodeSchemaValidated,
				"API v2 Generate Epin Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Epin with invalid passcode is unsuccessful ");

		// Generate Epin with invalid uuid
		Response generateEpinResponse4 = pageObj.endpoints().Api2GenerateEpin(dataSet.get("client"),
				dataSet.get("secret"), token, passcode, invaliduuid);
		Assert.assertEquals(generateEpinResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v2 Generate Epin");
		boolean isGenerateEpinInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, generateEpinResponse4.asString());
		Assert.assertTrue(isGenerateEpinInvalidUuidSchemaValidated, "API v2 Generate Epin Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Generate Epin with invalid uuid is unsuccessful ");

		// Create Gift Card Claim Token
		Response gcClaimTokenResponse1 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(gcClaimTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 Create Gift Card Claim Token");
		TestListeners.extentTest.get().pass("Api2 Create Gift Card Claim Token is successful ");
		String claim_token1 = gcClaimTokenResponse1.jsonPath().getString("claim_token").toString();
		String invitation_id1 = gcClaimTokenResponse1.jsonPath().getString("invitation_id").toString();

		// Create Gift Card Claim Token with invalid client
		Response gcClaimTokenResponse2 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(gcClaimTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Gift Card Claim Token");
		boolean isGiftCardClaimTokenInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, gcClaimTokenResponse2.asString());
		Assert.assertTrue(isGiftCardClaimTokenInvalidClientSchemaValidated,
				"API v2 Create Gift Card Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Gift Card Claim Token is unsuccessful ");

		// Create Gift Card Claim Token with invalid secret
		Response gcClaimTokenResponse3 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, uuid);
		Assert.assertEquals(gcClaimTokenResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Create Gift Card Claim Token");
		boolean isGiftCardClaimTokenInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, gcClaimTokenResponse3.asString());
		Assert.assertTrue(isGiftCardClaimTokenInvalidSecretSchemaValidated,
				"API v2 Create Gift Card Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Gift Card Claim Token is unsuccessful ");

		// Create Gift Card Claim Token with invalid token
		Response gcClaimTokenResponse4 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, uuid);
		Assert.assertEquals(gcClaimTokenResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Create Gift Card Claim Token");
		boolean isGiftCardClaimTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, gcClaimTokenResponse4.asString());
		Assert.assertTrue(isGiftCardClaimTokenInvalidTokenSchemaValidated,
				"API v2 Create Gift Card Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Gift Card Claim Token is unsuccessful ");

		// Create Gift Card Claim Token with invalid uuid
		Response gcClaimTokenResponse5 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, invaliduuid);
		Assert.assertEquals(gcClaimTokenResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Create Gift Card Claim Token");
		boolean isGiftCardClaimTokenInvalidUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, gcClaimTokenResponse5.asString());
		Assert.assertTrue(isGiftCardClaimTokenInvalidUuidSchemaValidated,
				"API v2 Create Gift Card Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Create Gift Card Claim Token is unsuccessful ");

		// Delete An Invitation Claim Token with invalid client
		Response deleteClaimTokenResponse = pageObj.endpoints().Api2DeleteClaimToken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, invitation_id1);
		Assert.assertEquals(deleteClaimTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Delete An Invitation Claim Token");
		boolean isDeleteClaimTokenInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, deleteClaimTokenResponse.asString());
		Assert.assertTrue(isDeleteClaimTokenInvalidClientSchemaValidated,
				"API v2 Delete An Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete An Invitation Claim Token is unsuccessful ");

		// Delete An Invitation Claim Token with invalid secret
		Response deleteClaimTokenResponse1 = pageObj.endpoints().Api2DeleteClaimToken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, invitation_id1);
		Assert.assertEquals(deleteClaimTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Delete An Invitation Claim Token");
		boolean isDeleteClaimTokenInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, deleteClaimTokenResponse1.asString());
		Assert.assertTrue(isDeleteClaimTokenInvalidSecretSchemaValidated,
				"API v2 Delete An Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete An Invitation Claim Token is unsuccessful ");

		// Delete An Invitation Claim Token with invalid token
		Response deleteClaimTokenResponse2 = pageObj.endpoints().Api2DeleteClaimToken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, invitation_id1);
		Assert.assertEquals(deleteClaimTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Delete An Invitation Claim Token");
		boolean isDeleteClaimTokenInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, deleteClaimTokenResponse2.asString());
		Assert.assertTrue(isDeleteClaimTokenInvalidTokenSchemaValidated,
				"API v2 Delete An Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete An Invitation Claim Token is unsuccessful ");

		// Delete An Invitation Claim Token with invalid invitation id
		Response deleteClaimTokenResponse3 = pageObj.endpoints().Api2DeleteClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, invalidinvitation_id);
		Assert.assertEquals(deleteClaimTokenResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for api2 Delete An Invitation Claim Token");
		boolean isDeleteClaimTokenInvalidInvitationIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, deleteClaimTokenResponse3.asString());
		Assert.assertTrue(isDeleteClaimTokenInvalidInvitationIdSchemaValidated,
				"API v2 Delete An Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Delete An Invitation Claim Token is unsuccessful ");

		// Get Invitations with invalid client
		Response getInvitationsResponse = pageObj.endpoints().Api2GetInvitations(dataSet.get("invalidclient"),
				dataSet.get("secret"), token);
		Assert.assertEquals(getInvitationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Get Invitations");
		boolean isGetInvitationsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, getInvitationsResponse.asString());
		Assert.assertTrue(isGetInvitationsInvalidClientSchemaValidated,
				"API v2 Get Invitations Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get Invitations with invalid client is unsuccessful ");

		// Get Invitations with invalid secret
		Response getInvitationsResponse1 = pageObj.endpoints().Api2GetInvitations(dataSet.get("client"),
				dataSet.get("invalidsecret"), token);
		Assert.assertEquals(getInvitationsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Get Invitations");
		boolean isGetInvitationsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, getInvitationsResponse1.asString());
		Assert.assertTrue(isGetInvitationsInvalidSecretSchemaValidated,
				"API v2 Get Invitations Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get Invitations with invalid secret is unsuccessful ");

		// Get Invitations with invalid token
		Response getInvitationsResponse2 = pageObj.endpoints().Api2GetInvitations(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken);
		Assert.assertEquals(getInvitationsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Get Invitations");
		boolean isGetInvitationsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, getInvitationsResponse2.asString());
		Assert.assertTrue(isGetInvitationsInvalidTokenSchemaValidated,
				"API v2 Get Invitations Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Get Invitations with invalid token is unsuccessful ");

		// Check Status of the claim token with invalid client
		Response claimTokenResponse = pageObj.endpoints().Api2CheckStatusofclaimtoken(dataSet.get("invalidclient"),
				dataSet.get("secret"), token, claim_token1);
		Assert.assertEquals(claimTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Check Status of the claim token");
		boolean isClaimTokenStatusInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, claimTokenResponse.asString());
		Assert.assertTrue(isClaimTokenStatusInvalidClientSchemaValidated,
				"API v2 Check Status of the claim token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Check Status of the claim token is unsuccessful");

		// Check Status of the claim token with invalid secret
		Response claimTokenResponse1 = pageObj.endpoints().Api2CheckStatusofclaimtoken(dataSet.get("client"),
				dataSet.get("invalidsecret"), token, claim_token1);
		Assert.assertEquals(claimTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Check Status of the claim token");
		boolean isClaimTokenStatusInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, claimTokenResponse1.asString());
		Assert.assertTrue(isClaimTokenStatusInvalidSecretSchemaValidated,
				"API v2 Check Status of the claim token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Check Status of the claim token is unsuccessful");

		// Check Status of the claim token with invalid token
		Response claimTokenResponse2 = pageObj.endpoints().Api2CheckStatusofclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), invalidtoken, claim_token1);
		Assert.assertEquals(claimTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Check Status of the claim token");
		boolean isClaimTokenStatusInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, claimTokenResponse2.asString());
		Assert.assertTrue(isClaimTokenStatusInvalidTokenSchemaValidated,
				"API v2 Check Status of the claim token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Check Status of the claim token is unsuccessful");

		// Check Status of the claim token with invalid claim token
		Response claimTokenResponse3 = pageObj.endpoints().Api2CheckStatusofclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), token, invalidclaim_token);
		Assert.assertEquals(claimTokenResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Check Status of the claim token");
		boolean isClaimTokenStatusInvalidClaimTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, claimTokenResponse3.asString());
		Assert.assertTrue(isClaimTokenStatusInvalidClaimTokenSchemaValidated,
				"API v2 Check Status of the claim token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Check Status of the claim token is unsuccessful");

		String userEmailNew = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseNew = pageObj.endpoints().Api2SignUp(userEmailNew, dataSet.get("client"),
				dataSet.get("secret"));
		String tokenNew = signUpResponseNew.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponseNew.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Transfer a Gift Card Using Invitation Claim Token with invalid client
		Response claimTokenTransferResponse = pageObj.endpoints().Api2Transferclaimtoken(dataSet.get("invalidclient"),
				dataSet.get("secret"), tokenNew, claim_token1);
		Assert.assertEquals(claimTokenTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Transfer a Gift Card Using Invitation Claim Token");
		boolean isClaimTokenTransferInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, claimTokenTransferResponse.asString());
		Assert.assertTrue(isClaimTokenTransferInvalidClientSchemaValidated,
				"API v2 Transfer a Gift Card Using Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer a Gift Card Using Invitation Claim Token is unsuccessful");

		// Transfer a Gift Card Using Invitation Claim Token with invalid secret
		Response claimTokenTransferResponse1 = pageObj.endpoints().Api2Transferclaimtoken(dataSet.get("client"),
				dataSet.get("invalidsecret"), tokenNew, claim_token1);
		Assert.assertEquals(claimTokenTransferResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Transfer a Gift Card Using Invitation Claim Token");
		boolean isClaimTokenTransferInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, claimTokenTransferResponse1.asString());
		Assert.assertTrue(isClaimTokenTransferInvalidSecretSchemaValidated,
				"API v2 Transfer a Gift Card Using Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer a Gift Card Using Invitation Claim Token is unsuccessful");

		// Transfer a Gift Card Using Invitation Claim Token with invalid token
		Response claimTokenTransferResponse2 = pageObj.endpoints().Api2Transferclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), invalidtokenNew, claim_token1);
		Assert.assertEquals(claimTokenTransferResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 Transfer a Gift Card Using Invitation Claim Token");
		boolean isClaimTokenTransferInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, claimTokenTransferResponse2.asString());
		Assert.assertTrue(isClaimTokenTransferInvalidTokenSchemaValidated,
				"API v2 Transfer a Gift Card Using Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer a Gift Card Using Invitation Claim Token is unsuccessful");

		// Transfer a Gift Card Using Invitation Claim Token
		Response claimTokenTransferResponse3 = pageObj.endpoints().Api2Transferclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), tokenNew, invalidclaim_token);
		Assert.assertEquals(claimTokenTransferResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 Transfer a Gift Card Using Invitation Claim Token");
		boolean isClaimTokenTransferInvalidClaimTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, claimTokenTransferResponse3.asString());
		Assert.assertTrue(isClaimTokenTransferInvalidClaimTokenSchemaValidated,
				"API v2 Transfer a Gift Card Using Invitation Claim Token Schema Validation failed");
		TestListeners.extentTest.get().pass("Api2 Transfer a Gift Card Using Invitation Claim Token is unsuccessful");
	}

	@Test(description = "SQ-T4955 Negative Scenarios Of Fetch Active Purchasable Subscription Plan")
	public void T4955_fetch_active_purchasable_subscription_plan_Negative() {

		// fetch subscription plan with invalid client
		Response subscriptionPlan = pageObj.endpoints()
				.Api2FetchActivePurchasableSubscriptionPlans(dataSet.get("invalidclient"), dataSet.get("secret"));
		Assert.assertEquals(subscriptionPlan.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api2 Fetch Active purchasable Subscription Plan");
		boolean isFetchSubscriptionPlanInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2MissingClientSchema, subscriptionPlan.asString());
		Assert.assertTrue(isFetchSubscriptionPlanInvalidClientSchemaValidated,
				"API v2 Fetch Active Purchasable Subscription Plan Schema Validation Failed");
		logger.info("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");
		TestListeners.extentTest.get().pass("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");

		// fetch subscription plan with invalid signature
		Response subscriptionPlan1 = pageObj.endpoints()
				.Api2FetchActivePurchasableSubscriptionPlans(dataSet.get("client"), dataSet.get("invalidsecret"));
		Assert.assertEquals(subscriptionPlan1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for api2 Fetch Active purchasable Subscription Plan");
		boolean isFetchSubscriptionPlanInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, subscriptionPlan1.asString());
		Assert.assertTrue(isFetchSubscriptionPlanInvalidSecretSchemaValidated,
				"API v2 Fetch Active Purchasable Subscription Plan Schema Validation Failed");
		logger.info("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");
		TestListeners.extentTest.get().pass("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");

		if (!(env.equalsIgnoreCase("qa"))) {
			// fetch subscription plan with Unprocessable Entity
			Response subscriptionPlan2 = pageObj.endpoints().Api2FetchActivePurchasableSubscriptionPlans(
					dataSet.get("unprocessableClient"), dataSet.get("unprocessableSecret"));
			Assert.assertEquals(subscriptionPlan2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
					"Status code 422 did not matched for api2 Fetch Active purchasable Subscription Plan");
			boolean isFetchSubscriptionPlanUnprocessableEntitySchemaValidated = Utilities.validateJsonAgainstSchema(
					ApiResponseJsonSchema.api2DisabledFeatureErrorArraySchema, subscriptionPlan2.asString());
			Assert.assertTrue(isFetchSubscriptionPlanUnprocessableEntitySchemaValidated,
					"API v2 Fetch Active Purchasable Subscription Plan Schema Validation Failed");
			logger.info("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");
			TestListeners.extentTest.get().pass("Api2 Fetch Active purchasable Subscription Plan is unsuccessful");
		}

	}

	@Test(description = "SQ-T4953 Verify Negative Scenarios Of Mobile Api2 Mark Offers as read, List Applicable Offers, List User Offers, Program Meta")
	public void T4953_mark_offers_as_read_Negative() throws InterruptedException {
		String invalidSignatureMsg = "Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.";
		String unknownClientMsg = "Client ID is incorrect. Please check client param or contact us";
		String inactiveTokenMsg = "An active access token must be used to query information about the current user.";

		// User SignUp using API
		TestListeners.extentTest.get().info("== Mobile API2 User Signup ==");
		logger.info("== Mobile API2 User Signup ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("API2 User signup is successful");
		logger.info("API2 User signup is successful");

		// send reward amount to user Reedemable
		TestListeners.extentTest.get().info("== Dashboard API Send message to user ==");
		logger.info("== Dashboard API Send message to user ==");
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"50", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		String redeemableID = dataSet.get("redeemable_id");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");

		// get reward id
		TestListeners.extentTest.get().info("== Auth API Get Reward Id ==");
		logger.info("== Auth API Get Reward Id ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);
		logger.info("Reward id " + rewardId + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId + " is generated successfully ");

		// Negative case: mark offers as read using null client
		TestListeners.extentTest.get().info("== Mobile API2 Mark offers as read using null client ==");
		logger.info("== Mobile API2 Mark offers as read using null client ==");
		Response offerResponse = pageObj.endpoints().Api2markOffersAsRead(token, dataSet.get("nullClient"),
				dataSet.get("secret"), rewardId, dataSet.get("event_type"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api2 mark offers as read");
		boolean isMarkReadNullClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2MissingClientSchema, offerResponse.asString());
		Assert.assertTrue(isMarkReadNullClientSchemaValidated, "API v2 Mark Offers As Read Schema Validation Failed");
		String markReadNullClientMsg = offerResponse.jsonPath().get("errors.client").toString();
		Assert.assertEquals(markReadNullClientMsg, "Required parameter missing or the value is empty.");
		logger.info("Api2 mark offers as read is unsuccessful because of null client");
		TestListeners.extentTest.get().pass("Api2 mark offers as read is unsuccessful because of null client");

		// Negative case: mark offers as read using invalid client
		TestListeners.extentTest.get().info("== Mobile API2 Mark offers as read using invalid client ==");
		logger.info("== Mobile API2 Mark offers as read using invalid client ==");
		Response offerResponse1 = pageObj.endpoints().Api2markOffersAsRead(token, dataSet.get("invalidclient"),
				dataSet.get("invalidsecret"), dataSet.get("invalidReward"), dataSet.get("event_type"));
		Assert.assertEquals(offerResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api2 mark offers as read");
		boolean isMarkReadInvalidClientSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2UnknownClientSchema, offerResponse1.asString());
		Assert.assertTrue(isMarkReadInvalidClientSchemaValidated,
				"API v2 Mark Offers As Read Schema Validation Failed");
		String markReadInvalidClientMsg = offerResponse1.jsonPath().get("errors.unknown_client[0]").toString();
		Assert.assertEquals(markReadInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get().pass("Api2 mark offers as read is unsuccessful because of invalid client");
		logger.info("Api2 mark offers as read is unsuccessful because of invalid client");

		// Negative case: mark offers as read using invalid event type
		TestListeners.extentTest.get().info("== Mobile API2 Mark offers as read using invalid event type ==");
		logger.info("== Mobile API2 Mark offers as read using invalid event type ==");
		Response offerResponse2 = pageObj.endpoints().Api2markOffersAsRead(token, dataSet.get("client"),
				dataSet.get("secret"), rewardId, dataSet.get("invalid_event_type"));
		Assert.assertEquals(offerResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 mark offers as read");
		boolean isMarkReadInvalidEventTypeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2InvalidEventTypeSchema, offerResponse2.asString());
		Assert.assertTrue(isMarkReadInvalidEventTypeSchemaValidated,
				"API v2 Mark Offers As Read Schema Validation Failed");
		String markReadInvalidEventTypeMsg = offerResponse2.jsonPath().get("errors.invalid_event_type[0]").toString();
		Assert.assertEquals(markReadInvalidEventTypeMsg,
				"Invalid name given. It should be read_through_app or app_open_via_push");
		TestListeners.extentTest.get().pass("API2 mark offers as read is unsuccessful because of invalid event type");
		logger.info("Api2 mark offers as read is unsuccessful because of invalid event type");

		// Negative case: List Applicable Offers (Redemptions 1.0) with invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid client ==");
		logger.info("== Mobile API2 List Applicable Offers with invalid client ==");
		Response applicableOfferInvalidClientResponse = pageObj.endpoints().Api2ListApplicableOffers("1",
				dataSet.get("secret"), token, dataSet.get("location_id"));
		Assert.assertEquals(applicableOfferInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApplicableOfferInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, applicableOfferInvalidClientResponse.asString());
		Assert.assertTrue(isApplicableOfferInvalidClientSchemaValidated,
				"Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid client Schema Validation Failed");
		String applicableOfferInvalidClientMsg = applicableOfferInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(applicableOfferInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get()
				.pass("Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid client is unsuccessful");
		logger.info("Mobile API2 List Applicable Offers with invalid client is unsuccessful");

		// Negative case: List Applicable Offers (Redemptions 1.0) with invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid secret ==");
		logger.info("== Mobile API2 List Applicable Offers with invalid secret ==");
		Response applicableOfferInvalidSecretResponse = pageObj.endpoints()
				.Api2ListApplicableOffers(dataSet.get("client"), "1", token, dataSet.get("location_id"));
		Assert.assertEquals(applicableOfferInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isApplicableOfferInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, applicableOfferInvalidSecretResponse.asString());
		Assert.assertTrue(isApplicableOfferInvalidSecretSchemaValidated,
				"Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid secret Schema Validation Failed");
		String applicableOfferInvalidSecretMsg = applicableOfferInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(applicableOfferInvalidSecretMsg, invalidSignatureMsg);
		TestListeners.extentTest.get()
				.pass("Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid secret is unsuccessful");

		// Negative case: List Applicable Offers (Redemptions 1.0) with invalid token
		TestListeners.extentTest.get()
				.info("== Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid token ==");
		logger.info("== Mobile API2 List Applicable Offers with invalid token ==");
		Response applicableOfferInvalidTokenResponse = pageObj.endpoints().Api2ListApplicableOffers(
				dataSet.get("client"), dataSet.get("secret"), "1", dataSet.get("location_id"));
		Assert.assertEquals(applicableOfferInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApplicableOfferInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, applicableOfferInvalidTokenResponse.asString());
		Assert.assertTrue(isApplicableOfferInvalidTokenSchemaValidated,
				"Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid token Schema Validation Failed");
		String applicableOfferInvalidTokenMsg = applicableOfferInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(applicableOfferInvalidTokenMsg, inactiveTokenMsg);
		TestListeners.extentTest.get()
				.pass("Mobile API2 List Applicable Offers (Redemptions 1.0) with invalid token is unsuccessful");
		logger.info("Mobile API2 List Applicable Offers with invalid token is unsuccessful");

		// Negative case: List User Offers with invalid client
		TestListeners.extentTest.get().info("== Mobile API2 List User Offers with invalid client ==");
		logger.info("== Mobile API2 List User Offers with invalid client ==");
		Response listOffersInvalidClientResponse = pageObj.endpoints().Api2ListOffers("1", dataSet.get("secret"),
				token);
		Assert.assertEquals(listOffersInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isListOffersInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, listOffersInvalidClientResponse.asString());
		Assert.assertTrue(isListOffersInvalidClientSchemaValidated,
				"Mobile API2 List User Offers with invalid client Schema Validation Failed");
		String listOffersInvalidClientMsg = listOffersInvalidClientResponse.jsonPath().get("errors.unknown_client[0]")
				.toString();
		Assert.assertEquals(listOffersInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get().pass("Mobile API2 List User Offers with invalid client is unsuccessful");
		logger.info("Mobile API2 List User Offers with invalid client is unsuccessful");

		// Negative case: List User Offers with invalid secret
		TestListeners.extentTest.get().info("== Mobile API2 List User Offers with invalid secret ==");
		logger.info("== Mobile API2 List User Offers with invalid secret ==");
		Response listOffersInvalidSecretResponse = pageObj.endpoints().Api2ListOffers(dataSet.get("client"), "1",
				token);
		Assert.assertEquals(listOffersInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isListOffersInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, listOffersInvalidSecretResponse.asString());
		Assert.assertTrue(isListOffersInvalidSecretSchemaValidated,
				"Mobile API2 List User Offers with invalid secret Schema Validation Failed");
		String listOffersInvalidSecretMsg = listOffersInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(listOffersInvalidSecretMsg, invalidSignatureMsg);
		TestListeners.extentTest.get().pass("Mobile API2 List User Offers with invalid secret is unsuccessful");
		logger.info("Mobile API2 List User Offers with invalid secret is unsuccessful");

		// Negative case: List User Offers with invalid token
		TestListeners.extentTest.get().info("== Mobile API2 List User Offers with invalid token ==");
		logger.info("== Mobile API2 List User Offers with invalid token ==");
		Response listOffersInvalidTokenResponse = pageObj.endpoints().Api2ListOffers(dataSet.get("client"),
				dataSet.get("secret"), "1");
		Assert.assertEquals(listOffersInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isListOffersInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, listOffersInvalidTokenResponse.asString());
		Assert.assertTrue(isListOffersInvalidTokenSchemaValidated,
				"Mobile API2 List User Offers with invalid token Schema Validation Failed");
		String listOffersInvalidTokenMsg = listOffersInvalidTokenResponse.jsonPath().get("errors.unauthorized[0]")
				.toString();
		Assert.assertEquals(listOffersInvalidTokenMsg, inactiveTokenMsg);
		TestListeners.extentTest.get().pass("Mobile API2 List User Offers with invalid token is unsuccessful");
		logger.info("Mobile API2 List User Offers with invalid token is unsuccessful");

		// Negative case: Program Meta with invalid client
		TestListeners.extentTest.get().info("== Mobile API2 Program Meta with invalid client ==");
		logger.info("== Mobile API2 Program Meta with invalid client ==");
		Response metaInvalidClientResponse = pageObj.endpoints().Api2Cards("1", dataSet.get("secret"));
		Assert.assertEquals(metaInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isMetaInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, metaInvalidClientResponse.asString());
		Assert.assertTrue(isMetaInvalidClientSchemaValidated,
				"Mobile API2 Program Meta with invalid client Schema Validation Failed");
		String metaInvalidClientMsg = metaInvalidClientResponse.jsonPath().get("errors.unknown_client[0]").toString();
		Assert.assertEquals(metaInvalidClientMsg, unknownClientMsg);
		TestListeners.extentTest.get().pass("Mobile API2 Program Meta with invalid client is unsuccessful");
		logger.info("Mobile API2 Program Meta with invalid client is unsuccessful");

		// Negative case: Program Meta with invalid secret
		TestListeners.extentTest.get().info("== Mobile API2 Program Meta with invalid secret ==");
		logger.info("== Mobile API2 Program Meta with invalid secret ==");
		Response metaInvalidSecretResponse = pageObj.endpoints().Api2Cards(dataSet.get("client"), "1");
		Assert.assertEquals(metaInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isMetaInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, metaInvalidSecretResponse.asString());
		Assert.assertTrue(isMetaInvalidSecretSchemaValidated,
				"Mobile API2 Program Meta with invalid secret Schema Validation Failed");
		String metaInvalidSecretMsg = metaInvalidSecretResponse.jsonPath().get("errors.invalid_signature[0]")
				.toString();
		Assert.assertEquals(metaInvalidSecretMsg, invalidSignatureMsg);
		TestListeners.extentTest.get().pass("Mobile API2 Program Meta with invalid secret is unsuccessful");
		logger.info("Mobile API2 Program Meta with invalid secret is unsuccessful");

	}

	@Test(description = "SQ-T4764 Verify Generate Single Scan Code negative scenarios; "
			+ "SQ-T4766 Verify Get Access Code (Redemptions 2.0) negative scenarios", groups = "api", priority = 19)
	public void api2generateSingleScanCodeNegative() throws InterruptedException {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String firstName = signUpResponse.jsonPath().get("user.first_name").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api2 signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// API2 Generate Single Scan Code using invalid payment type
		Response singleScanCodeInvalidPaymentTypeResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("invalidPaymentType"), dataSet.get("transactionToken"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeInvalidPaymentTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeInvalidPaymentTypeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, singleScanCodeInvalidPaymentTypeResponse.asString());
		Assert.assertTrue(isSingleScanCodeInvalidPaymentTypeSchemaValidated,
				"API v2 Generate Single Scan Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Generate Single Scan Code call is unsuccessful because of invalid payment type");

		// API2 Generate Single Scan Code using invalid client
		Response singleScanCodeInvalidClientResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("creditCardPaymentType"), dataSet.get("transactionToken"), dataSet.get("invalidClient"),
				dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, singleScanCodeInvalidClientResponse.asString());
		Assert.assertTrue(isSingleScanCodeInvalidClientSchemaValidated,
				"API v2 Generate Single Scan Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Generate Single Scan Code call is unsuccessful because of invalid client");

		// API2 Generate Single Scan Code using invalid secret
		Response singleScanCodeInvalidSecretResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("creditCardPaymentType"), dataSet.get("transactionToken"), dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(singleScanCodeInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, singleScanCodeInvalidSecretResponse.asString());
		Assert.assertTrue(isSingleScanCodeInvalidSecretSchemaValidated,
				"API v2 Generate Single Scan Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Generate Single Scan Code call is unsuccessful because of invalid secret");

		// API2 Generate Single Scan Code using invalid user access token
		Response singleScanCodeInvalidTokenResponse = pageObj.endpoints().api2SingleScanCode(
				dataSet.get("invalidToken"), dataSet.get("creditCardPaymentType"), dataSet.get("transactionToken"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, singleScanCodeInvalidTokenResponse.asString());
		Assert.assertTrue(isSingleScanCodeInvalidTokenSchemaValidated,
				"API v2 Generate Single Scan Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Generate Single Scan Code call is unsuccessful because of invalid token");

		// API2 Generate Single Scan Code with missing transaction token
		Response singleScanCodeMissingParameterResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("creditCardPaymentType"), "", dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeMissingParameterResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeMissingTransactionTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingTransactionTokenSchema,
				singleScanCodeMissingParameterResponse.asString());
		Assert.assertTrue(isSingleScanCodeMissingTransactionTokenSchemaValidated,
				"API v2 Generate Single Scan Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Generate Single Scan Code call is unsuccessful because of missing transaction token");

		// API2 Purchase Gift Card
		String gift_card_uuid = "";
		int counter = 0;
		while (counter < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + counter);
				logger.info("API hit count is : " + counter);
				utils.longwait(5000);
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("designId"), dataSet.get("amount"),
						dataSet.get("expDate"), firstName);
				gift_card_uuid = purchaseGiftCardResponse.jsonPath().getString("uuid").toString();
				if (gift_card_uuid != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for api2 purchase gift card");
					TestListeners.extentTest.get().info("Gift card uuid fetched successfully: " + gift_card_uuid);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
		}

		// API2 Get Access Code (Redemptions 2.0) using invalid payment type
		Response getAccessCodeInvalidPaymentTypeResponse = pageObj.endpoints().api2GetAccessCode(token,
				dataSet.get("invalidPaymentType"), gift_card_uuid, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(getAccessCodeInvalidPaymentTypeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API2 Get Access Code");
		boolean isGetAccessCodeInvalidPaymentTypeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, getAccessCodeInvalidPaymentTypeResponse.asString());
		Assert.assertTrue(isGetAccessCodeInvalidPaymentTypeSchemaValidated,
				"API v2 Get Access Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Get Access Code call is unsuccessful because of invalid payment type");

		// API2 Get Access Code (Redemptions 2.0) with missing gift card uuid
		Response getAccessCodeMissingGiftCardUuidResponse = pageObj.endpoints().api2GetAccessCode(token,
				dataSet.get("giftCardPaymentType"), "", dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(getAccessCodeMissingGiftCardUuidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API2 Get Access Code");
		boolean isGetAccessCodeMissingGiftCardUuidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingGiftCardUuidSchema,
				getAccessCodeMissingGiftCardUuidResponse.asString());
		Assert.assertTrue(isGetAccessCodeMissingGiftCardUuidSchemaValidated,
				"API v2 Get Access Code Schema Validation Failed");
		TestListeners.extentTest.get()
				.pass("API2 Get Access Code call is unsuccessful because of missing gift card uuid");

		// API2 Get Access Code (Redemptions 2.0) using invalid client
		Response getAccessCodeInvalidClientResponse = pageObj.endpoints().api2GetAccessCode(token,
				dataSet.get("giftCardPaymentType"), gift_card_uuid, dataSet.get("invalidClient"),
				dataSet.get("secret"));
		Assert.assertEquals(getAccessCodeInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Get Access Code");
		boolean isGetAccessCodeInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, getAccessCodeInvalidClientResponse.asString());
		Assert.assertTrue(isGetAccessCodeInvalidClientSchemaValidated,
				"API v2 Get Access Code Schema Validation Failed");
		TestListeners.extentTest.get().pass("API2 Get Access Code call is unsuccessful because of invalid client");

		// API2 Get Access Code (Redemptions 2.0) using invalid secret
		Response getAccessCodeInvalidSecretResponse = pageObj.endpoints().api2GetAccessCode(token,
				dataSet.get("giftCardPaymentType"), gift_card_uuid, dataSet.get("client"),
				dataSet.get("invalidSecret"));
		Assert.assertEquals(getAccessCodeInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API2 Get Access Code");
		boolean isGetAccessCodeInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, getAccessCodeInvalidSecretResponse.asString());
		Assert.assertTrue(isGetAccessCodeInvalidSecretSchemaValidated,
				"API v2 Get Access Code Schema Validation Failed");
		TestListeners.extentTest.get().pass("API2 Get Access Code call is unsuccessful because of invalid secret");

		// API2 Get Access Code (Redemptions 2.0) using invalid user access token
		Response getAccessCodeInvalidTokenResponse = pageObj.endpoints().api2GetAccessCode(dataSet.get("invalidToken"),
				dataSet.get("giftCardPaymentType"), gift_card_uuid, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(getAccessCodeInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API2 Get Access Code");
		boolean isGetAccessCodeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, getAccessCodeInvalidTokenResponse.asString());
		Assert.assertTrue(isGetAccessCodeInvalidTokenSchemaValidated,
				"API v2 Get Access Code Schema Validation Failed");
		TestListeners.extentTest.get().pass("API2 Get Access Code call is unsuccessful because of invalid token");

	}

//	@Test(description = "SQ-T4928 Verify API2 Payment Cards negative scenarios", groups = "api", priority = 20) // it keeps failing due to heartland service
	public void api2PaymentCardsNegative() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String authToken = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api2 user signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// Generate Heartland token
		Response heartlandTokenResponse = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartlandPublicKey"));
		Assert.assertEquals(heartlandTokenResponse.statusCode(), 201,
				"Not able to generate single Token for heartland adapter");
		String heartlandToken = heartlandTokenResponse.jsonPath().getString("token_value");
		TestListeners.extentTest.get().pass("Heartland Single token is generated: " + heartlandToken);

		// Create Payment Card using invalid adapter code
		Response createPaymentCardInvalidAdapterCodeResponse = pageObj.endpoints().api2CreatePaymentCard(authToken,
				heartlandToken, dataSet.get("invalidAdapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardInvalidAdapterCodeResponse.statusCode(), 422,
				"Status code 422 did not match for api2 Create Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Create Payment Card call is unsuccessful because of invalid adapter code");

		// Create Payment Card using invalid client
		Response createPaymentCardInvalidClientResponse = pageObj.endpoints().api2CreatePaymentCard(authToken,
				heartlandToken, dataSet.get("adapterCode"), dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardInvalidClientResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Create Payment Card");
		TestListeners.extentTest.get().pass("API2 Create Payment Card call is unsuccessful because of invalid client");

		// Create Payment Card using invalid secret
		Response createPaymentCardInvalidSecretResponse = pageObj.endpoints().api2CreatePaymentCard(authToken,
				heartlandToken, dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(createPaymentCardInvalidSecretResponse.statusCode(), 412,
				"Status code 412 did not match for api2 Create Payment Card");
		TestListeners.extentTest.get().pass("API2 Create Payment Card call is unsuccessful because of invalid secret");

		// Create Payment Card with missing Heartland token
		Response createPaymentCardInvalidHeartlandTokenResponse = pageObj.endpoints().api2CreatePaymentCard(authToken,
				"", dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardInvalidHeartlandTokenResponse.statusCode(), 400,
				"Status code 400 did not match for api2 Create Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Create Payment Card call is unsuccessful because of missing Heartland token");

		// Create Payment Card using invalid user access token
		Response createPaymentCardInvalidUserTokenResponse = pageObj.endpoints().api2CreatePaymentCard(
				dataSet.get("invalidToken"), heartlandToken, dataSet.get("adapterCode"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardInvalidUserTokenResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Create Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Create Payment Card call is unsuccessful because of invalid user access token");

		// Create Payment Card
		Response createPaymentCardResponse = pageObj.endpoints().api2CreatePaymentCard(authToken, heartlandToken,
				dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardResponse.statusCode(), 200,
				"Status code 200 did not match for api2 Create Payment Card");
		String paymentCardUuid = createPaymentCardResponse.jsonPath().getString("uuid");
		TestListeners.extentTest.get().pass("Payment card is created with uuid: " + paymentCardUuid);

		// Update Payment Card using invalid Payment card uuid
		Response updatePaymentCardInvalidUuidResponse = pageObj.endpoints().api2UpdatePaymentCard(authToken,
				dataSet.get("invalidPaymentCardUuid"), dataSet.get("nicknameToUpdate"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardInvalidUuidResponse.statusCode(), 422,
				"Status code 422 did not match for api2 Update Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Update Payment Card call is unsuccessful because of invalid payment card uuid");

		// Update Payment Card using invalid client
		Response updatePaymentCardInvalidClientResponse = pageObj.endpoints().api2UpdatePaymentCard(authToken,
				paymentCardUuid, dataSet.get("nicknameToUpdate"), dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardInvalidClientResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Update Payment Card");
		TestListeners.extentTest.get().pass("API2 Update Payment Card call is unsuccessful because of invalid client");

		// Update Payment Card using invalid secret
		Response updatePaymentCardInvalidSecretResponse = pageObj.endpoints().api2UpdatePaymentCard(authToken,
				paymentCardUuid, dataSet.get("nicknameToUpdate"), dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(updatePaymentCardInvalidSecretResponse.statusCode(), 412,
				"Status code 412 did not match for api2 Update Payment Card");
		TestListeners.extentTest.get().pass("API2 Update Payment Card call is unsuccessful because of invalid secret");

		// Update Payment Card using invalid user access token
		Response updatePaymentCardInvalidUserTokenResponse = pageObj.endpoints().api2UpdatePaymentCard(
				dataSet.get("invalidToken"), paymentCardUuid, dataSet.get("nicknameToUpdate"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardInvalidUserTokenResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Update Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Update Payment Card call is unsuccessful because of invalid user access token");

		// Update Payment Card with missing client
		Response updatePaymentCardMissingClientResponse = pageObj.endpoints().api2UpdatePaymentCard(authToken,
				paymentCardUuid, dataSet.get("nicknameToUpdate"), "", dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardMissingClientResponse.statusCode(), 400,
				"Status code 400 did not match for api2 Update Payment Card");
		TestListeners.extentTest.get().pass("API2 Update Payment Card call is unsuccessful because of missing client");

		// Fetch Payment Card using invalid adapter code
		Response fetchPaymentCardInvalidAdapterCodeResponse = pageObj.endpoints().api2FetchPaymentCard(authToken,
				dataSet.get("invalidAdapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardInvalidAdapterCodeResponse.statusCode(), 422,
				"Status code 422 did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Payment Card call is unsuccessful because of invalid adapter code");

		// Fetch Payment Card using invalid client
		Response fetchPaymentCardInvalidClientResponse = pageObj.endpoints().api2FetchPaymentCard(authToken,
				dataSet.get("adapterCode"), dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardInvalidClientResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get().pass("API2 Fetch Payment Card call is unsuccessful because of invalid client");

		// Fetch Payment Card using invalid secret
		Response fetchPaymentCardInvalidSecretResponse = pageObj.endpoints().api2FetchPaymentCard(authToken,
				dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(fetchPaymentCardInvalidSecretResponse.statusCode(), 412,
				"Status code 412 did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get().pass("API2 Fetch Payment Card call is unsuccessful because of invalid secret");

		// Fetch Payment Card using invalid user access token
		Response fetchPaymentCardInvalidUserTokenResponse = pageObj.endpoints().api2FetchPaymentCard(
				dataSet.get("invalidToken"), dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardInvalidUserTokenResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Payment Card call is unsuccessful because of invalid user access token");

		// Fetch Payment Card with missing client
		Response fetchPaymentCardMissingClientResponse = pageObj.endpoints().api2FetchPaymentCard(authToken,
				dataSet.get("adapterCode"), "", dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardMissingClientResponse.statusCode(), 400,
				"Status code 400 did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get().pass("API2 Fetch Payment Card call is unsuccessful because of missing client");

		// Delete Payment Card using invalid Payment card uuid
		Response deletePaymentCardInvalidUuidResponse = pageObj.endpoints().api2DeletePaymentCard(authToken,
				dataSet.get("invalidPaymentCardUuid"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardInvalidUuidResponse.statusCode(), 422,
				"Status code 422 did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Delete Payment Card call is unsuccessful because of invalid Payment card uuid");

		// Delete Payment Card using invalid client
		Response deletePaymentCardInvalidClientResponse = pageObj.endpoints().api2DeletePaymentCard(authToken,
				paymentCardUuid, dataSet.get("invalidClient"), dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardInvalidClientResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get().pass("API2 Delete Payment Card call is unsuccessful because of invalid client");

		// Delete Payment Card using invalid secret
		Response deletePaymentCardInvalidSecretResponse = pageObj.endpoints().api2DeletePaymentCard(authToken,
				paymentCardUuid, dataSet.get("client"), dataSet.get("invalidSecret"));
		Assert.assertEquals(deletePaymentCardInvalidSecretResponse.statusCode(), 412,
				"Status code 412 did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get().pass("API2 Delete Payment Card call is unsuccessful because of invalid secret");

		// Delete Payment Card using invalid user access token
		Response deletePaymentCardInvalidTokenResponse = pageObj.endpoints().api2DeletePaymentCard(
				dataSet.get("invalidToken"), paymentCardUuid, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardInvalidTokenResponse.statusCode(), 401,
				"Status code 401 did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get()
				.pass("API2 Delete Payment Card call is unsuccessful because of invalid user access token");

		// Delete Payment Card with missing client
		Response deletePaymentCardMissingClientResponse = pageObj.endpoints().api2DeletePaymentCard(authToken,
				paymentCardUuid, "", dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardMissingClientResponse.statusCode(), 400,
				"Status code 400 did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get().pass("API2 Delete Payment Card call is unsuccessful because of missing client");

	}

	@Test(description = "Verify Mobile API v2 Negative Scenarios:- SQ-T5138: Version Notes; SQ-T5142: Request for User Account Deletion; "
			+ "SQ-T5226: Beacon Entry; SQ-T5239: Beacon Exit", groups = "api", priority = 21)
	public void verifyAPIv2VersionNotesNegative() {

		String invalidValue = "123abc";
		String invalidSignatureMsg = "Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.";
		String invalidClientIdMsg = "Client ID is incorrect. Please check client param or contact us";
		String invalidAccessTokenMsg = "An active access token must be used to query information about the current user.";
		String invalidBeaconIdMsg = "location with ID  not found.";
		String missingParameterMsg = "Required parameter missing or the value is empty.";

		// Version Notes with missing version
		TestListeners.extentTest.get().info("== Mobile API v2: Version Notes with missing version ==");
		logger.info("== Mobile API v2: Version Notes with missing version ==");
		Response versionNotesMissingVersionResponse = pageObj.endpoints().api2VersionNotes("", dataSet.get("os"),
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingVersionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isVersionNotesMissingVersionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingVersionSchema,
				versionNotesMissingVersionResponse.asString());
		Assert.assertTrue(isVersionNotesMissingVersionSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNotesMissingVersionMsg = versionNotesMissingVersionResponse.jsonPath().get("errors.version")
				.toString();
		Assert.assertEquals(versionNotesMissingVersionMsg, missingParameterMsg, "Message did not match");
		TestListeners.extentTest.get().pass("API v2 Version Notes call with missing version is unsuccessful");
		logger.info("API v2 Version Notes call with missing version is unsuccessful");

		// Version Notes with missing OS
		TestListeners.extentTest.get().info("== Mobile API v2: Version Notes with missing OS ==");
		logger.info("== Mobile API v2: Version Notes with missing OS ==");
		Response versionNotesMissingOsResponse = pageObj.endpoints().api2VersionNotes(dataSet.get("version"), "",
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingOsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isVersionNotesMissingOsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingOsSchema, versionNotesMissingOsResponse.asString());
		Assert.assertTrue(isVersionNotesMissingOsSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNotesMissingOsMsg = versionNotesMissingOsResponse.jsonPath().get("errors.os").toString();
		Assert.assertEquals(versionNotesMissingOsMsg, missingParameterMsg, "Message did not match");
		TestListeners.extentTest.get().pass("API v2 Version Notes call with missing OS is unsuccessful");
		logger.info("API v2 Version Notes call with missing OS is unsuccessful");

		// Version Notes with invalid OS
		TestListeners.extentTest.get().info("== Mobile API v2: Version Notes using invalid OS ==");
		logger.info("== Mobile API v2: Version Notes using invalid OS ==");
		Response versionNotesInvalidOsResponse = pageObj.endpoints().api2VersionNotes(dataSet.get("version"),
				invalidValue, dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesInvalidOsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isVersionNotesInvalidOsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingOsSchema, versionNotesInvalidOsResponse.asString());
		Assert.assertTrue(isVersionNotesInvalidOsSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNotesInvalidOsMsg = versionNotesInvalidOsResponse.jsonPath().get("errors.os").toString();
		Assert.assertEquals(versionNotesInvalidOsMsg, "Operating System should be either android or ios.",
				"Message did not match");
		TestListeners.extentTest.get().pass("API v2 Version Notes call with invalid OS is unsuccessful");
		logger.info("API v2 Version Notes call with invalid OS is unsuccessful");

		// Version Notes with missing model
		TestListeners.extentTest.get().info("== Mobile API v2: Version Notes with missing model ==");
		logger.info("== Mobile API v2: Version Notes with missing model ==");
		Response versionNotesMissingModelResponse = pageObj.endpoints().api2VersionNotes(dataSet.get("version"),
				dataSet.get("os"), "", dataSet.get("client"));
		Assert.assertEquals(versionNotesMissingModelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isVersionNotesMissingModelSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingModelSchema, versionNotesMissingModelResponse.asString());
		Assert.assertTrue(isVersionNotesMissingModelSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNotesMissingModelMsg = versionNotesMissingModelResponse.jsonPath().get("errors.model").toString();
		Assert.assertEquals(versionNotesMissingModelMsg, missingParameterMsg, "Message did not match");
		TestListeners.extentTest.get().pass("API v2 Version Notes call with missing model is unsuccessful");
		logger.info("API v2 Version Notes call with missing model is unsuccessful");

		// Version Notes with missing client
		TestListeners.extentTest.get().info("== Mobile API v2: Version Notes with missing client ==");
		logger.info("== Mobile API v2: Version Notes with missing client ==");
		Response versionNotesMissingClientResponse = pageObj.endpoints().api2VersionNotes(dataSet.get("version"),
				dataSet.get("os"), dataSet.get("model"), "");
		Assert.assertEquals(versionNotesMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for Version Notes");
		boolean isVersionNotesMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesMissingClientSchema,
				versionNotesMissingClientResponse.asString());
		Assert.assertTrue(isVersionNotesMissingClientSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNotesMissingClientMsg = versionNotesMissingClientResponse.jsonPath().get("errors.client")
				.toString();
		Assert.assertEquals(versionNotesMissingClientMsg, missingParameterMsg, "Message did not match");
		TestListeners.extentTest.get().pass("API v2 Version Notes call with missing client is unsuccessful");
		logger.info("API v2 Version Notes call with missing client is unsuccessful");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String authToken = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Beacon Entry with invalid secret
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with invalid secret ==");
		logger.info("== Mobile API v2: Beacon Entry with invalid secret ==");
		Response beaconEntryInvalidSecretResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				invalidValue, authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Beacon Entry call with invalid secret due to "
						+ beaconEntryInvalidSecretResponse.asString());
		boolean isBeaconEntryInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, beaconEntryInvalidSecretResponse.asString());
		Assert.assertTrue(isBeaconEntryInvalidSecretSchemaValidated, "API v2 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidSecretResponseMsg = beaconEntryInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(beaconEntryInvalidSecretResponseMsg, invalidSignatureMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with invalid secret.");
		logger.info("API v2 Beacon Entry call is unsuccessful with invalid secret.");

		// Beacon Entry with invalid client
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with invalid client ==");
		logger.info("== Mobile API v2: Beacon Entry with invalid client ==");
		Response beaconEntryInvalidClientResponse = pageObj.endpoints().api2BeaconEntry(invalidValue,
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Entry call with invalid client due to "
						+ beaconEntryInvalidSecretResponse.asString());
		boolean isBeaconEntryInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, beaconEntryInvalidClientResponse.asString());
		Assert.assertTrue(isBeaconEntryInvalidClientSchemaValidated, "API v2 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidClientResponseMsg = beaconEntryInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(beaconEntryInvalidClientResponseMsg, invalidClientIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with invalid client.");
		logger.info("API v2 Beacon Entry call is unsuccessful with invalid client.");

		// Beacon Entry with invalid access_token
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with invalid access_token ==");
		logger.info("== Mobile API v2: Beacon Entry with invalid access_token ==");
		Response beaconEntryInvalidTokenResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Entry call with invalid access_token due to "
						+ beaconEntryInvalidTokenResponse.asString());
		boolean isBeaconEntryInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, beaconEntryInvalidTokenResponse.asString());
		Assert.assertTrue(isBeaconEntryInvalidTokenSchemaValidated, "API v2 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidTokenResponseMsg = beaconEntryInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(beaconEntryInvalidTokenResponseMsg, invalidAccessTokenMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with invalid access_token.");
		logger.info("API v2 Beacon Entry call is unsuccessful with invalid access_token.");

		// Beacon Entry with invalid beacon_major
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with invalid beacon_major ==");
		logger.info("== Mobile API v2: Beacon Entry with invalid beacon_major ==");
		Response beaconEntryInvalidBeaconMajorResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), authToken, "0", dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryInvalidBeaconMajorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v2 Beacon Entry call with invalid beacon_major due to "
						+ beaconEntryInvalidBeaconMajorResponse.asString());
		boolean isBeaconEntryInvalidBeaconMajorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, beaconEntryInvalidBeaconMajorResponse.asString());
		Assert.assertTrue(isBeaconEntryInvalidBeaconMajorSchemaValidated,
				"API v2 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidBeaconMajorResponseMsg = beaconEntryInvalidBeaconMajorResponse.jsonPath()
				.get("errors.base[0]").toString();
		Assert.assertEquals(beaconEntryInvalidBeaconMajorResponseMsg, invalidBeaconIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with invalid beacon_major.");
		logger.info("API v2 Beacon Entry call is unsuccessful with invalid beacon_major.");

		// Beacon Entry with invalid beacon_minor
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with invalid beacon_minor ==");
		logger.info("== Mobile API v2: Beacon Entry with invalid beacon_minor ==");
		Response beaconEntryInvalidBeaconMinorResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), "0");
		Assert.assertEquals(beaconEntryInvalidBeaconMinorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v2 Beacon Entry call with invalid beacon_minor due to "
						+ beaconEntryInvalidBeaconMinorResponse.asString());
		boolean isBeaconEntryInvalidBeaconMinorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, beaconEntryInvalidBeaconMinorResponse.asString());
		Assert.assertTrue(isBeaconEntryInvalidBeaconMinorSchemaValidated,
				"API v2 Beacon Entry Schema Validation failed");
		String beaconEntryInvalidBeaconMinorResponseMsg = beaconEntryInvalidBeaconMinorResponse.jsonPath()
				.get("errors.base[0]").toString();
		Assert.assertEquals(beaconEntryInvalidBeaconMinorResponseMsg, invalidBeaconIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with invalid beacon_minor.");
		logger.info("API v2 Beacon Entry call is unsuccessful with invalid beacon_minor.");

		// Beacon Entry with missing client
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with missing client ==");
		logger.info("== Mobile API v2: Beacon Entry with missing client ==");
		Response beaconEntryMissingClientResponse = pageObj.endpoints().api2BeaconEntry("", dataSet.get("secret"),
				authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Entry call with missing client due to "
						+ beaconEntryMissingClientResponse.asString());
		boolean isBeaconEntryMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingClientSchema, beaconEntryMissingClientResponse.asString());
		Assert.assertTrue(isBeaconEntryMissingClientSchemaValidated, "API v2 Beacon Entry Schema Validation failed");
		String beaconEntryMissingClientResponseMsg = beaconEntryMissingClientResponse.jsonPath().get("errors.client")
				.toString();
		Assert.assertEquals(beaconEntryMissingClientResponseMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with missing client.");
		logger.info("API v2 Beacon Entry call is unsuccessful with missing client.");

		// Beacon Entry with missing access_token
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with missing access_token ==");
		logger.info("== Mobile API v2: Beacon Entry with missing access_token ==");
		Response beaconEntryMissingTokenResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), "", dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryMissingTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Entry call with missing access_token due to "
						+ beaconEntryMissingTokenResponse.asString());
		boolean isBeaconEntryMissingTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, beaconEntryMissingTokenResponse.asString());
		Assert.assertTrue(isBeaconEntryMissingTokenSchemaValidated, "API v2 Beacon Entry Schema Validation failed");
		String beaconEntryMissingTokenResponseMsg = beaconEntryMissingTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(beaconEntryMissingTokenResponseMsg, invalidAccessTokenMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with missing access_token.");
		logger.info("API v2 Beacon Entry call is unsuccessful with missing access_token.");

		// Beacon Entry with missing beacon_major
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with missing beacon_major ==");
		logger.info("== Mobile API v2: Beacon Entry with missing beacon_major ==");
		Response beaconEntryMissingBeaconMajorResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), authToken, "\"\"", dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryMissingBeaconMajorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Entry call with missing beacon_major due to "
						+ beaconEntryMissingBeaconMajorResponse.asString());
		boolean isBeaconEntryMissingBeaconMajorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BeaconMajorErrorSchema, beaconEntryMissingBeaconMajorResponse.asString());
		Assert.assertTrue(isBeaconEntryMissingBeaconMajorSchemaValidated,
				"API v2 Beacon Entry Schema Validation failed");
		String beaconEntryMissingBeaconMajorResponseMsg = beaconEntryMissingBeaconMajorResponse.jsonPath()
				.get("errors.beacon_major").toString();
		Assert.assertEquals(beaconEntryMissingBeaconMajorResponseMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with missing beacon_major.");
		logger.info("API v2 Beacon Entry call is unsuccessful with missing beacon_major.");

		// Beacon Entry with missing beacon_minor
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Entry with missing beacon_minor ==");
		logger.info("== Mobile API v2: Beacon Entry with missing beacon_minor ==");
		Response beaconEntryMissingBeaconMinorResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), "\"\"");
		Assert.assertEquals(beaconEntryMissingBeaconMinorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Entry call with missing beacon_minor due to "
						+ beaconEntryMissingBeaconMinorResponse.asString());
		boolean isBeaconEntryMissingBeaconMinorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BeaconMinorErrorSchema, beaconEntryMissingBeaconMinorResponse.asString());
		Assert.assertTrue(isBeaconEntryMissingBeaconMinorSchemaValidated,
				"API v2 Beacon Entry Schema Validation failed");
		String beaconEntryMissingBeaconMinorResMsg = beaconEntryMissingBeaconMinorResponse.jsonPath()
				.get("errors.beacon_minor").toString();
		Assert.assertEquals(beaconEntryMissingBeaconMinorResMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Entry call is unsuccessful with missing beacon_minor.");
		logger.info("API v2 Beacon Entry call is unsuccessful with missing beacon_minor.");

		// Beacon Exit with invalid secret
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with invalid secret ==");
		logger.info("== Mobile API v2: Beacon Exit with invalid secret ==");
		Response beaconExitInvalidSecretResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				invalidValue, authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Beacon Exit call with invalid secret due to "
						+ beaconExitInvalidSecretResponse.asString());
		boolean isBeaconExitInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, beaconExitInvalidSecretResponse.asString());
		Assert.assertTrue(isBeaconExitInvalidSecretSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitInvalidSecretResponseMsg = beaconExitInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(beaconExitInvalidSecretResponseMsg, invalidSignatureMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with invalid secret.");
		logger.info("API v2 Beacon Exit call is unsuccessful with invalid secret.");

		// Beacon Exit with invalid client
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with invalid client ==");
		logger.info("== Mobile API v2: Beacon Exit with invalid client ==");
		Response beaconExitInvalidClientResponse = pageObj.endpoints().api2BeaconExit(invalidValue,
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Exit call with invalid client due to "
						+ beaconExitInvalidClientResponse.asString());
		boolean isBeaconExitInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, beaconExitInvalidClientResponse.asString());
		Assert.assertTrue(isBeaconExitInvalidClientSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitInvalidClientResponseMsg = beaconExitInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(beaconExitInvalidClientResponseMsg, invalidClientIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with invalid client.");
		logger.info("API v2 Beacon Exit call is unsuccessful with invalid client.");

		// Beacon Exit with invalid access_token
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with invalid access_token ==");
		logger.info("== Mobile API v2: Beacon Exit with invalid access_token ==");
		Response beaconExitInvalidTokenResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), invalidValue, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Exit call with invalid access_token due to "
						+ beaconExitInvalidTokenResponse.asString());
		boolean isBeaconExitInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, beaconExitInvalidTokenResponse.asString());
		Assert.assertTrue(isBeaconExitInvalidTokenSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitInvalidTokenResponseMsg = beaconExitInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(beaconExitInvalidTokenResponseMsg, invalidAccessTokenMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with invalid access_token.");
		logger.info("API v2 Beacon Exit call is unsuccessful with invalid access_token.");

		// Beacon Exit with invalid beacon_major
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with invalid beacon_major ==");
		logger.info("== Mobile API v2: Beacon Exit with invalid beacon_major ==");
		Response beaconExitInvalidBeaconMajorResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), authToken, "0", dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitInvalidBeaconMajorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v2 Beacon Exit call with invalid beacon_major due to "
						+ beaconExitInvalidBeaconMajorResponse.asString());
		boolean isBeaconExitInvalidBeaconMajorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, beaconExitInvalidBeaconMajorResponse.asString());
		Assert.assertTrue(isBeaconExitInvalidBeaconMajorSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitInvalidBeaconMajorResponseMsg = beaconExitInvalidBeaconMajorResponse.jsonPath()
				.get("errors.base[0]").toString();
		Assert.assertEquals(beaconExitInvalidBeaconMajorResponseMsg, invalidBeaconIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with invalid beacon_major.");
		logger.info("API v2 Beacon Exit call is unsuccessful with invalid beacon_major.");

		// Beacon Exit with invalid beacon_minor
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with invalid beacon_minor ==");
		logger.info("== Mobile API v2: Beacon Exit with invalid beacon_minor ==");
		Response beaconExitInvalidBeaconMinorResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), "0");
		Assert.assertEquals(beaconExitInvalidBeaconMinorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API v2 Beacon Exit call with invalid beacon_minor due to "
						+ beaconExitInvalidBeaconMinorResponse.asString());
		boolean isBeaconExitInvalidBeaconMinorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, beaconExitInvalidBeaconMinorResponse.asString());
		Assert.assertTrue(isBeaconExitInvalidBeaconMinorSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitInvalidBeaconMinorResponseMsg = beaconExitInvalidBeaconMinorResponse.jsonPath()
				.get("errors.base[0]").toString();
		Assert.assertEquals(beaconExitInvalidBeaconMinorResponseMsg, invalidBeaconIdMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with invalid beacon_minor.");
		logger.info("API v2 Beacon Exit call is unsuccessful with invalid beacon_minor.");

		// Beacon Exit with missing client
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with missing client ==");
		logger.info("== Mobile API v2: Beacon Exit with missing client ==");
		Response beaconExitMissingClientResponse = pageObj.endpoints().api2BeaconExit("", dataSet.get("secret"),
				authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Exit call with missing client due to "
						+ beaconExitMissingClientResponse.asString());
		boolean isBeaconExitMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingClientSchema, beaconExitMissingClientResponse.asString());
		Assert.assertTrue(isBeaconExitMissingClientSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitMissingClientResponseMsg = beaconExitMissingClientResponse.jsonPath().get("errors.client")
				.toString();
		Assert.assertEquals(beaconExitMissingClientResponseMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with missing client.");
		logger.info("API v2 Beacon Exit call is unsuccessful with missing client.");

		// Beacon Exit with missing access_token
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with missing access_token ==");
		logger.info("== Mobile API v2: Beacon Exit with missing access_token ==");
		Response beaconExitMissingTokenResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), "", dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitMissingTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Beacon Exit call with missing access_token due to "
						+ beaconExitMissingTokenResponse.asString());
		boolean isBeaconExitMissingTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, beaconExitMissingTokenResponse.asString());
		Assert.assertTrue(isBeaconExitMissingTokenSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitMissingTokenResponseMsg = beaconExitMissingTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(beaconExitMissingTokenResponseMsg, invalidAccessTokenMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with missing access_token.");
		logger.info("API v2 Beacon Exit call is unsuccessful with missing access_token.");

		// Beacon Exit with missing beacon_major
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with missing beacon_major ==");
		logger.info("== Mobile API v2: Beacon Exit with missing beacon_major ==");
		Response beaconExitMissingBeaconMajorResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), authToken, "\"\"", dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitMissingBeaconMajorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Exit call with missing beacon_major due to "
						+ beaconExitMissingBeaconMajorResponse.asString());
		boolean isBeaconExitMissingBeaconMajorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BeaconMajorErrorSchema, beaconExitMissingBeaconMajorResponse.asString());
		Assert.assertTrue(isBeaconExitMissingBeaconMajorSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitMissingBeaconMajorResponseMsg = beaconExitMissingBeaconMajorResponse.jsonPath()
				.get("errors.beacon_major").toString();
		Assert.assertEquals(beaconExitMissingBeaconMajorResponseMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with missing beacon_major.");
		logger.info("API v2 Beacon Exit call is unsuccessful with missing beacon_major.");

		// Beacon Exit with missing beacon_minor
		TestListeners.extentTest.get().info("== Mobile API v2: Beacon Exit with missing beacon_minor ==");
		logger.info("== Mobile API v2: Beacon Exit with missing beacon_minor ==");
		Response beaconExitMissingBeaconMinorResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("beaconMajor"), "\"\"");
		Assert.assertEquals(beaconExitMissingBeaconMinorResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API v2 Beacon Exit call with missing beacon_minor due to "
						+ beaconExitMissingBeaconMinorResponse.asString());
		boolean isBeaconExitMissingBeaconMinorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2BeaconMinorErrorSchema, beaconExitMissingBeaconMinorResponse.asString());
		Assert.assertTrue(isBeaconExitMissingBeaconMinorSchemaValidated, "API v2 Beacon Exit Schema Validation failed");
		String beaconExitMissingBeaconMinorResponseMsg = beaconExitMissingBeaconMinorResponse.jsonPath()
				.get("errors.beacon_minor").toString();
		Assert.assertEquals(beaconExitMissingBeaconMinorResponseMsg, missingParameterMsg, "Message did not match.");
		TestListeners.extentTest.get().pass("API v2 Beacon Exit call is unsuccessful with missing beacon_minor.");
		logger.info("API v2 Beacon Exit call is unsuccessful with missing beacon_minor.");

		// Request for User Account Deletion with invalid client
		TestListeners.extentTest.get()
				.info("== Mobile API v2: Request for User Account Deletion with invalid client ==");
		logger.info("== Mobile API v2: Request for User Account Deletion with invalid client ==");
		Response accountDeletionInvalidClientResponse = pageObj.endpoints().api2UserAccountDeletion(authToken,
				invalidValue, dataSet.get("secret"));
		Assert.assertEquals(accountDeletionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Request for User Account Deletion with invalid client");
		boolean isAccountDeletionInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, accountDeletionInvalidClientResponse.asString());
		Assert.assertTrue(isAccountDeletionInvalidClientSchemaValidated,
				"API v2 Request for User Account Deletion Schema Validation failed");
		String accountDeletionInvalidClientMsg = accountDeletionInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(accountDeletionInvalidClientMsg, invalidClientIdMsg, "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v2 Request for User Account Deletion call with invalid client is unsuccessful");
		logger.info("API v2 Request for User Account Deletion call with invalid client is unsuccessful");

		// Request for User Account Deletion with invalid secret
		TestListeners.extentTest.get()
				.info("== Mobile API v2: Request for User Account Deletion with invalid secret ==");
		logger.info("== Mobile API v2: Request for User Account Deletion with invalid secret ==");
		Response accountDeletionInvalidSecretResponse = pageObj.endpoints().api2UserAccountDeletion(authToken,
				dataSet.get("client"), invalidValue);
		Assert.assertEquals(accountDeletionInvalidSecretResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not match for API v2 Request for User Account Deletion with invalid secret");
		boolean isAccountDeletionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, accountDeletionInvalidSecretResponse.asString());
		Assert.assertTrue(isAccountDeletionInvalidSecretSchemaValidated,
				"API v2 Request for User Account Deletion Schema Validation failed");
		String accountDeletionInvalidSecretMsg = accountDeletionInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(accountDeletionInvalidSecretMsg, invalidSignatureMsg, "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v2 Request for User Account Deletion call with invalid secret is unsuccessful");
		logger.info("API v2 Request for User Account Deletion call with invalid secret is unsuccessful");

		// Request for User Account Deletion with invalid access token
		TestListeners.extentTest.get()
				.info("== Mobile API v2: Request for User Account Deletion with invalid access token ==");
		logger.info("== Mobile API v2: Request for User Account Deletion with invalid access token ==");
		Response accountDeletionInvalidTokenResponse = pageObj.endpoints().api2UserAccountDeletion(invalidValue,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(accountDeletionInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not match for API v2 Request for User Account Deletion with invalid access token");
		boolean isAccountDeletionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, accountDeletionInvalidTokenResponse.asString());
		Assert.assertTrue(isAccountDeletionInvalidTokenSchemaValidated,
				"API v2 Request for User Account Deletion Schema Validation failed");
		String accountDeletionInvalidTokenMsg = accountDeletionInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(accountDeletionInvalidTokenMsg, invalidAccessTokenMsg, "Message did not match");
		TestListeners.extentTest.get()
				.pass("API v2 Request for User Account Deletion call with invalid access token is unsuccessful");
		logger.info("API v2 Request for User Account Deletion call with invalid access token is unsuccessful");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
