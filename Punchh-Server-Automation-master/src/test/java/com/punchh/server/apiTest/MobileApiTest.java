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
public class MobileApiTest {

	private static Logger logger = LogManager.getLogger(MobileApiTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2375 Verify redemption using redeemable", groups = "api", priority = 0)
	public void verify_Redemption_Using_Redeemable() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		// Validate token and userID are not null/empty
		Assert.assertNotNull(token, "Access token should not be null");
		Assert.assertFalse(token.isEmpty(), "Access token should not be empty");
		Assert.assertNotNull(userID, "User ID should not be null");
		Assert.assertFalse(userID.isEmpty(), "User ID should not be empty");
		// Validate user email in response matches input
		String responseEmail = signUpResponse.jsonPath().get("user.email").toString();
		Assert.assertEquals(responseEmail, userEmail, "User email in response should match the input email");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Create Redemption using "redeemable" (fetch redemption code)
		Response redeemableResponse = pageObj.endpoints().Api2RedemptionWitReedemable_id(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using redeemable");
		boolean isCreateRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, redeemableResponse.asString());
		Assert.assertTrue(isCreateRedemptionSchemaValidated,
				"API v2 Create Redemption using redeemable Schema Validation failed");

		// Validate redemption_tracking_code
		String redemptionTrackingCode = redeemableResponse.jsonPath().getString("redemption_tracking_code");
		Assert.assertNotNull(redemptionTrackingCode, "Redemption tracking code should not be null");
		Assert.assertFalse(redemptionTrackingCode.isEmpty(), "Redemption tracking code should not be empty");
		Assert.assertEquals(redemptionTrackingCode.length(), 8, "Redemption tracking code should be 8 digits");

		// Validate redemption_id
		int redemptionId = redeemableResponse.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");

		// Validate redemption_status
		String redemptionStatus = redeemableResponse.jsonPath().getString("redemption_status");
		Assert.assertEquals(redemptionStatus, "redeemable", "Redemption status should be 'redeemable'");

		// Validate location_id
		int locationId = redeemableResponse.jsonPath().getInt("location_id");
		Assert.assertTrue(locationId > 0, "Location ID should be greater than 0");

		// Validate redeemable_id
		int redeemableId = redeemableResponse.jsonPath().getInt("redeemable_id");
		Assert.assertTrue(redeemableId > 0, "Redeemable ID should be greater than 0");

		// Validate redeemed_value
		double redeemedValue = redeemableResponse.jsonPath().getDouble("redeemed_value");
		Assert.assertTrue(redeemedValue > 0, "Redeemed value should be greater than 0");

		// Validate redeemable_name
		String redeemableName = redeemableResponse.jsonPath().getString("redeemable_name");
		Assert.assertNotNull(redeemableName, "Redeemable name should not be null");
		Assert.assertFalse(redeemableName.isEmpty(), "Redeemable name should not be empty");

		// Validate timestamps
		String createdAt = redeemableResponse.jsonPath().getString("created_at");
		Assert.assertNotNull(createdAt, "Created_at timestamp should not be null");
		String expiringAt = redeemableResponse.jsonPath().getString("expiring_at");
		Assert.assertNotNull(expiringAt, "Expiring_at timestamp should not be null");

		pageObj.utils().logit("pass", "Api2 Create Redemption using redeemable is successful. Tracking Code: "
				+ redemptionTrackingCode + ", Redemption ID: " + redemptionId);
	}

	@Test(description = "SQ-T2376 Verify redemption using visits", groups = "api", priority = 1)
	public void verify_Redemption_Using_Visits() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Create Redemption using "visits" (fetch redemption code)
		Response visitsResponse = pageObj.endpoints().Api2RedemptionWithVisit(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(visitsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using visits");
		boolean isCreateRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2CreateRedemptionSchema, visitsResponse.asString());
		Assert.assertTrue(isCreateRedemptionSchemaValidated,
				"API v2 Create Redemption using visits Schema Validation failed");

		// Validate redemption response fields
		String redemptionTrackingCode = visitsResponse.jsonPath().getString("redemption_tracking_code");
		Assert.assertNotNull(redemptionTrackingCode, "Redemption tracking code should not be null");
		Assert.assertFalse(redemptionTrackingCode.isEmpty(), "Redemption tracking code should not be empty");

		int redemptionId = visitsResponse.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");

		String redemptionStatus = visitsResponse.jsonPath().getString("redemption_status");
		Assert.assertEquals(redemptionStatus, "redeemable", "Redemption status should be 'redeemable'");

		int locationId = visitsResponse.jsonPath().getInt("location_id");
		Assert.assertTrue(locationId > 0, "Location ID should be greater than 0");

		pageObj.utils().logit("pass", "Api2 Create Redemption using visits is successful. Tracking Code: "
				+ redemptionTrackingCode + ", Redemption ID: " + redemptionId);
	}

