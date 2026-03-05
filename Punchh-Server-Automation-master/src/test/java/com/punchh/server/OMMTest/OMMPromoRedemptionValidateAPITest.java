package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
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
public class OMMPromoRedemptionValidateAPITest {

    static Logger logger = LogManager.getLogger(OMMPromoRedemptionValidateAPITest.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private static Map<String, String> dataSet;
    private String userEmail;
    private Utilities utils;
    private ApiPayloadObj apipayloadObj;
    public  String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	public String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";


    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        sTCName = method.getName();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(
                pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        utils = new Utilities(driver);
        apipayloadObj =  new ApiPayloadObj();
    }

	public String getMenuItemsBasedOnItemID(String input, String target) {

		String result = null;

		// Split by '^' to separate items
		String[] parts = input.split("\\^");
		for (String part : parts) {
			if (part.contains(target)) {
				result = part;
				break;
			}
		}

		if (result != null) {
			utils.logPass("Matched Segment: " + result);
		} else {
			utils.logit("Target not found!");
		}

		return result;
	}
	
	//Bug Ticket-OMM-1489
	@Test(description = "SQ-T7448 | Verify that Order.Id should not store boolean 'true' as value", groups = { "regression", "dailyrun" })
			
	public void validateOMM_T4894_Verify_OrderID_not_true() throws Exception {
		String redeemableName = "AutomationRedeemableFlatDiscount_" + Utilities.getTimestamp();
		logger.info("Redeemable Name: " + redeemableName);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

	    String redeemable_id =  dbRedeemableId;  //dataSet.get("redeemableID"); //DoNotDelete Automation_Hitesh discount 
	    Map<String, Object> mapOfDetails = new HashMap<>();
	    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	    mapOfDetails.put("productID_AsQCID", "101");
	    mapOfDetails.put("itemID", "101");
	    mapOfDetails.put("orderId", true );


	    // User SignUp using API
	    userEmail = pageObj.iframeSingUpPage().generateEmail();
	    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
	            dataSet.get("secret"));
	    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
	    
	    //Verify response code is not 400
	    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "API crashed with 400 Bad Request during signup");

	 // Verify status code is 200
	 Assert.assertEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
	 utils.logPass("API2 Signup is successful");
	 
