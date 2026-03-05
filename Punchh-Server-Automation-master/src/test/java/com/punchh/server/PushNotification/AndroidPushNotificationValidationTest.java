package com.punchh.server.PushNotification;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.mobilePages.AndroidPageObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.AndroidUtilities;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AndroidPushNotificationValidationTest {
    private static AppiumDriverLocalService service;
    static Logger logger = LogManager.getLogger(AndroidPushNotificationValidationTest.class);
    public AndroidDriver androidDriver = null;
    AndroidUtilities androidUtilitiesObj = null;
    PageObj pageObj = null;
    private AndroidPageObj androidPageObj = null;
    private String locationKey = "673936852a9b416a9452465ed9a31fc8";
    private String client = "5ec9641e9d8a77ffccd0a49181dae7e3b4f25c9453aaa043216507b625d794fb";
    private String secret = "c973ea47fcda14695dc6fd372234fe7cacf23eb502674f44b1dcc6d8643d14b1";
    private String apiKey = "SSbTsC1snP4wpYtz6FyE";
    private String sTCName;
    private String env;
    private String baseUrl;
    private static Map<String, String> dataSet;
    String run = "ui";
    private Properties prop;
    public WebDriver webDriver;

    @BeforeClass
    public void BeforeClass() {
        stopAppiumServer();
        startAppiumServer();
    }

    @AfterClass
    public void AfterClass() {
        stopAppiumServer();
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setPlatformVersion("15.0");
        options.setDeviceName("Pixel 9 Pro");
//        options.setAutomationName("Uiautomator2");
//        options.setApp("/Users/shashanksharma/eclipse-workspace/app-masterapp-release.apk");
        
        options.setAutomationName("uiautomator2");
        options.setApp("/Users/rakhirawat/eclipse-workspace/app-masterapp-release1.apk");
        URL url = new URL("http://127.0.0.1:4723");
        androidDriver = new AndroidDriver(url, options);
        androidPageObj = new AndroidPageObj(androidDriver);
        logger.info("Android driver is initialized ");
        androidUtilitiesObj = new AndroidUtilities(androidDriver);

    }

    //Verifying PN and Rich messages for Signup and Profile Update campaigns and Anniversary and Birthday campaigns and Post Checkin and Post Redemption campaigns
    @Test
    public void VerifyPushNotificationOnAndroidMobileApp() throws MalformedURLException, InterruptedException {
        int retryCounter = 0;
        pageObj = new PageObj();
        long phone = (long) (Math.random() * Math.pow(10, 10));
        String phone1 = String.valueOf(phone);

        String finalFailedPNString = "";
        String expSignupPnMessage = "Automation Push Notification Title Of Signup Campaign All Channel 15 April";
        String expProfileUpdatePnMessage = "Automation Push Notification Title Of Profile Update 06/02 PN SMS Email";
        String expAnniversaryBirthdayPnMessage = "Automation Push Notification Title Of Anniversary Campaign All Channel 15 April";
        String expPostCheckingPnMessage = "Automation Push Notification Title Post Checkin Offer All Channels";
        String expPostRedemptionPnMessage = "Automation Push Notification Title Of PN All Channel POS Redemption";
        logger.info("STARTED");
        pageObj.utils().logit("STARTED");

        Set<String> expPNMessageList = new LinkedHashSet<String>();
        Set<String> failedPNList = new LinkedHashSet<String>();
        expPNMessageList.add(expSignupPnMessage);
        expPNMessageList.add(expProfileUpdatePnMessage);
        expPNMessageList.add(expAnniversaryBirthdayPnMessage);
//        expPNMessageList.add(expPostCheckingPnMessage);
//        expPNMessageList.add(expPostRedemptionPnMessage);


        //switch to Pre Prod Env
        androidPageObj.androidAppPages().switchToPreProdEnvironment("payal+13@punchh.com", "qwertyui");
        pageObj.utils().logit("Switched to Pre prod env successfully");
        // user signup
        String userEmail = "automation_android_" + CreateDateTime.getTimeDateString() + "@partech.com";
        logger.info(userEmail + " email is generated");
        pageObj.utils().logit(userEmail + " email is generated");

        // Signup user from android device
        androidPageObj.androidAppPages().signupAndroidUser(userEmail, "shashank sharma", "Password@123", true, phone1);
        pageObj.utils().logit(userEmail + " user is signed up successfully");
        logger.info(userEmail + " user is signed up successfully");

        Thread.sleep(15000);
        //
        failedPNList = expPNMessageList;
        do {
            failedPNList = androidPageObj.androidAppPages().verifiedPushNotification(failedPNList);
            retryCounter++;

        } while (failedPNList.size() != 0 && retryCounter <= 3);

        if (failedPNList.size() != 0) {
            for (String str : failedPNList) {
                finalFailedPNString = finalFailedPNString + " / " + str;
                TestListeners.extentTest.get().fail(str + " PN message is not coming in push notification--");
            }
        }


        // API1 user login
        Response loginResponseAPI1 = pageObj.endpoints().Api1UserLoginForAndroidApp(userEmail, client,
                secret, "Password@123");

        String userID1 = loginResponseAPI1.jsonPath().getString("id").replace("[", "").replace("]", "");
        String authentication_token = loginResponseAPI1.jsonPath().getString("authentication_token").replace("[", "").replace("]", "");
        String userToken = loginResponseAPI1.jsonPath().getString("auth_token.token").replace("[", "").replace("]", "");


        // POS checkin
        Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, locationKey);
        Assert.assertEquals(checkinResponse.getStatusCode(), 200);

        Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, apiKey,
                "", "659", "", "");
        Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), 201,
                "Status code 201 did not match for Send reward amount to user");
        pageObj.utils().logPass("Send reward amount to user #1 is successful");
        logger.info("Send reward amount to user #1 is successful");

        // get reward id
        String rewardId = pageObj.redeemablesPage().getRewardId(userToken, client, secret,
                "659");

        logger.info("Reward id " + rewardId + " is generated successfully ");
        pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

        Response respo = pageObj.endpoints().posRedemptionOfReward(userEmail, locationKey, rewardId);
        Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

        logger.info("Send redeemable to the user successfully");
        pageObj.utils().logPass("Send redeemable to the user successfully");

        Thread.sleep(15000);
        // checking push notification and re-trying if failed
        expPNMessageList.clear();
        failedPNList.clear();
        expPNMessageList.add(expPostCheckingPnMessage);
        expPNMessageList.add(expPostRedemptionPnMessage);
        failedPNList = expPNMessageList;
        do {
            failedPNList = androidPageObj.androidAppPages().verifiedPushNotification(failedPNList);
            retryCounter++;

        } while (failedPNList.size() != 0 && retryCounter <= 3);

        if (failedPNList.size() != 0) {
            for (String str : failedPNList) {
                finalFailedPNString = finalFailedPNString + " / " + str;
                TestListeners.extentTest.get().fail(str + " PN message is not coming in push notification--");
            }
        }

        try {
            this.androidDriver.navigate().back();
            this.androidDriver.navigate().back();
            androidUtilitiesObj.longWaitInSeconds(2);
            androidUtilitiesObj.getLocator("mobileAndroidAppPage.skipLinkXpath").click();
            androidUtilitiesObj.longWaitInSeconds(4);
        } catch (Exception e) {
        }

        //checking Rich messages
        Set<String> allRichMessagesList = androidPageObj.androidAppPages().getAllRichMessagesFromInboxForPN();

        if (failedPNList.size() != 0) {
            for (String str : failedPNList) {
                finalFailedPNString = finalFailedPNString + " / " + str;
                TestListeners.extentTest.get().fail(str + " PN message is not coming in push notification--");
            }
        }
        Assert.assertEquals(finalFailedPNString, "", "Push Notification message is not coming for the following campaigns:: " + finalFailedPNString);
    }


    @Test
    public void VerifyPushNotificationOnAndroidForMassOfferCampaigns() throws InterruptedException {
        long phone = (long) (Math.random() * Math.pow(10, 10));
        String phone1 = String.valueOf(phone);
        String birthdayDate = androidUtilitiesObj.getCurrentDate("MMM dd ") + "2000";

        prop = Utilities.loadPropertiesFile("config.properties");
        webDriver = new BrowserUtilities().launchBrowser();
        //  sTCName = method.getName();
        pageObj = new PageObj(webDriver);
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), "VerifyPushNotificationOnAndroidForMassOfferCampaigns");
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);


        // user creation using api2
        String userEmail = pageObj.iframeSingUpPage().generateEmail();
        // AnniversaryForNrewardingDays
        String futureAnniversaryDate = CreateDateTime.getFutureDateTime(7);

        Response signUpResponse = pageObj.endpoints().Api2SignUpwithAnniversaryAndroid(client, secret, userEmail, "Password@123", phone1, birthdayDate);
        String token = signUpResponse.jsonPath().getString("access_token.refresh_token").replace("[", "").replace("]", "");
        int userID1 = Integer.parseInt(signUpResponse.jsonPath().getString("user.user_id").replace("[", "").replace("]", ""));


        System.out.println("signUpResponse: " + signUpResponse.asString());
        //create user using   mobApi2SignUp = "/api2/mobile/users";
        // switch to env in android
        //login via created user 

        // create segment customSegments = "/api2/dashboard/custom_segments";
        String segName = "CustomSegForAndroidTesting_" + CreateDateTime.getTimeDateString();
        System.out.println("segName: " + segName);
        Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName, apiKey);

        System.out.println("createSegmentResponse: " + createSegmentResponse.asString());
        int customSegmentId = Integer.parseInt(createSegmentResponse.jsonPath().getString("custom_segment_id").replace("[", "").replace("]", ""));
        // add user to custom segments with invalid authorization
        Response addUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, userEmail, apiKey);

        System.out.println("addUserSegmentResponse: " + addUserSegmentResponse.asString());


        // Searching user in custom segments with invalid user email
        Response userExistsInSegmentResponse2 = pageObj.endpoints().searchUserExistsInSegment(customSegmentId, userEmail, userID1, apiKey);

        System.out.println("userExistsInSegmentResponse2: " + userExistsInSegmentResponse2.asString());


        //add created user to segment customSegmentMembers = "/api2/dashboard/custom_segments/members";
        //check user is updated in segment 
        //mass offer create using segment and then run


        //check PN messages in android app


