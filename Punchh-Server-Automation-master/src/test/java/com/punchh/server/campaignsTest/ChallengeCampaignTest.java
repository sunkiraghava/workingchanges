package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ChallengeCampaignTest {

	private static Logger logger = LogManager.getLogger(ChallengeCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName,businessesQuery, businessId,challengeEnrollQuery, campaignPreferencesQuery;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	boolean flag1;

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
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
		campaignPreferencesQuery = "SELECT preferences FROM campaigns WHERE id = $campaign_id;";
		challengeEnrollQuery = "SELECT $column from `$table` where `user_id` = $userId ;";
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}
	
	public void updateChallengeFlag() throws Exception {
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set has_challenges to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		utils.logit("Flag is updated to respective values");

	}

	// Anant
	@Test(description = "SQ-T4711 Verify that the API endpoints (api2/mobile, v1 secure, auth, dashboard, POS) operate correctly with the flag disabled", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Anant")
	public void T4711_challengeCampaignWhenFlagDisabled() throws Exception {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String date = CreateDateTime.getCurrentDate();

		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();

		String oneDayOldDate = CreateDateTime.getPreviousDate(1);
		String twoDayOldDate = CreateDateTime.getPreviousDate(2);
		String todayDate = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");

		String accessToken = signUp.jsonPath().get("access_token.token").toString();

		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(login.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "login api not working");
		String authenticationToken = login.jsonPath().get("authentication_token").toString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");

		pageObj.newCamHomePage().challengeWhatPage(campaignName, dataSet.get("giftType"), dataSet.get("redeemable"));
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(4);
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		String query = "update campaigns set start_date='" + todayDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query);

		String query2 = "update campaigns set end_date='" + todayDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query2);

		// pos checkin
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		utils.logPass("Verfied user get the gifting which is the expected behaviour");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(4);

		// api2/mobile/users/balance
		Response resp2 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
				accessToken);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for api");
		String completedSteps1 = resp2.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
		String totalSteps1 = resp2.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps1, "1", "challenge step values are not updated in the balance api");
		Assert.assertEquals(totalSteps1, dataSet.get("points"), "total step values are not updated in the balance api");
		utils.logPass("verified challenge progress in the api2/mobile/users/balance");

		// api/mobile/users/balance
		Response resp3 = pageObj.endpoints().Api1MobileUsersbalance(accessToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for api");
		String completedSteps2 = resp3.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
		String totalSteps2 = resp3.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps2, "1",
				"challenge step values are not updated in the api/mobile/users/balance");
		Assert.assertEquals(totalSteps2, dataSet.get("points"),
				"total step values are not updated in the api/mobile/users/balance");
		utils.logPass("verified challenge progress in the api/mobile/users/balance");

		// api/auth/users/balance
		Response resp4 = pageObj.endpoints().authApiFetchUserBalance(authenticationToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for api");
		String completedSteps3 = resp4.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
		String totalSteps3 = resp4.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps3, "1",
				"challenge step values are not updated in the api/auth/users/balance");
		Assert.assertEquals(totalSteps3, dataSet.get("points"),
				"total step values are not updated in the api/auth/users/balance");
		utils.logPass("verified challenge progress in the api/auth/users/balance");

		// api2/mobile/challenges
		Response resp5 = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"), dataSet.get("secret"), "es");
		Assert.assertEquals(resp5.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isApi2ListChallengesSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2ListChallengesSchema, resp5.asString());
		Assert.assertTrue(isApi2ListChallengesSchemaValidated, "API v2 List Challenges Schema Validation failed");
		String actualCampaignID = pageObj.guestTimelinePage().verifyDiscountBasketVariable(resp5, "challenges", "name",
				campaignName, "id");

//			String actualCampaignID = resp5.jsonPath().get("challenges[0].id").toString();
		Assert.assertEquals(actualCampaignID, campaignID, "campaign id is not match");
		utils.logPass("Verified campaign id is verified");

		// api/mobile/challenges
		Response resp6 = pageObj.endpoints().apiMobileChallenge(dataSet.get("client"), dataSet.get("secret"),
				accessToken, "es");
		Assert.assertEquals(resp6.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isApi1ListChallengeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2ListChallengesSchema, resp6.asString());
		Assert.assertTrue(isApi1ListChallengeSchemaValidated, "API v1 List Challenges Schema Validation failed");
		String actualCampaignID2 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(resp6, "challenges", "name",
				campaignName, "id");

//			String actualCampaignID2 = resp6.jsonPath().get("challenges[0].id").toString();
		Assert.assertEquals(actualCampaignID2, campaignID, "campaign id is not match");
		utils.logPass("Verified campaign id is verified");

		// Mobile API1 List Challenges with invalid signature
		TestListeners.extentTest.get().info("== Mobile API1 List Challenges with invalid signature ==");
		logger.info("== Mobile API1 List Challenges with invalid signature ==");
		Response mobApiListChallengesInvalidSignature = pageObj.endpoints().apiMobileChallenge("1",
				dataSet.get("secret"), accessToken, "es");
		Assert.assertEquals(mobApiListChallengesInvalidSignature.getStatusCode(), 412);
		boolean isMobApiListChallengeInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, mobApiListChallengesInvalidSignature.asString());
		Assert.assertTrue(isMobApiListChallengeInvalidSignatureSchemaValidated,
				"Mobile API1 List Challenges with invalid signature Schema Validation failed");
		String mobApiListChallengesInvalidSignatureMessage = mobApiListChallengesInvalidSignature.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(mobApiListChallengesInvalidSignatureMessage, "Invalid Signature");
		TestListeners.extentTest.get().pass("Mobile API1 List Challenges with invalid signature is unsuccessful");
		logger.info("Mobile API1 List Challenges with invalid signature is unsuccessful");

		// api/auth/challenges
		Response resp7 = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, "es");
		Assert.assertEquals(resp7.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isAuthListChallengeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2ListChallengesSchema, resp7.asString());
		Assert.assertTrue(isAuthListChallengeSchemaValidated, "Auth API List Challenges Schema Validation failed");
		String actualCampaignID1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(resp7, "challenges", "name",
				campaignName, "id");