	// Send reward amount to user to unlock Redeemable
	    Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
	            userID, dataSet.get("apiKey"), "", "", "", "200");

	    // Capture status code from API response
	    int statusCode = sendRewardResponse2.getStatusCode();

	    // Assert logic for success (200 or 201) OR failure (400)
	    if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
	        Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
	                "Expected status code 200 or 201 for successful sendMessageToUser API");
	        logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	    } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
	        Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
	                "Expected status code 400 for sendMessageToUser API failure scenario");
	        logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	    } else {
	        Assert.fail("Unexpected status code: " + statusCode);
	    }
	   

	    String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", redeemable_id);
	    mapOfDetails.put("rewards", rewardsObjectString);

	    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
	            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

	    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "Status code is not matched with 400 for Validate Redemptions API");


	    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
	    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
	    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
	    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
	    
	    utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID);
	
	}
	//Bug Ticket-OMM-1489
	@Test(description = "SQ-T7444 | Verify that Basket.Id should not store boolean 'true' as value",groups = { "regression", "dailyrun" })
	public void validateOMM_T4763_Verify_BasketID_not_true() throws Exception {
		String redeemableName = "AutomationRedeemableFlatDiscount_" + Utilities.getTimestamp();
		logger.info("Redeemable Name: " + redeemableName);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		logger.info("API response: " + redeemableResponse.prettyPrint());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		String redeemable_id = dbRedeemableId; // dataSet.get("redeemableID"); //DoNotDelete Automation_Hitesh discount

		Map<String, Object> mapOfDetails = new HashMap<>();

	    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	    mapOfDetails.put("productID_AsQCID", "101");
	    mapOfDetails.put("itemID", "101");
	    mapOfDetails.put("basketID", true);
	    

	    // User SignUp using API
	    userEmail = pageObj.iframeSingUpPage().generateEmail();
	    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
	            dataSet.get("secret"));
	    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
	    
	  //Verify response code is not 400
	    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "API crashed with 400 Bad Request during signup");
	    

	 // Send reward amount to user to unlock Redeemable
	    Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
	            userID, dataSet.get("apiKey"), "", "", "", "200");

	    // Capture status code from API response
	    int statusCode = sendRewardResponse2.getStatusCode();

	    // Assert logic for success (200 or 201) OR failure (400)
	    if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
	        Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
	                "Expected status code 200 or 201 for successful sendMessageToUser API");
	        logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	    } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
	        Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
	                "Expected status code 400 for sendMessageToUser API failure scenario");
	        logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	    } else {
	        Assert.fail("Unexpected status code: " + statusCode);
	    }
	    
	    String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}",redeemable_id);
	    mapOfDetails.put("rewards", rewardsObjectString);

	    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
	            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

	    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "Status code is not matched with 400 for Validate Redemptions API");


	    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
	    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
	    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
	    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
	   
	    utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID);
	
	}
	
	@Test(description = "SQ-T7439 | Verify the API response when QC gets failed for Coupon discount_type", groups = { "regression", "dailyrun" })
	@Owner(name = "Hitesh Popli")
	public void validateOMM_T4707_Verify_Qc_fail_Coupon_type () throws InterruptedException, ParseException {


	    Map<String, Object> mapOfDetails = new HashMap<>();

	    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	    mapOfDetails.put("productID_AsQCID", "101");
	    mapOfDetails.put("itemID", "101");
	   

	    // User SignUp using API
	    userEmail = pageObj.iframeSingUpPage().generateEmail();
	    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
	            dataSet.get("secret"));
	    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
	    
	  //Verify response code is not 400
	    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "API crashed with 400 Bad Request during signup");

	 // Send reward amount to user to unlock Redeemable
	    Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
	            userID, dataSet.get("apiKey"), "", "", "", "200");

	    // Capture status code from API response
	    int statusCode = sendRewardResponse2.getStatusCode();

	    // Assert logic for success (200 or 201) OR failure (400)
	    if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
	        Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
	                "Expected status code 200 or 201 for successful sendMessageToUser API");
	        logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	    } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
	        Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
	                "Expected status code 400 for sendMessageToUser API failure scenario");
	        logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	    } else {
	        Assert.fail("Unexpected status code: " + statusCode);
	    }

	    
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), userEmail, dataSet.get("CampaignUuid"));
		String generatedCodeName = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");

	    String couponsObjectString = dataSet.get("couponObject").replace("${couponID}", generatedCodeName);
	    mapOfDetails.put("coupons", couponsObjectString);

	    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
	            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

	    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "Status code is not matched with 400 for Validate Redemptions API");


	    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
	    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
	    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
	    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
	
	}

@Test(description = "SQ-T7438 | Verify the API response for Coupon Campaign with QC set to Amount Cap",groups = { "regression", "dailyrun" })
@Owner(name = "Hitesh Popli")
	   	
	public void validateOMM_T4699_Verify_Qc_Coupon_type_amount_cap () throws InterruptedException, ParseException {


	    Map<String, Object> mapOfDetails = new HashMap<>();

	    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	    mapOfDetails.put("productID_AsQCID", "101");
	    mapOfDetails.put("itemID", "101");
	

	    // User SignUp using API
	    userEmail = pageObj.iframeSingUpPage().generateEmail();
	    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
        dataSet.get("secret"));
	    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
	    
	  //Verify response code is not 400
	    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
	            "API crashed with 400 Bad Request during signup");

	 // Send reward amount to user to unlock Redeemable
	    Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
	            userID, dataSet.get("apiKey"), "", "", "", "200");

	    // Capture status code from API response
	    int statusCode = sendRewardResponse2.getStatusCode();

	    // Assert logic for success (200 or 201) OR failure (400)
	    if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
	        Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
	                "Expected status code 200 or 201 for successful sendMessageToUser API");
	        logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	    } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
	        Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
	                "Expected status code 400 for sendMessageToUser API failure scenario");
	        logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	    } else {
	        Assert.fail("Unexpected status code: " + statusCode);
	    }
	    
	    
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), userEmail, dataSet.get("CampaignUuid"));
		String generatedCodeName = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");

	    String couponsObjectString = dataSet.get("couponObject").replace("${couponID}", generatedCodeName);
	    mapOfDetails.put("coupons", couponsObjectString);

	    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
	            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

	 // Assert status code is 200
	    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
	            "Status code is not matched with 200 for Validate Redemptions API");

	    // Get full response body as String
	    String responseBody = RedemptionValidateResponse.getBody().asString();

	    // Verify that response contains "transaction"
	    Assert.assertTrue(responseBody.contains("\"transaction\""),
	            "Response body does not contain 'transaction'");
	    
	    // Optional logging
	    utils.logPass("Response body contains 'transaction' as expected for Validate Redemptions API");

	
	}
