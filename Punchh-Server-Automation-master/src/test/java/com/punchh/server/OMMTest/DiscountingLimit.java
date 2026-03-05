package com.punchh.server.OMMTest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountingLimit {

    private static Logger logger = LogManager.getLogger(DiscountingLimit.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String env;
    private String baseUrl;
    private String userEmail;
    private static Map<String, String> dataSet;
    String run = "ui";
    private List<String> codeNameList;
    private boolean GlobalBenefitRedemptionThrottlingToggle;
    ApiUtils apiUtils;
    String externalUID;
    private String spName, PlanID, endDateTime, spPrice;
    Utilities utils;

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
        GlobalBenefitRedemptionThrottlingToggle = false;
        codeNameList = new ArrayList<String>();
        apiUtils = new ApiUtils();
        externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
        utils = new Utilities(driver);
    }

    @Test(description = "SQ-T5905 Mobile>Exact Strategy>Verify that when the subscription plan exhausts its Primary limit and the Secondary Discounting limit is available in the Benefit plan, the max_applicable_quantity is displayed based on the secondary discounting limit", groups = {
            "regression", "dailyrun" }, priority = 1)
    @Owner(name = "Vansham Mishra")
    public void T5905_verifyMaxApplicableQuantityWithSecondaryDiscountingLimit() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Navigate to Multiple Redemption Tab and check the flags
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify
        // max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, "12003", "10", externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS discount lookup API");

        // step4- Hit Pos batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step5 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in add discount to basket API");
        utils.longWaitInSeconds(7);

        // step6- Hit GET active basket API using Mobile API and verify
        // max_applicable_quantity value-10 in API response
        Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity6, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in GET active basket using Mobile API");

        // step7 - Hit discount lookup API with input receipt with qty-10
        map.put("item_qty", "10");
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response posDiscountLookupResponse2;

        do {
            posDiscountLookupResponse2 = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                    userID, "12003", "10", externalUID, map);
            if (posDiscountLookupResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity_posDiscountLookupResponse2 = posDiscountLookupResponse2.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity_posDiscountLookupResponse2, "10.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in POS discount lookup API");

        // step8 - Hit auth batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "10");
        Response batchRedemptionProcessResponseUser2;
        retryCount = 0; // Reset retry count
        do {
            batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                    dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                    externalUID,map);
            if (batchRedemptionProcessResponseUser2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "10.0", "max_applicable_quantity value is not matched with 10.0");
        utils.logPass(
                "Verified that max_applicable_quantity value is 10.0 to exhaust partial secondary limit in Auth batch redemption API");

    }
    @Test(description = "SQ-T5960 Point to Manual Business> Validate that User is able to add discount_amount using Auto Select API if it is getting qualified", groups = {
            "regression", "dailyrun" }, priority = 2)
    @Owner(name = "Vansham Mishra")
    public void T5960_validateDiscountAmountUsingAutoSelectAPI() throws InterruptedException {
        // login to instance
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // send reward amount to user Reedemable
        Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60", "", "",
                "");
        TestListeners.extentTest.get().pass("Send redeemable to the user successfully");
        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
                "Status code 201 did not matched for api2 send message to user");
        TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
        utils.logPass("Send amount to the user successfully");

        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // Auth Auto select API
        Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
                dataSet.get("secret"), token, "20", externalUID, parentMap);
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
                "Status code 200 did not match with add discount to basket ");
        TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
        String discount_type = redemptionResponse.jsonPath().get("discount_basket_items[0].discount_type").toString();
        Assert.assertEquals(discount_type, "discount_amount", "Discount type is not matched at 0th index");
        utils.logPass("Verified that Discount Amount gets added to discount_basket using Auto Select API");
        utils.logPass("Verified that User is able to add discount_amount using Auto Select API if it is getting qualified for Point to Manual Business");
    }
    @Test(description = "SQ-T5904 POS>Exact Strategy>Verify that when the subscription plan exhausts its Primary limit and the Secondary Discounting limit is available in the Benefit plan, the max_applicable_quantity is displayed based on the secondary discounting limit", groups = {
            "regression", "dailyrun" }, priority = 1)
    @Owner(name = "Vansham Mishra")
    public void T5904_verifyMaxApplicableQuantityWithSecondaryDiscountingLimit() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Navigate to Multiple Redemption Tab and check the flags
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify
        // max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, "12003", "10", externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS discount lookup API");

        // step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints()
                .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step5 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in add discount to basket API");
        utils.longWaitInSeconds(7);

        // step6- Hit GET active basket API using Mobile API and verify
        // max_applicable_quantity value-10 in API response
        Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity6, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in GET active basket using Mobile API");

        // step7 - Hit discount lookup API with input receipt with qty-10
        map.put("item_qty", "10");
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response posDiscountLookupResponse2;

        do {
            posDiscountLookupResponse2 = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                    userID, "12003", "10", externalUID, map);
            if (posDiscountLookupResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity_posDiscountLookupResponse2 = posDiscountLookupResponse2.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity_posDiscountLookupResponse2, "10.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in POS discount lookup API");

        // step8 - Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "10");
        Response batchRedemptionProcessResponseUser2;
        retryCount = 0; // Reset retry count

        do {
            batchRedemptionProcessResponseUser2 = pageObj.endpoints()
                    .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
            if (batchRedemptionProcessResponseUser2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            Thread.sleep(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "10.0", "max_applicable_quantity value is not matched with 10.0");
        utils.logPass(
                "Verified that max_applicable_quantity value is 10.0 to exhaust partial secondary limit in POS batch redemption API");

    }
    @Test(description = "SQ-T5906 Auth>Exact Strategy>Verify that when the subscription plan exhausts its Primary limit and Secondary limit both in the Benefit plan, the max_applicable_quantity=0 is displayed in API response", groups = {
            "regression", "dailyrun" }, priority = 1)
    @Owner(name = "Vansham Mishra")
    public void T5906_verifyMaxApplicableQuantityWithSecondaryDiscountingLimit() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Navigate to Multiple Redemption Tab and check the flags
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        //Step1- Add subscription in discount basket using Auth API and verify max_applicable_quantity value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, "12003", "10", externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS discount lookup API");

        // step4- Hit Auth batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step5 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in add discount to basket API");
        utils.longWaitInSeconds(7);

        // step6- Hit GET active basket API using Mobile API and verify
        // max_applicable_quantity value-10 in API response
        Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity6, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in GET active basket using Mobile API");

        // step7 - Hit discount lookup API with input receipt with qty-10
        map.put("item_qty", "10");
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response posDiscountLookupResponse2;

        do {
            posDiscountLookupResponse2 = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                    userID, "12003", "10", externalUID, map);
            if (posDiscountLookupResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity_posDiscountLookupResponse2 = posDiscountLookupResponse2.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity_posDiscountLookupResponse2, "10.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in POS discount lookup API");

        // step8 - Hit Auth batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "10");
        Response batchRedemptionProcessResponseUser2;
        retryCount = 0; // Reset retry count

        do {
            batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                    dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                    externalUID,map);
            if (batchRedemptionProcessResponseUser2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            Thread.sleep(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "10.0", "max_applicable_quantity value is not matched with 10.0");
        utils.logPass(
                "Verified that max_applicable_quantity value is 10.0 to exhaust partial secondary limit in POS batch redemption API");

        //step9 - Add subscription plan again in discount basket using Mobile Select API and verify max_applicable_quantity value in API response
        retryCount = 0; // Reset retry count
        Response discountBasketResponse3;

        do {
            discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                    dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
            if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            Thread.sleep(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity8 =  discountBasketResponse3.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity8, "0", "max_applicable_quantity value is not matched with null");
        utils.logPass("Verified that max_applicable_quantity value is 0 in add Subscription to basket API");

    }
    @Test(description = "SQ-T5897 POS>Extended>Validate the Calculation for Max Applicable Quantity for Primary Rule" +
            "SQ-T5898 (1.0) Auth>Extended>Validate the Calculation for Max Applicable Quantity for Primary Rule" +
            "SQ-T5888 (1.0) Auth>QC>Validate the max_applicable_quantity value when Benefit Discounting Rule is configured with Rate Rollback", groups = {
            "regression", "dailyrun" }, priority = 1)
    @Owner(name = "Vansham Mishra")
    public void T5897_validateMaxApplicableQuantityForPrimaryRule() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Navigate to Multiple Redemption Tab and check the flags
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "3");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        //Step1- Add subscription in discount basket using Auth API and verify max_applicable_quantity value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2- Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints()
                .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "3.0", "max_applicable_quantity value is not matched with 3");
        utils.logPass("Verified that max_applicable_quantity value is 3.0 in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step2 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response discountBasketResponse2;

        do {
            discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                    dataSet.get("secret"), "subscription", subscription_id + "");
            if (discountBasketResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "2.0", "max_applicable_quantity value is not matched with 2");
        utils.logPass("Verified that max_applicable_quantity value is 2.0 in add discount to basket API");
        utils.longWaitInSeconds(7);


        // step4 - Hit discount lookup API with input receipt with qty-10
        map.put("item_qty", "3");
        retryCount = 0; // Reset retry count
        Response posDiscountLookupResponse2;

        do {
            posDiscountLookupResponse2 = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                    userID, "12003", "10", externalUID, map);
            if (posDiscountLookupResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity_posDiscountLookupResponse2 = posDiscountLookupResponse2.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity_posDiscountLookupResponse2, "3.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 3.0 in POS discount lookup API");

        // step5 - Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "3");
        Response batchRedemptionProcessResponseUser2;
        retryCount = 0; // Reset retry count

        do {
            batchRedemptionProcessResponseUser2 = pageObj.endpoints()
                    .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
            if (batchRedemptionProcessResponseUser2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "3.0", "max_applicable_quantity value is not matched with 3.0");
        utils.logPass(
                "Verified that max_applicable_quantity value is 3.0 to exhaust partial secondary limit in POS batch redemption API");


    }
    @Test(description = "SQ-T5899 POS>Exact Strategy>Validate that Calculation logic-(threshold - discounted_quantity) gets applied for secondary rule When Primary Rule Threshold is Reached", groups = {
            "regression", "dailyrun" }, priority = 1)
    @Owner(name = "Vansham Mishra")
    public void T5899_validateMaxApplicableQuantityForPrimaryRule() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        // Navigate to Multiple Redemption Tab and check the flags
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        //Step1- Add subscription in discount basket using Auth API and verify max_applicable_quantity value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2 - Hit GET active basket using Auth API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using AUTH API");

        // step3- Hit Auth batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 3");
        utils.logPass("Verified that max_applicable_quantity value is 3.0 in AUTH batch redemption API");
        utils.longWaitInSeconds(7);

        // step4 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response discountBasketResponse2;

        do {
            discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
                    dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
            if (discountBasketResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 2");
        utils.logPass("Verified that max_applicable_quantity value is 2.0 in add discount to basket API");
        utils.longWaitInSeconds(7);


        // step5 - Hit discount lookup API with input receipt with qty-10
        map.put("item_qty", "5");
        retryCount = 0; // Reset retry count
        Response posDiscountLookupResponse2;

        do {
            posDiscountLookupResponse2 = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                    userID, "12003", "10", externalUID, map);
            if (posDiscountLookupResponse2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(posDiscountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity_posDiscountLookupResponse2 = posDiscountLookupResponse2.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity_posDiscountLookupResponse2, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 3.0 in POS discount lookup API");

        // step6 - Hit Auth batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "5");
        Response batchRedemptionProcessResponseUser2;
        retryCount = 0; // Reset retry count

        do {
            batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                    dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                    externalUID,map);
            if (batchRedemptionProcessResponseUser2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            utils.longWaitInMiliSeconds(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,"Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "5.0", "max_applicable_quantity value is not matched with 3.0");
        utils.logPass(
                "Verified that max_applicable_quantity value is 3.0 to exhaust partial secondary limit in AUTH batch redemption API");


    }
    @Test(description = "SQ-T5907 Mobile>Exact Strategy>Verify that when the subscription plan exhausts its Primary limit and Secondary limit both in the Benefit plan, the max_applicable_quantity=0 is displayed in API response", groups = {
            "regression", "dailyrun" }, priority = 2)
    @Owner(name = "Vansham Mishra")
    public void T5907_verifyMaxApplicableQuantityIsZeroWhenLimitsExhausted() throws InterruptedException {
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify
        // max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5 in get active basket api");
        utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, "12003", "10", externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, "5.0", "max_applicable_quantity value is not matched with 5 in discount lookup api");;
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS discount lookup API");

        // step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints()
                .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 5 in batch redemption api");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step5 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 10");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in add discount to basket API");
        utils.longWaitInSeconds(7);

        // step6- Hit GET active basket API using Mobile API and verify
        // max_applicable_quantity value-10 in API response
        Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity6, "10.0", "max_applicable_quantity value is not matched with 10 in get active basket api");
        utils.logPass("Verified that max_applicable_quantity value is 10.0 in GET active basket using Mobile API");

        // step7 - Hit auth batch redemption API with input receipt with qty-5 to exhaust
        // partial secondary limit
        map.put("item_qty", "10");
        Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity7, "10.0", "max_applicable_quantity value is not matched with 10.0 in auth batch redemption api");
        utils.logPass(
                "Verified that max_applicable_quantity value is 10.0 to exhaust partial secondary limit in auth batch redemption API");

        // step8 - Add subscription plan again in discount basket using Mobile Select
        // API and verify max_applicable_quantity value in API response
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response discountBasketResponse3;

        do {
            discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                    dataSet.get("secret"), "subscription", subscription_id + "");
            if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            Thread.sleep(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);
        Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity8 = discountBasketResponse3.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity8, "0", "max_applicable_quantity value is not matched with 0");
        utils.logPass("Verified that max_applicable_quantity value is 0 in add discount to basket API");
    }
    @Test(description = "SQ-T5887 POS>QC>Validate the max_applicable_quantity value when Benefit Discounting Rule is configured with Rate Rollback" +
            "SQ-T5890 POS>QC>Validate the max_applicable_quantity value when QC Max Discounted Unit Overrides Discounting Limit from Subscription", groups = {
            "regression", "dailyrun" }, priority = 2)
    @Owner(name = "Vansham Mishra")
    public void T5887_validateMaxApplicableQuantityWithRateRollback() throws InterruptedException {
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify
        // max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, dataSet.get("subscriptionMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        String discount_basket_item_id = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_basket_item_id").toString();
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);


        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, dataSet.get("subscriptionMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 5 in get active basket api");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, dataSet.get("item_id"), dataSet.get("spPrice"), externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, dataSet.get("qcMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 1 in discount lookup api");;
        utils.logPass("Verified that max_applicable_quantity value is 1 in POS discount lookup API");

        // step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints()
                .processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, dataSet.get("spPrice"), dataSet.get("item_id"), externalUID, map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, dataSet.get("qcMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 1 in batch redemption api");
        utils.logPass("Verified that max_applicable_quantity value is 1 in POS batch redemption API");
        utils.longWaitInSeconds(7);

    }
    @Test(description = "SQ-T5891 Auth>QC>Validate the max_applicable_quantity value when QC Max Discounted Unit Overrides Discounting Limit from Subscription" +
            "SQ-T5889 Mobile>QC>Validate the max_applicable_quantity value when Benefit Discounting Rule is configured with Rate Rollback", groups = {
            "regression", "dailyrun" }, priority = 2)
    @Owner(name = "Vansham Mishra")
    public void T5891_validateMaxApplicableQuantityWithRateRollback() throws InterruptedException {
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify
        // max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        // subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity, dataSet.get("subscriptionMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 5");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
        String discount_basket_item_id = discountBasketResponse.jsonPath()
                .get("discount_basket_items[0].discount_basket_item_id").toString();
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);


        // step2 - Hit GET active basket using Mobile API and verify
        // max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
                .get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity2, dataSet.get("subscriptionMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 5 in get active basket api");
        utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
                userID, dataSet.get("item_id"), dataSet.get("spPrice"), externalUID, map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
                .get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity3, dataSet.get("qcMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 1 in discount lookup api");;
        utils.logPass("Verified that max_applicable_quantity value is 1 in POS discount lookup API");

        // step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
        // primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionsAUTHAPI(
                dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
                externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
                .get("success[0].discount_details.max_applicable_quantity").toString();
        Assert.assertEquals(max_applicable_quantity4, dataSet.get("qcMaxApplicableQuantity"), "max_applicable_quantity value is not matched with 1 in batch redemption api");
        utils.logPass("Verified that max_applicable_quantity value is 1 in Auth batch redemption API");
        utils.longWaitInSeconds(7);

    }
    @Test(description = "SQ-T5902 Validate that max_applicable quantity-NULL when benefit discounting rule is configured with % or $ Off" +
            "SQ-T5903 Validate that max_applicable quantity-NULL when benefit discounting rule is configured with % or $ Off (Receipt level Discount)", groups = {
            "regression"}, priority = 3)
    @Owner(name = "Vansham Mishra")
    public void T5902_validateMaxApplicableQuantityIsNullForPercentageOrDollarOffDiscountSubscription() throws InterruptedException {
        String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_qty", "5");
        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        utils.logPass("API2 Signup is successful");

        // step1 - Add subscription in discount basket using Mobile API and verify max_applicable_quantity-5 value in API response
        String PlanID = dataSet.get("planId");
        String spPrice = dataSet.get("spPrice");
        String  externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        /// subscription purchase api 2
        Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
                dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
        Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
        String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
        logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//        String errorMessage = discountBasketResponse.jsonPath().get("errors.discount_id[0]").toString();
//        Assert.assertEquals(errorMessage, "Offer configuration is not supported.",
//                "Error message did not match for subscription with percentage or dollar off discount");




        // commenting below as the fix of omm-1265
        String max_applicable_quantity =  discountBasketResponse.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity,  "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is null in add discount to basket API");
        Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
        Map<String, String> detailsMap1 = new HashMap<String, String>();

        detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", dataSet.get("spPrice"), "M", "10", "999", "1",
                dataSet.get("item_id"));
        parentMap.put("Pizza1", detailsMap1);

        //step2 - Hit GET active basket using Mobile API and verify max_applicable_quantity value-5 in API response
        Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity2 =  basketDiscountDetailsResponse.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity2, "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

        // step3 - Hit discount lookup API with input receipt with qty-5
        Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"), userID, "12003","10",externalUID,map);
        Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity3 =  posDiscountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity3, "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in POS discount lookup API");

        //step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust primary limit
        Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionPosApi(
                dataSet.get("locationkey"), userID, "10", "12003", externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity4 =  batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity4,  "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in POS batch redemption API");
        utils.longWaitInSeconds(7);

        // step5 - Add subscription plan again in discount basket using Mobile Select API and verify max_applicable_quantity value-10 in API response
        // Adding subscription into discount basket
        String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                dataSet.get("secret"), "subscription", subscription_id + "");
        Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        String max_applicable_quantity5 =  discountBasketResponse2.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity5,  "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
        utils.longWaitInSeconds(7);

        //step6- Hit GET active basket API using Mobile API and verify max_applicable_quantity value-10 in API response
        Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
                dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
                "Status code 200 did not match with get user discount basket details");
        String max_applicable_quantity6 =  basketDiscountDetailsResponse2.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity6, "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

        // step7 - Hit POS batch redemption API with input receipt with qty-5 to exhaust partial secondary limit
        map.put("item_qty", "10");
        Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApi(
                dataSet.get("locationkey"), userID, "10", "12003", externalUID,map);
        Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for POS API Process Batch Redemption.");
        String max_applicable_quantity7 =  batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity7, "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null to exhaust partial secondary limit in POS batch redemption API");

        //step8 - Add subscription plan again in discount basket using Mobile Select API and verify max_applicable_quantity value in API response
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 2000; // Interval between retries in milliseconds
        int retryCount = 0;
        Response discountBasketResponse3;

        do {
            discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
                    dataSet.get("secret"), "subscription", subscription_id + "");
            if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
                break; // Exit the loop if status code is 200
            }
            retryCount++;
            Thread.sleep(retryInterval); // Wait before retrying
        } while (retryCount < maxRetries);

        Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Failed to get 200 status code after " + maxRetries + " retries");
        String max_applicable_quantity8 =  discountBasketResponse3.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
        Assert.assertNull(max_applicable_quantity8, "max_applicable_quantity value is not Null");
        utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
    }
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
