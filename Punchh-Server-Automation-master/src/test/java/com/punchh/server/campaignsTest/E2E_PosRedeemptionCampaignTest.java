package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
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
public class E2E_PosRedeemptionCampaignTest {

	private static Logger logger = LogManager.getLogger(E2E_PosRedeemptionCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	// private SeleniumUtilities selUtils;;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String campaignName;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		// [autoseven:[1,2]]
	}

	@Test(priority = -1)
	public void setFlagCheckinsAndRedemptionsAsOn() throws InterruptedException {
		System.out.println("E2E_PosRedeemptionCampaignTest.setFlagCheckinsAndRedemptionsAsOn() START");
		// Login to instance

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Turn off Checkins?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Turn off Redemptions?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		System.out.println("E2E_PosRedeemptionCampaignTest.setFlagCheckinsAndRedemptionsAsOn(::END)");
	}

	@Test(description = "SQ-T4512 Verify the gifting from post redemption offer campaign when campaign has recurring_days")
	@Owner(name = "Rakhi Rawat")
	public void T4512_postRedeemptionOfferCampaignRecurringDays() throws InterruptedException, ParseException {
		campaignName = "AutoPosRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1)
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(2);

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.campaignspage().selectGuestFrequency(dataSet.get("guestFrequency"));
		pageObj.campaignspage().setFrequency(dataSet.get("time"), dataSet.get("days"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// create User
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		logger.info("Api2 user signup is successful");
		pageObj.utils().logPass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 2
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 3
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 3);

		for (int i = 0; i < 3; i++) {
			Response posRedeem1 = null;
			posRedeem1 = pageObj.endpoints().posRedemptionOfReward(iFrameEmail, dataSet.get("locationkey"),
					rewardLst.get(i));
			Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
			logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(i));
			pageObj.utils().logPass(
					"Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(i));
		}

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		int size = pageObj.guestTimelinePage().noOfTimesCampaignVisibleInTimeLine(campaignName, 2);
		Assert.assertEquals(size, 2, "campaign did not trigger two times");
		logger.info("Verified as expected campaign trigger 2 times");
		pageObj.utils().logPass("Verified as expected campaign trigger 2 times");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T4513 Verify the gifting from post redemption message campaign when campaign has recurring_days"
			+ "SQ-T5076 (1.0) Verify the gifting from post redemption message campaign", groups = { "unstable" })
	@Owner(name = "Rakhi Rawat")
	public void T4513_postRedeemptionMsgCampaignRecurringDays() throws InterruptedException, ParseException {
		campaignName = "AutoPosRedeemptionMsg" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1)
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(2);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("msgDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().posRedeemptionMsgCampaignSetName(campaignName);
		pageObj.campaignspage().selectGuestFrequencyPosRedemMsg(dataSet.get("guestFrequency"));
		pageObj.campaignspage().setFrequencyPosRedeem(dataSet.get("time"), dataSet.get("days"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemMsgCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// create User
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		logger.info("Api2 user signup is successful");
		pageObj.utils().logPass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 2
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 3
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 3);

		for (int i = 0; i < 3; i++) {
			Response posRedeem1 = null;
			posRedeem1 = pageObj.endpoints().posRedemptionOfReward(iFrameEmail, dataSet.get("locationkey"),
					rewardLst.get(i));
			Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
			logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(i));
			pageObj.utils().logPass(
					"Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(i));
		}

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		int size = pageObj.guestTimelinePage().noOfTimesCampaignVisibleInTimeLine(campaignName, 2);
		Assert.assertEquals(size, 2, "campaign did not trigger two times");
		logger.info("Verified as expected campaign trigger 2 times");
		pageObj.utils().logPass("Verified as expected campaign trigger 2 times");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4732 E2E -- Post Redemption Offer Campaign with Guest Targeted from Campaign type reg 09/06")
	@Owner(name = "Shashank Sharma")
	public void T4732_VerifyPostRedemptionWithCheckinSegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4732_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > 0, "Sgemnt guest count is :" + guestCount);
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"),
				dataSet.get("segmentID"));
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4732");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4731 E2E -- Post Redemption Offer Campaign with Average checkin type reg 09/06")
	@Owner(name = "Shashank Sharma")
	public void T4731_VerifyPostRedemptionWithAverageCheckinSegmentType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4731_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"),
				dataSet.get("segmentID"));
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4731");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4729 E2E -- Post Redemption offer test 04 sep with total redeemable")
	@Owner(name = "Shashank Sharma")
	public void T4729_VerifyPostRedemptionWithTotalRedemptionSegementType()
			throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4729_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"),
				dataSet.get("segmentID"));
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4729");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4727 E2E -- Post Redemption Offer with N type loyalty guest")
	@Owner(name = "Shashank Sharma")
	public void T4727_VerifyPostRedemptionWithTopNLoayltySegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4727_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"),
				dataSet.get("segmentID"));

		Assert.assertTrue(userEmail != null && !userEmail.isEmpty(),
				"User email is not fetched from segment, please check the segment");
		logger.info("User email fetched from segment: " + userEmail);
		pageObj.utils().logit("User email fetched from segment: " + userEmail);

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4727");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4726 E2E -- Post Redemption Offer with rewards type segment")
	@Owner(name = "Shashank Sharma")
	public void T4726_VerifyPostRedemptionWithRewardsSegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4726_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Redeemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("baseRedeemable_id"), "", "");

		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		int segmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		if (segmentCount != 0) {
			userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), dataSet.get("segmentID"));
		}
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4726");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4725 E2E -- Post Redemption Offer Campaign with checkin type segment reg 09/06")
	@Owner(name = "Shashank Sharma")
	public void T4725_VerifyPostRedemptionWithCheckinsSegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4725_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"),
				dataSet.get("segmentID"));
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4725");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// shashank
	@Test(description = "SQ-T4724 E2E -- Post Redemption Offer Campaign with No checkin in X duration type segment reg 09/06")
	@Owner(name = "Shashank Sharma")
	public void T4724_VerifyPostRedemptionWithNoCheckinsSegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4724_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), "155237");
//		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4724");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

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