package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostRedemptionBulkingWithMigratedSegmentTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//bulking for signup and post redemption is not working on production so  we can disable it for some time atleast for this quarter
	// Author: Amit Kumar
	@Test(description = "SQ-T6383 Verify gifting from post redemption offer campaign with migrated segment and guest frequency Custom and when bulking is enable/disable", dataProvider = "TestDataProvider", priority = 0)
	public void T6383_verifyPostRedemptionOfferCampaignWithMigratedSegmentAndGuestFrequencyCustomAndWhenBulkingIsEnableDisable(
			String flagStatus, int waitTime) throws InterruptedException, ParseException {

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create migrated guest
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String timeStamp = CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("locationName"),
				dataSet.get("gender"));
		boolean result = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(result, "Migration user is not created successfully");

		// User register/signup using API2 Signup
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send three reward redeemable_id to user reward 1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");
		// reward 2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");
		// reward 3
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers for 1st redemption
		Response offerResponse1 = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id1 = offerResponse1.jsonPath().get("rewards[0].reward_id").toString();
		String reward_id2 = offerResponse1.jsonPath().get("rewards[1].reward_id").toString();
		String reward_id3 = offerResponse1.jsonPath().get("rewards[2].reward_id").toString();
		pageObj.utils().logit("reward_ids: " + reward_id1 + ", " + reward_id2 + ", " + reward_id3);
		pageObj.utils().logPass("Api2 user fetch user offers is successful");

		// Create Redemption code using "reward_id" (fetch redemption code) for 1st
		// redemption
		Response redemptionResponse1 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id1);
		Assert.assertEquals(redemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption code using reward_id");
		String redemption_code1 = redemptionResponse1.jsonPath().get("redemption_tracking_code").toString();
		pageObj.utils().logPass("redemption code is created successfully using reward_id");

		// Create Redemption code using "reward_id" (fetch redemption code) for 2nd
		// redemption
		Response redemptionResponse2 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id2);
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption code using reward_id");
		String redemption_code2 = redemptionResponse2.jsonPath().get("redemption_tracking_code").toString();
		pageObj.utils().logPass("redemption code is created successfully using reward_id");

		// Create Redemption code using "reward_id" (fetch redemption code) for 3rd
		// redemption
		Response redemptionResponse3 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id3);
		Assert.assertEquals(redemptionResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption code using reward_id");
		String redemption_code3 = redemptionResponse3.jsonPath().get("redemption_tracking_code").toString();
		pageObj.utils().logPass("redemption code is created successfully using reward_id");

		// Create post redemption Campaign
		campaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
		pageObj.utils().logit("Campaign name is :" + campaignName);

		// enable/disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Bulking For Post Redemption Campaign", flagStatus);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable("$1.0 OFF");
		pageObj.signupcampaignpage().setGuestFrequencyOfGiting(dataSet.get("guestFrequencyOption"));
		pageObj.campaignspage().setFrequency(dataSet.get("time"), dataSet.get("days"));
		// pageObj.signupcampaignpage().enterTimesPerDay("2");
		// pageObj.signupcampaignpage().checkUncheckFlag("Re-gift", "ON");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// perform redemption Pos redemption api for 1st redemption
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key1 = CreateDateTime.getTimeDateString();
		Response resp1 = pageObj.endpoints().posRedemptionOfCode(userEmail, date1, redemption_code1, key1, txn1,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// perform redemption Pos redemption api for 2nd redemption
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		Response resp2 = pageObj.endpoints().posRedemptionOfCode(userEmail, date2, redemption_code2, key2, txn2,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// user timeline Validation navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGiftedWithPooling(campaignName, 2);
		Assert.assertTrue(itemCount == 2, "reward gifted count did not matched");
		pageObj.utils().logPass("reward gifting happenend twice matched with campaign frequency limit on timeline");

		pageObj.guestTimelinePage().pingSessionforLongWait(waitTime);
		// perform redemption Pos redemption api for 3rd redemption
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key3 = CreateDateTime.getTimeDateString();
		Response resp3 = pageObj.endpoints().posRedemptionOfCode(userEmail, date3, redemption_code3, key3, txn3,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// user timeline Validation navigate to user timeline
		pageObj.guestTimelinePage().refreshTimeline();
		int rewardGiftCount = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(campaignName);
		Assert.assertTrue(rewardGiftCount == 2, "reward gifted count did not matched");
		pageObj.utils().logPass("reward gifting happenend twice matched with campaign frequency limit on timeline");
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {flagStatus, waitTime},
				{ "uncheck", 2 }, /* { "check", 3 } */ };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}
