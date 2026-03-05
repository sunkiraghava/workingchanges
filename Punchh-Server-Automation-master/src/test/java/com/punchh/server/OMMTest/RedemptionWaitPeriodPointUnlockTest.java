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
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionWaitPeriodPointUnlockTest {

	private static final Logger logger = LogManager.getLogger(RedemptionWaitPeriodPointUnlockTest.class);
	private PageObj pageObj;
	private ApiPayloadObj apipayloadObj;
	private String env;
	private String sTCName, baseUrl;
	String run = "ui";
	public WebDriver driver;
	private static Map<String, String> dataSet;
	private static String business_id;
	private List<String> redeemableExternalIds = new ArrayList<>();
	private Utilities utils;
	private final String getRedeemableID = "SELECT id FROM redeemables WHERE uuid ='$actualExternalIdRedeemable'";
	private static final String GET_REDEMPTION_STATUS_BY_USER = "SELECT status FROM redemptions "
			+ "WHERE internal_tracking_code = '%s' AND user_id = %s";

	private static final String GET_REDEMPTION_STATE_BY_USER = "SELECT redemption_state FROM redemptions "
			+ "WHERE internal_tracking_code = '%s' AND user_id = %s";
	private String userEmail;
	private static final String BUSINESS_PREF_QUERY = "SELECT preferences FROM businesses WHERE id='%s'";

	
	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		redeemableExternalIds.clear();
		apipayloadObj = new ApiPayloadObj();
		business_id = dataSet.get("business_id");
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
	@Test(description = "SQ-T7173| Verify staged redemption flag-UI")
	@Owner(name = "Hitesh Popli")
	public void T7173_verifyRedemptionWaitPeriodFlagsUI() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Multiple Redemptions", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.dashboardpage().navigateToTabs("Redemption Validations");

		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
				"Prevent guests from redeeming multiple times in one visit?", "uncheck");

		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Exclude ‘Staged’ offers from Redemption Wait Period",
				"uncheck");

		pageObj.dashboardpage().clickOnUpdateButton();
		
		boolean isRedemptionWaitPeriodVisible = pageObj.gamesPage().isPresent(utils.getLocatorValue("redemptionsPage.searchRedemptionWaitPeriod"));
		Assert.assertTrue(isRedemptionWaitPeriodVisible);
		
		// =========================================================
		// VERIFY REDEMPTION WAIT PERIOD FIELD VISIBILITY
		// =========================================================
		Assert.assertTrue(
		        pageObj.gamesPage().isPresent(
		                utils.getLocatorValue("redemptionsPage.searchRedemptionWaitPeriod")
		        ),
		        "Redemption Wait Period field is not visible"
		);

		// =========================================================
		// VERIFY REDEMPTION WAIT PERIOD HINT TEXT
		// =========================================================
		String redemptionWaitPeriodHintText =
		        "The number of hours after the last checkin on a visit based program after which a redemption can take place. " +
		        "Usually equal to the visit span (e.g. 4 hours)";

		Assert.assertTrue(
		        pageObj.gamesPage().verifyTextOnPage(redemptionWaitPeriodHintText),
		        "Redemption Wait Period hint text is not displayed"
		);

		// =========================================================
		// VERIFY EXCLUDE 'STAGED' OFFERS HINT TEXT
		// =========================================================
		String excludeStagedOffersHintText =
		        "When enabled, only 'Honored' offers will be considered for Redemption Wait Period validation. " +
		        "'Staged' offers will be excluded.";

		Assert.assertTrue(
		        pageObj.gamesPage().verifyTextOnPage(excludeStagedOffersHintText),
		        "Exclude ‘Staged’ offers from Redemption Wait Period hint text is not displayed"
		);
		
	    // =========================================================
	    // VERIFY SUCCESS MESSAGE ON PAGE
	    // =========================================================

		Assert.assertEquals(utils.getSuccessMessage(), "Business was successfully updated.");
		
	}

	// =========================================================
	// TESTCASE 2
	// =========================================================
	@Test(description = "SQ-T7167| Verify Redemption Wait Period for Staged Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7167_verifyWaitPeriodWithStagedFlagEnabled() throws Exception {

		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		updateIntegerPreference(env, business_id, "staged_redemptions_count", 2);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		logger.info("Redeemable Payload → " + redeemablePayload);

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200, "Create Redeemable API did not return 200");

		logger.info("Redeemable Created → " + redeemableResponse.prettyPrint());

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB!");
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Signup API Failed");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");
		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);	
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. GENERATE REDEMPTION CODE using Redeemable ID
		// ******************************************************************
		for (int i = 1; i <= 1; i++) {

			logger.info("------ Running Redemption Code API Attempt #" + i + " ------");

			Response redemptionCodeResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
					dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID,
					dataSet.get("locationId"));

			int statusCode = redemptionCodeResponse.getStatusCode();
			logger.info("Status Code → " + statusCode);

			// SUCCESS CASE
			String internalTrackingCode = redemptionCodeResponse.jsonPath().getString("redemption_tracking_code");

			Assert.assertNotNull(internalTrackingCode, "Redemption tracking code was not generated");

			Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "staged");

			Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 1);

			utils.logPass("Redemption validated for userId=" + userID + " | trackingCode=" + internalTrackingCode
					+ " | status=STAGED | state=1");
			
			utils.logit("Redemption Code API → " + redemptionCodeResponse);
			
		}
	}

	// =========================================================
	// TESTCASE 3
	// =========================================================
	@Test(description = "SQ-T7168| Verify Redemption Wait Period for Staged Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7168_verifyWaitPeriodWithStagedFlagDisabled() throws Exception {

		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		logger.info("Redeemable Payload → " + redeemablePayload);

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200, "Create Redeemable API did not return 200");

		logger.info("Redeemable Created → " + redeemableResponse.prettyPrint());

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);

		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB!");

		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Signup API Failed");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();

		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);
		
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. GENERATE REDEMPTION CODE using Redeemable ID
		// ******************************************************************

		for (int i = 1; i <= 2; i++) {

			logger.info("------ Running Redemption Code API Attempt #" + i + " ------");

			Response redemptionCodeResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
					dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID,
					dataSet.get("locationId"));

			int statusCode = redemptionCodeResponse.getStatusCode();
			logger.info("Status Code → " + statusCode);

			// =========================================================
			// FIRST ATTEMPT → MUST SUCCEED
			// =========================================================
			if (i == 1) {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_CREATED, "First redemption attempt should succeed");

				String internalTrackingCode = redemptionCodeResponse.jsonPath().getString("redemption_tracking_code");

				Assert.assertNotNull(internalTrackingCode, "Redemption tracking code not generated on first attempt");

				// DB VALIDATION

				Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "staged");

				Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 1);

				utils.logPass("Redemption validated for userId=" + userID + " | trackingCode="
						+ internalTrackingCode + " | status=STAGED | state=1");

				logger.info("Redemption Code API → " + redemptionCodeResponse.prettyPrint());
			}

			// =========================================================
			// SECOND ATTEMPT → MUST FAIL (422)
			// =========================================================
			else {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Second redemption attempt should be blocked");
				
				String errorMessage = redemptionCodeResponse.jsonPath().get("[0]").toString();


				Assert.assertEquals(errorMessage,
						"You're unable to redeem multiple times in one visit, please try again later");

				utils.logPass("Attempt #2 blocked as expected | Error: " + errorMessage);
			}
		}
	}

	// =========================================================
	// TESTCASE 4
	// =========================================================
	@Test(description = "SQ-T7169| Verify Redemption Wait Period for Expired Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7169_verifyWaitPeriodExpiredOffersStagedFlagEnabled() throws Exception {

		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		updateIntegerPreference(env, business_id, "staged_redemptions_count", 2);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200);

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID);
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();
		
		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");
		
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. GENERATE FIRST REDEMPTION CODE
		// ******************************************************************
		Response redemptionCodeResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		Assert.assertTrue(
				redemptionCodeResponse.getStatusCode() == ApiConstants.HTTP_STATUS_OK || redemptionCodeResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"First redemption should succeed");

		String internalTrackingCode = redemptionCodeResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code was not generated");

		// =========================================================
		// DB VALIDATION → MUST BE STAGED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "staged",
				"Redemption status is not STAGED before expiring");

		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 1,
				"Redemption state is not 1 (STAGED) before expiring");

		// ******************************************************************
		// 7. UPDATE STAGED → EXPIRED (DB)
		// ******************************************************************
		String updateRedemptionStatusQuery = "UPDATE redemptions " + "SET status = 'expired' "
				+ "WHERE internal_tracking_code = '" + internalTrackingCode + "' " + "AND status = 'staged'";

		logger.info("Updating redemption to expired for tracking code: " + internalTrackingCode);

		int rowsUpdated = DBUtils.executeUpdate(env, dataSet.get("dbName"), updateRedemptionStatusQuery);

		logger.info("Rows updated in redemptions table: " + rowsUpdated);

		Assert.assertTrue(rowsUpdated > 0, "No staged redemption updated to expired");

		// =========================================================
		// DB VALIDATION → MUST BE EXPIRED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(internalTrackingCode, userID), "expired",
				"Redemption status did not change to EXPIRED");

		// ******************************************************************
		// 8. GENERATE SECOND REDEMPTION CODE (SHOULD SUCCEED)
		// ******************************************************************
		Response secondAttemptResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		Assert.assertTrue(secondAttemptResponse.getStatusCode() == ApiConstants.HTTP_STATUS_OK || secondAttemptResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Second redemption should succeed after expiry");

		String secondTrackingCode = secondAttemptResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(secondTrackingCode, "Second redemption tracking code was not generated");

		utils.logPass("Second redemption generated successfully after expiry | Code: " + secondTrackingCode);
	}

	// =========================================================
	// TESTCASE 5
	// =========================================================
	@Test(description = "SQ-T7170| Verify Redemption Wait Period for Expired Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled. ")
	@Owner(name = "Hitesh Popli")
	public void T7170_verifyWaitPeriodExpiredOffersStagedFlagDisabled() throws Exception {

		// ******************************************************************
		// 1. SET FLAGS (KEY DIFFERENCE HERE)
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false"); //Expand commentComment on line R546
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200);

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID);
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");
		
		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");
		
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. FIRST REDEMPTION → SHOULD SUCCEED
		// ******************************************************************
		Response firstRedemptionResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		Assert.assertEquals(firstRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "First redemption attempt should succeed");

		String firstTrackingCode = firstRedemptionResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(firstTrackingCode, "Redemption tracking code not generated on first attempt");

		// =========================================================
		// DB VALIDATION → STAGED
		// =========================================================
		Assert.assertEquals(getRedemptionStatus(firstTrackingCode, userID), "staged",
				"Redemption status is not STAGED on first attempt");

		Assert.assertEquals(getRedemptionState(firstTrackingCode, userID), 1,
				"Redemption state is not 1 (STAGED) on first attempt");

		utils.logPass(
				"First redemption success | Code: " + firstTrackingCode + " | Status = STAGED | Redemption state = 1");

		// ******************************************************************
		// 7. SECOND REDEMPTION → SHOULD FAIL (422)
		// ******************************************************************
		Response secondAttemptResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		Assert.assertEquals(secondAttemptResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Expected 422 when staged redemption blocks multiple redemption");
		
		String errorMessage = secondAttemptResponse.jsonPath().get("[0]").toString();

		Assert.assertEquals(errorMessage,
				"You're unable to redeem multiple times in one visit, please try again later");

		utils.logPass("Second redemption blocked as expected | Error: " + errorMessage);
	}

	// =========================================================
	// TESTCASE 6
	// =========================================================
	@Test(description = "SQ-T7165| Verify Redemption Wait Period for Honored Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")

	public void T7165_verifyHonoredOfferWithExcludeStagedFlagEnabled() throws Exception {

		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "true");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(false)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setPoints(1).setApplicable_as_loyalty_redemptionFlag(true).setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200, "Create Redeemable API did not return 200");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB");
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		
		utils.logit("User Signed Up → userID: " + userId);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);
		
		utils.logit("Points sent to user → userID: " + userId);

		// ******************************************************************
		// 6. POS REDEMPTION — HIT TWICE (REDEEMABLE FLOW)
		// ******************************************************************
		for (int attempt = 1; attempt <= 2; attempt++) {

			logger.info("------ Running POS Redemption Attempt #" + attempt + " ------");

			String txnNo = "TXN_" + CreateDateTime.getTimeDateString();
			String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
			String key = CreateDateTime.getTimeDateString();

			Response posRedemptionResponse = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txnNo,
					dataSet.get("locationKey"), dbRedeemableID, dataSet.get("item_id"));

			int statusCode = posRedemptionResponse.getStatusCode();
			logger.info("POS Status Code → " + statusCode);

			// =========================================================
			// FIRST ATTEMPT → MUST SUCCEED
			// =========================================================
			if (attempt == 1) {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_OK, "First POS redemption should succeed");

				Assert.assertTrue(posRedemptionResponse.jsonPath().getString("status").contains("Please HONOR it."),
						"Unexpected POS success message");

				String posRedemptionCode = posRedemptionResponse.jsonPath().getString("redemption_code");

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

				String errorMessage = posRedemptionResponse.jsonPath().getString("[0]");

				Assert.assertEquals(errorMessage,
						"You're unable to redeem multiple times in one visit, please try again later");

				utils.logPass("POS Attempt #2 blocked as expected | Error: " + errorMessage);
			}
		}
	}

	// TESTCASE 7
	// =========================================================
	@Test(description = "SQ-T7166| Verify Redemption Wait Period for Honored Offers, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7166_verifyHonoredOfferWithExcludeStagedFlagDisabled() throws Exception {

		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(false)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setPoints(1).setApplicable_as_loyalty_redemptionFlag(true).setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.statusCode(), 200, "Create Redeemable API did not return 200");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB");
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		
		utils.logit("User Signed Up → userID: " + userId);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);
		
		utils.logit("Points sent to user → userID: " + userId);

		// ******************************************************************
		// 6. POS REDEMPTION – FIRST SHOULD PASS, SECOND SHOULD FAIL
		// ******************************************************************
		for (int attempt = 1; attempt <= 2; attempt++) {

			logger.info("------ POS Redemption Attempt #" + attempt + " ------");

			Response posResponse = pageObj.endpoints().posRedemptionOfRedeemable(userEmail,
					CreateDateTime.getCurrentDate() + "T17:57:32Z", CreateDateTime.getTimeDateString(),
					"TXN_" + CreateDateTime.getTimeDateString(), dataSet.get("locationKey"), dbRedeemableID,
					dataSet.get("item_id"));

			int statusCode = posResponse.getStatusCode();

			// =========================================================
			// FIRST ATTEMPT → SUCCESS
			// =========================================================
			if (attempt == 1) {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_OK);

				Assert.assertTrue(posResponse.jsonPath().getString("status").contains("Please HONOR it."));

				String redemptionCode = posResponse.jsonPath().getString("redemption_code");

				Assert.assertNotNull(redemptionCode);

				Assert.assertEquals(getRedemptionState(redemptionCode, userId), 3);

				utils.logPass("POS Attempt #1 succeeded | Code: " + redemptionCode);
			}

			// =========================================================
			// SECOND ATTEMPT → ERROR
			// =========================================================
			else {

				Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);

				String errorMessage = posResponse.jsonPath().getString("[0]");

				Assert.assertEquals(errorMessage,
						"You're unable to redeem multiple times in one visit, please try again later");

				utils.logPass("POS Attempt #2 blocked as expected | Error: " + errorMessage);

				// Expected end of test
				return;
			}
		}
	}

	// =========================================================
	// TESTCASE 8
	// =========================================================
	@Test(description = "SQ-T7171| Verify Redemption Wait Period for Cancelled Orders, when the new flag 'Exclude Staged offers from Redemption Wait Period' is enabled.")
	@Owner(name = "Hitesh Popli")
	public void T7171_verifyWaitPeriodCancelledOffersStagedFlagEnabled() throws Exception {

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
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Create Redeemable API failed");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");

		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB");
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");
		
		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);
		
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. GENERATE REDEMPTION CODE (FIRST TIME)
		// ******************************************************************
		Response redemptionCodeResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		int statusCode = redemptionCodeResponse.getStatusCode();
		logger.info("Redemption generate status → " + statusCode);

		// Handle existing redemption scenario
		if (statusCode == 406) {
			
			String errorMessage = redemptionCodeResponse.jsonPath().get("[0]").toString();

			Assert.assertEquals(errorMessage, "Please use your existing redemption codes or wait for them to expire.");
			return;
		}

		// Extract required values
		String redemptionId = redemptionCodeResponse.jsonPath().getString("redemption_id");
		String internalTrackingCode = redemptionCodeResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(redemptionId, "Redemption ID not returned");
		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code not returned");

		// ******************************************************************
		// 7. CANCEL REDEMPTION (API v1)
		// ******************************************************************
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, accessToken,
				dataSet.get("client"), dataSet.get("secret"));

		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Cancel redemption API did not return 200");

		String cancelMessage = cancelRedemptionResponse.jsonPath().getString("message");

		Assert.assertEquals(cancelMessage, "Redemption successfully cancelled.");

		// API success log
		utils.logPass("Redemption cancelled successfully. Redemption ID: " + redemptionId);

		// ******************************************************************
		// DB VALIDATION → redemption_state should be 4 (CANCELLED)
		// ******************************************************************
		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 4,
				"Redemption state did not change to 4 (CANCELLED)");

		// DB confirmation log
		utils.logPass("Redemption code: " + internalTrackingCode + " | Redemption state = 4 (CANCELLED)");

		// ******************************************************************
		// 8. GENERATE REDEMPTION CODE AGAIN (AFTER CANCEL)
		// ******************************************************************
		Response redemptionCodeResponseAfterCancel = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		int statusAfterCancel = redemptionCodeResponseAfterCancel.getStatusCode();

		logger.info("Redemption generate after cancel status → " + statusAfterCancel);

		// =========================================================
		// SUCCESS CASE → NEW CODE GENERATED
		// =========================================================
		String newRedemptionCode = redemptionCodeResponseAfterCancel.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(newRedemptionCode, "Redemption code not generated after cancellation");

		utils.logPass("Redemption generated again after cancel. Code: " + newRedemptionCode);
	}

	// =========================================================
	// TESTCASE 9
	// =========================================================
	@Test(description = "SQ-T7172| Verify Redemption Wait Period for Cancelled Orders, when the new flag 'Exclude Staged offers from Redemption Wait Period' is disabled.")
	@Owner(name = "Hitesh Popli")
	public void T7172_verifyWaitPeriodCancelledOffersStagedFlagDisabled() throws Exception {
		// ******************************************************************
		// 1. BUSINESS FLAGS
		// ******************************************************************
		Map<String, String> flags = new HashMap<>();
		flags.put("allow_multiple_redemptions", "false");
		flags.put("exclude_staged_from_prevent_multi_redemption", "false");
		flags.put("prevent_multiple_redemptions", "true");

		applyBooleanFlags(flags);

		// ******************************************************************
		// 2. CREATE REDEEMABLE
		// ******************************************************************
		String redeemableName = "Auto_Redeemable_" + Utilities.getTimestamp();

		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Create Redeemable API failed");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");

		redeemableExternalIds.add(redeemableExternalID);
		
		utils.logit("External ID → " + redeemableExternalID);

		// ******************************************************************
		// 3. FETCH REDEEMABLE ID FROM DB
		// ******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);

		String dbRedeemableID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableID, "Redeemable ID not found in DB");
		
		utils.logit("DB Redeemable ID → " + dbRedeemableID);

		// ******************************************************************
		// 4. USER SIGNUP
		// ******************************************************************
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		String accessToken = signUpResponse.jsonPath().getString("access_token.token");
		
		utils.logit("User Signed Up → userID: " + userID);

		// ******************************************************************
		// 5. SEND POINTS TO USER
		// ******************************************************************
		Response giftResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

		Assert.assertTrue(giftResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED);
		
		utils.logit("Points sent to user → userID: " + userID);

		// ******************************************************************
		// 6. GENERATE REDEMPTION CODE (FIRST TIME)
		// ******************************************************************
		Response redemptionCodeResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		int statusCode = redemptionCodeResponse.getStatusCode();
		logger.info("Redemption generate status → " + statusCode);

		// =========================================================
		// SUCCESS → EXTRACT VALUES
		// =========================================================
		String redemptionId = redemptionCodeResponse.jsonPath().getString("redemption_id");
		String internalTrackingCode = redemptionCodeResponse.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(redemptionId, "Redemption ID not returned");
		Assert.assertNotNull(internalTrackingCode, "Redemption tracking code not returned");

		// ******************************************************************
		// 7. CANCEL REDEMPTION (API v1)
		// ******************************************************************
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, accessToken,
				dataSet.get("client"), dataSet.get("secret"));

		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String cancelMessage = cancelRedemptionResponse.jsonPath().getString("message");

		Assert.assertEquals(cancelMessage, "Redemption successfully cancelled.");

		// API success log
		utils.logPass("Redemption cancelled successfully. Redemption ID: " + redemptionId);

		// ******************************************************************
		// DB VALIDATION → redemption_state should be 4 (CANCELLED)
		// ******************************************************************
		Assert.assertEquals(getRedemptionState(internalTrackingCode, userID), 4,
				"Redemption state did not change to 4 (CANCELLED)");

		// DB confirmation log
		utils.logPass("Redemption code: " + internalTrackingCode + " | Redemption state = 4 (CANCELLED)");

		// ******************************************************************
		// 8. GENERATE REDEMPTION CODE AGAIN (AFTER CANCEL)
		// ******************************************************************
		Response redemptionCodeResponseAfterCancel = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), accessToken, dbRedeemableID, dataSet.get("locationId"));

		int statusAfterCancel = redemptionCodeResponseAfterCancel.getStatusCode();

		logger.info("Redemption generate after cancel status → " + statusAfterCancel);

		// =========================================================
		// SUCCESS AFTER CANCEL
		// =========================================================
		String newRedemptionCode = redemptionCodeResponseAfterCancel.jsonPath().getString("redemption_tracking_code");

		Assert.assertNotNull(newRedemptionCode, "Redemption code not generated after cancellation");

		utils.logPass("Redemption generated again after cancel. Code: " + newRedemptionCode);
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


	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}