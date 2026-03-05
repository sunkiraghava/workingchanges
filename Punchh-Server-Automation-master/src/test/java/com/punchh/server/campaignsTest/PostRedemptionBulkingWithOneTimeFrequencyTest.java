package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostRedemptionBulkingWithOneTimeFrequencyTest {
	private static Logger logger = LogManager.getLogger(PostRedemptionBulkingWithOneTimeFrequencyTest.class);
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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//bulking for signup and post redemption is not working on production so  we can disable it for some time atleast for this quarter
	// Author: Amit Kumar
	@Test(description = "SQ-T6382 Verify gifting from post redemption offer campaign with guest frequency One Time and when bulking is enable/disable"
			+ "SQ-T6579 Verify PostRedemption campaign with bulking", dataProvider = "testDataProviderA", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T6382_verifyGiftingFromPostRedemptionOfferCampaignWithGuestFrequencyOneTimeAndWhenBulkingIsEnableDisable(
			String flagStatus, int waitTime) throws InterruptedException, ParseException {
		// enable Deactivate campaign when max gifting breached in global config

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send three reward redeemable_id to user reward 1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");
		// reward 2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers for 1st redemption
		Response offerResponse1 = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id1 = offerResponse1.jsonPath().get("rewards[0].reward_id").toString();
		String reward_id2 = offerResponse1.jsonPath().get("rewards[1].reward_id").toString();
		logger.info(reward_id1, reward_id2);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Create Redemption code using "reward_id" (fetch redemption code) for 1st
		// redemption
		Response redemptionResponse1 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id1);
		Assert.assertEquals(redemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption code using reward_id");
		String redemption_code1 = redemptionResponse1.jsonPath().get("redemption_tracking_code").toString();
		TestListeners.extentTest.get().pass("redemption code is created successfully using reward_id");

		// Create Redemption code using "reward_id" (fetch redemption code) for 2nd
		// redemption
		Response redemptionResponse2 = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id2);
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption code using reward_id");
		String redemption_code2 = redemptionResponse2.jsonPath().get("redemption_tracking_code").toString();
		TestListeners.extentTest.get().pass("redemption code is created successfully using reward_id");

		// Create post redemption Campaign
		campaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
		logger.info("Campaign name is :" + campaignName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

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
		// pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable("$1.0 OFF");
		pageObj.signupcampaignpage().setGuestFrequencyOfGiting(dataSet.get("guestFrequencyOption"));
		pageObj.signupcampaignpage().setFiniteGifting("1");
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

		// user timeline Validation navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(waitTime);

		// perform redemption Pos redemption api for 2nd redemption
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		Response resp2 = pageObj.endpoints().posRedemptionOfCode(userEmail, date2, redemption_code2, key2, txn2,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// user timeline Validation navigate to user timeline
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGiftedWithPooling(campaignName, 1);
		Assert.assertTrue(itemCount == 1, "reward gifted count did not matched");
		TestListeners.extentTest.get()
				.pass("reward gifting happenend twice matched with campaign frequency limit on timeline");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// check if campaign gets deactivated after max gifting
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(campaignName, "Inactive");
		Assert.assertEquals(camStatus, "Inactive", "Camapign not deactiaved after max gifting");
		logger.info("Camapign gets deactiaved after max gifting");
		TestListeners.extentTest.get().pass("Camapign gets deactiaved after max gifting");

	}

	@DataProvider(name = "testDataProviderA")
	public Object[][] testDataProviderA() {

		return new Object[][] {
				// {flagStatus, waittime},
				{ "uncheck", 5 }, /* { "check", 5 }, */ };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
