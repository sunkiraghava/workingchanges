package com.punchh.server.campaignsTest;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.Integration2.IntUtils;
import com.punchh.server.pages.PageObj;
import io.restassured.response.Response;
import java.lang.reflect.Method;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class AndroidOfferPassesTest {

    private static final Logger logger = LogManager.getLogger(AndroidOfferPassesTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env;
    private String campaignName;
    private String baseUrl;
    private String iFrameEmail;
    private Map<String, String> dataSet;
    String run = "ui";
    private Utilities utils;
    SeleniumUtilities selUtils;
    private Properties dbConfig;
    private String punchhDBName;
    TestListeners testListObj;
    private IntUtils intUtils;

    @BeforeMethod(alwaysRun = true)
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
        dbConfig = Utilities.loadPropertiesFile("dbConfig.properties");
        punchhDBName = dbConfig.getProperty(env.toLowerCase() + ".DBName");
        selUtils = new SeleniumUtilities(driver);
        utils = new Utilities(driver);
        intUtils = new IntUtils(driver);
    }

    
    @Test(description = "SQ-T5874 | New user signup via personalise andorid offer pass")
    public void T5874_verifyAndroidPassForUser() throws Exception {
        String slug = dataSet.get("slug");

        // Enable Passes in Cockpit
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(slug);
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");

        // create a user
        String userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
                dataSet.get("secret"));
        Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not match for api1 signup");
        logger.info("Verified user signup via API v1");
        TestListeners.extentTest.get().pass("Verified user signup via API v1");
        String userID = signUpResponse.jsonPath().get("id").toString();
        String authToken = signUpResponse.jsonPath().get("authentication_token");

        // Navigate to guest Timeline
        pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

        // gift redeemable of user via new gifting api
        Response sendGiftResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
                dataSet.get("businessAdminKey"), dataSet.get("redeemable_id"), dataSet.get("end_date"));
        Assert.assertEquals(sendGiftResponse.getStatusCode(), 202, "Status code 202 did not match for gifting");
        logger.info("Successfully gifted Redeemable to user ");
        TestListeners.extentTest.get().pass("Successfully gifted redeemable to user ");

        // Fetch available offers of the user
        Response fetchAvailableOffersOfTheUserResponse = pageObj.endpoints()
                .authApiFetchAvailableOffersOfTheUser(authToken, dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(fetchAvailableOffersOfTheUserResponse.getStatusCode(), 200,
                "Status code 200 did not match for fetch available offers");

        logger.info("Successfully fetched available offers for user ");
        TestListeners.extentTest.get().pass("Successfully fetched available offers for user ");
        String rewardId = fetchAvailableOffersOfTheUserResponse.jsonPath().get("rewards[0].id").toString();

        // Fetch Redemption Code for reward id
        Response fetchRedemptionCodeResponse = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
                dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"), "", rewardId);
        String internaltrakingcode = fetchRedemptionCodeResponse.jsonPath().get("internal_tracking_code").toString();
        Thread.sleep(10000);

        // Navigate to guest Timeline
        pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

        String notificationText = pageObj.guestTimelinePage().getPushNotificationText();
        logger.info("Notification text on user timeline: " + notificationText);

        pageObj.guestTimelinePage().extractUrlFromNotificationText(notificationText);

        String query1 = "select id from redemptions where internal_tracking_code = '" + internaltrakingcode + "'";
        String redemptionID = DBUtils.executeQueryAndGetColumnValue(env, query1, "id");

        // check entry in android_passes table
        String query2 = "select status from android_passes where owner_id = '" + redemptionID + "'";
        boolean expValue2 = DBUtils.verifyValueFromDBUsingPolling(env, query2, "status", "0");
        Assert.assertTrue(expValue2, "Entry not present or initial status not 0 in android_passes table");
        logger.info("Verified that entry is created in android_passes table and initial status is 0 ");
        TestListeners.extentTest.get().pass("Verified that entry is created in android_passes table and initial status is 0 ");

        Thread.sleep(5000);

        // add pass to wallet
        pageObj.guestTimelinePage().clickOnAddToWalletButton();

        Thread.sleep(5000);

        String query4 = "select expiring_on from redemptions where internal_tracking_code = '" + internaltrakingcode + "'";
        String redemptionExpiry = DBUtils.executeQueryAndGetColumnValue(env, query4, "expiring_on");
        String newformat = pageObj.guestTimelinePage().convertDbDateToUiFormat(redemptionExpiry);
        logger.info("Exipiring on value for redemption" + newformat);

        String query5 = "select created_at from redemptions where internal_tracking_code = '" + internaltrakingcode + "'";
        String redemptionCreatedat = DBUtils.executeQueryAndGetColumnValue(env, query5, "created_at");
        String newformatcreatedat = pageObj.guestTimelinePage().convertDbDateToUiFormat(redemptionCreatedat);
        logger.info("Created on value for redemption" + newformat);
        
  	  	String timeDifferece=    pageObj.guestTimelinePage().TimeDifferenceExample(redemptionExpiry, redemptionCreatedat);
        
        String query6 = "select code_expiry_minutes from redeemables where id='" + dataSet.get("redeemable_id") + "'"; 
        String countdownexpiry = DBUtils.executeQueryAndGetColumnValue(env, query6, "code_expiry_minutes");
        
        Assert.assertEquals(timeDifferece, countdownexpiry);
        logger.info("countdown values matched successsfully");
        
        String newExpiry = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("expiryLabel"));
        Assert.assertEquals(pageObj.guestTimelinePage().normalizeSpace(newformat),
                pageObj.guestTimelinePage().normalizeSpace(newExpiry));
        logger.info("Values matched for expiry date on android pass");
        TestListeners.extentTest.get().pass("Values matched for expiry date on android pass");

        String newName = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("nameLabel"));
        Assert.assertEquals(dataSet.get("newFN"), newName);
        logger.info("Name matched for android pass");
        TestListeners.extentTest.get().pass("Name matched for android pass");

        String newStatus = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("statusLabel"));
        Assert.assertEquals(dataSet.get("status"), newStatus);
        logger.info("Status matched for android pass");
        TestListeners.extentTest.get().pass("Status matched for android pass");
    }
    
   
    
    @Test(description = "Notification Template Check for Android Offer" ,dataProvider= "T5876_testDataProvider")
	public void T5876_verifyAndroidPassForUser(String slug , String adminauthorization) throws Exception {
	
    	pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), slug);
        dataSet.putAll(pageObj.readData().readTestData);

		//Points to Reward notifciation check on PTC business
		// Enable Passes in Cockpit
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	    pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkUncheckFlagOnCockpitDasboard("Enable Passes?", "check");
					
		// create a user
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
		dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		TestListeners.extentTest.get().pass("Verified user signup via API v1");
		String userID = signUpResponse.jsonPath().get("id").toString();
		String authenticationToken=signUpResponse.jsonPath().get("authentication_token").toString();
		 
		//Navigate to guest Timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
	
	// For newptc: send offer first, then send points; otherwise send only points
		Response sendGiftResponse;
	    if ("newptc".equalsIgnoreCase(slug)) {
	        // send offer
	        Response offerResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID, dataSet.get("businessAdminKey"), dataSet.get("redeemable_id"), dataSet.get("end_date"));
	        Assert.assertEquals(offerResponse.getStatusCode(), 202, "Status code 202 did not match for offer gifting");
	        logger.info("Successfully gifted Redeemable (offer) to user ");
	        TestListeners.extentTest.get().pass("Successfully gifted redeemable (offer) to user ");
	
	        // on offer success, send points
	        Response pointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID, adminauthorization, dataSet.get("points"));
	        Assert.assertEquals(pointsResponse.getStatusCode(), 202, "Status code 202 did not match for points gifting");
	        logger.info("Successfully gifted Points to user ");
	        TestListeners.extentTest.get().pass("Successfully gifted Points to user ");
	
	        sendGiftResponse = pointsResponse;
	    } else {
	        // default: send points only
	        sendGiftResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID, adminauthorization, dataSet.get("points"));
	        Assert.assertEquals(sendGiftResponse.getStatusCode(), 202, "Status code 202 did not match for points gifting");
	        logger.info("Successfully gifted Points to user ");
	        TestListeners.extentTest.get().pass("Successfully gifted Points to user ");
    	}

		driver.navigate().refresh();
		Thread.sleep(8000);
		driver.navigate().refresh();
		Thread.sleep(8000);
		
		String notificationText = pageObj.guestTimelinePage().getPushNotificationText();
	    logger.info("Notification text on user timeline: " + notificationText);
	
	    pageObj.guestTimelinePage().extractUrlFromNotificationText(notificationText);
	    
	    //click on add to wallet button
	    pageObj.guestTimelinePage().clickOnAddToWalletButton();
	    logger.info("Redeemable unlocked notification triggered and pass added to wallet succssfully"); 
	    Thread.sleep(2000);
}
    @DataProvider(name = "T5876_testDataProvider")
	public Object[][] T5876_testDataProvider() {
		return new Object[][] {
			{"nativegrill", "SdYjsdn4HszucvC-pqsD"},
			{ "grubburger", "5bmcQJMs6bc3ZswKWpv1"},
			{ "newptc", "k8nc5kxGxj3mz4_DsnCr"}	
		};
	}

   @AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}    
}
    
    


