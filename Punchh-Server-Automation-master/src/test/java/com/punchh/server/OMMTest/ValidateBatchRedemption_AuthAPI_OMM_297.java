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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
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

public class ValidateBatchRedemption_AuthAPI_OMM_297 {
	static Logger logger = LogManager.getLogger(ValidateBatchRedemption_AuthAPI_OMM_297.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;
	String location1, location2;
	String filterSetName;
	Utilities utils;
	boolean GlobalBenefitRedemptionThrottlingToggle;
	private OfferIngestionUtilities offerUtils;
	private ApiPayloadObj apipayloadObj;
	private String qcExternalID, redeemableExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils.logit(sTCName + " ==>" + dataSet);
		enableMenuItemAggregatorFlag = false;
		location1 = " Jacksonville - 3816";
		location2 = " Daphne - 66233";
		filterSetName = "Only Base Items";
		GlobalBenefitRedemptionThrottlingToggle = false;
		offerUtils = new OfferIngestionUtilities(driver);
		apipayloadObj = new ApiPayloadObj();
	}

	@Test(description = "SQ-T7478 - Step 8: Verify functionality validations for discount_type -> redeemable in Batch Redemption Process API", priority=1)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T127_Step8() throws Exception {
		String userEmailUser = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser, dataSet.get("client"), dataSet.get("secret"));
		utils.logPass(userEmailUser + " is created sucessfully");


