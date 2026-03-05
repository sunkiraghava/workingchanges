package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

/*
 * @Author :- Ashwini Shetty
 * 
 * TC - OMM-T77
*/

@Listeners(TestListeners.class)
public class ValidationInBatchProcessRedemptionOMM_T109 {
	static Logger logger = LogManager.getLogger(ValidationInBatchProcessRedemptionOMM_T109.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;
	Utilities utils;
	private ApiPayloadObj apipayloadObj;
	public Boolean originalDecoupledRedemptionFlag;
	private String lisExternalID_1001, qcExternalID, redeemableExternalID, businessID, decoupledFlagStatusStr,lisExternalID_2001;
	private StackingReusabilityDiscountDistributionTest flagObj;
	private OfferIngestionUtilities offerUtils;
	private boolean decoupledFlagStatus;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
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
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
		apipayloadObj = new ApiPayloadObj();
		originalDecoupledRedemptionFlag = null;
		businessID = dataSet.get("business_id");
		flagObj = new StackingReusabilityDiscountDistributionTest();
		offerUtils = new OfferIngestionUtilities(driver);
		decoupledFlagStatus = offerUtils.getDecoupledRedemptionEngineFlagStatus(env, dataSet.get("business_id"));
		decoupledFlagStatusStr = "_decoupledFlag_" + decoupledFlagStatus;
	}

