package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Arrays;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.LineItemSelectorPayloadBuilder;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OMM1536LineItemFilterOrSupportFlagTrueTest {
	private static final Logger logger = LogManager.getLogger(OMM1536LineItemFilterOrSupportFlagTrueTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	private ApiPayloadObj apipayloadObj;
	private String externalUID;
	Utilities utils;
	ApiUtils apiUtils;
	String firstId, secondId, thirdId, fourthId, fifthId, qcExternalID, qcExternalID1, redeemableExternalID,
			redeemableExternalID1;
	String businessId;

	// enable/disable flag-enable_decoupled_redemption_engine
	private void enableDecoupledRedemptionFlag(String decoupleFlagValue) throws Exception {

		String businessPreferenceLiveQuery = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		// Enable decoupled redemption engine
		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				decoupleFlagValue, "enable_decoupled_redemption_engine", businessId);
		Assert.assertTrue(businessLiveFlag,
				"enable_decoupled_redemption_engine value is not updated to " + decoupleFlagValue);
		utils.logit("enable_decoupled_redemption_engine value is updated to " + decoupleFlagValue);

	}

	@BeforeClass(alwaysRun = true)
	public void openBrowser() throws JsonProcessingException {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws JsonProcessingException {
		String sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		utils = new Utilities();
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		apiUtils = new ApiUtils();
		businessId = dataSet.get("business_id");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7028 Sum of Amounts | Base & Modifiers | Verify single offer qualifies when only one condition matches for OR operator ")
	@Owner(name = "Rahul Garg")
	public void T7028_testSumOfAmounts_BaseAndModifiers_OrOperatorSingleCondition() throws Exception {

		enableDecoupledRedemptionFlag("true");

		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated dynamic QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");
		utils.logPass("LIS created successfully: " + lisResponse.prettyPrint());

		// Get LIS IDs
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "LIS external ID is empty");
		utils.logInfo("Fetched LIS IDs: First=" + firstId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addItemQualifier("line_item_exists", firstId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC is created with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable is created with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 15.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-10.0, -5.0));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7029 Sum of Amounts | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MIN)")
	@Owner(name = "Rahul Garg")
	public void T7029_testSumOfAmounts_BaseAndModifiers_OrOperatorTwoConditionWithMin() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID missing");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID missing");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful with UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 15.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-10.0, -5.0));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7038 Sum of Amounts | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MAX)")
	@Owner(name = "Rahul Garg")
	public void T7038_testSumOfAmounts_BaseAndModifiers_OrOperatorTwoConditionWithMax() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");

		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 28.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Burger", "Cheese"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Burger DISCOUNT", "Cheese DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(20.0, 8.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-20.0, -8.0));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7030 Sum of Amounts | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MIN)")
	@Owner(name = "Rahul Garg")
	public void T7030_testSumOfAmounts_BaseAndModifiers_OrOperatorWithMin_AllLIFConditionsMatch() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId + ", Third="
				+ thirdId + ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 15.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_1", "Fries_1"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_1 DISCOUNT", "Fries_1 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-10.0, -5.0));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7039 Sum of Amounts | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MAX)")
	@Owner(name = "Rahul Garg")
	public void T7039_testSumOfAmounts_BaseAndModifiers_OrOperatorWithMax_AllLIFConditionsMatch() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId + ", Third="
				+ thirdId + ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 59.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_5", "Fries_5"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_5 DISCOUNT", "Fries_5 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(50.0, 9.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-50.0, -9.0));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7027 Sum of Amounts | Base & Modifiers | Verify the discount calculation for Offer 1 & Offer 2 with 50 % discount for OR Operator")
	@Owner(name = "Rahul Garg")
	public void T7027_testOrOperator_Offer1Offer2_50PctDiscount() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");

		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ========Second Offer============

		String secondQCName = "POS_QC_" + Utilities.getTimestamp();
		String secondRedeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// ===== Build QC Payload =====
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(secondQCName)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("sum_amounts")
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID1 = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID1, "QC External ID is null!");
		Assert.assertFalse(qcExternalID1.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID1);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = redeemableBuilder1.startNewData().setName(secondRedeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID1);

		// ===== Fetch Redeemable ID from DB =====
		String dbsecondOffer = DBUtils.executeQueryAndGetColumnValue(env,
				OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1), "id");
		Assert.assertNotNull(dbsecondOffer, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbsecondOffer);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response firstOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(firstOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");

		Response secondOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbsecondOffer, "", "");
		Assert.assertEquals(secondOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String firstRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		Assert.assertNotNull(firstRewardId, "Reward ID is null");
		Assert.assertFalse(firstRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("First Reward ID fetched: " + firstRewardId);

		String secondRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbsecondOffer);
		Assert.assertNotNull(secondRewardId, "Reward ID is null");
		Assert.assertFalse(secondRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Second Reward ID fetched: " + secondRewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "8", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "3", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "23", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String secondDiscountId = redemptionResponse.jsonPath().getString("discount_basket_items[1].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, firstRewardId);
		Assert.assertEquals(secondDiscountId, secondRewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "23", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmountFirstReward = jp.getDouble("selected_discounts[0].discount_amount");
		Double discountAmounSecondReward = jp.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupFirstReward = jp.getString("selected_discounts[0].discount_id");
		String discountIdLookupSecondReward = jp.getString("selected_discounts[1].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmountFirstReward, 5.5);
		Assert.assertEquals(discountAmounSecondReward, 2.75);
		Assert.assertEquals(discountIdLookupFirstReward, firstRewardId);
		Assert.assertEquals(discountIdLookupSecondReward, secondRewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name",
				List.of("Sandwich_2", "Fries_2", "Sandwich_2", "Fries_2"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT", "Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(8.0, 3.0, 4.0, 1.5));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-4.0, -1.5, -2.0, -0.75));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7032 Rate Rollback | Base & Modifiers | Verify single offer qualifies when only one condition matches for OR operator")
	@Owner(name = "Rahul Garg")
	public void T7032_testRateRollback_OrOperator_SingleConditionQualifies() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		utils.logPass("LIS created successfully with ID: " + firstId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addItemQualifier("line_item_exists", firstId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "15", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");
		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 2.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-1.33, -0.67));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7033 Rate Rollback | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MIN)")
	@Owner(name = "Rahul Garg")
	public void T7033_testRateRollback_OrOperator_TwoConditionsQualify_Min() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "43", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 2.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-1.33, -0.67));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7040 Rate Rollback | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MAX)")
	@Owner(name = "Rahul Garg")
	public void T7040_testRateRollback_OrOperator_TwoConditionsQualify_Max() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "43", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 2.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Burger", "Cheese"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Burger DISCOUNT", "Cheese DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(20.0, 8.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-1.43, -0.57));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7044 Rate Rollback | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MIN)")
	@Owner(name = "Rahul Garg")
	public void T7044_testRateRollback_OrOperator_AllConditionsQualify_Min() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId + ", Third="
				+ thirdId + ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "185", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 2.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_1", "Fries_1"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_1 DISCOUNT", "Fries_1 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-1.33, -0.67));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7041 Rate Rollback | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MAX)")
	@Owner(name = "Rahul Garg")
	public void T7041_testRateRollback_OrOperator_AllConditionsQualify_Max() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId + ", Third="
				+ thirdId + ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "185", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 2.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_5", "Fries_5"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_5 DISCOUNT", "Fries_5 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(50.0, 9.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-1.69, -0.31));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7031 Rate Rollback | Base & Modifiers | Verify the discount calculation for Offer 1 & Offer 2 with 50 % discount for OR Operator")
	@Owner(name = "Rahul Garg")
	public void T7031_testRateRollback_OrOperator_Offer1Offer2_50PctDiscount() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ========Second Offer============

		String secondQCName = "POS_QC_" + Utilities.getTimestamp();
		String secondRedeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// ===== Build QC Payload =====
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(secondQCName)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("rate_rollback").setUnitDiscount(3.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID1 = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID1, "QC External ID is null!");
		Assert.assertFalse(qcExternalID1.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID1);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = redeemableBuilder1.startNewData().setName(secondRedeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID1);

		// ===== Fetch Redeemable ID from DB =====
		String dbsecondOffer = DBUtils.executeQueryAndGetColumnValue(env,
				OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1), "id");
		Assert.assertNotNull(dbsecondOffer, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbsecondOffer);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response firstOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(firstOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");

		Response secondOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbsecondOffer, "", "");
		Assert.assertEquals(secondOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String firstRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		Assert.assertNotNull(firstRewardId, "Reward ID is null");
		Assert.assertFalse(firstRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("First Reward ID fetched: " + firstRewardId);

		String secondRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbsecondOffer);
		Assert.assertNotNull(secondRewardId, "Reward ID is null");
		Assert.assertFalse(secondRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Second Reward ID fetched: " + secondRewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "8", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "3", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "23", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String secondDiscountId = redemptionResponse.jsonPath().getString("discount_basket_items[1].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, firstRewardId);
		Assert.assertEquals(secondDiscountId, secondRewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "23", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmountFirstReward = jp.getDouble("selected_discounts[0].discount_amount");
		Double discountAmounSecondReward = jp.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupFirstReward = jp.getString("selected_discounts[0].discount_id");
		String discountIdLookupSecondReward = jp.getString("selected_discounts[1].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmountFirstReward, 1.0);
		Assert.assertEquals(discountAmounSecondReward, 1.5);
		Assert.assertEquals(discountIdLookupFirstReward, firstRewardId);
		Assert.assertEquals(discountIdLookupSecondReward, secondRewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name",
				List.of("Sandwich_2", "Fries_2", "Sandwich_2", "Fries_2"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT", "Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(8.0, 3.0, 7.27, 2.73));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-0.73, -0.27, -1.09, -0.41));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7035 Hit Target ​Menu Item Price | Base & Modifiers | Verify single offer qualifies when only one condition matches for OR operator")
	@Owner(name = "Rahul Garg")
	public void T7035_testTargetMenuItemPrice_OrOperator_SingleConditionQualifies() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS ID and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		utils.logPass("LIS created successfully with ID: " + firstId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addItemQualifier("line_item_exists", firstId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "15", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 13.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-8.67, -4.33));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7036 Hit Target ​Menu Item Price | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MIN)")
	@Owner(name = "Rahul Garg")
	public void T7036_testTargetMenuItemPrice_OrOperator_TwoConditionsQualify_Min() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "43", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 13.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich", "Fries"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich DISCOUNT", "Fries DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-8.67, -4.33));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7042 Hit Target ​Menu Item Price | Base & Modifiers  | Verify single offer qualifies when two condition matches for OR operator (MAX)")
	@Owner(name = "Rahul Garg")
	public void T7042_testTargetMenuItemPrice_OrOperator_TwoConditionsQualify_Max() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created successfully with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.addItemQualifier("line_item_exists", firstId, 0.0, 1)
				.addItemQualifier("line_item_exists", secondId, 0.0, 1).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Burger", new String[] { "Burger", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "8", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "43", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "43", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 26.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Burger", "Cheese"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Burger DISCOUNT", "Cheese DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(20.0, 8.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-18.57, -7.43));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7037 Hit Target ​Menu Item Price | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MIN) ")
	@Owner(name = "Rahul Garg")
	public void T7037_testTargetMenuItemPrice_OrOperator_AllConditionsQualify_Min() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created with IDs: First=" + firstId + ", Second=" + secondId + ", Third=" + thirdId
				+ ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "185", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 13.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_1", "Fries_1"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_1 DISCOUNT", "Fries_1 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0, 5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-8.67, -4.33));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7043 Hit Target ​Menu Item Price | Base & Modifiers  | Verify offer qualifies when all LIF condition matches for OR operator (MAX) ")
	@Owner(name = "Rahul Garg")
	public void T7043_testTargetMenuItemPrice_OrOperator_AllConditionsQualify_Max() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		// ===== Create LIS 3 =====
		builder.startNewRule().setName("Item id-301").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "301").addModifierClause("item_id", "==", "311").addCurrentRule();
		// ===== Create LIS 4 =====
		builder.startNewRule().setName("Item id-401").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "401").addModifierClause("item_id", "==", "411").addCurrentRule();
		// ===== Create LIS 5 =====
		builder.startNewRule().setName("Item id-501").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "501").addModifierClause("item_id", "==", "511").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		thirdId = lisResponse.jsonPath().getString("results[2].external_id");
		Assert.assertNotNull(thirdId, "Third LIS external ID is null");
		Assert.assertFalse(thirdId.isEmpty(), "Third LIS external ID is empty");
		fourthId = lisResponse.jsonPath().getString("results[3].external_id");
		Assert.assertNotNull(fourthId, "Fourth LIS external ID is null");
		Assert.assertFalse(fourthId.isEmpty(), "Fourth LIS external ID is empty");
		fifthId = lisResponse.jsonPath().getString("results[4].external_id");
		Assert.assertNotNull(fifthId, "Fifth LIS external ID is null");
		Assert.assertFalse(fifthId.isEmpty(), "Fifth LIS external ID is empty");
		utils.logPass("LIS created with IDs: First=" + firstId + ", Second=" + secondId + ", Third=" + thirdId
				+ ", Fourth=" + fourthId + ", Fifth=" + fifthId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("max").setItemFilterExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0).addLineItemFilter(thirdId, "", 0)
				.addLineItemFilter(fourthId, "", 0).addLineItemFilter(fifthId, "", 0).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward ID fetched: " + rewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "20", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "6", "M", "111", "111", "2.1", "211" });
		addDetails.accept("Sandwich_3", new String[] { "Sandwich_3", "1", "30", "M", "101", "101", "3.0", "301" });
		addDetails.accept("Fries_3", new String[] { "Fries_3", "1", "7", "M", "111", "111", "3.1", "311" });
		addDetails.accept("Sandwich_4", new String[] { "Sandwich_4", "1", "40", "M", "101", "101", "4.0", "401" });
		addDetails.accept("Fries_4", new String[] { "Fries_4", "1", "8", "M", "111", "111", "4.1", "411" });
		addDetails.accept("Sandwich_5", new String[] { "Sandwich_5", "1", "50", "M", "101", "101", "5.0", "501" });
		addDetails.accept("Fries_5", new String[] { "Fries_5", "1", "9", "M", "111", "111", "5.1", "511" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "185", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "185", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmount, 57.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich_5", "Fries_5"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_5 DISCOUNT", "Fries_5 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(50.0, 9.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-48.31, -8.69));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7034 Hit Target ​Menu Item Price | Base & Modifiers | Verify the discount calculation for Offer 1 & Offer 2 with 50 % discount for OR Operator")
	@Owner(name = "Rahul Garg")
	public void T7034_testTargetMenuItemPrice_OrOperator_Offer1Offer2_50PctDiscount() throws Exception {

		enableDecoupledRedemptionFlag("true");
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logInfo("Generated QC Name: " + qcname + " | Redeemable Name: " + redeemableName);

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();
		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "==", "111").addCurrentRule();
		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "==", "211").addCurrentRule();
		String lisPayload = builder.build();

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");

		// Get LIS IDs and Validate LIS response
		firstId = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external ID is empty");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external ID is empty");
		utils.logPass("LIS created with IDs: First=" + firstId + ", Second=" + secondId);

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// ========Second Offer============

		String secondQCName = "POS_QC_" + Utilities.getTimestamp();
		String secondRedeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// ===== Build QC Payload =====
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(secondQCName)
				.setPercentageOfProcessedAmount(50).setQCProcessingFunction("hit_target_price").setTargetPrice(3.0)
				.setDiscountEvaluationStrategy("min").setItemFilterExpressionsOperator("any").setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(firstId, "", 0).addLineItemFilter(secondId, "", 0)
				.setQualifyingExpressionsOperator("any").addItemQualifier("line_item_exists", firstId, null)
				.addItemQualifier("line_item_exists", secondId, null).build();

		// ===== Create QC via API =====
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");

		// Validate QC response
		qcExternalID1 = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID1, "QC External ID is null!");
		Assert.assertFalse(qcExternalID1.isEmpty(), "QC External ID is empty!");
		utils.logPass("QC created successfully with External ID: " + qcExternalID1);

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = redeemableBuilder1.startNewData().setName(secondRedeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// ===== Create Redeemable via API =====
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1, "Redeemable External ID is null!");
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty!");
		utils.logPass("Redeemable created successfully with External ID: " + redeemableExternalID1);

		// ===== Fetch Redeemable ID from DB =====
		String dbsecondOffer = DBUtils.executeQueryAndGetColumnValue(env,
				OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1), "id");
		Assert.assertNotNull(dbsecondOffer, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbsecondOffer);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("User Signup successful. UserID: " + userId);

		// ===== Gift Reward to User =====
		Response firstOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(firstOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");

		Response secondOfferGiftResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbsecondOffer, "", "");
		Assert.assertEquals(secondOfferGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String firstRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		Assert.assertNotNull(firstRewardId, "Reward ID is null");
		Assert.assertFalse(firstRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("First Reward ID fetched: " + firstRewardId);

		String secondRewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), dbsecondOffer);
		Assert.assertNotNull(secondRewardId, "Reward ID is null");
		Assert.assertFalse(secondRewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Second Reward ID fetched: " + secondRewardId);

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich_1", new String[] { "Sandwich_1", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries_1", new String[] { "Fries_1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Sandwich_2", new String[] { "Sandwich_2", "1", "8", "M", "101", "101", "2.0", "201" });
		addDetails.accept("Fries_2", new String[] { "Fries_2", "1", "3", "M", "111", "111", "2.1", "211" });
		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "23", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String secondDiscountId = redemptionResponse.jsonPath().getString("discount_basket_items[1].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, firstRewardId);
		Assert.assertEquals(secondDiscountId, secondRewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "23", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();
		Double discountAmountFirstReward = jp.getDouble("selected_discounts[0].discount_amount");
		Double discountAmounSecondReward = jp.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupFirstReward = jp.getString("selected_discounts[0].discount_id");
		String discountIdLookupSecondReward = jp.getString("selected_discounts[1].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountAmountFirstReward, 4.5);
		Assert.assertEquals(discountAmounSecondReward, 1.75);
		Assert.assertEquals(discountIdLookupFirstReward, firstRewardId);
		Assert.assertEquals(discountIdLookupSecondReward, secondRewardId);
		Assert.assertEquals(discountTypeLookup, "reward");
		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name",
				List.of("Sandwich_2", "Fries_2", "Sandwich_2", "Fries_2"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				List.of("Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT", "Sandwich_2 DISCOUNT", "Fries_2 DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(8.0, 3.0, 4.73, 1.77));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-3.27, -1.23, -1.27, -0.48));
		utils.logPass("Item level validations passed in POS Discount Lookup");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {

		try {
			// Delete the first entry with all params
			utils.deleteLISQCRedeemable(env, firstId, qcExternalID, redeemableExternalID);

			// Delete LIS IDs
			for (String id : Arrays.asList(secondId, thirdId, fourthId, fifthId)) {
				if (id != null) {
					utils.deleteLISQCRedeemable(env, id, null, null);
				}
			}

			// Delete QC ID
			if (qcExternalID1 != null) {
				utils.deleteLISQCRedeemable(env, null, qcExternalID1, null);
			}

			// Delete Redeemable ID
			if (redeemableExternalID1 != null) {
				utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Clear Data Set
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() throws Exception {
		// enableDecoupledRedemptionFlag("false");
	}

}