package com.punchh.server.EventsFrameworkNewCases;


import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Author :- Shashank sharma
//Purpose:- This class is used to create new custom webhook and validate the things
// Business Name:-  This class test cases used only "deltaco" only , do not use othe business in case of custom webhook creation
@Listeners(TestListeners.class)
public class EventFrameWorkCustomWebhookTest {

    static Logger logger = LogManager.getLogger(EventFrameWorkCustomWebhookTest.class);
    public WebDriver driver;
    String userEmail;
    String email = "autoemailTemp@punchh.com";
    ApiUtils apiUtils;
    PageObj pageObj;
    String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private String segID = "11554400";
    private String segName = "Test MP Audience " + segID;
    private static Map<String, String> dataSet;
    Utilities utils;
    private String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";

    @BeforeMethod()
    public void beforeMethod(Method method) throws InterruptedException {
        driver = new BrowserUtilities().launchBrowser();
        apiUtils = new ApiUtils();
        pageObj = new PageObj(driver);
        dataSet = new ConcurrentHashMap<>();
        sTCName = method.getName();
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        utils = new Utilities(driver);
    } // End of before method



    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 0 , description = "SQ-T5863[EF - Custom Webhook] Verification of Reward & Redemption Event (Step-4-2)")
    @Owner(name = "Shashank Sharma")
    public void T5863_VerificationOfWebhookRewardAndRedemptionEvent() throws InterruptedException, HeadlessException, UnsupportedFlavorException, IOException {

        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        String webhookName = dataSet.get("webhookName");
        String redeemable_id = dataSet.get("redeemable_id");
        String redeemableName = dataSet.get("redeemableName");
        String apiKey = dataSet.get("apiKey");

        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Rewards");

        // Navigate to Business and select the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
        pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
                "Enable Success Headers Logging", false);
        pageObj.webhookManagerPage().clickOnSubmitButton();

        pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

        // User SignUp using API
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
        String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
        String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
        String timeAfterEventTriggered =  utils.getCurrentTimeStampAfterEventTriggered();
        // Gift Reedemable to User
        Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", redeemable_id);
        Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
                "Status code 201 did not matched for api2 send message to user");
        TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

        // Get reward_id
        String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
                redeemable_id);
        logger.info("Reward id " + rewardId + " is generated successfully ");
        TestListeners.extentTest.get().info("Reward id " + rewardId + " is generated successfully ");

        // perform redemption second time
        Response posRedeem3 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
                rewardId);

        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

        // Comment out the below code as it is not working properly -
        // checking logs for Guest event
        String jsonObjectStr =pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Redemption", webhookName,"\"redemption_type\":\"RewardRedemption\"",timeAfterEventTriggered);

        String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;

        Assert.assertEquals(actualEventName , "redemptions","Redemption event is not coming in logs");
        logger.info("Verified that Redemption event is coming in logs");
        TestListeners.extentTest.get().pass("Verified that Redemption event is coming in logs");

        String actualRedemptionType=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "redemption_type") ;

        Assert.assertEquals(actualRedemptionType , "RewardRedemption","RewardRedemption redemption_type is not coming in logs");
        logger.info("Verified that RewardRedemption event is coming in logs");
        TestListeners.extentTest.get().pass("Verified that RewardRedemption event is coming in logs");

        String jsonObjectStr1  = 	pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Transactional Notifications", webhookName,"\"redeemable_name\":\""+redeemableName+"\"",timeAfterEventTriggered);
        logger.info("jsonObjectStr1-"+ jsonObjectStr1);
        TestListeners.extentTest.get().pass("jsonObjectStr1-"+ jsonObjectStr1);

        String actualEventNameTransactional=utils.findValueByKeyFromJsonAsString(jsonObjectStr1, "event_name") ;


        Assert.assertEquals(actualEventNameTransactional , "transactional_notifications","transactional_notifications event is not coming in logs");
        logger.info("Verified that transactional_notifications event is coming in logs");
        TestListeners.extentTest.get().pass("Verified that transactional_notifications event is coming in logs");

        int actualredeemable_discount_amount= Integer.parseInt( utils.findValueByKeyFromJsonAsString(jsonObjectStr1, "redeemable_discount_amount")) ;


        Assert.assertTrue(actualredeemable_discount_amount==13, "Loyalty Checkin event is not coming in logs");
        logger.info(
                "Verified that transactional_notifications event > redeemable_discount_amount amount is coming in logs");
        TestListeners.extentTest.get().pass(
                "Verified that transactional_notifications event > redeemable_discount_amount amount is coming in logs");

    } // end T5863_VerificationOfWebhookRewardAndRedemptionEvent


    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 1, description = "SQ-T5864[EF - Custom Webhook] Verification of Coupon Issuance & Redemption (Step-4.3)")
    @Owner(name = "Shashank Sharma")
    public void T5864_VerifyWebhookOfCouponIssuanceEvent() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookName = dataSet.get("webhookName");
        String redeemable_id = dataSet.get("redeemable_id");
        String redeemable_name = dataSet.get("redeemable_name");
        String apiKey = dataSet.get("apiKey");

        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Coupon Issuance");
        eventNameList.add("Coupon Redemption");
        // Navigate to Business and select the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
        pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
                "Enable Success Headers Logging", false);
        pageObj.webhookManagerPage().clickOnSubmitButton();

        pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

        String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                "preferences");

        List<String> keyValueFromPreferences_Kafka = Utilities
                .getPreferencesKeyValue(preferences, "kafka_topics");

        Assert.assertTrue(keyValueFromPreferences_Kafka.contains("coupons"),
                "coupons event is not appearing in business preferences ");
        Assert.assertTrue(keyValueFromPreferences_Kafka.contains("user_coupon_redemptions"),
                "user_coupon_redemptions event is not appearing in business preferences ");
        logger.info("Verified coupons and user_coupon_redemptions events are coming in business preferences");
        TestListeners.extentTest.get()
                .pass("Verified coupons and user_coupon_redemptions events are coming in business preferences");

        // user signup then get the code coupon code
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
        logger.info("Api1 user signup is successful");
        TestListeners.extentTest.get().pass("Api1 user signup is successful");
        String access_token = signUpResponse.jsonPath().get("auth_token.token");
        String timeAfterEventTriggered =  utils.getCurrentTimeStampAfterEventTriggered();
        // get coupon code

        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
        Thread.sleep(5000);

        // Comment out the below code as it is not working properly -
        // checking logs for Guest event
        String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Coupon Issuance",
                webhookName, "",timeAfterEventTriggered);

        String actualEventName = utils.findValueByKeyFromJsonAsString(jsonObjectStr.toString(), "event_name");

        Assert.assertEquals(actualEventName, "coupons", actualEventName + " Event name is not coupons");
        logger.info("Verified that coupons event is coming in logs");
        TestListeners.extentTest.get().info("Verified that coupons event is coming in logs");

        String couponCode = utils.findValueByKeyFromJsonAsString(jsonObjectStr.toString(), "code");

        logger.info(couponCode + " couponCode is generated in logs");
        TestListeners.extentTest.get().info(couponCode + " couponCode is generated in logs");

        String txn = "123456" + CreateDateTime.getTimeDateString();
        String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
        String key = CreateDateTime.getTimeDateString();
        Response respo = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, couponCode, key, txn,
                dataSet.get("locationkey"));
        Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

        logger.info("POS coupon code redemption is successful");
        TestListeners.extentTest.get().pass("POS coupon code redemption is successful");
        timeAfterEventTriggered =  utils.getCurrentTimeStampAfterEventTriggered();

        String jsonObjectStr1 = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Coupon Redemption",
                webhookName, "\"code\":\"" + couponCode + "\"",timeAfterEventTriggered);

        String actualEventName1 = utils.findValueByKeyFromJsonAsString(jsonObjectStr1.toString(), "event_name");
        Assert.assertEquals(actualEventName1, "user_coupon_redemptions",
                actualEventName1 + " Event name is not user_coupon_redemptions");
        logger.info("Verified that user_coupon_redemptions event is coming in logs");
        TestListeners.extentTest.get().info("Verified that user_coupon_redemptions event is coming in logs");

        String couponCode1 = utils.findValueByKeyFromJsonAsString(jsonObjectStr1.toString(), "code");
        Assert.assertEquals(couponCode, couponCode1, couponCode1 + " couponCode is not same as previous one");

        logger.info(couponCode + " couponCode is generated in logs");
        TestListeners.extentTest.get().info(couponCode + " couponCode is generated in logs");

    }//End of test T5864_VerifyWebhookOfCouponIssuanceEvent



    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 2, description = "SQ-T5867 [EF - Custom Webhook] Verification of User Subscription Event (Step-4.6)",dependsOnMethods = {"T5867_VerifyWebhookOfUserSubscriptionEvent_Prerequiste"})
    @Owner(name = "Shashank Sharma")
    public void T5867_VerifyWebhookOfUserSubscriptionEvent() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookName = dataSet.get("webhookName") ;
        String apiKey = dataSet.get("apiKey");
        String PlanName =  dataSet.get("subscriptionPlanName");
        String PlanID = dataSet.get("subscriptionPlanID");
        String spPrice = dataSet.get("spPrice");
        String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("User Subscription");

        // Navigate to Business and select the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