	@Test(description = "SQ-T2377 Verify redemption using banked currency", groups = "api", priority = 2)
	public void verify_Redemption_Using_Banked_Currency() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Create Redemption using banked currency (fetch redemption code)
		Response bankedCurrencyResponse = pageObj.endpoints().Api2RedemptionWithBankedCurrency(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(bankedCurrencyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using banked currency");
		boolean isCreateRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, bankedCurrencyResponse.asString());
		Assert.assertTrue(isCreateRedemptionSchemaValidated,
				"API v2 Create Redemption using banked currency Schema Validation failed");

		// Validate redemption response fields
		String redemptionTrackingCode = bankedCurrencyResponse.jsonPath().getString("redemption_tracking_code");
		Assert.assertNotNull(redemptionTrackingCode, "Redemption tracking code should not be null");
		Assert.assertFalse(redemptionTrackingCode.isEmpty(), "Redemption tracking code should not be empty");

		int redemptionId = bankedCurrencyResponse.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");

		String redemptionStatus = bankedCurrencyResponse.jsonPath().getString("redemption_status");
		Assert.assertEquals(redemptionStatus, "redeemable", "Redemption status should be 'redeemable'");

		double redeemedValue = bankedCurrencyResponse.jsonPath().getDouble("redeemed_value");
		Assert.assertTrue(redeemedValue > 0, "Redeemed value should be greater than 0");

		pageObj.utils().logit("pass", "Api2 Create Redemption using banked currency is successful. Tracking Code: "
				+ redemptionTrackingCode + ", Redeemed Value: " + redeemedValue);
	}

	@Test(description = "SQ-T2378 Verify redemption using reward_id", groups = "api", priority = 3)
	public void verify_Redemption_Using_Reward_id() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward redeemable_id to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");

		// Validate user offers response
		String reward_id = offerResponse.jsonPath().getString("rewards[0].reward_id");
		Assert.assertNotNull(reward_id, "Reward ID should not be null");
		Assert.assertFalse(reward_id.isEmpty(), "Reward ID should not be empty");

		String rewardName = offerResponse.jsonPath().getString("rewards[0].name");
		Assert.assertNotNull(rewardName, "Reward name should not be null");

