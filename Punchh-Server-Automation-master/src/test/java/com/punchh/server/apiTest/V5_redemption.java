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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class V5_redemption {

	private static Logger logger = LogManager.getLogger(V5_redemption.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "api";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	// @SuppressWarnings("unused")
	@Test(description = "SQ-T2909 - V5 redemption", groups = "api", priority = 0)
	public void T2909_V5_Redemption() throws InterruptedException {
		TestListeners.extentTest.get().info("=== V5 redemption via Redeemable ===");
		logger.info("=== V5 redemption via Redeemable ===");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");
		// V5 Redemption using redeemable_id API
		Response v5RedemptionWithRedemptionId = pageObj.endpoints().v5RedemptionWithRedemptionId(userEmail,
				dataSet.get("locationKey"), dataSet.get("redeemable_id"));
		Assert.assertEquals(v5RedemptionWithRedemptionId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the V5 Redemption API");
		boolean isRedemptionWithRedemptionIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV5RedemptionSchema, v5RedemptionWithRedemptionId.asString());
		Assert.assertTrue(isRedemptionWithRedemptionIdSchemaValidated,
				"API V5 Redemption With Redeemable Id Schema Validation failed");
		TestListeners.extentTest.get().pass("Redemption using V5-redemption is successful");
		logger.info("Redemption using V5-redemption is successful");

		// V5 Redemption using Redeemable with invalid user email
		TestListeners.extentTest.get().info("== V5 Redemption using Redeemable with invalid user email ==");
		logger.info("== V5 Redemption using Redeemable with invalid user email ==");
		Response redemptionWithRedemptionInvalidEmailResponse = pageObj.endpoints().v5RedemptionWithRedemptionId("1",
				dataSet.get("locationKey"), dataSet.get("redeemable_id"));
		Assert.assertEquals(redemptionWithRedemptionInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isRedemptionWithRedemptionInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionWithRedemptionInvalidEmailResponse.asString());
		Assert.assertTrue(isRedemptionWithRedemptionInvalidEmailSchemaValidated,
				"API V5 Redemption using Redeemable with invalid user email Schema Validation failed");
		String redemptionWithRedemptionInvalidEmailMsg = redemptionWithRedemptionInvalidEmailResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(redemptionWithRedemptionInvalidEmailMsg, "Email is invalid", "Message does not match");
		TestListeners.extentTest.get().pass("V5 Redemption using Redeemable with invalid user email is unsuccessful");
		logger.info("V5 Redemption using Redeemable with invalid user email is unsuccessful");

		Utilities.longWait(6);
		// V5 Redemption using Redeemable with missing redeemable id
		TestListeners.extentTest.get().info("== V5 Redemption using Redeemable with missing redeemable id ==");
		logger.info("== V5 Redemption using Redeemable with missing redeemable id ==");
		Response redemptionWithRedemptionMissingRedemptionIdResponse = pageObj.endpoints()
				.v5RedemptionWithRedemptionId(userEmail, dataSet.get("locationKey"), "");
		Assert.assertEquals(redemptionWithRedemptionMissingRedemptionIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isRedemptionWithRedemptionMissingRedemptionIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema,
				redemptionWithRedemptionMissingRedemptionIdResponse.asString());
		Assert.assertTrue(isRedemptionWithRedemptionMissingRedemptionIdSchemaValidated,
				"API V5 Redemption using Redeemable with missing redeemable id Schema Validation failed");
		String redemptionWithRedemptionMissingRedemptionIdMsg = redemptionWithRedemptionMissingRedemptionIdResponse
				.jsonPath().getString("[0]");
		String expectedRedemptionWithRedemptionMissingRedemptionIdMsg = "Another transaction is currently accessing the same code. Please try after some time.";
		Assert.assertEquals(redemptionWithRedemptionMissingRedemptionIdMsg,
				expectedRedemptionWithRedemptionMissingRedemptionIdMsg, "Message does not match");
		TestListeners.extentTest.get()
				.pass("V5 Redemption using Redeemable with missing redeemable id is unsuccessful");
		logger.info("V5 Redemption using Redeemable with missing redeemable id is unsuccessful");

	}

	// @SuppressWarnings("unused")
	@Test(description = "SQ-T2909 - V5 redemption via Reward", groups = "api", priority = 1)
	public void T2909_V5_RedemptionViaReward() throws InterruptedException {
		// User register/signup using API2 Signup
		TestListeners.extentTest.get().info("==== V5 redemption via Reward ====");
		logger.info("==== V5 redemption via Reward_id ====");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		logger.info("Api2 user signup is successful");
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");
		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");
		// V5 Redemption using reward_id API
		Response v5RedemptionWithRewardId = pageObj.endpoints().v5RedemptionWithRewardId(userEmail,
				dataSet.get("locationKey"), reward_id);
		Assert.assertEquals(v5RedemptionWithRewardId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the V5 Redemption API");
		boolean isRedemptionWithRewardIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV5RedemptionSchema, v5RedemptionWithRewardId.asString());
		Assert.assertTrue(isRedemptionWithRewardIdSchemaValidated,
				"API V5 Redemption using Reward Id Schema Validation failed");
		TestListeners.extentTest.get().pass("Redemption using V5-redemption is successful");
		logger.info("Redemption using V5-redemption via reward_id is successful");

		// V5 Redemption using Reward with invalid user email
		TestListeners.extentTest.get().info("== V5 Redemption using Reward with invalid user email ==");
		logger.info("== V5 Redemption using Reward with invalid user email ==");
		Response redemptionWithRewardInvalidEmailResponse = pageObj.endpoints().v5RedemptionWithRewardId("1",
				dataSet.get("locationKey"), reward_id);
		Assert.assertEquals(redemptionWithRewardInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isRedemptionWithRewardInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionWithRewardInvalidEmailResponse.asString());
		Assert.assertTrue(isRedemptionWithRewardInvalidEmailSchemaValidated,
				"API V5 Redemption using Reward with invalid user email Schema Validation failed");
		String redemptionWithRewardInvalidEmailMsg = redemptionWithRewardInvalidEmailResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(redemptionWithRewardInvalidEmailMsg, "Email is invalid", "Message does not match");
		TestListeners.extentTest.get().pass("V5 Redemption using Reward with invalid user email is unsuccessful");
		logger.info("V5 Redemption using Reward with invalid user email is unsuccessful");

		// V5 Redemption using Reward with missing reward id
		TestListeners.extentTest.get().info("== V5 Redemption using Reward with missing reward id ==");
		logger.info("== V5 Redemption using Reward with missing reward id ==");
		Response redemptionWithRewardMissingRewardIdResponse = pageObj.endpoints().v5RedemptionWithRewardId(userEmail,
				dataSet.get("locationKey"), "");
		Assert.assertEquals(redemptionWithRewardMissingRewardIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isRedemptionWithRewardMissingRewardIdSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, redemptionWithRewardMissingRewardIdResponse.asString());
		Assert.assertTrue(isRedemptionWithRewardMissingRewardIdSchemaValidated,
				"API V5 Redemption using Reward with missing reward id Schema Validation failed");
		String redemptionWithRewardMissingRewardIdMsg = redemptionWithRewardMissingRewardIdResponse.jsonPath()
				.getString("[0]");
		String expectedRedemptionWithRewardMissingRewardIdMsg = "Another transaction is currently accessing the same code. Please try after some time.";
		Assert.assertEquals(redemptionWithRewardMissingRewardIdMsg, expectedRedemptionWithRewardMissingRewardIdMsg,
				"Message does not match");
		TestListeners.extentTest.get().pass("V5 Redemption using Reward with missing reward id is unsuccessful");
		logger.info("V5 Redemption using Reward with missing reward id is unsuccessful");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
	}

}