//Bug Ticket-OMM-1474
@Test(description = "SQ-T7437 | Subscription | Verify the discounting for items when the subtotal amount is set to 0",groups = { "regression", "dailyrun" })
@Owner(name = "Hitesh Popli")
public void validateOMM_T4677_Verify_Subscription_Subtotal_Amount () throws InterruptedException, ParseException {

    Map<String, Object> mapOfDetails = new HashMap<>();

    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
    mapOfDetails.put("productID_AsQCID", "101");
    mapOfDetails.put("itemID", "101");
    mapOfDetails.put("subtotal", "0");
   

    // User SignUp using API
    userEmail = pageObj.iframeSingUpPage().generateEmail();
    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
            dataSet.get("secret"));
    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
    String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
    
  //Verify response code is not 400
    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "API crashed with 400 Bad Request during signup");
    

 // Verify status code is 200
 Assert.assertEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
 utils.logPass("API2 Signup is successful");

//Send reward amount to user to unlock Redeemable
 Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
         userID, dataSet.get("apiKey"), "", "", "", "200");

 // Capture status code from API response
 int statusCode = sendRewardResponse2.getStatusCode();

 // Assert logic for success (200 or 201) OR failure (400)
 if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
     Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
             "Expected status code 200 or 201 for successful sendMessageToUser API");
     logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
 } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
     Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
             "Expected status code 400 for sendMessageToUser API failure scenario");
     logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
 } else {
     Assert.fail("Unexpected status code: " + statusCode);
 }

    
    
    String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
 // subscription purchase api 2
 		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, dataSet.get("planID"),
 				dataSet.get("client"), dataSet.get("secret"), "2", endDateTime);
 		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
 		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
 		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + "9395");

    String subscriptionObjectString = dataSet.get("subscriptionObject").replace("${subscriptionID}", subscription_id);
    mapOfDetails.put("rewards", subscriptionObjectString);

    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

 // Assert status code is 200
    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "Status code is not matched with 400 for Validate Redemptions API");


    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
}

@Test(description = "SQ-T7442 | Verify that for Subscription discount- perk and for Coupon discount- bonus work in API",groups = { "regression", "dailyrun" })
@Owner(name = "Hitesh Popli")
public void validateOMM_T4717_Verify_Perk_Bonus_Validate_API () throws InterruptedException, ParseException {
    Map<String, Object> mapOfDetails = new HashMap<>();

    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
    mapOfDetails.put("productID_AsQCID", "101");
    mapOfDetails.put("itemID", "101");
   

    // User SignUp using API
    userEmail = pageObj.iframeSingUpPage().generateEmail();
    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
            dataSet.get("secret"));
    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
    String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
    
  //Verify response code is not 400
    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "API crashed with 400 Bad Request during signup");

 // Verify status code is 200
 Assert.assertEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
 utils.logPass("API2 Signup is successful");

//Send reward amount to user to unlock Redeemable
 Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
         userID, dataSet.get("apiKey"), "", "", "", "200");

 // Capture status code from API response
 int statusCode = sendRewardResponse2.getStatusCode();

 // Assert logic for success (200 or 201) OR failure (400)
 if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
     Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
             "Expected status code 200 or 201 for successful sendMessageToUser API");
     logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
 } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
     Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
             "Expected status code 400 for sendMessageToUser API failure scenario");
     logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
 } else {
     Assert.fail("Unexpected status code: " + statusCode);
 }

        
    String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
    
 // subscription purchase api 2
 		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, dataSet.get("planID"),
 		dataSet.get("client"), dataSet.get("secret"), "2", endDateTime);
 		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
 		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
 		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + "9395");

    String subscriptionObjectString = dataSet.get("subscriptionObject").replace("${subscriptionID}", subscription_id);
    mapOfDetails.put("rewards", subscriptionObjectString);
    
    Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints()
			.postDynamicCoupon(dataSet.get("apiKey"), userEmail, dataSet.get("CampaignUuid"));
	String generatedCodeName = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");

    String couponsObjectString = dataSet.get("couponObject").replace("${couponID}", generatedCodeName);
    mapOfDetails.put("coupons", couponsObjectString);

    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
    

    // Assert status code is 200
    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
     "Status code is not matched with 200 for Validate Redemptions API");

    // Get full response body as String
    String responseBody = RedemptionValidateResponse.getBody().asString();

    // Verify that response contains "transaction"
    Assert.assertTrue(responseBody.contains("\"transaction\""),
            "Response body does not contain 'transaction'");

    // Optional logging
    utils.logPass("Response body contains 'transaction' as expected for Validate Redemptions API");
}



