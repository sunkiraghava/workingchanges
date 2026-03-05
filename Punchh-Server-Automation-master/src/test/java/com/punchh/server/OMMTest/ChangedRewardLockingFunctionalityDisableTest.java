package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ChangedRewardLockingFunctionalityDisableTest {
	private static Logger logger = LogManager.getLogger(ChangedRewardLockingFunctionalityDisableTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	String query, expColValue = "";
	String externalUID;
	Properties prop;
	private ApiPayloadObj apipayloadObj;
	private Utilities utils;
	private String lisExternalID, qcExternalID, redeemableExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		apipayloadObj = new ApiPayloadObj();
		utils = new Utilities();
		lisExternalID = null;
		qcExternalID = null;
		redeemableExternalID = null;
	}

	@Test(description = "SQ-T5931 Verify lock should not be applied on basket when send external_uid in API request before enablement of Reward Locking feature.", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5931_verifyLockingFunctionalityPOS() throws Exception {

		String lisName = "Automation_LIS_SQ_T5931_POS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_SQ_T5931_POS_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_SQ_T5931_POS_" + Utilities.getTimestamp();

		// Create LIS with base item 12003
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "12003")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "Redeemable ID from DB is null");
		dataSet.put("redeemable_id", dbRedeemableId);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 user signup");
		utils.logPass("API2 Signup is successful with user ID: " + userID);

		// send reward amount to user with Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, query,
				new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS Api Adding Reward added Items in basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// POS remove discount from basket
		String expdiscount_basket_item_id1 = discountBasketResponse1.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse5 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id1, externalUID);
		Assert.assertEquals(deleteBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// POS discount lookup api
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap1);
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(dataSet.get("locationkey"), userID, "10",
						externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String discountAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.get("unselected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS discount lookup api is successful, Discount calculation should be shown in api response but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// POS batch redemption With Query Param true
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationkey"), userID, "10",
						"true", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS batch redemption With Query Param true is successful, Discount calculation should be shown in api response but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("locationkey"), userID, "10",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		discountAmount = batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS batch redemption With Query Param false is successful, Redemption should be done successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = signUpResponse1.jsonPath().get("access_token").toString();
		userID = signUpResponse1.jsonPath().get("id").toString();
		utils.logPass("API1 user Signup is successful with user ID: " + userID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that POS Auto Unlock is successful, Items should be added to basket automatically and discount should be shown in api response but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

	}

	@Test(description = "SQ-T5931 Verify lock should not be applied on basket when send external_uid in API request before enablement of Reward Locking feature.", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5931_verifyLockingFunctionalityAUTH() throws Exception {
		String lisName = "Automation_LIS_SQ_T5931_Auth_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_SQ_T5931_Auth_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_SQ_T5931_Auth_" + Utilities.getTimestamp();

		// Create LIS with base item 12003
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "12003")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID from DB is null");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("API1 user Signup is successful with user ID: " + userID);

		// send reward amount to user with Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, query,
				new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that AUTH Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Auth remove discount from basket
		String discount_basket_item_id = discountBasketResponse.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Auth Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		// AUTH batch redemption With Query Param true
		Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionAUTHAPIWithProcessValue(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
				externalUID, "true", "10");
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "10.0");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that AUTH batch redemption With Query Param true is successful, Discount calculation should be shown in api response but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry");

		// AUTH batch redemption With Query Param false
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionAUTHAPIWithProcessValue(dataSet.get("client"), dataSet.get("secret"),
						dataSet.get("locationkey"), token, userID, "12003", externalUID, "false", "10");
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		discountAmount = batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that AUTH batch redemption With Query Param false is successful, Redemption should be done successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = signUpResponse1.jsonPath().get("access_token").toString();
		userID = signUpResponse1.jsonPath().get("id").toString();
		utils.logPass("API1 user Signup is successful with user ID: " + userID);

		// send reward amount to user with Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Auto Unlock
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap1);
		Response autoUnlockResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "10", externalUID, parentMap);
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that AUTH Auto Unlock is successful, Items should be added to basket automatically and discount should be shown in api response but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
	}

	@Test(description = "SQ-T5931 Verify lock should not be applied on basket when send external_uid in API request before enablement of Reward Locking feature.", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5931_verifyLockingFunctionalitySecureAndMobile() throws Exception {
		String lisName = "Automation_LIS_SQ_T5931_Mobile_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_SQ_T5931_Mobile_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_SQ_T5931_Mobile_" + Utilities.getTimestamp();

		// Create LIS with base item 12003
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "12003")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID from DB is null");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().updateButton();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("API1 user Signup is successful with user ID: " + userID);

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// Secure Api Adding Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().secureApiDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, query,
				new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Secure Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Secure Api remove discount basket
		String discount_basket_item_id = discountBasketResponse.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse = pageObj.endpoints().removeDiscountBasketExtUIDSecureAPI(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Secure Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketWithExtIdAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Mobile Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// API2 remove discount from basket
		String discount_basket_item_id1 = discountBasketResponse1.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse1 = pageObj.endpoints().deleteDiscountToBasketWithExtUidAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id1, externalUID);
		Assert.assertEquals(deleteBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Mobile Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T5932 Verify lock should be applied on basket when send external_uid in API request after enablement of Reward Locking feature.", groups = { "regression", "dailyrun" }, priority = 1)
	public void T5932_verifyLockingFunctionalityPOS() throws Exception {
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");
		utils.logPass(
				"Verified that POS Api Add Reward Items in basket is successfully, Discount basket should be locked and items should be added in the basket successfully. external_uid and locked_at should be updated in DB discount_baskets table for the respective user entry.");

		// POS remove discount from basket
		String expdiscount_basket_item_id1 = discountBasketResponse1.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse5 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("location_key"),
				userID, expdiscount_basket_item_id1, externalUID);
		Assert.assertEquals(deleteBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that POS Api remove Reward from basket is successful, Discount basket should be locked and items should be removed from the basket successfully. external_uid and locked_at should be updated in DB discount_baskets table for the respective user entry.");

		// POS discount lookup api
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap1);
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(dataSet.get("location_key"), userID, "10",
						externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String discountAmount = batchRedemptionProcessResponseUser1.jsonPath()
				.get("unselected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");
		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that POS discount lookup api is successful, Discount basket should be locked and discount amount should be shown in api response. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// POS batch redemption With Query Param true
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("location_key"), userID, "10",
						"true", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");
		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that POS batch redemption With Query Param true is successful, Discount basket should be locked and discount amount should be shown in api response without actual redemption. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");

		// POS batch redemption With Query Param false
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPIWithExtUid(dataSet.get("location_key"), userID, "10",
						"false", externalUID, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		discountAmount = batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");
		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that POS batch redemption With Query Param false is successful, Discount basket should be locked and redemptions should be applied successfully on basket items. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = signUpResponse1.jsonPath().get("access_token").toString();
		userID = signUpResponse1.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("location_key"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that POS Auto Unlock is successful, Discount basket should be locked and discount should be applied on items added to the basket automatically. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");
	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T5932 Verify lock should be applied on basket when send external_uid in API request after enablement of Reward Locking feature.", groups = { "regression", "dailyrun" }, priority = 1)
	public void T5932_verifyLockingFunctionalityAUTH() throws Exception {
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");
		utils.logPass(
				"Verified that AUTH Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Auth remove discount from basket
		String discount_basket_item_id = discountBasketResponse.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that Auth Api remove Reward from basket is successful, Discount basket should be locked and items should be removed from the basket successfully. external_uid and locked_at should be updated in DB discount_baskets table for the respective user entry.");

		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(200, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// AUTH batch redemption With Query Param true
		Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionAUTHAPIWithProcessValue(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_key"), token, userID, "12003",
				externalUID, "true", "10");
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "10.0");
		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that AUTH batch redemption With Query Param true is successful, Discount basket should be locked and discount amount should be shown in api response without actual redemption. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");

		// AUTH batch redemption With Query Param false
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionAUTHAPIWithProcessValue(dataSet.get("client"), dataSet.get("secret"),
						dataSet.get("location_key"), token, userID, "12003", externalUID, "false", "10");
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");

		discountAmount = batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(discountAmount, "10.0");
		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that AUTH batch redemption With Query Param false is successful, Discount basket should be locked and redemptions should be applied successfully on basket items. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = signUpResponse1.jsonPath().get("access_token").toString();
		userID = signUpResponse1.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Auto Unlock
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "10", "M", "2", "", "1.0",
				dataSet.get("item_id"));
		parentMap.put("Sandwich", detailsMap1);
		Response autoUnlockResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "10", externalUID, parentMap);
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
		Assert.assertNotEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");

		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
		Assert.assertNotEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");

		utils.logPass(
				"Verified that AUTH Auto Unlock is successful, Discount basket should be locked and discount should be applied on items added to the basket automatically. external_uid and locked_at should also be updated in DB discount_baskets table for the respective user entry.");
	}

	// this is an issue which caught during automation of ticket SQ-T5932,
	// https://punchhdev.atlassian.net/browse/OMM-1289
	@Test(description = "SQ-T5932 Verify lock should be applied on basket when send external_uid in API request after enablement of Reward Locking feature.", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5932_verifyLockingFunctionalitySecureAndMobile() throws Exception {
		// Create LIS Payload with base item 12003
		String lisName = "Automation_LIS_SQ_T5932_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "12003")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC payload
		String qcname = "Automation_QC_SQ_T5932_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setStackDiscounting(false)
				.setReuseQualifyingItems(false).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, 0.0, 0)
				.build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable Payload with above QC
		String redeemableName = "Automation_Redeemable_SQ_T5932_" + Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setDescription(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is null");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("User signed up successfully with userID: " + userID);

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// Secure Api Adding Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().secureApiDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		query = OfferIngestionUtilities.getExtUidLockedAtDiscountBasketsQuery.replace("$user_id", userID);
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, query,
				new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Secure Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
//
//		// Secure Api remove discount basket
//		String discount_basket_item_id = discountBasketResponse.jsonPath()
//				.get("discount_basket_items[0].discount_basket_item_id").toString();
//		Response deleteBasketResponse = pageObj.endpoints().removeDiscountBasketExtUIDSecureAPI(token,
//				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
//		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not match with remove discount from basket ");
//
//		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
//		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
//		Assert.assertEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");
//
//		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
//		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
//		Assert.assertEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");
//
//		TestListeners.extentTest.get().pass(
//				"Verified that Secure Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
//		logger.info(
//				"Verified that Secure Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = signUpResponse1.jsonPath().get("access_token").toString();
		userID = signUpResponse1.jsonPath().get("id").toString();
		utils.logPass("User signed up successfully with userID: " + userID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketWithExtIdAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");

		values = DBUtils.executeQueryAndGetMultipleColumns(env, query, new String[] { "external_uid", "locked_at" });
		Assert.assertEquals(values.get(0).get("external_uid"), null,
				"Value is not present at external_uid column in discount basket");
		Assert.assertEquals(values.get(0).get("locked_at"), null,
				"Value is not present at locked_at column in discount basket");
		utils.logPass(
				"Verified that Mobile Api Adding Reward added Items should be added to basket successfully but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");

//		// API2 remove discount from basket
//		String discount_basket_item_id1 = discountBasketResponse1.jsonPath()
//				.get("discount_basket_items[0].discount_basket_item_id").toString();
//		Response deleteBasketResponse1 = pageObj.endpoints().deleteDiscountToBasketWithExtUidAPI2(token,
//				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id1, externalUID);
//		Assert.assertEquals(deleteBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not match with remove discount from basket ");
//
//		query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
//		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
//		Assert.assertEquals(expColValue, null, "Value is not present at external_uid column in discount basket ");
//
//		query = "Select locked_at from discount_baskets where user_id ='" + userID + "'";
//		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "locked_at");
//		Assert.assertEquals(expColValue, null, "Value is not present at locked_at column in discount basket ");
//
//		TestListeners.extentTest.get().pass(
//				"Verified that Mobile Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
//		logger.info(
//				"Verified that Mobile Api remove Reward from basket is successful but external_uid and locked_at should not be updated in DB discount_baskets table for the respective user entry.");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}