package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
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
public class ValidationInBatchRedemptionProcessOMM_T147 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_T147.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;
	Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, redeemableExternalID,qcExternalID2, redeemableExternalID2, businessID, lisExternalID2;
	public Boolean originalDecoupledRedemptionFlag;
	private StackingReusabilityDiscountDistributionTest flagObj;
	
	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils = new Utilities(driver);
		utils.logit(sTCName + " ==>" + dataSet);
		enableMenuItemAggregatorFlag = false;
		apipayloadObj = new ApiPayloadObj();
		originalDecoupledRedemptionFlag = null;
		businessID = dataSet.get("business_id");
		flagObj = new StackingReusabilityDiscountDistributionTest();
	}

	@Test(description = "SQ-T3448 - Step 1: Verify cases for Item Qualifiers expression 'Net Amount Greater Than Or Equal To'", priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void verify147_1() throws Exception {
		
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));
		utils.logit("Expected Redemption 1 Discount Amount: " + expRedeemption1DiscountAmt);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmail);

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logit("User signup API HTTP status: " + signUpResponse.getStatusCode());
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API 2 user signup failed");

		String token = signUpResponse.jsonPath().getString("access_token.token");
		utils.logit("User token generated: " + token);
		Assert.assertNotNull(token, "Access token is null after signup");

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		utils.logit("User ID generated: " + userID);
		Assert.assertNotNull(userID, "User ID is null after signup");
		
		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_T147_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "1001").build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);
		
		// ===================== Create LIS 2 =====================
		String lisName2 = "AutoSelect_LIS_2_T147_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "2001").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response2.prettyPrint());
		lisExternalID2 = response2.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID2);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_T147_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to",lisExternalID2, 10.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable =====================
		String redeemableName = "AutoRedeemable_T147_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", false)
				.setEndTime(dataSet.get("endTime"))
				.setTimeZone(dataSet.get("timeZone"))
				.setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");
		
		
		// =====================️ Create QC 2 =====================
		String qcname2 = "AutoSelect_QC2_T147_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2)
				.setPercentageOfProcessedAmount(30)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to",lisExternalID2, 10.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logit(qcname2 + "QC is created with External ID: " + qcExternalID2);

		// ===================== Create Redeemable 2 =====================
		String redeemableName2 = "AutoRedeemable_2_T147_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID2).build())
				.setBooleanField("indefinetely", false)
				.setEndTime(dataSet.get("endTime"))
				.setTimeZone(dataSet.get("timeZone"))
				.setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName2 + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId2);
				

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId2, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse2.asPrettyString());
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 1: " + rewardID1);

		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 2: " + rewardID2);

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		utils.logit(rewardID1 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID1 + " to basket API failed");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		utils.logit(rewardID2 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID2 + " to basket API failed");
		
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "1001" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "10", "M", "10", "888", "2", "2001" });
		

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		utils.logit("Actual Redemption 1 Discount Amount: " + actualRedeemption1DiscountAmt);
		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt, 
		        "Redemption 1 discount amount mismatch");

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt;
		utils.logit("Actual Total Discount Amount: " + actualTotalDiscountAmount);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));
		utils.logit("Expected Total Discount Amount: " + expTotalDiscountAmount);

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount,
		        "Overall total discount amount for the redemption mismatch");

		utils.logit("Verified the overall total discount amount for the redemption: " + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		utils.logit("Expected Redemption 2 Error Message: " + expErrorMessage);

		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
		        .getString("failures[0].message").replace("[", "").replace("]", "");
		utils.logit("Actual Redemption 2 Error Message: " + actualRedeemption2ErrorMessage);

		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);
		Assert.assertTrue(result, "Redemption 2 error message did not match expected");

		utils.logit("Verified the error message for Redemption 2");
	}

	@Test(description = "SQ-T3448 - Step 2: Verify cases for Item Qualifiers expression 'Net Amount Greater Than Or Equal To'", priority = 1)
	@Owner(name = "Ashwini Shetty")
	public void verify147_2() throws Exception {
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));
		utils.logit("Expected Redemption 1 Discount Amount: " + expRedeemption1DiscountAmt);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmail);

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.logit("User signup API HTTP status: " + signUpResponse.getStatusCode());
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API 2 user signup failed");

		String token = signUpResponse.jsonPath().getString("access_token.token");
		utils.logit("User token generated: " + token);
		Assert.assertNotNull(token, "Access token is null after signup");

		String userID = signUpResponse.jsonPath().getString("user.user_id");
		utils.logit("User ID generated: " + userID);
		Assert.assertNotNull(userID, "User ID is null after signup");
		
		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_T147_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "1001").build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);
		
		// ===================== Create LIS 2 =====================
		String lisName2 = "AutoSelect_LIS_2_T147_" + Utilities.getTimestamp();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "2001").build();
		Response response2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response2.prettyPrint());
		lisExternalID2 = response2.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID2);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_T147_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to",lisExternalID2, 10.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable =====================
		String redeemableName = "AutoRedeemable_T147_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", false)
				.setEndTime(dataSet.get("endTime"))
				.setTimeZone(dataSet.get("timeZone"))
				.setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse.asPrettyString());
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");
		
		// =====================️ Create QC 2 =====================
		String qcname2 = "AutoSelect_QC2_T147_" + Utilities.getTimestamp();
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname2)
				.setPercentageOfProcessedAmount(30)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to",lisExternalID2, 10.0, 0).build();
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID2 = qcResponse2.jsonPath().getString("results[0].external_id");
		utils.logit(qcname2 + "QC is created with External ID: " + qcExternalID2);

		// ===================== Create Redeemable 2 =====================
		String redeemableName2 = "AutoRedeemable_2_T147_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID2).build())
				.setBooleanField("indefinetely", false)
				.setEndTime(dataSet.get("endTime"))
				.setTimeZone(dataSet.get("timeZone"))
				.setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName2 + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId2);
		
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId2, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse2.asPrettyString());
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");

		
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 1: " + rewardID1);

		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 2: " + rewardID2);

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		utils.logit(rewardID1 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID1 + " to basket API failed");


		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		utils.logit(rewardID2 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID2 + " to basket API failed");
		
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "10", "M", "10", "999", "1", "1001" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "2", "30", "M", "10", "888", "2", "2001" });


		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");


		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath().getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		utils.logit("actualRedeemption1DiscountAmt = " + String.valueOf(actualRedeemption1DiscountAmt));
		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt, "Redeemption1 discount amount mismatch");
		utils.logPass("Verified the total discount amount for the redeemption1 = " + actualRedeemption1DiscountAmt);

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt;
		utils.logit("actualTotalDiscountAmount = " + String.valueOf(actualTotalDiscountAmount));
		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));
		utils.logit("expTotalDiscountAmount = " + String.valueOf(expTotalDiscountAmount));
		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount, "Overall total discount amount mismatch");
		utils.logPass("Verified the overall total discount amount for the redeemption = " + actualTotalDiscountAmount);

		String expErrorMessage = dataSet.get("expErrorMessage");
		utils.logit("expErrorMessage = " + expErrorMessage);
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath().getString("failures[0].message").replace("[", "").replace("]", "");
		utils.logit("actualRedeemption2ErrorMessage = " + actualRedeemption2ErrorMessage);
		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);
		Assert.assertTrue(result, "Redeemption2 error message verification failed");
		utils.logPass("Verified the error message for the redeemption2");
	}

		
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		utils.deleteLISQCRedeemable(env, lisExternalID2, qcExternalID2, redeemableExternalID2);
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Data set cleared");
	}
	

}
