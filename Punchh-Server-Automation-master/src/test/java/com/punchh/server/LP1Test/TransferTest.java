package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
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

import com.aventstack.extentreports.ExtentTest;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class TransferTest {
	static Logger logger = LogManager.getLogger(TransferTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String run = "ui";
	private String env;
	private static Map<String, String> dataSet;
	String userEmail;
	ApiUtils apiUtils;
	Properties uiProp;
	private ExtentTest pass;

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
		Utilities.loadPropertiesFile("segmentBeta.properties");
		new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		uiProp = Utilities.loadPropertiesFile("config.properties");
		apiUtils = new ApiUtils();
	}

	@Test(description = "Flag Enabled for Transfer")

	public void TransferFlagOn() throws InterruptedException {
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.cockpitTransferPage().transferFlags("check");
		pageObj.cockpitTransferPage().pointexpirystrategy(1);

		// User sign-up for user #1
		pageObj.utils().logit("== Mobile API v1: User #1 sign-up ==");
		logger.info("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String fName1 = signUpResponse1.jsonPath().get("first_name").toString();
		String lName1 = signUpResponse1.jsonPath().get("last_name").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		pageObj.utils().logPass("API v1 user #1 signup is successful with user id: " + userID1);
		logger.info("API v1 user #1 signup is successful with user id: " + userID1);

		// User sign-up for user #2
		pageObj.utils().logit("== Mobile API v1: User #2 sign-up ==");
		logger.info("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		String userID2 = signUpResponse2.jsonPath().get("id").toString();
		pageObj.utils().logPass("API v1 user #2 signup is successful with user id: " + userID2);
		logger.info("API v1 user #2 signup is successful with user id: " + userID2);

		// Send points to user #1
		pageObj.utils().logit("== Platform Functions: Send reward amount to user #1 ==");
		logger.info("== Platform Functions: Send points to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "",
				"", "", dataSet.get("points"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send points to user");
		pageObj.utils().logPass("Send points to user #1 is successful");
		logger.info("Send points to user #1 is successful");

		// Loyalty points transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Loyalty points transfer to user #2 ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 ==");
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("points"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsResponse.asString());
		Assert.assertTrue(isApi1TransferPointsSchemaValidated,
				"API v1 Transfer Loyalty Points Schema Validation failed");
		String actualTransferPointsMsg = transferPointsResponse.jsonPath().get("[0]").toString();
		String expectedTransferPointsMsg = dataSet.get("points") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualTransferPointsMsg.toLowerCase(), expectedTransferPointsMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("API v1 Loyalty points transfer to user #2 is successful");
		logger.info("API v1 Loyalty points transfer to user #2 is successful");

		// Send reward amount to user #1
		pageObj.utils().logit("== Platform Functions: Send reward amount to user #1 ==");
		logger.info("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		pageObj.utils().logPass("Send reward amount to user #1 is successful");
		logger.info("Send reward amount to user #1 is successful");

		// Get Reward Id for user #1
		pageObj.utils().logit("== Auth API: Get Reward Id for user #1 ==");
		logger.info("== Auth API: Get Reward Id for user #1 ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logit("Reward Id for user #1 is fetched: " + rewardId);
		logger.info("Reward Id for user #1 is fetched: " + rewardId);

		// Reward Transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Reward Transfer to user #2 ==");
		logger.info("== Mobile API v1: Reward Transfer to user #2 ==");
		Response rewardsTransferResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(dataSet.get("client"),
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferResponse.asString());
		Assert.assertTrue(isApi1RewardTransferSchemaValidated, "API1 Reward Transfer Schema Validation failed");
		String actualRewardTransferMsg = rewardsTransferResponse.jsonPath().getString("[0]");
		String expectedRewardTransferMsg = dataSet.get("rewardName") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualRewardTransferMsg.toLowerCase(), expectedRewardTransferMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("API Reward Transfer to user #2 is successful");
		logger.info("API v1 Reward Transfer to user #2 is successful");

		// Currency Transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Currency Transfer to user #2 ==");
		logger.info("== Mobile API v1: Currency Transfer to user #2 ==");
		Response currencyTransferResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferSchemaValidated, "API1 Currency Transfer Schema Validation failed");
		String actualCurrencyTransferMsg = currencyTransferResponse.jsonPath().getString("[0]");
		String expectedCurrencyTransferMsg = "$" + dataSet.get("amount") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualCurrencyTransferMsg.toLowerCase(), expectedCurrencyTransferMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("API v1 Currency Transfer to user #2 is successful");
		logger.info("API v1 Currency Transfer to user #2 is successful");


		// Currency Transfer to user #2 w/o amount balance
		pageObj.utils().logit("== Mobile API v1: Currency Transfer to user #2 ==");
		logger.info("== Mobile API v1: Currency Transfer to user #2 ==");
		Response currencyTransferResponse1 = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferSchemaValidated1 = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferResponse1.asString());
		Assert.assertTrue(isApi1CurrencyTransferSchemaValidated1, "API1 Currency Transfer Schema Validation failed");
		String actualCurrencyTransferMsg1 = currencyTransferResponse1.jsonPath().getString("[0]");
		String expectedCurrencyTransferMsg1 = "User does not have sufficient balance to transfer $"
				+ dataSet.get("amount");
		Assert.assertEquals(actualCurrencyTransferMsg1.toLowerCase(), expectedCurrencyTransferMsg1.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Does not have balance");
		logger.info("Does not have balance");


		// Loyalty points transfer to user #2 w/o point balance
		pageObj.utils().logit("== Mobile API v1: Loyalty points transfer to user #2 ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 ==");
		Response transferPointsResponse1 = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("points"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsSchemaValidated1 = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsResponse1.asString());
		Assert.assertTrue(isApi1TransferPointsSchemaValidated1,
				"API v1 Transfer Loyalty Points Schema Validation failed");
		String actualTransferPointsMsg1 = transferPointsResponse1.jsonPath().get("[0]").toString();
		String expectedTransferPointsMsg1 = "User does not have sufficient balance to transfer "
				+ dataSet.get("points") + " points";
		Assert.assertEquals(actualTransferPointsMsg1.toLowerCase(), expectedTransferPointsMsg1.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Does not have balance");
		logger.info("Does not have balance");

		// Reward Transfer to user #2 w/o reward
		pageObj.utils().logit("== Mobile API v1: Reward Transfer to user #2 ==");
		logger.info("== Mobile API v1: Reward Transfer to user #2 ==");
		Response rewardsTransferResponse1 = pageObj.endpoints().Api1GiftRewardToOtherUser(dataSet.get("client"),
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferSchemaValidated1 = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferResponse1.asString());
		Assert.assertTrue(isApi1RewardTransferSchemaValidated1, "API1 Reward Transfer Schema Validation failed");
		String actualRewardTransferMsg1 = rewardsTransferResponse1.jsonPath().getString("[0]");
		String expectedRewardTransferMsg1 = "Reward is either redeemed or does not exist";
		Assert.assertEquals(actualRewardTransferMsg1.toLowerCase(), expectedRewardTransferMsg1.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Reward is either redeemed or does not exist");
		logger.info("Reward is either redeemed or does not exist");

	}

	@Test(description = "Flag disabled for Point Transfer")

	public void transferFlagOff() throws InterruptedException {
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.cockpitTransferPage().transferFlags("uncheck");
		pageObj.cockpitTransferPage().pointexpirystrategy(5);

		// User sign-up for user #1
		pageObj.utils().logit("== Mobile API v1: User #1 sign-up ==");
		logger.info("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String fName1 = signUpResponse1.jsonPath().get("first_name").toString();
		String lName1 = signUpResponse1.jsonPath().get("last_name").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		pageObj.utils().logPass("API v1 user #1 signup is successful with user id: " + userID1);
		logger.info("API v1 user #1 signup is successful with user id: " + userID1);

		// User sign-up for user #2
		pageObj.utils().logit("== Mobile API v1: User #2 sign-up ==");
		logger.info("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		String userID2 = signUpResponse2.jsonPath().get("id").toString();
		pageObj.utils().logPass("API v1 user #2 signup is successful with user id: " + userID2);
		logger.info("API v1 user #2 signup is successful with user id: " + userID2);

		// Send points to user #1
		pageObj.utils().logit("== Platform Functions: Send points to user #1 ==");
		logger.info("== Platform Functions: Send points to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "",
				"", "", dataSet.get("points"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send points to user");
		pageObj.utils().logPass("Send points to user #1 is successful");
		logger.info("Send points to user #1 is successful");

		// Loyalty points transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Loyalty points transfer to user #2 ==");
		logger.info("== Mobile API v1: Loyalty points transfer to user #2 ==");
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("points"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsResponse.asString());
		Assert.assertTrue(isApi1TransferPointsSchemaValidated,
				"API v1 Transfer Loyalty Points Schema Validation failed");
		String actualTransferPointsMsg = transferPointsResponse.jsonPath().get("[0]").toString();
		String expectedTransferPointsMsg = "Sorry! Your request cannot be completed as business does not support this.";
		Assert.assertEquals(actualTransferPointsMsg.toLowerCase(), expectedTransferPointsMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Feature flag is disabled");
		logger.info("Feature flag is disabled");

		// Send reward amount to user #1
		pageObj.utils().logit("== Platform Functions: Send reward amount to user #1 ==");
		logger.info("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse11 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendMessageToUserResponse11.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		pageObj.utils().logPass("Send reward amount to user #1 is successful");
		logger.info("Send reward amount to user #1 is successful");

		// Get Reward Id for user #1
		pageObj.utils().logit("== Auth API: Get Reward Id for user #1 ==");
		logger.info("== Auth API: Get Reward Id for user #1 ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logit("Reward Id for user #1 is fetched: " + rewardId);
		logger.info("Reward Id for user #1 is fetched: " + rewardId);

		// Reward Transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Reward Transfer to user #2 ==");
		logger.info("== Mobile API v1: Reward Transfer to user #2 ==");
		Response rewardsTransferResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(dataSet.get("client"),
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferResponse.asString());
		Assert.assertTrue(isApi1RewardTransferSchemaValidated, "API1 Reward Transfer Schema Validation failed");
		String actualRewardTransferMsg = rewardsTransferResponse.jsonPath().getString("[0]");
		String expectedRewardTransferMsg = "Sorry! Your request cannot be completed as business does not support this.";
		Assert.assertEquals(actualRewardTransferMsg.toLowerCase(), expectedRewardTransferMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Feature flag is disabled");
		logger.info("Feature flag is disabled");

		// Currency Transfer to user #2
		pageObj.utils().logit("== Mobile API v1: Currency Transfer to user #2 ==");
		logger.info("== Mobile API v1: Currency Transfer to user #2 ==");
		Response currencyTransferResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 200 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferSchemaValidated, "API1 Currency Transfer Schema Validation failed");
		String actualCurrencyTransferMsg = currencyTransferResponse.jsonPath().getString("[0]");
		String expectedCurrencyTransferMsg = "Sorry! Your request cannot be completed as business does not support this.";
		Assert.assertEquals(actualCurrencyTransferMsg.toLowerCase(), expectedCurrencyTransferMsg.toLowerCase(),
				"Message did not match");
		pageObj.utils().logPass("Feature flag is disabled");
		logger.info("Feature flag is disabled");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}