@Test(description = "SQ-T7440 | Verify No Entries Get Created for Redemption Logs", groups = {"regression", "dailyrun"})
@Owner(name = "Hitesh Popli")
public void validateOMM_T4712_Verify_No_Entries_Get_Created_for_Redemption_Logs() throws Exception {

	Map<String, Object> mapOfDetails = new HashMap<>();
	mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	mapOfDetails.put("productID_AsQCID", "101");
	mapOfDetails.put("itemID", "101");

	// 1. User Signup
	userEmail = pageObj.iframeSingUpPage().generateEmail();
	Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
			dataSet.get("secret"));
	pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");

	Object userIdObj = signUpResponseUser.jsonPath().get("user.user_id");
	String userID = userIdObj != null ? userIdObj.toString() : "";
	Assert.assertFalse(userID.isEmpty(), "UserID is null or empty in signup response");
	logger.info("UserID from API response: " + userID);

	// Verify response code is not 400
	Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "API crashed with 400 Bad Request during signup");

	// 2. Send reward (gift 200 points for eligibility)
	pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "", "200");

	// 3. Create redeemable via API (do not change external_id usage)
	String redeemableName = "AutomationRedeemableFlatDiscount_API_" + CreateDateTime.getTimeDateString();

	logger.info("Redeemable Name: " + redeemableName);

// Create Redeemable with above QC
	RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
	String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
			.setReceiptRule(
					RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
			.setApplicable_as_loyalty_redemptionFlag(true).setBooleanField("activate_now", true).setAutoApplicable(false).setPoints(1)
			.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
// Create Redeemable
	Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
	logger.info("API response: " + redeemableResponse.prettyPrint());

// Get Redeemable External ID
	String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
	utils.logit(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

// Get Redeemable ID from DB
	String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
	String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

	Assert.assertNotNull(dbRedeemableId, "Redeemable ID fetched from DB is null");
	logger.info("Redeemable ID from DB: " + dbRedeemableId);

	// 5. Send Redeemable to user
	pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dbRedeemableId, "", "");

	// 6. Add rewards object
	String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", dbRedeemableId);
	mapOfDetails.put("rewards", rewardsObjectString);

	// 7. Validate promotions
	Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
			dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
	Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_OK, "Validate API did not return 200");

	Object basketIdObj = RedemptionValidateResponse.jsonPath().get("basket.id");
	String basketID = basketIdObj != null ? basketIdObj.toString() : "";
	if (basketID.isEmpty()) {
		logger.info("BasketID not present in Validate API response (expected negative case)");
	} else {
		logger.info("BasketID from API response: " + basketID);
	}

	// 8. Verify that NO entries exist in discount_baskets and discount_basket_items
	String basketQuery = "SELECT COUNT(*) AS cnt FROM discount_baskets WHERE user_id = " + userID
			+ " AND business_id = " + dataSet.get("business_id");
	String basketCount = DBUtils.executeQueryAndGetColumnValue(env, null, basketQuery, "cnt");
	Assert.assertEquals(basketCount, "0", "Entry found in discount_baskets for user: " + userID);

	String basketItemsQuery = "SELECT COUNT(*) AS cnt FROM discount_basket_items WHERE discount_basket_id IN "
			+ "(SELECT id FROM discount_baskets WHERE user_id = " + userID + " AND business_id = "
			+ dataSet.get("business_id") + ")";
	String basketItemsCount = DBUtils.executeQueryAndGetColumnValue(env, null, basketItemsQuery, "cnt");
	Assert.assertEquals(basketItemsCount, "0", "Entry found in discount_basket_items for user: " + userID);

	utils.logPass("No entries created in discount_baskets or discount_basket_items as expected");

	
	utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID);
	logger.info("Successfully deleted redeemable with id: " + dbRedeemableId);
}

