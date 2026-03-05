package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ItemQualifierExclusionRuleWithFlagFalseTest {

	static Logger logger = LogManager.getLogger(ItemQualifierExclusionRuleWithFlagFalseTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	ApiPayloads apipaylods;
	String lisExternalID;
	String qcExternalID;
	String redeemableExternalID;
	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";

	private ApiPayloadObj apipayloadObj;
	private String externalUID;
	private static final Object DB_LOCK = new Object();
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils.logInfo(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// enable flag from the db
		synchronized (DB_LOCK) {
			String businessQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
					+ dataSet.get("business_id") + "'";
			String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessQuery, "preferences");
			boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
					"return_qualifying_menu_items_to_pos", dataSet.get("business_id"));
			Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
			utils.logInfo(dataSet.get("dbFlag") + " value is updated to true");
		}
	}

	// disable flag-enable_decoupled_redemption_engine and enable
	// return_qualifying_menu_items_to_pos from the DB
	private void enableDecoupledRedemptionAndReturnQCItemsFlag(String decoupleFlagValue, String returnQCItemsFlagValue)
			throws Exception {

		String businessQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessQuery, "preferences");

		// Enable decoupled redemption engine
		boolean decoupleFlag = DBUtils.updateBusinessesPreference(env, expColValue, decoupleFlagValue,
				"enable_decoupled_redemption_engine", dataSet.get("business_id"));
		Assert.assertTrue(decoupleFlag, "enable_decoupled_redemption_engine value is not updated to true");
		utils.logInfo("enable_decoupled_redemption_engine value is updated to true");

		// Enable return_qualifying_menu_items_to_pos
		boolean returnQCItemsFlag = DBUtils.updateBusinessesPreference(env, expColValue, returnQCItemsFlagValue,
				"return_qualifying_menu_items_to_pos", dataSet.get("business_id"));
		Assert.assertTrue(returnQCItemsFlag, "return_qualifying_menu_items_to_pos value is not updated to true");
		utils.logInfo("return_qualifying_menu_items_to_pos value is updated to true");
	}

	@Test(description = "SQ-T7297 Verify the Receipt qualifies based on net amount excluding min priced item.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7297_verifyReceiptQualifiesBasedOnNetAmountExcludingMinPricedItem() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		;

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 25.0, 1)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();

		// User Sign-up API call
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Get userId and token from response
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich1", new String[] { "Sandwich1", "1", "10", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich2", "1", "20", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Sandwich3", new String[] { "Sandwich3", "1", "30", "M", "101", "100", "3.0", "103" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 60.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Delete LIS, QC and Redeemable Data
		// utils.deleteLISQCRedeemable(env, lisExternalID,
		// qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7298 Verify the Receipt qualifies based on net amount excluding max priced item.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7298_verifyReceiptQualifiesBasedOnNetAmountExcludingMaxPricedItem() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		;

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 25.0, 1)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich1", new String[] { "Sandwich1", "1", "10", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich2", "1", "20", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Sandwich3", new String[] { "Sandwich3", "1", "30", "M", "101", "100", "3.0", "103" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 60.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7299 Verify the Receipt qualifies based on net amount excluding min priced item with multiple Quantity.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7299_verifyReceiptQualifiesExcludingMinItemMultiQty() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 25.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich1", new String[] { "Sandwich1", "1", "10", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich2", "1", "20", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Sandwich3", new String[] { "Sandwich3", "1", "30", "M", "101", "100", "3.0", "103" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 60.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7300 verify the Receipt qualifies based on net amount excluding max priced item with multiple Quantity.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7300_verifyReceiptQualifiesExcludingMaxItemMultiQty() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 10.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich1", new String[] { "Sandwich1", "1", "10", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich2", "1", "20", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Sandwich3", new String[] { "Sandwich3", "1", "30", "M", "101", "100", "3.0", "103" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 60.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7301 Verify the Receipt qualifies based on net amount excluding min priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7301_verifyReceiptQualificationExcludingMinItem_ForMItems_WithTaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 14.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

//		receipt
//		Sandwich|1|10|M|101|1001|1000|1.0
//		Pizza|1|16|M|102|1001|1000|2.0
//		Toppings|1|8|M|102|1001|1000|2.1
//		Coke|1|16|M|103|1001|1000|3.0
//		CreditCard|1|30|P|104|1001|1000|4.0
//		Discount|1|2|D|107|1001|1000|5.0
//		Delivery|1|10|T|108|1001|1000|6.0
//		Service|1|10|S|109|1001|1000|7.0

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "8", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "4", "M", "101", "100", "2.1", "102" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "8", "M", "101", "100", "3.0", "103" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "15", "P", "101", "100", "4.0", "104" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "5.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "5", "T", "101", "100", "6.0", "108" });
		addDetails.accept("Service", new String[] { "Service", "1", "5", "S", "101", "100", "7.0", "109" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 40.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza", "Toppings", "Coke", "CreditCard");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7302 Verify the Receipt qualifies based on net amount excluding max priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7302_verifyReceiptQualificationExcludingMaxItem_ForMItems_WithTaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 8.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

//		receipt
//		Sandwich|1|10|M|101|1001|1000|1.0
//		Pizza|1|16|M|102|1001|1000|2.0
//		Toppings|1|8|M|102|1001|1000|2.1
//		Coke|1|16|M|103|1001|1000|3.0
//		CreditCard|1|30|P|104|1001|1000|4.0
//		Discount|1|2|D|107|1001|1000|5.0
//		Delivery|1|10|T|108|1001|1000|6.0
//		Service|1|10|S|109|1001|1000|7.0

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "6", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "8", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "4", "M", "101", "100", "2.1", "102" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "8", "M", "101", "100", "3.0", "103" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "15", "P", "101", "100", "4.0", "104" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "5.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "5", "T", "101", "100", "6.0", "108" });
		addDetails.accept("Service", new String[] { "Service", "1", "5", "S", "101", "100", "7.0", "109" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 41.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza", "Toppings", "Coke", "CreditCard");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7303 Only Modifiers: Verify the Receipt qualifies based on net amount excluding min priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7303_receiptQualification_onlyModifiers_excludeMinMItems_taxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114,115
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114,115").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_min_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 30.5, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "2.5", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1.5", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "15", "M", "101", "100", "3.0", "103" });
		addDetails.accept("Ice", new String[] { "Ice", "1", "2", "M", "101", "100", "3.1", "113" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "10", "P", "101", "100", "3.2", "101" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "6.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "101", "100", "7.0", "101" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "101", "100", "8.0", "101" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 16.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Cheese", "Toppings", "Ice", "CreditCard");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7304 Only Modifiers: Verify the Receipt qualifies based on net amount excluding max priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7304_receiptQualification_onlyModifiers_excludeMaxMItems_taxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114,115
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114,115").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 29.5, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "2.5", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1.5", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "15", "M", "101", "100", "3.0", "103" });
		addDetails.accept("Ice", new String[] { "Ice", "1", "2", "M", "101", "100", "3.1", "113" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "10", "P", "101", "100", "3.2", "101" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "6.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "101", "100", "7.0", "101" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "101", "100", "8.0", "101" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "60", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 16.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Cheese", "Toppings", "Ice", "CreditCard");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7305 Base Items and Modifiers: Verify the Receipt qualifies based on net amount excluding min priced item.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7305_verifyReceiptExcludingMinItem_BaseModifiers() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_min_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 10.0, 1)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|9|M|101|1001|1000|1.0 Cheese|1|1|M|111|1001|1000|1.1
		 * Pizza|1|1|M|102|1001|1000|2.0 Toppings|1|1|M|112|1001|1000|2.1
		 */
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "9", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Cheese", "1", "1", "M", "111", "100", "1.1", "111");
		parentMap.put("Cheese", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza", "1", "1", "M", "101", "100", "2.0", "102");
		parentMap.put("Pizza", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Toppings", "1", "1", "M", "112", "100", "2.1", "112");
		parentMap.put("Toppings", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "12", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 12.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");
		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7306 Base Items and Modifiers: Verify the Receipt qualifies based on net amount excluding max priced item.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7306_verifyReceiptExcludingMaxItem_BaseModifiers() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 2.0, 1)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|9|M|101|1001|1000|1.0 Cheese|1|1|M|111|1001|1000|1.1
		 * Pizza|1|1|M|102|1001|1000|2.0 Toppings|1|1|M|112|1001|1000|2.1
		 */
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "9", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Cheese", "1", "1", "M", "111", "100", "1.1", "111");
		parentMap.put("Cheese", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Pizza", "1", "1", "M", "101", "100", "2.0", "102");
		parentMap.put("Pizza", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Toppings", "1", "1", "M", "112", "100", "2.1", "112");
		parentMap.put("Toppings", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "12", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 12.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T7307 Base Items and Modifiers: Verify the Receipt qualifies based on net amount excluding min priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7307_verifyReceiptExcludingMinItem_BaseModifiers_WithTaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_min_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 8.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|9|M|101|1001|1000|1.0 Cheese|1|1|M|111|1001|1000|1.1
		 * Pizza|1|1|M|102|1001|1000|2.0 Toppings|1|1|M|112|1001|1000|2.1
		 * Sauces|1|8|M|113|1001|1000|2.2 Burger|1|5|M|102|1001|1000|3.0
		 * Lettuce|1|2|M|112|1001|1000|3.1 discount|1|2|D|109|1001|1000|4.0
		 * Delivery|1|10|T|104|1001|1000|5.0 Service|1|10|S|107|1001|1000|6.0
		 * coffee|1|4|P|2001|1001|1000|7.0
		 */

		// Using BiConsumer to avoid repetitive code
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "9", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "1", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "1", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Sauces", new String[] { "Sauces", "1", "8", "M", "112", "100", "2.2", "113" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "5", "M", "112", "100", "3.0", "103" });
		addDetails.accept("Lettuce", new String[] { "Lettuce", "1", "2", "M", "112", "100", "3.1", "114" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "112", "100", "4.0", "109" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "112", "100", "5.0", "104" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "112", "100", "6.0", "107" });
		addDetails.accept("Payment", new String[] { "Payment", "1", "4", "P", "112", "100", "7.0", "2001" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "27", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 27.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza", "Burger");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7308 Base Items and Modifiers: Verify the Receipt qualifies based on net amount excluding max priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7308_verifyReceiptExcludingMaxItem_BaseModifiers_WithTaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 5.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|9|M|101|1001|1000|1.0 Cheese|1|1|M|111|1001|1000|1.1
		 * Pizza|1|1|M|102|1001|1000|2.0 Toppings|1|1|M|112|1001|1000|2.1
		 * Sauces|1|8|M|113|1001|1000|2.2 Burger|1|5|M|102|1001|1000|3.0
		 * Lettuce|1|2|M|112|1001|1000|3.1 discount|1|2|D|109|1001|1000|4.0
		 * Delivery|1|10|T|104|1001|1000|5.0 Service|1|10|S|107|1001|1000|6.0
		 * coffee|1|4|P|2001|1001|1000|7.0
		 */

		// Using BiConsumer to avoid repetitive code
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "9", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "1", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "1", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Sauces", new String[] { "Sauces", "1", "8", "M", "112", "100", "2.2", "113" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "5", "M", "112", "100", "3.0", "103" });
		addDetails.accept("Lettuce", new String[] { "Lettuce", "1", "2", "M", "112", "100", "3.1", "114" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "112", "100", "4.0", "109" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "112", "100", "5.0", "104" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "112", "100", "6.0", "107" });
		addDetails.accept("Payment", new String[] { "Payment", "1", "4", "P", "112", "100", "7.0", "2001" });

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "27", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 27.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich", "Pizza", "Burger");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

		// Verify no extra items are present
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Response contains unexpected items!");
		utils.logPass("Response does not contain unexpected items");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7311 Redemption 2.0: Verify the Receipt qualifies based on net amount excluding min priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7311_verifyRedemption2_0_ReceiptExcludingMinItem_OnlyBase_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 14.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "8", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "4", "M", "101", "100", "2.1", "102" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "8", "M", "101", "100", "3.0", "103" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "15", "P", "101", "100", "4.0", "104" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "5.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "5", "T", "101", "100", "6.0", "108" });
		addDetails.accept("Service", new String[] { "Service", "1", "5", "S", "101", "100", "7.0", "109" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountAmount, 40.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Sandwich", "Pizza", "Toppings", "Coke");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");

		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();
		logger.info("Discount Amount: " + discountAmount);
		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, 40.0, "Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7312 Redemption 2.0: Verify the Receipt qualifies based on net amount excluding max priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7312_verifyRedemption2_0_ReceiptExcludingMaxItem_OnlyBase_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 7.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "8", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "4", "M", "101", "100", "2.1", "102" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "8", "M", "101", "100", "3.0", "103" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "15", "P", "108", "100", "4.0", "108" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "5.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "5", "T", "101", "100", "6.0", "108" });
		addDetails.accept("Service", new String[] { "Service", "1", "5", "S", "101", "100", "7.0", "109" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("POS Discount Lookup API call is successful");

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountAmount, 25.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Sandwich", "Pizza", "Toppings", "Coke");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");

		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();
		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, 25.0, "Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7313 Redemption 2.0: [Only Modifiers] Verify the Receipt qualifies based on net amount excluding min priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7313_verifyRedemption2_0_ReceiptExcludingMinItem_OnlyModifiers_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114,115
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114,115").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_min_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 30.5, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "2.5", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1.5", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "15", "M", "101", "100", "3.0", "103" });
		addDetails.accept("Ice", new String[] { "Ice", "1", "2", "M", "101", "100", "3.1", "113" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "10", "P", "101", "100", "3.2", "101" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "6.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "101", "100", "7.0", "101" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "101", "100", "8.0", "101" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmount = 16.0;
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Cheese", "Toppings", "Ice");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");

		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();
		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, expectedDiscountAmount,
				"Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7314 Redemption 2.0: [Only Modifiers] Verify the Receipt qualifies based on net amount excluding max priced item with only M items, when Tax and Delivery Fee also included in the receipt.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7314_verifyRedemption2_0_ReceiptExcludingMaxItem_OnlyModifiers_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114,115
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114,115").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 29.5, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "5", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "2.5", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1.5", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Coke", new String[] { "Coke", "1", "15", "M", "101", "100", "3.0", "103" });
		addDetails.accept("Ice", new String[] { "Ice", "1", "2", "M", "101", "100", "3.1", "113" });
		addDetails.accept("CreditCard", new String[] { "CreditCard", "1", "10", "P", "101", "100", "3.2", "101" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "101", "100", "6.0", "107" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "101", "100", "7.0", "101" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "101", "100", "8.0", "101" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmount = 16.0;
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Cheese", "Toppings", "Ice");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");
		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();
		logger.info("Discount Amount: " + discountAmount);
		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, expectedDiscountAmount,
				"Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7309 Redemptions 2.0- Base & Modiifers-Verify the Receipt qualifies based on net amount excluding min priced item with only M items, add Tax & Delivery fees.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7309_verifyRedemption2_0_ReceiptExcludingMinItem_BaseAndModifiers_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_min_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalID, 8.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "9", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "1", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "1", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Sauces", new String[] { "Sauces", "1", "8", "M", "112", "100", "2.2", "113" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "5", "M", "112", "100", "3.0", "103" });
		addDetails.accept("Lettuce", new String[] { "Lettuce", "1", "2", "M", "112", "100", "3.1", "114" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "112", "100", "4.0", "109" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "112", "100", "5.0", "104" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "112", "100", "6.0", "107" });
		addDetails.accept("Payment", new String[] { "Payment", "1", "4", "P", "112", "100", "7.0", "2001" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmount = 27.0;
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Sandwich", "Pizza", "Burger");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");

		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();
		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, expectedDiscountAmount,
				"Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");
		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@Test(description = "SQ-T7310 Redemptions 2.0- Base & Modiifers-Verify the Receipt qualifies based on net amount excluding max priced item with only M items, add Tax & Delivery fees", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7310_verifyRedemption2_0_ReceiptExcludingMaxItem_BaseAndModifiers_TaxAndDelivery() throws Exception {

		enableDecoupledRedemptionAndReturnQCItemsFlag("false", "true");

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101,102,103,104 and modifiers 111,112,113,114
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101,102,103,104")
				.addModifierClause("item_id", "in", "111,112,113,114").build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created LIS External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		// net_amount_excluding_max_priced_item_equal_to_or_more_than
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalID, 5.0, 2)
				.build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		/*
		 * Sandwich|1|5|M|101|1001|1000|1.0 Pizza|1|10|M|101|1001|1000|2.0
		 * coke|1|15|M|101|1001|1000|3.0
		 * 
		 * Cheese|1|2.5|M|111|1001|1000|1.1 Toppings|1|1.5|M|101|1001|1000|2.1
		 * 
		 * Ice|1|2|M|101|1001|1000|3.1 Discount|1|2|D|107|1001|1000|5.0
		 * 
		 * Delivery|1|10|T|107|1001|1000|6.0 Service|1|10|S|107|1001|1000|7.0
		 * CreditCard|1|10|P|101|1001|1000|3.2
		 */

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "9", "M", "101", "100", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "1", "M", "111", "100", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "1", "M", "101", "100", "2.0", "102" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "1", "M", "112", "100", "2.1", "112" });
		addDetails.accept("Sauces", new String[] { "Sauces", "1", "8", "M", "112", "100", "2.2", "113" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "5", "M", "112", "100", "3.0", "103" });
		addDetails.accept("Lettuce", new String[] { "Lettuce", "1", "2", "M", "112", "100", "3.1", "114" });
		addDetails.accept("Discount", new String[] { "Discount", "1", "2", "D", "112", "100", "4.0", "109" });
		addDetails.accept("Delivery", new String[] { "Delivery", "1", "10", "T", "112", "100", "5.0", "104" });
		addDetails.accept("Service", new String[] { "Service", "1", "10", "S", "112", "100", "6.0", "107" });
		addDetails.accept("Payment", new String[] { "Payment", "1", "4", "P", "112", "100", "7.0", "2001" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "60", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "60", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Extract values
		Double discountAmount = discountLookupResponse.jsonPath().getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_id");
		String discountTypeLookup = discountLookupResponse.jsonPath().getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmount = 27.0;
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate items with item_type = 'M' ---
		List<String> qualifiedMItems = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'M' }.item_name");

		// Expected item names
		List<String> expectedMItems = List.of("Sandwich", "Pizza", "Burger");

		Assert.assertEquals(qualifiedMItems, expectedMItems, " M-type qualified items mismatch!");
		utils.logit("M-type qualified items matched");

		utils.logit("Discount validation successful!");

		// Extract all R-type amounts (negative values)
		List<Double> rItemAmounts = discountLookupResponse.jsonPath()
				.getList("selected_discounts[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Calculate absolute sum of all R-item amounts
		double sumOfRItems = rItemAmounts.stream().mapToDouble(Math::abs).sum();

		utils.logit("Discount Amount: " + discountAmount);

		// Log sum of R-item amounts
		utils.logit("Sum of R-item amounts: " + sumOfRItems);

		// Validation: sum of R amounts == discount_amount
		Assert.assertEquals(sumOfRItems, discountAmount, "Discount amount does not match the sum of R item amounts!");
		utils.logPass("Discount amount matches sum of R items!");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "60",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		// Extract all R-type amounts
		List<Double> rItemAmountsBatch = batchRedemptionProcessResponse.jsonPath()
				.getList("success[0].qualified_items.findAll { it.item_type == 'R' }.amount", Double.class);

		// Sum of absolute values of R items
		double sumOfRItemsBatch = rItemAmountsBatch.stream().mapToDouble(Math::abs).sum();
		utils.logit("Sum of R-type item amounts from batch redemption: " + sumOfRItemsBatch);

		// Validate that discount_amount equals sum of R items
		Assert.assertEquals(sumOfRItemsBatch, discountAmountBatchRedemption,
				"discount_amount does not match sum of R-type items!");
		utils.logPass("discount_amount matches sum of R-type items!");

		// Validate discount amount
		Assert.assertEquals(discountAmountBatchRedemption, expectedDiscountAmount,
				"Incorrect discount_amount in batch redemption");
		utils.logPass("Correct discount_amount in batch redemption");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		try {
			// Delete LIS, QC and Redeemable Data
			utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

			// Clean up test data
			pageObj.utils().clearDataSet(dataSet);

		} catch (Exception e) {
			utils.logit("warn", "Failed to clear dataset: " + e.getMessage());
		} finally {
			try {
				if (driver != null) {
					driver.quit(); // safer than driver.close(), closes all windows and ends session
					utils.logInfo("Browser closed");
				}
			} catch (Exception e) {
				utils.logit("error", "Failed to close browser: " + e.getMessage());
			}
		}
	}
}
