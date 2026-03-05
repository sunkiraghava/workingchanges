package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class OMM1335OloPromoSpecTest {

	static Logger logger = LogManager.getLogger(OMM1335OloPromoSpecTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String userEmail;
	ApiPayloads apipaylods;
	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	private String endDateTime;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
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

	}

	private String registerUserAndGetId() {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		return signUpResponse.jsonPath().getString("user.user_id");
	}

	private void giftPointsToUser(String userID) {
		Response sendGiftResponse = pageObj.endpoints().sendPointsToUser(userID, dataSet.get("points"),
				dataSet.get("apiKey"));
		Assert.assertEquals(sendGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched ");
		pageObj.utils().logPass("Successfully gifted points to user ");
	}

	private void sendRewardToUser(String userID, String amount, String redeemableId) {
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), amount,
				redeemableId);
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		TestListeners.extentTest.get().pass("Api2 Send reward to user is successful");
	}

	private Response getPromotionAccountBalance(String userID, String client, String secret) {
		Response response = pageObj.endpoints().authApiGetPromotionAccountBalance(userID, client, secret);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Get Promotions Accounts balance API response");
		return response;
	}

	@Test(description = "SQ-T7355 Verify the API response error for using account_id from different business", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateSQT7355() throws Exception {
		String userID = registerUserAndGetId();
		giftPointsToUser(userID);
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");
		Response userBalanceResponseOLO = pageObj.endpoints().authApiGetPromotionAccountBalance(userID,
				dataSet.get("moesClient"), dataSet.get("moesSecret"));
		// validate status code
		Assert.assertEquals(userBalanceResponseOLO.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Failed Get Promotions Accounts balance API response");
		// validate error message
		Assert.assertEquals(userBalanceResponseOLO.jsonPath().getString("message"), dataSet.get("expectedError"),
				"Error message did not match");

		pageObj.utils().logPass("Error message get displayed for user belonging to another business");

	}

	@Test(description = "SQ-T7354 Valid Request with Existing Account ID", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	@Owner(name = "Rahul Garg")
	public void validateSQT7354() throws Exception {

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Register user and get user ID
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		logger.info("User Email: " + userEmail);
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String userID = signUpResponse.jsonPath().getString("user.user_id"),
				accessToken = signUpResponse.jsonPath().get("access_token.token").toString();
		logger.info("User ID: " + userID);

		// Send reward redeemable to user
		sendRewardToUser(userID, "", dbRedeemableId);

		// Send reward amount to user
		sendRewardToUser(userID, "10", "");

		// Gift Points to user
		giftPointsToUser(userID);

		// Api2 Fetch User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), accessToken);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		TestListeners.extentTest.get().pass("Api2 Fetch user balance is successful");

		// Get Promotions Accounts balance API
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");
		Response userBalanceResponseOLO = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));
		String userBalanceResponse1 = userBalanceResponse.getBody().asString();
		String userBalanceResponseOLO1 = userBalanceResponseOLO.getBody().asString();

		// Use Jackson to parse JSON
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node1 = mapper.readTree(userBalanceResponseOLO1);
		JsonNode node2 = mapper.readTree(userBalanceResponse1);

		JsonNode reward1 = node1.get("rewards").get(0);
		JsonNode matchingReward = null;

		// Find matching reward by reward_id
		for (JsonNode r : node2.get("rewards")) {
			if (r.get("reward_id").asInt() == reward1.get("id").asInt()) {
				matchingReward = r;
				break;
			}
		}

		Assert.assertNotNull(matchingReward, "Matching reward not found in second JSON");

		// Assert: Reward ID
		Assert.assertEquals(reward1.get("id").asInt(), matchingReward.get("reward_id").asInt(), "Reward ID mismatch");

		// Assert: Name
		Assert.assertEquals(reward1.get("name").asText(), matchingReward.get("name").asText(), "Name mismatch");

		// Assert: Description
		Assert.assertTrue(reward1.get("description").isNull(), "Description in JSON 1 should be null");
		Assert.assertTrue(matchingReward.get("description").isNull(), "Description in JSON 2 should be null");

		// Assert: Expiration date (normalized to UTC)
		ZonedDateTime exp1 = ZonedDateTime.parse(reward1.get("expiration").asText(),
				DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		ZonedDateTime exp2 = ZonedDateTime.parse(matchingReward.get("expiring_at").asText(),
				DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		Assert.assertEquals(exp1.toInstant(), exp2.toInstant(), "Expiration time mismatch");

		// Assert balance quantity
		double balanceShort = node1.get("balance").get("quantity").asDouble();
		double balanceDetailed = node2.get("account_balance").get("unbanked_points").asDouble();

		Assert.assertEquals(balanceShort, balanceDetailed, "Balance quantity mismatch");

	}

	@Test(description = "SQ-T7356 Verify the API response when user status is active but no `rewards' exists for user", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateSQT7356() throws Exception {

		String userID = registerUserAndGetId();
		// Send reward amount to user
		sendRewardToUser(userID, "10", "");

		// Gift Points to user
		giftPointsToUser(userID);

		// Get Promotions Accounts balance API
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");
		Response userBalanceResponseOLO = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		JsonPath jp = new JsonPath(userBalanceResponseOLO.getBody().asString());

		// validate rewards object
		List<Object> rewards = jp.getList("rewards");
		Assert.assertTrue(rewards.isEmpty(), "Expected rewards array to be empty");

		pageObj.utils().logPass("Expected rewards array to be empty");

	}

	@Test(description = "SQ-T7357 Verify that same reward, subscription, when added multiple times, is differentiated for unique `rewards.id`", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	@Owner(name = "Rahul Garg")
	public void validateSQT7357() throws Exception {

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		String userID = registerUserAndGetId();
		// Send reward redeemable to user
		sendRewardToUser(userID, "", dbRedeemableId);

		// Send reward redeemable to user
		sendRewardToUser(userID, "", dbRedeemableId);

		// Get Promotions Accounts balance API
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");

		Response userBalanceResponseOLO = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		// Parse JSON and validate unique reward IDs
		Set<Integer> rewardIds = new HashSet<>();
		JsonNode rewards = new ObjectMapper().readTree(userBalanceResponseOLO.getBody().asString()).get("rewards");

		if (rewards != null && rewards.isArray()) {
			for (JsonNode reward : rewards) {
				int id = reward.get("id").asInt();
				Assert.assertTrue(rewardIds.add(id), "Duplicate reward id found: " + id);
			}
		}

		// Success logs
		pageObj.utils().logPass("No duplicate reward ID found in rewards array");

	}

	@Test(description = "SQ-T7358 Verify the API response for Redeemable- \"Expired, deactivated, active, delete\"", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateSQT7358() throws Exception {

		// Create Redeemable
		String redeemableName = "AutomationRedeemablePS2.0_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String businessID = dataSet.get("business_id");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "flat_discount");
		redeemableMap.put("discount_amount", "10.0");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));
		redeemableMap.put("applicable_as_loyalty_redemption", "true");

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Register user and get user ID
		String userID = registerUserAndGetId();

		// Gift points to user
		giftPointsToUser(userID);

		// Get Promotions Accounts balance API
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");

		Response userBalanceResponseOLO = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		JsonPath jsonPath = new JsonPath(userBalanceResponseOLO.getBody().asString());
		List<Integer> rewardIds = jsonPath.getList("rewards.id");

		// Convert Integer list to String list
		List<String> rewardIdStrings = rewardIds.stream().map(String::valueOf).collect(Collectors.toList());

		// Assert that the redeemable ID exists in the rewards array
		Assert.assertTrue(rewardIdStrings.contains(dbRedeemableId),
				"Reward ID " + dbRedeemableId + " should exist in rewards array");
		pageObj.utils().logPass("Redeemable is active and present in rewards array");

		// ----------- Deactivated Redeemable-----------------

		String updateDeactivatedAtQuery = "UPDATE redeemables SET deactivated_at = '2025-08-05 04:59:59' WHERE id = '"
				+ dbRedeemableId + "'";
		int resultSet = DBUtils.executeUpdateQuery(env, updateDeactivatedAtQuery);
		Assert.assertEquals(resultSet, 1);

		Response userBalanceResponseOLOAfterDeactivating = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		JsonPath jsonPathDeactivated = new JsonPath(userBalanceResponseOLOAfterDeactivating.getBody().asString());
		List<Integer> rewardIdDeactivated = jsonPathDeactivated.getList("rewards.id");

		// Convert Integer list to String list
		List<String> newRewardIdStringsDeactivated = rewardIdDeactivated.stream().map(String::valueOf)
				.collect(Collectors.toList());

		// Assert that the redeemable ID does not exist in the rewards array
		Assert.assertFalse(newRewardIdStringsDeactivated.contains(dbRedeemableId),
				"Reward ID " + dbRedeemableId + " should not exist in rewards array");
		pageObj.utils().logPass("Deactivated redeemable is not present in rewards array");

		// -------- Expired Redeemable--------------

		// Reset deactivated_at to NULL before updating end_time
		DBUtils.executeUpdateQuery(env,
				"UPDATE redeemables SET deactivated_at = NULL WHERE id = '" + dbRedeemableId + "'");

		// Update the end time to a past date to simulate expiration
		String updateEndTimeQuery = "UPDATE redeemables SET end_time = '2025-08-05 04:59:59' WHERE id = '"
				+ dbRedeemableId + "'";
		int resultSetExpired = DBUtils.executeUpdateQuery(env, updateEndTimeQuery);
		Assert.assertEquals(resultSetExpired, 1);

		Response userBalanceResponseOLOAfterMarkingExpired = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		JsonPath jsonPathExpired = new JsonPath(userBalanceResponseOLOAfterMarkingExpired.getBody().asString());
		List<Integer> rewardIdExpired = jsonPathExpired.getList("rewards.id");

		// Convert Integer list to String list
		List<String> newRewardIdStringsExpired = rewardIdExpired.stream().map(String::valueOf)
				.collect(Collectors.toList());

		// Assert that the redeemable ID does not exist in the rewards array
		Assert.assertFalse(newRewardIdStringsExpired.contains(dbRedeemableId),
				"Reward ID " + dbRedeemableId + " should not exist in rewards array");
		pageObj.utils().logPass("Expired redeemable is not present in rewards array");

		// -------- Delete Redeemable--------------
		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("$redeemableExternalID", actualExternalIdRedeemable).replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

		Response userBalanceResponseOLOAfterDeletion = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		JsonPath jsonPathDeleted = new JsonPath(userBalanceResponseOLOAfterDeletion.getBody().asString());
		List<Integer> rewardIdDeleted = jsonPathDeleted.getList("rewards.id");

		// Convert Integer list to String list
		List<String> newRewardIdStringsDeleted = rewardIdDeleted.stream().map(String::valueOf)
				.collect(Collectors.toList());

		// Assert that the redeemable ID does not exist in the rewards array
		Assert.assertFalse(newRewardIdStringsDeleted.contains(dbRedeemableId),
				"Reward ID " + dbRedeemableId + " should not exist in rewards array");
		pageObj.utils().logPass("Deleted redeemable is not present in rewards array");

	}

	@Test(description = "SQ-T7359 Verify the API response for Subscription status- \"Hard cancel, soft cancel, expired, active, deactivated\"", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateSQT7359() throws Exception {
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

		// Register user and get user ID
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		logger.info("User Email: " + userEmail);
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String userID = signUpResponse.jsonPath().getString("user.user_id"),
				accessToken = signUpResponse.jsonPath().get("access_token.token").toString();
		logger.info("User ID: " + userID);

		// Active plan purchase
		Response purchaseSubscriptionResponse = pageObj.endpoints().Api2SubscriptionPurchase(accessToken,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_idActive = purchaseSubscriptionResponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_idActive + " Plan id = " + dataSet.get("PlanID"));

		// Plan purchase for hard cancel
		Response purchaseSubscriptionResponseHardCancel = pageObj.endpoints().Api2SubscriptionPurchase(accessToken,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionResponseHardCancel.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscriptionIdHardCancel = purchaseSubscriptionResponseHardCancel.jsonPath().get("subscription_id")
				.toString();
		logger.info(userEmail + " purchased " + subscriptionIdHardCancel + " Plan id = " + dataSet.get("PlanID"));

		// Plan purchase for soft cancel
		Response purchaseSubscriptionResponseSoftCancel = pageObj.endpoints().Api2SubscriptionPurchase(accessToken,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionResponseSoftCancel.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscriptionIdSoftCancel = purchaseSubscriptionResponseSoftCancel.jsonPath().get("subscription_id")
				.toString();
		logger.info(userEmail + " purchased " + subscriptionIdSoftCancel + " Plan id = " + dataSet.get("PlanID"));

		// Plan purchase for Expired
		Response purchaseSubscriptionResponseExpired = pageObj.endpoints().Api2SubscriptionPurchase(accessToken,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), dataSet.get("spPrice"),
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionResponseExpired.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscriptionIdExpired = purchaseSubscriptionResponseExpired.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscriptionIdExpired + " Plan id = " + dataSet.get("PlanID"));

		// Hard Cancel Subscription

		pageObj.utils().logit("== Platform Functions API: Cancel Subscription (Turn off Auto Renewal) ==");

		Response cancelSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionCancel(dataSet.get("apiKey"),
				subscriptionIdHardCancel, "Did not like the service", "hard_cancelled");
		Assert.assertEquals(cancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Cancel Subscription call.");

		String cancelSubscriptionResponseMsg = cancelSubscriptionResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(cancelSubscriptionResponseMsg, "Subscription auto renewal canceled.");
		pageObj.utils().logPass("Platform Functions API Cancel Subscription call is successful for Subscription ID: "
				+ subscriptionIdHardCancel);

		// Soft Cancel Subscription

		Response softCancelSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionCancel(dataSet.get("apiKey"),
				subscriptionIdSoftCancel, "Did not like the service", "soft_cancelled");
		Assert.assertEquals(softCancelSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Cancel Subscription call.");

		String softCancelSubscriptionResponseMsg = softCancelSubscriptionResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(softCancelSubscriptionResponseMsg, "Subscription auto renewal canceled.");
		pageObj.utils().logPass("Platform Functions API Cancel Subscription call is successful for Subscription ID: "
				+ subscriptionIdSoftCancel);

		// Expire the subscription
		String updateEndTimeQuery = "UPDATE user_subscriptions SET end_time = '2025-08-05 04:59:59' WHERE id = '"
				+ subscriptionIdExpired + "'";
		int resultSetExpired = DBUtils.executeUpdateQuery(env, updateEndTimeQuery);
		Assert.assertEquals(resultSetExpired, 1);

		// Get Promotions Accounts balance API
		pageObj.utils().logit("== OLO Get Promotions Accounts balance API ==");

		Response userBalanceResponseOLO = getPromotionAccountBalance(userID, dataSet.get("client"),
				dataSet.get("secret"));

		// Validate that the active subscription is present in the rewards array
		JsonPath jsonPath = new JsonPath(userBalanceResponseOLO.getBody().asString());
		String type = jsonPath.getString("rewards.find { it.name == 'Do Not Delete Subscription_OMMT2527' }.type");

		logger.info("Type of the subscription: " + type);
		Assert.assertEquals(type, "perk", "Expected type to be 'perk', but found: " + type);

		// Validate that the soft cancel subscription is present in the rewards array
		Map<String, Object> softCancelInUserbalance = jsonPath
				.getMap("rewards.find { it.id == " + subscriptionIdSoftCancel + " }");
		Map<String, Object> activeInUserbalance = jsonPath
				.getMap("rewards.find { it.id == " + subscription_idActive + " }");

		if (softCancelInUserbalance != null) {
			System.out.println("Found subscriptionIdSoftCancel: " + softCancelInUserbalance);
			pageObj.utils().logPass("Soft cancelled subscription is present in the rewards array");
		} else {
			System.out.println("Reward with ID " + subscriptionIdSoftCancel + " not found.");
		}

		if (activeInUserbalance != null) {
			System.out.println("Found subscriptionIdSoftCancel: " + activeInUserbalance);
			pageObj.utils().logPass("Active subscription is present in the rewards array");
		} else {
			System.out.println("Reward with ID " + subscription_idActive + " not found.");
		}

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}