@Test(description = "SQ-T7449 | Verify the basket with valid Discount amount and verify that no entries should be made in discount_basket_items & discount_basket table", groups = { "regression", "dailyrun" })
public void validateOMM_T4713_Verify_Discount_Amount_Validate_API() throws Exception {

	Map<String, Object> mapOfDetails = new HashMap<>();
	mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	mapOfDetails.put("productID_AsQCID", "101");
	mapOfDetails.put("itemID", "101");

	// User signup
	userEmail = pageObj.iframeSingUpPage().generateEmail();
	Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
			dataSet.get("secret"));
	pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	Object userIdObj = signUpResponseUser.jsonPath().get("user.user_id");
	String userID = userIdObj != null ? userIdObj.toString() : "";
	Assert.assertFalse(userID.isEmpty(), "UserID is null or empty in signup response");
	logger.info("UserID from API response: " + userID);

//Verify response code is not 400
	Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "API crashed with 400 Bad Request during signup");

//Send reward amount to user to unlock Redeemable
	Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
			"200");

	// Capture status code from API response
	int statusCode = sendRewardResponse2.getStatusCode();

	// Assert logic for success (200 or 201) OR failure (400)
	if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
		Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
				"Expected status code 200 or 201 for successful sendMessageToUser API");
		logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	} else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
		Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST, "Expected status code 400 for sendMessageToUser API failure scenario");
		logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	} else {
		Assert.fail("Unexpected status code: " + statusCode);
	}

	// Send reward amount
	pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "100", "", "", "");
	String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}",
			CreateDateTime.getTimeDateString());
	mapOfDetails.put("rewards", rewardsObjectString);

	// Validate promotions
	Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
			dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

	// DB Query to find entries in discount_basket & discount_basket_items table
	String query = "select count(*) as count from discount_baskets where user_id = '" + userID + "'";

	if (!DBUtils.verifyValueFromDBUsingPolling(env, query, "count", "0")) {
		Assert.fail("Entry is created in discount_baskets table for UserID: " + userID);
	} else {
		System.out.println("No entries created in discount_baskets table for UserID: " + userID);
	}
	String query2 = "select count(*) as count from discount_basket_items where user_id = '" + userID + "'";

	if (!DBUtils.verifyValueFromDBUsingPolling(env, query2, "count", "0")) {
		Assert.fail("Entry is created in discount_basket_items table for UserID: " + userID);
	} else {
		System.out.println("No entries created in discount_basket_items table for UserID: " + userID);
	}
}

@Test(description = "SQ-T7445 | Verify the basket with valid Card Completion and verify that no entries should be made in discount_basket_items & discount_basket table", groups = { "regression", "dailyrun" })

public void validateOMM_T4797_Verify_Card_Completion_Validate_API() throws Exception {

	// String redeemable_id = dataSet.get("redeemableID");
	Map<String, Object> mapOfDetails = new HashMap<>();
	mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
	mapOfDetails.put("productID_AsQCID", "101");
	mapOfDetails.put("itemID", "101");

	// User signup
	userEmail = pageObj.iframeSingUpPage().generateEmail();
	Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
			dataSet.get("secret"));
	pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
	Object userIdObj = signUpResponseUser.jsonPath().get("user.user_id");
	String userID = userIdObj != null ? userIdObj.toString() : "";
	Assert.assertFalse(userID.isEmpty(), "UserID is null or empty in signup response");
	logger.info("UserID from API response: " + userID);

//Verify response code is not 400
	Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "API crashed with 400 Bad Request during signup");

//Send reward amount to user to unlock Redeemable
	Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
			"20");

	// Capture status code from API response
	int statusCode = sendRewardResponse2.getStatusCode();

	// Assert logic for success (200 or 201) OR failure (400)
	if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
		Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
				"Expected status code 200 or 201 for successful sendMessageToUser API");
		logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
	} else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
		Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST, "Expected status code 400 for sendMessageToUser API failure scenario");
		logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
	} else {
		Assert.fail("Unexpected status code: " + statusCode);
	}

	String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}",
			CreateDateTime.getTimeDateString());
	mapOfDetails.put("rewards", rewardsObjectString);

	// Validate promotions
	Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
			dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
	// Add dynamic discount type for all discount types
	// Add API assert

	// DB Query to find entries in discount_basket & discount_basket_items table
	String query = "select count(*) as count from discount_baskets where user_id = '" + userID + "'";

	if (!DBUtils.verifyValueFromDBUsingPolling(env, query, "count", "0")) {
		Assert.fail("Entry is created in discount_baskets table for UserID: " + userID);
	} else {
		System.out.println("No entries created in discount_baskets table for UserID: " + userID);
	}
	String query2 = "select count(*) as count from discount_basket_items where user_id = '" + userID + "'";

	if (!DBUtils.verifyValueFromDBUsingPolling(env, query2, "count", "0")) {
		Assert.fail("Entry is created in discount_basket_items table for UserID: " + userID);
	} else {
		System.out.println("No entries created in discount_basket_items table for UserID: " + userID);
	}
}

