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
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Rakhi Rawat
 */

@Listeners(TestListeners.class)

public class VerifyQualificationCriteriaDetailsTest {
	static Logger logger = LogManager.getLogger(VerifyQualificationCriteriaDetailsTest.class);
	public WebDriver driver;
	private String userEmail;
	Utilities utils;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, redeemableExternalID;

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
	}

	// Rakhi
	@Test(description = "SQ-T6917 Verify QC(Processing function->Buy one get one(fix item)) details in failure of POS redemption's response in case of discount amount"
			+ "SQ-T6922 Verify QC(Processing function->Buy one get one(fix item))  details in failure of Auth redemption's response in case of discount amount", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T6917_verifyQcDetailsForDiscountAmount() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth API User Sign-up call is successful for user ID: " + userID);

		String lisName = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String qcname = "AutomationQC_API_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemable_API_" + Utilities.getTimestamp();

		// Create LIS with base item 777
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "777")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null in API response");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setQCProcessingFunction("bogof")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, 0.0, 0)
				.build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null in API response");
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
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null in API response");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// add redeemable to loyalty goal completion
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().selectRedeemableOnLoyaltyGoalCompletion(redeemableName);

		// enable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "check");

		// send amount to user
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"), "100",
				"", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logInfo("Api2 send amount to user is successful");

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn, "50",
				dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
				"Status code 200 did not match for POS redemption API");
		Assert.assertTrue(resp.asString().contains("Discount qualification on receipt failed"),
				"Response did not matched");
		Assert.assertEquals(resp.jsonPath().getString("qualifying_conditions.name"), qcname,
				"QC name did not matched in POS redemption response");
		Assert.assertEquals(
				resp.jsonPath().getString("qualifying_conditions.line_item_filters[0].line_item_selector.name"),
				lisName, "LIS name did not matched in POS redemption response");
		utils.logPass("QC and LIS details are verified in POS redemption response");
		utils.longWaitInSeconds(8); // static wait between two b2b redemptions

		// SQ-T6922 Part
		// Create Online Redemption
		utils.logInfo("== Auth API Create Online Redemption ==");
		Response resp1 = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(resp1.asString().contains("Discount qualification on receipt failed"),
				"Response did not matched");
		Assert.assertEquals(resp1.jsonPath().getString("qualifying_conditions.name"), qcname,
				"QC name did not matched in POS redemption response");
		Assert.assertEquals(
				resp1.jsonPath().getString("qualifying_conditions.line_item_filters[0].line_item_selector.name"),
				lisName, "LIS name did not matched in POS redemption response");
		Assert.assertEquals(
				resp1.jsonPath().getString("qualifying_conditions.item_qualifiers[0].line_item_selector.name"), lisName,
				"LIS name did not match in POS redemption response");
		utils.logPass("QC and LIS details are verified in POS redemption response");

		// add base redeemable back to loyalty goal completion
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().selectRedeemableOnLoyaltyGoalCompletion("Base Redeemable");

		// disable flag in POS Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Return qualifying condition to v1", "uncheck");
	}

	// Rakhi
	@Test(description = "SQ-T6923 Verify QC(Processing function->Sum of Amounts Incremental) details in failure of auth redemption's response in case of reward redemption", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T6923_verifyQcDetailsForRewardRedemption() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// add redeemable to loyalty goal completion
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_return_qualifying_condition_to_v1", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_return_qualifying_condition_to_v2", "check");
		pageObj.dashboardpage().updateCheckBox();
		
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token").toString();
		utils.logPass("Auth API User Sign-up call is successful for user ID: " + userID);

		String lisName = "AutomationLIS_API_" + Utilities.getTimestamp();
		String qcname = "AutomationQC_API_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemable_API_" + Utilities.getTimestamp();

		// Create LIS with base item 777
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "777")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null in API response");
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts_incremental").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null in API response");
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
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null in API response");
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
		utils.logInfo("Api2 Send Redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null in API response");
		Assert.assertFalse(rewardId.isEmpty(), "Reward ID is empty in API response");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(authToken, rewardId,
				dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.asString().contains("Discount qualification on receipt failed"),
				"Response did not matched");
		Assert.assertEquals(resp.jsonPath().getString("qualifying_conditions.name"), qcname,
				"QC name did not matched in POS redemption response");
		Assert.assertEquals(
				resp.jsonPath().getString("qualifying_conditions.item_qualifiers[0].line_item_selector.name"), lisName,
				"LIS name did not match in POS redemption response");
		utils.logPass("QC and LIS details are verified in auth redemption response");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}