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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.LineItemSelectorPayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
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
public class OMM1158DecoupleHitTargetMenuItemPriceRuleEngineFlagTrueTest {

	static Logger logger = LogManager.getLogger(OMM1158DecoupleHitTargetMenuItemPriceRuleEngineFlagTrueTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	ApiPayloads apipaylods;
	Utilities utils;
	String lisExternalID1, lisExternalID2;
	String qcExternalID1, qcExternalID2;
	String redeemableExternalID1, redeemableExternalID2;
	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";

	private ApiPayloadObj apipayloadObj;
	private String externalUID;
	private ApiUtils apiUtils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils.logInfo(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		apiUtils = new ApiUtils();
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

	}

	public enum LISType {
		BASE_ONLY, MODIFIERS_ONLY, BASE_AND_MODIFIERS
	}

	private String createLIS(LISType lisType, String baseItems, String modifierItems, String lisProcessingMethod,
			int lisMaxUnits) throws JsonProcessingException {

		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder()
				.setName("POS_LIS_" + lisType + "_" + Utilities.getTimestamp());

		switch (lisType) {

		case BASE_ONLY:
			builder.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in",
					baseItems);
			break;

		case MODIFIERS_ONLY:
			builder.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
					.addBaseItemClause("item_id", "in", baseItems).addModifierClause("item_id", "in", modifierItems)
					.setProcessingMethod(lisProcessingMethod).setMaxDiscountUnits(lisMaxUnits);
			break;

		case BASE_AND_MODIFIERS:
			builder.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
					.addBaseItemClause("item_id", "in", baseItems).addModifierClause("item_id", "in", modifierItems)
					.setProcessingMethod(lisProcessingMethod).setMaxDiscountUnits(lisMaxUnits);
			break;

		default:
			throw new IllegalArgumentException("Unsupported LIS type: " + lisType);
		}

		String payload = builder.build();

		Response response = pageObj.endpoints().createLIS(payload, dataSet.get("apiKey"));

		logger.info(response.prettyPrint());
		utils.logit("LIS Creation Response: " + response.asString());
		return response.jsonPath().getString("results[0].external_id");
	}

	private String createQC(String qcFunction, Double targetPrice, Integer percentage, Integer AmountCap,
			String lisExternalId, String lifProcessingMethod, String qualifierType, Double qualifierValue,
			boolean StackDiscounting, boolean ReuseQualifyingItems) throws JsonProcessingException {

		var builder = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction(qcFunction).setPercentageOfProcessedAmount(percentage)
				.setStackDiscounting(StackDiscounting).setReuseQualifyingItems(ReuseQualifyingItems)
				.addLineItemFilter(lisExternalId, lifProcessingMethod, 1)
				.addItemQualifier(qualifierType, lisExternalId, qualifierValue, 1);

		if (targetPrice != null) {
			builder.setTargetPrice(targetPrice);
		}
		if (AmountCap != null) {
			builder.setAmountCap(AmountCap);
		}

		Response response = pageObj.endpoints().createQC(builder.build(), dataSet.get("apiKey"));

		return response.jsonPath().getString("results[0].external_id");
	}

	private String createRedeemable(String qcExternalId) throws JsonProcessingException {
		String payload = apipayloadObj.redeemableBuilder().startNewData()
				.setName("POS_Redeemable_" + Utilities.getTimestamp()
						+ ThreadLocalRandom.current().nextLong(1000L, 5000L))
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		Response response = pageObj.endpoints().createRedeemable(payload, dataSet.get("apiKey"));

		return response.jsonPath().getString("results[0].external_id");
	}

	private String getRedeemableDbId(String externalId) throws Exception {
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", externalId);
		return DBUtils.executeQueryAndGetColumnValue(env, query, "id");
	}