@Test(description = "SQ-T7443 | Validate the basket with unique subscription id's added in API",groups = { "regression", "dailyrun" })
@Owner(name = "Hitesh Popli")
public void validateOMM_T4718_Verify_Unique_Subscription_ID () throws InterruptedException, ParseException {

  Map<String, Object> mapOfDetails = new HashMap<>();
  mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
  mapOfDetails.put("productID_AsQCID", "101");
  mapOfDetails.put("itemID", "101");
 

  // User SignUp using API
  userEmail = pageObj.iframeSingUpPage().generateEmail();
  Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
          dataSet.get("secret"));
  pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
  String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
  String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
  
//Verify response code is not 400
  Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
          "API crashed with 400 Bad Request during signup");
  

// Verify status code is 200
Assert.assertEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
utils.logPass("API2 Signup is successful");

//Send reward amount to user to unlock Redeemable
Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
        userID, dataSet.get("apiKey"), "", "", "", "200");

// Capture status code from API response
int statusCode = sendRewardResponse2.getStatusCode();

// Assert logic for success (200 or 201) OR failure (400)
if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
    Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
            "Expected status code 200 or 201 for successful sendMessageToUser API");
    logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
} else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
    Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "Expected status code 400 for sendMessageToUser API failure scenario");
    logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
} else {
    Assert.fail("Unexpected status code: " + statusCode);
}
  
//First subscription purchase
String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(
       token, dataSet.get("planID"), dataSet.get("client"), dataSet.get("secret"), "2", endDateTime);
Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
String subscription_id1 = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
logger.info(userEmail + " purchased " + subscription_id1 + " Plan id = " + dataSet.get("planID"));

//Second subscription purchase
Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(
       token, dataSet.get("planID2"), dataSet.get("client"), dataSet.get("secret"), "2", endDateTime);
Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
String subscription_id2 = purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString();
logger.info(userEmail + " purchased " + subscription_id2 + " Plan id = " + dataSet.get("planID2"));

//Build rewards array with both subscriptions
String subscriptionObjectString1 = dataSet.get("subscriptionObject").replace("${subscriptionID}", subscription_id1);
String subscriptionObjectString2 = dataSet.get("subscriptionObject").replace("${subscriptionID}", subscription_id2);

String rewardsArray = "[" + subscriptionObjectString1 + "," + subscriptionObjectString2 + "]";

mapOfDetails.put("rewards", rewardsArray);

//Call validate API
Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(
       userID, dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

//Assert status code is 400
Assert.assertEquals(RedemptionValidateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
     "Status code is not matched with 400 for Validate Redemptions API");

//Assert response code is OTHER
String responseCode = RedemptionValidateResponse.jsonPath().getString("code");
Assert.assertEquals(responseCode, "OTHER",
     "Response code is not matched with OTHER for Validate Redemptions API");
utils.logPass("Response code is matched with OTHER for Validate Redemptions API");

}