//			actualCampaignID1 = resp7.jsonPath().get("challenges[0].id").toString();
		Assert.assertEquals(actualCampaignID1, campaignID, "campaign id is not match");
		utils.logPass("Verified campaign id is verified");

		// Auth API List Challenges with invalid signature
		TestListeners.extentTest.get().info("== Auth API List Challenges with invalid signature ==");
		logger.info("== Auth API List Challenges with invalid signature ==");
		Response authApiListChallengesInvalidSignature = pageObj.endpoints().apiAuthChallenge("1",
				dataSet.get("secret"), authenticationToken, "es");
		Assert.assertEquals(authApiListChallengesInvalidSignature.getStatusCode(), 412);
		boolean isAuthApiListChallengeInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, authApiListChallengesInvalidSignature.asString());
		Assert.assertTrue(isAuthApiListChallengeInvalidSignatureSchemaValidated,
				"Auth API List Challenges with invalid signature Schema Validation failed");
		String authApiListChallengesInvalidSignatureMessage = authApiListChallengesInvalidSignature.jsonPath()
				.get("[0]").toString();
		Assert.assertEquals(authApiListChallengesInvalidSignatureMessage, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth API List Challenges with invalid signature is unsuccessful");
		logger.info("Auth API List Challenges with invalid signature is unsuccessful");

		// Auth API List Challenges with invalid authentication token
		TestListeners.extentTest.get().info("== Auth API List Challenges with invalid authentication token ==");
		logger.info("== Auth API List Challenges with invalid authentication token ==");
		Response authApiListChallengesInvalidToken = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"),
				dataSet.get("secret"), "1", "es");
		Assert.assertEquals(authApiListChallengesInvalidToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthApiListChallengeInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, authApiListChallengesInvalidToken.asString());
		Assert.assertTrue(isAuthApiListChallengeInvalidTokenSchemaValidated,
				"Auth API List Challenges with invalid authentication token Schema Validation failed");
		String authApiListChallengesInvalidTokenMessage = authApiListChallengesInvalidToken.jsonPath().get("error")
				.toString();
		Assert.assertEquals(authApiListChallengesInvalidTokenMessage,
				"You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Auth API List Challenges with invalid authentication token is unsuccessful");
		logger.info("Auth API List Challenges with invalid authentication token is unsuccessful");

		// api2/mobile/challenges/{challenge_campaign_id}
		Response resp8 = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "es");
		Assert.assertEquals(resp8.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isApi2FetchChallengeDetailsSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FetchChallengeDetailsSchema, resp8.asString());
		Assert.assertTrue(isApi2FetchChallengeDetailsSchemaValidated,
				"API v2 Fetch Challenge Details Schema Validation failed");
		String completedSteps4 = resp8.jsonPath().get("progress[0].completed_steps").toString();
		String totalSteps4 = resp8.jsonPath().get("progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps4, "1",
				"challenge step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
		Assert.assertEquals(totalSteps4, dataSet.get("points"),
				"total step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
		logger.info("verified challenge progress in the api2/mobile/challenges/{challenge_campaign_id}");
		TestListeners.extentTest.get()
				.pass("verified challenge progress in the api2/mobile/challenges/{challenge_campaign_id}");

		// Mobile API2 Fetch Challenge Details with invalid client
		TestListeners.extentTest.get().info("== Mobile API2 Fetch Challenge Details with invalid client ==");
		logger.info("== Mobile API2 Fetch Challenge Details with invalid client ==");
		Response mobApi2FetchChallengeDetailsInvalidClient = pageObj.endpoints().fetchChallengeDetails("1",
				dataSet.get("secret"), accessToken, campaignID, "es");
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidClient.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isMobApi2FetchChallengeDetailsInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, mobApi2FetchChallengeDetailsInvalidClient.asString());
		Assert.assertTrue(isMobApi2FetchChallengeDetailsInvalidClientSchemaValidated,
				"Mobile API2 Fetch Challenge Details with invalid client Schema Validation failed");
		String mobApi2FetchChallengeDetailsInvalidClientMessage = mobApi2FetchChallengeDetailsInvalidClient.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidClientMessage,
				"Client ID is incorrect. Please check client param or contact us");
		TestListeners.extentTest.get().pass("Mobile API2 Fetch Challenge Details with invalid client is unsuccessful");
		logger.info("Mobile API2 Fetch Challenge Details with invalid client is unsuccessful");

		// Mobile API2 Fetch Challenge Details with invalid secret
		TestListeners.extentTest.get().info("== Mobile API2 Fetch Challenge Details with invalid secret ==");
		logger.info("== Mobile API2 Fetch Challenge Details with invalid secret ==");
		Response mobApi2FetchChallengeDetailsInvalidSecret = pageObj.endpoints()
				.fetchChallengeDetails(dataSet.get("client"), "1", accessToken, campaignID, "es");
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidSecret.getStatusCode(), 412);
		boolean isMobApi2FetchChallengeDetailsInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, mobApi2FetchChallengeDetailsInvalidSecret.asString());
		Assert.assertTrue(isMobApi2FetchChallengeDetailsInvalidSecretSchemaValidated,
				"Mobile API2 Fetch Challenge Details with invalid secret Schema Validation failed");
		String mobApi2FetchChallengeDetailsInvalidSecretMessage = mobApi2FetchChallengeDetailsInvalidSecret.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidSecretMessage,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		TestListeners.extentTest.get().pass("Mobile API2 Fetch Challenge Details with invalid secret is unsuccessful");
		logger.info("Mobile API2 Fetch Challenge Details with invalid secret is unsuccessful");

		// Mobile API2 Fetch Challenge Details with invalid authentication token
		TestListeners.extentTest.get()
				.info("== Mobile API2 Fetch Challenge Details with invalid authentication token ==");
		logger.info("== Mobile API2 Fetch Challenge Details with invalid authentication token ==");
		Response mobApi2FetchChallengeDetailsInvalidToken = pageObj.endpoints()
				.fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"), "1", campaignID, "es");
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isMobApi2FetchChallengeDetailsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, mobApi2FetchChallengeDetailsInvalidToken.asString());
		Assert.assertTrue(isMobApi2FetchChallengeDetailsInvalidTokenSchemaValidated,
				"Mobile API2 Fetch Challenge Details with invalid authentication token Schema Validation failed");
		String mobApi2FetchChallengeDetailsInvalidTokenMessage = mobApi2FetchChallengeDetailsInvalidToken.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidTokenMessage,
				"An active access token must be used to query information about the current user.");
		TestListeners.extentTest.get()
				.pass("Mobile API2 Fetch Challenge Details with invalid authentication token is unsuccessful");
		logger.info("Mobile API2 Fetch Challenge Details with invalid authentication token is unsuccessful");

		// Mobile API2 Fetch Challenge Details with invalid campaign id
		TestListeners.extentTest.get().info("== Mobile API2 Fetch Challenge Details with invalid campaign id ==");
		logger.info("== Mobile API2 Fetch Challenge Details with invalid campaign id ==");
		Response mobApi2FetchChallengeDetailsInvalidCampaignId = pageObj.endpoints()
				.fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"), accessToken, "1", "es");
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isMobApi2FetchChallengeDetailsInvalidCampaignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2ChallengeNotFoundSchema,
				mobApi2FetchChallengeDetailsInvalidCampaignId.asString());
		Assert.assertTrue(isMobApi2FetchChallengeDetailsInvalidCampaignIdSchemaValidated,
				"Mobile API2 Fetch Challenge Details with invalid campaign id Schema Validation failed");
		String mobApi2FetchChallengeDetailsInvalidCampaignIdMessage = mobApi2FetchChallengeDetailsInvalidCampaignId
				.jsonPath().get("errors.challenge_not_found").toString();
		Assert.assertEquals(mobApi2FetchChallengeDetailsInvalidCampaignIdMessage, "Challenge campaign not found.");
		TestListeners.extentTest.get()
				.pass("Mobile API2 Fetch Challenge Details with invalid campaign id is unsuccessful");
		logger.info("Mobile API2 Fetch Challenge Details with invalid campaign id is unsuccessful");

		// api/mobile/challenges/{challenge_campaign_id}
		Response resp9 = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "es");
		Assert.assertEquals(resp9.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isApi1FetchChallengeDetailsSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FetchChallengeDetailsSchema, resp9.asString());
		Assert.assertTrue(isApi1FetchChallengeDetailsSchemaValidated,
				"API v1 Fetch Challenge Details Schema Validation failed");
		String completedSteps5 = resp9.jsonPath().get("progress[0].completed_steps").toString();
		String totalSteps5 = resp9.jsonPath().get("progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps5, "1",
				"challenge step values are not updated in the api/mobile/challenges/{challenge_campaign_id}");
		Assert.assertEquals(totalSteps5, dataSet.get("points"),
				"total step values are not updated in the api/mobile/challenges/{challenge_campaign_id}");
		logger.info("verified challenge progress in the api/mobile/challenges/{challenge_campaign_id}");
		TestListeners.extentTest.get()
				.pass("verified challenge progress in the api/mobile/challenges/{challenge_campaign_id}");

		// Mobile API1 Fetch Challenge Details with invalid signature
		TestListeners.extentTest.get().info("== Mobile API1 Fetch Challenge Details with invalid signature ==");
		logger.info("== Mobile API1 Fetch Challenge Details with invalid signature ==");
		Response mobApi1FetchChallengeDetailsInvalidSignature = pageObj.endpoints().api1fetchChallengeDetails("1",
				dataSet.get("secret"), accessToken, campaignID, "es");
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidSignature.getStatusCode(), 412);
		boolean isMobApi1FetchChallengeDetailsInvalidSignatureSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema,
						mobApi1FetchChallengeDetailsInvalidSignature.asString());
		Assert.assertTrue(isMobApi1FetchChallengeDetailsInvalidSignatureSchemaValidated,
				"Mobile API1 Fetch Challenge Details with invalid signature Schema Validation failed");
		String mobApi1FetchChallengeDetailsInvalidSignatureMessage = mobApi1FetchChallengeDetailsInvalidSignature
				.jsonPath().get("[0]").toString();
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidSignatureMessage, "Invalid Signature");
		TestListeners.extentTest.get()
				.pass("Mobile API1 Fetch Challenge Details with invalid signature is unsuccessful");
		logger.info("Mobile API1 Fetch Challenge Details with invalid signature is unsuccessful");

		// Mobile API1 Fetch Challenge Details with invalid access token
		TestListeners.extentTest.get().info("== Mobile API1 Fetch Challenge Details with invalid access token ==");
		logger.info("== Mobile API1 Fetch Challenge Details with invalid access token ==");
		Response mobApi1FetchChallengeDetailsInvalidToken = pageObj.endpoints()
				.api1fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"), "1", campaignID, "es");
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isMobApi1FetchChallengeDetailsInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, mobApi1FetchChallengeDetailsInvalidToken.asString());
		Assert.assertTrue(isMobApi1FetchChallengeDetailsInvalidTokenSchemaValidated,
				"Mobile API1 Fetch Challenge Details with invalid access token Schema Validation failed");
		String mobApi1FetchChallengeDetailsInvalidTokenMessage = mobApi1FetchChallengeDetailsInvalidToken.jsonPath()
				.get("error").toString();
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidTokenMessage,
				"You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Mobile API1 Fetch Challenge Details with invalid access token is unsuccessful");
		logger.info("Mobile API1 Fetch Challenge Details with invalid access token is unsuccessful");

		// Mobile API1 Fetch Challenge Details with invalid campaign id
		TestListeners.extentTest.get().info("== Mobile API1 Fetch Challenge Details with invalid campaign id ==");
		logger.info("== Mobile API1 Fetch Challenge Details with invalid campaign id ==");
		Response mobApi1FetchChallengeDetailsInvalidCampaignId = pageObj.endpoints()
				.api1fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"), accessToken, "1", "es");
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isMobApi1FetchChallengeDetailsInvalidCampaignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2ChallengeNotFoundSchema,
				mobApi1FetchChallengeDetailsInvalidCampaignId.asString());
		Assert.assertTrue(isMobApi1FetchChallengeDetailsInvalidCampaignIdSchemaValidated,
				"Mobile API1 Fetch Challenge Details with invalid campaign id Schema Validation failed");
		String mobApi1FetchChallengeDetailsInvalidCampaignIdMessage = mobApi1FetchChallengeDetailsInvalidCampaignId
				.jsonPath().get("errors.challenge_not_found").toString();
		Assert.assertEquals(mobApi1FetchChallengeDetailsInvalidCampaignIdMessage, "Challenge campaign not found.");
		TestListeners.extentTest.get()
				.pass("Mobile API1 Fetch Challenge Details with invalid campaign id is unsuccessful");
		logger.info("Mobile API1 Fetch Challenge Details with invalid campaign id is unsuccessful");

		// api/auth/challenges/{challenge_campaign_id}
		Response resp10 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, campaignID, "es");
		Assert.assertEquals(resp10.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isAuthFetchChallengeDetailsSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FetchChallengeDetailsSchema, resp10.asString());
		Assert.assertTrue(isAuthFetchChallengeDetailsSchemaValidated,
				"Auth API Fetch Challenge Details Schema Validation failed");
		String completedSteps6 = resp10.jsonPath().get("progress[0].completed_steps").toString();
		String totalSteps6 = resp10.jsonPath().get("progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps6, "1",
				"challenge step values are not updated in the api/auth/challenges/{challenge_campaign_id}");
		Assert.assertEquals(totalSteps6, dataSet.get("points"),
				"total step values are not updated in the api/auth/challenges/{challenge_campaign_id}");
		logger.info("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");
		TestListeners.extentTest.get()
				.pass("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");

		// Auth Fetch Challenge detail with invalid signature
		TestListeners.extentTest.get().info("== Auth Fetch Challenge detail with invalid signature ==");
		logger.info("== Auth Fetch Challenge detail with invalid signature ==");
		Response authFetchChallengeDetailInvalidSignature = pageObj.endpoints()
				.apiAuthChallengeDetails(dataSet.get("client"), "1", authenticationToken, campaignID, "es");
		Assert.assertEquals(authFetchChallengeDetailInvalidSignature.getStatusCode(), 412);
		boolean isAuthFetchChallengeDetailInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, authFetchChallengeDetailInvalidSignature.asString());
		Assert.assertTrue(isAuthFetchChallengeDetailInvalidSignatureSchemaValidated,
				"Auth Fetch Challenge detail with invalid signature Schema Validation failed");
		String authFetchChallengeDetailInvalidSignatureMessage = authFetchChallengeDetailInvalidSignature.jsonPath()
				.get("[0]").toString();
		Assert.assertEquals(authFetchChallengeDetailInvalidSignatureMessage, "Invalid Signature");
		TestListeners.extentTest.get().pass("Auth Fetch Challenge detail with invalid signature is unsuccessful");
		logger.info("Auth Fetch Challenge detail with invalid signature is unsuccessful");

		// Auth Fetch Challenge detail with invalid authentication token
		TestListeners.extentTest.get().info("== Auth Fetch Challenge detail with invalid authentication token ==");
		logger.info("== Auth Fetch Challenge detail with invalid authentication token ==");
		Response authFetchChallengeDetailInvalidToken = pageObj.endpoints()
				.apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"), "1", campaignID, "es");
		Assert.assertEquals(authFetchChallengeDetailInvalidToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthFetchChallengeDetailInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, authFetchChallengeDetailInvalidToken.asString());
		Assert.assertTrue(isAuthFetchChallengeDetailInvalidTokenSchemaValidated,
				"Auth Fetch Challenge detail with invalid authentication token Schema Validation failed");
		String authFetchChallengeDetailInvalidTokenMessage = authFetchChallengeDetailInvalidToken.jsonPath()
				.get("error").toString();
		Assert.assertEquals(authFetchChallengeDetailInvalidTokenMessage,
				"You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Auth Fetch Challenge detail with invalid authentication token is unsuccessful");
		logger.info("Auth Fetch Challenge detail with invalid authentication token is unsuccessful");

		// Auth Fetch Challenge detail with invalid campaign id
		TestListeners.extentTest.get().info("== Auth Fetch Challenge detail with invalid campaign id ==");
		logger.info("== Auth Fetch Challenge detail with invalid campaign id ==");
		Response authFetchChallengeDetailInvalidCampaignId = pageObj.endpoints()
				.apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"), authenticationToken, "1", "es");
		Assert.assertEquals(authFetchChallengeDetailInvalidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthFetchChallengeDetailInvalidCampaignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				authFetchChallengeDetailInvalidCampaignId.asString());
		Assert.assertTrue(isAuthFetchChallengeDetailInvalidCampaignIdSchemaValidated,
				"Auth Fetch Challenge detail with invalid campaign id Schema Validation failed");
		String authFetchChallengeDetailInvalidCampaignIdMessage = authFetchChallengeDetailInvalidCampaignId.jsonPath()
				.get("error.message").toString();
		Assert.assertEquals(authFetchChallengeDetailInvalidCampaignIdMessage, "Challenge campaign not found.");
		TestListeners.extentTest.get().pass("Auth Fetch Challenge detail with invalid campaign id is unsuccessful");
		logger.info("Auth Fetch Challenge detail with invalid campaign id is unsuccessful");

		// api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}
		Response resp11 = pageObj.endpoints().apiPosChallenge(campaignID, userEmail, dataSet.get("locationkey"), "es");
		Assert.assertEquals(resp11.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		boolean isPosFetchChallengeDetailsSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FetchChallengeDetailsSchema, resp11.asString());
		Assert.assertTrue(isPosFetchChallengeDetailsSchemaValidated,
				"POS API Fetch Challenge Details Schema Validation failed");
		String completedSteps7 = resp11.jsonPath().get("progress[0].completed_steps").toString();
		String totalSteps7 = resp11.jsonPath().get("progress[0].total_steps").toString();
		Assert.assertEquals(completedSteps7, "1",
				"challenge step values are not updated in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
		Assert.assertEquals(totalSteps7, dataSet.get("points"),
				"total step values are not updated in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
		logger.info(
				"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
		TestListeners.extentTest.get().pass(
				"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");

		// POS API Fetch Challenge Details with invalid user email
		TestListeners.extentTest.get().info("== POS API Fetch Challenge Details with invalid user email ==");
		logger.info("== POS API Fetch Challenge Details with invalid user email ==");
		Response posFetchChallengeDetailsInvalidEmail = pageObj.endpoints().apiPosChallenge(campaignID, "1",
				dataSet.get("locationkey"), "es");
		Assert.assertEquals(posFetchChallengeDetailsInvalidEmail.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND);
		boolean isPosFetchChallengeDetailsInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, posFetchChallengeDetailsInvalidEmail.asString());
		Assert.assertTrue(isPosFetchChallengeDetailsInvalidEmailSchemaValidated,
				"POS API Fetch Challenge Details with invalid user email Schema Validation failed");
		String posFetchChallengeDetailsInvalidEmailMessage = posFetchChallengeDetailsInvalidEmail.jsonPath().get("[0]")
				.toString();
		Assert.assertEquals(posFetchChallengeDetailsInvalidEmailMessage, "User not found");
		TestListeners.extentTest.get().pass("POS API Fetch Challenge Details with invalid user email is unsuccessful");
		logger.info("POS API Fetch Challenge Details with invalid user email is unsuccessful");

		// POS API Fetch Challenge Details with invalid campaign id
		TestListeners.extentTest.get().info("== POS API Fetch Challenge Details with invalid campaign id ==");
		logger.info("== POS API Fetch Challenge Details with invalid campaign id ==");
		Response posFetchChallengeDetailsInvalidCampaignId = pageObj.endpoints().apiPosChallenge("1", userEmail,
				dataSet.get("locationkey"), "es");
		Assert.assertEquals(posFetchChallengeDetailsInvalidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isPosFetchChallengeDetailsInvalidCampaignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema3, posFetchChallengeDetailsInvalidCampaignId.asString());
		Assert.assertTrue(isPosFetchChallengeDetailsInvalidCampaignIdSchemaValidated,
				"POS API Fetch Challenge Details with invalid campaign id Schema Validation failed");
		String posFetchChallengeDetailsInvalidCampaignIdMessage = posFetchChallengeDetailsInvalidCampaignId.jsonPath()
				.get("error[0]").toString();
		Assert.assertEquals(posFetchChallengeDetailsInvalidCampaignIdMessage, "Challenge campaign not found.");
		TestListeners.extentTest.get().pass("POS API Fetch Challenge Details with invalid campaign id is unsuccessful");
		logger.info("POS API Fetch Challenge Details with invalid campaign id is unsuccessful");

		// delete camapigns
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		utils.longWaitInSeconds(4);
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().deleteCampaign(campaignName);
	}

	// Need to delete all the Active Challenge on Autothirteen business
	// Anant
	@Test(description = "SQ-T4710 Verify that the API endpoints (api2/mobile, v1 secure, auth, dashboard, POS) operate correctly with the flag enabled", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Anant")
	public void T4710_challengeCampaignWhenFlagEnabled() throws Exception {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String campaignName1 = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate();
		String oneDayOldDate = CreateDateTime.getPreviousDate(1);
		String twoDayOldDate = CreateDateTime.getPreviousDate(2);

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");

		String accessToken = signUp.jsonPath().get("access_token.token").toString();

		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(login.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "login api not working");
		String authenticationToken = login.jsonPath().get("authentication_token").toString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		List<String> camLst = new ArrayList<>();
		camLst.add(campaignName1);
		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");

		pageObj.newCamHomePage().challengeWhatPage(campaignName1, dataSet.get("giftType"), dataSet.get("redeemable"));
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName1);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(4);
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName1);

		String query = "update campaigns set start_date='" + twoDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query);
		String query2 = "update campaigns set end_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query2);

		// pos checkin
		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZoneWithFullMonth();
		Boolean value = CreateDateTime.compareTimeRange(exptectedTimeOfCheckin);
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		if (value) {
			// check gifting from campaigns
			boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
					dataSet.get("redeemable"));
			Assert.assertTrue(val, "User did not get the gifting from campaign which has id -- " + campaignID
					+ " it is not the expected behaviour");
			logger.info("Verified user did get the gifting from campaign which has id -- " + campaignID
					+ " it is the expected behaviour");
			TestListeners.extentTest.get().pass("Verified user did get the gifting from campaign which has id -- "
					+ campaignID + " it is the expected behaviour");

			pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

			// api2/mobile/users/balance
			Response resp2 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
					accessToken);
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps1 = resp2.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
			String totalSteps1 = resp2.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps1, "1", "challenge step values are not updated in the balance api");
			Assert.assertEquals(totalSteps1, dataSet.get("points"),
					"total step values are not updated in the balance api");
		utils.logPass("verified challenge progress in the api2/mobile/users/balance");

			// api/mobile/users/balance
			Response resp3 = pageObj.endpoints().Api1MobileUsersbalance(accessToken, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps2 = resp3.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
			String totalSteps2 = resp3.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps2, "1",
					"challenge step values are not updated in the api/mobile/users/balance");
			Assert.assertEquals(totalSteps2, dataSet.get("points"),
					"total step values are not updated in the api/mobile/users/balance");
		utils.logPass("verified challenge progress in the api/mobile/users/balance");

			// api/auth/users/balance
			Response resp4 = pageObj.endpoints().authApiFetchUserBalance(authenticationToken, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps3 = resp4.jsonPath().get("punch_cards[0].progress[0].completed_steps").toString();
			String totalSteps3 = resp4.jsonPath().get("punch_cards[0].progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps3, "1",
					"challenge step values are not updated in the api/auth/users/balance");
			Assert.assertEquals(totalSteps3, dataSet.get("points"),
					"total step values are not updated in the api/auth/users/balance");
			logger.info("verified challenge progress in the api/mobile/users/balance");
			TestListeners.extentTest.get().pass("verified challenge progress in the api/auth/users/balance");

			// api2/mobile/challenges
			Response resp5 = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"), dataSet.get("secret"), "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp5.getStatusCode(), "Status code 200 did not matched for api");
			String actualCampaignID = resp5.jsonPath().get("challenges[0].id").toString();
			Assert.assertEquals(actualCampaignID, campaignID, "campaign 1 id is not match");
			utils.logPass("Verified campaign id is verified");

			// api/mobile/challenges
			Response resp6 = pageObj.endpoints().apiMobileChallenge(dataSet.get("client"), dataSet.get("secret"),
					accessToken, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp6.getStatusCode(), "Status code 200 did not matched for api");
			String actualCampaignID2 = resp6.jsonPath().get("challenges[0].id").toString();
			Assert.assertEquals(actualCampaignID2, campaignID, "campaign 1 id is not match");
		utils.logPass("Verified campaign id is verified");

			// api/auth/challenges
			Response resp7 = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"), dataSet.get("secret"),
					authenticationToken, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp7.getStatusCode(), "Status code 200 did not matched for api");
			actualCampaignID2 = resp7.jsonPath().get("challenges[0].id").toString();
			Assert.assertEquals(actualCampaignID2, campaignID, "campaign 1 id is not match");
		utils.logPass("Verified campaign id is verified");

			// api2/mobile/challenges/{challenge_campaign_id}
			Response resp8 = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
					accessToken, campaignID, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp8.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps4 = resp8.jsonPath().get("progress[0].completed_steps").toString();
			String totalSteps4 = resp8.jsonPath().get("progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps4, "1",
					"challenge step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
			Assert.assertEquals(totalSteps4, dataSet.get("points"),
					"total step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
			logger.info("verified challenge progress in the api2/mobile/challenges/{challenge_campaign_id}");
			TestListeners.extentTest.get()
					.pass("verified challenge progress in the api2/mobile/challenges/{challenge_campaign_id}");

			// api/auth/challenges/{challenge_campaign_id}
			Response resp10 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
					authenticationToken, campaignID, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp10.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps6 = resp10.jsonPath().get("progress[0].completed_steps").toString();
			String totalSteps6 = resp10.jsonPath().get("progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps6, "1",
					"challenge step values are not updated in the api/auth/challenges/{challenge_campaign_id}");
			Assert.assertEquals(totalSteps6, dataSet.get("points"),
					"total step values are not updated in the api/auth/challenges/{challenge_campaign_id}");
			logger.info("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");
			TestListeners.extentTest.get()
					.pass("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");

			// api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}
			Response resp11 = pageObj.endpoints().apiPosChallenge(campaignID, userEmail, dataSet.get("locationkey"),
					"es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp11.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps7 = resp11.jsonPath().get("progress[0].completed_steps").toString();
			String totalSteps7 = resp11.jsonPath().get("progress[0].total_steps").toString();
			Assert.assertEquals(completedSteps7, "1",
					"challenge step values are not updated in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
			Assert.assertEquals(totalSteps7, dataSet.get("points"),
					"total step values are not updated in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
			logger.info(
					"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
			TestListeners.extentTest.get().pass(
					"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");

		} else {
			// check gifting from campaigns
			boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
					dataSet.get("redeemable"));
			Assert.assertFalse(val, "User did get the gifting from campaign which has id -- " + campaignID
					+ " it is not the expected behaviour");
			logger.info("Verified user did not get the gifting from campaign which has id -- " + campaignID
					+ " it is the expected behaviour");
			TestListeners.extentTest.get().pass("Verified user not did get the gifting from campaign which has id -- "
					+ campaignID + " it is the expected behaviour");

			pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

			// api2/mobile/users/balance
			Response resp2 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"), dataSet.get("secret"),
					accessToken);
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps1 = resp2.jsonPath().get("punch_cards").toString();
			Assert.assertEquals(completedSteps1, "[]", "complete step is not null");
			logger.info("Verified as expected complete step is null for the api -- api2/mobile/users/balance");
			TestListeners.extentTest.get()
					.pass("Verified as expected complete step is null for the api -- api2/mobile/users/balance");

			// api/mobile/users/balance
			Response resp3 = pageObj.endpoints().Api1MobileUsersbalance(accessToken, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps2 = resp3.jsonPath().get("punch_cards").toString();
			Assert.assertEquals(completedSteps2, "[]", "complete step is not null");
			logger.info("Verified as expected complete step is null for the api -- api/mobile/users/balance");
			TestListeners.extentTest.get()
					.pass("Verified as expected complete step is null for the api -- api1/mobile/users/balance");

			// api/auth/users/balance
			Response resp4 = pageObj.endpoints().authApiFetchUserBalance(authenticationToken, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp4.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps3 = resp4.jsonPath().get("punch_cards").toString();
			Assert.assertEquals(completedSteps3, "[]", "complete step is not null");
			logger.info("Verified as expected complete step is null for the api -- api/auth/users/balance");
			TestListeners.extentTest.get()
					.pass("Verified as expected complete step is null for the api -- api/auth/users/balance");

			// api2/mobile/challenges
			Response resp5 = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"), dataSet.get("secret"), "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp5.getStatusCode(), "Status code 200 did not matched for api");
			String actualCampaignID = resp5.jsonPath().get("challenges").toString();
			Assert.assertNotEquals(actualCampaignID, campaignID, "campaign id is match");
			logger.info("Verified campaign id did not match as expected for the api -- api2/mobile/challenges");
			TestListeners.extentTest.get()
					.pass("\"Verified campaign id did not match as expected as the api -- api2/mobile/challenges");

			// api/mobile/challenges
			Response resp6 = pageObj.endpoints().apiMobileChallenge(dataSet.get("client"), dataSet.get("secret"),
					accessToken, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp6.getStatusCode(), "Status code 200 did not matched for api");
			String actualCampaignID2 = resp6.jsonPath().get("challenges").toString();
			Assert.assertNotEquals(actualCampaignID2, campaignID, "campaign id is match");
			logger.info("Verified campaign id did not match as expected for the api -- api/mobile/challenges");
			TestListeners.extentTest.get()
					.pass("Verified campaign id did not match as expected for the api -- api/mobile/challenges");

			// api/auth/challenges
			Response resp7 = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"), dataSet.get("secret"),
					authenticationToken, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp7.getStatusCode(), "Status code 200 did not matched for api");
			actualCampaignID2 = resp7.jsonPath().get("challenges").toString();
			Assert.assertNotEquals(actualCampaignID2, campaignID, "campaign id is match");
			logger.info("verified campaign id did not match as expected for the api -- api/auth/challenges");
			TestListeners.extentTest.get()
					.pass("verified campaign id did not match as expected for the api -- api/auth/challenges");

			// api2/mobile/challenges/{challenge_campaign_id}
			Response resp8 = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
					accessToken, campaignID, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp8.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps4 = resp8.jsonPath().get("progress[0].completed_steps").toString();
			Assert.assertEquals(completedSteps4, "0",
					"challenge step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
			logger.info(
					"verified challenge step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");
			TestListeners.extentTest.get().pass(
					"verified challenge step values are not updated in the api2/mobile/challenges/{challenge_campaign_id}");

			// api/auth/challenges/{challenge_campaign_id}
			Response resp10 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
					authenticationToken, campaignID, "es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp10.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps6 = resp10.jsonPath().get("progress[0].completed_steps").toString();
			Assert.assertEquals(completedSteps6, "0",
					"challenge step values are not updated in the api/auth/challenges/{challenge_campaign_id}");
			logger.info("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");
			TestListeners.extentTest.get()
					.pass("verified challenge progress in the api/auth/challenges/{challenge_campaign_id}");

			// api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}
			Response resp11 = pageObj.endpoints().apiPosChallenge(campaignID, userEmail, dataSet.get("locationkey"),
					"es");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp11.getStatusCode(), "Status code 200 did not matched for api");
			String completedSteps7 = resp11.jsonPath().get("progress[0].completed_steps").toString();
			Assert.assertEquals(completedSteps7, "0",
					"challenge step values are not updated in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
			logger.info(
					"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
			TestListeners.extentTest.get().pass(
					"verified challenge progress in the api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}");
		}

		// delete camapigns
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		utils.longWaitInSeconds(4);
		pageObj.newCamHomePage().searchCampaign(campaignName1);
		pageObj.newCamHomePage().deleteCampaign(campaignName1);
	}

	// Rakhi
	@Test(description = "SQ-T4934 Validate the creation of reward debit entries where a user earns a multiple reward at a single time running on multiple pods."
			+ "SQ-T5987 Verify the UI changes for Banked Redeemable, Redemption mark and Reward Value Fields for Points Convert to Rewards", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4934_rewardDebitEntriesOfMultipleRewards() throws Exception {

		String postCheckinCampaignName1 = "DND_AutomationPostCheckinCampaignOne23040828072024";
		String postCheckinCampaignName2 = "DND_AutomationPostCheckinCampaignTwo23053628072024";
		String campaignName = "DND_AutomationChallengeCampaign230615280720242";
		String date = CreateDateTime.getCurrentDate();
		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Set checkin expiry Point Based Points Convert To> Rewards
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");
		pageObj.earningPage().setProgramType(dataSet.get("earningType1"));
		pageObj.earningPage().setPointsConvertTo("Rewards");
		pageObj.earningPage().updateConfiguration();

		// navigate to membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel(dataSet.get("membership"));

		for (int i = 1; i < 3; i++) {
			Boolean flag = pageObj.settingsPage().verifyFiledAvailableOrNot(dataSet.get("field" + i));
			Assert.assertTrue(flag,
					dataSet.get("field" + i) + " is not visible for Points Convert To Rewards earning type");
			logger.info(dataSet.get("field" + i) + " is visible for Points Convert To Rewards earning type");
			TestListeners.extentTest.get()
					.info(dataSet.get("filed" + i) + " is visible for Points Convert To Rewards earning type");
		}
		Boolean flag = pageObj.settingsPage().verifyFiledAvailableOrNot(dataSet.get("field3"));
		Assert.assertFalse(flag, dataSet.get("field3") + " is visible for Points Convert To Rewards earning type");
		logger.info("Verified " + dataSet.get("field3") + " is not visible for Points Convert To Rewards earning type");
		TestListeners.extentTest.get().info(
				"Verified " + dataSet.get("field3") + " is not visible for Points Convert To Rewards earning type");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().clickOnSwitchToClassicCamp();

		// activate Post Checkin Campaigns
		String query1 = "UPDATE `free_punchh_campaigns` SET `status` = 'active' WHERE `id` IN ('"
				+ dataSet.get("campId1") + "', '" + dataSet.get("campId2") + "');";
		int rs = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertTrue(rs >= 0, "No campaigns were activated. Update query affected 0 rows.");
		utils.logit("Campaigns '" + dataSet.get("campId1") + "' and '" + dataSet.get("campId2") + "' status set to active successfully");
		logger.info("Post checkin campaigns activated successfully");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchCampaign(campaignName);
		String camstatus2 = pageObj.campaignsbetaPage().getCampaignStatus();
		if (camstatus2.equalsIgnoreCase("Inactive")) {
			// pageObj.campaignsbetaPage().selectCampaign();
			pageObj.campaignspage().deactivateOrDeleteTheCoupon("activate");
		}
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS checkin
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(resp.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Verify campaign triggered or not in guest timeline , Post checkin campaign
		// one
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(postCheckinCampaignName1);
		Assert.assertTrue(campName.equalsIgnoreCase(postCheckinCampaignName1), "Campaign name did not matched");
		logger.info("Post checkin campaign one triggered ie : " + postCheckinCampaignName1);
		TestListeners.extentTest.get().pass("Post checkin campaign one triggered ie : " + postCheckinCampaignName1);

		// Verify campaign triggered or not in guest timeline , Post checkin campaign
		// two
		String campName1 = pageObj.guestTimelinePage().getcampaignNameMasscampaign(postCheckinCampaignName2);
		Assert.assertTrue(campName1.equalsIgnoreCase(postCheckinCampaignName2), "Campaign name did not matched");
		logger.info("Post checkin campaign two triggered ie : " + postCheckinCampaignName2);
		TestListeners.extentTest.get().pass("Post checkin campaign two triggered ie : " + postCheckinCampaignName2);

		// Verify campaign triggered or not in guest timeline , challenge campaign
		boolean campStatus = pageObj.guestTimelinePage().CheckIfCampaignTriggeredChallenge(campaignName);
		Assert.assertTrue(campStatus, "Campaign did not Triggered");
		utils.logPass("Challenge campaign triggered ie : " + campaignName);

		// check gifting from post checkin campaign one
		boolean status = pageObj.guestTimelinePage().checkPointsGifting(postCheckinCampaignName1, "500");
		Assert.assertTrue(status, "Gifted points by post checkin campaign one did not appeared");
		logger.info("Verified user get the gifting from campaign -- " + postCheckinCampaignName1);
		TestListeners.extentTest.get()
				.pass("Verified user get the gifting from campaign -- " + postCheckinCampaignName1);

		// check gifting from post checkin campaign two
		boolean status1 = pageObj.guestTimelinePage().checkPointsGifting(postCheckinCampaignName2, "1,000");
		Assert.assertTrue(status1, "Gifted points by post checkin campaign two did not appeared");
		logger.info("Verified user get the gifting from campaign -- " + postCheckinCampaignName2);
		TestListeners.extentTest.get()
				.pass("Verified user get the gifting from campaign -- " + postCheckinCampaignName2);

		// check gifting from challenge campaign
		boolean status2 = pageObj.guestTimelinePage().checkPointsGifting(campaignName, "510");
		Assert.assertTrue(status2, "Gifted points by challenge campaign did not appeared");
		utils.logPass("Verified user get the gifting from campaign -- " + campaignName);

		// deactivate Post Checkin Campaigns
		String query2 = "UPDATE `free_punchh_campaigns` SET `status` = 'inactive' WHERE `id` IN ('"
				+ dataSet.get("campId1") + "', '" + dataSet.get("campId2") + "');";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertTrue(rs2 > 0, "No post checkin campaigns were deactivated.");
		utils.logit("Campaigns '" + dataSet.get("campId1") + "' and '" + dataSet.get("campId2") + "' status set to inactive successfully");
		logger.info("Post checkin campaigns deactivated successfully");

		// deactivate challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date + "' WHERE `id` = '"
				+ dataSet.get("challengeId") + "' ;";
		int rs1 = DBUtils.executeUpdateQuery(env, deactivateQuery);
		utils.logit("Challenge campaigns deactivated successfully");

	}

	// Rakhi
	@Test(description = "SQ-T4939 Verify that admins can select a timezone from the \"Execution Window\" section on the 3rd step during Challenge Creation.", groups = {
			"regression", "dailyrun" })
	public void T4939_SelectTimezoneFromExecutionWindow() throws Exception {

		updateChallengeFlag();
		// Login to instance

		// get admin timezone
		String adminTimezone = pageObj.dashboardpage().getAdminTimezone();
		logger.info("Admin timezone is : " + adminTimezone);
		TestListeners.extentTest.get().info("Admin timezone is : " + adminTimezone);

		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// create challenge campaign with 1000 points
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue2"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
//			pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		String timezone = pageObj.newCamHomePage().verifySelectedCampaignTimezone();
		Assert.assertEquals(timezone, adminTimezone, "Timezone did not matched");
		logger.info("Admin timezone matched with campaign timezone ie : " + timezone);
		TestListeners.extentTest.get().pass("Admin timezone matched with campaign timezone ie : " + timezone);

		String hint = pageObj.newCamHomePage().verifyHintBelowTimezoneField();
		Assert.assertEquals(hint,
				"The challenge will consider transactions within the Execution Window as per this Timezone. It also dictates the Timezone for Challenge Availability Schedule if applicable.",
				"Hint below timezone field did not matched");
		logger.info("Hint below timezone field matched ie " + hint);
		TestListeners.extentTest.get().pass("Hint below timezone field matched ie " + hint);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().saveAsDraftButton();

		// search and open campaign
		pageObj.campaignspage().searchCampaign(campaignName);
		pageObj.campaignsbetaPage().selectCampaign();
		pageObj.campaignspage().navigateToLastPageOfCamp();
		String timezone1 = pageObj.newCamHomePage().verifySelectedCampaignTimezone();
		Assert.assertEquals(timezone1, dataSet.get("timeZone"), "Timezone did not matched");
		logger.info("Updated timezone matched ie : " + timezone1);
		TestListeners.extentTest.get().pass("Updated timezone matched ie : " + timezone1);

	}

	// Rakhi
	@Test(description = "SQ-T3813 challenge campaign with every x point", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T3813_challengeCampaignEveryXpoint() throws Exception {

		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String date = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");

		// create challenge campaign with every x point
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
//			pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS checkin
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Verify campaign status on guest timeline , challenge campaign
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_progress");
		Assert.assertTrue(flag, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// POS checkin
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "80");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Verify campaign status on guest timeline , challenge campaign
		boolean flag1 = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_completed");
		Assert.assertTrue(flag1, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// check gifting from challenge campaign
		boolean status = pageObj.guestTimelinePage().checkPointsGifting(campaignName, "500");
		Assert.assertTrue(status, "Gifted points by challenge campaign did not appeared");
		utils.logPass("Verified user get the gifting from campaign -- " + campaignName);

		// Verify challenge campaign triggered or not in challenge tab
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		boolean flag2 = pageObj.guestTimelinePage().CheckIfCampaignTriggeredChallenge(campaignName);
		Assert.assertTrue(flag2, "Campaign did not Triggered");
		utils.logPass("Verified challenge campaign triggered : " + campaignName);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// delete camapigns
		pageObj.campaignspage().removeSearchedCampaign(campaignName);

	}

	// Rakhi
	@Test(description = "SQ-T3814 challenge campaign with receipt qualification || "
			+ "SQ-T5980 Verify that the Challenge campaign having challenge type as Receipt Qualification is reflected in the user’s dashboard > challenge section if the user is targeted by the campaign after user cache enrolment || "
			+ "SQ-T5993 Verify that the Challenge campaign having challenge type as segment is reflected in the user’s dashboard >  challenge section if the user is targeted by the campaign after user cache enrolment")
	@Owner(name = "Rakhi Rawat")
	public void T3814_challengeCampaignReceiptQualification() throws Exception {

		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String currentDate = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + currentDate
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");

		// create challenge campaign with receipt qualification
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// --------------- Segment Auto-enrollment Type--------------
		pageObj.signupcampaignpage().setCampaignName(campaignName);
//			pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		String campaignId = pageObj.signupcampaignpage().getCampaignid();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.newCamHomePage().challengeReachDrpDown("Segment Auto Enrolment");
		pageObj.newCamHomePage().segmentDrpDown(dataSet.get("segmentName"));
		pageObj.newCamHomePage().challengeQualificationDrpDown(dataSet.get("qcName"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeAvailable(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);

		// challenge availability schedule
		pageObj.signupcampaignpage().setFrequency("Once");
		pageObj.signupcampaignpage().setStartDateAndTime();
		pageObj.signupcampaignpage().challengeAvailabilityScheduleStartTime("12:00 AM");

		pageObj.newCamHomePage().activateChallengeCampaign();
		// Run Segment User Cache Schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Segment User Cache Schedule");
		pageObj.schedulespage().runSchedule();
		utils.longWaitInSeconds(3);

		// Run Challenge Availability Schedules
		pageObj.schedulespage().selectScheduleType(dataSet.get("schedule"));
		pageObj.schedulespage().runChallengeCampaignSchedule(campaignName);
		// Verify campaign status on guest timeline , challenge campaign
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		pageObj.guestTimelinePage().pingSessionforLongWait(2);
		boolean flag1 = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_enrollment");
		Assert.assertTrue(flag1, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, dataSet.get("userEmail"), key, txn,
				dataSet.get("locationkey"), dataSet.get("menuItemid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Verify campaign progress status on guest timeline , challenge campaign
		boolean flag = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_progress");
		Assert.assertTrue(flag, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// gift challenge progress to user from guest timeline
		pageObj.guestTimelinePage().messageGiftChallengeProgressToUser("Challenge Progress", campaignName,
				dataSet.get("steps"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		utils.logPass("Verified that Success message of challenge progress send to user ");

		// Verify campaign status on guest timeline , challenge campaign
		boolean flag2 = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_completed");
		Assert.assertTrue(flag2, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// check gifting from challenge campaign
		boolean status1 = pageObj.guestTimelinePage().checkPointsGifting(campaignName, "500");
		Assert.assertTrue(status1, "Gifted points by challenge campaign did not appeared");
		utils.logPass("Verified user get the gifting from campaign -- " + campaignName);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// delete camapigns
		pageObj.campaignspage().removeSearchedCampaign(campaignName);
		
		//delete ChallengeCampaignSchedule from db
		String challengeScheduleDeletion = "DELETE FROM schedules WHERE source_id = " + campaignId
				+ " AND business_id = " + businessId + ";";
		DBUtils.executeUpdateQuery(env, challengeScheduleDeletion);

	}

	// Rakhi
	@Test(description = "SQ-T5351 challenge campaign with Segment Type")
	@Owner(name = "Rakhi Rawat")
	public void T5351_challengeCampaignWithSegmentType() throws Exception {

		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String date = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");

		// create challenge campaign with receipt qualification
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// --------------- Segment Auto-enrollment Type--------------
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		// pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.newCamHomePage().segmentDrpDown("Signed up via Mobile");
		pageObj.newCamHomePage().challengeCampaignPN(campaignName);
		pageObj.newCamHomePage().activateChallengeCampaign();

		// user signup using mobile api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS checkin
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Verify campaign triggered or not in guest timeline , challenge campaign
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flag = pageObj.guestTimelinePage().CheckIfCampaignTriggeredChallenge(campaignName);
		Assert.assertTrue(flag, "Campaign did not Triggered");
		utils.logPass("Challenge campaign triggered ie : " + campaignName);

		// check gifting from challenge campaign
		boolean status2 = pageObj.guestTimelinePage().checkPointsGifting(campaignName, "500");
		Assert.assertTrue(status2, "Gifted points by challenge campaign did not appeared");
		utils.logPass("Verified user get the gifting from campaign -- " + campaignName);

		// Verify challenge campaign triggered or not in challenge tab
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		boolean flag2 = pageObj.guestTimelinePage().CheckIfCampaignTriggeredChallenge(campaignName);
		Assert.assertTrue(flag2, "Campaign did not Triggered");
		utils.logPass("Verified challenge campaign triggered : " + campaignName);

		// delete camapigns
		pageObj.utils().deleteCampaignFromDb(campaignName, env);

	}

	// Rakhi
	@Test(description = "SQ-T5384 Create duplicate classic signup Campaign", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5384_duplicateChallengeCampaign() throws Exception {

		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create challenge campaign with every x point
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
//			pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and duplicate classic campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		String name = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(campaignName, "Edit");
		Assert.assertEquals(name, campaignName + " - copy");
		utils.logPass("Campaign name is prefilled as : " + campaignName + " - copy");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().activateChallengeCampaign();

		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Post Checkin Campaign is not created...");

		// delete camapigns
		pageObj.campaignspage().removeSearchedCampaign(campaignName);

	}

	@Test(description = "SQ-T5704 Verify UI Support for Localized Language Input"
			+ "SQ-T5705 Validate Accepted Language Parameter in API Request Header"
			+ "SQ-T5713 Verify API Response with Localized Language", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5704_verifyUiSupportForLocalizedLanguageInput() throws Exception {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		String date = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String query = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().alternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// verify New challenge campaign screen get displayed
		String pageTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		Assert.assertEquals(pageTitle, "New Challenge Campaign", "Page title did not matched");
		utils.logPass("New challenge campaign page title matched");

		String campaignNameEn = "AutomationChallengeCampaignEnglish" + CreateDateTime.getTimeDateString();
		String campaignNameFr = "AutomationChallengeCampaignFrench" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().createWhatDetailsChallengeCampaignInLocalisedlanguage(campaignNameEn,
				campaignNameFr, dataSet.get("descriptionEn"), dataSet.get("descriptionFr"), campaignNameEn,
				campaignNameFr, campaignNameEn, campaignNameFr);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignNameEn);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignNameEn);

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");
		String accessToken = signUp.jsonPath().get("access_token.token").toString();

		// user login
		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(login.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "login api not working");
		String authenticationToken = login.jsonPath().get("authentication_token").toString();

		// verify response for /api/mobile/challenges
		Response resp1 = pageObj.endpoints().apiMobileChallenge(dataSet.get("client"), dataSet.get("secret"),
				accessToken, "es");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName = resp1.jsonPath().get("challenges[0].name").toString();
		String actualCampDescription = resp1.jsonPath().get("challenges[0].description").toString();
		String actualMiscellaneous = resp1.jsonPath().get("challenges[0].miscellaneous").toString();
		String actualGiftReason = resp1.jsonPath().get("challenges[0].gift_reason").toString();

		Assert.assertEquals(actualCampName, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified the response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified the response returns the localized values for all fields in parity with the provided input");

		// api/auth/challenges
		Response resp2 = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, "es");
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName1 = resp2.jsonPath().get("challenges[0].name").toString();
		String actualCampDescription1 = resp2.jsonPath().get("challenges[0].description").toString();
		String actualMiscellaneous1 = resp2.jsonPath().get("challenges[0].miscellaneous").toString();
		String actualGiftReason1 = resp2.jsonPath().get("challenges[0].gift_reason").toString();

		Assert.assertEquals(actualCampName1, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription1, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous1, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason1, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified the response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified the response returns the localized values for all fields in parity with the provided input");

		// api/mobile/challenges/{challenge_campaign_id}
		Response resp3 = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "es");
		Assert.assertEquals(resp3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName2 = resp3.jsonPath().get("name").toString();
		String actualCampDescription2 = resp3.jsonPath().get("description").toString();
		String actualMiscellaneous2 = resp3.jsonPath().get("miscellaneous").toString();
		String actualGiftReason2 = resp3.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName2, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription2, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous2, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason2, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified the response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified the response returns the localized values for all fields in parity with the provided input");

		// api2/mobile/challenges/{challenge_campaign_id}
		Response resp4 = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "es");
		Assert.assertEquals(resp4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName3 = resp4.jsonPath().get("name").toString();
		String actualCampDescription3 = resp4.jsonPath().get("description").toString();
		String actualMiscellaneous3 = resp4.jsonPath().get("miscellaneous").toString();
		String actualGiftReason3 = resp4.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName3, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription3, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous3, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason3, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified the response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified the response returns the localized values for all fields in parity with the provided input");

		// api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}
		Response resp5 = pageObj.endpoints().apiPosChallenge(campaignID, userEmail, dataSet.get("locationKey"), "es");
		Assert.assertEquals(resp5.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName4 = resp5.jsonPath().get("name").toString();
		String actualCampDescription4 = resp5.jsonPath().get("description").toString();
		String actualMiscellaneous4 = resp5.jsonPath().get("miscellaneous").toString();
		String actualGiftReason4 = resp5.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName4, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription4, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous4, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason4, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified the response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified the response returns the localized values for all fields in parity with the provided input");

		// api/auth/challenges/{challenge_campaign_id}
		// passing Accept-language parameter es
		Response resp6 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, campaignID, "es");
		Assert.assertEquals(resp6.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName5 = resp6.jsonPath().get("name").toString();
		String actualCampDescription5 = resp6.jsonPath().get("description").toString();
		String actualMiscellaneous5 = resp6.jsonPath().get("miscellaneous").toString();
		String actualGiftReason5 = resp6.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName5, campaignNameEn, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription5, dataSet.get("descriptionEn"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous5, campaignNameEn, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason5, campaignNameEn, "Campaign gift reason did not match");
		logger.info(
				"Verified that Accepted-Language header contains the correct language code matching the configured localized language.");
		TestListeners.extentTest.get().pass(
				"Verified that Accepted-Language header contains the correct language code matching the configured localized language.");

		// passing Accept-language parameter fr
		Response resp7 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, campaignID, "fr");
		Assert.assertEquals(resp7.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName6 = resp7.jsonPath().get("name").toString();
		String actualCampDescription6 = resp7.jsonPath().get("description").toString();
		String actualMiscellaneous6 = resp7.jsonPath().get("miscellaneous").toString();
		String actualGiftReason6 = resp7.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName6, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription6, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous6, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason6, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified that Accepted-Language header contains the correct language code matching the configured localized language.");
		TestListeners.extentTest.get().pass(
				"Verified that Accepted-Language header contains the correct language code matching the configured localized language.");

		// delete campaign
		pageObj.campaignspage().removeSearchedCampaign(campaignNameEn);
	}

	@Test(description = "SQ-T5730 Test the complete workflow from configuring a localized challenge campaign to verifying the localized data across UI and API"
			+ "SQ-T5732 Verify Localization on POS"
			+ "SQ-T5731 Verify Localization in User Balance", groups = { "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5730_verifyLocalizedDataAcrossUIandAPI() throws Exception {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		String date = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String query = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertTrue(rs >= 0,
				"No challenge campaigns were deactivated or challenge campaign status is already inactive in table");
		utils.logit("Challenge campaigns deactivated successfully");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().alternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// verify New challenge campaign screen get displayed
		String pageTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		Assert.assertEquals(pageTitle, "New Challenge Campaign", "Page title did not matched");
		utils.logPass("New challenge campaign page title matched");

		String campaignNameEn = "AutomationChallengeCampaignEnglish" + CreateDateTime.getTimeDateString();
		String campaignNameFr = "AutomationChallengeCampaignFrench" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().createWhatDetailsChallengeCampaignInLocalisedlanguage(campaignNameEn,
				campaignNameFr, dataSet.get("descriptionEn"), dataSet.get("descriptionFr"), campaignNameEn,
				campaignNameFr, campaignNameEn, campaignNameFr);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignNameEn);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignNameEn);

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");
		String accessToken = signUp.jsonPath().get("access_token.token").toString();

		// user login
		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(login.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "login api not working");
		String authenticationToken = login.jsonPath().get("authentication_token").toString();

		// api/pos/challenges/{challenge_campaign_id}?email={email_id}&location_key={location_pos_key}
		Response resp1 = pageObj.endpoints().apiPosChallenge(campaignID, userEmail, dataSet.get("locationKey"), "fr");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName4 = resp1.jsonPath().get("name").toString();
		String actualCampDescription4 = resp1.jsonPath().get("description").toString();
		String actualMiscellaneous4 = resp1.jsonPath().get("miscellaneous").toString();
		String actualGiftReason4 = resp1.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName4, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription4, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous4, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason4, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api/pos/challenges/id response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api/pos/challenges/id response returns the localized values for all fields in parity with the provided input");

		// verify response for /api/mobile/challenges
		Response resp2 = pageObj.endpoints().apiMobileChallenge(dataSet.get("client"), dataSet.get("secret"),
				accessToken, "fr");
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName = resp2.jsonPath().get("challenges[0].name").toString();
		String actualCampDescription = resp2.jsonPath().get("challenges[0].description").toString();
		String actualMiscellaneous = resp2.jsonPath().get("challenges[0].miscellaneous").toString();
		String actualGiftReason = resp2.jsonPath().get("challenges[0].gift_reason").toString();

		Assert.assertEquals(actualCampName, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api/mobile/challenges response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api/mobile/challenges response returns the localized values for all fields in parity with the provided input");

		// api/mobile/challenges/{challenge_campaign_id}
		Response resp3 = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "fr");
		Assert.assertEquals(resp3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName2 = resp3.jsonPath().get("name").toString();
		String actualCampDescription2 = resp3.jsonPath().get("description").toString();
		String actualMiscellaneous2 = resp3.jsonPath().get("miscellaneous").toString();
		String actualGiftReason2 = resp3.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName2, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription2, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous2, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason2, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api/mobile/challenges/{challenge_campaign_id} response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api/mobile/challenges/{challenge_campaign_id} response returns the localized values for all fields in parity with the provided input");

		// api/auth/challenges
		Response resp4 = pageObj.endpoints().apiAuthChallenge(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, "fr");
		Assert.assertEquals(resp4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName1 = resp4.jsonPath().get("challenges[0].name").toString();
		String actualCampDescription1 = resp4.jsonPath().get("challenges[0].description").toString();
		String actualMiscellaneous1 = resp4.jsonPath().get("challenges[0].miscellaneous").toString();
		String actualGiftReason1 = resp4.jsonPath().get("challenges[0].gift_reason").toString();

		Assert.assertEquals(actualCampName1, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription1, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous1, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason1, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api/auth/challenges response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api/auth/challenges response returns the localized values for all fields in parity with the provided input");

		// passing Accept-language parameter fr
		Response resp5 = pageObj.endpoints().apiAuthChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				authenticationToken, campaignID, "fr");
		Assert.assertEquals(resp5.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName6 = resp5.jsonPath().get("name").toString();
		String actualCampDescription6 = resp5.jsonPath().get("description").toString();
		String actualMiscellaneous6 = resp5.jsonPath().get("miscellaneous").toString();
		String actualGiftReason6 = resp5.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName6, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription6, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous6, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason6, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api/auth/challenges/{challenge_campaign_id} Accepted-Language header contains the correct language code matching the configured localized language.");
		TestListeners.extentTest.get().pass(
				"Verified api/auth/challenges/{challenge_campaign_id} Accepted-Language header contains the correct language code matching the configured localized language.");

		// api2/mobile/challenges
		Response resp6 = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"), dataSet.get("secret"), "fr");
		Assert.assertEquals(resp6.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName5 = resp4.jsonPath().get("challenges[0].name").toString();
		String actualCampDescription5 = resp4.jsonPath().get("challenges[0].description").toString();
		String actualMiscellaneous5 = resp4.jsonPath().get("challenges[0].miscellaneous").toString();
		String actualGiftReason5 = resp4.jsonPath().get("challenges[0].gift_reason").toString();

		Assert.assertEquals(actualCampName5, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription5, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous5, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason5, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api2/mobile/challenges response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api2/mobile/challenges response returns the localized values for all fields in parity with the provided input");

		// api2/mobile/challenges/{challenge_campaign_id}
		Response resp7 = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				accessToken, campaignID, "fr");
		Assert.assertEquals(resp7.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api");
		String actualCampName3 = resp7.jsonPath().get("name").toString();
		String actualCampDescription3 = resp7.jsonPath().get("description").toString();
		String actualMiscellaneous3 = resp7.jsonPath().get("miscellaneous").toString();
		String actualGiftReason3 = resp7.jsonPath().get("gift_reason").toString();

		Assert.assertEquals(actualCampName3, campaignNameFr, "Campaign name did not match");
		Assert.assertEquals(actualCampDescription3, dataSet.get("descriptionFr"), "Campaign description did not match");
		Assert.assertEquals(actualMiscellaneous3, campaignNameFr, "Campaign Miscellaneous data did not match");
		Assert.assertEquals(actualGiftReason3, campaignNameFr, "Campaign gift reason did not match");
		logger.info(
				"Verified api2/mobile/challenges/{challenge_campaign_id} response returns the localized values for all fields in parity with the provided input");
		TestListeners.extentTest.get().pass(
				"Verified api2/mobile/challenges/{challenge_campaign_id} response returns the localized values for all fields in parity with the provided input");

	}

	// Rakhi
	@Test(description = "SQ-T5836 Verify that challenge campaign does not get appeared in dropdown if start and end date is set to past date in IST timezone"
			+ "SQ-T5838 Verify that On user's challenge campaign tab, list of challenges are getting displayed according to challenge timezone"
			+ "SQ-T5860 Verify that challenge campaign gets appeared in dropdown if start and end date is set to past date in PST timezone")
	@Owner(name = "Rakhi Rawat")
	public void T5836_VerifyChallengeCampaignAppearedInDropdown() throws Exception {

		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String date = CreateDateTime.getCurrentDate();

		// deactivate active challenge campaigns
		String deactivateQuery = "UPDATE `campaigns` SET `deactivated_at` = '" + date
				+ "' WHERE `type`= 'ChallengeCampaign' AND `business_id` = '" + dataSet.get("business_id") + "' ;";
		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
		Assert.assertTrue(rs >= 1, "No challenge campaigns were deactivated");
		utils.logit("Challenge campaigns deactivated successfully");

		// create challenge campaign with every x point
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String oneDayOldDate = CreateDateTime.getPreviousDate(1);
		String oneDayAheadDate = CreateDateTime.getFutureDateNew(1);
		String timezonePST = "America/Los_Angeles";

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and get campaign id
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		// update one day old start date and end date in db
		String query = "update campaigns set start_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query);

		String query2 = "update campaigns set end_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query2);

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift challenge progress to user from guest timeline
		boolean flag = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
				campaignName);
		Assert.assertFalse(flag, campaignName + " appeared in Challenge Campaigns dropdown");
		logger.info("Verified that " + campaignName + " did not appeared in Challenge Campaigns dropdown");
		TestListeners.extentTest.get()
				.pass("Verified that " + campaignName + " did not appeared in Challenge Campaigns dropdown");

		// update one day old start date and one day ahead end date in db
		String query3 = "update campaigns set start_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query3);

		String query4 = "update campaigns set end_date='" + oneDayAheadDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query4);

		String query5 = "update campaigns set timezone='" + timezonePST + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query5);

		utils.refreshPage();
		// gift challenge progress to user from guest timeline
		boolean flag1 = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
				campaignName);
		Assert.assertTrue(flag1, campaignName + " did not appeared in Challenge Campaigns dropdown");
		logger.info("Verified that " + campaignName + " appeared in Challenge Campaigns dropdown");
		TestListeners.extentTest.get()
				.pass("Verified that " + campaignName + " appeared in Challenge Campaigns dropdown");

		// Verify challenge campaign status in challenge tab
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		boolean flag2 = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "Active");
		Assert.assertTrue(flag2, "Challenge Campaign status did not match in challenges tab");
		utils.logPass("Verified that " + campaignName + " status is Active in challenges tab");

		// update one day old start date and end date in db , timezone will be PST
		String query6 = "update campaigns set start_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query6);

		String query7 = "update campaigns set end_date='" + oneDayOldDate + "' where id=" + campaignID;
		DBUtils.executeUpdateQuery(env, query7);

		String currentTime = CreateDateTime.getCurrentTimeInIST();
		logger.info("Current Time in IST: " + currentTime);
		TestListeners.extentTest.get().info("Current Time in IST: " + currentTime);

		utils.refreshPage();

		if (CreateDateTime.isBeforeGivenTime(12, 30)) {
			logger.info(
					"As test case is executed before 12:30 PM IST, we can confirm the campaign's visibility in the Gifting dropdown menu");
			TestListeners.extentTest.get().info(
					"As test case is executed before 12:30 PM IST, we can confirm the campaign's visibility in the Gifting dropdown menu");
			// gift challenge progress to user from guest timeline
			boolean flag3 = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
					campaignName);
			Assert.assertTrue(flag3, campaignName + " did not appeared in Challenge Campaigns dropdown");
			logger.info("Verified that " + campaignName + " appeared in Challenge Campaigns dropdown");
			TestListeners.extentTest.get()
					.pass("Verified that " + campaignName + " appeared in Challenge Campaigns dropdown");

		} else {
			logger.info(
					"As test case is executed after 12:30 PM IST, we can confirm the campaign's is not visibile in the Gifting dropdown menu");
			TestListeners.extentTest.get().info(
					"As test case is executed after 12:30 PM IST, we can confirm the campaign's is not visibile in the Gifting dropdown menu");

			// gift challenge progress to user from guest timeline
			boolean flag4 = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
					campaignName);
			Assert.assertFalse(flag4, campaignName + " appeared in Challenge Campaigns dropdown");
			logger.info("Verified that " + campaignName + " did not appeared in Challenge Campaigns dropdown");
			TestListeners.extentTest.get()
					.pass("Verified that " + campaignName + " did not appeared in Challenge Campaigns dropdown");
		}
		// delete camapigns
		pageObj.utils().deleteCampaignFromDb(campaignName, env);

	}

	@Test(description = "SQ-T6576 User verifies the functionality of challenge campaign having gift type as points and Challenge point for Receipt Qualification with Universal Auto enrolment ")
	public void T6576_verifyChallengeCampaignWithPointsGiftTypeAndReceiptQualificationForUniversalAutoEnrolment()
			throws Exception {
		updateChallengeFlag();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create challenge campaign with receipt qualification
		String campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// --------------- Segment Auto-enrollment Type--------------
		pageObj.signupcampaignpage().setCampaignName(campaignName);
//			pageObj.signupcampaignpage().setChallengeCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.newCamHomePage().challengeReachDrpDown("Universal Auto Enrolment");
		pageObj.newCamHomePage().challengeQualificationDrpDown(dataSet.get("qcName"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);

		// challenge availability schedule
//		pageObj.signupcampaignpage().setFrequency("Once");
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");

		String accessToken = signUp.jsonPath().get("access_token.token").toString();

		Response login = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(login.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "login api not working");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift challenge progress to user from guest timeline
		pageObj.guestTimelinePage().messageGiftChallengeProgressToUser("Challenge Progress", campaignName,
				dataSet.get("steps"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		utils.logPass("Verified that Success message of challenge progress send to user ");

		// Verify campaign status on guest timeline , challenge campaign
		boolean flag2 = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "campaign_completed");
		Assert.assertTrue(flag2, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// check gifting from challenge campaign
		boolean status1 = pageObj.guestTimelinePage().checkPointsGifting(campaignName, "500");
		Assert.assertTrue(status1, "Gifted points by challenge campaign did not appeared");
		utils.logPass("Verified user get the gifting from campaign -- " + campaignName);
		// delete campaigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// delete camapigns
		pageObj.campaignspage().removeSearchedCampaign(campaignName);

	}
	@Test(description = "SQ-T7048 Verify challenge progress starts after opt-in"
			+ "SQ-T7049 Verify challenge stops running after opt-out"
			+ "SQ-T7050 Verify challenge resumes after re-opt-in"
			+ "SQ-T7294: [Secure API] Verify opted_in is returned as false in API response if user has opted_out of the campaign; "
			+ "SQ-T7379: [Secure API] Verify user is opted_in for the campaign via opt_in API for new campaigns when Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled; "
			+ "SQ-T7383: [Secure API] Verify opt_out API returns error for old/new campaigns when user opts_out of the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> On and enable_optout_for_challenges -> Off]; "
			+ "SQ-T7380: [Secure API] Verify user is opted_out of campaign via opt_out API for new campaigns when Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled; "
			+ "SQ-T7390: [API2] Verify opt_out API returns error for old/new campaigns when user opts_out of the campaign [Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign -> On and enable_optout_for_challenges -> Off]; "
			+ "SQ-T7293: [API2] Verify opted_in is returned as false in API response if user has opted_out of the campaign")
	@Owner(name = "Rakhi Rawat")
	public void T7048_VerifyChallengeProgressAfterOptin() throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set has_challenges to true
		flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(flag1, dataSet.get("dbFlag1") + " value is not updated to true");
		// Set enable_optin_for_challenges to true
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag2"),
				businessId);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to true");
		// Set enable_optout_for_challenges to true
		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag3"),
				businessId);
		Assert.assertTrue(flag3, dataSet.get("dbFlag3") + " value is not updated to true");
		logger.info("Flags are updated to respective values");
		TestListeners.extentTest.get().info("Flags are updated to respective values");

		// Verify that the challenge campaign used is new
		String campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id",
				dataSet.get("newChallengeCampId"));
		String campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertTrue(campPrefValue.contains(dataSet.get("dbFlag4")),
				"Campaign preferences doesn't contains the explicit_opt_in flag");
		utils.logit("pass", "Verified that campaign is new as its preferences contains the explicit_opt_in flag");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		int validCampaignID = Integer.parseInt(dataSet.get("newChallengeCampId"));
		
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline and verify no progress before opt-in
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		String value= pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "0", "Challenge progress is displayed before opt-in");
		logger.info("Verified that no challenge progress is displayed before opt-in");
		TestListeners.extentTest.get().pass("Verified that no challenge progress is displayed before opt-in");
		
		// SQ-T7379/LPE-T3181. Set explicit_opt_in to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag4"), businessId);
		// Verify opt-in API for new campaign when explicit_opt_in and
		// enable_optin_for_challenges are true. Hit `api/mobile/challenge_opt_in`
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
		
		// Guest performs qualifying actions for the challenge (POS Checkin)
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		utils.refreshPage();
		// verify challenge progress after opt-in
		value= pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "10", "Challenge progress is not displayed after opt-in");
		logger.info("Verified that Challenge progress starts accumulating after opt-in.");
		TestListeners.extentTest.get().pass("Verified that Challenge progress starts accumulating after opt-in.");
		
		// SQ-T7383/LPE-T3198. Verify opt-out secure API errors for new campaign when
		// enable_optin_for_challenges and explicit_opt_in are true.
		// Set enable_optout_for_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);
		// Hit `/api/mobile/challenge_opt_out`
		response = pageObj.endpoints().Api1ChallengeOptOut(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for challenge opt out secure API");
		String errorMsgSecureOptOut = response.jsonPath().get("errors.challenges_opt_out_disabled");
		Assert.assertEquals(errorMsgSecureOptOut, MessagesConstants.challengesOptOutDisabledMsg,
				"Error message mismatch");
		utils.logPass(
				"Verified that opt-out secure API errors for new campaign while enable_optout_for_challenges is false and explicit_opt_in is true.");

		// SQ-T7390/LPE-T3203. Verify opt-out API2 errors for new campaign when
		// enable_optout_for_challenges is false and enable_optin_for_challenges
		// and explicit_opt_in are true. Hit `/api2/mobile/challenge_opt_out`
		Response optOutResponseAPI2 = pageObj.endpoints().api2ChallengeOptOut(dataSet.get("newChallengeCampId"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(optOutResponseAPI2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for challenge opt out API2");
		String errorMsgApi2OptOut = optOutResponseAPI2.jsonPath().getString("errors.challenges_opt_out_disabled");
		Assert.assertEquals(errorMsgApi2OptOut, MessagesConstants.challengesOptOutDisabledMsg,
				"Error message mismatch");
		utils.logPass(
				"Verified that opt-out API2 errors for new campaign while enable_optout_for_challenges is false.");

		// Revert enable_optout_for_challenges to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);

		// SQ-T7380/LPE-T3185. Verify opt-out API for new campaign when
		// explicit_opt_in and enable_optin_for_challenges are true.
		// Hit `api/mobile/challenge_opt_out`
		response = pageObj.endpoints().Api1ChallengeOptOut(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt out with valid campaign id");
		TestListeners.extentTest.get()
				.pass("Status code 200 did not matched for api challenge opt out with valid campaign id");
		Assert.assertEquals(response.jsonPath().get("message"),
				"You have successfully opted out of the challenge.",
				"Challenge opt out message not matched");
		utils.logPass("Challenge opt out message matched successfully");	
		
		// SQ-T7294/LPE-T3031 [Challenge Campaign type: Universal Auto Enrollment]
		// Verify opted_in is returned as false in API response if user has opted_out of
		// the campaign. Hit `/api/mobile/challenges`
		Response fetchCampDetailsResponse = pageObj.endpoints().api1fetchChallengeDetails(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("newChallengeCampId"), "es");
		Assert.assertEquals(fetchCampDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedIn = fetchCampDetailsResponse.jsonPath().getBoolean("opted_in");
		Assert.assertFalse(optedIn, "opted_in is not false for the user who has opted_out of the campaign");
		utils.logit(
				"Verified opted_in is returned as false in Fetch challenge details secure API response if user has opted_out of the campaign");

		// SQ-T7293/LPE-T3030
		// Verify opted_in is returned as false for new campaigns in API response if user
		// has opted_out of the campaign. Hit `/api2/mobile/challenges`
		Response fetchCampDetailsAPI2Response = pageObj.endpoints().fetchChallengeDetails(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("newChallengeCampId"), "es");
		Assert.assertEquals(fetchCampDetailsAPI2Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean optedInAPI2 = fetchCampDetailsAPI2Response.jsonPath().getBoolean("opted_in");
		Assert.assertFalse(optedInAPI2, "opted_in is not false for the user who has opted_out of the campaign");
		utils.logit(
				"Verified opted_in is returned as false in Fetch challenge details API2 response if user has opted_out of the campaign");

		// Guest performs qualifying actions for the challenge (POS Checkin)
		resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post checkin api");
		
		utils.refreshPage();
		// verify challenge progress after opt-in on timeline
		value= pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "10", "Challenge progress is not displayed after opt-in");
		logger.info("Verified that Challenge progress is still same after opt-out.");
		TestListeners.extentTest.get().pass("Verified that Challenge progress is still same after opt-out.");
		
		// verify challenge progress after opt-out
		String query = "SELECT progress_count FROM `challenge_milestones` WHERE `user_id`='" + userID + "' ORDER BY id ASC LIMIT 1;";
		String progress = DBUtils.executeQueryAndGetColumnValue(env, query, "progress_count");
		Assert.assertEquals(progress, "10", "progress_count did not match in DB after opt-out");
		logger.info("Verified that Challenge progress is retained after opt-out");
		TestListeners.extentTest.get().pass("Verified that Challenge progress is retained after opt-out");
		
		//re-optin
		response = pageObj.endpoints().Api1ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt in with valid campaign id");
		
		// Guest performs qualifying actions for the challenge (POS Checkin)
		resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post checkin api");
		
		utils.refreshPage();
		// verify challenge progress after re opt-in
		value = pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "20", "Challenge progress is not displayed after re opt-in");
		logger.info("Verified that Challenge progress resumes from the last recorded state after re opt-in.");
		TestListeners.extentTest.get().pass("Verified that Challenge progress resumes from the last recorded state after re opt-in.");
		
	}

	@Test(description = "SQ-T7051: Verify challenge behavior when opt-in flag is disabled; "
			+ "SQ-T7052: Verify challenge behavior when opt-in flag is enabled and user is not opted-in; "
			+ "SQ-T7382: [Secure API] Verify opt_out API returns error for old/new campaigns when user opts_out of the campaign [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign disabled]; "
			+ "SQ-T7389: [API2] Verify opt_out API returns error for old/new campaigns when user opts_out of the campaign [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign disabled]; " 
			+ "SQ-T7387: [API2] Verify user is opted_in for the campaign via opt_in API for new campaigns when Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled; " 
			+ "SQ-T7388: [API2] Verify user is opted_out of campaign via opt_out API for new campaigns when Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7363_verifyChallengeProgressAndOptOutApi() throws Exception {
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		// Set has_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		// Set enable_optout_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		// Set explicit_opt_in to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag4"), businessId);

		// User signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for API2 user signup");
		utils.logPass("API2 user signup is successful for user: " + userEmail);

		// SQ-T7052. User performs qualifying action for the challenge (POS Checkin)
		Response posCheckinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(posCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for POS checkin API");
		String posCheckinUserEmail = posCheckinResponse.jsonPath().getString("email");
		Assert.assertEquals(posCheckinUserEmail, userEmail, "User email mismatch in POS checkin response");
		utils.logPass("POS checkin of 10 points is successful for user");
		// Navigate to user timeline and verify no challenge progress
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		String value = pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "0", "Challenge progress is not 0");
		utils.logPass("Verified that Challenge campaign not triggered for the user as user has not opted-in yet.");

		// SQ-T7051. Set enable_optin_for_challenges to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag2"), businessId);
		// User performs qualifying action for the challenge (POS Checkin)
		posCheckinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(posCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for POS checkin API");
		posCheckinUserEmail = posCheckinResponse.jsonPath().getString("email");
		Assert.assertEquals(posCheckinUserEmail, userEmail, "User email mismatch in POS checkin response");
		utils.logPass("POS checkin of 10 more points is successful for user");
		// Navigate to user timeline and verify challenge progress
		utils.refreshPage();
		value = pageObj.guestTimelinePage().getChallengeCompletedSteps(dataSet.get("campaignName"));
		Assert.assertEquals(value, "10", "Challenge progress is not 10");
		utils.logPass(
				"Verified that Challenge campaign runs for all eligible guests and progress accumulates regardless of opt-in status.");

		// SQ-T7382/LPE-T3187. Set explicit_opt_in to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag4"), businessId);
		// Verify that opt-out secure API errors for new campaign
		// when enable_optin_for_challenges and explicit_opt_in are both false.
		// Hit `/api/mobile/challenge_opt_out`
		int validCampaignID = Integer.parseInt(dataSet.get("newChallengeCampId"));
		Response secureOptOutResponse = pageObj.endpoints().Api1ChallengeOptOut(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(secureOptOutResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for challenge opt out secure API");
		String errorMsgSecureOptOut = secureOptOutResponse.jsonPath().get("errors.challenges_opt_in_disabled");
		Assert.assertEquals(errorMsgSecureOptOut, MessagesConstants.challengesOptInDisabledMsg,
				"Error message mismatch");
		utils.logPass(
				"Verified that opt-out secure API errors for new campaign when enable_optin_for_challenges and explicit_opt_in are false.");

		// SQ-T7389/LPE-T3201. Verify that opt-out API2 errors for new campaign
		// when enable_optin_for_challenges and explicit_opt_in are false.
		// Hit `/api2/mobile/challenge_opt_out`
		Response optOutResponseAPI2 = pageObj.endpoints().api2ChallengeOptOut(dataSet.get("newChallengeCampId"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(optOutResponseAPI2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code mismatch for challenge opt out API2");
		String errorMsgApi2OptOut = optOutResponseAPI2.jsonPath().getString("errors.challenges_opt_in_disabled");
		Assert.assertEquals(errorMsgApi2OptOut, MessagesConstants.challengesOptInDisabledMsg, "Error message mismatch");
		utils.logPass(
				"Verified that opt-out API2 errors for new campaign when enable_optin_for_challenges and explicit_opt_in are false.");

		// SQ-T7387/LPE-T3189. Verify that opt-in API2 opts user in the new campaign
		// successfully when all flags are true.
		// Set enable_optin_for_challenges to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		// Set explicit_opt_in to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag4"), businessId);
		// Hit `api2/mobile/challenge_opt_in`
		Response optInResponseAPI2 = pageObj.endpoints().Api2ChallengeOptIn(validCampaignID, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(optInResponseAPI2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not matched for challenge opt in API2");
		String optInMsgApi2 = optInResponseAPI2.jsonPath().getString("message");
		Assert.assertEquals(optInMsgApi2, MessagesConstants.challengeCampaignJoinedMsg, "Success message mismatch");
		utils.logPass("Verified that opt-in API2 opts user in the new campaign successfully when all flags are true.");

		// SQ-T7388/LPE-T3199. Verify that opt-out API2 opts user out of new campaign
		// successfully when all flags are true.
		// Hit `/api2/mobile/challenge_opt_out`
		optOutResponseAPI2 = pageObj.endpoints().api2ChallengeOptOut(dataSet.get("newChallengeCampId"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(optOutResponseAPI2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for challenge opt out API2");
		String optOutMsgApi2 = optOutResponseAPI2.jsonPath().getString("message");
		Assert.assertEquals(optOutMsgApi2, MessagesConstants.challengeCampaignOptedOutMsg, "Success message mismatch");
		utils.logPass("Verified that opt-out API2 opts user out of new campaign successfully when all flags are true.");

	}

	@Test(description = "SQ-T7270 Verify Audit Log is available on challenge campaign card only if user is enrolled in campaign [Universal Auto Enrolment]"
			+ "SQ-T7271 Verify Audit Log is not available on challenge campaign card if user is not enrolled in campaign [Universal Auto Enrolment]"
			+ "SQ-T7285 Verify on click Audit Log link navigates to a new page with log entries of the specific campaign"
			+ "SQ-T7286 Verify Audit Log page is not accessible when \"Enable Opt-In for Challenges\" flag is disabled [enable_optin_for_challenges -> false and enable_optout_for_challenges -> true/false]"
			+ "SQ-T7289 Verify Audit log link is available on challenge campaign card only if user is opted_in/out of campaign and Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled"
			+ "SQ-T7288 Verify Audit log link is not available on challenge campaign card if user has never opted_in/out of campaign and Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled; "
			+ "SQ-T7391: Verify Opt In button is visible on challenge card if user has opted_out of campaign [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled]; "
			+ "SQ-T7392: Verify Opt out button is visible on challenge card if user has opted_in for the campaign and enable_optout_for_challenges is true [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled]; "
			+ "SQ-T7393: Verify Opt out button is not visible on challenge card if user has opted_in for the campaign but enable_optout_for_challenges is false [Enable Opt-In for Challenges and explicit_opt_in for challenge campaign enabled]; "
			+ "SQ-T7394: Verify Opt in/Opt out buttons are not visible on challenge card when Enable Opt-In for Challenges -> On and explicit_opt_in for challenge campaign / enable_optout_for_challenges -> false")
	@Owner(name = "Rakhi Rawat")
	public void T7270_VerifyAuditLogOnChallengeCampaign() throws Exception {

        // Update business preference flags		
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		// Set has_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		// Set enable_optout_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);
		
		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		int campId = Integer.parseInt(dataSet.get("newCampId"));

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// Verify that the challenge campaign used is new (it has explicit_opt_in: true)
		String campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id",
				dataSet.get("newCampId"));
		String campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertTrue(campPrefValue.contains(dataSet.get("dbFlag4") + ": true"),
				"Campaign preferences doesn't contains the explicit_opt_in flag with value true");
		utils.logPass("Verified that campaign is new as its preferences contains the explicit_opt_in flag with value true");

		// Verify Audit log when user is not enrolled in challenge campaign
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		boolean value = pageObj.guestTimelinePage().verifyChallengeCampaignAuditLog(dataSet.get("newCampName"),"Audit Log");
		Assert.assertFalse(value, "Audit log link is displayed when user is not enrolled in challenge campaign");
		utils.logit("pass", "Verified that Audit log is not displayed when user is not enrolled in challenge campaign");
		
		// api/mobile/challenge_opt_in
		Response response = pageObj.endpoints().Api1ChallengeOptIn(campId, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt in with valid campaign id");
		Assert.assertEquals(response.jsonPath().get("message"), "You have successfully joined the challenge.",
				"Challenge opt in message not matched");
		utils.logit("pass", "Api challenge opt in is successful for valid campaign id");

		utils.refreshPage();
		// SQ-T7392/LPE-T3207. Verify Opt-out button is visible for
		// the challenge campaign in which user has opted-in
		String btnText = pageObj.guestTimelinePage().verifyAvailableChallengeOptBtn(dataSet.get("newCampId"), "opt_out");
		Assert.assertEquals(btnText, "Opt out", "Button is not visible on the challenge card");
		utils.logPass("Verified Opt-out button is visible on challenge card after user has opted-in");
		// Verify Audit log when user is enrolled in challenge campaign
		value = pageObj.guestTimelinePage().verifyChallengeCampaignAuditLog(dataSet.get("newCampName"), "Audit Log");
		Assert.assertTrue(value, "Audit log link is not displayed for enrolled user");
		utils.logit("pass", "Verified that Audit log is displayed on challenge campaign for enrolled user");
		
		// click Audit log
		pageObj.guestTimelinePage().clickAuditLogInChallengeTab(dataSet.get("newCampName"), "Audit Log");
		utils.switchToWindow();
		
		String query = challengeEnrollQuery.replace("$table", "challenge_enrolments").replace("$column", "id, opted_in_at, created_at, updated_at").replace("$userId", userID);
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "id", "opted_in_at", "created_at", "updated_at" }); 
		String challengeEnrollmentId =values.get(0).get("id");
		Assert.assertNotNull(challengeEnrollmentId, "No challenge Enrollment Id fetched from DB");	
		
		String optedInRaw = values.get(0).get("opted_in_at");
		Assert.assertNotNull(optedInRaw, "No opted_in_at value fetched from DB");
		Assert.assertFalse(optedInRaw.isEmpty(), "Empty opted_in_at value fetched from DB");
		String optedIn = optedInRaw.split("\\.")[0] + " UTC";
		utils.logit("Opted_in_at value from DB: " + optedIn);
		
		String createdAtRaw = values.get(0).get("created_at");
		Assert.assertNotNull(createdAtRaw, "No created_at value fetched from DB");
		Assert.assertFalse(createdAtRaw.isEmpty(), "Empty created_at value fetched from DB");
		String createdAt = createdAtRaw.split("\\.")[0] + " UTC";
		
		String updatedAtRaw = values.get(0).get("updated_at");
		Assert.assertNotNull(updatedAtRaw, "No updated_at value fetched from DB");
		Assert.assertFalse(updatedAtRaw.isEmpty(), "Empty updated_at value fetched from DB");
		String updatedAt = updatedAtRaw.split("\\.")[0] + " UTC";
		
		Map<String, String> runtimeValues = new HashMap<>();
		runtimeValues.put("User", userID);
		runtimeValues.put("Id", challengeEnrollmentId);
		runtimeValues.put("Opted In At", optedIn);
		runtimeValues.put("Created At", createdAt);
		runtimeValues.put("Updated At", updatedAt);
		
		Map<String, String> expectedAuditValues = new LinkedHashMap<>();
		// Static values from test data
		expectedAuditValues.put("Business", businessId);
		expectedAuditValues.put("Campaign", dataSet.get("newCampId"));

		// Runtime values
		expectedAuditValues.putAll(runtimeValues);
		for (Map.Entry<String, String> entry : expectedAuditValues.entrySet()) {

			String section = entry.getKey();
			String expectedValue = entry.getValue();
			String actualValue = pageObj.mobileconfigurationPage().verifyUpdatedConfigInAuditLogs(section, "");
			utils.logit(
					"Audit Log Check -> " + section + " | Expected: " + expectedValue + " | Actual: " + actualValue);

			Assert.assertEquals(actualValue, expectedValue, "Audit log value did not match for " + section);
		}
		utils.logit("pass", "All audit log values matched successfully for challenge enrollment");
		
		// Set enable_optin_for_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag2"), businessId);
	
		// Verify Audit log when enable_optin_for_challenges false
		utils.switchToParentWindow();
		utils.refreshPage();
		value = pageObj.guestTimelinePage().verifyChallengeCampaignAuditLog(dataSet.get("newCampName"),
				"Audit Log");
		Assert.assertFalse(value, "Audit log link is displayed when user is not enrolled in challenge campaign");
		utils.logit("pass", "Verified that Audit log is not displayed when user is not enrolled in challenge campaign");
	
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		// Set enable_optin_for_challenges to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag2"), businessId);
		//Set explicit_opt_in to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag4"), businessId);
		// SQ-T7393/LPE-T3209. Set enable_optout_for_challenges to false
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);
		// Verify Opt-out button is not visible since enable_optout_for_challenges is false
		utils.refreshPage();
		btnText = pageObj.guestTimelinePage().verifyAvailableChallengeOptBtn(dataSet.get("newCampId"), "opt_out");
		Assert.assertEquals(btnText, "", "Button is visible on the challenge card");
		utils.logPass("Verified Opt-out button is NOT visible on challenge card as enable_optout_for_challenges is false");
		// Set enable_optout_for_challenges back to true
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag3"), businessId);

		// user opt out from challenge campaign
		response = pageObj.endpoints().Api1ChallengeOptOut(campId, dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api challenge opt out with valid campaign id");

		utils.refreshPage();
		// SQ-T7391/LPE-T3206. Verify Opt-in button is visible for
		// the challenge campaign in which user has opted-out
		btnText = pageObj.guestTimelinePage().verifyAvailableChallengeOptBtn(dataSet.get("newCampId"), "opt_in");
		Assert.assertEquals(btnText, "Opt In", "Button is not visible on the challenge card");
		utils.logPass("Verified Opt-in button is visible on challenge card after user has opted-out");
		// Verify Audit log when enable_optin_for_challenges,explicit_opt_in true and user opted out
		value = pageObj.guestTimelinePage().verifyChallengeCampaignAuditLog(dataSet.get("newCampName"), "Audit Log");
		Assert.assertTrue(value, "Audit log link is not displayed when user is opted out of challenge campaign");
		utils.logit("pass", "Verified that Audit log is displayed even when user is opted out of challenge campaign");

		// second user signup
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// navigate to second user's timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		
		//Verify Audit log when enable_optin_for_challenges,explicit_opt_in true and user never opted in or opted out
		value = pageObj.guestTimelinePage().verifyChallengeCampaignAuditLog(dataSet.get("newCampName"),
				"Audit Log");
		Assert.assertFalse(value, "Audit log link is displayed even when user never opted in or opted out in challenge campaign");
		utils.logit("pass",
				"Verified that Audit log is not displayed when user never opted in or opted out in challenge campaign");

		// Verify that the challenge campaign used is old (it does not have explicit_opt_in key)
		campPreferencesQuery = campaignPreferencesQuery.replace("$campaign_id", dataSet.get("oldCampId"));
		campPrefValue = DBUtils.executeQueryAndGetColumnValue(env, campPreferencesQuery, "preferences");
		Assert.assertFalse(campPrefValue.contains(dataSet.get("dbFlag4")),
				"Campaign preferences contains the explicit_opt_in flag");
		utils.logPass("Verified that campaign is old as its preferences doesn't contain the explicit_opt_in flag.");

		// SQ-T7394/LPE-T3211. Set enable_optout_for_challenges to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag3"), businessId);
		// Verify opt-in/opt-out buttons are not visible on challenge card
		// when both flags are false and user has already opted-in the campaign.
		utils.logInfo("User is already enrolled as it's an old campaign");
		utils.refreshPage();
		btnText = pageObj.guestTimelinePage().verifyAvailableChallengeOptBtn(dataSet.get("oldCampId"), "opt_in");
		Assert.assertTrue(btnText.isEmpty(), "Button is visible on the challenge card");
		btnText = pageObj.guestTimelinePage().verifyAvailableChallengeOptBtn(dataSet.get("oldCampId"), "opt_out");
		Assert.assertTrue(btnText.isEmpty(), "Button is visible on the challenge card");
		utils.logPass("Verified opt-in/opt-out buttons are not visible on challenge card when "
						+ "enable_optout_for_challenges: false and explicit_opt_in:false for campaign.");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
