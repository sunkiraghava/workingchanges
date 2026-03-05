package com.punchh.server.EventsFrameworkNewCases;

import com.punchh.server.EventFrameworkTest.EventFrameWorkTestCases;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//Author :- Shashank sharma
//Purpose:- This class is used to create new custom webhook and validate the things
// Business Name:-  This class test cases used only "automationtwentytwo" only , do not use othe business in case of custom webhook creation
@Listeners(TestListeners.class)
public class CustomWebhookEFTestCases {
        static Logger logger = LogManager.getLogger(CustomWebhookEFTestCases.class);
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
        }



//shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 1, description = "SQ-T5818 [EF - Custom Webhook] Custom Webhook Creation with No Authentication Webhook including webhook event sync (Step-1)"
            + "SQ-T5865 [EF - Custom Webhook] Verification of Marketing Notifications (Step-4.4)")
    @Owner(name = "Shashank Sharma")
    public void T5818_ValidateCustomWebhookCreationWithAuthenticationEvents() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookPrefixName = "AutoCustomWebhookNoAuth_";
        String webhookName = CreateDateTime.getUniqueString(webhookPrefixName);

            List<String> eventNameList = new ArrayList<String>();
            eventNameList.add("Guest");
            eventNameList.add("Loyalty Checkin");
            eventNameList.add("Redemption");
            eventNameList.add("Rewards");
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

            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
            pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
            pageObj.webhookManagerPage().clickOnCreateWebhookButton();

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
                    webhookName + " Description");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
                    "Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
            pageObj.webhookManagerPage().selectEvent(eventNameList);
            pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

            pageObj.webhookManagerPage().clickOnSubmitButton();
            logger.info(webhookName + " webhook is created successfully");
            TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
            isWebhookCreatedSuccessfully = true;
            String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");

            List<String> keyValueFromPreferences_Kafka = Utilities
                    .getPreferencesKeyValue(preferences, "kafka_topics");

            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
                    "User event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
                    "Checkins event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
                    "Redemptions event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
                    "Rewards event is not appearing in business preferences ");

            logger.info("Verified that all events are coming in business preferences");
            TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

            // create New User
            String userEmail = pageObj.iframeSingUpPage().generateEmail();
            Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                    dataSet.get("secret"));
            String userID = signUpResponse.jsonPath().get("user.user_id").toString();
            String timeAfterEventTriggered = utils.getCurrentTimeStampAfterEventTriggered();

            // Gift Reedemable to User
            Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"));
            Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
                    "Status code 201 did not matched for api2 send message to user");
            TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

            // Comment out the below code as it is not working properly -
            // checking logs for Guest event
            String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);
            Assert.assertTrue(jsonObjectStr.contains("\"event_name\":\"users\""), "Guest event is not coming in logs");
            logger.info("Verified that Guest event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

            Assert.assertTrue(jsonObjectStr.contains("\"email\":\"" + userEmail + "\""),
                    "Loyalty Checkin event is not coming in logs");
            logger.info("Verified that Loyalty Checkin event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

            // checking logs for rewards

            String jsonObjectStrRewards = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Rewards",
                    webhookName,"",timeAfterEventTriggered);

            Assert.assertTrue(jsonObjectStrRewards.contains("\"event_name\":\"rewards\""),
                    "Guest event is not coming in logs");
            logger.info("Verified that Guest event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

            Assert.assertTrue(jsonObjectStrRewards.contains("\"email\":\"" + userEmail + "\""),
                    "Rewards event is not coming in logs");
            logger.info("Verified that Rewards event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Rewards event is coming in logs");


    }// end of test case T5818_ValidateCustomWebhookCreationWithAuthenticationEvents

    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 2, description = "SQ-T5819 [EF - Custom Webhook] Custom Webhook Creation with Basic Auth Authentication including webhook event sync (Step-2)")
    @Owner(name = "Shashank Sharma")
    public void T5819_ValidateCustomWebhookCreationWithBasicAuthIncludingAuthenticationEvents() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookName = CreateDateTime.getUniqueString("AutoTestWebhookBasicAuth");
        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Guest");
        eventNameList.add("Loyalty Checkin");
        eventNameList.add("Redemption");
        eventNameList.add("Rewards");

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
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
            pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
            pageObj.webhookManagerPage().clickOnCreateWebhookButton();

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
                    webhookName + " Description");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
                    "Custom Webhook-2 Base URL (https://flask-webhook-449610.et.r.appspot.com)");

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point",
                    "/basic-auth-webhook");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "Basic");
            Thread.sleep(2000);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("User Name", "WebhookAdmin");
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Password", "Password");

            pageObj.webhookManagerPage().selectEvent(eventNameList);
            pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

            pageObj.webhookManagerPage().clickOnSubmitButton();
            logger.info(webhookName + " webhook is created successfully");
            TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
            isWebhookCreatedSuccessfully = true;
            String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_Kafka = Utilities
                    .getPreferencesKeyValue(preferences, "kafka_topics");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
                    "User event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
                    "Checkins event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
                    "Redemptions event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
                    "Rewards event is not appearing in business preferences ");

            logger.info("Verified that all events are coming in business preferences");
            TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

            // create New User
            String userEmail = pageObj.iframeSingUpPage().generateEmail();
            Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                    dataSet.get("secret"));
            String timeAfterEventTriggered = utils.getCurrentTimeStampAfterEventTriggered();
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

            // checking logs for Guest event
            String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail,
                    "Guest", webhookName, "",timeAfterEventTriggered);
            String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;

            Assert.assertEquals(actualEventName,"users" ,  "Guest event is not coming in logs");
            logger.info("Verified that Guest event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

            String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "email") ;

            Assert.assertEquals(actualEmail, userEmail, "Loyalty Checkin event is not coming in logs");
            logger.info("Verified that Loyalty Checkin event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");



    }// end of test case
    // T5819_ValidateCustomWebhookCreationWithBasicAuthIncludidngAuthenticationEvents

    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 3, description = "SQ-T5820 [EF - Custom Webhook] Custom Webhook Creation with Bearer Authentication including webhook event sync (Step-3)\n")
    @Owner(name = "Shashank Sharma")
    public void T5820_ValidateCustomWebhookCreationWithBearerAuthIncludingAuthenticationEvents() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";

        String webhookName = CreateDateTime.getUniqueString("AutoWebhookBearerAuth");
        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Guest");
        eventNameList.add("Loyalty Checkin");
        eventNameList.add("Redemption");
        eventNameList.add("Rewards");
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
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
            pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
            pageObj.webhookManagerPage().clickOnCreateWebhookButton();

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
                    webhookName + " Description");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
                    "Custom Webhook-2 Base URL (https://flask-webhook-449610.et.r.appspot.com)");

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/auth-webhook");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "Bearer");
            Thread.sleep(2000);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Auth Bearer Token",
                    "epopbJnj_s5mEzGx3zEd");

            pageObj.webhookManagerPage().selectEvent(eventNameList);
            pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

            pageObj.webhookManagerPage().clickOnSubmitButton();
            logger.info(webhookName + " webhook is created successfully");
            TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
            String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_Kafka = Utilities
                    .getPreferencesKeyValue(preferences, "kafka_topics");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
                    "User event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
                    "Checkins event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
                    "Redemptions event is not appearing in business preferences ");
            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
                    "Rewards event is not appearing in business preferences ");

            logger.info("Verified that all events are coming in business preferences");
            TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

            // create New User
            String userEmail = pageObj.iframeSingUpPage().generateEmail();
            Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
                    dataSet.get("secret"));
            String timeAfterEventTriggered =  utils.getCurrentTimeStampAfterEventTriggered();
            Thread.sleep(5000);
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

            // checking logs for Guest event

            String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);

            String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;
            Assert.assertEquals(actualEventName , "users", "Guest event is not coming in logs");
            logger.info("Verified that Guest event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

            String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "email") ;
            Assert.assertEquals(actualEmail , userEmail, "Loyalty Checkin event is not coming in logs");
            logger.info("Verified that Loyalty Checkin event is coming in logs");
            TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

    }// end of test case
    // T5820_ValidateCustomWebhookCreationWithBearerAuthIncludidngAuthenticationEvents


    //Shashank sharma
    @Test(groups = {"regression", "dailyrun" },priority = 4, description = "SQ-T5825 [EF - Custom Webhook] Event syncing on Webhook Activation, Deactivation and Deletion (Step 8)")
    @Owner(name = "Shashank Sharma")
    public void T5825_VerifyActivateDeactivateAndDeleteForWebhooks() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookName = CreateDateTime.getUniqueString("AutoActiveDeleteWebhook_");
        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("Anniversary Campaigns");
       // try {
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
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
            pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
            pageObj.webhookManagerPage().clickOnCreateWebhookButton();

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
                    webhookName + " Description");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
                    "Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
            pageObj.webhookManagerPage().selectEvent(eventNameList);
            pageObj.webhookManagerPage().clickOnActiveCheckBox(false);

            pageObj.webhookManagerPage().clickOnSubmitButton();
            logger.info(webhookName + " webhook is created successfully");
            TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
            isWebhookCreatedSuccessfully = true;

            String preferences2 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");

            List<String> keyValueFromPreferences_KafkaInactive = Utilities
                    .getPreferencesKeyValue(preferences2, "kafka_topics");

            Assert.assertFalse(keyValueFromPreferences_KafkaInactive.contains("marketing_notifications"),
                    "marketing_notifications event is appearing in business kafka_topics preferences ");

            logger.info(
                    "Verified that marketing_notifications events not appearing in business kafka_topics preferences");
            TestListeners.extentTest.get().info(
                    "Verified that marketing_notifications events not appearing in business  kafka_topics preferences");

            String preferences3 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_marketing_eventsInActive = Utilities
                    .getPreferencesKeyValue(preferences3, "marketing_events");
            Assert.assertFalse(keyValueFromPreferences_marketing_eventsInActive.contains("anniversary_campaign"),
                    "anniversary_campaign event is appearing in business marketing_events preferences ");

            logger.info(
                    "Verified that anniversary_campaign events  not appearing in business marketing_events preferences");
            TestListeners.extentTest.get().info(
                    "Verified that anniversary_campaign events  not appearing in business marketing_events preferences");

            // deactivate the webhook and check events are not coming in business
            // preferences
            pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

            // check webhook is active or not on Ui

            String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");

            List<String> keyValueFromPreferences_Kafka = Utilities
                    .getPreferencesKeyValue(preferences, "kafka_topics");

            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("marketing_notifications"),
                    "marketing_notifications event is not appearing in business kafka_topics preferences ");

            logger.info("Verified that marketing_notifications events are coming in business kafka_topics preferences");
            TestListeners.extentTest.get().info(
                    "Verified that marketing_notifications events are coming in business kafka_topics preferences");

            String preferences1 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_marketing_events = Utilities
                    .getPreferencesKeyValue(preferences1, "marketing_events");
            Assert.assertTrue(keyValueFromPreferences_marketing_events.contains("anniversary_campaign"),
                    "anniversary_campaign event is not appearing in business marketing_events preferences ");

            logger.info("Verified that anniversary_campaign events are coming in business preferences");
            TestListeners.extentTest.get()
                    .info("Verified that anniversary_campaign events are coming in business preferences");