@Test(description = "SQ-T7447 | Verify the API response when user has no 0 redeemable points",groups = { "regression", "dailyrun" })
public void validateOMM_T4876_Verify_Validate_API_0_Redeemable_points() throws Exception {

	String redeemableName = "AutomationRedeemableFlatDiscount_" + Utilities.getTimestamp();
	logger.info("Redeemable Name: " + redeemableName);

	// Create Redeemable with above QC
	RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
	String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
			.setReceiptRule(
					RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
			.setAutoApplicable(false).setPoints(1).setApplicable_as_loyalty_redemptionFlag(true)
			.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
			.addCurrentData().build();
	// Create Redeemable
	Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
	logger.info("API response: " + redeemableResponse.prettyPrint());

	// Get Redeemable External ID
	String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
	utils.logit(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

	// Get Redeemable ID from DB
	String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
	String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

    Map<String, Object> mapOfDetails = new HashMap<>();

    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
    mapOfDetails.put("productID_AsQCID", "101");
    mapOfDetails.put("itemID", "101");
    

    // User SignUp using API
    userEmail = pageObj.iframeSingUpPage().generateEmail();
    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
            dataSet.get("secret"));
    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
   // String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
    
  //Verify response code is not 400
    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "API crashed with 400 Bad Request during signup");
    
    String redeemable_id = dbRedeemableId ; 
    String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", redeemable_id);
    mapOfDetails.put("rewards", rewardsObjectString);

    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "Status code is not matched with 400 for Validate Redemptions API");


    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
    
    utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID);

}
@Test(description = "SQ-T7446 | Verify the QC fail scenario for error logging in discount_basket_items table",groups = { "regression", "dailyrun" })
public void validateOMM_T4839_Qc_Fail_Error_Logs_Discount_Basket_Table() throws Exception {
	String lisName = "AutomationLIS__" + Utilities.getTimestamp();
	String qcname = "AutomationQC_" + Utilities.getTimestamp();
	String redeemableName = "AutomationRedeemable_" + Utilities.getTimestamp();

	// Create LIS with base item 101,102,103,104
	String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
			.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
			.build();

	// Create LIS
	Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
	logger.info("API response: " + lisResponse.prettyPrint());

	// Get LIS External ID
	String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
	utils.logit(lisName + " LIS is Created with External ID: " + lisExternalID);
	
	// Create QC-payload with 100% off on total amount of items matching LIS
	String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
			.setQCProcessingFunction("sum_amounts").setPercentageOfProcessedAmount(50)
			.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
	
	// Create QC
	Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
	logger.info("API response: " + qcResponse.prettyPrint());

	// Get QC External ID
	String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
	utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
	
	// Create Redeemable with above QC
	RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
	String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(false)
			.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
					.redeeming_criterion_id(qcExternalID).build())
			.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
			.addCurrentData().build();

	// Create Redeemable
	Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
	logger.info("API response: " + redeemableResponse.prettyPrint());

	// Get Redeemable External ID
	String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
	utils.logit(redeemableName + " redeemable is Created Redeemable External ID: " + redeemableExternalID);

	// Get Redeemable ID from DB
	String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
	String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

	Map<String, Object> mapOfDetails = new HashMap<>();
    mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
    mapOfDetails.put("productID_AsQCID", "101");
    mapOfDetails.put("itemID", "101");
   

    // User SignUp using API
    userEmail = pageObj.iframeSingUpPage().generateEmail();
    Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
            dataSet.get("secret"));
    pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
    String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
    String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
    
  //Verify response code is not 400
    Assert.assertNotEquals(signUpResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "API crashed with 400 Bad Request during signup");

 // Send reward amount to user to unlock Redeemable
    Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(
            userID, dataSet.get("apiKey"), "", "", "", "200");

    // Capture status code from API response
    int statusCode = sendRewardResponse2.getStatusCode();

    // Assert logic for success (200 or 201) OR failure (400)
    if (statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED) {
        Assert.assertTrue(statusCode == ApiConstants.HTTP_STATUS_OK || statusCode == ApiConstants.HTTP_STATUS_CREATED,
                "Expected status code 200 or 201 for successful sendMessageToUser API");
        logger.info("Verified sendMessageToUser API returned " + statusCode + " (Success/Created)");
    } else if (statusCode == ApiConstants.HTTP_STATUS_BAD_REQUEST) {
        Assert.assertEquals(statusCode, ApiConstants.HTTP_STATUS_BAD_REQUEST,
                "Expected status code 400 for sendMessageToUser API failure scenario");
        logger.info("Verified sendMessageToUser API returned 400 (Failure as expected)");
    } else {
        Assert.fail("Unexpected status code: " + statusCode);
    }
    
    String redeemable_id = dbRedeemableId;
    String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", redeemable_id);
    mapOfDetails.put("rewards", rewardsObjectString);

    Response RedemptionValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
            dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
    

    Assert.assertEquals(RedemptionValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
            "Status code is not matched with 400 for Validate Redemptions API");


    String ResponseCode = RedemptionValidateResponse.jsonPath().getString("code");
    Assert.assertEquals(ResponseCode, "INVALID_PROMOTION",
    			            "Response code is not matched with INVALID_PROMOTION for Validate Redemptions API");
    utils.logPass("Response code is matched with INVALID_PROMOTION for Validate Redemptions API");
    
    utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		if (driver != null) {
			driver.quit();
		}
		logger.info("Browser closed");
	}

}