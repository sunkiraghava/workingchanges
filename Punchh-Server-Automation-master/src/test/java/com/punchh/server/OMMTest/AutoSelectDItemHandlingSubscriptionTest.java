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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * As discussed with Hitesh, these cases needs to be run on enable_decoupled_redemption_engine: true only.
 * We are updating the flag status to true and then reverting it back to original status in the AfterMethod.
 */

@Listeners(TestListeners.class)
public class AutoSelectDItemHandlingSubscriptionTest {
	private static Logger logger = LogManager.getLogger(AutoSelectDItemHandlingSubscriptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String businessID, planID, lisExternalId1, lisExternalId2, qcExternalId1, redeemableExternalID1;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private OfferIngestionUtilities offerUtils;
	private StackingReusabilityDiscountDistributionTest flagObj;
	public Boolean originalDecoupledRedemptionFlag;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
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
		offerUtils = new OfferIngestionUtilities(driver);
		flagObj = new StackingReusabilityDiscountDistributionTest();
		planID = null;
		lisExternalId2 = null;
		originalDecoupledRedemptionFlag = null;
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T7153: Verify the qualification of Single M & Single D items for auto-select API with Line Item exists (Discount type= Subscription)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7153_verifyAutoSelectWithSubscriptionSingleMAndD() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);
		// Create LIS
		String lisName = "Automation_LIS_SQ-T7153_" + CreateDateTime.getTimeDateString();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage);
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		utils.logit("LIS '" + lisName + "' has been created successfully");

		// Create QC (Processing Function = rate_rollback)
		String qcName = "Automation_QC_SQ-T7153_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(1.0).addLineItemFilter(lisExternalId1, "", 0)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Subscription Plan > SignUp user > Purchase subscription
		String userID = userSignUpAndPurchaseSubscription("SQ-T7153", dataSet, qcName, "Rate Rollback");

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

		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "20",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "subscription", true, env);
		utils.logPass("Verified Auto-Select API for qualification of Single M & Single D items");

	}

	@Test(description = "SQ-T7154: Verify the qualification of Receipt when only D item is provided for auto-select API (Discount type= Subscription)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7154_verifyAutoSelectWithSubscriptionOnlyD() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);
		// Create LIS
		String lisName = "Automation_LIS_SQ-T7154_" + CreateDateTime.getTimeDateString();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		Response createLisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage = createLisResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage);
		lisExternalId1 = createLisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		utils.logit("LIS '" + lisName + "' has been created successfully");

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ-T7154_" + CreateDateTime.getTimeDateString();
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

		// Create Subscription Plan > SignUp user > Purchase subscription
		String userID = userSignUpAndPurchaseSubscription("SQ-T7154", dataSet, qcName,
				"% or $ Off(Receipt Level Discount)");

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
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "20",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "subscription", false, env);
		utils.logPass("Verified Auto-Select API for qualification of receipt when only D item is provided and Discount type = Subscription");
	}

	@Test(description = "SQ-T7163: Base & Modifiers | Verify the qualification of Two M & Single D items for auto-select API with Line Item exists (Discount type= Subscription)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7163_verifyAutoSelectWithSubscriptionBaseAndModifiers() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);
		// Create LIS #1 (101 Base And 111 Modifier)
		String lisName1 = "Automation_LIS_101_111_SQ-T7163_" + CreateDateTime.getTimeDateString();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "101").addModifierClause("item_id", "in", "111").build();
		Response createLisResponse1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage1 = createLisResponse1.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage1);
		lisExternalId1 = createLisResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create LIS #2 (201 Base And 211 Modifier)
		String lisName2 = "Automation_LIS_201_211_SQ-T7163_" + CreateDateTime.getTimeDateString();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "in", "201").addModifierClause("item_id", "in", "211").build();
		Response createLisResponse2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage2 = createLisResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2);
		lisExternalId2 = createLisResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId2);
		utils.logit("LIS '" + lisName2 + "' has been created successfully");

		// Create QC (Processing Function = sum_amounts)
		String qcName = "Automation_QC_SQ-T7163_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalId2, "", 0)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Subscription Plan > SignUp user > Purchase subscription
		String userID = userSignUpAndPurchaseSubscription("SQ-T7163", dataSet, qcName,
				"% or $ Off(Receipt Level Discount)");

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
		addDetails.accept("Base_Item_1", new String[] { "Base_Item_1", "1", "11", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Base_Item_2", new String[] { "Base_Item_2", "1", "10", "M", "11", "23", "2.0", "201" });
		addDetails.accept("Modifier_Item_1",
				new String[] { "Modifier_Item_1", "1", "10", "M", "11", "23", "1.1", "111" });
		addDetails.accept("Modifier_Item_2",
				new String[] { "Modifier_Item_2", "1", "10", "M", "11", "23", "2.1", "211" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "2", "D", "11", "23", "2.0", "421" });

		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "50",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "subscription", true, env);
		utils.logPass("Verified Auto-Select API for qualification of Two M & Single D items with Base & Modifiers");

	}

	@Test(description = "SQ-T7162: Only Modifiers | Verify the qualification of Two M & Single D items for auto-select API with Line Item exists (Discount type= Subscription)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7162_verifyAutoSelectWithSubscriptionOnlyModifiers() throws Exception {
		// Business type: PTC
		// Enable Decoupled Redemption Engine flag for business if not already enabled
		originalDecoupledRedemptionFlag = flagObj.updateDecoupledRedemptionEngineFlag(true, env, businessID);
		// Create LIS #1 (101 Base And 111 Modifier)
		String lisName1 = "Automation_LIS_101_111_SQ-T7162_" + CreateDateTime.getTimeDateString();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "101").addModifierClause("item_id", "in", "111").build();
		Response createLisResponse1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage1 = createLisResponse1.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage1);
		lisExternalId1 = createLisResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId1);
		utils.logit("LIS '" + lisName1 + "' has been created successfully");

		// Create LIS #2 (201 Base And 211 Modifier)
		String lisName2 = "Automation_LIS_201_211_SQ-T7162_" + CreateDateTime.getTimeDateString();
		String lisPayload2 = apipayloadObj.lineItemSelectorBuilder().setName(lisName2)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "201").addModifierClause("item_id", "in", "211").build();
		Response createLisResponse2 = pageObj.endpoints().createLIS(lisPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(createLisResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create LIS API response status code mismatch");
		boolean actualSuccessMessage2 = createLisResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2);
		lisExternalId2 = createLisResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalId2);
		utils.logit("LIS '" + lisName2 + "' has been created successfully");

		// Create QC (Processing Function = bundle_price_target_advanced)
		String qcName = "Automation_QC_SQ-T7162_" + CreateDateTime.getTimeDateString();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(1.0)
				.addLineItemFilter(lisExternalId1, "", 1).addLineItemFilter(lisExternalId2, "", 1)
				.addItemQualifier("line_item_exists", lisExternalId1, 0.0)
				.addItemQualifier("line_item_exists", lisExternalId2, 0.0).build();
		Response createQcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(createQcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Create QC API response status code mismatch");
		boolean qcSuccessMessage = createQcResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(qcSuccessMessage);
		qcExternalId1 = createQcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalId1);
		utils.logit("QC '" + qcName + "' has been created successfully");

		// Create Subscription Plan > SignUp user > Purchase subscription
		String userID = userSignUpAndPurchaseSubscription("SQ-T7162", dataSet, qcName,
				"Target Price For A Bundle (Advanced)");

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
		addDetails.accept("Base_Item_1", new String[] { "Base_Item_1", "1", "11", "M", "11", "23", "1.0", "101" });
		addDetails.accept("Base_Item_2", new String[] { "Base_Item_2", "1", "10", "M", "11", "23", "2.0", "201" });
		addDetails.accept("Modifier_Item_1",
				new String[] { "Modifier_Item_1", "1", "10", "M", "11", "23", "1.1", "111" });
		addDetails.accept("Modifier_Item_2",
				new String[] { "Modifier_Item_2", "1", "10", "M", "11", "23", "2.1", "211" });
		addDetails.accept("Discounted_Item",
				new String[] { "Discounted_Item", "1", "2", "D", "11", "23", "2.0", "421" });

		// Hit POS Auto Select API > Verify response and DB should have entry
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response autoSelectResponse = pageObj.endpoints().posAutoSelectAPI(userID, dataSet.get("locationKey"), "100",
				externalUID, parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Auto Select API response status code mismatch");
		offerUtils.verifyDiscountBasketItemDB(autoSelectResponse, userID, "subscription", true, env);
		utils.logPass("Verified Auto-Select API for qualification of Two M & Single D items with Only Modifiers");

	}

	// Helper methods:

	// Create Subscription Plan > SignUp user > Purchase subscription
	public String userSignUpAndPurchaseSubscription(String testCaseId, Map<String, String> dataSet, String qcName,
			String planDiscountType) throws Exception {
		// Create Subscription Plan
		String spName = "Automation_SubcriptionPlan_" + testCaseId + "_" + CreateDateTime.getTimeDateString();
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, qcName, planDiscountType, false,
				endDateTime, false);
		planID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);
		// SignUp User
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Mobile API2 Signup API response status code mismatch");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		// Purchase Subscription for user
		String purchasePrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		Response purchaseSubscriptionResponse = pageObj.endpoints().dashboardSubscriptionPurchase(dataSet.get("apiKey"),
				planID, dataSet.get("client"), dataSet.get("secret"), purchasePrice, endDateTime, "false", userID);
		Assert.assertEquals(purchaseSubscriptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Dashboard Subscription Purchase API response status code mismatch");
		String subscriptionId = purchaseSubscriptionResponse.jsonPath().getString("subscription_id");
		Assert.assertNotNull(subscriptionId, "Subscription ID is null in Subscription Purchase API response");
		utils.logPass("Subscription [plan ID: " + planID + "] is purchased for user [user ID: " + userID + "] successfully");

		return userID;
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Subscription Plan
		utils.deleteLISQCRedeemable(env, lisExternalId1, qcExternalId1, redeemableExternalID1);
		utils.deleteLISQCRedeemable(env, lisExternalId2, null, null);
		utils.deleteSubscriptionPlan(env, planID, businessID);
		// Restore original value for Decoupled Redemption Engine flag
		if (originalDecoupledRedemptionFlag != null) {
			utils.logit("Restoring 'enable_decoupled_redemption_engine' to original value: "
					+ originalDecoupledRedemptionFlag);
			flagObj.updateDecoupledRedemptionEngineFlag(originalDecoupledRedemptionFlag, env, businessID);
		}
		pageObj.utils().clearDataSet(dataSet);
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}