		// Create Redeemable
		String redeemableName = "Automation_RedeemableWithLocation_BatchRedemption_T127_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(1.0).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(4)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.setEffectiveLocation(dataSet.get("effective_locationA"))
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		/// 20 point gift
		Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "", "", "", "20");
		 utils.logit("info", "Send message API response status: " + sendMessageResponse.getStatusCode());
		    Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, 
		        "Failed: Expected status 201 for sendMessageToUser API, but got " + sendMessageResponse.getStatusCode());
		    utils.logPass("20 point gift sent successfully");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
		utils.logit("info", "Add to basket API response status: " + discountBasketResponse.getStatusCode());
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
				"Failed: Expected status 200 for authListDiscountBasketAddedAUTH API, but got " + discountBasketResponse.getStatusCode());
		utils.logPass(dbRedeemableId + " redeemable is added to the basket");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationB_Key"), userInfo.get("token"), userInfo.get("userID"), "101");
		utils.logit("info", "Batch redemption API response for locationB status: " + batchRedemptionProcessResponse.getStatusCode());
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
				"Failed: Expected status 200 for processBatchRedemptionOfBasketAUTHAPI API (locationB), but got " + batchRedemptionProcessResponse.getStatusCode());
		utils.logPass("Batch redemption API call for locationB executed");


		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
		String expectedResult = dataSet.get("expectedErrorMessage");
		utils.logit("info", "Verifying error message. Actual: " + actualErrorMessage + ", Expected: " + expectedResult);

		Assert.assertEquals(actualErrorMessage, expectedResult, "Failed: Error message mismatch. Expected: '" + expectedResult + "', but got: '" + actualErrorMessage + "'");
		utils.logPass("Verified the actual error message: " + actualErrorMessage);

		utils.logit("info", "Processing batch redemption for correct location");
		Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationA_Key"), userInfo.get("token"), userInfo.get("userID"), "101");
		Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed: Expected status 200 for processBatchRedemptionOfBasketAUTHAPI API (correct location), but got " + batchRedemptionProcessResponse1.getStatusCode());
		utils.logPass("Batch redemption for correct location processed successfully");

	}

	@Test(description = "SQ-T7478 - Step 7: Verify functionality validations for discount_type -> redeemable in Batch Redemption Process API", priority=2)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T127_Step7() throws Exception {
	
		// Generate user email and signup
	    String userEmailUser = pageObj.iframeSingUpPage().generateEmail();
	    Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser, dataSet.get("client"), dataSet.get("secret"));
	    utils.logPass(userEmailUser + " is created successfully");

	    // Create Redeemable
	    String redeemableName = "Automation_ZeroDiscount_BatchRedemption_T127_" + Utilities.getTimestamp();
	    utils.logit("info", "Creating redeemable: " + redeemableName);
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(0.0).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(2)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		 Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
			        "Failed: Expected status 200 for createRedeemable API, but got " + redeemableResponse.getStatusCode());
			    utils.logPass("Redeemable created successfully via API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		/// 20 point gift
		utils.logit("info", "Sending 20 point gift to user: " + userInfo.get("userID"));
	    Response sendMessageResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "", "", "", "20");
	    utils.logit("info", "Send message API response status: " + sendMessageResponse.getStatusCode());
	    Assert.assertEquals(sendMessageResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
	        "Failed: Expected status 201 for sendMessageToUser API, but got " + sendMessageResponse.getStatusCode());
	    utils.logPass("20 point gift sent successfully");

	    utils.logit("info", "Adding redeemable " + dbRedeemableId + " to user's discount basket");
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
		utils.logit("info", "Add to basket API response status: " + discountBasketResponse.getStatusCode());
	    Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
	        "Failed: Expected status 200 for authListDiscountBasketAddedAUTH API, but got " + discountBasketResponse.getStatusCode());
	    utils.logit("pass", dbRedeemableId + " redeemable is added to the basket");
	    
	 // Process batch redemption
	    utils.logit("info", "Processing batch redemption for user: " + userInfo.get("userID") + " at location: " + dataSet.get("locationkey"));
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"), "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		        "Failed: Expected status 200 for processBatchRedemptionOfBasketAUTHAPI API, but got " 
		        + batchRedemptionProcessResponse.getStatusCode());
		utils.logPass("Batch redemption API call returned 200 OK");

		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
		String expectedResult = dataSet.get("expectedErrorMessage");
		utils.logit("info", "Verifying error message. Actual: '" + actualErrorMessage + "', Expected: '" + expectedResult + "'");
		Assert.assertEquals(actualErrorMessage, expectedResult,
		        "Failed: Error message mismatch. Expected: '" + expectedResult + "', but got: '" + actualErrorMessage + "'");
		utils.logPass("Verified the actual error message: " + actualErrorMessage);

	}

	@Test(description = "SQ-T7478 - Step 4: Verify functionality validations for discount_type -> redeemable in Batch Redemption Process API", priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T127_Step4() throws Exception {

		 // Create Redeemable
	    String redeemableName = "Automation_FlatDiscount1_BatchRedemption_T127" + Utilities.getTimestamp();
	    utils.logit("Creating Redeemable with name: " + redeemableName);
	    
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(10.0).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(5)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to create redeemable via API");

		// Get Redeemable External ID
	    redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
	    utils.logit("Created Redeemable External ID: " + redeemableExternalID);
	    Assert.assertNotNull(redeemableExternalID, "Redeemable External ID should not be null");

	    // Get Redeemable ID from DB
	    String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
	    String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
	    utils.logit("DB Redeemable ID: " + dbRedeemableId);
	    Assert.assertNotNull(dbRedeemableId, "Redeemable ID from DB should not be null");

	 // Navigate and deactivate redeemable in UI
	    pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	    pageObj.instanceDashboardPage().loginToInstance();
	    pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	    pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

	    pageObj.redeemablePage().searchRedeemable(redeemableName);
	    pageObj.redeemablePage().deactivateThRedeemable(redeemableName, "ON");
	    utils.logit("Redeemable deactivated in UI");


	 // User SignUp using API
	    userEmail = pageObj.iframeSingUpPage().generateEmail();
	    utils.logit("Generated user email: " + userEmail);

	    Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"));
	    Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API2 signup status code is not 200");
	    String token = signUpResponse.jsonPath().get("access_token.token").toString();
	    String userID = signUpResponse.jsonPath().get("user.user_id").toString();
	    utils.logit("API2 Signup successful for userID: " + userID);

	    // Send points to user
	    pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "50");
	    utils.logit("Sent 50 points to user: " + userID);
		 
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
	            "Failed to add redeemable " + dbRedeemableId + " to discount basket");
	    utils.logit("Reward " + dbRedeemableId + " successfully added to basket");

	    // Reactivate redeemable in UI
	    pageObj.redeemablePage().searchRedeemable(redeemableName);
	    pageObj.redeemablePage().deactivateThRedeemable(redeemableName, "OFF");
	    utils.logit("Redeemable reactivated in UI");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
	            "Batch redemption API call failed for user: " + userID);
	    utils.logit("Batch redemption API call executed");

	    // Verify error message
	    String actualErrorMessage = batchRedemptionProcessResponse.jsonPath()
	            .getString("failures.message").replace("]", "").replace("[", "");
	    String expectedErrorMessage = dataSet.get("expectedErrorMessage");
	    Assert.assertEquals(actualErrorMessage, expectedErrorMessage,
	            "Expected error message did not match actual error message");
	    utils.logit("Verified error message: " + actualErrorMessage);

	    // Deactivate redeemable finally
	    pageObj.redeemablePage().searchRedeemable(redeemableName);
	    pageObj.redeemablePage().deactivateThRedeemable(redeemableName, "ON");
	    utils.logit("Redeemable deactivated finally in UI");
	}

	@Test(description = "SQ-T7478 - Step 1: Verify functionality validations for discount_type -> redeemable in Batch Redemption Process API", priority=4)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T127_Step1() throws Exception {
		
		// Generate a unique email for the new user
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
	    utils.logit("Generated user email: " + userEmailUser1);

		// =====================️ Create QC =====================
		String qcname = "AutomationQC_OMM_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.setReuseQualifyingItems(true)
				.addLineItemFilter("", "", 0)
				.addItemQualifier("line_item_exists", "", 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"QC creation API failed. Expected HTTP 200, but got: " + qcResponse.getStatusCode());
		utils.logPass("QC created successfully with HTTP 200 status");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		 Assert.assertNotNull(qcExternalID, "QC External ID should not be null");
		    utils.logit("QC '" + qcname + "' External ID: " + qcExternalID);

		// Create Redeemable
		String redeemableName = "AutomationRedeemable_OMM_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing").redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(1)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
	            "Redeemable creation API failed. Expected HTTP 200, but got: " + redeemableResponse.getStatusCode());
	    utils.logPass("Redeemable created successfully with HTTP 200 status");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		 Assert.assertNotNull(redeemableExternalID, "Redeemable External ID should not be null");
		    utils.logit("Redeemable '" + redeemableName + "' External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null for External ID: " + redeemableExternalID);
	    utils.logit("Fetched Redeemable ID from DB: " + dbRedeemableId);

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),dataSet.get("secret"));
		Assert.assertEquals(signUpResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
		    "User signup API failed. Expected HTTP 200, but got: " + signUpResponseUser1.getStatusCode());
		String token = signUpResponseUser1.jsonPath().get("access_token.token").toString();
		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();
		Assert.assertNotNull(token, "Access token should not be null for user signup");
	    Assert.assertNotNull(userIDUser1, "User ID should not be null for user signup");
	    utils.logPass("User signed up successfully. UserID: " + userIDUser1);

		/// 20 point gift
	    pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "", "", "", "20");
	    utils.logit("Sent 20 points to user: " + userIDUser1);

	    Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
	    		dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
	    Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
	    		"Failed to add redeemable to basket. Expected HTTP 200, but got: " + discountBasketResponse.getStatusCode());
	    utils.logPass("Redeemable ID " + dbRedeemableId + " successfully added to basket");

	    Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userIDUser1, "20");
	    Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
	    		"Failed to force redeem points. Expected HTTP 201, but got: " + forceRedeem_Response.getStatusCode());
	    utils.logPass("20 points force redeemed successfully for user: " + userIDUser1);


	    Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
	    		dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser1, "101");
	    Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
	    		"Batch redemption API call failed. Expected HTTP 200, but got: " + batchRedemptionProcessResponse.getStatusCode());
	    utils.logPass("Batch redemption API call successful");

	    boolean isAuthBatchRedemptionInsufficientBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
	    		ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponse.asString());
	    Assert.assertTrue(isAuthBatchRedemptionInsufficientBalanceSchemaValidated,
	    		"Auth API Process Batch Redemption Schema Validation failed");
	    utils.logPass("Batch redemption response schema validated successfully");

	    // Verify error message
	    String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("error");
	    String expectedErrorMessage = dataSet.get("expectedErrorMessage");
	    Assert.assertEquals(actualErrorMessage, expectedErrorMessage,
	    		"Expected error message: '" + expectedErrorMessage + "', but got: '" + actualErrorMessage + "'");
	    utils.logPass("Verified error message: '" + actualErrorMessage + "'");

	}

	@Test(description = "SQ-T7479 - Step 2: Verify functionality validations for discount_type -> redemption_code in Batch Redemption Process API", priority=5)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T128_Step2() throws Exception {
		
		String coupanCampaignName = "Auto_CuponCampaign_T128_" + CreateDateTime.getTimeDateString();
		utils.logit("info", "Starting test validateOMM297_T128_Step2 with campaign: " + coupanCampaignName);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.logit("info", "Successfully logged in and navigated to Campaigns page");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status,"FAILURE: Coupon campaign creation success message was NOT displayed");
	    utils.logPass("Coupon campaign created successfully");


		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode();
		Assert.assertNotNull(generatedCodeName,"FAILURE: Generated coupon code is NULL");
		utils.logit("Generated coupon code fetched: " + generatedCodeName);


		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
	    String userIDUser= signUpResponse.jsonPath().getString("user.user_id");
	    Assert.assertNotNull(token, "FAILURE: Access token is NULL after signup");
	    Assert.assertNotNull(userIDUser, "FAILURE: User ID is NULL after signup");
	    utils.logit("pass", "User signed up successfully. UserID: " + userIDUser);

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", generatedCodeName);
		Assert.assertEquals(discountBasketResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
	            "FAILURE: Add discount basket API did not return HTTP 200");
		utils.logPass("Verified coupon code in add basket response");

		String actualDiscountId = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountId, generatedCodeName, "FAILURE: Discount ID in basket response does not match generated coupon code");
		utils.logPass("Verified coupon code in add basket response");


		String discountType = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_type");
		Assert.assertEquals(discountType, "[redemption_code]", "FAILURE: Discount type is not 'redemption_code'");
		utils.logPass("Verified discount type as redemption_code");

	
		String expdiscount_basket_item_id = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		utils.logit("info", "Fetched discount_basket_item_id from add basket response: " + expdiscount_basket_item_id);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Coupons?", "uncheck");
		utils.logit("info", "Disabled 'Enable Coupons?' flag from Cockpit");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "FAILURE: Batch redemption API did not return HTTP 200 when coupons are disabled");

		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
		String expectedErrorMessage = dataSet.get("expectedErrorMessage");
		Assert.assertEquals(actualErrorMessage, expectedErrorMessage, "FAILURE: Error message mismatch when coupons are disabled");
		utils.logPass("Verified expected error message when coupons are disabled");

		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Coupons?", "check");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		pageObj.campaignspage().deactivateOrDeleteTheCoupon("deactivate");

		boolean deactivateStatus = pageObj.campaignspage().validateSuccessMessage();
	    Assert.assertTrue(deactivateStatus, "FAILURE: Success message not displayed after deactivating coupon");
	    utils.logPass("Coupon campaign deactivated successfully");

	    Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
	    		dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser, "101");
	    Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "FAILURE: Batch redemption API did not return HTTP 200 for deactivated coupon");

	    String actualErrorMessageDeactivate = batchRedemptionProcessResponse1.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
	    Assert.assertEquals(actualErrorMessageDeactivate, dataSet.get("counonDeactivateErrorMessage"), "FAILURE: Error message mismatch for deactivated coupon redemption");
	    utils.logPass("Verified expected error message for deactivated coupon");

	    utils.logPass("Test validateOMM297_T128_Step2 completed successfully");

	}

	@Test(description = "SQ-T7479 - Step 3: Verify functionality validations for discount_type -> redemption_code in Batch Redemption Process API", priority=6)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T128_Step1AndStep3() throws Exception {
		
		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("info", "Promo Code: " + promoCode);
	    utils.logit("info", "Campaign Name: " + coupanCampaignName);


		userEmail = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("info", "Generated user email: " + userEmail);
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userIDUser = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertNotNull(token, "Access token is NULL after signup");
	    Assert.assertNotNull(userIDUser, "User ID is NULL after signup");

	    utils.logPass("User signup successful. UserId: " + userIDUser);

		// Login to instance
	    utils.logit("info", "Logging into Punchh instance");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Promos?", "check");
		utils.logPass("Enable Promos flag turned ON");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(coupanCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setPromoCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("giftType"), dataSet.get("amount"), "", "");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean campaignCreated = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(campaignCreated,"Coupon campaign success message was NOT displayed after creation");
		utils.logit("pass", "Coupon campaign created successfully");

		// add basket redemption code in basket - promo code

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", promoCode);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Add basket API failed. Expected 200 but got " + discountBasketResponse.getStatusCode());
		

		String actualDiscountId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountId, promoCode, "Discount ID mismatch in add basket response");
		utils.logPass("Verified promo code added in basket response: " + actualDiscountId);

		String actualDiscountType = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_type");
		Assert.assertEquals(actualDiscountType, "[redemption_code]", "Discount type is not redemption_code");
	    utils.logPass("Verified redemption_code discount type");
		
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Promos?", "uncheck");
		utils.logit("info", "Enable Promos flag disabled");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Batch redemption API failed when promos disabled");

		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
	    String expectedErrorMessage = dataSet.get("expectedErrorMessage");
	    Assert.assertEquals(actualErrorMessage, expectedErrorMessage, "Error message mismatch when promos are disabled");
	    utils.logPass("Verified error message when promos are disabled: " + actualErrorMessage);


		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Promos?", "check");
		utils.logit("info", "Enable Promos flag enabled again");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		pageObj.campaignspage().deactivateOrDeleteTheCoupon("deactivate");

		boolean deactivateStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(deactivateStatus, "Success message not displayed after coupon deactivation");
		utils.logit("pass", "Coupon campaign deactivated successfully");

		Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser, "101");
	    Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Batch redemption API failed for deactivated campaign");

	    String actualErrorMessageDeactivate = batchRedemptionProcessResponse1.jsonPath().getString("failures.message").replace("[", "").replace("]", "");
	    Assert.assertEquals(actualErrorMessageDeactivate, dataSet.get("counonDeactivateErrorMessage"), "Error message mismatch for deactivated coupon");
	    utils.logit("pass", "Verified error message for deactivated coupon: " + actualErrorMessageDeactivate);

	}

	@Test(description = "SQ-T7480: Verify API parameter validations for Process Batch Redemption API (Auth)", priority=7)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T125() throws Exception {
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("pass",userEmailUser1 + " is created sucessfully");

		// =====================️ Create QC =====================
		String qcname = "AutomationQC_OMM_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.setReuseQualifyingItems(true)
				.addLineItemFilter("", "", 0)
				.addItemQualifier("line_item_exists", "", 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		utils.logit("pass", "QC creation API response: " + qcResponse.prettyPrint());
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation API failed");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit("pass", qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable
		String redeemableName = "AutomationRedeemable_OMM_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing").redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(1)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		utils.logit("pass", "Redeemable creation API response: " + redeemableResponse.prettyPrint());
	    Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Redeemable creation API failed");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("pass", "Redeemable ID fetched from DB: " + dbRedeemableId);

		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser1, "API 2 user signup");
		String token = signUpResponseUser1.jsonPath().get("access_token.token").toString();
		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();

		// send reward amount to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser1, dataSet.get("apiKey"), "", "","", "50");
		utils.logit("pass", "Send Reward API response: " + sendRewardResponse.prettyPrint());
	    Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed to send 50 points to user");
	    utils.logit("pass", "50 points sent to the user");

		// Step15--
		Response batchRedemptionProcessResponseStep15 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser1, "101");
		utils.logit("pass", "Batch Redemption Step15 API response: " + batchRedemptionProcessResponseStep15.prettyPrint());
	    Assert.assertEquals(batchRedemptionProcessResponseStep15.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Step15: Expected 422 status for no basket");
	    
		boolean isAuthBatchRedemptionProcessNoBasketValidated = Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				batchRedemptionProcessResponseStep15.asString());
		Assert.assertTrue(isAuthBatchRedemptionProcessNoBasketValidated,"Auth API Process Batch Redemption Schema Validation failed");

		String actualStep15ErrorMessage = batchRedemptionProcessResponseStep15.jsonPath().getString("error.message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStep15ErrorMessage, "No Basket found.", "Step15: Error message mismatch");
	    utils.logit("pass", "Verified Step15 and 422 status code for no basket");

	    Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
	    		dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
	    utils.logit("pass", "Discount Basket API response: " + discountBasketResponse.prettyPrint());
	    Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to add redeemable to basket");

	    String basketItemID = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
	    utils.logit("pass", "Basket Item ID: " + basketItemID);

		// Step8 ​"authentication_token" or "Authorization" (user) not sent in API
		// exp-Result -{ "error": "You need to sign in or sign up before continuing." }

		Response batchRedemptionProcessResponseStep8 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), null, userIDUser1, "101");
	    Assert.assertEquals(batchRedemptionProcessResponseStep8.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Step8: Expected 401 for missing token");
	    Assert.assertEquals(batchRedemptionProcessResponseStep8.jsonPath().getString("error").replace("[","").replace("]",""),
	            "You need to sign in or sign up before continuing.", "Step8: Error message mismatch");
	    utils.logit("pass", "Verified Step8 and 401 status code for missing token");

		// Step9 ​"authentication_token" or "Authorization" (user) sent blank in API
		// exp-Result -{ "error": "You need to sign in or sign up before continuing." }

		Response batchRedemptionProcessResponseStep9 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), "", userIDUser1, "101");
		Assert.assertEquals(batchRedemptionProcessResponseStep9.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Step9: Expected 401 for blank token");
	    Assert.assertEquals(batchRedemptionProcessResponseStep9.jsonPath().getString("error").replace("[","").replace("]",""),
	            "You need to sign in or sign up before continuing.", "Step9: Error message mismatch");
	    utils.logit("pass", "Verified Step9 and 401 status code for blank token");

		// Step10 "authentication_token" or "Authorization" (user) sent is incorrect in
		// API
		// exp-Result -{ "error": "You need to sign in or sign up before continuing." }

		Response batchRedemptionProcessResponseStep10 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token + "testing123456",
				userIDUser1, "101");
		Assert.assertEquals(batchRedemptionProcessResponseStep10.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthBatchRedemptionProcessInvalidAuthTokenValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponseStep8.asString());
		Assert.assertTrue(isAuthBatchRedemptionProcessInvalidAuthTokenValidated,"Auth API Process Batch Redemption Schema Validation failed");
		String actualStep10ErrorMessage = batchRedemptionProcessResponseStep10.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStep10ErrorMessage, "You need to sign in or sign up before continuing.");
		utils.logit("pass","Verified Step10 and the 401 status code,if user send invalid token");

		// Ban a User
		Response banUserresponse = pageObj.endpoints().banUser(userIDUser1, dataSet.get("apiKey"));
		Assert.assertEquals(banUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,"Status code 200 did not matched for PLATFORM FUNCTIONS API Ban a User");
		utils.logit("PLATFORM FUNCTIONS API Ban a User is successful");

		// Step13 Pre-condition -> "authentication_token" or "Authorization" (user) sent
		// in API is of a banned user
		// exp-Result -{ "error": "You need to sign in or sign up before continuing." }

		Response batchRedemptionProcessResponseStep13 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser1, "101");
		Assert.assertEquals(batchRedemptionProcessResponseStep13.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		String actualStep13ErrorMessage = batchRedemptionProcessResponseStep13.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStep13ErrorMessage, "You need to sign in or sign up before continuing.");
		utils.logit("pass","Verified Step13 and the 401 status code,if user send invalid token");

		// UnBan a User
		Response unBanUserresponse = pageObj.endpoints().unBanUser(userIDUser1, dataSet.get("apiKey"));
		Assert.assertEquals(unBanUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Status code 200 did not matched for PLATFORM FUNCTIONS API UnBan a User");
		utils.logit("pass","PLATFORM FUNCTIONS API UnBan a User is successful");

		// Step16-Pre-condition -> "authentication_token" or "Authorization" (user) sent
		// in API has an active basket but it is empty

		Response deleteBasketID_Response = pageObj.endpoints().deleteItemFromBasket_AuthAPI(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationkey"), token, basketItemID, "");
		Assert.assertEquals(deleteBasketID_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Status code 200 did not matched for deleteItemFromBasket_AuthAPI API");
		
		Response batchRedemptionProcessResponseStep16 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser1, "101");
		Assert.assertEquals(batchRedemptionProcessResponseStep16.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String actualStep16ErrorMessage = batchRedemptionProcessResponseStep16.jsonPath().getString("error.message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStep16ErrorMessage, "No Basket found.");
		utils.logit("pass","Verified Step16 and the 422 status code,if user send invalid token");

		// Deactivate a User
		Response deactivateUserresponse = pageObj.endpoints().deactivateUser(userIDUser1, dataSet.get("apiKey"));
		Assert.assertEquals(deactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		utils.logit("pass","PLATFORM FUNCTIONS API Deactivate a User is successful");

		// STEP-14 Pre-condition -> "authentication_token" or "Authorization" (user)
		// sent in API is of a deactivated user

		Response batchRedemptionProcessResponseStep14 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser1, "101");
		Assert.assertEquals(batchRedemptionProcessResponseStep13.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		String actualStep14ErrorMessage = batchRedemptionProcessResponseStep14.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStep14ErrorMessage, "You need to sign in or sign up before continuing.");
		utils.logit("pass","Verified Step14 and the 401 status code,if user send invalid token");

		// Reactivate a User
		Response reactivateUserresponse = pageObj.endpoints().reactivateUser(userIDUser1, dataSet.get("apiKey"));
		Assert.assertEquals(reactivateUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Status code 200 did not matched for PLATFORM FUNCTIONS API Reactivate a User");
		utils.logit("pass","PLATFORM FUNCTIONS API Reactivate a User is successful");

	}


	@Test(description = "SQ-T7481 - Step 1: Verify on successful redemption \"qualified_menu_items\" are returned in updated naming convention format in API response (Auth API)", priority=8)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T121_Step1() throws Exception {
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("pass",userEmailUser1 + " is created sucessfully");

		// =====================️ Create QC =====================
		String qcname = "AutomationQC_OMM_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.setReuseQualifyingItems(true)
				.addLineItemFilter("", "", 0)
				.addItemQualifier("line_item_exists", "", 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
	    Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation API failed");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit("pass", qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable
		String redeemableName = "AutomationRedeemable_OMM_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing").redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(1)
				.setBooleanField("applicable_as_loyalty_redemption", true)
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Redeemable creation API failed");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("pass", "Redeemable ID fetched from DB: " + dbRedeemableId);

		
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));
	    utils.logit("pass", "User signup API response: " + signUpResponseUser.prettyPrint());
	    Assert.assertEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");

	    String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
	    String userIDUser = signUpResponseUser.jsonPath().get("user.user_id").toString();
	    utils.logit("pass", "User token: " + token + ", User ID: " + userIDUser);

		// send reward amount to user
	    Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userIDUser, dataSet.get("apiKey"), "", "","", "50");
	    Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed to send 50 points to user");
	    utils.logit("pass", "50 points sent to the user");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dbRedeemableId);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to add redeemable to basket");
		utils.logit("pass",dbRedeemableId + " redeemable is added to the basket");

		String actualDiscountType = discountBasketResponse.jsonPath().getString("discount_basket_items[0].discount_type").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "redeemable");
		utils.logit("pass","Verified the actual discount type 'redeemable' in add basket response ");

		String actualDiscountID = discountBasketResponse.jsonPath().getString("discount_basket_items[0].discount_id").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountID, dbRedeemableId);
		utils.logit("pass","Verified the actual discount id '" + actualDiscountID + "' in add basket response ");

		String actualRedeemableName = discountBasketResponse.jsonPath().getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName, redeemableName);
		utils.logit("pass","Verified the actual redeemable name '" + actualRedeemableName + "' in add basket response ");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userIDUser, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Batch redemption process failed");
		
		String actualProcessDiscountType = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_type").replace("[", "").replace("]", "");
		Assert.assertEquals(actualProcessDiscountType, "redeemable", "Discount type mismatch in batch redemption response");
		utils.logit("pass", "Verified discount type 'redeemable' in batch redemption response");

		String actualProcessDiscountID = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_id").replace("[", "").replace("]", "");
		Assert.assertEquals(actualProcessDiscountID, dbRedeemableId, "Discount ID mismatch in batch redemption response");
	    utils.logit("pass", "Verified discount ID '" + actualProcessDiscountID + "' in batch redemption response");

		String actualProcessRedeemableName = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualProcessRedeemableName, redeemableName, "Redeemable name mismatch in batch redemption response");
	    utils.logit("pass", "Verified redeemable name '" + actualProcessRedeemableName + "' in batch redemption response");

	    double expectedDiscountAmount = Double.parseDouble(dataSet.get("discountAmount"));
	    String actualDiscountAmountStr = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_amount").replace("[", "").replace("]", "");
	    double actualDiscountAmountDouble = Double.parseDouble(actualDiscountAmountStr);
	    Assert.assertEquals(actualDiscountAmountDouble, expectedDiscountAmount, "Discount amount mismatch in batch redemption response");
	    utils.logit("pass", "Verified discount amount '" + actualDiscountAmountDouble + "' in batch redemption response");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, "", qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		utils.logit("Browser closed");
	}

} // END of class
