package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class StackingReusabilityDiscountDistributionTest {
	private static Logger logger = LogManager.getLogger(StackingReusabilityDiscountDistributionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String businessID, lisExternalId1, qcExternalId1, redeemableExternalID1, lisExternalId2, qcExternalId2,
			redeemableExternalID2, decoupledFlagStatusStr;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String redeemableId1;
	private String externalUID;
	private String redeemableName1;
	private String userID;
	public Boolean originalDecoupledRedemptionFlag;
	private OfferIngestionUtilities offerUtils;
	private boolean decoupledFlagStatus;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		businessID = dataSet.get("business_id");
		lisExternalId2 = null;
		qcExternalId2 = null;
		redeemableExternalID2 = null;
		originalDecoupledRedemptionFlag = null;
		offerUtils = new OfferIngestionUtilities(driver);
		utils = new Utilities();
		apipayloadObj = new ApiPayloadObj();
		utils.logit(sTCName + " ==> " + dataSet);
		// Get current value of Decoupled Redemption Engine flag
		decoupledFlagStatus = offerUtils.getDecoupledRedemptionEngineFlagStatus(env, businessID);
		decoupledFlagStatusStr = "_decoupledFlag_" + decoupledFlagStatus;
	}
	
	public boolean updateDecoupledRedemptionEngineFlag(boolean enableFlag, String env, String businessID)
			throws Exception {
		String businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id = $business_id;";
		businessesQuery = businessesQuery.replace("$business_id", businessID);
		// Get value of enable_decoupled_redemption_engine flag
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		List<String> decoupledRedemptionEngineValue = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		boolean currentValue = Boolean.parseBoolean(decoupledRedemptionEngineValue.get(0));
		// Store original value only once
		if (originalDecoupledRedemptionFlag == null) {
			originalDecoupledRedemptionFlag = currentValue;
			logger.info("Original 'enable_decoupled_redemption_engine' value stored as: "
					+ originalDecoupledRedemptionFlag);
			TestListeners.extentTest.get().info("Original 'enable_decoupled_redemption_engine' value stored as: "
					+ originalDecoupledRedemptionFlag);
		}

		// If already in desired state, do nothing
		if (currentValue == enableFlag) {
			logger.info("'enable_decoupled_redemption_engine' already set to " + enableFlag + " for businessID: "
					+ businessID);
			TestListeners.extentTest.get().info("'enable_decoupled_redemption_engine' already set to " + enableFlag + " for businessID: "
					+ businessID);
			return currentValue;
		}
		// Update DB
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, String.valueOf(enableFlag), "enable_decoupled_redemption_engine",
				businessID);
		logger.info("'enable_decoupled_redemption_engine' flag updated to " + enableFlag + " for businessID: "
				+ businessID);
		TestListeners.extentTest.get().info("'enable_decoupled_redemption_engine' flag updated to " + enableFlag + " for businessID: "
				+ businessID);
		return currentValue;
	}
	
	private void createLisQcAndRedeemable(String testId) throws Exception {
		// Create LIS via API
		String lisName1 = "Automation_LIS_" + testId + "_" + CreateDateTime.getTimeDateString();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage);
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create QC via API
		String qcName1 = "Automation_QC_" + testId + "_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "min_price", 1)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName1 + "' has been created successfully");

		// Create Redeemable via API
		redeemableName1 =  "Automation_Redeemable_" + testId + "_" + CreateDateTime.getTimeDateString();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create Redeemable API response status code mismatch");
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		utils.logit("Redeemable '" + redeemableName1 + "' created successfully");
		String getRedeemableIdQuery1 = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1);
		redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
	}

	private void signupUserAndAddRewardToBasket() throws Exception {
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("Mobile API2 Signup is successful for userID: " + userID);

		// Send redeemable to user
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId1, "1");
		// Get reward ID
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId1, 20);
		// Add the reward to basket
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable, token, dataSet.get("client"),
		dataSet.get("secret"), externalUID);
	}

	@Test(description = "SQ-T7140 Verify discount distribution when one discount amount is applied to two same-priced menu items.", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7140_discountCalculationWithSamePricedItems() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedDiscountAmt = dataSet.get("expectedDiscountAmt" + decoupledFlagStatusStr);
		String expectedQualifiedItemAmt = dataSet.get("expectedQualifiedItemAmt" + decoupledFlagStatusStr);
		String expectedQualifiedItemNegativeAmt = dataSet.get("expectedQualifiedItemNegativeAmt" + decoupledFlagStatusStr);

		// Create LIS via API
		String lisName1 = "Automation_LIS_SQ-T7140_" + CreateDateTime.getTimeDateString();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage);
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		Assert.assertFalse(lisExternalId1.isEmpty(), "LIS External ID is empty");
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create QC via API
		String qcName1 = "Automation_QC_SQ-T7140_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		Assert.assertFalse(qcExternalId1.isEmpty(), "QC External ID is empty");
		utils.logit("QC '" + qcName1 + "' has been created successfully");

		// Create Redeemable via API
		String redeemableName1 = "Automation_Redeemable_SQ-T7140_" + CreateDateTime.getTimeDateString();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty");
		utils.logit("Redeemable '" + redeemableName1 + "' created successfully");
		String getRedeemableIdQuery1 = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");

		// User SignUp
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("Mobile API2 Signup is successful for userID: " + userID);

		// Send redeemable to user
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId1, "1");
		// Get reward ID
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId1, 20);
		// Add the reward to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String rewardId = rewardIdListForRedeemable.get(0);
		Response addToDiscountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(addToDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String offerName = addToDiscountBasketResponse.jsonPath().get("discount_basket_items[0].discount_details.name")
				.toString();
		Assert.assertEquals(offerName, redeemableName1);
		utils.logPass("AUTH add discount to basket is successful. Reward ID " + rewardId + " added to basket");

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "999", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "D", "10", "999", "3", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "20", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "20", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify Discount Lookup API Response
		// TRUE = discount_amount{10.0}. FALSE = discount_amount{14.0}
		String lookup_discount_amount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(lookup_discount_amount, expectedDiscountAmt);
		// TRUE = {Pizza1}: item_qty{1.0}, amount{5.0}, item_id{101}, item_type{M}
		// FALSE = {Pizza1}: item_qty{1.0}, amount{7.0}, item_id{101}, item_type{M}
		String lookup_item_qty_1 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[0].item_qty").toString();
		String lookup_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String lookup_item_id_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		String lookup_item_type_1 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[0].item_type").toString();
		Assert.assertEquals(lookup_item_qty_1, "1.0");
		Assert.assertEquals(lookup_amount_1, expectedQualifiedItemAmt);
		Assert.assertEquals(lookup_item_id_1, "101");
		Assert.assertEquals(lookup_item_type_1, "M");
		// TRUE = {Pizza2}: item_qty{1.0}, amount{5.0}, item_id{101}, item_type{M}
		// FALSE = {Pizza2}: item_qty{1.0}, amount{7.0}, item_id{101}, item_type{M}
		String lookup_item_qty_2 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[1].item_qty").toString();
		String lookup_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String lookup_item_id_2 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		String lookup_item_type_2 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[1].item_type").toString();
		Assert.assertEquals(lookup_item_qty_2, "1.0");
		Assert.assertEquals(lookup_amount_2, expectedQualifiedItemAmt);
		Assert.assertEquals(lookup_item_id_2, "101");
		Assert.assertEquals(lookup_item_type_2, "M");
		// TRUE = {Pizza1 DISCOUNT}: item_qty{1.0}, amount{-5.0}, item_id{101}, item_type{R}
		// FALSE = {Pizza1 DISCOUNT}: item_qty{1.0}, amount{-7.0}, item_id{101}, item_type{R}
		String lookup_item_qty_3 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[2].item_qty").toString();
		String lookup_amount_3 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[2].amount")
				.toString();
		String lookup_item_id_3 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[2].item_id")
				.toString();
		String lookup_item_type_3 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[2].item_type").toString();
		Assert.assertEquals(lookup_item_qty_3, "1.0");
		Assert.assertEquals(lookup_amount_3, expectedQualifiedItemNegativeAmt);
		Assert.assertEquals(lookup_item_id_3, "101");
		Assert.assertEquals(lookup_item_type_3, "R");
		// TRUE = {Pizza2 DISCOUNT}: item_qty{1.0}, amount{-5.0}, item_id{101}, item_type{R}
		// FALSE = {Pizza2 DISCOUNT}: item_qty{1.0}, amount{-7.0}, item_id{101}, item_type{R}
		String lookup_item_qty_4 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[3].item_qty").toString();
		String lookup_amount_4 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[3].amount")
				.toString();
		String lookup_item_id_4 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[3].item_id")
				.toString();
		String lookup_item_type_4 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[3].item_type").toString();
		Assert.assertEquals(lookup_item_qty_4, "1.0");
		Assert.assertEquals(lookup_amount_4, expectedQualifiedItemNegativeAmt);
		Assert.assertEquals(lookup_item_id_4, "101");
		Assert.assertEquals(lookup_item_type_4, "R");
		utils.logPass("Discount Lookup API Response is verified");

		// Verify Batch Redemption API Response
		String redemption_discount_amount = authBatchRedemptionResponse.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		
		String redemption_item_qty_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty")
				.toString();
		String redemption_amount_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount")
				.toString();
		String redemption_item_id_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id")
				.toString();
		String redemption_item_type_1 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[0].item_type").toString();
		Assert.assertEquals(redemption_item_qty_1, lookup_item_qty_1);
		Assert.assertEquals(redemption_amount_1, lookup_amount_1);
		Assert.assertEquals(redemption_item_id_1, lookup_item_id_1);
		Assert.assertEquals(redemption_item_type_1, lookup_item_type_1);
		
		String redemption_item_qty_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty")
				.toString();
		String redemption_amount_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount")
				.toString();
		String redemption_item_id_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id")
				.toString();
		String redemption_item_type_2 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[1].item_type").toString();
		Assert.assertEquals(redemption_item_qty_2, lookup_item_qty_2);
		Assert.assertEquals(redemption_amount_2, lookup_amount_2);
		Assert.assertEquals(redemption_item_id_2, lookup_item_id_2);
		Assert.assertEquals(redemption_item_type_2, lookup_item_type_2);
		
		String redemption_item_qty_3 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[2].item_qty")
				.toString();
		String redemption_amount_3 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[2].amount")
				.toString();
		String redemption_item_id_3 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[2].item_id")
				.toString();
		String redemption_item_type_3 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[2].item_type").toString();
		Assert.assertEquals(redemption_item_qty_3, lookup_item_qty_3);
		Assert.assertEquals(redemption_amount_3, lookup_amount_3);
		Assert.assertEquals(redemption_item_id_3, lookup_item_id_3);
		Assert.assertEquals(redemption_item_type_3, lookup_item_type_3);
		
		String redemption_item_qty_4 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[3].item_qty")
				.toString();
		String redemption_amount_4 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[3].amount")
				.toString();
		String redemption_item_id_4 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[3].item_id")
				.toString();
		String redemption_item_type_4 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[3].item_type").toString();
		Assert.assertEquals(redemption_item_qty_4, lookup_item_qty_4);
		Assert.assertEquals(redemption_amount_4, lookup_amount_4);
		Assert.assertEquals(redemption_item_id_4, lookup_item_id_4);
		Assert.assertEquals(redemption_item_type_4, lookup_item_type_4);
		utils.logPass("Batch Redemption API Response is verified");
	}

	@Test(description = "SQ-T7141: Verify discount distribution when one discount amount is applied to two menu items with different prices.", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7141_discountCalculationWithDifferentPricedItems() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedDiscountAmt = dataSet.get("expectedDiscountAmt" + decoupledFlagStatusStr);
		String expectedDiscountNegativeAmt = dataSet.get("expectedDiscountNegativeAmt" + decoupledFlagStatusStr);

		// Create LIS via API
		String lisName1 = "Automation_LIS_SQ-T7141_" + CreateDateTime.getTimeDateString();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage);
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		Assert.assertFalse(lisExternalId1.isEmpty(), "LIS External ID is empty");
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create QC via API
		String qcName1 = "Automation_QC_SQ-T7141_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "max_price", 1)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		Assert.assertFalse(qcExternalId1.isEmpty(), "QC External ID is empty");
		utils.logit("QC '" + qcName1 + "' has been created successfully");

		// Create Redeemable via API
		String redeemableName1 = "Automation_Redeemable_SQ-T7141_" + CreateDateTime.getTimeDateString();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty");
		utils.logit("Redeemable '" + redeemableName1 + "' created successfully");
		String getRedeemableIdQuery1 = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		Assert.assertNotNull(redeemableId1, "Redeemable ID is null");

		// User SignUp
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("Mobile API2 Signup is successful for userID: " + userID);

		// Send redeemable to user
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId1, "1");
		// Get reward ID
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId1, 20);
		// Add the reward to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String rewardId = rewardIdListForRedeemable.get(0);
		Response addToDiscountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(addToDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String offerName = addToDiscountBasketResponse.jsonPath().get("discount_basket_items[0].discount_details.name")
				.toString();
		Assert.assertEquals(offerName, redeemableName1);
		utils.logPass("AUTH add discount to basket is successful. Reward ID " + rewardId + " added to basket");

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "5", "M", "10", "999", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "D", "10", "999", "3", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "20", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "20", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Perform assertions based on the Decoupled Redemption Engine flag status
		// Verify Discount Lookup API Response.
		// TRUE = discount_amount ={4.67}. FALSE = discount_amount{7.0}
		String lookup_discount_amount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(lookup_discount_amount, expectedDiscountAmt);
		// TRUE = {Pizza1}: item_qty{1.0}, amount{4.67}, item_id{101}, item_type{M}
		// FALSE = {Pizza1}: item_qty{1.0}, amount{7.0}, item_id{101}, item_type{M}
		String lookup_item_qty_1 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[0].item_qty").toString();
		String lookup_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String lookup_item_id_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		String lookup_item_type_1 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[0].item_type").toString();
		Assert.assertEquals(lookup_item_qty_1, "1.0");
		Assert.assertEquals(lookup_amount_1, expectedDiscountAmt);
		Assert.assertEquals(lookup_item_id_1, "101");
		Assert.assertEquals(lookup_item_type_1, "M");
		// TRUE = {Pizza1 DISCOUNT}: item_qty{1.0}, amount{-4.67}, item_id{101}, item_type{R}
		// FALSE = {Pizza1 DISCOUNT}: item_qty{1.0}, amount{-7.0}, item_id{101}, item_type{R}
		String lookup_item_qty_2 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[1].item_qty").toString();
		String lookup_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String lookup_item_id_2 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		String lookup_item_type_2 = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].qualified_items[1].item_type").toString();
		Assert.assertEquals(lookup_item_qty_2, "1.0");
		Assert.assertEquals(lookup_amount_2, expectedDiscountNegativeAmt);
		Assert.assertEquals(lookup_item_id_2, "101");
		Assert.assertEquals(lookup_item_type_2, "R");
		utils.logPass("Discount Lookup API Response is verified");

		// Verify Batch Redemption API Response
		String redemption_discount_amount = authBatchRedemptionResponse.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		String redemption_item_qty_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty")
				.toString();
		String redemption_amount_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount")
				.toString();
		String redemption_item_id_1 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id")
				.toString();
		String redemption_item_type_1 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[0].item_type").toString();
		Assert.assertEquals(redemption_item_qty_1, lookup_item_qty_1);
		Assert.assertEquals(redemption_amount_1, lookup_amount_1);
		Assert.assertEquals(redemption_item_id_1, lookup_item_id_1);
		Assert.assertEquals(redemption_item_type_1, lookup_item_type_1);
		String redemption_item_qty_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty")
				.toString();
		String redemption_amount_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount")
				.toString();
		String redemption_item_id_2 = authBatchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id")
				.toString();
		String redemption_item_type_2 = authBatchRedemptionResponse.jsonPath()
				.get("success[0].qualified_items[1].item_type").toString();
		Assert.assertEquals(redemption_item_qty_2, lookup_item_qty_2);
		Assert.assertEquals(redemption_amount_2, lookup_amount_2);
		Assert.assertEquals(redemption_item_id_2, lookup_item_id_2);
		Assert.assertEquals(redemption_item_type_2, lookup_item_type_2);
		utils.logPass("Batch Redemption API Response is verified");
	}

	@Test(description = "SQ-T7142: Verify discount calculation when multiple offers are applied.", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7142_discountCalculationWhenMultipleOffersApplied() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1DiscountAmt = dataSet.get("expectedOffer1DiscountAmt" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem1 = dataSet.get("expectedOffer1QualifiedItem1" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem2 = dataSet.get("expectedOffer1QualifiedItem2" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem3 = dataSet.get("expectedOffer1QualifiedItem3" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem4 = dataSet.get("expectedOffer1QualifiedItem4" + decoupledFlagStatusStr);
		String expectedOffer2DiscountAmt = dataSet.get("expectedOffer2DiscountAmt" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem1 = dataSet.get("expectedOffer2QualifiedItem1" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2 = dataSet.get("expectedOffer2QualifiedItem2" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem3 = dataSet.get("expectedOffer2QualifiedItem3" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem4 = dataSet.get("expectedOffer2QualifiedItem4" + decoupledFlagStatusStr);

		// Create LIS {item_id 2001} via API
		String lisName1 = "Automation_LIS_SQ-T7142_2001_" + CreateDateTime.getTimeDateString();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "2001")
				.build();
		Response createLisResponse1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean actualSuccessMessage1 = createLisResponse1.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage1);
		lisExternalId1 = createLisResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		Assert.assertFalse(lisExternalId1.isEmpty(), "LIS External ID is empty");
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create LIS {item_id 101} via API
		String lisName2 = "Automation_LIS_SQ-T7142_101_" + CreateDateTime.getTimeDateString();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean actualSuccessMessage2 = createLisResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2);
		lisExternalId2 = createLisResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId2);
		Assert.assertFalse(lisExternalId2.isEmpty(), "LIS External ID is empty");
		utils.logit("LIS '" + lisName2 + "' has been created successfully");

		// Create QC {Stacking ON, Reusability ON} via API
		String qcName1 = "Automation_QC_SQ-T7142_StackOnReuseOn_" + CreateDateTime.getTimeDateString();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcName1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts").setPercentageOfProcessedAmount(50)
				.addLineItemFilter(lisExternalId2, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean qcSuccessMessage1 = createQcResponse1.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage1);
		qcExternalId1 = createQcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		Assert.assertFalse(qcExternalId1.isEmpty(), "QC External ID is empty");
		utils.logit("QC '" + qcName1 + "' has been created successfully");

		// Create Redeemable attached to above QC via API
		String redeemableName1 = qcName1 + "_Redeemable";
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		Assert.assertFalse(redeemableExternalID1.isEmpty(), "Redeemable External ID is empty");
		utils.logit("Redeemable '" + redeemableName1 + "' created successfully");
		String getRedeemableIdQuery1 = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		Assert.assertNotNull(redeemableId1, "Redeemable ID is null");

		// Create QC {Stacking ON, Reusability OFF} via API
		String qcName2 = "Automation_QC_SQ-T7142_StackOnReuseOff_" + CreateDateTime.getTimeDateString();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcName2).setStackDiscounting(true)
				.setReuseQualifyingItems(false).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId2, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean qcSuccessMessage2 = createQcResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage2);
		qcExternalId2 = createQcResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId2);
		Assert.assertFalse(qcExternalId2.isEmpty(), "QC External ID is empty");
		utils.logit("QC '" + qcName2 + "' has been created successfully");

		// Create Redeemable attached to above QC via API
		String redeemableName2 = qcName2 + "_Redeemable";
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID2);
		Assert.assertFalse(redeemableExternalID2.isEmpty(), "Redeemable External ID is empty");
		utils.logit("Redeemable '" + redeemableName2 + "' created successfully");
		String getRedeemableIdQuery2 = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		Assert.assertNotNull(redeemableId2, "Redeemable ID is null");

		// User SignUp
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("Mobile API2 Signup is successful for userID: " + userID);

		// Send both redeemables to user
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId1, "1");
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId2, "1");
		// Get reward IDs
		List<String> rewardIdListForRedeemable1 = pageObj.redeemablesPage().getRewardIdList(token,
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable2 = pageObj.redeemablesPage().getRewardIdList(token,
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add the rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable1, token, dataSet.get("client"),
				dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable2, token, dataSet.get("client"),
				dataSet.get("secret"), externalUID);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "6", "M", "10", "999", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "2", "4", "M", "10", "999", "3", "2001" });
		addDetails.accept("Pizza3", new String[] { "Pizza3", "1", "6", "D", "10", "999", "4", "101" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "25", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "25", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_basket1_offer_name = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_details.name").toString();
		String lookup_basket2_offer_name = discountLookupResponse.jsonPath()
				.get("selected_discounts[1].discount_details.name").toString();
		String redemption_basket1_offer_name = authBatchRedemptionResponse.jsonPath()
				.get("success[0].discount_details.name").toString();
		String redemption_basket2_offer_name = authBatchRedemptionResponse.jsonPath()
				.get("success[1].discount_details.name").toString();
		Assert.assertEquals(lookup_basket1_offer_name, redeemableName1);
		Assert.assertEquals(lookup_basket2_offer_name, redeemableName2);
		Assert.assertEquals(redemption_basket1_offer_name, lookup_basket1_offer_name);
		Assert.assertEquals(redemption_basket2_offer_name, lookup_basket2_offer_name);
		// Extract the qualified items and discount amounts from API responses
		String lookup_basket1_discount_amount = discountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		String lookup_basket1_qualified_item1 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 0);
		String lookup_basket1_qualified_item2 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 1);
		String lookup_basket1_qualified_item3 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 2);
		String lookup_basket1_qualified_item4 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 3);
		String lookup_basket2_discount_amount = discountLookupResponse.jsonPath()
				.get("selected_discounts[1].discount_amount").toString();
		String lookup_basket2_qualified_item1 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 1, 0);
		String lookup_basket2_qualified_item2 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 1, 1);
		String lookup_basket2_qualified_item3 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 1, 2);
		String lookup_basket2_qualified_item4 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 1, 3);
		String redemption_basket1_discount_amount = authBatchRedemptionResponse.jsonPath()
				.get("success[0].discount_amount").toString();
		String redemption_basket1_qualified_item1 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 0);
		String redemption_basket1_qualified_item2 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 1);
		String redemption_basket1_qualified_item3 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 2);
		String redemption_basket1_qualified_item4 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 3);
		String redemption_basket2_discount_amount = authBatchRedemptionResponse.jsonPath()
				.get("success[1].discount_amount").toString();
		String redemption_basket2_qualified_item1 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 1, 0);
		String redemption_basket2_qualified_item2 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 1, 1);
		String redemption_basket2_qualified_item3 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 1, 2);
		String redemption_basket2_qualified_item4 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 1, 3);

		// Perform assertions based on the Decoupled Redemption Engine flag status
		// Verify Discount Lookup API Response for both offers
		Assert.assertEquals(lookup_basket1_discount_amount, expectedOffer1DiscountAmt);
		Assert.assertEquals(lookup_basket1_qualified_item1, expectedOffer1QualifiedItem1);
		Assert.assertEquals(lookup_basket1_qualified_item2, expectedOffer1QualifiedItem2);
		Assert.assertEquals(lookup_basket1_qualified_item3, expectedOffer1QualifiedItem3);
		Assert.assertEquals(lookup_basket1_qualified_item4, expectedOffer1QualifiedItem4);
		Assert.assertEquals(lookup_basket2_discount_amount, expectedOffer2DiscountAmt);
		Assert.assertEquals(lookup_basket2_qualified_item1, expectedOffer2QualifiedItem1);
		Assert.assertEquals(lookup_basket2_qualified_item2, expectedOffer2QualifiedItem2);
		Assert.assertEquals(lookup_basket2_qualified_item3, expectedOffer2QualifiedItem3);
		Assert.assertEquals(lookup_basket2_qualified_item4, expectedOffer2QualifiedItem4);
		// Verify Batch Redemption API Response for both offers
		Assert.assertEquals(redemption_basket1_discount_amount, lookup_basket1_discount_amount);
		Assert.assertEquals(redemption_basket1_qualified_item1, lookup_basket1_qualified_item1);
		Assert.assertEquals(redemption_basket1_qualified_item2, lookup_basket1_qualified_item2);
		Assert.assertEquals(redemption_basket1_qualified_item3, lookup_basket1_qualified_item3);
		Assert.assertEquals(redemption_basket1_qualified_item4, lookup_basket1_qualified_item4);
		Assert.assertEquals(redemption_basket2_discount_amount, lookup_basket2_discount_amount);
		Assert.assertEquals(redemption_basket2_qualified_item1, lookup_basket2_qualified_item1);
		Assert.assertEquals(redemption_basket2_qualified_item2, lookup_basket2_qualified_item2);
		Assert.assertEquals(redemption_basket2_qualified_item3, lookup_basket2_qualified_item3);
		Assert.assertEquals(redemption_basket2_qualified_item4, lookup_basket2_qualified_item4);
		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}

	@Test(description = "SQ-T7139 Verify discount calculation when discount amount is negative.", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7139_discountCalculationWhenDiscountAmountNegative() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedDiscountAmt = dataSet.get("expectedDiscountAmt" + decoupledFlagStatusStr);
		String expectedQualifiedItem1 = dataSet.get("expectedQualifiedItem1" + decoupledFlagStatusStr);
		String expectedQualifiedItem2 = dataSet.get("expectedQualifiedItem2" + decoupledFlagStatusStr);
		// Create LIS, QC and Redeemable
		createLisQcAndRedeemable("SQ-T7139");
		// User SignUp > Send redeemable to user > Add reward to basket
		signupUserAndAddRewardToBasket();

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "-4", "D", "10", "999", "2", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "15", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_offer_name = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.name")
				.toString();
		String redemption_offer_name = authBatchRedemptionResponse.jsonPath().get("success[0].discount_details.name")
				.toString();
		Assert.assertEquals(lookup_offer_name, redeemableName1);
		Assert.assertEquals(redemption_offer_name, lookup_offer_name);
		// Extract the qualified items and discount amounts from API responses
		String lookup_discount_amount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		String lookup_qualified_item1 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 0);
		String lookup_qualified_item2 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 1);
		String redemption_discount_amount = authBatchRedemptionResponse.jsonPath().get("success[0].discount_amount")
				.toString();
		String redemption_qualified_item1 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 0);
		String redemption_qualified_item2 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 1);

		// Perform assertions based on the Decoupled Redemption Engine flag status
		// Verify Discount Lookup API Response
		Assert.assertEquals(lookup_discount_amount, expectedDiscountAmt);
		Assert.assertEquals(lookup_qualified_item1, expectedQualifiedItem1);
		Assert.assertEquals(lookup_qualified_item2, expectedQualifiedItem2);
		// Verify Batch Redemption API Response
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		Assert.assertEquals(redemption_qualified_item1, lookup_qualified_item1);
		Assert.assertEquals(redemption_qualified_item2, lookup_qualified_item2);
		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}

	@Test(description = "SQ-T7135 Verify discount when discount item amount and item amount are the same.", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7135_discountCalculationWhenDiscountItemAmountAndItemAmountAreTheSame() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		updateDecoupledRedemptionEngineFlag(true,env, businessID);
	    // Create Line Item Selector (LIS), Qualification Criteria (QC), and Redeemable for this test
		createLisQcAndRedeemable("SQ-T7135");
		// Sign up a new user, send the redeemable, and add the reward to the basket
		signupUserAndAddRewardToBasket();

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "7", "D", "10", "999", "2", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "15", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_offer_name = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.name")
				.toString();
		utils.logit("Lookup offer names verified: " + lookup_offer_name);
		String redemption_offer_name = authBatchRedemptionResponse.jsonPath().get("failures[0].discount_details.name")
				.toString();
		utils.logit("Redemption offer name : " + redemption_offer_name);
		String lookupErrorMessage = discountLookupResponse.jsonPath().getString("selected_discounts[0].message[0]");
		utils.logit("LookUp Error Message : " + lookupErrorMessage);
		
		String redemptionErrorMessage = authBatchRedemptionResponse.jsonPath().getString("failures[0].message[0]");
		utils.logit("Redemption Error Message : " + redemptionErrorMessage);
		
		Assert.assertEquals(lookup_offer_name, redeemableName1);
		Assert.assertEquals(redemption_offer_name, lookup_offer_name);
		Assert.assertEquals(lookupErrorMessage, dataSet.get("error_message"));
		Assert.assertEquals(redemptionErrorMessage, dataSet.get("error_message"));
		
		//Extract discount amounts for LookUp
		Object discountAmountObj =discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount");
		String lookup_discount_amount = discountAmountObj == null ? null : discountAmountObj.toString();
		 
		// Extract qualified_items lists and verify they are null or empty for Lookup
		List<Map<String, Object>> qualifiedItemsLookUp = discountLookupResponse.jsonPath().getList("selected_discounts[0].qualified_items");
		Assert.assertTrue(qualifiedItemsLookUp == null || qualifiedItemsLookUp.isEmpty(),"Expected qualified_items to be null or empty, but found: " + qualifiedItemsLookUp);
		
		//Extract discount amounts for Redemption
		Object redemptionAmountObj =authBatchRedemptionResponse.jsonPath().get("failures[0].discount_amount");
		String redemption_discount_amount = redemptionAmountObj == null ? null : redemptionAmountObj.toString();
		
		// Extract qualified_items lists and verify they are null or empty for Redemption
		List<Map<String, Object>> qualifiedItemsRedemptions = authBatchRedemptionResponse.jsonPath().getList("failures[0].qualified_items");
		Assert.assertTrue(qualifiedItemsRedemptions == null || qualifiedItemsRedemptions.isEmpty(),"Expected qualified_items to be null or empty, but found: " + qualifiedItemsRedemptions);
				
		//Verify discount amounts and qualified items consistency
		Assert.assertEquals(lookup_discount_amount, dataSet.get("lookup_discount_amount"));
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		Assert.assertEquals(qualifiedItemsRedemptions, qualifiedItemsLookUp);
		Assert.assertEquals(lookupErrorMessage, redemptionErrorMessage);
		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}
	
	@Test(description = "SQ-T7137 Verify discount when discount amount is greater than the menu item price.", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7137_discountCalculationWhenDiscountAmountIsGreaterThanTheMenuItemPrice() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		updateDecoupledRedemptionEngineFlag(true, env, businessID);
		// Create Line Item Selector (LIS), Qualification Criteria (QC), and Redeemable for this test
		createLisQcAndRedeemable("SQ-T7137");
		// Sign up a new user, send the redeemable, and add the reward to the basket
		signupUserAndAddRewardToBasket();

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "7.01", "D", "10", "999", "2", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "15", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_offer_name = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.name")
				.toString();
		utils.logit("Lookup offer names verified: " + lookup_offer_name);
		String redemption_offer_name = authBatchRedemptionResponse.jsonPath().get("failures[0].discount_details.name")
				.toString();
		utils.logit("Redemption offer name : " + redemption_offer_name);
		String lookupErrorMessage = discountLookupResponse.jsonPath().getString("selected_discounts[0].message[0]");
		utils.logit("LookUp Error Message : " + lookupErrorMessage);
		
		String redemptionErrorMessage = authBatchRedemptionResponse.jsonPath().getString("failures[0].message[0]");
		utils.logit("Redemption Error Message : " + redemptionErrorMessage);
		
		Assert.assertEquals(lookup_offer_name, redeemableName1);
		Assert.assertEquals(redemption_offer_name, lookup_offer_name);
		Assert.assertEquals(lookupErrorMessage, dataSet.get("error_message"));
		Assert.assertEquals(redemptionErrorMessage, dataSet.get("error_message"));
		
		//Extract discount amounts for LookUp
		Object discountAmountObj =discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount");
		String lookup_discount_amount = discountAmountObj == null ? null : discountAmountObj.toString();
		 
		// Extract qualified_items lists and verify they are null or empty for Lookup
		List<Map<String, Object>> qualifiedItemsLookUp = discountLookupResponse.jsonPath().getList("selected_discounts[0].qualified_items");
		Assert.assertTrue(qualifiedItemsLookUp == null || qualifiedItemsLookUp.isEmpty(),"Expected qualified_items to be null or empty, but found: " + qualifiedItemsLookUp);
		
		//Extract discount amounts for Redemption
		Object redemptionAmountObj =authBatchRedemptionResponse.jsonPath().get("failures[0].discount_amount");
		String redemption_discount_amount = redemptionAmountObj == null ? null : redemptionAmountObj.toString();
		
		// Extract qualified_items lists and verify they are null or empty for Redemption
		List<Map<String, Object>> qualifiedItemsRedemptions = authBatchRedemptionResponse.jsonPath().getList("failures[0].qualified_items");
		Assert.assertTrue(qualifiedItemsRedemptions == null || qualifiedItemsRedemptions.isEmpty(),"Expected qualified_items to be null or empty, but found: " + qualifiedItemsRedemptions);
				
		//Verify discount amounts and qualified items consistency
		Assert.assertEquals(lookup_discount_amount, dataSet.get("lookup_discount_amount"));
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		Assert.assertEquals(qualifiedItemsRedemptions, qualifiedItemsLookUp);
		Assert.assertEquals(lookupErrorMessage, redemptionErrorMessage);
		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}
	
	@Test(description = "SQ-T7136 Verify discount when discount amount is less than the menu item price.", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7136_discountCalculationWhenDiscountLessThanMenuItemPrice() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create Line Item Selector (LIS), Qualification Criteria (QC), and Redeemable for this test
		createLisQcAndRedeemable("SQ-T7136");
		// Sign up a new user, send the redeemable, and add the reward to the basket
		signupUserAndAddRewardToBasket();

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "D", "10", "999", "2", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "15", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_offer_name = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.name")
				.toString();
		String redemption_offer_name = authBatchRedemptionResponse.jsonPath().get("success[0].discount_details.name")
				.toString();
		Assert.assertEquals(lookup_offer_name, redeemableName1);
		Assert.assertEquals(redemption_offer_name, lookup_offer_name);
		// Extract the qualified items and discount amounts from API responses
		String lookup_discount_amount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		String lookup_qualified_item1 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 0);
		String lookup_qualified_item2 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 1);
		String redemption_discount_amount = authBatchRedemptionResponse.jsonPath().get("success[0].discount_amount")
				.toString();
		String redemption_qualified_item1 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 0);
		String redemption_qualified_item2 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 1);

		// Verify Discount Lookup API Response
		Assert.assertEquals(lookup_discount_amount, dataSet.get("lookup_discount_amount"));
		Assert.assertEquals(lookup_qualified_item1, dataSet.get("lookup_qualified_item1"));
		Assert.assertEquals(lookup_qualified_item2, dataSet.get("lookup_qualified_item2"));
		// Verify Batch Redemption API Response
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		Assert.assertEquals(redemption_qualified_item1, lookup_qualified_item1);
		Assert.assertEquals(redemption_qualified_item2, lookup_qualified_item2);

		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}
	
	@Test(description = "SQ-T7138 Verify discount when discount amount is zero and menu item has a value.", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7138_discountCalculationWhenDiscountAmtIsZeroAndMenuItemHasValue() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create Line Item Selector (LIS), Qualification Criteria (QC), and Redeemable for this test
		createLisQcAndRedeemable("SQ-T7138");
		// Sign up a new user, send the redeemable, and add the reward to the basket
		signupUserAndAddRewardToBasket();

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "7", "M", "10", "999", "1", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "0", "D", "10", "999", "2", "5001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userID, "15", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup API response status code mismatch");
		// Hit Process Batch Redemption API
		Response authBatchRedemptionResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "15", parentMap);
		Assert.assertEquals(authBatchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Batch Redemption API response status code mismatch");

		// Verify the applied offer name in API responses
		String lookup_offer_name = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.name")
				.toString();
		String redemption_offer_name = authBatchRedemptionResponse.jsonPath().get("success[0].discount_details.name")
				.toString();
		Assert.assertEquals(lookup_offer_name, redeemableName1);
		Assert.assertEquals(redemption_offer_name, lookup_offer_name);
		// Extract the qualified items and discount amounts from API responses
		String lookup_discount_amount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		String lookup_qualified_item1 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 0);
		String lookup_qualified_item2 = ApiUtils.getQualifiedItemJson(discountLookupResponse, 0, 1);
		String redemption_discount_amount = authBatchRedemptionResponse.jsonPath().get("success[0].discount_amount")
				.toString();
		String redemption_qualified_item1 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 0);
		String redemption_qualified_item2 = ApiUtils.getQualifiedItemJson(authBatchRedemptionResponse, 0, 1);

		// Verify Discount Lookup API Response
		Assert.assertEquals(lookup_discount_amount, dataSet.get("lookup_discount_amount"));
		Assert.assertEquals(lookup_qualified_item1, dataSet.get("lookup_qualified_item1"));
		Assert.assertEquals(lookup_qualified_item2, dataSet.get("lookup_qualified_item2"));
		// Verify Batch Redemption API Response
		Assert.assertEquals(redemption_discount_amount, lookup_discount_amount);
		Assert.assertEquals(redemption_qualified_item1, lookup_qualified_item1);
		Assert.assertEquals(redemption_qualified_item2, lookup_qualified_item2);

		utils.logPass("Discount Lookup and Batch Redemption API Responses are verified.");
	}
	
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalId1, qcExternalId1, redeemableExternalID1);
		utils.deleteLISQCRedeemable(env, lisExternalId2, qcExternalId2, redeemableExternalID2);
		// Restore original flag value
	    if (originalDecoupledRedemptionFlag != null) {
	        utils.logit("Restoring 'enable_decoupled_redemption_engine' to original value: " + originalDecoupledRedemptionFlag);
	        updateDecoupledRedemptionEngineFlag(originalDecoupledRedemptionFlag, env, businessID);
	    }
		pageObj.utils().clearDataSet(dataSet);
	}

}