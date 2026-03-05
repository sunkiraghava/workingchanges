package com.punchh.server.apiTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AuthAPITest {
	static Logger logger = LogManager.getLogger(AuthAPITest.class);
	public WebDriver driver;
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
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		apiUtils = new ApiUtils();
		utils = new Utilities();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		apiUtils = new ApiUtils();
	}

	@Test(description = "SQ-T2360 Auth Signup login update user info and verify user info", groups = "api", priority = 0)
	public void verifySSOSignUpUserdetails() {

		// User Sign-up
		pageObj.utils().logit("info", "== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		boolean isAuthSignUpSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authUserSignUpLogInUpdateInfoSchema, signUpResponse.asString());
		Assert.assertTrue(isAuthSignUpSchemaValidated, "Auth API User Sign-up Schema Validation failed");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		Assert.assertFalse(authToken.isEmpty(), "Authentication token should not be empty");
		String accessToken = signUpResponse.jsonPath().getString("access_token");
		Assert.assertNotNull(accessToken, "Access token should not be null");
		String fName = signUpResponse.jsonPath().getString("first_name");
		String lName = signUpResponse.jsonPath().getString("last_name");
		Assert.assertNotNull(fName, "First name should not be null");
		Assert.assertNotNull(lName, "Last name should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		String signupEmail = signUpResponse.jsonPath().getString("email");
		Assert.assertEquals(signupEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");
		pageObj.utils().logit("pass", "Auth API User Sign-up call is successful. User ID: " + signupUserId + ", Email: " + signupEmail);

		// User Log-in
		pageObj.utils().logit("info", "== Auth API User Log-in ==");
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed - Auth API user login");
		boolean isAuthLogInSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authUserSignUpLogInUpdateInfoSchema, loginResponse.asString());
		Assert.assertTrue(isAuthLogInSchemaValidated, "Auth API User Log-in Schema Validation failed");
		String loginUserId = loginResponse.jsonPath().getString("user_id");
		Assert.assertEquals(loginUserId, signupUserId, "Login user ID should match signup user ID");
		String loginAuthToken = loginResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(loginAuthToken, "Login authentication token should not be null");
		String updateEmail = "updated" + userEmail;
		String updateFName = "updated" + fName;
		String updateLName = "updated" + lName;
		pageObj.utils().logit("pass", "Auth API User Log-in call is successful. User ID: " + loginUserId);

		// Update User Info
		pageObj.utils().logit("info", "== Auth API Update User Info ==");
		Response updateUserResponse = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken, updateEmail, updateFName, updateLName);
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed - to update user info");
		boolean isAuthUpdateInfoSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authUserSignUpLogInUpdateInfoSchema, updateUserResponse.asString());
		Assert.assertTrue(isAuthUpdateInfoSchemaValidated, "Auth API Update User Info Schema Validation failed");
		String updatedEmailResponse = updateUserResponse.jsonPath().getString("email");
		Assert.assertEquals(updatedEmailResponse.toLowerCase(), updateEmail.toLowerCase(), "Updated email should match in response");
		String updatedFNameResponse = updateUserResponse.jsonPath().getString("first_name");
		Assert.assertEquals(updatedFNameResponse.toLowerCase(), updateFName.toLowerCase(), "Updated first name should match");
		String updatedLNameResponse = updateUserResponse.jsonPath().getString("last_name");
		Assert.assertEquals(updatedLNameResponse.toLowerCase(), updateLName.toLowerCase(), "Updated last name should match");
		pageObj.utils().logit("pass", "Auth API Update User Info call is successful. Updated Email: " + updatedEmailResponse);

		// Verify updated user info
		pageObj.utils().logit("info", "== Auth API Fetch User Info ==");
		Response fetchUserResponse = pageObj.endpoints().authApiFetchUserInfo(dataSet.get("client"),
				dataSet.get("secret"), authToken);
		Assert.assertEquals(fetchUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth API Fetch User Info");
		boolean isAuthFetchInfoSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authUserSignUpLogInUpdateInfoSchema, fetchUserResponse.asString());
		Assert.assertTrue(isAuthFetchInfoSchemaValidated, "Auth API Fetch User Info Schema Validation failed");
		Assert.assertEquals(fetchUserResponse.jsonPath().getString("email").toLowerCase(),
				updateEmail.toLowerCase(), "Failed to verify updated email");
		Assert.assertEquals(fetchUserResponse.jsonPath().getString("first_name").toLowerCase(),
				updateFName.toLowerCase(), "Failed to verify updated firstname");
		Assert.assertEquals(fetchUserResponse.jsonPath().getString("last_name").toLowerCase(),
				updateLName.toLowerCase(), "Failed to verify updated lastname");
		String fetchedUserId = fetchUserResponse.jsonPath().getString("user_id");
		Assert.assertEquals(fetchedUserId, signupUserId, "Fetched user ID should match signup user ID");
		pageObj.utils().logit("pass", "Auth API Fetch User Info call is successful. User ID: " + fetchedUserId + ", Email: " + fetchUserResponse.jsonPath().getString("email"));
	}

	@Test(description = "SQ-T2361 SSO Checkin, get account history, account balance, user balance and forgot password", groups = "api", priority = 1)
	public void verifySSOSignUpUserCheckinAndBalance() {
		// User Sign-up
		pageObj.utils().logit("info", "== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		Assert.assertFalse(authToken.isEmpty(), "Authentication token should not be empty");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Auth API User Sign-up call is successful. User ID: " + signupUserId);

		// Checkin via auth API
		pageObj.utils().logit("info", "== Auth API Checkin ==");
		String amount = "110.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String checkinFirstName = checkinResponse.jsonPath().getString("first_name");
		Assert.assertNotNull(checkinFirstName, "First name should not be null in checkin response");
		int checkinCount = checkinResponse.jsonPath().getInt("checkins");
		Assert.assertTrue(checkinCount >= 1, "Checkin count should be at least 1");
		long checkinId = checkinResponse.jsonPath().getLong("checkin.checkin_id");
		Assert.assertTrue(checkinId > 0, "Checkin ID should be greater than 0");
		String barCode = checkinResponse.jsonPath().getString("checkin.bar_code");
		Assert.assertNotNull(barCode, "Bar code should not be null");
		int pointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertTrue(pointsEarned > 0, "Points earned should be greater than 0");
		pageObj.utils().logit("pass", "Auth API Checkin call is successful. Checkin ID: " + checkinId + ", Points Earned: " + pointsEarned);

		// Get account history
		pageObj.utils().logit("info", "== Auth API Get Account History ==");
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		boolean isAuthAccountHistorySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authGetAccountHistorySchema, accountHistoryResponse.asString());
		Assert.assertTrue(isAuthAccountHistorySchemaValidated, "Auth API Get Account History Schema Validation failed");
		int historyCount = accountHistoryResponse.jsonPath().getList("$").size();
		Assert.assertTrue(historyCount > 0, "Account history should have at least one entry");
		String firstEventName = accountHistoryResponse.jsonPath().getString("[0].event_name");
		Assert.assertNotNull(firstEventName, "First event name should not be null");
		pageObj.utils().logit("pass", "Auth API Get Account History call is successful. History Count: " + historyCount + ", First Event: " + firstEventName);

		// Fetch User Balance
		pageObj.utils().logit("info", "== Auth API Fetch User Balance ==");
		Response userBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		boolean isAuthUserBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchUserBalanceSchema, userBalanceResponse.asString());
		Assert.assertTrue(isAuthUserBalanceSchemaValidated, "Auth API Fetch User Balance Schema Validation failed");
		Assert.assertNotNull(userBalanceResponse.jsonPath().get("account_balance"), "Account balance should not be null");
		double bankedCurrency = userBalanceResponse.jsonPath().getDouble("account_balance.banked_currency");
		Assert.assertTrue(bankedCurrency >= 0, "Banked currency should be non-negative");
		double unbankingPoints = userBalanceResponse.jsonPath().getDouble("account_balance.unbanked_points");
		Assert.assertTrue(unbankingPoints >= 0, "Unbanked points should be non-negative");
		String membershipLevel = userBalanceResponse.jsonPath().getString("account_balance.current_membership_level_name");
		Assert.assertNotNull(membershipLevel, "Membership level name should not be null");
		pageObj.utils().logit("pass", "Auth API Fetch User Balance call is successful. Banked Currency: $" + bankedCurrency + ", Unbanked Points: " + unbankingPoints + ", Level: " + membershipLevel);

		// Forgot password
		pageObj.utils().logit("info", "== Auth API Forgot Password ==");
		Response forgotPasswordResponse = pageObj.endpoints().authApiForgotPassword(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API forgot response response");
		pageObj.utils().logit("pass", "Auth API Forgot Password call is successful for email: " + userEmail);

	}

	@Test(description = "SQ-T2362 verifying SSO Void Redemption", groups = "api", priority = 2)
	public void verifySSOCheckinAndVoidCheckin() {
		pageObj.utils().logit("info", "== Auth API User Sign-up for Void Redemption ==");
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Auth API user signup successful. User ID: " + signupUserId);
		
		// Checkin via auth API
		pageObj.utils().logit("info", "== Online Order Checkin ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		long checkinId = checkinResponse.jsonPath().getLong("checkin.checkin_id");
		Assert.assertTrue(checkinId > 0, "Checkin ID should be greater than 0");
		int pointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertTrue(pointsEarned > 0, "Points earned should be greater than 0");
		pageObj.utils().logit("pass", "Online order checkin successful. Checkin ID: " + checkinId + ", Points Earned: " + pointsEarned);
		
		// POS redemption API
		pageObj.utils().logit("info", "== POS Redemption ==");
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		int redemptionId = respo.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");
		String redemptionCode = respo.jsonPath().getString("redemption_code");
		Assert.assertNotNull(redemptionCode, "Redemption code should not be null");
		String redemptionStatus = respo.jsonPath().getString("status");
		Assert.assertNotNull(redemptionStatus, "Redemption status should not be null");
		Assert.assertTrue(redemptionStatus.contains("Please HONOR it"), "Status should contain honor message");
		double redemptionAmount = respo.jsonPath().getDouble("redemption_amount");
		Assert.assertTrue(redemptionAmount > 0, "Redemption amount should be greater than 0");
		pageObj.utils().logit("pass", "POS Redemption successful. Redemption ID: " + redemptionId + ", Code: " + redemptionCode + ", Amount: $" + redemptionAmount);

		// Auth void redemption
		pageObj.utils().logit("info", "== Auth Void Redemption ==");
		Response voidRedemptionResponse = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), Integer.toString(redemptionId));
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for Auth void redemption");
		pageObj.utils().logit("pass", "Auth Void Redemption successful for Redemption ID: " + redemptionId);
		
		// List Available rewards of a given user
		pageObj.utils().logit("info", "== Auth List Available Rewards ==");
		Response listAvailableRewardsResponse = pageObj.endpoints().authListAvailableRewards(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(listAvailableRewardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify list of avialable rewards response");
		pageObj.utils().logit("pass", "Auth List Available rewards successful. Response: " + listAvailableRewardsResponse.asString());
	}

	@Test(description = "SQ-T2363 SSO verifying Grant Loyalty checkin Against receipt", groups = "api", priority = 3)
	public void verifyAuthCheckinAgainstReciept() {
		// User Sign-up
		pageObj.utils().logit("info", "== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Auth API User Sign-up call is successful. User ID: " + signupUserId);

		// Checkin Against Reciept Schema
		pageObj.utils().logit("info", "== Auth API Checkin Against Reciept ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken, amount,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthCheckinAgainstRecieptSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authCheckinAgainstRecieptSchema, checkinResponse.asString());
		Assert.assertTrue(isAuthCheckinAgainstRecieptSchemaValidated,
				"Auth API Checkin Against Reciept Schema Validation failed");
		String firstName = checkinResponse.jsonPath().getString("first_name");
		Assert.assertNotNull(firstName, "First name should not be null");
		String lastName = checkinResponse.jsonPath().getString("last_name");
		Assert.assertNotNull(lastName, "Last name should not be null");
		long checkinId = checkinResponse.jsonPath().getLong("checkin.checkin_id");
		Assert.assertTrue(checkinId > 0, "Checkin ID should be greater than 0");
		String barCode = checkinResponse.jsonPath().getString("checkin.bar_code");
		Assert.assertNotNull(barCode, "Bar code should not be null");
		Assert.assertFalse(barCode.isEmpty(), "Bar code should not be empty");
		String externalUid = checkinResponse.jsonPath().getString("checkin.external_uid");
		Assert.assertNotNull(externalUid, "External UID should not be null");
		int pointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertTrue(
				pointsEarned == 630 || pointsEarned == 210,
				"Points earned should be either 630 or 210, actual: " + pointsEarned);
		int checkinCount = checkinResponse.jsonPath().getInt("checkins");
		Assert.assertEquals(checkinCount, 1, "Checkin count should be 1");
		pageObj.utils().logit("pass", "Auth API Checkin Against Reciept call is successful. Checkin ID: " + checkinId + ", Points Earned: " + pointsEarned + ", Bar Code: " + barCode);
	}

	@Test(description = "SQ-T2365 verifying auth online Bank Currency redemption", groups = "api", priority = 4)
	public void verifyAuthOnlineBankCurrencyRedemption() {
		// User Sign-up
		pageObj.utils().logit("info", "== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Auth API User Sign-up call is successful. User ID: " + signupUserId);

		// Checkin via auth API
		pageObj.utils().logit("info", "== Auth API Checkin ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		long checkinId = checkinResponse.jsonPath().getLong("checkin.checkin_id");
		Assert.assertTrue(checkinId > 0, "Checkin ID should be greater than 0");
		int pointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertTrue(pointsEarned > 0, "Points earned should be greater than 0");
		pageObj.utils().logit("pass", "Auth API Checkin call is successful. Checkin ID: " + checkinId + ", Points Earned: " + pointsEarned);

		// Create Online Redemption
		pageObj.utils().logit("info", "== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthOnlineRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authOnlineRedemptionSchema, resp.asString());
		Assert.assertTrue(isAuthOnlineRedemptionSchemaValidated,
				"Auth API Create Online Redemption Schema Validation failed");
		String redemptionStatus = resp.jsonPath().getString("status");
		Assert.assertNotNull(redemptionStatus, "Redemption status should not be null");
		Assert.assertTrue(redemptionStatus.contains("Please HONOR it."), "Status should contain honor message");
		double redemptionAmount = resp.jsonPath().getDouble("redemption_amount");
		Assert.assertTrue(redemptionAmount > 0, "Redemption amount should be greater than 0");
		int redemptionId = resp.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");
		String redemptionCode = resp.jsonPath().getString("redemption_code");
		Assert.assertNotNull(redemptionCode, "Redemption code should not be null");
		String category = resp.jsonPath().getString("category");
		Assert.assertEquals(category, "redeemable", "Category should be redeemable");
		pageObj.utils().logit("pass", "Auth API Create Online Redemption call is successful. Redemption ID: " + redemptionId + ", Code: " + redemptionCode + ", Amount: $" + redemptionAmount);
	}

	@Test(description = "SQ-T2366 verifying auth online Card Based redemption", groups = "api", priority = 5)
	// user creation using auth signup api
	public void verifyAuthOnlineCardBasedRedemption() {
		pageObj.utils().logit("info", "== Verifying auth online Card Based redemption ==");
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Auth API user signup successful. User ID: " + signupUserId);
		
		// Checkin via auth API - Multiple checkins
		pageObj.utils().logit("info", "== Performing multiple checkins ==");
		String amount = "50";
		Response checkinResponse = null;
		int numberOfVisits = Integer.parseInt(dataSet.get("numberOfVisit"));
		for (int i = 0; i < numberOfVisits; i++) {
			Utilities.longWait(1000);
			checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Checkin " + (i + 1) + " failed");
		}
		int finalCheckinCount = checkinResponse.jsonPath().getInt("checkins");
		Assert.assertEquals(finalCheckinCount, 7, "Final checkin count should be 7");
		long lastCheckinId = checkinResponse.jsonPath().getLong("checkin.checkin_id");
		Assert.assertTrue(lastCheckinId > 0, "Last checkin ID should be greater than 0");
		int lastPointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertTrue(lastPointsEarned > 0, "Last points earned should be greater than 0");
		pageObj.utils().logit("pass", "Multiple checkins successful. Total Checkins: " + finalCheckinCount + ", Last Checkin ID: " + lastCheckinId);

		// Auth_redemption_online_order
		pageObj.utils().logit("info", "== Auth Online Card Based Redemption ==");
		Response resp = pageObj.endpoints().authOnlineCardBasedRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for card based redemption api");
		String redemptionStatus = resp.jsonPath().getString("status");
		Assert.assertNotNull(redemptionStatus, "Redemption status should not be null");
		Assert.assertTrue(redemptionStatus.contains("Please HONOR it."), "Status should contain honor message");
		double redemptionAmount = resp.jsonPath().getDouble("redemption_amount");
		Assert.assertTrue(redemptionAmount > 0, "Redemption amount should be greater than 0");
		int redemptionId = resp.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");
		String redemptionCode = resp.jsonPath().getString("redemption_code");
		Assert.assertNotNull(redemptionCode, "Redemption code should not be null");
		String category = resp.jsonPath().getString("category");
		Assert.assertEquals(category, "redeemable", "Category should be redeemable");
		pageObj.utils().logit("pass", "Auth online Card Based Redemption successful. Redemption ID: " + redemptionId + ", Code: " + redemptionCode + ", Amount: $" + redemptionAmount);
	}

	@Test(description = "Verify Auth API:- SQ-T5334: Ordering Meta; SQ-T5336: Subscription Meta; "
			+ "SQ-T5339: Cancel Subscription; SQ-T5548: Fetch Active Purchasable Subscription Plans", groups = "api")
	public void verifyAuthMetaApi() {

		// Auth API User Sign-up
		pageObj.utils().logit("info", "== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Auth API User Sign-up");
		String accessToken = signUpResponse.jsonPath().getString("access_token");
		Assert.assertNotNull(accessToken, "Access token should not be null");
		Assert.assertFalse(accessToken.isEmpty(), "Access token should not be empty");
		String authToken = signUpResponse.jsonPath().getString("authentication_token");
		Assert.assertNotNull(authToken, "Authentication token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		String signupEmail = signUpResponse.jsonPath().getString("email");
		Assert.assertEquals(signupEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");
		pageObj.utils().logit("pass", "Auth API User Sign-up call is successful. User ID: " + signupUserId + ", Email: " + signupEmail);

		// Auth API Ordering Meta
		pageObj.utils().logit("info", "== Auth API Ordering Meta ==");
		Response orderingMetaResponse = pageObj.endpoints().authApiOrderingMeta("base_redeemable",
				dataSet.get("client"), dataSet.get("secret"), accessToken);
		Assert.assertEquals(orderingMetaResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth API Ordering Meta");
		boolean isAuthOrderingMetaSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authOrderingMetaSchema, orderingMetaResponse.asString());
		Assert.assertTrue(isAuthOrderingMetaSchemaValidated, "Auth API Ordering Meta Schema Validation failed");
		String baseRedeemableName = orderingMetaResponse.jsonPath().getString("base_redeemable_name");
		Assert.assertNotNull(baseRedeemableName, "Base redeemable name should not be null");
		Assert.assertEquals(baseRedeemableName, "Base Redeemable", "Name did not match.");
		String baseRedeemableDescription = orderingMetaResponse.jsonPath().getString("base_redeemable_description");
		Assert.assertNotNull(baseRedeemableDescription, "Base redeemable description should not be null");
		String baseRedeemableImage = orderingMetaResponse.jsonPath().getString("base_redeemable_image");
		Assert.assertNotNull(baseRedeemableImage, "Base redeemable image should not be null");
		pageObj.utils().logit("pass", "Auth API Ordering Meta call is successful. Base Redeemable Name: " + baseRedeemableName);

		// Auth API Subscription Meta
		pageObj.utils().logit("info", "== Auth API Subscription Meta ==");
		Response subscriptionMetaResponse = pageObj.endpoints().authSubscriptionMeta(dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(subscriptionMetaResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth API Subscription Meta");
		boolean isAuthSubscriptionMetaSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authSubscriptionMetaSchema, subscriptionMetaResponse.asString());
		Assert.assertTrue(isAuthSubscriptionMetaSchemaValidated, "Auth API Subscription Meta Schema Validation failed");
		Assert.assertNotNull(subscriptionMetaResponse.jsonPath().get("subscription_cancellation_reasons"), 
				"Subscription cancellation reasons should not be null");
		String cancellationReason = utils.getJsonReponseKeyValueFromJsonArray(subscriptionMetaResponse,
				"subscription_cancellation_reasons", "cancellation_reason", "expensive");
		String cancellationReasonId = utils.getJsonReponseKeyValueFromJsonArray(subscriptionMetaResponse,
				"subscription_cancellation_reasons", "cancellation_reason_id", "38");
		Assert.assertEquals(cancellationReason, "expensive", "Reason did not match.");
		Assert.assertNotNull(cancellationReasonId, "Cancellation reason ID should not be null");
		pageObj.utils().logit("pass", "Auth API Subscription Meta call is successful. Cancellation Reason: " + cancellationReason + ", Reason ID: " + cancellationReasonId);

		// Auth API Fetch Active Purchasable Subscription Plans
		pageObj.utils().logit("info", "== Auth API Fetch Active Purchasable Subscription Plans ==");
		Response activePurchasableSubscriptionPlansResponse = pageObj.endpoints()
				.authFetchActivePurchasableSubscriptionPlan(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(activePurchasableSubscriptionPlansResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isActivePurchasableSubscriptionPlansSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authFetchActivePurchasableSubscriptionPlansSchema,
				activePurchasableSubscriptionPlansResponse.asString());
		Assert.assertTrue(isActivePurchasableSubscriptionPlansSchemaValidated,
				"Auth API Fetch Active Purchasable Subscription Plans Schema Validation failed");
		int plansCount = activePurchasableSubscriptionPlansResponse.jsonPath().getList("$").size();
		Assert.assertTrue(plansCount > 0, "At least one subscription plan should be available");
		String subscriptonPlanId = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(
				activePurchasableSubscriptionPlansResponse, "plan_id", "2222");
		Assert.assertEquals(subscriptonPlanId, dataSet.get("existingSubscriptionPlanID"));
		pageObj.utils().logit("pass", "Auth API Fetch Active Purchasable Subscription Plans call is successful. Plans Count: " + plansCount);

		// Auth API Purchase Subscription
		pageObj.utils().logit("info", "== Auth API Purchase Subscription ==");
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		Response purchaseSubscriptionResponse = pageObj.endpoints().authApiSubscriptionPurchase(authToken,
				dataSet.get("existingSubscriptionPlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice,
				startDateTime, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Auth API Purchase Subscription");
		boolean isAuthPurchaseSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPurchaseSubscriptionSchema, purchaseSubscriptionResponse.asString());
		Assert.assertTrue(isAuthPurchaseSubscriptionSchemaValidated,
				"Auth API Purchase Subscription Schema Validation failed");
		int subscriptionId = purchaseSubscriptionResponse.jsonPath().getInt("subscription_id");
		Assert.assertTrue(subscriptionId > 0, "Subscription ID should be greater than 0");
		String startTime = purchaseSubscriptionResponse.jsonPath().getString("start_time");
		Assert.assertNotNull(startTime, "Start time should not be null");
		String endTime = purchaseSubscriptionResponse.jsonPath().getString("end_time");
		Assert.assertNotNull(endTime, "End time should not be null");
		pageObj.utils().logit("pass", "Auth API Purchase Subscription call is successful. Subscription ID: " + subscriptionId + ", Start: " + startTime + ", End: " + endTime);

		// Auth API Cancel Subscription
		pageObj.utils().logit("info", "== Auth API Cancel Subscription ==");
		Response cancelSubscriptionResponse = pageObj.endpoints().authSubscriptionCancel(dataSet.get("client"),
				dataSet.get("secret"), accessToken, Integer.toString(subscriptionId), "Price too High.", cancellationReasonId,
				"soft_cancelled");
		Assert.assertEquals(cancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth API Cancel Subscription");
		boolean isAuthCancelSubscriptionSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, cancelSubscriptionResponse.asString());
		Assert.assertTrue(isAuthCancelSubscriptionSchemaValidated,
				"Auth API Cancel Subscription Schema Validation failed");
		String cancelSubscriptionMsg = cancelSubscriptionResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(cancelSubscriptionMsg, "Cancel subscription message should not be null");
		Assert.assertEquals(cancelSubscriptionMsg, "Subscription auto renewal canceled.", "Message did not match.");
		pageObj.utils().logit("pass", "Auth API Cancel Subscription call is successful. Message: " + cancelSubscriptionMsg);

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