		pageObj.utils().logit("pass",
				"Api2 user fetch user offers is successful. Reward ID: " + reward_id + ", Reward Name: " + rewardName);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using reward_id");
		boolean isCreateRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateRedemptionSchema, redemptionResponse.asString());
		Assert.assertTrue(isCreateRedemptionSchemaValidated,
				"API v2 Create Redemption using reward_id Schema Validation failed");

		// Validate redemption response fields
		String redemptionTrackingCode = redemptionResponse.jsonPath().getString("redemption_tracking_code");
		Assert.assertNotNull(redemptionTrackingCode, "Redemption tracking code should not be null");
		Assert.assertFalse(redemptionTrackingCode.isEmpty(), "Redemption tracking code should not be empty");

		int redemptionId = redemptionResponse.jsonPath().getInt("redemption_id");
		Assert.assertTrue(redemptionId > 0, "Redemption ID should be greater than 0");

		String redemptionStatus = redemptionResponse.jsonPath().getString("redemption_status");
		Assert.assertEquals(redemptionStatus, "redeemable", "Redemption status should be 'redeemable'");

		pageObj.utils().logit("pass", "Api2 Create Redemption using reward_id is successful. Tracking Code: "
				+ redemptionTrackingCode + ", Redemption ID: " + redemptionId);

	}

	@Test(description = "SQ-T2379 Verify gifting loyalty reward to user", groups = "api", priority = 4)
	public void verify_Gifting_Loyalty_Reward_User_to_User() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		pageObj.utils().logPass("Api2 user fetch user offers is successful. Reward ID: " + reward_id);

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Gift loyalty reward to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Gift loyalty reward to other user");
		boolean isGiftRewardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftRewardResponse.asString());
		Assert.assertTrue(isGiftRewardSchemaValidated, "API v2 Gift Loyalty Reward Schema Validation failed");

		// Validate gift reward response
		Assert.assertNotNull(giftRewardResponse.asString(), "Gift reward response should not be null");
		Assert.assertFalse(giftRewardResponse.asString().isEmpty(), "Gift reward response should not be empty");

		String giftRewardMessage = giftRewardResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(giftRewardMessage, "Gift reward message should not be null");
		Assert.assertTrue(giftRewardMessage.contains("transferred to"),
				"Gift reward message should contain 'transferred to'");

		pageObj.utils().logit("pass",
				"Api2 Gift Loyalty Reward to other user is successful. Response: " + giftRewardMessage);

	}

	@Test(description = "SQ-T2380 Verify gifting loyalty amount user to user", groups = "api", priority = 5)
	public void verify_Gifting_Loyalty_Amount_User_to_User() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Gift Banked Currency to other user
		Response giftAmountResponse = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("secret"), newUserEmail, token);
		Assert.assertEquals(giftAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Gift Banked Currency to other user");
		boolean isGiftCurrencySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, giftAmountResponse.asString());
		Assert.assertTrue(isGiftCurrencySchemaValidated, "API v2 Gift Banked Currency Schema Validation failed");

		// Validate gift amount response
		Assert.assertNotNull(giftAmountResponse.asString(), "Gift amount response should not be null");
		Assert.assertFalse(giftAmountResponse.asString().isEmpty(), "Gift amount response should not be empty");

		String giftAmountMessage = giftAmountResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(giftAmountMessage, "Gift amount message should not be null");
		Assert.assertTrue(giftAmountMessage.contains("transferred to"),
				"Gift amount message should contain 'transferred to'");

		pageObj.utils().logit("pass",
				"Api2 Gift Banked Currency to other user is successful. Response: " + giftAmountMessage);

	}

	@Test(description = "SQ-T2382 Verify list deals", groups = "api", priority = 6)
	public void verify_List_Deals() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// List all deals
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(listdealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals");
		boolean isListDealsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authListAllDealsSchema, listdealsResponse.asString());
		Assert.assertTrue(isListDealsSchemaValidated, "API v2 List All Deals Schema Validation failed");

		// Validate list deals response
		int dealsCount = listdealsResponse.jsonPath().getList("$").size();
		Assert.assertTrue(dealsCount > 0, "Deals count should be greater than 0");

		String redeemableUUID = listdealsResponse.jsonPath().getString("[0].redeemable_uuid");
		Assert.assertNotNull(redeemableUUID, "Redeemable UUID should not be null");
		Assert.assertFalse(redeemableUUID.isEmpty(), "Redeemable UUID should not be empty");

		int redeemableId = listdealsResponse.jsonPath().getInt("[0].redeemable_id");
		Assert.assertTrue(redeemableId > 0, "Redeemable ID should be greater than 0");

		String dealName = listdealsResponse.jsonPath().getString("[0].name");
		Assert.assertNotNull(dealName, "Deal name should not be null");

		double discountAmount = listdealsResponse.jsonPath().getDouble("[0].discount_amount");
		Assert.assertTrue(discountAmount >= 0, "Discount amount should be non-negative");

		pageObj.utils().logit("pass", "Api2 List All Deals is successful. Deal Name: " + dealName
				+ ", Redeemable UUID: " + redeemableUUID + ", Deals Count: " + dealsCount);

		// Get details of deal
		Response detailsDealsResponse = pageObj.endpoints().Api2getDetailsofDeals(dataSet.get("client"),
				dataSet.get("secret"), token, redeemableUUID);
		Assert.assertEquals(detailsDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Get Deal Details");
		boolean isDetailsDealsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authGetDealDetailsSchema, detailsDealsResponse.asString());
		Assert.assertTrue(isDetailsDealsSchemaValidated, "API v2 Get Deal Details Schema Validation failed");

		// Validate deal details response
		String redeemable_uuid = detailsDealsResponse.jsonPath().getString("redeemable_uuid");
		Assert.assertNotNull(redeemable_uuid, "Redeemable UUID in details should not be null");
		Assert.assertEquals(redeemable_uuid, redeemableUUID, "Redeemable UUID should match the requested UUID");

		String detailDealName = detailsDealsResponse.jsonPath().getString("name");
		Assert.assertNotNull(detailDealName, "Deal name in details should not be null");

		int detailRedeemableId = detailsDealsResponse.jsonPath().getInt("redeemable_id");
		Assert.assertTrue(detailRedeemableId > 0, "Redeemable ID in details should be greater than 0");

		double detailDiscountAmount = detailsDealsResponse.jsonPath().getDouble("discount_amount");
		Assert.assertTrue(detailDiscountAmount >= 0, "Discount amount in details should be non-negative");

		pageObj.utils().logit("pass", "Api2 Get Deal Details is successful. Deal Name: " + detailDealName
				+ ", Redeemable ID: " + detailRedeemableId);

		// Save selected deal
		/*
		 * Response saveDealResponse =
		 * pageObj.endpoints().Api2SaveSelectedDeal(dataSet.get("client"),
		 * dataSet.get("secret"), token, redeemable_uuid);
		 * Assert.assertEquals(saveDealResponse.getStatusCode(),
		 * ApiConstants.HTTP_STATUS_OK,
		 * "Status code 200 did not matched for api2 save selected deal");
		 */

	}

	@Test(description = "SQ-T2383 verify_Create_Checkins", groups = "api", priority = 7)
	public void verify_Create_Checkins() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Create Loyalty Checkin by Barcode
		/*
		 * try {
		 * 
		 * driver = new BrowserUtilities().launchBrowser(); pageObj = new
		 * PageObj(driver);
		 * 
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get(
		 * "instanceUrl")); pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * generateBarcode pageObj.menupage().navigateToSubMenuItem("Support",
		 * "Test Barcodes");
		 * pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		 * String barcode = pageObj.instanceDashboardPage().captureBarcode();
		 * logger.info(barcode); driver.close();
		 * 
		 * Response barcodeCheckinResponse =
		 * pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
		 * dataSet.get("secret"), token, barcode);
		 * Assert.assertEquals(barcodeCheckinResponse.getStatusCode(),
		 * ApiConstants.HTTP_STATUS_CREATED,
		 * "Status code 201 did not matched for api2 Loyalty Checkin by bar code"); }
		 * catch (Exception e) { e.printStackTrace(); } // Create Loyalty Checkin by QR
		 * Code
		 * 
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
		 * logger.info(qrCode); driver.close();
		 * 
		 * Response qrcodeCheckinResponse =
		 * pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("client"),
		 * dataSet.get("secret"), token, qrCode);
		 * Assert.assertEquals(qrcodeCheckinResponse.getStatusCode(),
		 * ApiConstants.HTTP_STATUS_CREATED,
		 * "Status code 201 did not matched for api2 Loyalty Checkin by qr code"); }
		 * catch (Exception e) { e.printStackTrace(); }
		 */

		// Create Loyalty Checkin by Receipt Image
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Loyalty Checkin by Receipt Image");

		// Validate receipt checkin response
		Assert.assertNotNull(receiptCheckinResponse.asString(), "Receipt checkin response should not be null");

		long receiptCheckinId = receiptCheckinResponse.jsonPath().getLong("checkin_id");
		Assert.assertTrue(receiptCheckinId > 0, "Receipt checkin_id should be greater than 0");

		int receiptLocationId = receiptCheckinResponse.jsonPath().getInt("location_id");
		Assert.assertTrue(receiptLocationId > 0, "Receipt location_id should be greater than 0");

		String storeNumber = receiptCheckinResponse.jsonPath().getString("store_number");
		Assert.assertNotNull(storeNumber, "Store number should not be null");

		String membershipLevel = receiptCheckinResponse.jsonPath().getString("current_membership_level");
		Assert.assertNotNull(membershipLevel, "Current membership level should not be null");

		pageObj.utils().logit("pass", "Api2 Loyalty Checkin by Receipt Image is successful. Checkin ID: "
				+ receiptCheckinId + ", Store: " + storeNumber + ", Membership Level: " + membershipLevel);

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch checkin");
		boolean isFetchCheckinSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2FetchCheckinSchema, fetchCheckinResponse.asString());
		Assert.assertTrue(isFetchCheckinSchemaValidated, "API v2 Fetch Checkin Schema Validation failed");

		// Validate fetch checkin response
		String checkin_id = fetchCheckinResponse.jsonPath().getString("[0].checkin_id");
		Assert.assertNotNull(checkin_id, "Checkin ID should not be null");
		Assert.assertFalse(checkin_id.isEmpty(), "Checkin ID should not be empty");

		String checkinStatus = fetchCheckinResponse.jsonPath().getString("[0].status");
		Assert.assertNotNull(checkinStatus, "Checkin status should not be null");

		pageObj.utils().logit("pass",
				"Api2 Fetch Checkin is successful. Checkin ID: " + checkin_id + ", Status: " + checkinStatus);

		// Account Balance
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		boolean isAccountBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2AccountBalanceSchema, accountBalResponse.asString());
		Assert.assertTrue(isAccountBalanceSchemaValidated, "API v2 Account Balance Schema Validation failed");

		// Validate account balance response
		Assert.assertNotNull(accountBalResponse.jsonPath().get("account_balance_details"),
				"Account balance details should not be null");
		double bankedCurrency = accountBalResponse.jsonPath().getDouble("account_balance_details.banked_currency");

		pageObj.utils().logit("pass", "Api2 Account Balance is successful. Banked Currency: " + bankedCurrency);

		// Transaction Details
		Response txnDetailsResponse = pageObj.endpoints().Api2Trasactiondetails(dataSet.get("client"),
				dataSet.get("secret"), token, checkin_id);
		Assert.assertEquals(txnDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 transaction details");
		boolean isTransactionDetailsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2TransactionDetailsSchema, txnDetailsResponse.asString());
		Assert.assertTrue(isTransactionDetailsSchemaValidated, "API v2 Transaction Details Schema Validation failed");

		// Validate transaction details response structure
		Assert.assertNotNull(txnDetailsResponse.asString(), "Transaction details response should not be null");
		Assert.assertNotNull(txnDetailsResponse.jsonPath().get("redemptions"), "Redemptions array should not be null");

		pageObj.utils().logit("pass",
				"Api2 Transaction Details is successful. Response: " + txnDetailsResponse.asString());

	}

	@Test(description = "SQ-T2856 verify List User Offer", groups = "api", priority = 8)
	public void verify_List_User_Offers() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// List User Offers
		Response offersResponse = pageObj.endpoints().Api2ListOffers(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(offersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list user offers");
		boolean isListOffersSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2ListUserOffersSchema, offersResponse.asString());
		Assert.assertTrue(isListOffersSchemaValidated, "API v2 List User Offers Schema Validation failed");

		// Validate list user offers response
		Assert.assertNotNull(offersResponse.jsonPath().get("rewards"), "Rewards array should not be null");
		int rewardsCount = offersResponse.jsonPath().getList("rewards").size();
		Assert.assertTrue(rewardsCount > 0, "Rewards count should be greater than 0");

		String rewardId = offersResponse.jsonPath().getString("rewards[0].reward_id");
		Assert.assertNotNull(rewardId, "Reward ID should not be null");

		pageObj.utils().logit("pass",
				"Api2 List User Offers is successful. Rewards Count: " + rewardsCount + ", Reward ID: " + rewardId);

		// List applicable offers
		Response applicableOffersResponse = pageObj.endpoints().Api2ListApplicableOffers(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("location_id"));
		Assert.assertEquals(applicableOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list applicable offers");
		boolean isApplicableOffersSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2ListApplicableOffersSchema, applicableOffersResponse.asString());
		Assert.assertTrue(isApplicableOffersSchemaValidated, "API v2 List Applicable Offers Schema Validation failed");

		// Validate applicable offers response
		Assert.assertNotNull(applicableOffersResponse.asString(), "Applicable offers response should not be null");

		pageObj.utils().logit("pass",
				"Api2 List Applicable Offers is successful. Response: " + applicableOffersResponse.asString());

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}

}
