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
public class OMM1167DecoupleRuleTargetBundlePriceAdvancedFlagTrueTest {

	static Logger logger = LogManager.getLogger(OMM1167DecoupleRuleTargetBundlePriceAdvancedFlagTrueTest.class);
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
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
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

	@Test(description = "SQ-T7220 Stacking-OFF, Reusability-Off>Verify the behavior of a single bundle when two \"Target Price for Bundle (Advanced)\" offers are applied with stacking disabled", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7220_testStackingReusabilityOff_SingleBundle_TwoTargetPriceAdvancedOffers() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// Create LIS, QC and Redeemable for two offers

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(30.0).setStackDiscounting(false)
				.setReuseQualifyingItems(false).addLineItemFilter(lif1, "", 1).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(20.0).setStackDiscounting(false)
				.setReuseQualifyingItems(false).addLineItemFilter(lif1, "", 1).addLineItemFilter(lif2, "", 1)
				.addItemQualifier(lif1, null, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "20", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Sandwich2",
				new String[] { "Sandwich2", "1", "12", "M", "22222211", "22222211", "2.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "17", "M", "8001", "8001", "3.0", "8001" });
		addDetails.accept("Fries2", new String[] { "Fries2", "1", "10", "M", "8001", "8001", "4.0", "8001" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "59", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "59", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 7.0;
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
		List<String> expectedMNames = Arrays.asList("Sandwich1", "Fries1", "Sandwich2", "Fries2");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich1 DISCOUNT", "Fries1 DISCOUNT", "Sandwich2 DISCOUNT",
				"Fries2 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(20.0, 17.0, 12.0, 10.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-3.78, -3.22, -1.09, -0.91);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "59",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7221 Stacking ON>Test bundle target advanced for double bundle two offer and stacking ON and multiple qty in receipt 2", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7221_testStackingOn_DoubleBundle_TwoOffers_MultiQtyReceipt2() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "min_price", 1)
				.addLineItemFilter(lif2, "max_price", 3).addItemQualifier("line_item_exists", lif1, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(6.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "", 1).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "3", "10", "M", "22222211", "22222211", "1.0", "22222211" });

		addDetails.accept("Fries1", new String[] { "Fries1", "5", "40", "M", "8001", "8001", "2.0", "8001" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "50", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 17.33;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 0.52;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich1", "Fries1", "Sandwich1", "Fries1");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich1 DISCOUNT", "Fries1 DISCOUNT", "Sandwich1 DISCOUNT",
				"Fries1 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(10.0, 40.0, 7.89, 24.78);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-2.11, -15.22, -0.28, -0.24);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7222 Use-case-1:Test bundle target advanced with bundle created with different qty", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7222_1_testBundleTargetAdvanced_BundleWithDifferentQty() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "max_price", 2)
				.addLineItemFilter(lif2, "min_price", 1).addItemQualifier("line_item_exists", lif1, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(6.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "", 1).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "3", "20", "M", "22222211", "22222211", "1.0", "22222211" });

		addDetails.accept("Fries1", new String[] { "Fries1", "2", "4", "M", "8001", "8001", "2.0", "8001" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "24", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "24", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 5.34;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 0.35;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich1", "Fries1", "Sandwich1", "Fries1");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich1 DISCOUNT", "Fries1 DISCOUNT", "Sandwich1 DISCOUNT",
				"Fries1 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(20.0, 4.0, 15.36, 3.3);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-4.64, -0.7, -0.24, -0.11);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "15",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7222 Use-case-2:Test bundle target advanced with bundle created with different qty", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7222_2_testBundleTargetAdvanced_BundleWithDifferentQty() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "min_price", 2)
				.addLineItemFilter(lif2, "min_price", 1).addItemQualifier(lif1, null, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "3", "30", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Sandwich2",
				new String[] { "Sandwich2", "3", "12", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "2", "4", "M", "8001", "8001", "2.0", "8001" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "46", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, autoSelectResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		List<Map<String, Object>> items = autoSelectResponse.jsonPath().getList("discount_basket_items");

		// Validate response in Auth Auto Select
		Assert.assertEquals(items.get(0).get("discount_type"), "reward", "Incorrect discount_type!");
		Assert.assertEquals(items.get(0).get("discount_id"), rewardId1, "Incorrect discount_id!");

		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "46", externalUID, parentMap);
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

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich1", "Sandwich2", "Fries1");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich1 DISCOUNT", "Sandwich2 DISCOUNT", "Fries1 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(30.0, 12.0, 4.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-3.75, -1.5, -0.75);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "46",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7222 Use-case-3:Test bundle target advanced with bundle created with different qty", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7222_3_testBundleTargetAdvanced_BundleWithDifferentQty() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "min_price", 1)
				.addLineItemFilter(lif2, "max_price", 3).addItemQualifier("line_item_exists", lif1, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(6.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "", 1).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "3", "10", "M", "22222211", "22222211", "1.0", "22222211" });

		addDetails.accept("Fries1", new String[] { "Fries1", "5", "40", "M", "8001", "8001", "2.0", "8001" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "50", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 17.33;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		// Extract values
		double expectedDiscountAmountReward2 = 0.52;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich1", "Fries1", "Sandwich1", "Fries1");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich1 DISCOUNT", "Fries1 DISCOUNT", "Sandwich1 DISCOUNT",
				"Fries1 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(10.0, 40.0, 7.89, 24.78);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-2.11, -15.22, -0.28, -0.24);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7223 POS discount lookup API>Reusability-ON,Stacking-ON>Only Base Items>Validate that error message gets displayed when LIF is having bundle of item with qty-2 but in input receipt item is present with qty-1 only", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7223_testPOSDiscountLookup_ReusabilityOnStackingOn_BaseOnly_BundleQtyMismatch_Error()
			throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(5.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "min_price", 2)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "10", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "5", "M", "8001", "8001", "2.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");
		utils.logit("POS discount lookup API call is successful");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Validated error message in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		utils.logit("POS batch redemption API call is successful");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate error message
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Redemption not possible since amount is 0"));
		utils.logPass("Validated error message in batch redemption API response");

	}

	@Test(description = "SQ-T7224 POS discount lookup API>Reusability-ON,Stacking-OFF>Only Base Items>Validate that error message gets displayed when LIF is having bundle of item with qty-2 but in input receipt item is present with qty-1 only", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7224_testPOSDiscountLookup_ReusabilityOnStackingOff_BaseOnly_BundleQtyMismatch_Error()
			throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(5.0).setStackDiscounting(false)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "min_price", 2)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "10", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "5", "M", "8001", "8001", "2.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");
		utils.logit("POS discount lookup API call is successful");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Validated error message in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		utils.logit("POS batch redemption API call is successful");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate error message
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Redemption not possible since amount is 0"));
		utils.logPass("Validated error message in batch redemption API response");

	}

	@Test(description = "SQ-T7225 POS discount lookup API>Reusability-OFF,Stacking-ON>Only Base Items>Validate that error message gets displayed when LIF is having bundle of item with qty-2 but in input receipt item is present with qty-1 only", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7225_testPOSDiscountLookup_ReusabilityOffStackingOn_BaseOnly_BundleQtyMismatch_Error()
			throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(5.0).setStackDiscounting(true)
				.setReuseQualifyingItems(false).addLineItemFilter(lif1, "min_price", 2)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "10", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "5", "M", "8001", "8001", "2.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");
		utils.logit("POS discount lookup API call is successful");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Validated error message in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		utils.logit("POS batch redemption API call is successful");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate error message
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Redemption not possible since amount is 0"));
		utils.logPass("Validated error message in batch redemption API response");

	}