	@Test(description = "SQ-T7054 [Stacking ON, Reusability ON] Verify discount calculation for Offer 1 and Offer 2 with LIS base only type and Hit Target Menu Item Price & sum of amounts processing function for both offers respectively", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7054_StackingOn_ReuseOn_TwoOffers_LisBase_HitTargetAndSumOfAmounts() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 15.0, 50, null, lisExternalID1, "min_price",
				"line_item_exists", 1.0, true, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201,202", null, null, 0);
		String qcExternalID2 = createQC("sum_amounts", null, 10, null, lisExternalID2, "min_price",
				"net_amount_greater_than_or_equal_to", 10.0, true, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "40", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "2", "40", "M", "201", "201", "2.0", "201" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "80", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "80", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 2.5;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 2.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Fries");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Fries DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(40.0, 40.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-2.5, -2.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "80",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7055 [Stacking ON, Reusability ON] Verify discount calculation for Offer 1 and Offer 2 with LIS base only type and Hit Target Menu Item Price & sum of amounts processing function for both offers respectively", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7055_testStackingReusabilityOn_BaseLIS_Offer1Offer2_PercentCap() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 15.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201,202", null, null, 0);
		String qcExternalID2 = createQC("hit_target_price", 5.0, 10, 2, lisExternalID2, "", "line_item_exists", 1.0,
				true, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "40", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Fries", new String[] { "Fries", "2", "40", "M", "201", "201", "2.0", "201" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "80", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "80", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 2.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 2.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Fries");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Fries DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(40.0, 40.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-2.0, -2.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "80",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7056 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7056_testStackingReusabilityOn_ModifiersLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112,113", "min_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.MODIFIERS_ONLY, "201,202", "211,212,213", "max_price", 2);
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, 1, lisExternalID2, "", "line_item_exists", 1.0,
				true, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "40", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "7", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "9", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "11", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "2", "25", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "7", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "9", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "11", "M", "213", "213", "2.3", "213" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "105", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 1.2;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 1.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Topping1", "Topping2", "FTopping2", "FTopping3");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Topping1 DISCOUNT", "Topping2 DISCOUNT", "FTopping2 DISCOUNT",
				"FTopping3 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(7.0, 9.0, 9.0, 11.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-0.4, -0.8, -0.43, -0.57);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7057 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS having exclude function and net quantity equal to 2.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7057_testStackingReusabilityOn_ModifiersLIS_ExcludeNetQty2_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112,113", "min_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, null, null, lisExternalID1, "exclude",
				"line_item_does_not_exist", 1.0, true, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.MODIFIERS_ONLY, "201,202", "211,212,213", "max_price", 2);
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, null, lisExternalID2, "",
				"net_quantity_equal_to", 2.0, true, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String accessToken = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "40", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "7", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "9", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "11", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "2", "25", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "7", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "9", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "11", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(accessToken,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(accessToken,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String actualMessage = discountLookupJP.getString("selected_discounts[0].message");
		String expectedPartialMessage = "Discount qualification on receipt failed";
		Assert.assertTrue(actualMessage.contains(expectedPartialMessage),
				"Expected message to contain '" + expectedPartialMessage + "' but was '" + actualMessage + "'");

		// Extract values
		double expectedDiscountAmountReward2 = 14.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("FTopping2", "FTopping3");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("FTopping2 DISCOUNT", "FTopping3 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(9.0, 11.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-6.0, -8.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7058 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with base items and modifiers type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7058_testStackingReusabilityOn_BaseAndModifiersLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112,113,114,115", "max_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_AND_MODIFIERS, "201,202", "211,212,213,214,215", "min_price", 2);
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, 2, lisExternalID2, "max_price",
				"net_quantity_greater_than_or_equal_to", 1.0, true, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "15", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "7", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "9", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "11", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "12.5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "7", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "9", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "11", "M", "213", "213", "2.3", "213" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "105", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 6.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 2.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Topping3", "Topping2", "Fries", "FTopping1",
				"FTopping2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Topping3 DISCOUNT", "Topping2 DISCOUNT",
				"Fries DISCOUNT", "FTopping1 DISCOUNT", "FTopping2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(15.0, 11.0, 9.0, 12.5, 7.0, 9.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-2.57, -1.89, -1.54, -0.88, -0.49, -0.63);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7060 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with LIS base only type and Hit Target Menu Item Price & sum of amounts processing function for both offers respectively.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7060_StackingOFF_ReuseOFF_TwoOffers_LisBase_HitTargetAndSumOfAmounts() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 50, null, lisExternalID1, "min_price",
				"line_item_exists", 1.0, false, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		String qcExternalID2 = createQC("sum_amounts", null, null, null, lisExternalID1, "min_price",
				"net_amount_greater_than_or_equal_to", 10.0, false, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "30", "M", "101", "101", "1.0", "101" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "30", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 5.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 15.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Sandwich");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Sandwich DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(30.0, 25.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-5.0, -15.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "30",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7061 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing % and amount cap.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7061_testStackingReusabilityOff_BaseLIS_Offer1Offer2_PercentCap() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				false, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		String qcExternalID2 = createQC("hit_target_price", 3.0, null, 1, lisExternalID1, "", "line_item_exists", 1.0,
				false, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "30", "M", "101", "101", "1.0", "101" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "30", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 4.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		// --- Assertions ---

		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		// validate error message in message field
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[1].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(30.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-4.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "30",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7062 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7062_testStackingReusabilityOff_ModifiersLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112", "min_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				false, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112", "max_price", 2);
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, 1, lisExternalID2, "", "line_item_exists", 1.0,
				false, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "30", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "7", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "9", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "11", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "30", "M", "101", "101", "2.0", "101" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "7", "M", "111", "111", "2.1", "111" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "9", "M", "112", "112", "2.2", "112" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "11", "M", "113", "11", "2.3", "114" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "110", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "110", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 2.4;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Assertions for Offer-2
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		// validate error message in message field
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[1].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Topping1", "Topping2", "FTopping1", "FTopping2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Topping1 DISCOUNT", "Topping2 DISCOUNT", "FTopping1 DISCOUNT",
				"FTopping2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(7.0, 9.0, 7.0, 9.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-0.4, -0.8, -0.4, -0.8);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "110",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7063 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS having exclude function and net quantity equal to 2.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7063_testStackingReusabilityOff_ModifiersLIS_ExcludeNetQty2_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112,113,114,115", "min_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, null, null, lisExternalID1, "exclude",
				"line_item_does_not_exist", 1.0, false, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, null, lisExternalID1, "",
				"net_quantity_equal_to", 2.0, false, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "30", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "7", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "9", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "11", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "25", "M", "101", "101", "2.0", "101" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "7", "M", "111", "111", "2.1", "111" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "9", "M", "112", "112", "2.2", "112" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "11", "M", "113", "11", "2.3", "114" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// Validate message for Offer-1
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Discount qualification on receipt failed"));

		// Assertions for Offer-2
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		// validate error message in message field
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[1].message")
				.contains("Discount qualification on receipt failed"));

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate message for Offer-1
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Discount qualification on receipt failed"));
		// Validate message for Offer-2
		Assert.assertTrue(
				batchJP.getString("failures[1].message").contains("Discount qualification on receipt failed"));

	}

	@Test(description = "SQ-T7064 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base items and modifiers type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7064_testStackingReusabilityOff_BaseAndModifiersLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112,113,114,115", "max_price", 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				false, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112,113,114,115", "min_price", 2);
		String qcExternalID2 = createQC("hit_target_price", 3.0, null, 1, lisExternalID2, "max_price",
				"net_quantity_greater_than_or_equal_to", 2.0, false, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "8", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "3", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "4", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "5", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "6", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "7", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 2.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");
		// validate error message in message field
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[1].message")
				.contains("Discount qualification on receipt failed"));

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Topping3", "Topping2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Topping3 DISCOUNT", "Topping2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(8.0, 4.0, 3.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-1.07, -0.53, -0.4);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7065 [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 with base items and modifiers type LIS having exclude function.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7065_testStackingReusabilityOff_BaseAndModifiersLIS_Exclude_Offer1() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112,113,114,115", null, 3);
		String lisExternalID2 = createLIS(LISType.BASE_AND_MODIFIERS, "201,202", "211, 212, 213, 214, 215", null, 3);

		var builder = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("hit_target_price").setStackDiscounting(false).setReuseQualifyingItems(false)
				.setTargetPrice(5.0).addLineItemFilter(lisExternalID1, "", 1)
				.addLineItemFilter(lisExternalID2, "exclude", null)
				.addItemQualifier("line_item_exists", lisExternalID1, 1.0);

		Response response = pageObj.endpoints().createQC(builder.build(), dataSet.get("apiKey"));

		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not match for QC creation");

		String qcExternalID1 = response.jsonPath().getString("results[0].external_id");
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "8", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "3", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "4", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "5", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "6", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "7", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "40", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 12.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Topping1", "Topping2", "Topping3");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Topping1 DISCOUNT", "Topping2 DISCOUNT",
				"Topping3 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(8.0, 2.0, 3.0, 4.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-5.65, -1.41, -2.12, -2.82);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "40",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7066 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with LIS base only type and Hit Target Menu Item Price & sum of amounts processing function for both offers respectively.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7066_testStackingOnReusabilityOff_BaseOnlyLIS_Offer1Offer2_HitTargetAndSumAmounts() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 50, null, lisExternalID1, "min_price",
				"line_item_exists", 1.0, true, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201,202", null, null, 0);
		String qcExternalID2 = createQC("sum_amounts", null, null, null, lisExternalID2, "min_price",
				"net_amount_greater_than_or_equal_to", 10.0, true, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "2", "5", "M", "102", "102", "2.0", "102" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "8", "M", "201", "201", "3.0", "201" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "6", "M", "202", "202", "4.0", "202" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message in message field
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		// Extract values
		String discountAmountLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(Double.parseDouble(discountAmountLookupReward2), 6.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Topping3");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Topping3 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(6.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-6.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		double discountAmountBatchRedemption = batchRedemptionProcessResponse.jsonPath()
				.getDouble("success[0].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption, 6.0, "Incorrect discount_amount");
		utils.logit("Discount Amount from batch redemption: " + discountAmountBatchRedemption);

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// validate error in failures
		String failureMessage = batchJP.getString("failures[0].message");
		Assert.assertTrue(failureMessage.contains("Redemption not possible since amount is 0"));

	}

	@Test(description = "SQ-T7067 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing % and amount cap.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7067_testStackingOnReusabilityOff_BaseOnlyLIS_Offer1Offer2_PercentAndCap() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201,202", null, null, 0);
		String qcExternalID2 = createQC("hit_target_price", 1.0, null, null, lisExternalID2, "min_price",
				"net_quantity_equal_to", 2.0, true, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "3", "15", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "3", "12", "M", "201", "201", "2.0", "201" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "27", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message in message field
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		// Extract values
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[1].message")
				.contains("Discount qualification on receipt failed"));

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "27",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// validate error in failures
		String failureMessage1 = batchJP.getString("failures[0].message");
		Assert.assertTrue(failureMessage1.contains("Redemption not possible since amount is 0"));

		String failureMessage2 = batchJP.getString("failures[1].message");
		Assert.assertTrue(failureMessage2.contains("Discount qualification on receipt failed"));

	}

	@Test(description = "SQ-T7068 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7068_testStackingOnReusabilityOff_ModifiersOnlyLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112", null, 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.MODIFIERS_ONLY, "201,202", "211,212", null, 2);
		String qcExternalID2 = createQC("hit_target_price", 1.0, null, null, lisExternalID2, "", "line_item_exists",
				1.0, true, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		Assert.assertFalse(rewardId1.isEmpty(), "Reward Id for user is empty");
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "4", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "10", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "6", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "4", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "5", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "6", "M", "213", "213", "2.3", "213" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "110", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "110", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 1.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Assertions for Offer-2
		double expectedDiscountAmountReward2 = 7.0;
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		// validate error message in message field
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountLookupJP.getDouble("selected_discounts[1].discount_amount"),
				expectedDiscountAmountReward2, "Incorrect discount_amount");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Topping2", "FTopping1", "FTopping2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Topping2 DISCOUNT", "FTopping1 DISCOUNT", "FTopping2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(10.0, 4.0, 5.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-1.0, -3.0, -4.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "110",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Assertions for Offer-1
		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption1, 1.0, "Incorrect discount_amount");

		// Assertions for Offer-2
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption2, 7.0, "Incorrect discount_amount");

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7069 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with modifiers only type LIS having exclude function and net quantity equal to 2.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7069_testStackingOnReusabilityOff_ModifiersOnlyLIS_ExcludeNetQty2_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.MODIFIERS_ONLY, "101,102", "111,112", null, 2);
		String qcExternalID1 = createQC("hit_target_price", 5.0, null, null, lisExternalID1, "exclude",
				"line_item_does_not_exist", 1.0, true, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.MODIFIERS_ONLY, "201,202", "211,212", null, 2);
		String qcExternalID2 = createQC("hit_target_price", 1.0, null, null, lisExternalID2, "",
				"net_quantity_equal_to", 2.0, true, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "4", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "10", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "6", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "4", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "5", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "6", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// Validate message for Offer-1
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Discount qualification on receipt failed"));

		// Assertions for Offer-2
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		// validate error message in message field
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountLookupJP.getDouble("selected_discounts[1].discount_amount"), 7.0,
				"Incorrect discount_amount");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("FTopping1", "FTopping2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("FTopping1 DISCOUNT", "FTopping2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(4.0, 5.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-3.0, -4.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// Validate message for Offer-1
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Discount qualification on receipt failed"));

	}

	@Test(description = "SQ-T7070 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base items and modifiers type LIS.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7070_testStackingOnReusabilityOff_BaseAndModifiersLIS_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112", null, 3);
		String qcExternalID1 = createQC("hit_target_price", 5.0, 20, null, lisExternalID1, "", "line_item_exists", 1.0,
				true, false);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_AND_MODIFIERS, "201,202", "211,212,213,214,215", null, 3);
		String qcExternalID2 = createQC("hit_target_price", 1.0, null, null, lisExternalID2, "max_price",
				"net_quantity_greater_than_or_equal_to", 1.0, true, false);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "8", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "3", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "4", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "2", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "5", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "6", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "7", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Add reward-2 into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "105", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 1.6;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values for Offer-2
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");

		// --- Assertions ---
		double expectedDiscountAmountReward2 = 10.5;
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Topping1", "Topping2", "Fries", "FTopping1",
				"FTopping2", "FTopping3");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Topping1 DISCOUNT", "Topping2 DISCOUNT",
				"Fries DISCOUNT", "FTopping1 DISCOUNT", "FTopping2 DISCOUNT", "FTopping3 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(8.0, 2.0, 3.0, 5.0, 5.0, 6.0, 7.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-0.99, -0.25, -0.37, -1.28, -2.56, -3.07, -3.59);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "105",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Assertions for Offer-1
		double discountAmountBatchRedemption = batchJP.getDouble("success[0].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption, 1.6, "Incorrect discount_amount for Offer-1");

		// Assertions for Offer-2
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption2, 10.5, "Incorrect discount_amount for Offer-2");

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7071 [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 with base items and modifiers type LIS having exclude function for both offers.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7071_testStackingOnReusabilityOff_BaseAndModifiersLIS_Exclude_Offer1Offer2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_AND_MODIFIERS, "101,102", "111,112,113,114,115", null, 3);
		String lisExternalID2 = createLIS(LISType.BASE_AND_MODIFIERS, "201,202", "211, 212, 213, 214, 215", null, 3);

		var builder = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("hit_target_price").setStackDiscounting(true).setReuseQualifyingItems(false)
				.setTargetPrice(5.0).addLineItemFilter(lisExternalID1, "", 1)
				.addLineItemFilter(lisExternalID2, "exclude", null)
				.addItemQualifier("line_item_does_not_exist", lisExternalID1, 1.0);

		Response response = pageObj.endpoints().createQC(builder.build(), dataSet.get("apiKey"));

		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not match for QC creation");

		String qcExternalID1 = response.jsonPath().getString("results[0].external_id");
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "8", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Topping1", new String[] { "Topping1", "1", "2", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Topping2", new String[] { "Topping2", "1", "3", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Topping3", new String[] { "Topping3", "1", "4", "M", "113", "113", "1.3", "113" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "5", "M", "201", "201", "2.0", "201" });
		addDetails.accept("FTopping1", new String[] { "FTopping1", "1", "5", "M", "211", "211", "2.1", "211" });
		addDetails.accept("FTopping2", new String[] { "FTopping2", "1", "6", "M", "212", "212", "2.2", "212" });
		addDetails.accept("FTopping3", new String[] { "FTopping3", "1", "7", "M", "213", "213", "2.3", "213" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "40", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values

		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message in message field
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Discount qualification on receipt failed"), "Incorrect message");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "40",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// validate error message in message field
		Assert.assertTrue(batchJP.getString("failures[0].message").contains("Discount qualification on receipt failed"),
				"Incorrect message");
		utils.logPass("Discount details are correct in POS batch redemption API response");

	}

	@Test(description = "SQ-T7072 [Stacking OFF, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIS base only type and Hit Target Menu Item Price & sum of amounts processing function for both offers respectively.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7072_testStackingOffReusabilityOn_BaseOnlyLIS_Offer1Offer2_HitTargetAndSumAmounts() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 5.0, null, null, lisExternalID1, "min_price",
				"line_item_exists", 1.0, false, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201,202", null, null, 0);
		String qcExternalID2 = createQC("sum_amounts", null, null, null, lisExternalID2, "min_price",
				"net_amount_greater_than_or_equal_to", 10.0, false, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "2", "12", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Bread", new String[] { "Bread", "2", "15", "M", "102", "102", "2.0", "102" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "8", "M", "201", "201", "3.0", "201" });
		addDetails.accept("Potato", new String[] { "Potato", "1", "6", "M", "202", "202", "4.0", "202" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "80", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "80", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 1.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 6.0;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Potato");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Potato DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(12.0, 6.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-1.0, -6.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "80",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		// Assertions for Offer-1 and Offer-2
		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount for Offer-1");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount for Offer-2");

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@Test(description = "SQ-T7073 [Stacking OFF, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing % and amount cap.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7073_testStackingOffReusabilityOn_BaseOnlyLIS_Offer1Offer2_PercentAndCap() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================
		String lisExternalID1 = createLIS(LISType.BASE_ONLY, "101,102", null, null, 0);
		String qcExternalID1 = createQC("hit_target_price", 0.5, null, null, lisExternalID1, "", "line_item_exists",
				1.0, false, true);
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================
		String lisExternalID2 = createLIS(LISType.BASE_ONLY, "201", null, null, 0);
		String qcExternalID2 = createQC("hit_target_price", 1.0, null, null, lisExternalID2, "", "line_item_exists",
				1.0, false, true);
		String redeemableExternalID2 = createRedeemable(qcExternalID2);
		String dbId2 = getRedeemableDbId(redeemableExternalID2);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// Extract userId and token
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse1 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId1, "", "");
		Assert.assertEquals(sendMessageToUserResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId1);
		utils.logPass("Reward Id for user is fetched: " + rewardId1);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse2 = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbId2, "", "");
		Assert.assertEquals(sendMessageToUserResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbId2);
		utils.logPass("Reward Id for user is fetched: " + rewardId2);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		// Input Receipt Details
		addDetails.accept("Sandwich", new String[] { "Sandwich", "3", "15", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "3", "12", "M", "201", "201", "2.0", "201" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		Assert.assertEquals(items.get(1).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(1).get("discount_id"), rewardId2, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "30", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 13.5;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 9.0;
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Cheese");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Cheese DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(15.0, 12.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-13.5, -9.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "30",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		// Assertions for Offer-1 and Offer-2
		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount for Offer-1");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount for Offer-2");

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		apiUtils.validateItems(batchJP, "success", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {

		// disable decoupled redemption engine flag
		// utils.updatePreferenceFlag(env, "businesses", "preferences", "id",
		// dataSet.get("business_id"),
		// "enable_decoupled_redemption_engine", "false");

		// Delete LIS, QC and Redeemable Data
		String[][] offers = { { lisExternalID1, qcExternalID1, redeemableExternalID1 },
				{ lisExternalID2, qcExternalID2, redeemableExternalID2 } };

		for (String[] r : offers) {
			utils.deleteLISQCRedeemable(env, r[0], r[1], r[2]);
		}

		// Clear Data Set
		pageObj.utils().clearDataSet(dataSet);
		utils.logInfo("Data set cleared");
	}

}