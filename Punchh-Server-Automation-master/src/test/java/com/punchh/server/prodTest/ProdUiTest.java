package com.punchh.server.prodTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ProdUiTest {
	static Logger logger = LogManager.getLogger(ProdUiTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String iFrameEmail;
	private String userEmail;
	private String sTCName;
	String transactionNumber, externalUid;

	ApiUtils apiUtils;
	Utilities utils;
	SeleniumUtilities selUtils;
	private String baseUrl, env, run = "ui";
	private static Map<String, String> dataSet;
	private String client;
	private String secret;
	private String locationKey;

	@BeforeTest
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		sTCName = method.getName();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2233 Sign Up Campaign Trigger For User", groups = "Sanity", priority = 0)
	public void T2233_verifySignupCampaignTriggersonUserCreation() throws Exception {
		// String val=utils.encrypt("geyq2xxns5pg2b5q3pird3igtdcdavup");
		// String pwd=utils.decrypt("wWOJxmc+MCtcFisGAceurw==");
		// System.out.println(pwd);
		// String buisness = prop.getProperty("prodSlug");
		String signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		// pageObj.instanceDashboardPage().selectBusiness(buisness);

		client = pageObj.oAuthAppPage().getClient();
		secret = pageObj.oAuthAppPage().getSecret();
		locationKey = pageObj.oAuthAppPage().getLocationKey();

		System.out.println("client is ==> " + client);
		System.out.println("secret is ==> " + secret);
		System.out.println("locationKey is ==> " + locationKey);

		// Click Campaigns Link
		Thread.sleep(2000);
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(signUpCampaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, locationKey);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		boolean rewardedRedeemableStatus = pageObj.guestTimelinePage().verifyrewardedRedeemable();

		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
			softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
			softassert.assertTrue(rewardedRedeemableStatus, "Rewarded Redeemable notification did not displayed...");
			softassert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
			softassert.assertAll();
			utils.logPass(
					"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating signup campaign details on timeline" + e);
			Assert.fail("Error in validating signup campaign details on timeline" + e);
		}
		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue("Signup");
		pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);

	}

	@Test(description = "SQ-T2187 Post Checkin Campaign Trigger For User", groups = "Sanity", priority = 1)
	public void T2187_verifyPostCheckinCampaignTriggerForUser() throws InterruptedException {
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");

		// Click Campaigns Link
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(iFrameEmail, locationKey);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		Thread.sleep(20000);
		// String iFrameEmail="autoIframe11574729032022g9pnvc@punchh.com"; //prod user
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, iFrameEmail, key, txn, locationKey);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(postCheckinCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		// boolean rewardedRedeemableStatus =
		// pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();

		try {
			SoftAssert softassert = new SoftAssert();
			softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
			softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
			// softassert.assertTrue(rewardedRedeemableStatus, "Rewarded Redeemable
			// notification did not displayed...");
			softassert.assertTrue(campaignName.equalsIgnoreCase(postCheckinCampaignName),
					"Campaign name did not matched");
			softassert.assertAll();

			utils.logPass(
					"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		} catch (Exception e) {
			logger.error("Error in validating postcheckin campaign details on timeline" + e);
			Assert.fail("Error in validating postcheckin campaign details on timeline" + e);
		}

		// Delete created campaign
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2167 Verify the redemption of Amount", groups = "Sanity", priority = 2)
	public void T2167_verifytheredemptionofAmount() throws InterruptedException {

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, locationKey);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messageRewardAmountToUser(dataSet.get("subject"), dataSet.get("location"),
				dataSet.get("giftTypes"), dataSet.get("amount"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		// Pos redemption of amount
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date, key, txn,
				dataSet.get("redeemAmount"), locationKey);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		// String discountValuePosCheckin =
		// pageObj.guestTimelinePage().getDiscountValuePosApprovedLoyaltyDetails();
		// String discountedAmount =
		// pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not decreased in account balance");
		utils.logPass("Redemption of reward is validated in acount history");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2186 Verify the redemption of Reward", groups = "Sanity", priority = 3)
	public void T2186_VerifyredemptionofReward() throws InterruptedException {

		// User register/signup using API2 Signup
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, client, secret);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");

		// fetch user offers using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, client, secret);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit(reward_id);

		// Pos redemption of reward id
		Response resp = pageObj.endpoints().posRedemptionOfReward(iFrameEmail, locationKey, reward_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed") | Itemdata.get(0).contains("Points Earned"),
				"Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not decreased in account balance");
		utils.logPass("Redemption of reward id is validated in acount history");

	}

	@Test(groups = { "sanity" }, priority = 4)
	public void T2189_Api1UserSignUpAndLoginValidation() throws InterruptedException {
		utils.logit("== API 1 user signup Test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);
		apiUtils.verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
		Response loginResponse = pageObj.endpoints().Api1UserLogin(userEmail, client, secret);
		apiUtils.verifyResponse(loginResponse, "API 1 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Verifying user Signup on guest timeline
		Thread.sleep(20000);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(prop.getProperty("joinedViaMobile"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utils.logPass("Successfully verified guest email and joined channel");
	}

	@Test(description = "SQ-T2358 Validate Login/Signup from API 2", groups = "sanity", priority = 5)
	public void T2358_Api2UserSignUpValidation() throws InterruptedException {
		utils.logit("== API 2 user signup Test ==");

		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, client, secret);
		apiUtils.verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, client, secret);
		apiUtils.verifyResponse(loginResponse, "API 2 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());

		// Verifying user Signup on timeline
		Thread.sleep(20000);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utils.logPass("Successfully verified guest email and joined channel");

	}

	@Test(description = "SQ-T2172 POS user signup test", priority = 6)
	public void T2172_PosUserSignUp() throws InterruptedException {
		utils.logit("== POS user signup and test ==");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, locationKey);
		apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// Verify User Signin OnTimeline
		Thread.sleep(20000);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelPOS();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(prop.getProperty("joinedViaPOS"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utils.logPass("Successfully verified guest email and joined channel");
	}

	@Test(description = "SQ-T2204 Verify Online Order Checkin >> Check-in through valid details.", groups = "Sanity", priority = 7)
	public void T2204_authOnlineOrderCheckinTest() throws InterruptedException {
		utils.logit("== Online order checkin test ==");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret);

		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// System.out.println(response.prettyPrint());
		String authToken = response.jsonPath().get("authentication_token");
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, client, secret);

		apiUtils.verifyCreateResponse(loginResponse, "Auth API user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed - Auth API user login");
		String amount = "110";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, client, secret);
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		externalUid = pageObj.endpoints().externalUid;
		transactionNumber = pageObj.endpoints().transactionNumber;

		SoftAssert softAssertion = new SoftAssert();
		Thread.sleep(20000);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(prop.getProperty("joinedViaMobile"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		utils.logPass("Successfully verified guest email and joined channel");
		/*
		 * softAssertion.assertTrue(pageObj.guestTimelinePage().
		 * verifyAuthCheckinInTimeLine(externalUid, amount,
		 * prop.getProperty("baseConversionRate")),
		 * "Error in verifying Checkin tiemline");
		 */
		softAssertion.assertTrue(
				pageObj.guestTimelinePage().verifyCheckinChannelAndLocation("OnlineOrder", "Smyrna - 3132"),
				"Error in capturing checkin channel ");
		softAssertion.assertAll();
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
	}

	@Test(description = "SQ-T2225 Verify dashboard stats and graph on any business", groups = "Regression", priority = 8)
	public void T2225_verifydashboardstats_andgraphonanybusiness() throws InterruptedException {

		pageObj.menupage().clickDashboardMenu();
		pageObj.instanceDashboardPage().closeBanner();
		// String pwd=utils.decrypt("UyBqYWnaTSJKa5F5+lyplg==");

		// utils.encrypt("6wespyiedouv55pq2m3klotkj3go4ur6");
		// verify dashboard stats for all locations filter and one month filter
		pageObj.dashboardpage().selectLocation("All Locations");
		pageObj.dashboardpage().selectDataRange("Last Week");
		Thread.sleep(3000);
		try {
			List<String> topStatsTilesname = pageObj.dashboardpage().getTopStatsCards();

			System.out.println(topStatsTilesname);
			Assert.assertTrue(topStatsTilesname.contains("AvgVisits"));
			Assert.assertTrue(topStatsTilesname.contains("LoyaltySales"));
			Assert.assertTrue(topStatsTilesname.contains("ParticipationRate"));
			Assert.assertTrue(topStatsTilesname.contains("SpendLift"));

			boolean signupsChart = pageObj.dashboardpage().signupsChart();
			boolean checkinSalesGraph = pageObj.dashboardpage().checkinSalesGraph();
			boolean loyaltyCheckinsGraph = pageObj.dashboardpage().loyaltyCheckinsGraph();
			Thread.sleep(3000);
			boolean redemptionsSalesGraph = pageObj.dashboardpage().redemptionsSalesGraph();
			boolean CampaignsTile = pageObj.dashboardpage().CampaignsTile();
			boolean appDownloadsGraph = pageObj.dashboardpage().appDownloadsGraph();

			Assert.assertTrue(signupsChart, "Signup chart did not displayed");
			Assert.assertTrue(checkinSalesGraph, "Loyalty Checkins and Sales chart did not displayed");
			Assert.assertTrue(loyaltyCheckinsGraph, "Loyalty Checkins chart did not displayed");
			Assert.assertTrue(redemptionsSalesGraph, "Redemptions & Sales chart did not displayed");
			Assert.assertTrue(CampaignsTile, "Campaigns tile did not displayed");
			Assert.assertTrue(appDownloadsGraph, "App Downloads  chart did not displayed");
			utils.logPass("Dashboard stats verified for all locations");
		} catch (Exception e) {
			logger.error("Error in verifying dashboard stats for all locations" + e);
			TestListeners.extentTest.get().fail("Error in verifying dashboard stats for all locations" + e);
			Assert.fail("Error in verifying dashboard stats for all locations" + e);
		}
		// verify dashboard stats for one locations filter and one month filter
		pageObj.dashboardpage().selectLocation("Daphne - 66233");
		pageObj.dashboardpage().selectDataRange("Last Month");
		Thread.sleep(3000);
		try {
			List<String> topStatsTilesname = pageObj.dashboardpage().getTopStatsCards();
			/*
			 * Assert.assertTrue(topStatsTilesname.equals(actualtopStatsTilesname),
			 * "Top Stats Tiles name did not matched on dashboard page");
			 */

			Assert.assertTrue(topStatsTilesname.contains("AvgVisits"));
			Assert.assertTrue(topStatsTilesname.contains("LoyaltySales"));
			Assert.assertTrue(topStatsTilesname.contains("ParticipationRate"));
			Assert.assertTrue(topStatsTilesname.contains("SpendLift"));
//				System.out.println(actualtopStatsTilesname);
//				System.out.println(topStatsTilesname);
			/*
			 * Assert.assertTrue(topStatsTilesname.equals(actualtopStatsTilesname),
			 * "Top Stats Tiles name did not matched on dashboard page");
			 */

			boolean signupsChart = pageObj.dashboardpage().signupsChart();
			boolean checkinSalesGraph = pageObj.dashboardpage().checkinSalesGraph();
			boolean loyaltyCheckinsGraph = pageObj.dashboardpage().loyaltyCheckinsGraph();
			Thread.sleep(3000);
			boolean redemptionsSalesGraph = pageObj.dashboardpage().redemptionsSalesGraph();
			boolean CampaignsTile = pageObj.dashboardpage().CampaignsTile();
			boolean appDownloadsGraph = pageObj.dashboardpage().appDownloadsGraph();

			Assert.assertTrue(signupsChart, "Signup chart did not displayed");
			Assert.assertTrue(checkinSalesGraph, "Loyalty Checkins and Sales chart did not displayed");
			Assert.assertTrue(loyaltyCheckinsGraph, "Loyalty Checkins chart did not displayed");
			Assert.assertTrue(redemptionsSalesGraph, "Redemptions & Sales chart did not displayed");
			Assert.assertTrue(CampaignsTile, "Campaigns tile did not displayed");
			Assert.assertTrue(appDownloadsGraph, "App Downloads  chart did not displayed");
			utils.logPass("Dashboard stats verified for one location");
		} catch (Exception e) {
			logger.error("Error in verifying dashboard stats for one location" + e);
			TestListeners.extentTest.get().fail("Error in verifying dashboard stats for one location" + e);
			Assert.fail("Error in verifying dashboard stats for one location" + e);
		}
		// verify dashboard stats for group locations filter and one month filter
		pageObj.dashboardpage().selectLocation("Automation Group (4)");
		pageObj.dashboardpage().selectDataRange("Last 90 Days");
		Thread.sleep(3000);
		try {
			List<String> topStatsTilesname = pageObj.dashboardpage().getTopStatsCards();
			/*
			 * Assert.assertTrue(topStatsTilesname.equals(actualtopStatsTilesname),
			 * "Top Stats Tiles name did not matched on dashboard page");
			 */
			Assert.assertTrue(topStatsTilesname.contains("AvgVisits"));
			Assert.assertTrue(topStatsTilesname.contains("LoyaltySales"));
			Assert.assertTrue(topStatsTilesname.contains("ParticipationRate"));
			Assert.assertTrue(topStatsTilesname.contains("SpendLift"));
			boolean signupsChart = pageObj.dashboardpage().signupsChart();
			boolean checkinSalesGraph = pageObj.dashboardpage().checkinSalesGraph();
			boolean loyaltyCheckinsGraph = pageObj.dashboardpage().loyaltyCheckinsGraph();
			Thread.sleep(3000);
			boolean redemptionsSalesGraph = pageObj.dashboardpage().redemptionsSalesGraph();
			boolean CampaignsTile = pageObj.dashboardpage().CampaignsTile();
			boolean appDownloadsGraph = pageObj.dashboardpage().appDownloadsGraph();
			Assert.assertTrue(signupsChart, "Signup chart did not displayed");
			Assert.assertTrue(checkinSalesGraph, "Loyalty Checkins and Sales chart did not displayed");
			Assert.assertTrue(loyaltyCheckinsGraph, "Loyalty Checkins chart did not displayed");
			Assert.assertTrue(redemptionsSalesGraph, "Redemptions & Sales chart did not displayed");
			Assert.assertTrue(CampaignsTile, "Campaigns tile did not displayed");
			Assert.assertTrue(appDownloadsGraph, "App Downloads  chart did not displayed");
			utils.logPass("Dashboard stats verified for group locations");
		} catch (Exception e) {
			logger.error("Error in verifying dashboard stats for group locations" + e);
			TestListeners.extentTest.get().fail("Error in verifying dashboard stats for group locations" + e);
			Assert.fail("Error in verifying dashboard stats for group locations" + e);
		}
		pageObj.dashboardpage().verifyLoyaltyCheckinsandSalesgraphswithAlloption();
		pageObj.dashboardpage().verifyRedemptionsandSalesGraphWithAllOptions();
		pageObj.dashboardpage().verifyBrowserLogs();
	}

	@AfterTest
	public void tearDown() {
		driver.quit();
		logger.info("Browser closed");
	}
}