package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class camRedemptionTest {
	static Logger logger = LogManager.getLogger(camRedemptionTest.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private String endDateTime;
	private Utilities utils;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	private ApiPayloadObj apipayloadObj;
	public String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	public String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";



	@BeforeMethod
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		GlobalBenefitRedemptionThrottlingToggle = false;
		endDateTime = CreateDateTime.getTomorrowDate() + " 10:00 AM";
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
	}

	@Test(description = "MPC-T1435 Verify QC(Processing function->Hit Target Menu Item Price) details in failure of auth redemption's response in case of reward redemption", groups = "Regression")
	public void MPC_T1435verifyQCHitTargetMenuItemPriceDetailsInAuthRedemptionFailureResponse() throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCHitTargetPrice_" + Utilities.getTimestamp();

		
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)  .addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass("Created Redeemable External ID: " + redeemableExternalID + " for " + redeemableName);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");	
		dataSet.put("redeemable_id", dbRedeemableId);

		// User register/signup using API2 Signup
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				redeemableName, "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		Response possibleRedemptionRespo = pageObj.endpoints().posPossibleRedemptionOfRedeemable(userEmail,
				dataSet.get("redeemable_id"), dataSet.get("locationkey"), "103");
		Response resp = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"), dataSet.get("secret"),
				authTokenFromDB, rewardId, "14", parentMap);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "MPC-T1436 Verify QC(Processing function->Rate rollback) details in failure of auth redemption's response in case of redeemable redemption", groups = "Regression")
	public void MPC_T1436verifyQCRateRollbackDetailsInAuthRedemptionFailureResponseForRedeemableRedemption()
			throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCRateRollback_" + Utilities.getTimestamp();

		
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass("Created Redeemable External ID: " + redeemableExternalID + " for " + redeemableName);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");	
		dataSet.put("redeemable_id", dbRedeemableId);

		
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				redeemableName, "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		Response resp = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"), dataSet.get("secret"),
				authTokenFromDB, rewardId, "14", parentMap);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "MPC-T1437 Verify QC(Processing function->Sum of Amounts) details in failure of auth redemption's response in case of banked currency", groups = "Regression")
	public void MPC_T1437verifyQCSumOfAmountsDetailsInAuthRedemptionFailureResponseForBankedCurrency()
			throws Exception {
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				"DNDAutomationRedeemableSumOfAmount", "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		Response resp = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"), dataSet.get("secret"),
				authTokenFromDB, rewardId, "14", parentMap);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");

	}

	@Test(description = "MPC-T1439 Verify QC(Processing function->Hit Target Menu Item Price) details in failure of POS redemption's response in case of reward redemption", groups = "Regression")
	public void MPC_T1439verifyQCHitTargetMenuItemPriceDetailsInPOSRedemptionFailureResponseForRewardRedemption()
			throws Exception {
		
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCHitTargetPrice_" + Utilities.getTimestamp();

		
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)  .addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass("Created Redeemable External ID: " + redeemableExternalID + " for " + redeemableName);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");	
		dataSet.put("redeemable_id", dbRedeemableId);

		// User register/signup using API2 Signup
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				redeemableName, "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "102");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "MPC-T1440 VVerify QC(Processing function->Rate rollback) details in failure of POS redemption's response in case of redeemable redemption", groups = "Regression")
	public void MPC_T1440verifyQCRateRollbackDetailsInPOSRedemptionFailureResponseForRedeemableRedemption()
			throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCHitTargetPrice_" + Utilities.getTimestamp();

		
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)  .addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass("Created Redeemable External ID: " + redeemableExternalID + " for " + redeemableName);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");	
		dataSet.put("redeemable_id", dbRedeemableId);

		// User register/signup using API2 Signup
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				redeemableName, "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "102");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "MPC-T1441 Verify QC(Processing function->Sum of Amounts) details in failure of POS redemption's response in case of banked currency", groups = "Regression")
	public void MPC_T1441verifyQCSumOfAmountsDetailsInPOSRedemptionFailureResponseForBankedCurrency() throws Exception {
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v1", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				"DNDAutomationRedeemableSumOfAmount", "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "102");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");

	}

	@Test(description = "MPC-T1452 Verify QC details in failure of auth redemption api response in case of batch redemption", groups = "Regression")
	public void MPC_T1452verifyQCDetailsInAuthRedemptionFailureResponseForBatchRedemption() throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCHitTargetPrice_" + Utilities.getTimestamp();

		
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0)  .addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logPass("Created Redeemable External ID: " + redeemableExternalID + " for " + redeemableName);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");	
		dataSet.put("redeemable_id", dbRedeemableId);


		// User register/signup using API2 Signup
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to pos integration submenu
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v2", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				redeemableName, "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");
		String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		Response resp = pageObj.endpoints().processBatchRedemptionsAUTHAPI(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), token, userID, "12003", external_id, map);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for process batch redemption api");
		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("failures[0].message[0]").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.line_item_filters[0]") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.item_qualifiers[0]") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "MPC-T1452 Verify QC details in failure of POS redemption api response in case of batch redemption", groups = "Regression")
	public void MPC_T1453verifyQCDetailsInAuthRedemptionFailureResponseForBatchRedemption() throws Exception {

		// User register/signup using API2 Signup
		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to pos integration submenu
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying condition to v2", "check");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser("Message from AutoFive-PendingCheckin", "Redeemable",
				"DNDAutomationRedeemableSumOfAmount", "Support Activity");
		String status = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(status.contains("Message sent!"), "Message sent did not displayed on timeline");
		TestListeners.extentTest.get().pass("Message sent did displayed on timeline");
		logger.info("Message sent did displayed on timeline");
		utils.longWaitInSeconds(10);
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");
		String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// pos add reward to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId, external_id);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("POS add reward to basket is successful");
		TestListeners.extentTest.get().pass("POS add reward to basket is successful");

		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap1);
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		Response resp = pageObj.endpoints().processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10",
				"12003", external_id, map);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for process batch redemption api");
		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("failures[0].message[0]").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.line_item_filters[0]") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("failures[0].qualifying_conditions.item_qualifiers[0]") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");

	}

	@Test(description = "OMM-T4853 Campaign Name verification in Redemption api 1.0 and 2.0",groups = {"nonNightly", "Regression"}, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void OMM_T4853verifyCampaignNameInRedemptionAPI() throws Exception {

        String userEmail = "autoiframe14504419092025glbkqx@punchh.com";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.campaignspage().createDuplicateCampaigns("DNDMassOfferCampaign2");
		String massCampaignName = "MassOfferCampaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// edit the custom segment list
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().updateCustomList("DND automation Segment2", "edit", userEmail);
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		// navigate to the guesttimline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickRewards();
		String RewardId = pageObj.guestTimelinePage().getForceRedemptionRewardId(massCampaignName);
		Assert.assertNotNull(RewardId, "Reward id is null");
		logger.info("Reward id is " + RewardId);
		TestListeners.extentTest.get().info("Reward id is " + RewardId);
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				RewardId, "12003");
		Assert.assertEquals(posRedeem1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		// fetch campaign name from posRedeem1 response
		String campaignName = posRedeem1.jsonPath().get("campaign_name").toString();
		Assert.assertEquals(campaignName, massCampaignName, "Campaign name did not matched");
		logger.info("Campaign name " + campaignName + " is matched in POS redemption response");
	}

	@Test(description = "MPC-T1452 Campaign Name verification in Redemption api 1.0 and 2.0",groups = {"nonNightly", "Regression"}, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void OMM_T4853verifyCampaignNameInRedemptionAPI2() throws Exception {
        String userEmail = "autoiframe14504419092025glbkqx@punchh.com";
        String token = "-igQGZBNjdem5hB_57ur";
        String userID = "455972200";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.campaignspage().createDuplicateCampaigns("DNDMassOfferCampaign2");
		String massCampaignName = "MassOfferCampaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		String dateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// edit the custom segment list
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().updateCustomList("DND automation Segment2", "edit", userEmail);
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		// navigate to the guesttimline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickRewards();
		String RewardId = pageObj.guestTimelinePage().getForceRedemptionRewardId(massCampaignName);
		Assert.assertNotNull(RewardId, "Reward id is null");
		logger.info("Reward id is " + RewardId);
		TestListeners.extentTest.get().info("Reward id is " + RewardId);
		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		Response resp = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"), dataSet.get("secret"),
				authTokenFromDB, RewardId, "14", parentMap);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		// fetch the campaign name from resp
		String campaignName = resp.jsonPath().get("campaign_name").toString();
		Assert.assertEquals(campaignName, massCampaignName, "Campaign name did not matched");
		logger.info("Campaign name " + campaignName + " is matched in AUTH online redemption response");
		TestListeners.extentTest.get()
				.info("Campaign name " + campaignName + " is matched in AUTH online redemption response");
	}

	@Test(description = "MPC-T1452 Campaign Name verification in Redemption api 1.0 and 2.0",groups = {"nonNightly", "Regression"}, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void OMM_T4853verifyCampaignNameInRedemptionAPI3() throws Exception {
        String userEmail = "autoiframe14504419092025glbkqx@punchh.com";
        String token = "14504419092025";
        String userID = "455972200";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign("DNDMassOfferCampaign2");
		pageObj.campaignspage().createDuplicateCampaigns("DNDMassOfferCampaign2");
		String massCampaignName = "MassOfferCampaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		String dateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// edit the custom segment list
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().updateCustomList("DND automation Segment2", "edit", userEmail);
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		// navigate to the guesttimline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickRewards();
		String RewardId = pageObj.guestTimelinePage().getForceRedemptionRewardId(massCampaignName);
		Assert.assertNotNull(RewardId, "Reward id is null");
		logger.info("Reward id is " + RewardId);
		TestListeners.extentTest.get().info("Reward id is " + RewardId);
		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", RewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		Response resp2 = pageObj.endpoints().processBatchRedemptionsAUTHAPI(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003", external_id, map);
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for process batch redemption api");
		// .fetch the campaign name from auth batch redemption response
		String campaignName = resp2.jsonPath().get("success[0].discount_details.campaign_name").toString();
		Assert.assertEquals(campaignName, massCampaignName, "Campaign name did not matched");
		logger.info("Campaign name " + campaignName + " is matched in AUTH batch redemption response");
		TestListeners.extentTest.get()
				.info("Campaign name " + campaignName + " is matched in AUTH batch redemption response");

	}

	@Test(description = "MPC-T1452 Campaign Name verification in Redemption api 1.0 and 2.0",groups = {"nonNightly", "Regression"}, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void OMM_T4853verifyCampaignNameInRedemptionAPI4() throws Exception {
        String userEmail = "autoiframe14504419092025glbkqx@punchh.com";
        String token = "14504419092025";
        String userID = "455972200";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.campaignspage().createDuplicateCampaigns("DNDMassOfferCampaign2");
		String massCampaignName = "MassOfferCampaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		String dateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// edit the custom segment list
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().updateCustomList("DND automation Segment2", "edit", userEmail);
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		// navigate to the guesttimline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickRewards();
		String RewardId = pageObj.guestTimelinePage().getForceRedemptionRewardId(massCampaignName);
		Assert.assertNotNull(RewardId, "Reward id is null");
		logger.info("Reward id is " + RewardId);
		TestListeners.extentTest.get().info("Reward id is " + RewardId);
		// fetch authentication_token from users table
		String query = "SELECT authentication_token FROM users WHERE email='" + userEmail + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, query,
				"authentication_token");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		// .fetch the campaign name from auth online order response
		String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		// pos add reward to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", RewardId, external_id);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("POS add reward to basket is successful");
		TestListeners.extentTest.get().pass("POS add reward to basket is successful");
		Response resp3 = pageObj.endpoints().processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10",
				"12003", external_id, map);
		Assert.assertEquals(resp3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for process batch redemption api");
		// .fetch the campaign name from POS batch redemption response
		String campaignName = resp3.jsonPath().get("success[0].discount_details.campaign_name").toString();
		Assert.assertEquals(campaignName, massCampaignName, "Campaign name did not matched");
		logger.info("Campaign name " + campaignName + " is matched in POS batch redemption response");
		TestListeners.extentTest.get()
				.info("Campaign name " + campaignName + " is matched in POS batch redemption response");
	}

	@Test(description = "Verify QC(Processing function->Receipt Total Amount)  details in failure of POS redemption's response in case of redeemable redemption", groups = "Regression")
	@Owner(name = "Vansham Mishra")
	public void MPC_T1444verifyQCReceiptTotalAmountDetailsInPOSRedemptionFailureResponseForRedeemableRedemption()
			throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemableQCHitTargetPrice_" + Utilities.getTimestamp();

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		logger.info(lisName + " LIS is Created with External ID: " + lisExternalID);
		TestListeners.extentTest.get().pass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		logger.info("Created Redeemable External ID: " + redeemableExternalID);
		TestListeners.extentTest.get()
				.pass(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");
		dataSet.put("redeemable_id", dbRedeemableId);

		// update business timezone using db query
		pageObj.utils().updateBusinessRedemption1Dot0Flag(dataSet.get("slug"), true, env);

		// User SignUp using API
		String external_source_id = CreateDateTime.getTimeDateString();
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap = new HashMap<String, String>();
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "106", "100", "1", "102");
		parentMap.put("Pizza1", detailsMap);
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "102");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = resp.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(resp.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "Verify QC(Processing function->Receipt Total Amount)  details in failure of POS redemption's response in case of redeemable redemption", groups = "Regression")
	@Owner(name = "Vansham Mishra")
	public void MPC_T1451verifyQCReceiptTotalAmountDetailsInAuthRedemptionFailureResponseForRedeemableRedemption()
			throws Exception {
		String lisName = "Automation_LIS_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_" + Utilities.getTimestamp();
		String redeemableName = "AutomationRedeemable_" + Utilities.getTimestamp();

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		logger.info(lisName + " LIS is Created with External ID: " + lisExternalID);
		TestListeners.extentTest.get().pass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(2.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		logger.info("Created Redeemable External ID: " + redeemableExternalID);
		TestListeners.extentTest.get()
				.pass(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query1 = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");
		dataSet.put("redeemable_id", dbRedeemableId);

		// update business timezone using db query
		pageObj.utils().updateBusinessRedemption1Dot0Flag(dataSet.get("slug"), true, env);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String userID = signUpResponse.jsonPath().get("id").toString();
		String auth_token = signUpResponse.jsonPath().get("authentication_token").toString();

		// send reward to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");

		Response response = pageObj.endpoints().authOnlineRedeemableRedemption(auth_token, dataSet.get("client"),
				dataSet.get("secret"), "1001", dataSet.get("redeemable_id"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for auth redemption api");

		// verify status in response resp should contain the message Discount
		// qualification on receipt failed
		String statusInResponse = response.jsonPath().get("status").toString();
		Assert.assertTrue(statusInResponse.contains("Discount qualification on receipt failed"),
				"Status in response did not matched");
		// validate that receipt_qualifiers is present in the response
		Assert.assertTrue(response.jsonPath().get("qualifying_conditions.receipt_qualifiers") != null,
				"receipt_qualifiers is not present in the response");
		// validate that line_item_filters is present
		Assert.assertTrue(response.jsonPath().get("qualifying_conditions.line_item_filters") != null,
				"line_item_filters is not present in the response");
		// validate that item_qualifiers is present
		Assert.assertTrue(response.jsonPath().get("qualifying_conditions.item_qualifiers") != null,
				"item_qualifiers is not present in the response");
		logger.info("verified that Verify the response and It should contain status msg: \n"
				+ "Discount qualification on receipt failed for [user name] at [location].\n"
				+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		TestListeners.extentTest.get()
				.pass("verified that Verify the response and It should contain status msg: \n"
						+ "Discount qualification on receipt failed for [user name] at [location].\n"
						+ "and details of QC like item_qualifier, receipt_qualifier, line_item_filters ");
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