	@Test(description = "SQ-T7226 POS discount lookup API>Reusability-OFF,Stacking-OFF>Only Base Items>Validate that error message gets displayed when LIF is having bundle of item with qty-2 but in input receipt item is present with qty-1 only", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7226_testPOSDiscountLookup_ReusabilityOffStackingOff_BaseOnly_BundleQtyMismatch_Error()
			throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(5.0).setStackDiscounting(false)
				.setReuseQualifyingItems(false).addLineItemFilter(lif1, "min_price", 2)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "10", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "5", "M", "8001", "8001", "2.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");
		utils.logit("POS discount lookup API call is successful");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Validated error message in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		utils.logit("POS batch redemption API call is successful");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate error message
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Redemption not possible since amount is 0"));
		utils.logPass("Validated error message in batch redemption API response");

	}

	@Test(description = "SQ-T7227 POS discount lookup API>Only Modifier>Multiple Offers>Validate the discount amount for target bundle price processing method having multiple rewards in discount basket when Item Qualifiers recycling ON; Stacked discounting ON", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7227_ModifierOnly_MultiOffers_TargetBundle_RecycleOnStackOn() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.MODIFIERS_ONLY, "101", "111,112", "", 2);
		String lif2 = createLIS(LISType.BASE_ONLY, "201,202", "211,212", "", 2);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "", 2).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(6.0)
				.setPercentageOfProcessedAmount(33).setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lif2, "", 1).addLineItemFilter(lif1, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "11", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Modifier1", new String[] { "Modifier1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Modifier2", new String[] { "Modifier2", "1", "5", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "4", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "5", "M", "202", "202", "3.0", "202" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "50", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
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
		double expectedDiscountAmountReward2 = 0.99;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Modifier1", "Modifier2", "Fries", "Modifier1", "Modifier2",
				"Fries", "Pizza");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Modifier1 DISCOUNT", "Modifier2 DISCOUNT", "Fries DISCOUNT",
				"Modifier1 DISCOUNT", "Modifier2 DISCOUNT", "Fries DISCOUNT", "Pizza DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(5.0, 5.0, 4.0, 3.57, 3.57, 2.86, 5.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-1.43, -1.43, -1.14, -0.08, -0.35, -0.06, -0.5);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7228 POS discount lookup API>Only Modifier>Multiple Offers>Validate the discount amount for target bundle price processing method having multiple rewards in discount basket when Item Qualifiers recycling ON; Stacked discounting ON", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7228_ModifiersOnly_MultiOffers_TargetBundle_RcyOnStackOn() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.MODIFIERS_ONLY, "101", "111,112", "", 2);
		String lif2 = createLIS(LISType.MODIFIERS_ONLY, "201,202", "211,212", "", 2);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setStackDiscounting(true)
				.setReuseQualifyingItems(true).addLineItemFilter(lif1, "", 2).addLineItemFilter(lif2, "", 1)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
		String redeemableExternalID1 = createRedeemable(qcExternalID1);
		String dbId1 = getRedeemableDbId(redeemableExternalID1);

		// ================= Offer-2 =================

		// Create QC
		String payload2 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(6.0)
				.setPercentageOfProcessedAmount(33).setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lif2, "", 1).addLineItemFilter(lif1, "", 1)
				.addItemQualifier("line_item_exists", lif1, null).build();
		Response response2 = pageObj.endpoints().createQC(payload2, dataSet.get("apiKey"));
		String qcExternalID2 = response2.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "11", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Modifier1", new String[] { "Modifier1", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Modifier2", new String[] { "Modifier2", "1", "5", "M", "112", "112", "1.2", "112" });
		addDetails.accept("Fries", new String[] { "Fries", "1", "4", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Modifier3", new String[] { "Modifier3", "1", "5", "M", "211", "211", "2.1", "211" });
		addDetails.accept("Modifier4", new String[] { "Modifier4", "1", "10", "M", "212", "212", "2.2", "212" });

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "50", externalUID, parentMap);
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
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
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
		double expectedDiscountAmountReward2 = 2.64;
		Double actualDiscountAmountReward2 = discountLookupJP.getDouble("selected_discounts[1].discount_amount");
		String discountIdLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_id");
		String discountTypeLookupReward2 = discountLookupJP.getString("selected_discounts[1].discount_type");

		// --- Assertions ---
		Assert.assertEquals(actualDiscountAmountReward2, expectedDiscountAmountReward2, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward2, rewardId2, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward2, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Modifier1", "Modifier2", "Modifier3", "Modifier1", "Modifier2",
				"Modifier3", "Modifier4");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Modifier1 DISCOUNT", "Modifier2 DISCOUNT", "Modifier3 DISCOUNT",
				"Modifier1 DISCOUNT", "Modifier2 DISCOUNT", "Modifier3 DISCOUNT", "Modifier4 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(5.0, 5.0, 5.0, 3.33, 3.33, 3.33, 10.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-1.67, -1.67, -1.67, -0.11, -0.6, -0.11, -1.82);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R item amounts: " + expectedRAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		double discountAmountBatchRedemption2 = batchJP.getDouble("success[1].discount_amount");

		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");
		Assert.assertEquals(discountAmountBatchRedemption2, expectedDiscountAmountReward2,
				"Incorrect discount_amount in batch redemption");

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

	@Test(description = "SQ-T7229 Only Base Item>Single Offer>Validate that no discount gets applied of bundle price is less than target bundle price", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7229_testBaseOnly_SingleOffer_NoDiscount_WhenBundlePriceLessThanTarget() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(30.0).setStackDiscounting(false)
				.setReuseQualifyingItems(false).addLineItemFilter(lif1, "", 1)
				.addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "20", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "5", "M", "8001", "8001", "2.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");
		utils.logit("POS discount lookup API call is successful");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");
		// validate error message
		Assert.assertTrue(discountLookupJP.getString("selected_discounts[0].message")
				.contains("Redemption not possible since amount is 0"));

		utils.logPass("Validated error message in POS discount lookup API response");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		utils.logit("POS batch redemption API call is successful");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		// Validate error message
		Assert.assertTrue(
				batchJP.getString("failures[0].message").contains("Redemption not possible since amount is 0"));
		utils.logPass("Validated error message in batch redemption API response");

	}

	@Test(description = "SQ-T7230 Validate that API is returning correctly qualified items in the response for Target bundle price(Advanced) processing function", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T7230_testPOSDLookup_TargetBundleAdv_QualifiedItems() throws Exception {

		// Enable decoupled redemption engine flag
		utils.updatePreferenceFlag(env, "businesses", "preferences", "id", dataSet.get("business_id"),
				"enable_decoupled_redemption_engine", "true");

		// ================= Offer-1 =================

		// Create LIS
		String lif1 = createLIS(LISType.BASE_ONLY, "22222211", null, null, 0);
		String lif2 = createLIS(LISType.BASE_ONLY, "8001", null, null, 0);

		// Create QC
		String payload1 = apipayloadObj.qualificationCriteriaBuilder().setName("POS_QC_" + Utilities.getTimestamp())
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(10.0).setMaxDiscountUnits(1)
				.setStackDiscounting(true).setReuseQualifyingItems(true).addLineItemFilter(lif1, "max_price", 1)
				.addLineItemFilter(lif2, "", 1).addItemQualifier("line_item_exists", lif2, null).build();

		Response response1 = pageObj.endpoints().createQC(payload1, dataSet.get("apiKey"));
		String qcExternalID1 = response1.jsonPath().getString("results[0].external_id");

		// Create Redeemable and Get DB ID
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
		addDetails.accept("Sandwich1",
				new String[] { "Sandwich1", "1", "11", "M", "22222211", "22222211", "1.0", "22222211" });
		addDetails.accept("Sandwich2",
				new String[] { "Sandwich2", "1", "12", "M", "22222211", "22222211", "2.0", "22222211" });
		addDetails.accept("Fries1", new String[] { "Fries1", "1", "6", "M", "8001", "8001", "3.0", "8001" });

		// Add reward-1 into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH add discount to basket is successful");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup status code is not 200 ");

		JsonPath discountLookupJP = discountLookupResponse.jsonPath();

		// Extract values
		Double actualDiscountAmountReward1 = discountLookupJP.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_id");
		String discountTypeLookupReward1 = discountLookupJP.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		double expectedDiscountAmountReward1 = 8.0;
		Assert.assertEquals(actualDiscountAmountReward1, expectedDiscountAmountReward1, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookupReward1, rewardId1, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookupReward1, "reward", "Incorrect discount_type");

		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich2", "Fries1");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich2 DISCOUNT", "Fries1 DISCOUNT");
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(12.0, 6.0);
		apiUtils.validateItems(discountLookupJP, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationKey"), userId, "50",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		JsonPath batchJP = batchRedemptionProcessResponse.jsonPath();

		double discountAmountBatchRedemption1 = batchJP.getDouble("success[0].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemption1, expectedDiscountAmountReward1,
				"Incorrect discount_amount in batch redemption");

		// --- Validate M-item names ---
		apiUtils.validateItems(batchJP, "success", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		apiUtils.validateItems(batchJP, "success", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		apiUtils.validateItems(batchJP, "success", "M", "amount", expectedMAmounts);
		utils.logit("Validated M item amounts: " + expectedMAmounts);

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