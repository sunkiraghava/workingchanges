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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * As discussed with Hitesh, this class needs to be run on enable_decoupled_redemption_engine: true only.
 * We are updating the flag status to true and then reverting it back to original status in the AfterMethod.
 */

@Listeners(TestListeners.class)
public class AutoSelectApiDItemHandlingTest {
	private static Logger logger = LogManager.getLogger(AutoSelectApiDItemHandlingTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String businessID, lisExternalId1, lisExternalId2, qcExternalId1, redeemableExternalID1, existing_redeeming_criterion_id;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private StackingReusabilityDiscountDistributionTest flagObj;
	private OfferIngestionUtilities offerUtils;
	public Boolean originalDecoupledRedemptionFlag;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		businessID = dataSet.get("business_id");
		utils = new Utilities();
		apipayloadObj = new ApiPayloadObj();
		flagObj = new StackingReusabilityDiscountDistributionTest();
		originalDecoupledRedemptionFlag = null;
		existing_redeeming_criterion_id = null;
		offerUtils = new OfferIngestionUtilities(driver);
	}
	
	private String createLIS(String testId,LineItemSelectorFilterItemSet filterItemSet,String baseItemId,String modifierItemId,String apiKey) throws JsonProcessingException {

	    String lisName = "Automation_LIS_SQ_"+testId +"_" + CreateDateTime.getTimeDateString();
	    var builder = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(filterItemSet).addBaseItemClause("item_id", "in", baseItemId);

	    if (filterItemSet == LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS && modifierItemId != null) {
	        builder.addModifierClause("item_id", "in", modifierItemId);
	    }
	    String lisPayload = builder.build();
	    Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, apiKey);

	    Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Create LIS API response status code mismatch");
	    boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "Expected success to be true, but it was false in the response");
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("LIS '" + lisName + "' has been created successfully");
		return lisExternalId1;
	}
	
	private String createRedeemable(String testId, String qcExternalId) throws Exception {
		String redeemableName = "Automation_Redeemable_" + testId + "_" + CreateDateTime.getTimeDateString();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
				.redeeming_criterion_id(qcExternalId1).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create Redeemable API response status code mismatch");
		redeemableExternalID1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1, "Expected results[0].external_id to be present in redeemable response, but it was null");
		utils.logit("Redeemable '" + redeemableName + "' created successfully");
		String getRedeemableIdQuery = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery, "id");
		utils.logit("Redeemable ID from DB:: '" + redeemableId + "");
		return redeemableId;
	}
	
	private String signUpUserAndSendRedeemable(String redeemableId) {
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch for user");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("Mobile API2 Signup is successful for user with ID: " + userID);
		// Send redeemable to user
		pageObj.redeemablesPage().sendRedeemableNTimes(userID, dataSet.get("apiKey"), redeemableId, "1");
		return userID;
	}

	@Test(description = "SQ-T7150: Verify Auto-Select API when the D item amount is less than the M item amount", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7150_verifyAutoSelectApiWithDAmountLessThanM() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1 = createLIS("T7150",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));

		// Create QC (Processing Function = hit_target_price)
		String qcName = "Automation_QC_SQ_T7150_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(1.0)
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7150", qcExternalId1);

		// SignUp User > Send redeemable to user
		String userID = signUpUserAndSendRedeemable(redeemableId);

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
		addDetails.accept("item_1", new String[] { "item_1", "1", "20", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "9", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", true, env);
		utils.logPass("Verified Auto-Select API when the D item amount is less than the M item amount");

	}

	@Test(description = "SQ-T7151: Verify Auto-Select API when the D item amount is more than the M item amount", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7151_verifyAutoSelectApiWithDAmountMoreThanM() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1 = createLIS("T7151",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7151_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7151", qcExternalId1);

		// SignUp User > Send redeemable to user
		String userID = signUpUserAndSendRedeemable(redeemableId);

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
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "11", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API > Verify response and no DB entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", false, env);
		utils.logPass("Verified Auto-Select API when D item amount is more than the M item amount");

	}

	@Test(description = "SQ-T7152: Verify the Auto Select API when the D item value is 0 & M item has value more than 0", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7152_verifyAutoSelectApiWithD0AndMMoreThan0() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1 = createLIS("T7152",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));

		// Create QC (Processing Function = rate_rollback)
		String qcName = "Automation_QC_SQ_T7152_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(1.0)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7152", qcExternalId1);

		// SignUp User > Send redeemable to user
		String userID = signUpUserAndSendRedeemable(redeemableId);

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
		addDetails.accept("item_1", new String[] { "item_1", "1", "5", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "0", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", true, env);
		utils.logPass("Verified Auto-Select API when the D item value is 0 & M item has value more than 0");

	}

	@Test(description = "SQ-T7144: Verify the Auto Select API when the D item has value & M item has value 0", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7144_verifyAutoSelectApiWithDItemHasValueAndMItemHasZeroValue() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7144",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7144_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7144", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("item_1", new String[] { "item_1", "1", "0", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "10", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "", false, env);
		utils.logPass("Verified the Auto Select API when the D item has value & M item has value 0");
	}
	
	@Test(description = "SQ-T7143: Verify the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type= Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7143_verifyAutoSelectApiWithQualificationOfSingleMAndSingleD() throws Exception {
	
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7143",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		
		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7143_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1, "Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7143", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "5", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", true, env);
		utils.logPass("Verified the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type= Reward)");
	}
	
	@Test(description = "SQ-T7145: Verify the qualification of Receipt when only D item is provided for auto-select API (Discount type= Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7145_verifyAutoSelectApiWithOnlyDItemIsProvided() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7145",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		
		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7145_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7145", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);
		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Discounted_Item", new String[] { "Discounted_Item", "1", "10", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
	
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "", false, env);
		utils.logPass("Verified the qualification of Receipt when only D item is provided for auto-select API (Discount type= Reward)");
	}
	
	
	@Test(description = "SQ-T7146: Rate Rollback | Verify the qualification of M & D items for auto-select API with LIQ-> Net Quantity Greater than (Discount type= Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7146_verifyAutoSelectApiWithQualiForMAndDWithLIQNetQtyGreaterThan() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		
		// Create LIS
		lisExternalId1=createLIS("T7146",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));

		// Create QC (Processing Function = rate_rollback)
		String qcName = "Automation_QC_SQ_T7146_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0)
				.addItemQualifier("net_quantity_greater_than_or_equal_to", lisExternalId1, 2.0, 2).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Create QC API response status code mismatch");
		
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7146", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);

		// ------------------- Scenario 1: Net Qty < Threshold ------------------- //
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
		
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "2", "D", "11", "23", "1.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "", false, env);
		
		
		// ------------------- Scenario 2: Net Qty >= Threshold ------------------- //
		
		parentMap.clear();
		
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetailsItemQtytwo = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetailsItemQtytwo.accept("item_1", new String[] { "item_1", "2", "10", "M", "11", "23", "1.0", "101" });
		addDetailsItemQtytwo.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "2", "D", "11", "23", "1.0", "101" });

		// Hit POS Auto Select API
		Response autoSelectResponseItemQtyTwo = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",
				Integer.toString(Utilities.getRandomNoFromRange(50000, 100000)), parentMap);
		Assert.assertEquals(autoSelectResponseItemQtyTwo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponseItemQtyTwo, userID, "reward", true, env);
		utils.logPass("Verified the qualification of M & D items for auto-select API with LIQ-> Net Quantity Greater than (Discount type= Reward)");

	}

	@Test(description = "SQ-T7156: Verify the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type- Discount Amount); "
			+ "SQ-T7164: Verify the database entries are created for every discount type in discount_basket_items table", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7156_verifyAutoSelectWithDiscountAmountSingleMAndD() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);

		// SignUp user > Send reward amount to user
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch for user");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), 
				"10", "", "", "");
		Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send Message to User API response status code mismatch");
		utils.logit("Reward amount 10 is sent to signed-up user ID " + userID);

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
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "5", "D", "11", "23", "5.0", "101" });

		// SQ-T7164 is covered below for discount type = Discount Amount.
		// For other discount types, it is covered in SQ-T7150, SQ-T7153, SQ-T7158.
		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "20",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "discount_amount", true, env);
		utils.logPass("Verified Auto-Select API for qualification of Single M & Single D items when Discount type = Discount Amount");
	}

	@Test(description = "SQ-T7157: Verify the qualification of Receipt when only D item is provided for auto-select API (Discount type- Discount Amount)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7157_verifyAutoSelectWithDiscountAmountOnlyD() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);

		// Create LIS
		lisExternalId1 = createLIS("T7157", LineItemSelectorFilterItemSet.BASE_ONLY, "101", "", dataSet.get("apiKey"));

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ-T7157_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");
		// Getting QC's Id value
		String idFromQCTableQuery = OfferIngestionUtilities.idFromQCQuery.replace("$external_id", qcExternalId1);
		String id = DBUtils.executeQueryAndGetColumnValue(env, idFromQCTableQuery, "id");
		utils.logit(qcName + " has Id: " + id);

		// Fetch and Store existing value for redeeming_criterion_id for Base Redeemable
		utils.logit("Fetching existing redeeming_criterion_id for Base Redeemable");
		String existingRedeemingCriterionId = OfferIngestionUtilities.currentRedeemableCriteriaIdQuery.replace("$redeemable_id",
				dataSet.get("redeemable_id"));
		existing_redeeming_criterion_id = DBUtils.executeQueryAndGetColumnValue(env, existingRedeemingCriterionId,
				"redeeming_criterion_id");
		utils.logit("Existing Redeemable Criterion ID for Base Redeemable is: " + existing_redeeming_criterion_id);

		// Updating redeeming_criterion_id to the desired value
		String updateRedeemableCriteriaColVal = OfferIngestionUtilities.updateRedeemableCriteriaColQuery.replace("$redeeming_criterion_id", id)
				.replace("$redeemable_id", dataSet.get("redeemable_id"));
		int updateResult = DBUtils.executeUpdateQuery(env, updateRedeemableCriteriaColVal);
		Assert.assertEquals(updateResult, 1, "Failed to update redeeming_criterion_id");
		utils.logPass("Redeemable successfully mapped to new QC");

		// SignUp user > Send reward amount to user
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch for user");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "10", "",
				"", "");
		Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send Message to User API response status code mismatch");
		utils.logit("Reward amount 10 is sent to signed-up user ID " + userID);

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
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "10", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API > Verify response and no DB entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "10",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "discount_amount", false, env);
		utils.logPass("Verified Auto-Select API for qualification of receipt when only D item is provided and Discount type = Discount Amount");

	}

	@Test(description = "SQ-T7148: Verify Auto-Select API when the D item amount and M item amount are the same.", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7148_verifyAutoSelectApiWithDAndMHasSameItemAmt() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		
		// Create LIS
		lisExternalId1=createLIS("T7148",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		
		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7148_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7148", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);
		
		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "10", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "", false, env);
		utils.logPass("Verified the Auto-Select API when the D item amount and M item amount are the same.");
	}
	
	@Test(description = "SQ-T7149: Verify the qualification of M & D items for auto-select API with LIQ-> Net Amount Excluding Min Priced Item Equal To Or More Than (Discount type= Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7149_verifyAutoSelectApiWithDAndMItemWithLIQWhenMinPricedItemEqualToOrMore() throws Exception {
		
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7149",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		
		// Create QC (Processing Function = bundle_price_target_advanced)
		String qcName = "Automation_QC_SQ_T7149_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(1.0)
				.addLineItemFilter(lisExternalId1, "", 1)
				.addItemQualifier("net_amount_excluding_min_priced_item_equal_to_or_more_than", lisExternalId1, 5.0, 1)
				.build();
		
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1,"Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7149", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "1.0", "101" });
		addDetails.accept("item_2", new String[] { "item_2", "1", "15", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "5", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "35",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", true, env);
		utils.logPass("Verified the qualification of M & D items for auto-select API with LIQ-> Net Amount Excluding Min Priced Item Equal To Or More Than (Discount type= Reward");
	}
	
	@Test(description = "SQ-T7160: Base & Modifiers | Verify the qualification of Two M & Single D items for auto-select API with Line Item exists (Discount type= Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7160_verifyAutoSelectApiWithQualificationOfTwoMAndSingleD() throws Exception {
	
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		
		// LIS 1 qualifies Base item (101) with Modifier (111)
		lisExternalId1=createLIS("T7160_1_",LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS,"101","111",dataSet.get("apiKey"));
		
		// LIS 2 qualifies Base item (201) with Modifier (211)
		lisExternalId2=createLIS("T7160_2_",LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS,"201","211",dataSet.get("apiKey"));
		

		//Create Qualification Criteria (QC)
		// QC uses sum_amounts as processing function and validates:
		//  - Two M items via LIS
		//  - Line item existence condition
		String qcName = "Automation_QC_SQ_T7160_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 1)
				.addLineItemFilter(lisExternalId2, "", 1)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1, "Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Redeemable
		String redeemableId = createRedeemable("SQ_T7160", qcExternalId1);

		// SignUp user
		String userID = signUpUserAndSendRedeemable(redeemableId);

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		// Two Base (M) items
		addDetails.accept("Base_Item_1", new String[] { "Base_Item_1", "1", "11", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Base_Item_2",new String[] { "Base_Item_2", "1", "10", "M", "11", "23", "2.0", "201" });
		// Two Modifier (M) items
		addDetails.accept("Modifier_Item_1",new String[] { "Modifier_Item_1", "1", "10", "M", "11", "23", "1.1", "111" });
		addDetails.accept("Modifier_Item_2",new String[] { "Modifier_Item_2", "1", "10", "M", "11", "23", "2.1", "211" });
		// One Discount (D) item
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "2", "D", "11", "23", "2.0", "421" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "reward", true, env);
		utils.logPass("Verified the qualification of Two M & Single D items for auto-select API with Line Item exists (Discount type= Reward)");
	}
	
	
	@Test(description = "SQ-T7158: Verify the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type= Fuel Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7158_verifyAutoSelectApiWithQualificationOfSingleMAndDDiscountTypeFuelReward() throws Exception {
	
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7158",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7158_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1, "Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");
		
		
		//fetching existing value for column redeeming_criterion_id from DB for redeemable_id = BASE Redeemable id
		utils.logit("Fetching existing redeeming_criterion_id for base redeemable");
		String existingRedeemingCriterionId = OfferIngestionUtilities.currentRedeemableCriteriaIdQuery.replace("$redeemable_id", dataSet.get("redeemable_id"));
		existing_redeeming_criterion_id = DBUtils.executeQueryAndGetColumnValue(env, existingRedeemingCriterionId, "redeeming_criterion_id");
		utils.logit("Existing Redeemable Criterion ID from DB for Base Redeemable is :: '" + existing_redeeming_criterion_id + "");
		
		
		//Getting Id value from DB for Created QC's external ID
		String idFromQCTableQuery = OfferIngestionUtilities.idFromQCQuery.replace("$external_id", qcExternalId1);
		String id = DBUtils.executeQueryAndGetColumnValue(env, idFromQCTableQuery, "id");
		utils.logit("Created QC Id from DB:: '" + id + "");
		
		//Updating redeeming_criterion_id = id fetched from above from DB for Base Redeemable
		String updateRedeemableCriteriaColVal = OfferIngestionUtilities.updateRedeemableCriteriaColQuery.replace("$redeeming_criterion_id", id)
				.replace("$redeemable_id", dataSet.get("redeemable_id"));
		int updateResult = DBUtils.executeUpdateQuery(env, updateRedeemableCriteriaColVal);
		Assert.assertEquals(updateResult, 1, "Failed to update redeeming_criterion_id");
		utils.logPass("Redeemable successfully mapped to new QC");

		// SignUp user
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch for user");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("User signup successful. User ID: " + userID);

		utils.logit("Sending Fuel Reward discount to user");
		Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),"", "", "10", "");
		Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,"Send Message to User API response status code mismatch");
		utils.logPass("Fuel Reward discount sent successfully");

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("item_1", new String[] { "item_1", "1", "10", "M", "11", "23", "4.0", "101" });
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "5", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationkey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		List<Map<String, Object>> discountBasketItems = autoSelectResponse.jsonPath().getList("discount_basket_items");

		Assert.assertNotNull(discountBasketItems, "discount_basket_items is null");
		Assert.assertFalse(discountBasketItems.isEmpty(), "discount_basket_items is empty");

		Map<String, Object> discountItem = discountBasketItems.get(0);
		utils.logit("Validating discount basket item response");
		Assert.assertNotNull(discountItem.get("discount_basket_item_id"));
		Assert.assertEquals(discountItem.get("discount_type"), "fuel_reward");
		Assert.assertNull(discountItem.get("discount_id"));
		Assert.assertNull(discountItem.get("discount_value"));
		Assert.assertNull(discountItem.get("discount_details"));
		Assert.assertNotNull(discountItem.get("created_at"));
		
		//DB Validation
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "fuel_reward", true, env);
		utils.logPass("Verified the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type= Fuel Reward)");
	
	}
	@Test(description = "SQ-T7159: Verify the qualification of Receipt when only D item is provided for auto-select API (Discount type= Fuel Reward)", groups = "regression")
	@Owner(name = "Apurva Agarwal")
	public void T7159_verifyAutoSelectApiWithQualificationOfOnlyDItemDiscountTypeFuelReward() throws Exception {
	
		// Enable Decoupled Redemption Engine flag for the business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true,env, businessID);
		// Create LIS
		lisExternalId1=createLIS("T7159",LineItemSelectorFilterItemSet.BASE_ONLY,"101","",dataSet.get("apiKey"));
		

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ_T7159_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalId1, "", 0).addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage, "Expected success to be true, but it was false in the response");
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1, "Expected results[0].external_id to be present, but it was null in the response");
		utils.logit("QC '" + qcName + "' has been created successfully");
		
		
		//fetching existing value for column redeeming_criterion_id from DB for redeemable_id = BASE Redeemable id
		utils.logit("Fetching existing redeeming_criterion_id for base redeemable");
		String existingRedeemingCriterionId = OfferIngestionUtilities.currentRedeemableCriteriaIdQuery.replace("$redeemable_id", dataSet.get("redeemable_id"));
		existing_redeeming_criterion_id = DBUtils.executeQueryAndGetColumnValue(env, existingRedeemingCriterionId, "redeeming_criterion_id");
		utils.logit("Existing Redeemable Criterion ID from DB for Base Redeemable is :: '" + existing_redeeming_criterion_id + "");
		
		
		//Getting Id value from DB for Created QC's external ID
		String idFromQCTableQuery = OfferIngestionUtilities.idFromQCQuery.replace("$external_id", qcExternalId1);
		String id = DBUtils.executeQueryAndGetColumnValue(env, idFromQCTableQuery, "id");
		utils.logit("Created QC Id from DB:: '" + id + "");
		
		//Updating redeeming_criterion_id = id fetched from above from DB for Base Redeemable
		String updateRedeemableCriteriaColVal = OfferIngestionUtilities.updateRedeemableCriteriaColQuery.replace("$redeeming_criterion_id", id)
				.replace("$redeemable_id", dataSet.get("redeemable_id"));
		int updateResult = DBUtils.executeUpdateQuery(env, updateRedeemableCriteriaColVal);
		Assert.assertEquals(updateResult, 1, "Failed to update redeeming_criterion_id");
		utils.logPass("Redeemable successfully mapped to new QC");

		// SignUp user
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 SignUp API response status code mismatch for user");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.logPass("User signup successful. User ID: " + userID);

		utils.logit("Sending Fuel Reward discount to user");
		Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),"", "", "10", "");
		Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,"Send Message to User API response status code mismatch");
		utils.logPass("Fuel Reward discount sent successfully");

		// Create Input Receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		// Input Receipt Details in below format:
		// item_name|item_qty|amount|item_type|item_family|item_group|serial_number|item_id
		addDetails.accept("Discounted_Item",new String[] { "Discounted_Item", "1", "10", "D", "11", "23", "5.0", "101" });

		// Hit POS Auto Select API
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationkey"), "25",externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"POS Auto Select API response status code mismatch");
		
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "", false, env);
		utils.logPass("Verify the qualification of Receipt when only D item is provided for auto-select API (Discount type= Fuel Reward)");
	
	}
	
	
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalId1, qcExternalId1, redeemableExternalID1);
		// Restore original flag value
		if (originalDecoupledRedemptionFlag != null) {
			utils.logit("Restoring 'enable_decoupled_redemption_engine' to original value: "
					+ originalDecoupledRedemptionFlag);
			flagObj.updateDecoupledRedemptionEngineFlag(originalDecoupledRedemptionFlag, env, businessID);
		}
		if (existing_redeeming_criterion_id != null && !existing_redeeming_criterion_id.isEmpty()) {
			//Restoring Original Redeemable Configuration
			utils.logit("Restoring redeemable configuration :: redeemable_id = "+ dataSet.get("redeemable_id")+ ", redeeming_criterion_id = "
					+ existing_redeeming_criterion_id);
			String updateRedeemableCriteriaColVal = OfferIngestionUtilities.updateRedeemableCriteriaColQuery
					.replace("$redeeming_criterion_id", existing_redeeming_criterion_id)
					.replace("$redeemable_id", dataSet.get("redeemable_id"));

			DBUtils.executeUpdateQuery(env, updateRedeemableCriteriaColVal);
			utils.logPass("Redeemable configuration restored successfully");
		}
		pageObj.utils().clearDataSet(dataSet);
	}

}