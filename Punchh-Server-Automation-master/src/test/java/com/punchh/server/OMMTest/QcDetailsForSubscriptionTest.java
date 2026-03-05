package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class QcDetailsForSubscriptionTest {
	static Logger logger = LogManager.getLogger(QcDetailsForSubscriptionTest.class);
	public WebDriver driver;
	private String userEmail;
	Utilities utils;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private String endDateTime;
	private static Map<String, String> dataSet;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, qcExternalID2, redeemableExternalID, planID, businessID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
		businessID = dataSet.get("business_id");
		lisExternalID = null;
		qcExternalID = null;
		qcExternalID2 = null;
		redeemableExternalID = null;
		planID = null;
	}

	// Rakhi
	@Test(description = "SQ-T6921 Verify QC(Processing function->Rate rollback)  details in failure of POS redemption's response in case of subscription", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T6921_verifyQcDetailsForSubscription() throws Exception {

		String spName = "Automation_SubscriptionPlan_SQ_T6921_" + CreateDateTime.getTimeDateString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 User signup");
		utils.logPass("API2 User Signup is successful");

		String lisName = "Automation_LIS_SQ_T6921_" + CreateDateTime.getTimeDateString();
		String qcName = "Automation_QC_SQ_T6921_" + CreateDateTime.getTimeDateString();

		// Create LIS with base item 777
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "777")
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
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcName)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(1.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty");
		utils.logPass(qcName + " QC is created with External ID: " + qcExternalID);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "check");

		// create subscription plan with above QC
		String spPrice = "100";
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, qcName, dataSet.get("qcFunctionName"),
				false, endDateTime, false);
		planID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);
		Assert.assertNotNull(planID, "Subscription Plan ID is null");

		// gift Subscription Plan to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Subscription", spName,
				dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		utils.logPass("Verified that Success message of subscription send to user ");

		// click message gift and gift orders visits
		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");
		utils.logInfo("Gift orders visits is successful");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, planID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		Assert.assertNotNull(subscription_id, "Subscription ID is null in purchase subscription response");
		utils.logInfo(userEmail + " purchased " + subscription_id + " Plan id = " + planID);

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date, subscription_id, key, txn,
				dataSet.get("locationkey"), spPrice, "1010");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.asString().contains("Redemption not possible since amount is 0."),
				"Response did not matched");
		Assert.assertEquals(resp.jsonPath().getString("qualifying_conditions.name"), qcName,
				"QC name did not matched in POS redemption response");
		Assert.assertEquals(
				resp.jsonPath().getString("qualifying_conditions.line_item_filters[0].line_item_selector.name"),
				lisName, "LIS name did not matched in POS redemption response");
		utils.logPass("QC and LIS details are verified in POS redemption response");

		// disable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "uncheck");
		// clean up is done in @AfterMethod
	}

	// Rakhi
	@Test(description = "SQ-T6925 Verify QC(Processing function->Rate rollback)  details in failure of auth redemption's response in case of subscription. "
			+ "SQ-T6927 Verify QC(Processing function->Sum of Amounts Incremental)  details in failure of POS redemption's response in case of redemption code", groups = {
					"regression" })
	@Owner(name = "Rakhi Rawat")
	public void T6925_verifyQcDetailsWithModifiersOnly() throws Exception {

		String spName = "Automation_SubscriptionPlan_SQ_T6925_" + CreateDateTime.getTimeDateString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth API User Sign-up call is successful with user ID: " + userID);

		String lisName = "Automation_LIS_SQ_T6925_" + CreateDateTime.getTimeDateString();
		String qcName1 = "Automation_QC1_SQ_T6925_" + Utilities.getTimestamp();
		String qcName2 = "Automation_QC2_SQ_T6925_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_SQ_T6925_" + Utilities.getTimestamp();

		// Create LIS with base item 777
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "in", "777").addModifierClause("item_id", "in", "777")
				.setMaxDiscountUnits(1).setProcessingMethod("max_price").build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create First QC-payload with Processing Function rate rollback
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcName1)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(1.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC1
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC1 External ID
		qcExternalID = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC1 External ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC1 External ID is empty");
		utils.logPass(qcName1 + " QC1 is created with External ID: " + qcExternalID);

		// Create second QC-payload with Processing Function sum_amounts_incremental
		String qcPayload2 = apipayloadObj.qualificationCriteriaBuilder().setName(qcName2)
				.setQCProcessingFunction("sum_amounts_incremental").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC2
		Response qcResponse2 = pageObj.endpoints().createQC(qcPayload2, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC2 External ID
		qcExternalID2 = qcResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID2, "QC2 External ID is null");
		Assert.assertFalse(qcExternalID2.isEmpty(), "QC2 External ID is empty");
		utils.logPass(qcName2 + " QC2 is created with External ID: " + qcExternalID2);

		// Create Redeemable with QC2
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID2).build())
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
		Assert.assertNotNull(dbRedeemableId, "Redeemable ID is null in DB");

		// gift Redeemable to user
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 Send Redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Redeemable not received to user");
		Assert.assertFalse(rewardId.isEmpty(), "Redeemable ID is empty in response");
		utils.logPass("Reward id " + rewardId + " is generated successfully");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "check");

		// login user in IFrame
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail, Utilities.getApiConfigProperty("password"));

		// generate redemption code via IFrame
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer(redeemableName + " (Never Expires)");
		Assert.assertNotNull(redemptionCode, "Redemption code is not generated");

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemptionCode, key, txn,
				dataSet.get("locationkey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");

		Assert.assertTrue(resp.asString().contains("Discount qualification on receipt failed"),
				"Response did not matched");
		Assert.assertEquals(resp.jsonPath().getString("qualifying_conditions.name"), qcName2,
				"QC name did not matched in POS redemption response");
		utils.logPass("QC and LIS details are verified in POS redemption response");

		// SQ-T6925 Part
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// create subscription plan with above QC
		String spPrice = "100";
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");
		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, qcName1, dataSet.get("qcFunctionName"),
				false, endDateTime, false);
		planID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);
		Assert.assertNotNull(planID, "Subscription Plan ID is null");

		// gift Subscription Plan to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Subscription", spName,
				dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		utils.logPass("Verified that Success message of subscription send to user ");

		// click message gift and gift orders visits
		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");
		utils.logInfo("Gift orders visits is successful");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, planID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		Assert.assertNotNull(subscription_id, "Subscription ID is null in purchase subscription response");
		utils.logInfo(userEmail + " purchased " + subscription_id + " with Plan ID = " + planID);

		// Create Online Subscription Redemption
		utils.logInfo("== Auth API Create Online Redemption ==");
		Response resp1 = pageObj.endpoints().authOnlineSubscriptionRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"), "101", subscription_id);
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp1.asString().contains("Redemption not possible since amount is 0."),
				"Response did not matched");
		Assert.assertEquals(resp1.jsonPath().getString("qualifying_conditions.name"), qcName1,
				"QC name did not matched in auth redemption response");
		Assert.assertEquals(
				resp1.jsonPath().getString("qualifying_conditions.line_item_filters[0].line_item_selector.name"),
				lisName, "LIS name did not matched in auth redemption response");
		Assert.assertEquals(
				resp1.jsonPath().getString("qualifying_conditions.item_qualifiers[0].line_item_selector.name"), lisName,
				"LIS name did not match in auth redemption response");
		utils.logPass("QC and LIS details are verified in auth redemption response");

		// disable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "uncheck");
		// clean up is done in @AfterMethod

	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		utils.deleteLISQCRedeemable(env, null, qcExternalID2, null);
		utils.deleteSubscriptionPlan(env, planID, businessID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}