//        String massCampaignName = "AutomationMasOfferPNTesting_"
//                + CreateDateTime.getTimeDateString();
//        String dateTime = CreateDateTime.getCurrentDate() + " 10:00 PM";
//
////        String userEmail = dataSet.get("email");
//        // Login to instance
//        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//        pageObj.instanceDashboardPage().loginToInstance();
//        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//        pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//
//        // Select offer dropdown value
//        pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
//        pageObj.campaignspage().clickNewCampaignBtn();
//        pageObj.signupcampaignpage().setCampaignName(massCampaignName);
//        pageObj.signupcampaignpage().setCouponCampaign(dataSet.get("couponCampaign"));
//        pageObj.signupcampaignpage().clickNextBtn();
//
//        pageObj.signupcampaignpage().setAudianceType(dataSet.get("audiance"));
//        pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
////		pageObj.signupcampaignpage().setAdvertisingCampaign(dataSet.get("advertisingCampaign"));
//        pageObj.signupcampaignpage().setPushNotification(massCampaignName);
//        pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
//        pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
//        pageObj.signupcampaignpage().clickNextButton();
//
//        pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
//        boolean status = pageObj.campaignspage().validateSuccessMessage();
//        Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");
    }


    public static void startAppiumServer() {
        service = new AppiumServiceBuilder()
                .usingDriverExecutable(
                        new File("/Users/rakhirawat/.nvm/versions/node/v20.19.6/bin/node")) // Path to
                // Node.js
                .withAppiumJS(new File("/Users/rakhirawat/.nvm/versions/node/v20.19.6/lib/node_modules/appium/build/lib/main.js"
     )) // Path to
                // Appium
                // main.js
                .withIPAddress("127.0.0.1").usingPort(4723) // Default Appium port
                .withArgument(GeneralServerFlag.SESSION_OVERRIDE) // Override existing session
                .withArgument(GeneralServerFlag.LOG_LEVEL, "debug") // Set log level
                .build();

        service.start();
        logger.info("✅ Appium Server Started at: " + service.getUrl());
    }

    public static void stopAppiumServer() {
        if (service != null) {
            service.stop();
            TestListeners.extentTest.get().pass("🛑 Appium Server Stopped.");
            logger.info("🛑 Appium Server Stopped.");
        }
    }


}// end of class