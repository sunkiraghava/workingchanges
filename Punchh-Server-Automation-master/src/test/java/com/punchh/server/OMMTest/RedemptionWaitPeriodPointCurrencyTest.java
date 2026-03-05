package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionWaitPeriodPointCurrencyTest {

	private static final Logger logger = LogManager.getLogger(RedemptionWaitPeriodPointCurrencyTest.class);
	private PageObj pageObj;
	private String env;
	private String sTCName;
	String run = "ui";
	public WebDriver driver;
	private static Map<String, String> dataSet;
	private static String business_id;
	private List<String> redeemableExternalIds = new ArrayList<>();
	private Utilities utils;
	private static final String GET_REDEMPTION_STATUS_BY_USER = "SELECT status FROM redemptions "
			+ "WHERE internal_tracking_code = '%s' AND user_id = %s";
	private static final String GET_REDEMPTION_STATE_BY_USER = "SELECT redemption_state FROM redemptions "
			+ "WHERE internal_tracking_code = '%s' AND user_id = %s";
	private String userEmail;
	private static final String BUSINESS_PREF_QUERY = "SELECT preferences FROM businesses WHERE id='%s'";

	// =========================================================
	// BEFORE METHOD
	// =========================================================
	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		business_id = dataSet.get("business_id");
		utils = new Utilities();
		redeemableExternalIds.clear();

	}

	// =========================================================
	// DB HELPER METHODS
	// =========================================================
	private String getRedemptionStatus(String trackingCode, String userId) throws Exception {
	    return String.valueOf(DBUtils.executeQueryAndGetColumnValue(env,
	        String.format(GET_REDEMPTION_STATUS_BY_USER, trackingCode, userId),
	        "status"));
	}

	private Integer getRedemptionState(String trackingCode, String userId) throws Exception {
	    return Integer.valueOf(DBUtils.executeQueryAndGetColumnValue(env,
	        String.format(GET_REDEMPTION_STATE_BY_USER, trackingCode, userId),
	        "redemption_state"));
	}

	// =========================================================
	// TESTCASE 1
	// =========================================================
	@Test(description = "SQ-T7209| Verify Redemption Wait Period for Staged Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7209_verifyWaitPeriodWithStagedFlagEnabled() throws Exception {

		// =========================================================
		// 1. ENABLE FLAGS
		// =========================================================
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		updateIntegerPreference(env, business_id, "staged_redemptions_count", 1);

		// =========================================================
		// 2. USER SIGNUP
		// =========================================================
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Signup API Failed");

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");

		utils.logit("User Signed Up → userID: " + userID);

		// =========================================================
		// 3. SEND POINTS TO USER
		// =========================================================
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertEquals(giftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed to credit points to user");

		utils.logit("Points sent to user → userID: " + userID);

		// =========================================================
		// 4. CREATE REDEMPTION USING BANKED CURRENCY (DYNAMIC LOCATION) (HIT 2 TIMES,
		// SHOULD GENERATE ERROR 2ND TIME)
		// =========================================================

		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		// -------------------------
		// FIRST ATTEMPT → SUCCESS
		// -------------------------
		logger.info("------ Banked Currency Redemption Attempt #1 ------");

		Response firstRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(firstRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"First banked currency redemption should succeed");

		String firstTrackingCode = firstRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(firstTrackingCode, "Tracking code not generated on first attempt");

		utils.logPass("First banked currency redemption succeeded | Code: " + firstTrackingCode);

		logger.info("First Redemption Code → " + firstTrackingCode);

		// =========================================================
		// DB VALIDATION → STAGED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(firstTrackingCode, userID), "staged",
				"Redemption status is not STAGED on first attempt");

		Assert.assertEquals(getRedemptionState(firstTrackingCode, userID), 1,
			    "Redemption state is not 1 (STAGED) on first attempt");


		// -------------------------
		// SECOND ATTEMPT → FAIL
		// -------------------------
		logger.info("------ Banked Currency Redemption Attempt #2 ------");

		Response secondRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(secondRedemptionResponse.getStatusCode(), 406,
				"Please use your existing redemption codes or wait for them to expire.");

		logger.info("Second redemption blocked as expected");

		// =========================================================
		// UPDATE PREFERENCE AFTER FAILURE
		// =========================================================
		updateIntegerPreference(env, business_id, "staged_redemptions_count", 2);

		logger.info("Updated staged_redemptions_count to 2");

		// -------------------------
		// THIRD ATTEMPT → SUCCESS
		// -------------------------
		logger.info("------ Banked Currency Redemption Attempt #3 ------");

		Response thirdRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(thirdRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Third banked currency redemption should succeed after preference update");

		String thirdTrackingCode = thirdRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(thirdTrackingCode, "Third redemption tracking code was not generated");

		// Extent Report log
		utils.logPass("Third banked currency redemption succeeded | Code: " + thirdTrackingCode);
	}

	// =========================================================
	// TESTCASE 2
	// =========================================================
	@Test(description = "SQ-T7210| Verify Redemption Wait Period for Staged Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7210_verifyWaitPeriodWithStagedFlagDisabled() throws Exception {

		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 1. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Signup API Failed");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 2. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);

		utils.logit("Points sent to user → userID: " + userID);

		// =========================================================
		// 3. FIRST REDEMPTION USING BANKED CURRENCY → SHOULD SUCCEED (201)
		// =========================================================
		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		Response firstRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(firstRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"First redemption using banked currency failed");

		utils.logPass("First banked currency redemption succeeded");

		// =========================================================
		// 4. DB VALIDATION → FIRST ATTEMPT (STAGED)
		// =========================================================
		String internalTrackingCode = firstRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(internalTrackingCode, "Internal tracking code not generated");

		Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "staged",
				"Redemption status is not STAGED on first attempt");

		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 1,
			    "Redemption state is not 1 (STAGED) on first attempt");


		utils.logPass("First redemption validated | userId=" + userID + " | trackingCode=" + internalTrackingCode
				+ " | status=STAGED | state=1");

		// =========================================================
		// 5. SECOND REDEMPTION → MUST FAIL (422)
		// =========================================================
		Response secondRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(secondRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Second redemption attempt should be blocked");

		logger.info("Second Banked Currency Redemption Response → " + secondRedemptionResponse.prettyPrint());

		// =========================================================
		// 7. ERROR MESSAGE VALIDATION //(ADD TO LISTENER FOR EXTENT REPORT ERROR
		// MESSAGE)
		// =========================================================

		String errorMessage = secondRedemptionResponse.jsonPath().get("[0]").toString();

		Assert.assertEquals(errorMessage,
				"You're unable to redeem multiple times in one visit, please try again later");

		utils.logPass("Second redemption blocked as expected | Error=" + errorMessage);
	}

	// =========================================================
	// TESTCASE 3
	// =========================================================
	@Test(description = "SQ-T7211| Verify Redemption Wait Period for Expired Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7211_verifyWaitPeriodExpiredOffersStagedFlagEnabled() throws Exception {

		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		updateIntegerPreference(env, business_id, "staged_redemptions_count", 2);

		// ******************************************************************
		// 1. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 2. SEND POINTS TO USER
		// ******************************************************************
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 3. GENERATE FIRST REDEMPTION (BANKED CURRENCY)
		// ******************************************************************
		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		Response firstRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(firstRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"First banked currency redemption should succeed");

		String internalTrackingCode = firstRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code was not generated");

		utils.logPass("First banked currency redemption generated successfully | Code: " + internalTrackingCode);

		// =========================================================
		// 4. DB VALIDATION → MUST BE STAGED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "staged",
				"Redemption status is not STAGED before expiring");

		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 1,
			    "Redemption state is not 1 (STAGED) before expiring");
		

		// ******************************************************************
		// 5. UPDATE STAGED → EXPIRED (DB)
		// ******************************************************************
		String updateRedemptionStatusQuery = "UPDATE redemptions " + "SET status = 'expired' "
				+ "WHERE internal_tracking_code = '" + internalTrackingCode + "' " + "AND status = 'staged'";

		logger.info("Updating redemption to expired for tracking code: " + internalTrackingCode);

		int rowsUpdated = DBUtils.executeUpdate(env, dataSet.get("dbName"), updateRedemptionStatusQuery);

		logger.info("Rows updated in redemptions table: " + rowsUpdated);

		Assert.assertTrue(rowsUpdated > 0, "No staged redemption updated to expired");

		// =========================================================
		// 6. DB VALIDATION → MUST BE EXPIRED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "expired",
				"Redemption status did not change to EXPIRED");

		// ******************************************************************
		// 7. GENERATE SECOND REDEMPTION (BANKED CURRENCY → SHOULD SUCCEED)
		// ******************************************************************
		Response secondRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(secondRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Second redemption should succeed after expiry");

		String secondTrackingCode = secondRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(secondTrackingCode, "Second redemption tracking code was not generated");

		utils.logPass(
				"Second banked currency redemption generated successfully after expiry | Code: " + secondTrackingCode);
	}

	// =========================================================
	// TESTCASE 4
	// =========================================================
	@Test(description = "SQ-T7212| Verify Redemption Wait Period for Expired Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled. ")
	@Owner(name = "Hitesh Popli")
	public void T7212_verifyWaitPeriodExpiredOffersStagedFlagDisabled() throws Exception {

		// ******************************************************************
		// 1. SET FLAGS (KEY DIFFERENCE HERE)
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false"); //Expand commentComment on line R416
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 3. SEND POINTS TO USER
		// ******************************************************************
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 4. FIRST REDEMPTION → SHOULD SUCCEED (BANKED CURRENCY)
		// ******************************************************************
		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		Response firstRedemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(firstRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"First banked currency redemption attempt should succeed");

		String firstTrackingCode = firstRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(firstTrackingCode, "Redemption tracking code not generated on first attempt");

		utils.logPass("First banked currency redemption succeeded | Code: " + firstTrackingCode);

		// =========================================================
		// DB VALIDATION → MUST BE STAGED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(firstTrackingCode, userID), "staged",
				"Redemption status is not STAGED on first attempt");

		Assert.assertEquals(getRedemptionState(firstTrackingCode, userID), 1,
			    "Redemption state is not 1 (STAGED) on first attempt");

		utils.logPass("First banked currency redemption success | Code: " + firstTrackingCode
				+ " | Status = STAGED | Redemption state = 1");

		// ******************************************************************
		// 5. SECOND REDEMPTION → SHOULD FAIL (422)
		// ******************************************************************
		Response secondAttemptResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(secondAttemptResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Expected 422 when staged redemption blocks multiple redemption");

		// =========================================================
		// ERROR VALIDATION
		// =========================================================		
		String errorMessage = secondAttemptResponse.jsonPath().get("[0]").toString();

		logger.info("Error Message → " + errorMessage);

		Assert.assertEquals(errorMessage,
				"You're unable to redeem multiple times in one visit, please try again later");

		utils.logPass("Second banked currency redemption blocked as expected | Error: " + errorMessage);
	}

	// =========================================================
	// TESTCASE 5
	// =========================================================
	@Test(description = "SQ-T7207| Verify Redemption Wait Period for Honored Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")

	public void T7207_verifyHonoredOfferWithExcludeStagedFlagEnabled() throws Exception {

		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");

		utils.logit("User Signed Up → userID: " + userId);

		// ******************************************************************
		// 3. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "20", "", "", "");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);

		utils.logit("Points sent to user → userID: " + userId);

		// ******************************************************************
		// 4. POS REDEMPTION FOR DISCOUNT AMOUNT— HIT TWICE (REDEEMABLE FLOW)
		// ******************************************************************
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		for (int attempt = 1; attempt <= 2; attempt++) {

			logger.info("------ POS Redemption Attempt #" + attempt + " ------");

			Response posResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
					dataSet.get("redemptionAmount"), dataSet.get("locationkey"));

			int statusCode = posResponse.getStatusCode();

			// =========================================================
			// FIRST ATTEMPT → MUST SUCCEED
			// =========================================================
			if (attempt == 1) {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_OK, "First POS redemption should succeed");

				Assert.assertTrue(posResponse.jsonPath().getString("status").contains("Please HONOR it."),
						"Unexpected POS success message");

				String posRedemptionCode = posResponse.jsonPath().getString("redemption_code");

				Assert.assertNotNull(posRedemptionCode, "POS redemption code not returned");

				Assert.assertEquals(getRedemptionState(posRedemptionCode, userId), 3,
						"Redemption state is not 3 (PROCESSED)");

				utils.logPass(
						"POS Attempt #1 success | Code: " + posRedemptionCode + " | Redemption state = 3 (PROCESSED)");
			}

			// =========================================================
			// SECOND ATTEMPT → MUST FAIL (422)
			// =========================================================
			else {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Second POS redemption should be blocked");

				String errorMessage = posResponse.jsonPath().getString("[0]");

				Assert.assertEquals(errorMessage,
						"You're unable to redeem multiple times in one visit, please try again later");

				utils.logPass("POS Attempt #2 blocked as expected | Error: " + errorMessage);
			}
		}
	}

	// TESTCASE 6
	// =========================================================
	@Test(description = "SQ-T7208| Verify Redemption Wait Period for Honored Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7208_verifyHonoredOfferWithExcludeStagedFlagDisabled() throws Exception {

		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");

		utils.logit("User Signed Up → userID: " + userId);

		// ******************************************************************
		// 3. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "20", "", "", "");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);

		utils.logit("Points sent to user → userID: " + userId);

		// ******************************************************************
		// 4. POS REDEMPTION – FIRST SHOULD PASS, SECOND SHOULD FAIL
		// ******************************************************************
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		for (int attempt = 1; attempt <= 2; attempt++) {

			logger.info("------ POS Redemption Attempt #" + attempt + " ------");

			Response posResponse = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
					dataSet.get("redemptionAmount"), dataSet.get("locationkey"));

			int statusCode = posResponse.getStatusCode();

			if (attempt == 1) {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_OK);
				Assert.assertTrue(posResponse.jsonPath().getString("status").contains("Please HONOR it."));

				String redemptionCode = posResponse.jsonPath().getString("redemption_code");
				Assert.assertNotNull(redemptionCode);

				Assert.assertEquals(getRedemptionState(redemptionCode, userId), 3);

				utils.logPass("POS Attempt #1 succeeded | Code=" + redemptionCode);

			} else {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);

				String errorMessage = posResponse.jsonPath().getString("[0]");
				Assert.assertEquals(errorMessage,
						"You're unable to redeem multiple times in one visit, please try again later");

				utils.logPass("POS Attempt #2 blocked as expected");
			}
		}
	}

	// =========================================================
	// TESTCASE 7
	// =========================================================
	@Test(description = "SQ-T7213| Verify Redemption Wait Period for Cancelled Orders, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7213_verifyWaitPeriodCancelledOffersStagedFlagEnabled() throws Exception {

		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		updateIntegerPreference(env, business_id, "staged_redemptions_count", 2);

		// ******************************************************************
		// 2. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 3. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);

		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 4. GENERATE REDEMPTION (FIRST TIME → BANKED CURRENCY)
		// ******************************************************************
		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(dataSet.get("client"),
				dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency, latitude, longitude,
				gpsAccuracy);

		int statusCode = redemptionResponse.getStatusCode();
		logger.info("Redemption generate status → " + statusCode);

		// =========================================================
		// EXISTING REDEMPTION CASE (406)
		// =========================================================
		if (statusCode == 406) {
			
			String errorMessage = redemptionResponse.jsonPath().get("[0]").toString();

			Assert.assertEquals(errorMessage, "Please use your existing redemption codes or wait for them to expire.");

			utils.logPass("Redemption blocked as expected | Error: " + errorMessage);
			return;
		}

		// =========================================================
		// SUCCESS CASE → EXTRACT VALUES
		// =========================================================
		String redemptionId = redemptionResponse.jsonPath().getString("redemption_id");

		String internalTrackingCode = redemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(redemptionId, "Redemption ID not returned");
		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code not returned");

		// ******************************************************************
		// 5. CANCEL REDEMPTION (API v1)
		// ******************************************************************
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, accessToken,
				dataSet.get("client"), dataSet.get("secret"));

		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Cancel redemption API did not return 200");

		String cancelMessage = cancelRedemptionResponse.jsonPath().getString("message");

		Assert.assertEquals(cancelMessage, "Redemption successfully cancelled.");

		utils.logPass("Redemption cancelled successfully | Redemption ID: " + redemptionId);

		// ******************************************************************
		// DB VALIDATION → redemption_state = 4 (CANCELLED)
		// ******************************************************************
		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 4,
				"Redemption state did not change to 4 (CANCELLED)");

		utils.logPass("Redemption code: " + internalTrackingCode + " | Redemption state = 4 (CANCELLED)");

		// ******************************************************************
		// 6. GENERATE REDEMPTION AGAIN (AFTER CANCEL → BANKED CURRENCY)
		// ******************************************************************
		Response redemptionAfterCancelResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(redemptionAfterCancelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Redemption after cancel should succeed");

		String newRedemptionCode = redemptionAfterCancelResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(newRedemptionCode, "Redemption code not generated after cancellation");

		utils.logPass("Redemption generated again after cancel | Code: " + newRedemptionCode);
	}

	// =========================================================
	// TESTCASE 8
	// =========================================================
	@Test(description = "SQ-T7214| Verify Redemption Wait Period for Cancelled Orders, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7214_verifyWaitPeriodCancelledOffersStagedFlagDisabled() throws Exception {
		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 3. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);

		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 4. GENERATE REDEMPTION (FIRST TIME → BANKED CURRENCY)
		// ******************************************************************
		int bankedCurrency = 20;
		double latitude = 26.9167509;
		double longitude = 75.8136926;
		int gpsAccuracy = 27;

		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(dataSet.get("client"),
				dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency, latitude, longitude,
				gpsAccuracy);

		int statusCode = redemptionResponse.getStatusCode();

		utils.logit("Redemption generate status → " + statusCode);

		// =========================================================
		// SUCCESS → EXTRACT VALUES
		// =========================================================
		Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_CREATED, "Redemption using banked currency failed");

		String redemptionId = redemptionResponse.jsonPath().getString("redemption_id");

		String internalTrackingCode = redemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(redemptionId, "Redemption ID not returned");
		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code not returned");

		// ******************************************************************
		// 5. CANCEL REDEMPTION (API v1)
		// ******************************************************************
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, accessToken,
				dataSet.get("client"), dataSet.get("secret"));

		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Cancel redemption API did not return 200");

		String cancelMessage = cancelRedemptionResponse.jsonPath().getString("message");

		Assert.assertEquals(cancelMessage, "Redemption successfully cancelled.");

		// API success log
		utils.logPass("Redemption cancelled successfully | Redemption ID: " + redemptionId);

		// ******************************************************************
		// DB VALIDATION → redemption_state = 4 (CANCELLED)
		// ******************************************************************
		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 4,
				"Redemption state did not change to 4 (CANCELLED)");

		utils.logPass("Redemption code: " + internalTrackingCode + " | Redemption state = 4 (CANCELLED)");

		// ******************************************************************
		// 6. GENERATE REDEMPTION AGAIN (AFTER CANCEL → BANKED CURRENCY)
		// ******************************************************************
		Response redemptionAfterCancelResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrencyDynamic(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dataSet.get("locationId"), bankedCurrency,
				latitude, longitude, gpsAccuracy);

		Assert.assertEquals(redemptionAfterCancelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Redemption after cancel should succeed");

		String newRedemptionCode = redemptionAfterCancelResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(newRedemptionCode, "Redemption code not generated after cancellation");

		utils.logPass("Redemption generated again after cancel | Code: " + newRedemptionCode);
	}

	// =========================================================
	// APPLY BOOLEAN FLAGS
	// =========================================================
	private void applyBooleanFlags(Map<String, String> flags) throws Exception {

		String preferences = DBUtils.executeQueryAndGetColumnValue(env, String.format(BUSINESS_PREF_QUERY, business_id),
				"preferences");

		for (Map.Entry<String, String> entry : flags.entrySet()) {

			DBUtils.updateBusinessFlag(env, preferences, entry.getValue(), entry.getKey(), business_id);

			preferences = DBUtils.executeQueryAndGetColumnValue(env, String.format(BUSINESS_PREF_QUERY, business_id),
					"preferences");
		}
	}

	// =========================================================
	// INTEGER FLAG UPDATE
	// =========================================================
	private void updateIntegerPreference(String env, String business_id, String flag, int newValue) throws Exception {

		String preferences = DBUtils.executeQueryAndGetColumnValue(env, String.format(BUSINESS_PREF_QUERY, business_id),
				"preferences");

		String oldValue = Utilities.getPreferencesKeyValue(preferences, flag).get(0).replaceAll("[\\[\\]' ]", "");

		if (Integer.parseInt(oldValue) == newValue) {
			logger.info("{} already set to {}", flag, newValue);
			return;
		}

		String query = "UPDATE businesses SET preferences = REPLACE(" + "preferences, ':" + flag + ": " + oldValue
				+ "', " + "':" + flag + ": " + newValue + "')" + " WHERE id = " + business_id;

		DBUtils.executeUpdateQuery(env, query);
	}

	// =========================================================
	// AFTER METHOD – ROLLBACK
	// =========================================================
	@AfterMethod(alwaysRun = true)
	public void rollbackAndDelete() throws Exception {

		// =====================================================
		// 1. DELETE ALL CREATED REDEEMABLES
		// =====================================================
		for (String externalId : redeemableExternalIds) {
			try {
				utils.deleteLISQCRedeemable(env, null, null, externalId);
				logger.info("Deleted redeemable → {}", externalId);
			} catch (Exception e) {
				logger.error("Failed to delete redeemable → " + externalId, e);
			}
		}

		redeemableExternalIds.clear();

		// =====================================================
		// 2. SET allow_multiple_redemptions TRUE
		// =====================================================

		logger.info("Rolling back allow_multiple_redemptions to TRUE");

		String preferences = DBUtils.executeQueryAndGetColumnValue(env, String.format(BUSINESS_PREF_QUERY, business_id),
				"preferences");

		DBUtils.updateBusinessesPreference(env, preferences, "true", // ALWAYS reset to true
				"allow_multiple_redemptions", business_id);

		logger.info("Rollback completed → allow_multiple_redemptions = true");
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}
}