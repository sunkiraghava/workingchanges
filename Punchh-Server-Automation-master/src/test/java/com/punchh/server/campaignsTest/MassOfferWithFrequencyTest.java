package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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
public class MassOfferWithFrequencyTest {

	private static Logger logger = LogManager.getLogger(MassOfferWithFrequencyTest.class);
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5275 Mass offer campaign with segment once frequecy users and sto", groups = {
			"regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T5275_verifyMassOfferCampaignWithSegmentOnceFrequencySTO() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		campaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				campaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().setStoOn();
		boolean stostatus = pageObj.signupcampaignpage().setStoOn();
		pageObj.signupcampaignpage().scheduleCampaign();
		Assert.assertTrue(stostatus, "STO status did not matched for Once frequency");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */
		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * pageObj.guestTimelinePage().clickAccountHistory(); String pageData
		 * =pageObj.accounthistoryPage().getPageData();
		 */

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("100 Bonus points earned-" + campaignName),
				"Gifted points did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Mass offer points campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");
	}

	@Test(description = "SQ-T3637 Mass Offer Campaign With Frequency Daily and sto presence || "
			+ "T4562 Verify the presence of processed mass offer campaign in finished tab on superadmin page || "
			+ "SQ-T4564 Verify the archival of mass offer campaign with schedule status || "
			+ "SQ-T6099 Verify on schedule page when click on Mass campaign and segment export name it should be navigate to preview page - Mass campaign- Processed ", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void verify_MassCampaign_With_Frequency_Daily() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				campaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequency("Daily");
		boolean stostatus = pageObj.signupcampaignpage().setStoOn();
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertFalse(stostatus, "STO status did not matched for daily frequency");
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// check the archival status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(campaignName);
		boolean flag = pageObj.newCamHomePage().checkOptionPresent(dataSet.get("option"));
		Assert.assertFalse(flag, "Mass offer campaign is in scheduled state but the Archive option is present");
		pageObj.utils().logPass(
				"Verfied when mass offer campaign is in scheduled state as expected archieve option is not present");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer 1st time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			pageObj.utils().logPass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer 2nd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			pageObj.utils().logPass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer 3rd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		long giftedCount = pageObj.guestTimelinePage().getAccountHistoryForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(giftedCount, 3,
				"Campaign reward gifted did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass offer campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// search campaign and get the id
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignId = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		// SQ-T6099 Verify on schedule page when click on Mass campaign and segment
		// export name it should be navigate to preview page - Mass campaign- Processed
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		boolean nameCheckFlag = pageObj.schedulespage().clickOnScheduledFunctionalityName("campaign", campaignName);
		Assert.assertTrue(nameCheckFlag,
				"Clicked Mass Campaign is not redirecting to the same Mass campaign performance page");
		pageObj.utils().logPass(
				"Verified that clicking on a processed Mass Campaign redirects to its corresponding performance page.");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		// check if the campaign present in finished campaign
		boolean val = pageObj.campaignspage().searchCampaignInFinishedTab(campaignName, campaignId);
		Assert.assertTrue(val, "Processed mass campaign is not present in the finished tab");
		pageObj.utils().logPass("Verified processed mass campaign is not present in the finished tab");

	}

	@Test(description = "SQ-T3379 Mass Offer Campaign With Frequency Weekly and sto presence || "
			+ "SQ-T6099 Verify on schedule page when click on Mass campaign and segment export name it should be navigate to preview page - Mass campaign- schedule for future date", groups = {
					"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void verify_MassCampaign_With_Frequency_Weekly() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				campaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequency("Weekly");
		boolean stostatus = pageObj.signupcampaignpage().setStoOn();
		pageObj.signupcampaignpage().setRepeatAndDaysOfWeek("2", "Monday");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(15) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertFalse(stostatus, "STO status did not matched for weekly frequency");
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer 1st time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			pageObj.utils().logPass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer 2nd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			pageObj.utils().logPass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer 3rd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		long giftedCount = pageObj.guestTimelinePage().getAccountHistoryForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(giftedCount, 3,
				"Campaign reward gifted did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass offer campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// SQ-T6099 Verify on schedule page when click on Mass campaign and segment
		// export name it should be navigate to preview page - Mass campaign- schedule
		// for future date
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		boolean nameCheckFlag = pageObj.schedulespage().clickOnScheduledFunctionalityName("campaign", campaignName);
		Assert.assertTrue(nameCheckFlag,
				"Clicked Mass Campaign is not redirecting to the same Mass campaign performance page");
		pageObj.utils().logPass(
				"Verified that clicking on a future date Mass Campaign redirects to its corresponding performance page.");

		/*
		 * TestData.AddTestDataToWriteInJSON("email", iFrameEmail);
		 * TestData.EditOrAddNewGivenFieldForGivenScenarioFromJson(TestData.
		 * getJsonFilePath(run , env), sTCName);
		 */
	}

	@Test(description = "SQ-T3380 Mass Offer Campaign With Frequency Monthly and sto presence", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void verify_MassCampaign_With_Frequency_Monthly() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				campaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequency("Monthly");
		boolean stostatus = pageObj.signupcampaignpage().setStoOn();
		pageObj.signupcampaignpage().setRepeatOn("1");
		pageObj.signupcampaignpage().setRepeatAndDaysOfWeek("1", "Monday");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(120) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertFalse(stostatus, "STO status did not matched for monthly frequency");
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer 1st time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			pageObj.utils().logPass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer 2nd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			pageObj.utils().logPass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			pageObj.utils().logit(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer 3rd time
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		long giftedCount = pageObj.guestTimelinePage().getAccountHistoryForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(giftedCount, 3,
				"Campaign reward gifted did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass offer campaign detail: push notification, campaign name, reward notification validated successfully on timeline");
	}

	@Test(description = "SQ-T3678 Mass Campaign Send test Notification with including the gifts flag", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void verify_MassCampaignSendTestNotificationWithIncludingGiftsFlag() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();
		pageObj.signupcampaignpage().setSendNotificationGuest(userEmail);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign(); boolean
		 * rewardedRedeemableStatus =
		 * pageObj.guestTimelinePage().verifyrewardedRedeemableMassCampaign();
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String massCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(massCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redemable")),
				"Gifted reward did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Mass offer campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		/*
		 * TestData.AddTestDataToWriteInJSON("email", iFrameEmail);
		 * TestData.EditOrAddNewGivenFieldForGivenScenarioFromJson(TestData.
		 * getJsonFilePath(run , env), sTCName);
		 */
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