//        }catch (AssertionError ae){
//            logger.info(ae.getMessage());
//            TestListeners.extentTest.get().info(ae.getMessage());
//            Assert.fail(ae.getMessage());
//
//        } catch (Exception e) {
//            logger.info(e.getMessage());
//            TestListeners.extentTest.get().fail(e.getMessage());
//            Assert.fail(e.getMessage());
//        } finally {
//
//            // delete the webhook and check events are not coming in business preferences
//            if (isWebhookCreatedSuccessfully) {
//                pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
//                logger.info(webhookName + " webhook is deleted successfully");
//                TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");
//
//                String preferences4 = DBUtils.executeQueryAndGetColumnValue(env, query1,
//                        "preferences");
//
//                List<String> keyValueFromPreferences_KafkaInactive4 = pageObj.singletonDBUtilsObj()
//                        .getPreferencesKeyValue(preferences4, "kafka_topics");
//
//                Assert.assertTrue(!keyValueFromPreferences_KafkaInactive4.contains("marketing_notifications"),
//                        "marketing_notifications event is appearing in business kafka_topics preferences after deletion");
//
//                logger.info(
//                        "Verified that marketing_notifications events not appearing in business kafka_topics preferences after deletion");
//                TestListeners.extentTest.get().info(
//                        "Verified that marketing_notifications events not appearing in business  kafka_topics preferences after deletion");
//
//                String preferences5 = DBUtils.executeQueryAndGetColumnValue(env, query1,
//                        "preferences");
//                List<String> keyValueFromPreferences_marketing_eventsInActive5 = pageObj.singletonDBUtilsObj()
//                        .getPreferencesKeyValue(preferences5, "marketing_events");
//                Assert.assertTrue(!keyValueFromPreferences_marketing_eventsInActive5.contains("anniversary_campaign"),
//                        "anniversary_campaign event is appearing in business marketing_events preferences ");
//
//                logger.info(
//                        "Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
//                TestListeners.extentTest.get().info(
//                        "Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
//            }
//        }

    }// end of test case T5825_VerifyActivateDeactivateAndDeleteForWebhooks


    //Shashank Sharma
    @Test(groups = {"regression", "dailyrun" },priority = 5, description = "SQ-T5825 [EF - Custom Webhook] Event syncing on Webhook Activation, Deactivation and Deletion (Step 8)")
    @Owner(name = "Shashank Sharma")
    public void T5825_VerifyActivateDeactivateAndDeleteForWebhooksPartTwo() throws Exception {
        String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
                + dataSet.get("business_id") + "';";
        boolean isWebhookCreatedSuccessfully = false;
        String webhookName = CreateDateTime.getUniqueString("AutoActiveDeleteWebhook_");
        List<String> eventNameList = new ArrayList<String>();
        eventNameList.add("POS Scanner Checkin");
        try {

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
            pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
            pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
            pageObj.webhookManagerPage().clickOnCreateWebhookButton();

            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
                    webhookName + " Description");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
                    "Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
            pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
            pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
            pageObj.webhookManagerPage().selectEvent(eventNameList);
            pageObj.webhookManagerPage().clickOnActiveCheckBox(false);

            pageObj.webhookManagerPage().clickOnSubmitButton();
            logger.info(webhookName + " webhook is created successfully");
            TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
            isWebhookCreatedSuccessfully = true;

            String preferences2 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");

            List<String> keyValueFromPreferences_KafkaInactive = Utilities
                    .getPreferencesKeyValue(preferences2, "kafka_topics");

            Assert.assertTrue(!keyValueFromPreferences_KafkaInactive.contains("marketing_notifications"),
                    "marketing_notifications event is appearing in business kafka_topics preferences ");

            logger.info(
                    "Verified that marketing_notifications events not appearing in business kafka_topics preferences");
            TestListeners.extentTest.get().info(
                    "Verified that marketing_notifications events not appearing in business  kafka_topics preferences");

            String preferences3 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_transactionEvents = Utilities
                    .getPreferencesKeyValue(preferences3, "transactional_events");
            Assert.assertFalse(keyValueFromPreferences_transactionEvents.contains("pos_scanner_checkin"),
                    "pos_scanner_checkin event is not appearing in business marketing_events preferences ");

            logger.info(
                    "Verified that pos_scanner_checkin events  not appearing in business marketing_events preferences");
            TestListeners.extentTest.get().info(
                    "Verified that pos_scanner_checkin events  not appearing in business marketing_events preferences");

            // deactivate the webhook and check events are not coming in business
            // preferences
            pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

            // check webhook is active or not on Ui

            String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");

            List<String> keyValueFromPreferences_Kafka = Utilities
                    .getPreferencesKeyValue(preferences, "kafka_topics");

            Assert.assertTrue(keyValueFromPreferences_Kafka.contains("transactional_notifications"),
                    "transactional_notifications event is not appearing in business kafka_topics preferences ");

            logger.info("Verified that transactional_events events are coming in business kafka_topics preferences");
            TestListeners.extentTest.get()
                    .info("Verified that transactional_events events are coming in business kafka_topics preferences");

            String preferences6 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                    "preferences");
            List<String> keyValueFromPreferences_transactionEvents1 = Utilities
                    .getPreferencesKeyValue(preferences6, "transactional_events");
            Assert.assertTrue(keyValueFromPreferences_transactionEvents1.contains("pos_scanner_checkin"),
                    "pos_scanner_checkin event is not appearing in business marketing_events preferences ");

            logger.info("Verified that pos_scanner_checkin events are coming in business preferences");
            TestListeners.extentTest.get()
                    .info("Verified that pos_scanner_checkin events are coming in business preferences");

        }catch (AssertionError ae){
            logger.info(ae.getMessage());
            TestListeners.extentTest.get().info(ae.getMessage());
            Assert.fail(ae.getMessage());

        } catch (Exception e) {
            logger.info(e.getMessage());
            TestListeners.extentTest.get().fail(e.getMessage());
            Assert.fail(e.getMessage());
        } finally {

            // delete the webhook and check events are not coming in business preferences
            if (isWebhookCreatedSuccessfully) {
                pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
                logger.info(webhookName + " webhook is deleted successfully");
                TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

                String preferences4 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                        "preferences");

                List<String> keyValueFromPreferences_KafkaInactive4 = Utilities
                        .getPreferencesKeyValue(preferences4, "kafka_topics");

                Assert.assertTrue(!keyValueFromPreferences_KafkaInactive4.contains("marketing_notifications"),
                        "marketing_notifications event is appearing in business kafka_topics preferences after deletion");

                logger.info(
                        "Verified that marketing_notifications events not appearing in business kafka_topics preferences after deletion");
                TestListeners.extentTest.get().info(
                        "Verified that marketing_notifications events not appearing in business  kafka_topics preferences after deletion");

                String preferences5 = DBUtils.executeQueryAndGetColumnValue(env, query1,
                        "preferences");
                List<String> keyValueFromPreferences_marketing_eventsInActive5 = Utilities
                        .getPreferencesKeyValue(preferences5, "marketing_events");
                Assert.assertTrue(!keyValueFromPreferences_marketing_eventsInActive5.contains("anniversary_campaign"),
                        "anniversary_campaign event is appearing in business marketing_events preferences ");

                logger.info(
                        "Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
                TestListeners.extentTest.get().info(
                        "Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
            }
        }

    }// end of test case T5825_VerifyActivateDeactivateAndDeleteForWebhooks

  @AfterMethod
    public void afterClass() {
        dataSet.clear();
        driver.quit();
        logger.info("Browser closed");
    } //end of after method

} // End of class
