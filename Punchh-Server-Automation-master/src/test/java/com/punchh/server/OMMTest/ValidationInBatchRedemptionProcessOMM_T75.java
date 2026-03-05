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
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOMM_T75 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_T75.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String userEmail;
	private OfferIngestionUtilities offerUtils;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalId1, qcExternalId1, redeemableExternalId1;

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
		offerUtils = new OfferIngestionUtilities(driver);
		utils = new Utilities();
		apipayloadObj = new ApiPayloadObj();
	}

	// Test cases T_75
//		Pre-condition -> Turn On Discount Stacking On (for all rewards)
//		Guest Discount Basket ->
//		Reward 1 -> Receipt total Amount
//		Reward 2 -> Receipt total Amount Reward 3 -> Receipt total Amount
	@Test(description = "OMM-T75/SQ-T3323 (1.0) STEP -1 Verify cases for QC -> Processing Function \"Receipt total Amount\" with total amount of $10", priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void validateReceiptTotalAmountFor10() throws Exception {
		// Create LIS
		String lisName = "Automation_LIS_SQ_T3323_1_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName + " LIS is created with External ID: " + lisExternalId1);
		// Create QC
		String qcname = "Automation_QC_SQ_T3323_1_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("receipt_total_amount")
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalId1 = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable
		String redeemableName = "Automation_Redeemable_SQ_T3323_1_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false).setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalId1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// Send Reedemable to user 3 times
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId,
				"3");
		// Get reward IDs
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId, 20);
		utils.logInfo("Reward IDs for redeemable ID " + redeemableId + " are: " + rewardIdListForRedeemable);
		// Add the rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable, userInfo.get("token"),
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		// Hit POS Batch Redemption Process API
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "10", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify the API response
		double actualRedemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption1DiscountAmt, 10.0);
		utils.logPass("Verified the total discount amount for the redemption1 = " + actualRedemption1DiscountAmt);
		String actualRedemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedemption2ErrorMessage, MessagesConstants.redemptionNotPossible);
		utils.logPass("Verified the error message for the redemption 2 = " + actualRedemption2ErrorMessage);
		String actualRedemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedemption3ErrorMessage, MessagesConstants.redemptionNotPossible);
		utils.logPass("Verified the error message for the redemption 3 = " + actualRedemption3ErrorMessage);
		
	}

	@Test(description = "OMM-T75/SQ-T3323 (1.0) STEP -2 Verify cases for QC -> Processing Function \"Receipt total Amount\"", priority = 1)
	@Owner(name = "Ashwini Shetty")
	public void validateReceiptTotalAmount() throws Exception {
		// Create LIS
		String lisName = "Automation_LIS_SQ_T3323_2_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName + " LIS is created with External ID: " + lisExternalId1);
		// Create QC
		String qcname = "Automation_QC_SQ_T3323_2_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("receipt_total_amount")
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalId1 = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable
		String redeemableName = "Automation_Redeemable_SQ_T3323_2_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false).setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalId1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// Send Reedemable to user 3 times
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId,
				"3");
		// Get reward IDs
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId, 20);
		utils.logInfo("Reward IDs for redeemable ID " + redeemableId + " are: " + rewardIdListForRedeemable);
		// Add the rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable, userInfo.get("token"),
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });
		// Hit POS Batch Redemption Process API
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "21", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify the API response based on the Decoupled Redemption Engine flag status
		String actualRedemption1DiscountAmt = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedemption1DiscountAmt, "21.0");
		utils.logPass("Verified the total discount amount for the redemption1 = " + actualRedemption1DiscountAmt);
		String actualRedemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedemption2ErrorMessage, MessagesConstants.redemptionNotPossible);
		utils.logPass("Verified the error message for redemption 2 = " + actualRedemption2ErrorMessage);
		String actualRedemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		Assert.assertTrue(actualRedemption3ErrorMessage.contains(MessagesConstants.redemptionNotPossible));
		utils.logPass("Verified the error message for redemption 3 = " + actualRedemption3ErrorMessage);

	}

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Total Amount (Percentage)
//	Reward 2 -> Receipt Total Amount (Percentage)
//	Reward 3 -> Receipt Total Amount (Percentage)
	@Test(description = "OMM-T75/SQ-T3323 (1.0) STEP -3 Verify cases for QC -> Processing Function \"Receipt Total Amount\" with percentage", priority = 2)
	@Owner(name = "Ashwini Shetty")
	public void validateReceiptTotalAmountPercentageRedemption() throws Exception {
		// Create LIS
		String lisName = "Automation_LIS_SQ_T3323_3_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName + " LIS is created with External ID: " + lisExternalId1);
		// Create QC
		String qcname = "Automation_QC_SQ_T3323_3_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setPercentageOfProcessedAmount(25)
				.setQCProcessingFunction("receipt_total_amount")
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalId1 = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable
		String redeemableName = "Automation_Redeemable_SQ_T3323_3_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false).setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalId1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// Send Reedemable to user 3 times
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId,
				"3");
		// Get reward IDs
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId, 20);
		utils.logInfo("Reward IDs for redeemable ID " + redeemableId + " are: " + rewardIdListForRedeemable);
		// Add the rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable, userInfo.get("token"),
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "4", "M", "10", "889", "3", "201" });
		// Hit POS Batch Redemption Process API
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "21", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify the API response
		double actualRedemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption1DiscountAmt, 5.25);
		utils.logPass("Verified the total discount amount for the redemption 1 = " + actualRedemption1DiscountAmt);

		double actualRedemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption2DiscountAmt, 3.94);
		utils.logPass("Verified the total discount amount for the redemption 2 = " + actualRedemption2DiscountAmt);

		double actualRedemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption3DiscountAmt, 2.95);
		utils.logPass("Verified the total discount amount for the redemption 3 = " + actualRedemption3DiscountAmt);

		double actualTotalDiscountAmount = actualRedemption1DiscountAmt + actualRedemption2DiscountAmt
				+ actualRedemption3DiscountAmt;
		Assert.assertEquals(actualTotalDiscountAmount, 12.14);
		utils.logPass("Verified the overall total discount amount for the redemption = " + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");
		utils.logPass("Verified the discount type in batch process response is = " + actualDiscountType);

		// Verify Auto Checkin gets created from DB
		query = OfferIngestionUtilities.getCheckinDetailsForUserQuery.replace("$user_id", userInfo.get("userID"))
				.replace("$checkin_type", dataSet.get("checkinType")).replace("$location_id", dataSet.get("locationId"));
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env, query,
				new String[] { "receipt_amount", "points_earned" }, 10, 18);
		Assert.assertEquals(values.get(0).get("receipt_amount"), "8.86", "receipt_amount mismatch in checkins table");
		Assert.assertEquals(values.get(0).get("points_earned"), "9", "points_earned mismatch in checkins table");
		utils.logPass("Verified Auto checkin is created with receipt amount: " + values.get(0).get("receipt_amount")
				+ " and points earned: " + values.get(0).get("points_earned"));

	}

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount (Amount Cap)
//	Reward 2 -> Receipt Subtotal Amount (Amount Cap)
//  Reward 3 -> Receipt Subtotal Amount (Amount Cap)
	@Test(description = "OMM-T75/SQ-T3323 (1.0) STEP-4 Verify cases for QC -> Processing Function \"Receipt Total Amount\"", priority = 3)
	@Owner(name = "Ashwini Shetty")
	public void validateReceiptTotalAmountAmountCapRedemption() throws Exception {

		// Create LIS
		String lisName = "Automation_LIS_SQ_T3323_4_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalId1 = response.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName + " LIS is created with External ID: " + lisExternalId1);
		// Create QC
		String qcname = "Automation_QC_SQ_T3323_4_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setAmountCap(5).setQCProcessingFunction("receipt_total_amount")
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalId1 = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalId1);
		// Create Redeemable
		String redeemableName = "Automation_Redeemable_SQ_T3323_4_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false).setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalId1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(redeemableName + " Redeemable is created with External ID: " + redeemableExternalId1);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalId1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass("Signup is successful for userID: " + userInfo.get("userID"));

		// Send Reedemable to user 3 times
		pageObj.redeemablesPage().sendRedeemableNTimes(userInfo.get("userID"), dataSet.get("apiKey"), redeemableId,
				"3");
		// Get reward IDs
		List<String> rewardIdListForRedeemable = pageObj.redeemablesPage().getRewardIdList(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), redeemableId, 20);
		utils.logInfo("Reward IDs for redeemable ID " + redeemableId + " are: " + rewardIdListForRedeemable);
		// Add the rewards to basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.redeemablesPage().addRewardsToBasket(rewardIdListForRedeemable, userInfo.get("token"),
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "101" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "4", "M", "10", "888", "2", "101" });
		addDetails.accept("Coffee1", new String[] { "Coffee1", "1", "2", "M", "10", "889", "3", "201" });
		// Hit POS Batch Redemption Process API
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userInfo.get("userID"), "16", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Verify the API response
		double actualRedemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption1DiscountAmt, 5.0);
		utils.logPass("Verified the total discount amount for the redemption 1 = " + actualRedemption1DiscountAmt);

		double actualRedemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption2DiscountAmt, 5.0);
		utils.logPass("Verified the total discount amount for the redemption 2 = " + actualRedemption2DiscountAmt);

		double actualRedemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemption3DiscountAmt, 5.0);
		utils.logPass("Verified the total discount amount for the redemption 3 = " + actualRedemption3DiscountAmt);

		double actualTotalDiscountAmount = actualRedemption1DiscountAmt + actualRedemption2DiscountAmt
				+ actualRedemption3DiscountAmt;
		Assert.assertEquals(actualTotalDiscountAmount, 15.0);
		utils.logPass("Verified the overall total discount amount for the redemption = " + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");
		utils.logPass("Verified the discount type as in API response = " + actualDiscountType);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalId1, qcExternalId1, redeemableExternalId1);
		pageObj.utils().clearDataSet(dataSet);
	}

}