	@Test(description = "SQ-T3330 Step 1 : Verify cases for Item Qualifiers expression 'Line Item Does Not Exist'", priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void validatePercentageDiscount_109() throws Exception {
		
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);
				
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
		String lisName_1001 = "Automation_LIS_1001__T109_" + Utilities.getTimestamp();
		String lisPayload_1001 = apipayloadObj.lineItemSelectorBuilder().setName(lisName_1001)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "1001").build();
		Response response_1001 = pageObj.endpoints().createLIS(lisPayload_1001, dataSet.get("apiKey"));
		Assert.assertEquals(response_1001.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response_1001.prettyPrint());
		lisExternalID_1001 = response_1001.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID_1001);

		// ===================== Create LIS 2 =====================
		String lisName_2001 = "Automation_LIS_2_2001__T109_" + Utilities.getTimestamp();
		String lisPayload_2001 = apipayloadObj.lineItemSelectorBuilder().setName(lisName_2001)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "2001").build();
		Response response_2001 = pageObj.endpoints().createLIS(lisPayload_2001, dataSet.get("apiKey"));
		Assert.assertEquals(response_2001.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response_2001.prettyPrint());
		lisExternalID_2001 = response_2001.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID_2001);
		
		// =====================️ Create QC =====================
		String qcname = "Automation_QC_1_T109_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID_1001, "max_price", 1)
				.addItemQualifier("line_item_does_not_exist",lisExternalID_2001, 0.0, 0).build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
		// ===================== Create Redeemable =====================
		String redeemableName = "AutoRedeemable_T109_"+ Utilities.getTimestamp();
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

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse2.asPrettyString());
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse3.asPrettyString());
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "authListAvailableRewardsNew API failed");
		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 1: " + rewardID1);
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 2: " + rewardID2);
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 3: " + rewardID3);

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

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		utils.logit(rewardID3 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID3 + " to basket API failed");
		
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "8", "M", "10", "999", "1", "1001" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "1001" });
		addDetails.accept("Pizza3", new String[] { "Pizza3", "1", "3", "M", "10", "888", "3", "2001" });

		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");

		String expErrorMessage = dataSet.get("expErrorMessage");
		utils.logit("Expected Error Message: " + expErrorMessage);
		
		String actualRedeemption1ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		utils.logit("Actual Redemption 1 Error Message: " + actualRedeemption1ErrorMessage);
		boolean result = pageObj.apiUtils().verifyErrorMessage(actualRedeemption1ErrorMessage, expErrorMessage);
		Assert.assertTrue(result, "Error message validation failed for Redemption 1. Expected: " 
		        + expErrorMessage + ", Actual: " + actualRedeemption1ErrorMessage);
	

		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		utils.logit("Actual Redemption 2 Error Message: " + actualRedeemption2ErrorMessage);
		boolean result2 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption2ErrorMessage, expErrorMessage);
		Assert.assertTrue(result2, "Error message validation failed for Redemption 2. Expected: " 
		        + expErrorMessage + ", Actual: " + actualRedeemption2ErrorMessage);

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[2].message").replace("[", "").replace("]", "");
		utils.logit("Actual Redemption 3 Error Message: " + actualRedeemption3ErrorMessage);
		boolean result3 = pageObj.apiUtils().verifyErrorMessage(actualRedeemption3ErrorMessage, expErrorMessage);
		Assert.assertTrue(result3, "Error message validation failed for Redemption 3. Expected: " 
		        + expErrorMessage + ", Actual: " + actualRedeemption3ErrorMessage);
	}

	@Test(description = "SQ-T3330 Step 2 : Verify cases for Item Qualifiers expression 'Line Item Does Not Exist'", priority = 1)
	@Owner(name = "Ashwini Shetty")
	public void validatePercentageDiscount1_109() throws Exception {
	
		
		// Fetch Expected values based on Decoupled Redemption Engine flag
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);

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
		String lisName_1001 = "Automation_LIS_1001__T109_" + Utilities.getTimestamp();
		String lisPayload_1001 = apipayloadObj.lineItemSelectorBuilder().setName(lisName_1001)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "1001").build();
		Response response_1001 = pageObj.endpoints().createLIS(lisPayload_1001, dataSet.get("apiKey"));
		Assert.assertEquals(response_1001.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response_1001.prettyPrint());
		lisExternalID_1001 = response_1001.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID_1001);

		// ===================== Create LIS 2 =====================
		String lisName_2001 = "Automation_LIS_2_2001__T109_" + Utilities.getTimestamp();
		String lisPayload_2001 = apipayloadObj.lineItemSelectorBuilder().setName(lisName_2001)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "2001").build();
		Response response_2001 = pageObj.endpoints().createLIS(lisPayload_2001, dataSet.get("apiKey"));
		Assert.assertEquals(response_2001.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response_2001.prettyPrint());
		lisExternalID_2001 = response_2001.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID_2001);

		// =====================️ Create QC =====================
		String qcname = "Automation_QC_1_T109_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10)
				.setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.addLineItemFilter(lisExternalID_1001, "max_price", 1)
				.addItemQualifier("line_item_does_not_exist",lisExternalID_2001, 0.0, 0).build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
		// ===================== Create Redeemable =====================
		String redeemableName = "AutoRedeemable_T109_"+ Utilities.getTimestamp();
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


		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse2.asPrettyString());
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");


		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		utils.logit("Send reward API response: " + sendRewardResponse3.asPrettyString());
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Send reward API failed");


		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "authListAvailableRewardsNew API failed");
		
		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 1: " + rewardID1);
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 2: " + rewardID2);
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");
		utils.logit("Reward ID 3: " + rewardID3);


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

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		utils.logit(rewardID3 + "  is added to basket");
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Add Reward ID " + rewardID3 + " to basket API failed");
		
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
		addDetails.accept("Pizza1", new String[] { "Pizza1", "1", "8", "M", "10", "999", "1", "1001" });
		addDetails.accept("Pizza2", new String[] { "Pizza2", "1", "7", "M", "10", "888", "2", "1001" });
		addDetails.accept("Pizza3", new String[] { "Pizza3", "1", "3", "M", "10", "888", "3", "1001" });


		// Process basket by user1
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID,
						dataSet.get("subAmount"),parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for processBatchRedemptionOfBasketPOSNewDiscountLookup API for User1");
		utils.logit("Batch redemption processed successfully for User1");
		
		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));
		utils.logit("Expected Redemption 1 Discount Amount: " + expRedeemption1DiscountAmt);

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));
		utils.logit("Expected Redemption 2 Discount Amount: " + expRedeemption2DiscountAmt);

		double expRedeemption3DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount3"));
		utils.logit("Expected Redemption 3 Discount Amount: " + expRedeemption3DiscountAmt);


		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));
		utils.logit("Actual Redemption 1 Discount Amount: " + actualRedeemption1DiscountAmt);
		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt, 
				"Mismatch in Redemption 1 discount amount. Expected: " + expRedeemption1DiscountAmt + ", Actual: " + actualRedeemption1DiscountAmt);
		utils.logPass("Verified the total discount amount for Redemption 1 = " + actualRedeemption1DiscountAmt);


		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));
		utils.logit("Actual Redemption 2 Discount Amount: " + actualRedeemption2DiscountAmt);
		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt, 
				"Mismatch in Redemption 2 discount amount. Expected: " + expRedeemption2DiscountAmt + ", Actual: " + actualRedeemption2DiscountAmt);
		utils.logPass("Verified the total discount amount for Redemption 2 = " + actualRedeemption2DiscountAmt);


		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));
		utils.logit("Actual Redemption 3 Discount Amount: " + actualRedeemption3DiscountAmt);
		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt, 
				"Mismatch in Redemption 3 discount amount. Expected: " + expRedeemption3DiscountAmt + ", Actual: " + actualRedeemption3DiscountAmt);
		utils.logPass("Verified the total discount amount for Redemption 3 = " + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));
		utils.logit("Expected Total Discount Amount: " + expTotalDiscountAmount);
		
		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;
		utils.logit("Actual Total Discount Amount: " + actualTotalDiscountAmount);
		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount, 
				"Mismatch in overall total discount amount. Expected: " + expTotalDiscountAmount + ", Actual: " + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		utils.logit("Actual Discount Type: " + actualDiscountType);
		Assert.assertEquals(actualDiscountType, "reward");
		utils.logPass("Verified the discount type in batch process response");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalID_1001, qcExternalID, redeemableExternalID);
		utils.deleteLISQCRedeemable(env, lisExternalID_2001, "", "");
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Data set cleared");
	}
}
