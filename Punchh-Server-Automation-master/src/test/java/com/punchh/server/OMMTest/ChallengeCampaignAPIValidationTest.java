package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ChallengeCampaignAPIValidationTest {

	private static Logger logger = LogManager.getLogger(ChallengeCampaignAPIValidationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private static Map<String, String> dataSet;
	String run = "ui";
	String userEmail, businessId;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		businessId = dataSet.get("business_id");
	}

    @Test(
        description = "SQ-T3304 Verify API parameter validations related to challenge_campaign_id and progress_count",
        groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3304_challengCampaignIdAPIValidations() throws Exception {
    	// Pre-requisite: Create/Have mentioned Challenge Campaigns in business

    	// Set has_challenges to true
    	String businessesQuery = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", "has_challenges", businessId);

		// User SignUp using API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

//		Step-1
		// Send message api send challenge campaign to user when challenge_campaign_id
		// parameter is not sent choice, userID, authToken, challenge_campaign_id,
		// progress_count

		Response sendRewardResponse = pageObj.endpoints().API2SendMessageToUserChallengeCampaign(
				"withoutChallengeCampaign", userID, dataSet.get("apiKey"), "", "1");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse.jsonPath().get("errors.base[0]"),
				"There must be a purpose to this note to the guest");
		utils.logPass(
				"Send message api send challenge campaign to user is unsuccessfully (expected) when challenge_campaign_id parameter is not sent");

//		Step-5
		// Send message api send challenge campaign to user when challenge_campaign_id
		// of Drafted challenge campaign is sent
		Response sendRewardResponse4 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121031", "1");
		Assert.assertEquals(sendRewardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse4.jsonPath().get("errors.base[0]"),
				"Sorry. Either this is an invalid campaign ID or the campaign has a start date in future, is a draft, is deactivated or expired.");
		utils.logPass(
				"Send message api send challenge campaign to user is unsuccessfully (expected) when challenge_campaign_id of Drafted challenge campaign is sent");

//		Step-9
		// Send message api send challenge campaign to user when challenge_campaign_id
		// with Future Date challenge campaign is sent
		Response sendRewardResponse6 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121034", "1");
		Assert.assertEquals(sendRewardResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse6.jsonPath().get("errors.base[0]"),
				"Sorry. Either this is an invalid campaign ID or the campaign has a start date in future, is a draft, is deactivated or expired.");
		utils.logPass(
				"Send message api send challenge campaign to user is unsuccessfully (expected) when challenge_campaign_id with Future Date challenge campaign is sent");

//		Step-10
		// Send message api send "challenge_campaign_id" sent in API is of an Active
		// campaign and "Challenge type -> Segment" and user present / not present in
		// attached segment
		Response sendRewardResponse2 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121035", "1");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse2.jsonPath().get("errors.base[0]"),
				"Sorry. Either this is an invalid campaign ID or the campaign has a start date in future, is a draft, is deactivated or expired.");
		utils.logPass(
				"Send message api send challenge campaign to user is unsuccessfully (expected) when challenge_campaign_id with Future Date challenge campaign is sent");

//		step-11
		// Send message api send "challenge_campaign_id" sent in API is of an Active
		// campaign and "Challenge type -> Receipt Qualification"
		Response sendRewardResponse3 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121036", "9");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send challenge campaign to user");
		utils.logPass(
				"Send message api send \"challenge_campaign_id\" sent in API is of an Active campaign and \"Challenge type -> Receipt Qualification\"");

//		Step-13
		// Send message api send User already completed a non-recurring campaign and
		// same campaign id sent in API again
		Response sendRewardResponse7 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121036", "3");
		Assert.assertEquals(sendRewardResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse7.jsonPath().get("errors.base[0]"),
				"Sorry. The guest seems to have already completed the challenge or is not enrolled in it.");
		utils.logPass(
				"Send message api send User already completed a non-recurring campaign and same campaign id sent in API again ");

//		Step-15
		// Send message api send Campaign with Challenge reach -> Segment Auto Enrolment
		// and future Start date and user is present in user_feature_enrollments
		Response sendRewardResponse8 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121031", "3");
		Assert.assertEquals(sendRewardResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse8.jsonPath().get("errors.base[0]"),
				"Sorry. Either this is an invalid campaign ID or the campaign has a start date in future, is a draft, is deactivated or expired.");
		utils.logPass(
				"Send message api send Campaign with Challenge reach -> Segment Auto Enrolment and future Start date and user is present in user_feature_enrollments ");

//		Step-16
		// Send message api progress_count" parameter not sent with campaign id
		Response sendRewardResponse9 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121036", "");
		Assert.assertEquals(sendRewardResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse9.jsonPath().get("errors.progress_count[0]"),
				"Progress count is not a number");
		utils.logPass(
				"Send message api send Campaign with Challenge reach -> Segment Auto Enrolment and future Start date and user is present in user_feature_enrollments ");

		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "has_challenges", businessId);

//		step-24
		// Send message api "Enable Challenges" Off in Business
		Response sendRewardResponse11 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), "121036", "1");
		Assert.assertEquals(sendRewardResponse11.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 send challenge campaign to user");
		Assert.assertEquals(sendRewardResponse11.jsonPath().get("errors"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		utils.logPass("Send message api \"Enable Challenges\" Off  in Business");

		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", "has_challenges", businessId);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
	}

}
