package com.punchh.server.apiTest;

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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileApiTest2 {

	private static Logger logger = LogManager.getLogger(MobileApiTest2.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String mode_or_token;
	private String sTCName;
	private String env, run = "api";
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	// akansha jain
	@Test(description = "SQ-T6288 Validate login api by passing verification_mode key in the login API request and it should not return 500", groups = "api", priority = 8)
	public void SQ_T2688_Api2dUserLoginInvalidVerificationMode() {
		// Need :enable_params_handling_for_login: true in db preferences
		// Negative case: Existing User login using API2 with verification mode
		TestListeners.extentTest.get().info("== Mobile API2 login with invalid verification_mode ==");
		logger.info("== Mobile API2 login with invalid verification_mode ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		mode_or_token = "verification_mode";
		Response loginResponse3 = pageObj.endpoints().Api2Login(dataSet.get("validate_mode_or_token"), mode_or_token,
				userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 login");
		boolean isloginInvalidVerificationModeValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema3, loginResponse3.asString());
		Assert.assertTrue(isloginInvalidVerificationModeValidated,
				"API2 Existing User Login with verification mode validation failed");
		String loginInvalidVerificationModeMsg = loginResponse3.jsonPath().getString("errors.base[0]");
		Assert.assertNotNull(loginInvalidVerificationModeMsg, "Error message should not be null");
		Assert.assertEquals(loginInvalidVerificationModeMsg, "Incorrect information submitted. Please retry.");

		pageObj.utils().logit("pass", "Api2 user Login is unsuccessful because of invalid verification mode. Error: "
				+ loginInvalidVerificationModeMsg);

	}

	// akansha jain
	@Test(description = "SQ-T6289 Validate login api by passing firebase_token key in the login API request and it should not return 500.", groups = "api", priority = 9)
	public void SQ_T2689_Api2dUserLoginInvalidFirebaseToken() {
		// Need :enable_params_handling_for_login: true in db preferences
		// Negative case: Existing User login using API2 with verification mode
		TestListeners.extentTest.get().info("== Mobile API2 login with invalid firebase_token ==");
		logger.info("== Mobile API2 login with invalid firebase_token ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		mode_or_token = "firebase_token";
		Response loginResponse3 = pageObj.endpoints().Api2Login(dataSet.get("validate_mode_or_token"), mode_or_token,
				userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 login");
		boolean isloginInvalidFirebaseTokenValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorsObjectSchema3, loginResponse3.asString());
		Assert.assertTrue(isloginInvalidFirebaseTokenValidated,
				"API2 Existing User Login with firbase token validation failed");
		String loginInvalidFirebaseTokenMsg = loginResponse3.jsonPath().getString("errors.base[0]");
		Assert.assertNotNull(loginInvalidFirebaseTokenMsg, "Error message should not be null");
		Assert.assertEquals(loginInvalidFirebaseTokenMsg, "Incorrect information submitted. Please retry.");

		pageObj.utils().logit("pass", "Api2 user Login is unsuccessful because of invalid firebase token. Error: "
				+ loginInvalidFirebaseTokenMsg);
	}

	@Test(description = "SQ-T2357 Verify Api2 user signup login and logout", groups = "api", priority = 0)
	public void verify_Api2_UserSignUp_Login_Logout() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		boolean isUserSignUpSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, signUpResponse.asString());
		Assert.assertTrue(isUserSignUpSchemaValidated, "API v2 User Sign-up Schema Validation failed");

		// Validate signup response fields
		String signupToken = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(signupToken, "Signup token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		String signupEmail = signUpResponse.jsonPath().getString("user.email");
		Assert.assertEquals(signupEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");

		pageObj.utils().logit("pass",
				"Api2 user signup is successful. User ID: " + signupUserId + ", Email: " + signupEmail);
		utils.getAPIResponseTime(signUpResponse);

		// User login using API2 Signin
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 login");
		boolean isUserLoginSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, loginResponse.asString());
		Assert.assertTrue(isUserLoginSchemaValidated, "API v2 User Login Schema Validation failed");

		// Validate login response fields
		String token = loginResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(token, "Login token should not be null");
		Assert.assertFalse(token.isEmpty(), "Login token should not be empty");
		String loginUserId = loginResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(loginUserId, signupUserId, "User ID should match signup user ID");

		pageObj.utils().logit("pass",
				"Api2 user Login is successful. Token: " + token.substring(0, Math.min(10, token.length())) + "...");
		utils.getAPIResponseTime(loginResponse);

		// User logout using API2 Logout
		Response logoutResponse = pageObj.endpoints().Api2Logout(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(logoutResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 logout");

		pageObj.utils().logit("pass", "Api2 user Logout is successful");
		utils.getAPIResponseTime(logoutResponse);
	}

	@Test(description = "SQ-T2371 Verify fetch user info and update user profile; "
			+ "SQ-T4748 Verify API2 User Show", groups = "api", priority = 1)
	public void verify_FetchUserinfo_UpdateUserProfile() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		String user_id = signUpResponse.jsonPath().getString("user.user_id");
		String email = signUpResponse.jsonPath().getString("user.email");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		Assert.assertNotNull(token, "Token should not be null");
		Assert.assertNotNull(user_id, "User ID should not be null");

		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + user_id + ", Email: " + email);

		// User Show
		Response userShowResponse = pageObj.endpoints().api2UserShow(token, user_id, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userShowResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 user show");
		boolean isUserShowSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, userShowResponse.asString());
		Assert.assertTrue(isUserShowSchemaValidated, "API v2 User Show Schema Validation failed");

		String showUserId = userShowResponse.jsonPath().getString("user.user_id");
		String showEmail = userShowResponse.jsonPath().getString("user.email");
		Assert.assertEquals(showUserId, user_id, "User id did not match for API2 user show");
		Assert.assertEquals(showEmail, email, "User email did not match for API2 user show");

		pageObj.utils().logit("pass", "API2 user show call is successful. User ID: " + showUserId);

		// Fetch user information
		Response userInfoResponse = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user info");
		boolean isFetchUserInfoSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, userInfoResponse.asString());
		Assert.assertTrue(isFetchUserInfoSchemaValidated, "API v2 Fetch User Info Schema Validation failed");

		String fetchedUserId = userInfoResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(fetchedUserId, user_id, "Fetched User ID should match original");

		pageObj.utils().logit("pass", "Api2 fetch user information is successful. User ID: " + fetchedUserId);

		// Update user profile
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), token);
		Assert.assertEquals(updateUserInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 update user info");
		boolean isUpdateUserInfoSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, updateUserInfoResponse.asString());
		Assert.assertTrue(isUpdateUserInfoSchemaValidated, "API v2 Update User Info Schema Validation failed");

		String updatedUserId = updateUserInfoResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(updatedUserId, user_id, "Updated User ID should match original");

		pageObj.utils().logit("pass", "Api2 update user information is successful. User ID: " + updatedUserId);
	}

	@Test(description = "SQ-T2372 Verify create update and delete user relation", groups = "api", priority = 2)
	public void verify_Create_update_delete_User_Relation() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		String userId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		Assert.assertNotNull(token, "Token should not be null");

		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userId);

		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create user relation");
		boolean isCreateUserRelationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserRelationSchema, createUserRelationResponse.asString());
		Assert.assertTrue(isCreateUserRelationSchemaValidated, "API v2 Create User Relation Schema Validation failed");

		int id = createUserRelationResponse.jsonPath().getInt("id");
		Assert.assertTrue(id > 0, "User relation ID should be greater than 0");

		pageObj.utils().logit("pass", "Api2 Create user relation is successful. Relation ID: " + id);

		// Update user relation
		Response updateUserRelationResponse = pageObj.endpoints().Api2UpdateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token, id);
		Assert.assertEquals(updateUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 update user relation");
		boolean isUpdateUserRelationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserRelationSchema, updateUserRelationResponse.asString());
		Assert.assertTrue(isUpdateUserRelationSchemaValidated, "API v2 Update User Relation Schema Validation failed");

		int updatedId = updateUserRelationResponse.jsonPath().getInt("id");
		Assert.assertEquals(updatedId, id, "Updated relation ID should match original");

		pageObj.utils().logit("pass", "Api2 Update user relation is successful. Relation ID: " + updatedId);

		// Delete user relation
		Response deleteUserRelationResponse = pageObj.endpoints().Api2DeleteUserRelation(dataSet.get("client"),
				dataSet.get("secret"), token, Integer.toString(id));
		Assert.assertEquals(deleteUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 delete user relation");

		pageObj.utils().logit("pass", "Api2 Delete user relation is successful. Relation ID: " + id);
	}

	@Test(description = "SQ-T2373 Verify asynchronous user update and get user session token", groups = "api", priority = 3)
	public void verify_Asynchronous_User_Update_Get_User_Session_Token() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		String userId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		Assert.assertNotNull(token, "Token should not be null");

		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userId);

		// Asynchronous User Update
		Response updateUserInfoResponse = pageObj.endpoints().Api2AsynchronousUserUpdate(dataSet.get("client"),
				userEmail, dataSet.get("secret"), token);
		Assert.assertEquals(updateUserInfoResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for api2 asynchronous user update");

		pageObj.utils().logit("pass",
				"Api2 asynchronous user update is successful. Status: " + updateUserInfoResponse.getStatusCode());

		// Get User Session Token
		Response sessionTokenResponse = pageObj.endpoints().Api2UserSessionToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(sessionTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 get user session token");
		boolean isSessionTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSessionTokenSchema, sessionTokenResponse.asString());
		Assert.assertTrue(isSessionTokenSchemaValidated, "API v2 Get User Session Token Schema Validation failed");

		pageObj.utils().logit("pass",
				"Api2 get user session token is successful. Response: " + sessionTokenResponse.asString());

		// Send Verification Email
		Response verificationEmailResponse = pageObj.endpoints().Api2SendVreificationEmail(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(verificationEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 send verification email");

		pageObj.utils().logit("pass", "Api2 send verification email is successful");

	}

	@Test(description = "SQ-T2374 Verify forgot password and fetch user balance", groups = "api", priority = 4)
	public void verify_forgot_password_and_fetch_user_balance() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		String userID = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		Assert.assertNotNull(token, "Token should not be null");
		Assert.assertNotNull(userID, "User ID should not be null");

		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userID);

		// Forgot password
		Response forgotPasswordResponse = pageObj.endpoints().Api2ForgotPassword(dataSet.get("client"),
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 forgot password");

		pageObj.utils().logit("pass", "Api2 Forgot password is successful for email: " + userEmail);

		// Send reward redeemable to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		pageObj.utils().logit("pass",
				"Api2 Send reward redeemable to user is successful. Redeemable ID: " + dataSet.get("redeemable_id"));

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		boolean isUserBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchUserBalanceSchema, userBalanceResponse.asString());
		Assert.assertTrue(isUserBalanceSchemaValidated, "API v2 Fetch User Balance Schema Validation failed");

		pageObj.utils().logit("pass",
				"Api2 Fetch user balance is successful. Response: " + userBalanceResponse.asString());

		// Balance Timeline
		Response balanceTimelineResponse = pageObj.endpoints().Api2BalanceTimeline(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(balanceTimelineResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 balance timeline");
		boolean isBalanceTimelineSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authBalanceTimelineSchema, balanceTimelineResponse.asString());
		Assert.assertTrue(isBalanceTimelineSchemaValidated, "API v2 Balance Timeline Schema Validation failed");

		pageObj.utils().logit("pass", "Api2 Balance Timeline is successful");

		// Estimate Points Earning
		Response pointsEarningResponse = pageObj.endpoints().Api2EstimatePointsEarning(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(pointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 estimate points earning");
		boolean isEstimatePointsEarningSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authEstimateLoyaltyPointsEarningSchema, pointsEarningResponse.asString());
		Assert.assertTrue(isEstimatePointsEarningSchemaValidated,
				"API v2 Estimate Points Earning Schema Validation failed");

		pageObj.utils().logit("pass",
				"Api2 Estimate Points Earning is successful. Response: " + pointsEarningResponse.asString());
	}

	@Test(description = "SQ-T4954 Fetch Active Purchasable Subscription Plan")
	public void T4954_fetch_active_purchasable_subscription_plan() {
		Response fetchActivePurchasableSubscriptionPlan = pageObj.endpoints()
				.Api2FetchActivePurchasableSubscriptionPlans(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchActivePurchasableSubscriptionPlan.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isFetchActivePurchasableSubscriptionPlanSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authFetchActivePurchasableSubscriptionPlansSchema,
				fetchActivePurchasableSubscriptionPlan.asString());
		Assert.assertTrue(isFetchActivePurchasableSubscriptionPlanSchemaValidated,
				"API v2 Fetch Active Purchasable Subscription Plan Schema Validation failed");

		pageObj.utils().logit("pass", "Api2 Fetch Active purchasable Subscription Plan is successful. Response: "
				+ fetchActivePurchasableSubscriptionPlan.asString());
	}

	@Test(description = "SQ-T4763 Verify API2 Generate Single Scan Code; "
			+ "SQ-T4765 Verify Get Access Code (Redemptions 2.0)", groups = "api", priority = 5)
	public void api2generateSingleScanCode() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().getString("access_token.token");
		String firstName = signUpResponse.jsonPath().getString("user.first_name");
		String userId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api2 user signup");
		Assert.assertNotNull(token, "Token should not be null");

		pageObj.utils().logit("pass",
				"API2 User Signup is successful. User ID: " + userId + ", First Name: " + firstName);

		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("creditCardPaymentType"), dataSet.get("transaction_token"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SingleScanCodeSchema, singleScanCodeResponse.asString());
		Assert.assertTrue(isSingleScanCodeSchemaValidated, "API v2 Generate Single Scan Code Schema Validation failed");

		String singleScanCode = singleScanCodeResponse.jsonPath().getString("single_scan_code");
		Assert.assertNotNull(singleScanCode, "single_scan_code should not be null");
		Assert.assertFalse(singleScanCode.isEmpty(), "single_scan_code should not be empty");

		pageObj.utils().logit("pass",
				"API2 Generate Single Scan Code call is successful. Scan Code: " + singleScanCode);

		// API2 Purchase Gift Card
		String giftCardUuid = "";
		int counter = 0;
		while (counter < 20) {
			try {
				pageObj.utils().logit("info", "API hit count is: " + counter);
				utils.longwait(5000);
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("designId"), dataSet.get("amount"),
						dataSet.get("expDate"), firstName);
				giftCardUuid = purchaseGiftCardResponse.jsonPath().getString("uuid");
				if (giftCardUuid != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for api2 purchase gift card");
					pageObj.utils().logit("info", "Gift card uuid fetched successfully: " + giftCardUuid);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
		}

		// API2 Get Access Code (Redemptions 2.0)
		Response getAccessCodeResponse = pageObj.endpoints().api2GetAccessCode(token,
				dataSet.get("giftCardPaymentType"), giftCardUuid, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(getAccessCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 Get Access Code");
		boolean isGetAccessCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SingleScanCodeSchema, getAccessCodeResponse.asString());
		Assert.assertTrue(isGetAccessCodeSchemaValidated, "API v2 Get Access Code Schema Validation failed");

		String accessCode = getAccessCodeResponse.jsonPath().getString("single_scan_code");
		Assert.assertNotNull(accessCode, "Access code should not be null");
		Assert.assertFalse(accessCode.isEmpty(), "Access code should not be empty");

		pageObj.utils().logit("pass", "API2 Get Access Code call is successful. Access Code: " + accessCode
				+ ", Gift Card UUID: " + giftCardUuid);
	}

//	@Test(description = "SQ-T4927 Verify API2 Payment Cards", groups = "api", priority = 6) //it keeps failing due to heartland
	void api2PaymentCards() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String authToken = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api2 user signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// Generate Heartland token
		Response heartlandTokenResponse = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartlandPublicKey"));
		Assert.assertEquals(heartlandTokenResponse.statusCode(), 201,
				"Not able to generate single Token for heartland adapter");
		String heartlandToken = heartlandTokenResponse.jsonPath().getString("token_value");
		TestListeners.extentTest.get().pass("Heartland Single token is generated: " + heartlandToken);

		// Create Payment Card
		Response createPaymentCardResponse = pageObj.endpoints().api2CreatePaymentCard(authToken, heartlandToken,
				dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(createPaymentCardResponse.statusCode(), 200,
				"Status code 200 did not match for api2 Create Payment Card");
		String paymentCardUuid = createPaymentCardResponse.jsonPath().getString("uuid");
		TestListeners.extentTest.get().pass("Payment card is created with uuid: " + paymentCardUuid);

		// Update Payment Card
		Response updatePaymentCardResponse = pageObj.endpoints().api2UpdatePaymentCard(authToken, paymentCardUuid,
				dataSet.get("nicknameToUpdate"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardResponse.statusCode(), 200,
				"Status code 200 did not match for api2 Update Payment Card");
		String updatedNickname = updatePaymentCardResponse.jsonPath().getString("nickname");
		String preferredValue = updatePaymentCardResponse.jsonPath().getString("preferred");
		Assert.assertEquals(updatedNickname, dataSet.get("nicknameToUpdate"),
				"Nickname did not match for api2 Update Payment Card");
		Assert.assertEquals(preferredValue, dataSet.get("preferrenceToUpdate"),
				"Preferred value did not match for api2 Update Payment Card");
		TestListeners.extentTest.get().pass("Payment card is successfuly updated");

		// Fetch Payment Card
		Response fetchPaymentCardResponse = pageObj.endpoints().api2FetchPaymentCard(authToken,
				dataSet.get("adapterCode"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardResponse.statusCode(), 200,
				"Status code 200 did not match for api2 Fetch Payment Card");
		String paymentCardUuidActual = fetchPaymentCardResponse.jsonPath().get("[0].uuid").toString();
		Assert.assertEquals(paymentCardUuidActual, paymentCardUuid,
				"Payment card uuid did not match for api2 Fetch Payment Card");
		TestListeners.extentTest.get().pass("Payment card is successfuly fetched");

		// Delete Payment Card
		Response deletePaymentCardResponse = pageObj.endpoints().api2DeletePaymentCard(authToken, paymentCardUuid,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardResponse.statusCode(), 200,
				"Status code 200 did not match for api2 Delete Payment Card");
		String message = deletePaymentCardResponse.jsonPath().getString("message");
		String status = deletePaymentCardResponse.jsonPath().getString("status");
		Assert.assertEquals(message, dataSet.get("deletionSuccessMessage"),
				"Payment card deletion message did not match for api2 Delete Payment Card");
		Assert.assertEquals(status, dataSet.get("deletionSuccessStatus"),
				"Payment card deletion status did not match for api2 Delete Payment Card");
		TestListeners.extentTest.get().pass("Payment card is successfuly deleted");
	}

	@Test(description = "SQ-T4952 Mark Offers As Read", priority = 7)
	public void T4952_mark_offers_as_read() throws InterruptedException {
		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String communicableEmail = signUpResponse.jsonPath().getString("user.communicable_email");
		Assert.assertEquals(communicableEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");

		String token = signUpResponse.jsonPath().getString("access_token.token");
		String userID = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(token, "Token should not be null");
		Assert.assertNotNull(userID, "User ID should not be null");

		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"50", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		pageObj.utils().logit("pass", "Send redeemable to the user successfully. Redeemable ID: " + redeemableID);

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);
		Assert.assertNotNull(rewardId, "Reward ID should not be null");

		pageObj.utils().logit("pass", "Reward id " + rewardId + " is generated successfully");

		// mark offers as read Api
		Response offerResponse = pageObj.endpoints().Api2markOffersAsRead(token, dataSet.get("client"),
				dataSet.get("secret"), rewardId, dataSet.get("event_type"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 mark offers as read");

		pageObj.utils().logit("pass", "Api2 mark offers as read is successful. Reward ID: " + rewardId);
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}