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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class StackingReusabilityDiscountCalculationTest {
	private static Logger logger = LogManager.getLogger(StackingReusabilityDiscountCalculationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String businessID;
	private Utilities utils;
	private OfferIngestionUtilities offerUtils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalId1, lisExternalId2, qcExternalId1, qcExternalId2, redeemableExternalId1,
			redeemableExternalId2, decoupledFlagStatusStr;
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
		logger.info(sTCName + " ==>" + dataSet);
		businessID = dataSet.get("business_id");
		lisExternalId2 = null;
		qcExternalId2 = null;
		redeemableExternalId2 = null;
		utils = new Utilities();
		offerUtils = new OfferIngestionUtilities(driver);
		apipayloadObj = new ApiPayloadObj();
		// Get current value of Decoupled Redemption Engine flag
		decoupledFlagStatus = offerUtils.getDecoupledRedemptionEngineFlagStatus(env, dataSet.get("business_id"));
		decoupledFlagStatusStr = "_decoupledFlag_" + decoupledFlagStatus;
	}

	@Test(description = "SQ-T6489: [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having different LIS and LIQ config.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6489_discountCalculationWithVariedLISAndLIQConfig() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedDiscountAmt1 = dataSet.get("expectedDiscountAmt1" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem1Qty = dataSet.get("expectedOffer1QualifiedItem1Qty" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem1Amt = dataSet.get("expectedOffer1QualifiedItem1Amt" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6489_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6489_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2").setRoundingRule("round")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_quantity_greater_than_or_equal_to", lisExternalId1, 2.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6489_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6489_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6489_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2").setEffectiveLocationsID("408901")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("net_amount_greater_than_or_equal_to", lisExternalId2, 10.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6489_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "5", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "20", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {2.5} with flag true
		// and {3} with flag false
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, expectedDiscountAmt1, "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "5.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} with flag true and
		// item_qty as {2.0}, amount as {-3.0} with flag false. item_id remains {10001}
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem1Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, expectedOffer1QualifiedItem1Amt, "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> Discount amount is {10.0}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "10.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} with flag true and
		// item_qty as {2.0}, amount as {-10.0} with flag false. item_id remains {20001}
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-10.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = either {2.5 + 10.0 = 12.5} or {3.0 + 10.0 = 13.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_2);
		Assert.assertTrue(totalDiscountAmount == 12.5 || totalDiscountAmount == 13.0,
				"Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {2.5} with flag true
		// and {3} with flag false
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, expectedDiscountAmt1, "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "5.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} with flag true and
		// item_qty as {2.0}, amount as {-3.0} with flag false. item_id remains {10001}
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem1Qty, "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_12, expectedOffer1QualifiedItem1Amt, "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 2nd offer -> Discount amount is {10.0}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "10.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_21, "20.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the second offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} with flag true and
		// item_qty as {2.0}, amount as {-10.0} with flag false. item_id remains {20001}
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_22, "-10.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6500: [Stacking OFF, Reusability OFF] Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing percent and amount cap")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6500_discountCalculationWithLISProcessingPercentAmountCap() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6500_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6500_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2").setPercentageOfProcessedAmount(20)
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0)
				.build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6500_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6500_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6500_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2").setAmountCap(1)
				.addLineItemFilter(lisExternalId2, "", 0).addItemQualifier("line_item_exists", lisExternalId2, 10.0, 0)
				.build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6500_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "20", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "30", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {2.0}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "2.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> Discount amount is {1.0}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "1.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "30.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-1.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-1.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {2.0 + 1.0 = 3.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_2);
		Assert.assertEquals(totalDiscountAmount, 3.0, "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {2.0}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "2.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "20.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 2nd offer -> Discount amount is {1.0}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "1.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_21, "30.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the second offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-1.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false.
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-1.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6503: [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having all receipt qualifiers and different LIQ.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6503_discountCalculationWithReceiptQualifiersLISDifferentLIQ() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6503_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6503_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalId1, 5.0, 1)
				.addReceiptQualifier("total_amount", ">=", "10").addReceiptQualifier("subtotal_amount", ">=", "10")
				.build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6503_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6503_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6503_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalId2, 5.0, 1)
				.build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6503_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "20", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "40", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "120", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "120", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {10.0}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_2 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_2.contains(expectedMessage), "Message for the second offer is not matched");
		// 3rd offer -> Discount amount is {20.0}
		String discount_amount_3 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_3, "20.0", "Discount amount for the third offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {40.0} and item_id as {20001}
		String item_qty_31 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_31 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_31 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_31, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_31, "40.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_31, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-20.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_32 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_32 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_32 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_32, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_32, "-20.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_32, "20001", "Item ID for the second qualified item is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {10.0 + 20.0 = 30.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_3);
		Assert.assertEquals(totalDiscountAmount, 30.0, "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {10.0}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "20.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> Discount amount is {20.0}
		discount_amount_3 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_3, "20.0", "Discount amount for the third offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {40.0} and item_id as {20001}
		item_qty_31 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_31 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_31 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_31, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_31, "40.0", "Item amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_31, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-20.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false.
		item_qty_32 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_32 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_32 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_32, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_32, "-20.0", "Item amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_32, "20001", "Item ID for the second qualified item is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6498: [Stacking OFF, Reusability ON] Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having line item doesn't exist function and net quantity equal to 2.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6498_discountCalculationWithLISDoesntExistNetQuantityEqual() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6498_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6498_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_quantity_equal_to", lisExternalId1, 2.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6498_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6498_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6498_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("line_item_does_not_exist", lisExternalId2, 0.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6498_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "5", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "20", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {2.5}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "2.5", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "5.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.5", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].message[0]").toString();
		Assert.assertTrue(message_2.contains(expectedMessage), "Message for the second offer is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {2.5}
		Assert.assertEquals(discount_amount_1, "2.5", "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {2.5}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "2.5", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "5.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.5", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 1);

	}

	@Test(description = "SQ-T6499: [Stacking OFF, Reusability ON] Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having all receipt qualifiers and different LIQ.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6499_discountCalculationWithReceiptQualifiersLISDifferentLIQ() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);

		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6499_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6499_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalId1, 5.0, 1)
				.addReceiptQualifier("total_amount", ">=", "10").addReceiptQualifier("subtotal_amount", ">=", "10")
				.build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6499_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6499_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6499_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalId2, 5.0, 1)
				.build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6499_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "20", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "40", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "120", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "120", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {10.0}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> Discount amount is {20.0}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "20.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {40.0} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "40.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-20.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-20.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {10.0 + 20.0 = 30.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_2);
		Assert.assertEquals(totalDiscountAmount, 30.0, "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {10.0}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "20.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 2nd offer -> Discount amount is {20.0}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "20.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {40.0} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_21, "40.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the second offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-20.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false.
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-20.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6495: [Stacking ON, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having all receipt qualifiers and different LIQ.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6495_discountCalculationWithReceiptQualifiersLISDifferentLIQ() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6495_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6495_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(true)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalId1, 5.0, 1)
				.addReceiptQualifier("total_amount", ">=", "10").addReceiptQualifier("subtotal_amount", ">=", "10")
				.build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6495_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6495_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6495_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(true)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("net_amount_excluding_max_priced_item_equal_to_or_more_than", lisExternalId2, 5.0, 1)
				.build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6495_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "20", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "30", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {10.0}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> Discount amount is {15.0}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "15.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "30.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-15.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-15.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {10.0 + 15.0 = 25.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_2);
		Assert.assertEquals(totalDiscountAmount, 25.0, "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {10.0}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "10.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "20.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-10.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-10.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 2nd offer -> Discount amount is {15.0}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "15.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_21, "30.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the second offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-15.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false.
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-15.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6496: [Stacking OFF, Reusability ON] Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing % and amount cap")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6496_discountCalculationWithLISProcessingPercentAmountCap() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer2QualifiedItem2Qty = dataSet.get("expectedOffer2QualifiedItem2Qty" + decoupledFlagStatusStr);

		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6496_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6496_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2").setPercentageOfProcessedAmount(20)
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0)
				.build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6496_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6496_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6496_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(true).setQCProcessingFunction("bogof2").setAmountCap(1)
				.addLineItemFilter(lisExternalId2, "", 0).addItemQualifier("line_item_exists", lisExternalId2, 0.0, 0)
				.build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6496_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "20", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "30", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "100", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {2.0}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "2.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "20.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> Discount amount is {1.0}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "1.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "30.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-1.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-1.0", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {2.0 + 1.0 = 3.0}
		double totalDiscountAmount = Double.parseDouble(discount_amount_1) + Double.parseDouble(discount_amount_2);
		Assert.assertEquals(totalDiscountAmount, 3.0, "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {2.0}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "2.0", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {20.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "20.0", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.0} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 2nd offer -> Discount amount is {1.0}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "1.0", "Discount amount for the second offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {30.0} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the second offer is not matched");
		Assert.assertEquals(amount_21, "30.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the second offer is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-1.0} and item_id as {20001}
		// with flag true and item_qty as {2.0} with flag false.
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer2QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-1.0", "Item amount for the second offer is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second offer is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	@Test(description = "SQ-T6502: [Stacking OFF, Reusability OFF]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having line item doesn't exist function and net quantity equal to 2.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6502_discountCalculationWithLISDoesntExistNetQuantityEqual() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		
		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6502_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6502_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_quantity_equal_to", lisExternalId1, 2.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6502_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6502_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6502_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("line_item_does_not_exist", lisExternalId2, 0.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6502_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "5", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "20", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "50", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> Discount amount is {2.5}
		String discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_1, "2.5", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "5.0", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.5", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_2 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_2.contains(expectedMessage), "Message for the second offer is not matched");
		// 3rd offer -> message contains {Discount qualification on receipt failed}
		String message_3 = discountLookupResponse.jsonPath().get("selected_discounts[2].message[0]").toString();
		Assert.assertTrue(message_3.contains(expectedMessage), "Message for the third offer is not matched");
		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		// Total Discount Amount = {2.5}
		Assert.assertEquals(discount_amount_1, "2.5", "Total discount amount is not matched");
		utils.logPass("Discount Lookup API Response is verified. Total discount amount is: " + discount_amount_1);

		// == Verify Batch Redemption API Response ==
		// 1st offer -> Discount amount is {2.5}
		discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discount_amount_1, "2.5", "Discount amount for the first offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.0} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "5.0", "Item amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.5} and item_id as {10001}
		// with flag true and item_qty as {2.0} with flag false
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, "-2.5", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		utils.logPass("Batch Redemption API Response is verified");

		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 1);

	}

	@Test(description = "SQ-T6501: [Stacking OFF, Reusability OFF] Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having different LIS and LIQ config.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6501_discountCalculationWithVariedLISAndLIQConfig() throws Exception {
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
		String expectedOffer1QualifiedItem2Qty = dataSet.get("expectedOffer1QualifiedItem2Qty" + decoupledFlagStatusStr);
		String expectedOffer1QualifiedItem2Amt = dataSet.get("expectedOffer1QualifiedItem2Amt" + decoupledFlagStatusStr);
		String expectedOffer3QualifiedItem2Qty = dataSet.get("expectedOffer3QualifiedItem2Qty" + decoupledFlagStatusStr);

		// Create LIS 1
		String lisName1 = "Automation_LIS_SQ_T6501_1_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "10001,10002").build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response1.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalId1);
		// Create QC 1
		String qcname1 = "Automation_QC_SQ_T6501_1_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2").setRoundingRule("round")
				.addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("net_quantity_greater_than_or_equal_to", lisExternalId1, 2.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 1 External ID
		qcExternalId1 = qcResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable 1
		String redeemableName1 = "Automation_Redeemable_SQ_T6501_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1)
				.setDescription(redeemableName1 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 1 External ID
		redeemableExternalId1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName1 + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable 1 ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Create LIS 2
		String lisName2 = "Automation_LIS_SQ_T6501_2_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "20001,20002").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId2 = response2.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName2 + " LIS is created with External ID: " + lisExternalId2);
		// Create QC 2
		String qcname2 = "Automation_QC_SQ_T6501_2_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("bogof2").setEffectiveLocationsID("408901")
				.addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("net_amount_greater_than_or_equal_to", lisExternalId2, 5.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC 2 External ID
		qcExternalId2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname2 + " QC is created with External ID: " + qcExternalId2);
		// Create Redeemable 2
		String redeemableName2 = "Automation_Redeemable_SQ_T6501_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setDescription(redeemableName2 + " description").setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId2).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable 2 External ID
		redeemableExternalId2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName2 + " Redeemable is created with External ID: " + redeemableExternalId2);
		// Get Redeemable 2 ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// send redeemable 1 and 2 to user twice
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId1,
				"2");
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId2,
				"2");
		utils.logPass("Both " + redeemableName1 + " and " + redeemableName2 + " are sent two times to user");
		// Get reward IDs for all 4
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId1, 20);
		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId2, 20);
		// Add all 4 rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_1, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable_2, userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), externalUID);

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
		addDetails.accept("Sandwich1", new String[] { "Sandwich", "2", "5.8", "M", "11", "23", "1.0", "10001" });
		addDetails.accept("Sandwich2", new String[] { "Sandwich", "2", "5.8", "M", "11", "23", "2.0", "20001" });

		// Hit POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userInfo.get("userID"), "30", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Hit POS Batch Redemption Process API
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "30", parentMap);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// == Verify Discount Lookup API Response ==
		// 1st offer -> First Qualified item_qty as {2.0}, amount as {5.8} and item_id as {10001}
		String item_qty_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_qty")
				.toString();
		String amount_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].amount")
				.toString();
		String item_id_11 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_11, "5.8", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0} with flag true. item_qty as {2.0}
		// and amount as {-3.0} with flag false. item_id remains {10001}
		String item_qty_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_qty")
				.toString();
		String amount_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].amount")
				.toString();
		String item_id_12 = discountLookupResponse.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, expectedOffer1QualifiedItem2Amt, "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second qualified item is not matched");
		// 2nd offer -> message contains {Discount qualification on receipt failed}
		String expectedMessage = "Discount qualification on receipt failed";
		String message_2 = discountLookupResponse.jsonPath().get("selected_discounts[1].message[0]").toString();
		Assert.assertTrue(message_2.contains(expectedMessage), "Message for the second offer is not matched");
		// 3rd offer -> Discount amount is {2.9}
		String discount_amount_2 = discountLookupResponse.jsonPath().get("selected_discounts[2].discount_amount")
				.toString();
		Assert.assertEquals(discount_amount_2, "2.9", "Discount amount for the third offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.8} and item_id as {20001}
		String item_qty_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_qty")
				.toString();
		String amount_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].amount")
				.toString();
		String item_id_21 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[0].item_id")
				.toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "5.8", "Amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.9} and item_id as {20001}
		// with flag true. item_qty as {2.0} with flag false
		String item_qty_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_qty")
				.toString();
		String amount_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].amount")
				.toString();
		String item_id_22 = discountLookupResponse.jsonPath().get("selected_discounts[2].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(item_qty_22, expectedOffer3QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-2.9", "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");

		// 4th offer -> message contains {Discount qualification on receipt failed}
		String message_4 = discountLookupResponse.jsonPath().get("selected_discounts[3].message[0]").toString();
		Assert.assertTrue(message_4.contains(expectedMessage), "Message for the fourth offer is not matched");
		utils.logPass("Discount Lookup API Response is verified");

		// == Verify Batch Redemption API Response ==
		// First Qualified item_qty as {2.0}, amount as {5.8} and item_id as {10001}
		item_qty_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_qty").toString();
		amount_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].amount").toString();
		item_id_11 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_11, "2.0", "Item quantity for the first offer is not matched");
		Assert.assertEquals(amount_11, "5.8", "Item amount for the first offer is not matched");
		Assert.assertEquals(item_id_11, "10001", "Item ID for the first offer is not matched");
		// Second Qualified item_qty as {1.0} with flag true. item_qty as {2.0}
		// and amount as {-3.0} with flag false. item_id remains {10001}
		item_qty_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_qty").toString();
		amount_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].amount").toString();
		item_id_12 = batchRedemptionResponse.jsonPath().get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_12, expectedOffer1QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_12, expectedOffer1QualifiedItem2Amt, "Amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_12, "10001", "Item ID for the second offer is not matched");
		// 3rd offer -> Discount amount is {2.9}
		discount_amount_2 = batchRedemptionResponse.jsonPath().get("success[1].discount_amount").toString();
		Assert.assertEquals(discount_amount_2, "2.9", "Discount amount for the third offer is not matched");
		// First Qualified item_qty as {2.0}, amount as {5.8} and item_id as {20001}
		item_qty_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_qty").toString();
		amount_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].amount").toString();
		item_id_21 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(item_qty_21, "2.0", "Item quantity for the first qualified item is not matched");
		Assert.assertEquals(amount_21, "5.8", "Item amount for the first qualified item is not matched");
		Assert.assertEquals(item_id_21, "20001", "Item ID for the first qualified item is not matched");
		// Second Qualified item_qty as {1.0}, amount as {-2.9} and item_id as {20001}
		// with flag true. item_qty as {2.0} with flag false.
		item_qty_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_qty").toString();
		amount_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].amount").toString();
		item_id_22 = batchRedemptionResponse.jsonPath().get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(item_qty_22, expectedOffer3QualifiedItem2Qty, "Item quantity for the second qualified item is not matched");
		Assert.assertEquals(amount_22, "-2.9", "Item amount for the second qualified item is not matched");
		Assert.assertEquals(item_id_22, "20001", "Item ID for the second qualified item is not matched");
		utils.logPass("Batch Redemption API Response is verified");
		// Discount Amounts are being verified for flag false scenario only = {3.0 + 2.9 = 5.9}
		if (!decoupledFlagStatus) {
			String lookup_discount_amount_1 = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
					.toString();
			Assert.assertEquals(lookup_discount_amount_1, "3", "Discount amount for the first offer is not matched");
			double totalDiscountAmount = Double.parseDouble(lookup_discount_amount_1) + Double.parseDouble(discount_amount_2);
			Assert.assertEquals(totalDiscountAmount, 5.9, "Total discount amount is not matched");
			String redemption_discount_amount_1 = batchRedemptionResponse.jsonPath().get("success[0].discount_amount")
					.toString();
			Assert.assertEquals(redemption_discount_amount_1, lookup_discount_amount_1, "Discount amount for the first offer is not matched");
		}
		verifyRedemptionTableEntries(userInfo.get("userID"), businessID, 2);

	}

	private void verifyRedemptionTableEntries(String userID, String businessID, int expectedEntries) throws Exception {
		// == Verify the entries in redemptions table for the user ==
		String redemptionCodeIdQuery = "SELECT redemption_code_id FROM redemptions WHERE user_id = " + userID
				+ " AND business_id=" + businessID + ";";
		DBUtils.executeQueryAndGetColumnValuePollingUsed(env, redemptionCodeIdQuery, "redemption_code_id", 3);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, redemptionCodeIdQuery, "redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), expectedEntries);
		utils.logPass("Redemption code id entries are created in redemptions table");
		// == Verify the status should be processed in redemption_codes table ==
		String redemptionStatusQuery = "SELECT status FROM redemption_codes WHERE id = " + redemptionIds.get(0)
				+ " AND business_id=" + businessID + ";";
		String status = DBUtils.executeQueryAndGetColumnValue(env, redemptionStatusQuery, "status");
		Assert.assertEquals(status, "processed");
		utils.logPass("Status is processed in redemption_codes table for id: " + redemptionIds.get(0));
		// == Verify the entries in redemption_logs table ==
		String redemptionLogsQuery = "SELECT id FROM redemption_logs WHERE redemption_code_id = " + redemptionIds.get(0)
				+ ";";
		String id = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, redemptionLogsQuery, "id", 40);
		Assert.assertTrue(!id.isEmpty());
		utils.logPass("Redemption log entry is created for id: " + id);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalId1, qcExternalId1, redeemableExternalId1);
		utils.deleteLISQCRedeemable(env, lisExternalId2, qcExternalId2, redeemableExternalId2);
		pageObj.utils().clearDataSet(dataSet);
	}

}