//        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

		// Commenting below lines as it is causing issue navigating to guest timeline ,
		// We can uncomment it when we want to run it individually

//        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
//        pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
//        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
//        pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
//                "Enable Success Headers Logging", false);
//        pageObj.webhookManagerPage().clickOnSubmitButton();
//
//        pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");
        utils.longWaitInSeconds(4);
        String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                "preferences");

        List<String> keyValueFromPreferences_Kafka = Utilities
                .getPreferencesKeyValue(preferences, "kafka_topics");

        Assert.assertTrue(keyValueFromPreferences_Kafka.contains("user_subscriptions"),
                "user_subscriptions event is not appearing in business preferences ");

        logger.info("Verified user_subscriptions events are coming in business preferences");
        TestListeners.extentTest.get().pass("Verified user_subscriptions events are coming in business preferences");

        // user signup then get the code coupon code
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
        logger.info("Api1 user signup is successful");
        TestListeners.extentTest.get().pass("Api1 user signup is successful");
        String access_token = signUpResponse.jsonPath().get("auth_token.token");
        String timeAfterEventTriggered =  utils.getCurrentTimeStampAfterEventTriggered();
        driver.switchTo().defaultContent();
        pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
        pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
        utils.longWaitInSeconds(3);
        pageObj.guestTimelinePage().messageGiftRewardsToUser("Gifting for Webhook event as subscription", "Subscription", PlanName,"");
        pageObj.guestTimelinePage().navigateToTabs("Subscriptions");
        int actualSubscriptionID = pageObj.guestTimelinePage().getGiftedSubscriptionID();
        logger.info(actualSubscriptionID + " subscription ID is generated after gifting to user");
        TestListeners.extentTest.get().info(actualSubscriptionID + " subscription ID is generated after gifting to user");

        pageObj.guestTimelinePage().clickOnSubscriptionCancel(dataSet.get("cancelType"));
        pageObj.guestTimelinePage().accecptSubscriptionCancellation("Price is Too High");
        logger.info("Subscription hard cancellation is successful");
        TestListeners.extentTest.get().info("Subscription hard cancellation is successful");

        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
        String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook("", "User Subscription",
                webhookName, "\"subscription_id\":"+actualSubscriptionID , timeAfterEventTriggered);

        String actualEventName1 =utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_type") ; //      actBodyData1.getString("event_type").replace("[", "").replace("]", "");
        Assert.assertEquals("user_subscription_cancel", actualEventName1, actualEventName1 + " event_type name is not user_subscription_cancel");
        logger.info("Verified that user_subscription_cancel event is coming in logs");
        TestListeners.extentTest.get().info("Verified that user_subscription_cancel event is coming in logs");

    }//End of test T5867_VerifyWebhookOfUserSubscriptionEvent


    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 3, description = "SQ-T5868 [EF - Custom Webhook] Verification of Redeemable Event (Step-4.7)")
    @Owner(name = "Shashank Sharma")
    public void T5868_VerifyWebhookOfRedeemableEvent() throws Exception {

        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        String webhookName = dataSet.get("webhookName");

        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Redeemable");
        String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
        String redeemableExternalID = "";

        // Navigate to Business and select the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
        pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
                "Enable Success Headers Logging", false);
        pageObj.webhookManagerPage().clickOnSubmitButton();

        pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

        String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                "preferences");

        List<String> keyValueFromPreferences_Kafka = Utilities
                .getPreferencesKeyValue(preferences, "kafka_topics");

        Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redeemables"),
                "redeemables event is not appearing in business preferences ");

        logger.info("Verified redeemables events are coming in business preferences");
        TestListeners.extentTest.get().pass("Verified redeemables events are coming in business preferences");

        // create redeemable

        String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
        String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);

        String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

        Map<String, String> map = new HashMap<String, String>();
        map.put("name", QCName);
        map.put("redeemableName", redeemableName);
        map.put("external_id", "");
        map.put("amount_cap", "10.0");
        map.put("percentage_of_processed_amount", "1");
        map.put("qc_processing_function", "sum_amounts");
        map.put("line_item_selector_id", dataSet.get("lisExternalID"));
        map.put("locationID", null);
        map.put("external_id_redeemable", "");
        map.put("redeemableProcessingFunction", "Sum Of Amount");
        map.put("qualifier_type", "existing");
        map.put("amount_cap", "10.0");
        map.put("expQCName", dataSet.get("qcName"));
        map.put("end_time", endTime);
        map.put("redeeming_criterion_id", "InvalidQCExternalID");
        map.put("indefinetely", "true");
        map.put("lineitemSelector", dataSet.get("lineItemSelector"));
        // Added redeemable with existing QC
        map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
        // SQ-T5501 Create Redeemable with flat discount
        String redeemableNameWithFlatDiscount = "AutomationRedeemableFlatDiscount_API_"
                + CreateDateTime.getTimeDateString();
        map.put("qualifier_type", "flat_discount");
        map.put("discount_amount", "230.0");
        map.put("redeemableName", redeemableNameWithFlatDiscount);
        map.put("end_time", null);
        map.put("expiry_days", "2");

        Response responseFlatDiscount = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
        Assert.assertEquals(responseFlatDiscount.getStatusCode(), 200);
        logger.info(redeemableNameWithFlatDiscount + " redeemable is created successfully");
        TestListeners.extentTest.get().pass(redeemableNameWithFlatDiscount + " redeemable is created successfully");

        redeemableExternalID = responseFlatDiscount.jsonPath().getString("results[0].external_id").replace("]", "")
                .replace("[", "");

        String timeAfterEventTriggered = utils.getCurrentTimeStampAfterEventTriggered();

        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

        String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook("", "Redeemable", webhookName, "\"name\":\"" + redeemableNameWithFlatDiscount + "\"", timeAfterEventTriggered);
        Assert.assertTrue(jsonObjectStr != null, "Redeemable event is not coming in logs");
        Assert.assertTrue(jsonObjectStr.contains("\"event_name\":\"redeemables\""),
                "Redeemable event is not coming in logs");

        if (redeemableExternalID != null) {
            String deleteRedeemableQuery1 = deleteRedeemableQuery
                    .replace("$redeemableExternalID", redeemableExternalID)
                    .replace("$businessID", dataSet.get("business_id"));

            boolean isRedeemableDeleted = DBUtils.executeQuery(env, deleteRedeemableQuery1);
            Assert.assertTrue(isRedeemableDeleted, redeemableName + " redeemable is not deleted");
            logger.info(redeemableName + " redeemable is deleted successfully");
            TestListeners.extentTest.get().pass(redeemableName + " redeemable is deleted successfully");
        }
    } //End of test T5868_VerifyWebhookOfRedeemableEvent


    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 4, description = "SQ-T6265 [EF - Error Codes] Webhook Error Verifications through Custom Webhook")
    @Owner(name = "Shashank Sharma")
    public void T6265_ValidateWebhookErrorsThroughCustomWebhookTest() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";

        Map<String, Integer> webhookNameInfoListmap = new HashMap<String, Integer>();
        webhookNameInfoListmap.put("DoNotDelete_TestWebhook401_Error", 401);
        webhookNameInfoListmap.put("DoNotDelete_TestWebhook408TransientError", 408);
        webhookNameInfoListmap.put("DoNotDelete_TestWebhook408_Error", 408);
        webhookNameInfoListmap.put("DoNotDelete_TestWebhook429_Error", 429);
        webhookNameInfoListmap.put("DoNotDelete_TestWebhook500_Error", 500);

        // Navigate to Business and select the business
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");

        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");

        pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
                "Enable Success Headers Logging", false);
        pageObj.webhookManagerPage().clickOnSubmitButton();

        // create New User
        String userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        String timeAfterEventTriggered = utils.getCurrentTimeStampAfterEventTriggered();
        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

        // checking logs for Guest event

        for (Map.Entry<String, Integer> entry : webhookNameInfoListmap.entrySet()) {
            String webhookName = entry.getKey();
            int expectedErrorCode = entry.getValue();
            String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,
                    "", timeAfterEventTriggered);

            int actualEventName = Integer
                    .parseInt(utils.findValueByKeyFromJsonAsString(jsonObjectStr, "WebhookStatusCode"));
            Assert.assertEquals(actualEventName, expectedErrorCode, actualEventName
                    + "Guest event is not coming in logs with expected status code: " + expectedErrorCode);
            logger.info("Verified that webhook " + webhookName
                    + " is triggered for Guest event is coming in logs with status code: " + expectedErrorCode);
            TestListeners.extentTest.get().info("Verified that webhook " + webhookName
                    + " is triggered for Guest event is coming in logs with status code: " + expectedErrorCode);

        } // end of for loop

    } //End of test T6265_ValidateWebhookErrorsThroughCustomWebhookTest
    
    
    @Test(description = "This is a prerequiste test case to be run before SQ-T5867")
    @Owner(name = "Shashank Sharma")
	public void T5867_VerifyWebhookOfUserSubscriptionEvent_Prerequiste() {
		String webhookName = dataSet.get("webhookName");

		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("User Subscription");

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");
	}

    @AfterMethod
    public void afterClass() {
        dataSet.clear();
        driver.quit();
        logger.info("Browser closed");
    } //end of after method

} // End of class
