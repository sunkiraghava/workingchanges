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
public class MassNotificationCampaignTest {

	private static Logger logger = LogManager.getLogger(MassNotificationCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	CreateDateTime createDateTime;

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
		createDateTime = new CreateDateTime();
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2512 Verify mass notification campaign With segment triggered to all guest fulfilling particular criteria || SQ-T3014 Verify guest targeted and guest reachability count is matching in mass offer"
			+ "T4563 Verify the presence of processed mass notification campaign in finished tab on superadmin page"
			+ "SQ-T4565 Verify the archival of mass notification campaign with schedule status", groups = {
					"Regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2512_verifyMassNotificationCampaignWithSegmentTriggeredToAllGuestFulfillingParticularCriteria()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

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
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		int guestReachCount = pageObj.signupcampaignpage().guestsReachCount();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// check the archival status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		boolean flag = pageObj.newCamHomePage().checkOptionPresent(dataSet.get("option"));
		Assert.assertFalse(flag, "Mass notification campaign is in scheduled state but the Archive option is present");
		pageObj.utils().logPass(
				"Verfied when mass notification campaign is in scheduled state as expected archieve option is not present");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		TestListeners.extentTest.get().pass(
				"Mass Notification campaign detail: push notification, campaign name, validated successfully on timeline");

		// validate guest reachability
		/*
		 * pageObj.menupage().clickCampaignsMenu();
		 * pageObj.menupage().clickCampaignsLink(); // Select offer dropdown value
		 * Thread.sleep(2000);
		 * pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);
		 * pageObj.campaignspage().gotoClassiccampaignSummaryPage(); int
		 * val=pageObj.campaignspage().campaignEngagementStats();
		 * Assert.assertEquals(guestReachCount,val,
		 * "Guest reach count did not matched with Guests Targeted");
		 * TestListeners.extentTest.get().
		 * pass("Guest reach count matched with Guests Targeted count");
		 */

		// search campaign and get the id
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignId = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		// check if the campaign present in finished campaign
		boolean val = pageObj.campaignspage().searchCampaignInFinishedTab(campaignName, campaignId);
		Assert.assertTrue(val, "Processed mass campaign is not present in the finished tab");
		pageObj.utils().logPass("Verified processed mass campaign is present in the finished tab");

	}

	@Test(description = "SQ-T2513 Verify mass notification campaign With location triggered to all guest fulfilling particular criteria", groups = {
			"Regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2513_verifyMassNotificationCampaignWithLocationTriggeredToAllGuestFulfillingParticularCriteria()
			throws InterruptedException {
		// Precondition: location and user is present

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// User signup at location
		userEmail = dataSet.get("email");

		// Pos api checkin at location
		/*
		 * String key = CreateDateTime.getTimeDateString(); String txn = "123456" +
		 * CreateDateTime.getTimeDateString(); String date =
		 * CreateDateTime.getCurrentDate() + "T10:50:00+05:30"; Response respo =
		 * pageObj.endpoints().posCheckin(date, userEmail, key, txn,
		 * dataSet.get("locationKey")); Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for post chekin api");
		 */

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setLocationGroup(dataSet.get("location"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIVeryLongPolling(
				massCampaignName, dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		TestListeners.extentTest.get().pass(
				"Mass Notification campaign detail: push notification, campaign name, validated successfully on timeline");

	}

	@Test(description = "SQ-T2514 Verify mass notification campaign With favourite location triggered to all guest fulfilling particular criteria", groups = {
			"Regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2514_verifyMassNotificationCampaignWithFavouriteLocationTriggeredToAllGuestFulfillingParticularCriteria()
			throws InterruptedException {
		// Precondition: location and user is present

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// user signup with fav location
		userEmail = dataSet.get("email");

		// Pos api checkin
		/*
		 * String key = CreateDateTime.getTimeDateString(); String txn = "123456" +
		 * CreateDateTime.getTimeDateString(); String date =
		 * CreateDateTime.getCurrentDate() + "T10:50:00+05:30"; Response respo =
		 * pageObj.endpoints().posCheckin(date, userEmail, key, txn,
		 * dataSet.get("locationKey")); Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for post chekin api");
		 */

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setLocationGroup(dataSet.get("location"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 */

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		TestListeners.extentTest.get().pass(
				"Mass Notification campaign detail: push notification, campaign name, validated successfully on timeline");

	}

	@Test(description = "SQ-T3432 Mass Notification Campaign Trigger For User When Coupon is attached with the campaign", groups = {
			"Regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T3432_verifyMassNotificationCampaignTriggerForUserWhenCouponIsAttacheWithCampaign()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setCouponCampaign(dataSet.get("couponCampaign"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(massCampaignName);
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();

		String pn = pageObj.guestTimelinePage().getPushNotificationText();
		String[] vals = pn.split(" ");
		String couponCode = vals[4];

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertNotNull(couponCode, "couponCode has null value");
		TestListeners.extentTest.get().pass("Mass Notification campaign detail, push notification: " + pn
				+ ", campaign name, coupon code  validated successfully on timeline :" + couponCode);

	}

	// Rakhi
	@Test(description = "SQ-T5250 Add Send Time Optimization Feature to classic campaign creation", groups = {
			"regression", "unstable", "dailyrun" }, priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5250_addSendTimeOptimizationFeature() throws InterruptedException {

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		userEmail = dataSet.get("email");
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify time optimization when STO is disabled from cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().offEnableSTOCheckbox();

		// update membership
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Fair Scheduling?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequency("Once");
		Boolean status1 = pageObj.campaignsbetaPage().checkSTOfeaturePresence();
		Assert.assertFalse(status1, "Send time optimization feature is added for classic  mass notification campaign");
		boolean isTooltipPresent1 = pageObj.campaignsbetaPage().verifyStoTooltipPresence(dataSet.get("stoTooltipText"));
		Assert.assertFalse(isTooltipPresent1,
				"Tooltip for Send time optimization feature is present for classic mass notification campaign");
		pageObj.utils().logPass(
				"Send time optimization feature is not added for classic  mass notification campaign when disable STO");

		// verify time optimization when STO is enabled from cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		Boolean status = pageObj.campaignsbetaPage().checkSTOfeaturePresence();
		Assert.assertTrue(status, "Send time optimization feature not  added for classic  mass notification campaign");
		boolean isTooltipPresent2 = pageObj.campaignsbetaPage().verifyStoTooltipPresence(dataSet.get("stoTooltipText"));
		Assert.assertTrue(isTooltipPresent2,
				"Tooltip for Send time optimization feature is not present for classic mass notification campaign");
		pageObj.utils().logPass("Send time optimization feature is added for classic  mass notification campaign");

		pageObj.campaignsbetaPage().selectSTOcheckbox();
		pageObj.signupcampaignpage().clickScheduleBtn();

		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Filter Sidekiq jobs by user ID of default user (uses campaign default time)
		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		String expectedHour = "11"; // This time (11 PM of Asia/Kolkata) has been provided during campaign creation
		String jobName = "BulkMassGiftingNotifierWorker";
		String nonStoUserId = "438738229";
		String actualDefaultHour = pageObj.sidekiqPage().getConvertedTimeHour(nonStoUserId, jobName, "Asia/Kolkata",
				createDateTime, baseUrl);
		Assert.assertTrue(actualDefaultHour.contains(expectedHour));
		pageObj.utils().logPass("Campaign Scheduled time hour matched for default user.");

		// Filter Sidekiq jobs by user ID of STO user (uses STO time from DocDB)
		String stoUserId = "438738228";
		String expectedStoHour = "6"; // This time (6 PM of America/New_York) has been added for the user in DocDB
		String actualStoHour = pageObj.sidekiqPage().getConvertedTimeHour(stoUserId, jobName, "America/New_York",
				createDateTime, baseUrl);
		Assert.assertTrue(actualStoHour.contains(expectedStoHour));
		pageObj.utils().logPass("Campaign Scheduled time hour matched for STO user.");

		// Enqueue job in sidekiq
		pageObj.sidekiqPage().filterByJob("bulk_mass_notifications_fair_scheduling");
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();

		// Navigate to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName),
				massCampaignName + " Campaign name did not matched");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(massCampaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		pageObj.utils().logit("Guest received PN for campaign :" + massCampaignName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
