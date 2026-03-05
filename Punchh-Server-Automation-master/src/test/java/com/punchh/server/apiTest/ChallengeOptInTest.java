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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ChallengeOptInTest {

	private static Logger logger = LogManager.getLogger(ChallengeOptInTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, businessesQuery, businessId, optedInAtQuery, campaignPreferencesQuery;
	private String userEmail;
	private String env, run = "api";
	boolean flag1, flag2, flag3, flag4;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
		optedInAtQuery = "SELECT `opted_in_at` FROM `challenge_enrolments` WHERE `user_id` = $user_id AND `campaign_id` = $campaign_id;";
		campaignPreferencesQuery = "SELECT preferences FROM campaigns WHERE id = $campaign_id;";
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T6936 Verify Challenge Opt in APIs (/api/mobile/challenge_opt_in); "
			+ "SQ-T7292: [Secure API] Verify opted_in is returned as true for new campaigns in API response if user has opted_in for the campaign; "
			+ "SQ-T7296: [Secure API] Verify opted_in is returned as false in API response if user has never opted_in/opted_out of the campaign [if no entry in challenge_enrolments table present for opt_in/opt_out]; "
			+ "SQ-T7361: [Secure API] Verify opt_in API returns error for old/new campaigns when user opts_in for the campaign [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign disabled]", groups = "api")
	@Owner(name = "Rakhi Rawat")
	public void T6936_VerifyChallengeOptInAPIs() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set has_challenges to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_optin_for_challenges to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"),
				businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		utils.logit("Flags are updated to respective values");

		// Verify that the challenge campaign used is new
		String campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id",
				dataSet.get("valid_campaign_id"));
		String campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertTrue(campPrefValue.contains(dataSet.get("dbFlag3") + ": true"),
				"Campaign preferences doesn't contains the explicit_opt_in: true flag");
		utils.logit("pass", "Verified that campaign is new as its preferences contains the explicit_opt_in: true flag");
		
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// SQ-T7296/LPE-T3034 [Challenge Campaign type: Universal Auto Enrollment]
		// Verify opted_in is returned as false in API response if user has never 
		// opted_in/opted_out of the campaign. Hit `/api/mobile/challenges`
		Response fetchCampDetailsSecureResponseBefore = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("valid_campaign_id"), "es");
		Assert.assertEquals(fetchCampDetailsSecureResponseBefore.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedInBefore = fetchCampDetailsSecureResponseBefore.jsonPath().getBoolean("opted_in");
		Assert.assertFalse(optedInBefore, "opted_in is not false for the user who has never opted_in/opted_out of the campaign");
		utils.logit(
				"Verified opted_in is returned as false in Fetch challenge details Secure API response if user has never opted_in/opted_out of the campaign");
		// Verify no entry in challenge_enrolments table present for opt_in/opt_out
		String query = optedInAtQuery.replace("$user_id", userID).replace("$campaign_id",
				dataSet.get("valid_campaign_id"));
		String optedInAtValueBefore = DBUtils.executeQueryAndGetColumnValue(env, query, "opted_in_at");
		Assert.assertEquals(optedInAtValueBefore, "", "opted_in_at value is present");
		utils.logPass("Verified No entry present in challenge_enrolments table before opt_in");

		int validCampaignID = Integer.parseInt(dataSet.get("valid_campaign_id"));
		int invalidCampaignID = 1234;
		String wrongToken = "1234abc";

		// api/mobile/challenge_opt_in with valid campaign id
		Response response = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt in with valid campaign id");
		TestListeners.extentTest.get()
				.pass("Status code 200 did not matched for api challenge opt in with valid campaign id");
		Assert.assertEquals(response.jsonPath().get("message"),
				"You have successfully joined the challenge.",
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message matched successfully");

		// SQ-T7292/LPE-T3027
		// Verify opted_in is returned as true for new campaigns in API response if user
		// has opted_in for the campaign. Hit `/api/mobile/challenges`
		Response fetchCampDetailsResponse = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("valid_campaign_id"), "es");
		Assert.assertEquals(fetchCampDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedIn = fetchCampDetailsResponse.jsonPath().getBoolean("opted_in");
		Assert.assertTrue(optedIn, "opted_in is not true for the user who has opted_in of the campaign");
		utils.logit(
				"Verified opted_in is returned as true in Fetch challenge details secure API response if user has opted_in of the campaign");

		// verify entry created in challenge_enrolments table
		String optedInAtValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "opted_in_at",
				10);
		Assert.assertNotNull(optedInAtValue, "opted_in_at value is null — enrolment not created properly.");
		Assert.assertFalse(optedInAtValue.trim().isEmpty(), "opted_in_at is empty — enrolment not valid.");
		logger.info("verified New entry created in challenge_enrolments table");

		// api/mobile/challenge_opt_in with invalid campaign id
		Response response1 = pageObj.endpoints().Api1ChallengeOptIn(invalidCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with invalid campaign id");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api challenge opt in with invalid campaign id");
		String error = response1.jsonPath().get("errors.challenge_not_found").toString();
		Assert.assertTrue(error.contains("Challenge campaign not found"),
				"Challenge opt in message not matched for invalid campaign id");
		utils.logPass("Challenge opt in error message verified for invalid campaign id");

		// api/mobile/challenge_opt_in with Guest Already Enrolled
		Response response2 = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_CONFLICT,
				"Status code 409 did not matched for api challenge opt in with Guest Already Enrolled");
		Assert.assertEquals(response2.jsonPath().get("message"),
				"You have already enrolled in this challenge campaign.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for Guest Already Enrolled");

		// api/mobile/challenge_opt_in with missing required parameter
		Response response3 = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "empty");
		Assert.assertEquals(response3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api challenge opt in with missing required parameter");
		Assert.assertEquals(response3.jsonPath().get("error"), "Required parameter missing or the value is empty: id",
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for missing required parameter");

		// api/mobile/challenge_opt_in with invalid token (Unauthorized access attempts)
		Response response4 = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), wrongToken, "body");
		Assert.assertEquals(response4.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api challenge opt in with invalid token");
		Assert.assertEquals(response4.jsonPath().get("error"), "You need to sign in or sign up before continuing.",
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for invalid token");

		// Set enable_optin_for_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag2"),
				businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag2") + " value is not updated to false");
		utils.logit("Flags are updated to respective values");

		// SQ-T7361/LPE-T3183
		// Set explicit_opt_in to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);

		// api/mobile/challenge_opt_in with enable_optin_for_challenges:false
		Response response5 = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with enable_optin_for_challenges flag false");
		Assert.assertEquals(response5.jsonPath().get("errors.challenges_opt_in_disabled"),
				"Business does not have challenges opt in enabled.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified with enable_optin_for_challenges flag false");

		// Set has_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag4 = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(flag4, dataSet.get("dbFlag1") + " value is not updated to false");

		// api/mobile/challenge_opt_in with has_challenges: false
		Response response6 = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response6.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with has_challenges flag false");
		Assert.assertEquals(response6.jsonPath().get("errors.challenges_disabled"),
				"Business does not have challenges enabled.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for has_challenges flag false");

		// Revert flags to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_optin_for_challenges to true
		flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"),
				businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		utils.logit("Flags are updated to respective values");

	}

	@Test(description = "SQ-T6944 Verify Challenge Opt in APIs (/api2/mobile/challenge_opt_in); "
			+ "SQ-T7291: [API2] Verify opted_in is returned as true for new campaigns in API response if user has opted_in for the campaign; "
			+ "SQ-T7295: [API2] Verify opted_in is returned as false in API response if user has never opted_in/opted_out of the campaign [if no entry in challenge_enrolments table present for opt_in/opt_out]; "
			+ "SQ-T7363: [API2] Verify opt_in API returns error for old/new campaigns when user opts_in for the campaign [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign disabled]", groups = "api")
	@Owner(name = "Rakhi Rawat")
	public void T6944_VerifyChallengeOptInAPI2() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set has_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		utils.logit("Flags are updated to respective values");

		// Verify that the challenge campaign used is new
		String campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id",
				dataSet.get("valid_campaign_id"));
		String campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertTrue(campPrefValue.contains(dataSet.get("dbFlag3") + ": true"),
				"Campaign preferences doesn't contains the explicit_opt_in: true flag");
		utils.logit("pass", "Verified that campaign is new as its preferences contains the explicit_opt_in: true flag");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// SQ-T7295/LPE-T3033 [Challenge Campaign type: Universal Auto Enrollment]
		// Verify opted_in is returned as false in API response if user has never 
		// opted_in/opted_out of the campaign. Hit `/api2/mobile/challenges`
		Response fetchCampDetailsAPI2ResponseBefore = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("valid_campaign_id"), "es");
		Assert.assertEquals(fetchCampDetailsAPI2ResponseBefore.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedInBefore = fetchCampDetailsAPI2ResponseBefore.jsonPath().getBoolean("opted_in");
		Assert.assertFalse(optedInBefore, "opted_in is not false for the user who has never opted_in/opted_out of the campaign");
		utils.logit(
				"Verified opted_in is returned as false in Fetch challenge details API2 response if user has never opted_in/opted_out of the campaign");
		// Verify no entry in challenge_enrolments table present for opt_in/opt_out
		String query = optedInAtQuery.replace("$user_id", userID).replace("$campaign_id",
				dataSet.get("valid_campaign_id"));
		String optedInAtValueBefore = DBUtils.executeQueryAndGetColumnValue(env, query, "opted_in_at");
		Assert.assertEquals(optedInAtValueBefore, "", "opted_in_at value is present");
		utils.logPass("Verified No entry present in challenge_enrolments table before opt_in");

		int validCampaignID = Integer.parseInt(dataSet.get("valid_campaign_id"));
		int invalidCampaignID = 1234;
		String wrongToken = "1234abc";

		// api2/mobile/challenge_opt_in with valid campaign id
		Response response = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt in with valid campaign id");
		TestListeners.extentTest.get()
				.pass("Status code 200 did not matched for api challenge opt in with valid campaign id");
		Assert.assertEquals(response.jsonPath().get("message"),
				"You have successfully joined the challenge.",
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message matched successfully");

		// SQ-T7291/LPE-T3026
		// Verify opted_in is returned as true for new campaigns in API response if user
		// has opted_in for the campaign. Hit `/api2/mobile/challenges`
		Response fetchCampDetailsResponse = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("valid_campaign_id"), "es");
		Assert.assertEquals(fetchCampDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedIn = fetchCampDetailsResponse.jsonPath().getBoolean("opted_in");
		Assert.assertTrue(optedIn, "opted_in is not true for the user who has opted_in for the campaign");
		utils.logit(
				"Verified opted_in is returned as true in Fetch challenge details API2 response if user has opted_in for the campaign");

		// verify entry created in challenge_enrolments table
		String optedInAtValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "opted_in_at",
				10);
		Assert.assertNotNull(optedInAtValue, "opted_in_at value is null — enrolment not created properly.");
		Assert.assertFalse(optedInAtValue.trim().isEmpty(), "opted_in_at is empty — enrolment not valid.");
		logger.info("verified New entry created in challenge_enrolments table");

		// api2/mobile/challenge_opt_in with invalid campaign id
		Response response1 = pageObj.endpoints().Api2ChallengeOptIn(invalidCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with invalid campaign id");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api challenge opt in with invalid campaign id");
		String error = response1.jsonPath().get("errors.challenge_not_found").toString();
		Assert.assertTrue(error.contains("Challenge campaign not found"),
				"Challenge opt in message not matched for invalid campaign id");
		utils.logPass("Challenge opt in error message verified for invalid campaign id");

		// api2/mobile/challenge_opt_in with Guest Already Enrolled
		Response response2 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response2.getStatusCode(), 409,
				"Status code 409 did not matched for api challenge opt in with Guest Already Enrolled");
		Assert.assertEquals(response2.jsonPath().get("message"),
				"You have already enrolled in this challenge campaign.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for Guest Already Enrolled");

		// api2/mobile/challenge_opt_in with missing required parameter
		Response response3 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "empty");
		Assert.assertEquals(response3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api challenge opt in with missing required parameter");
		Assert.assertEquals(response3.jsonPath().get("errors.id"), "Required parameter missing or the value is empty.",
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for missing required parameter");

		// api2/mobile/challenge_opt_in with invalid token (Unauthorized access
		// attempts)
		Response response4 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), wrongToken, "body");
		Assert.assertEquals(response4.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for api challenge opt in with invalid token");
		String errors = response4.jsonPath().get("errors.unauthorized").toString();
		Assert.assertTrue(
				errors.contains("An active access token must be used to query information about the current user"),
				"Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for invalid token");

		// Set enable_optin_for_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag2"), businessId);
		utils.logit("Flags are updated to respective values");

		// SQ-T7363/LPE-T3191
		// Set explicit_opt_in to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);

		// api2/mobile/challenge_opt_in with enable_optin_for_challenges:false
		Response response5 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with enable_optin_for_challenges flag false");
		Assert.assertEquals(response5.jsonPath().get("errors.challenges_opt_in_disabled"),
				"Business does not have challenges opt in enabled.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified with enable_optin_for_challenges flag false");

		// Set has_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag1"), businessId);
		
		// api2/mobile/challenge_opt_in with has_challenges: false
		Response response6 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response6.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api challenge opt in with has_challenges flag false");
		Assert.assertEquals(response6.jsonPath().get("errors.challenges_disabled"),
				"Business does not have challenges enabled.", "Challenge opt in message not matched");
		utils.logPass("Challenge opt in message verified for has_challenges flag false");

		// Revert flags to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set has_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		utils.logit("Flags are updated to respective values");

	}

	@Test(description = "SQ-T7362: [API2] Verify opt_in API returns error for old campaigns when user opts_in for the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> not present/Off]; "
			+ "SQ-T7378: [Secure API] Verify opted_in is not returned for old campaigns in API response; "
			+ "SQ-T7377: [API2] Verify opted_in is not returned for old campaigns in API response; "
			+ "SQ-T7381: [Secure API] Verify opt_out API returns error for old campaigns when user opts_out of the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> not present/Off]; "
			+ "SQ-T7384: [API2] Verify opt_out API returns error for old campaigns when user opts_out of the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> not present/Off]; "
			+ "SQ-T7360: [Secure API] Verify opt_in API returns error for old campaigns when user opts_in for the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> not present/Off]", groups = "api")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7362_verifyChallengeOptInOptOutOnOldCampaign() throws Exception {
		// Set has_challenges to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		// Set explicit_opt_in to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);

		// User signup using Mobile API2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for Mobile API2 User signup");
		utils.logit("pass", "User signup successful using Mobile API2 with user id: " + userID);

		// Ensure that campaign's preferences doesn't contain explicit_opt_in flag
		String campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id",
				dataSet.get("oldChallengeCampId"));
		String campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertFalse(campPrefValue.contains(dataSet.get("dbFlag3")),
				"Campaign preferences contains explicit_opt_in flag");
		utils.logit("pass", "Verified that campaign's preferences doesn't contain explicit_opt_in flag");

		// SQ-T7362/LPE-T3190: Try enrolling user to campaign.
		// [Challenge Campaign type: Universal Auto Enrollment]
		// Hit `api2/mobile/challenge_opt_in` with valid campaign id
		int validCampaignID = Integer.parseInt(dataSet.get("oldChallengeCampId"));
		Response challengeOptInApi2Response = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID,
				dataSet.get("client"), dataSet.get("secret"), token, "body");
		Assert.assertEquals(challengeOptInApi2Response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for API2 Challenge Opt In");
		String challengeOptInApi2ErrorMsg = challengeOptInApi2Response.jsonPath()
				.get("errors.challenge_explicit_opt_in_disabled").toString();
		Assert.assertEquals(challengeOptInApi2ErrorMsg, MessagesConstants.challengeCampaignExplicitOptInDisabledMsg,
				"Error message not matched");
		utils.logit("pass",
				"Verified challenge_opt_in API2 returns error when user opts_in for an old campaign with explicit_opt_in disabled");

		// SQ-T7360/LPE-T3182: Try enrolling user to campaign.
		// Hit `api/mobile/challenge_opt_in` with valid campaign id
		Response challengeOptInSecureResponse = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID,
				dataSet.get("client"), dataSet.get("secret"), token, "body");
		Assert.assertEquals(challengeOptInSecureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for Secure API Challenge Opt In");
		String challengeOptInSecureErrorMsg = challengeOptInSecureResponse.jsonPath()
				.get("errors.challenge_explicit_opt_in_disabled").toString();
		Assert.assertEquals(challengeOptInSecureErrorMsg, MessagesConstants.challengeCampaignExplicitOptInDisabledMsg,
				"Error message not matched");
		utils.logit("pass",
				"Verified challenge_opt_in secure API returns error when user opts_in for an old campaign with explicit_opt_in disabled");

		// SQ-T7378/LPE-T3174. Hit `api/mobile/challenges` for old campaign
		// and verify that API response doesn't contain opted_in
		utils.logit("User is already enrolled as it's an old campaign");
		Response fetchCampDetailsSecureResponse = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("oldChallengeCampId"), "es");
		Assert.assertEquals(fetchCampDetailsSecureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for Fetch Challenge Details Secure API");
		boolean isOptedInPresent = fetchCampDetailsSecureResponse.jsonPath().getMap("").containsKey("opted_in");
		Assert.assertFalse(isOptedInPresent, "opted_in is present in Fetch Challenge Details Secure API response");
		utils.logit("pass",
				"Verified that opted_in is not returned for old campaigns in Fetch Challenge Details Secure API response");

		// SQ-T7377/LPE-T3173. Hit `api2/mobile/challenges` for old campaign
		// and verify that API response doesn't contain opted_in
		Response fetchCampDetailsAPI2Response = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("oldChallengeCampId"), "es");
		Assert.assertEquals(fetchCampDetailsAPI2Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for Fetch Challenge Details API2");
		isOptedInPresent = fetchCampDetailsAPI2Response.jsonPath().getMap("").containsKey("opted_in");
		Assert.assertFalse(isOptedInPresent, "opted_in is present in Fetch Challenge Details API2 response");
		utils.logit("pass",
				"Verified that opted_in is not returned for old campaigns in Fetch Challenge Details API2 response");

		// SQ-T7381/LPE-T3186. Verify that `api/mobile/challenge_opt_out` errors for old
		// campaign when enable_optin_for_challenges is true and explicit_opt_in is
		// false. Set enable_optout_for_challenges to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag4"), businessId);
		Response challengeOptOutSecureResponse = pageObj.endpoints().Api1ChallengeOptOut(validCampaignID,
				dataSet.get("client"), dataSet.get("secret"), token, "body");
		Assert.assertEquals(challengeOptOutSecureResponse.getStatusCode(),
				ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code mismatch for Secure API Challenge Opt Out");
		String challengeOptOutSecureErrorMsg = challengeOptOutSecureResponse.jsonPath()
				.get("errors.challenge_explicit_opt_in_disabled").toString();
		Assert.assertEquals(challengeOptOutSecureErrorMsg, MessagesConstants.challengeCampaignExplicitOptInDisabledMsg,
				"Error message mismatch");
		utils.logit("pass",
				"Verified challenge_opt_out Secure API returns error when user opts_out for an old campaign with explicit_opt_in disabled");

		// SQ-T7384/LPE-T3200. Verify that `api2/mobile/challenge_opt_out` errors for
		// old campaign when enable_optin_for_challenges and
		// enable_optout_for_challenges are true and explicit_opt_in is false
		Response challengeOptOutAPI2Response = pageObj.endpoints().api2ChallengeOptOut(dataSet.get("oldChallengeCampId"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(challengeOptOutAPI2Response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for API2 Challenge Opt Out");
		String challengeOptOutAPI2ErrorMsg = challengeOptOutAPI2Response.jsonPath()
				.get("errors.challenge_explicit_opt_in_disabled").toString();
		Assert.assertEquals(challengeOptOutAPI2ErrorMsg, MessagesConstants.challengeCampaignExplicitOptInDisabledMsg,
				"Error message mismatch");
		utils.logit("pass",
				"Verified challenge_opt_out API2 returns error when user opts_out for an old campaign with explicit_opt_in disabled");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}

}
