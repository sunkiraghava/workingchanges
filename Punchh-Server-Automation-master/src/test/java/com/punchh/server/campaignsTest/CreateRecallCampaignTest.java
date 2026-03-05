package com.punchh.server.campaignsTest;

// AUTHOR - Vansham
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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class CreateRecallCampaignTest {

	private static Logger logger = LogManager.getLogger(CreateRecallCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String userEmail;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String recallCampaignName;
	private Utilities utils;

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
		utils = new Utilities(driver);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2549 Verify Campaign Recall", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2549_verifyCampaignRecall() throws Exception {

		// user creation using pos signup api

		recallCampaignName = "Automation Recall Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable recall campaigns bulk segment", "uncheck");
		// Click Guest menu Link
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		String days = pageObj.lapsedguestPage().getLapsedGuestsDetails("2"); // 1 days user

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("$5 Off");
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().getLapsedGuestsDetails("2"); // 1 days user
		pageObj.lapsedguestPage().getUser("2");

		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(recallCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();

		Assert.assertTrue(campaignName.equalsIgnoreCase(recallCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass(
				"Recall campaign campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@Test(description = "SQ-T5440 Verify Re-gifting with the recall campaign", groups = { "regression",
			"dailyrun","nonNightly" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void verifyRegiftingWithRecallCampaign() throws InterruptedException, ParseException {
		recallCampaignName = "Regifting Recall Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String days = "4";
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("Base Redeemable");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();
		pageObj.utils().refreshPage();

		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(recallCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(recallCampaignName), "Campaign name did not matched");
		utils.logPass("Recall campaign with name " + recallCampaignName + " is found on user Timeline");

		// check whether push notification is received by the user or not
		String pushNotification = pageObj.guestTimelinePage().getcampaignName();

		logger.info(pushNotification + " : is the displayed push notification");
		// Go to the scheduler and again run the scheduler which was created for the
		// recall campaign
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();
		// again navigate to the timeline of the same user and verify that user should
		// not be gifted again with the same reward for recall campaign
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();
		int reGifting = pageObj.guestTimelinePage().verifyRegiftingOfRecallCampaign(recallCampaignName);
		if (reGifting == 1) {
			Assert.assertEquals(reGifting, 1,
					"No Reward has been given to the user on re-running the recall campaign schedule");
			utils.logPass("Only One Reward has been given to the user on re-running the recall campaign schedule");
		} else if (reGifting >= 1) {
			logger.info("More than One Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("More than One Reward has been given to the user on re-running the recall campaign schedule");
		}
		// if regifting count is 0 then also add the assertion and fail the test case
		else if (reGifting == 0) {
			logger.info("No Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("No Reward has been given to the user on re-running the recall campaign schedule");
		}
	}

	@Test(description = "SQ-T5712 Verify segment API call count in case of Recall campaign based on recall campaigns bulk segment flag", groups = {
			"regression", "dailyrun", "nonNightly" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T5712_verifySegmentAPICallCountInCaseOfRecallCampaignBasedOnRecallCampaignsBulkSegmentFlag()
			throws InterruptedException {

		// user creation using pos signup api

		recallCampaignName = "Automation Recall Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable recall campaigns bulk segment", "check");
		// Click Guest menu Link
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		String days = pageObj.lapsedguestPage().getLapsedGuestsDetails("3"); // 2 days user

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("$2.0 OFF");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().getLapsedGuestsDetails("3"); // 2 days user
		pageObj.lapsedguestPage().getUser("3");

		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(recallCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();

		Assert.assertTrue(campaignName.equalsIgnoreCase(recallCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass(
				"Recall campaign campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(recallCampaignName);
		 */
	}

	@Test(description = "SQ-T6142 Verify Campaign Recall With Email Template And Dynamic Tags", groups = {
			"regression" ,"nonNightly" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6142_verifyCampaignRecallWithEmailTemplateAndDynamicTags() throws Exception {

		// user creation using pos signup api

		recallCampaignName = "Automation Recall Campaign" + CreateDateTime.getTimeDateString();
		userEmail = dataSet.get("email");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable recall campaigns bulk segment", "uncheck");
		// Click Guest menu Link
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		String days = pageObj.lapsedguestPage().getLapsedGuestsDetails("2"); // 1 days user

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("New Redeemable");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		// pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setLapseDays(days);

		// Select Email Template
		pageObj.emailTemplatePage().searchAndSelectEmailTemplate(dataSet.get("emailsubject"));

		// Step 4: Send a test notification with gift to verify dynamic tags
		Thread.sleep(5000);
		pageObj.campaignspage().sendTestNotificationWithGift(dataSet.get("email"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().removeSearchedCampaign(recallCampaignName);

		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// fetch campaign name and reward gifted in account history
		// String campaignName =
		// pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(recallCampaignName,
		// dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(recallCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		// Assert.assertTrue(campaignName.contains(massCampaignName), "Campaign name did
		// not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(recallCampaignName),
				"Gifted redeemable did not appeared in account history");
		TestListeners.extentTest.get()
				.pass("Mass offer with dynamic tags using email template validated successfully on timeline");

	}

	@Test(description = "SQ-T6404 Verify gifting from recall campaign when re-arch flow enable ", groups = {
			"regression", "dailyrun","nonNightly" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T6404_verifyGiftingFromRecallCampaignWithReArchFlowEnabled()
			throws InterruptedException, ParseException {
		recallCampaignName = "Regifting Recall Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		// navigate to tab
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Recall campaign service enabled", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Recall campaign rearch", "check");
		String days = "4";
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("Base Redeemable");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();

		// check whether push notification is received by the user or not
		// String pushNotification = pageObj.guestTimelinePage().getcampaignName();
		String pushNotification = pageObj.guestTimelinePage().getcampaignNameWithWait(recallCampaignName);
		Assert.assertEquals(pushNotification, recallCampaignName,
				"Push notification did not match with expected value");
		utils.logPass("Push notification for recall campaign is displayed successfully on timeline");
		// get the current url
		String currentUrl = driver.getCurrentUrl();
		// verify jobs in sidekiq
//		int count =
//				pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "CampaignService::RecallCampaignsRunnerRearch", 100);
//		Assert.assertTrue(count>0,"CampaignService::RecallCampaignsRunnerRearch is not called in sidekiq");
//		logger.info("CampaignService::RecallCampaignsRunnerRearch is called in sidekiq");
//		TestListeners.extentTest.get().info("CampaignService::RecallCampaignsRunnerRearch is called in sidekiq");
//
//		int count2 =
//				pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "RecallBulkingWorker", 100);
//		Assert.assertTrue(count2>0,"RecallBulkingWorker is not called in sidekiq");
//		logger.info("RecallBulkingWorker is called in sidekiq");
//		TestListeners.extentTest.get().info("RecallBulkingWorker is called in sidekiq");
		// Go to the scheduler and again run the scheduler which was created for the
		// again navigate to the timeline of the same user and verify that user should
		// not be gifted again with the same reward for recall campaign
		driver.get(currentUrl);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();
		int reGifting = pageObj.guestTimelinePage().verifyRegiftingOfRecallCampaign(recallCampaignName);
		if (reGifting == 1) {
			Assert.assertEquals(reGifting, 1,
					"No Reward has been given to the user on re-running the recall campaign schedule");
			utils.logPass("Only One Reward has been given to the user on re-running the recall campaign schedule");
		} else if (reGifting >= 1) {
			logger.info("More than One Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("More than One Reward has been given to the user on re-running the recall campaign schedule");
		}
		// if regifting count is 0 then also add the assertion and fail the test case
		else if (reGifting == 0) {
			logger.info("No Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("No Reward has been given to the user on re-running the recall campaign schedule");
		}
	}

	@Test(description = "SQ-T6405 Verify gifting from recall campaign when re-arch flow disable ", groups = {
			"regression", "dailyrun", "nonNightly" }, priority = 5)
	@Owner(name = "Vansham Mishra")
	public void T6405_verifyGiftingFromRecallCampaignWithReArchFlowDisabled()
			throws InterruptedException, ParseException {
		recallCampaignName = "Regifting Recall Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		// navigate to tab
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Recall campaign service enabled", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Recall campaign rearch", "uncheck");
		String days = "4";
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("Base Redeemable");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().runRecallScheule();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();

		// check whether push notification is received by the user or not
		// String pushNotification = pageObj.guestTimelinePage().getcampaignName();
		String pushNotification = pageObj.guestTimelinePage().getcampaignNameWithWait(recallCampaignName);
		Assert.assertEquals(pushNotification, recallCampaignName,
				"Push notification did not match with expected value");
		utils.logPass("Push notification for recall campaign is displayed successfully on timeline");
		// get the current url
		String currentUrl = driver.getCurrentUrl();
		// verify jobs in sidekiq
//		int count =
//				pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "CampaignService::RecallCampaignsRunnerRearch", 5);
//		Assert.assertTrue(count>0,"CampaignService::RecallCampaignsRunnerRearch is not called in sidekiq");
//		logger.info("CampaignService::RecallCampaignsRunnerRearch is called in sidekiq");
//		TestListeners.extentTest.get().info("CampaignService::RecallCampaignsRunnerRearch is called in sidekiq");
//
//		int count2 =
//				pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "RecallBulkingWorker", 5);
//		Assert.assertTrue(count2>0,"RecallBulkingWorker is not called in sidekiq");
//		logger.info("RecallBulkingWorker is called in sidekiq");
//		TestListeners.extentTest.get().info("RecallBulkingWorker is called in sidekiq");
		// Go to the scheduler and again run the scheduler which was created for the
		// again navigate to the timeline of the same user and verify that user should
		// not be gifted again with the same reward for recall campaign
		driver.get(currentUrl);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		pageObj.lapsedguestPage().clickLapsedUser();
		int reGifting = pageObj.guestTimelinePage().verifyRegiftingOfRecallCampaign(recallCampaignName);
		if (reGifting == 1) {
			Assert.assertEquals(reGifting, 1,
					"No Reward has been given to the user on re-running the recall campaign schedule");
			utils.logPass("Only One Reward has been given to the user on re-running the recall campaign schedule");
		} else if (reGifting >= 1) {
			logger.info("More than One Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("More than One Reward has been given to the user on re-running the recall campaign schedule");
		}
		// if regifting count is 0 then also add the assertion and fail the test case
		else if (reGifting == 0) {
			logger.info("No Reward has been given to the user on re-running the recall campaign schedule");
			TestListeners.extentTest.get()
					.fail("No Reward has been given to the user on re-running the recall campaign schedule");
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(recallCampaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
