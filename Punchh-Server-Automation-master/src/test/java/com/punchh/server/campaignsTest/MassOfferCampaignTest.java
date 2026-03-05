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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MassOfferCampaignTest {

	private static Logger logger = LogManager.getLogger(MassOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		utils = new Utilities(driver);
	}
	// Amit

	@Test(description = "SQ-T3429 Mass offer campaign gifting points to segment users || "
			+ "SQ-T6131 Verify Custom segment use with mass campaign and user get rewards properly or not", groups = {
					"regression", "dailyrun", "nonNightly" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T3429_verifyMassOfferCampaignGiftingPointsToSegmentUsers() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
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
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); //boolean
		 * campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignSystemNotificationIsVisible(
		 * massCampaignName); boolean pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * pageObj.guestTimelinePage().clickAccountHistory(); String pageData
		 * =pageObj.accounthistoryPage().getPageData(); //List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 */
		// System.out.println(Itemdata);

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("100 Bonus points earned-" + massCampaignName),
				"Gifted points did not appeared in account history");
		// Assert.assertTrue(pageData.contains("100 Bonus points earned-" +
		// massCampaignName),"Gifted points did not appeared in account history");
		utils.logPass(
				"Mass offer points campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");
	}

// case moved SQ-T3431 Mass Campaign Trigger For User When Gift type is Currency  - covering this under SQ-T6397
// case moved - SQ-T3430 Mass Campaign Trigger For User When Gift type is Derived Reward covering this under SQ-T6310

	@Test(description = "SQ-T3583 Verify the functionality of advertising campaign when advertising campaign is post checkin offer", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void massNotificationCampaignForAdvertisingCampaign() throws InterruptedException {
		// Precondition: segement and user is present Segement:advertising automation
		String massCampaignName = "Automation MassNotification Advertising Campaign"
				+ CreateDateTime.getTimeDateString();
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
//		pageObj.signupcampaignpage().setAdvertisingCampaign(dataSet.get("advertisingCampaign"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
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
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignSystemNotificationIsVisible(
		 * massCampaignName);
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Mass Notification campaign detail, campaign name, validated successfully on timeline");
	}

	@Test(description = "SQ-T3429 Mass offer with complex segment beta", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T3429_verifyVerifyMassOfferWithComplexSegmentBeta() throws InterruptedException {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		userEmail = "autoframe001+1@punchh.com";
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

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */
		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		/*
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus = pageObj.guestTimelinePage()
		 * .verifyCampaignSystemNotificationIsVisible(massCampaignName);
		 * 
		 * 
		 * pageObj.guestTimelinePage().clickAccountHistory(); String pageData
		 * =pageObj.accounthistoryPage().getPageData();
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("10 Bonus points earned-" + massCampaignName),
				"Gifted points did not appeared in account history");
		utils.logPass(
				"Mass offer wit complex segment detail: push notification, campaign name, pointsnotification validated successfully on timeline");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		 * pageObj.segmentsBetaPage().findAndSelectSegment(segmentName);
		 */

	}

	@Test(description = "SQ-T5246 Verify mass offer with variable points", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T5246_verifyMassOfferWithVariablePoints() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGuestEmailwithVariablepoints(userEmail, dataSet.get("variablePoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		// pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"),
		// dataSet.get("segment"),
		// dataSet.get("pushNotification"), dataSet.get("emailSubject"),
		// dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * pageObj.guestTimelinePage().clickAccountHistory(); String pageData
		 * =pageObj.accounthistoryPage().getPageData();
		 */
		// List<String> Itemdata =
		// pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		// System.out.println(Itemdata);
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("10 Bonus points earned-" + massCampaignName),
				"Gifted points did not appeared in account history");
		utils.logPass(
				"Mass offer points campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");
	}

	@Test(description = "SQ-T5380 Create duplicate classic Mass Offer Campaign"
			+ "SQ-T4922 Verify the success toast message for exporting reports for one-time Mass campaigns with Processed and Stopped status", groups = {
					"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5380_duplicateMassOfferCampaign() throws InterruptedException {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setRichMessage(dataSet.get("richMsg"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		 * boolean campaignNotificationStatus = pageObj.guestTimelinePage()
		 * .verifyCampaignSystemNotificationIsVisible(massCampaignName); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		 */
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		// System.out.println(Itemdata.get(0));

		utils.logPass(
				"Mass offer currency campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// search and duplicate campaign
		pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);
		pageObj.campaignspage().selectCPPOptions("Duplicate");
		String name = pageObj.campaignspage().captureCampaignName();
		Assert.assertEquals(name, massCampaignName + " - copy");
		utils.logPass("Campaign name is prefilled as : " + massCampaignName + " - copy");
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// Delete created campaign
		pageObj.campaignspage().removeSearchedCampaign(massCampaignName + " - copy");

		// Click Campaigns Link
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		// processed mass recurring campaign
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Processed", "Status");
		// utils.getLocator("newCamHomePage.moreFilterButton").click();

		// pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Mass (One-time)", "Type");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		// export report
		pageObj.newCamHomePage().exportReport();
		String text = pageObj.newCamHomePage().getexportReportMsg();
		Assert.assertEquals(text, "Your campaign report is on its way");
		logger.info("Toast message for processed mass recurring campaign verified as : " + text);
		utils.logPass("Toast message for processed mass recurring campaign verified as : " + text);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// stopped mass recurring campaign
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Stopped", "Status");
		// pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Mass (One-time)", "Type");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		// export report
		pageObj.newCamHomePage().exportReport();
		String text1 = pageObj.newCamHomePage().getexportReportMsg();
		Assert.assertEquals(text1, "Your campaign report is on its way");
		utils.logPass("Toast message for stopped mass recurring campaign verified as : " + text1);

	}

	@Test(description = "SQ-T6143 Verify dynamic tags in email content with Send Test Functionality", groups = "Regression", priority = 5)
	@Owner(name = "Amit Kumar")
	public void T6143_SendTestWithEmailTemplateUsingDynamicTags() throws InterruptedException {
		String emailTemplateName = "DynamicTagsEmailTemplate";
		emailTemplateName = emailTemplateName + "_" + CreateDateTime.getTimeDateString();

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("email");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Email Templates
		// pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);

		// Select Email Template
		pageObj.emailTemplatePage().searchAndSelectEmailTemplate(dataSet.get("emailsubject"));

		// Step 4: Send a test notification with gift to verify dynamic tags
		Thread.sleep(5000);
		pageObj.campaignspage().sendTestNotificationWithGift(dataSet.get("email"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().removeSearchedCampaign(massCampaignName);

		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// fetch campaign name and reward gifted in account history
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");
		utils.logPass("Mass offer with dynamic tags using email template validated successfully on timeline");

	}

	// Rakhi
	@Test(description = "SQ-T6117 Verify error in schedule page when campaign deleted from mass giftings table", priority = 6)
	@Owner(name = "Rakhi Rawat")
	public void T6117_verifyErrorInSchedulePage() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setRichMessage(dataSet.get("richMsg"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		int preCount = pageObj.schedulespage().errorInSchedule();
		logger.info("Error count in schedule page before deleting campaign: " + preCount);
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// delete campaign from mass_giftings table
		String query = "delete from mass_giftings where name = '" + massCampaignName + "'and business_id = '"
				+ dataSet.get("business_id") + "';";
		DBUtils.executeQuery(env, query);

		// check error in schedule page
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		boolean flag = pageObj.schedulespage().isCampaignExistsInSchedule(massCampaignName);
		Assert.assertFalse(flag, massCampaignName + " exists in schedule");
		utils.logPass("Verified " + massCampaignName + " does not exist in schedule");

		int postCount = pageObj.schedulespage().errorInSchedule();
		logger.info("Error count in schedule page after deleting campaign: " + postCount);
		Assert.assertTrue(postCount > preCount,
				"Error count did not increased after deleting campaign from mass_giftings table");
		logger.info("Verified error in schedule page when campaign deleted from mass giftings table");
		utils.logPass("Verified error in schedule page when campaign deleted from mass giftings table");

	}

	// Piyush
	@Test(description = "SQ-T6700 Verify gifting from derived reward mass campaign with bulking enabled", priority = 7)
	@Owner(name = "Piyush Kumar")
	public void T6700_verifyGiftingFromDerivedRewardMassCampaignWithBulkingEnable() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_bulk_derived_reward_flow", "check", true);
		// pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_bulk_insert_derived_reward_flow",

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("derivedReward"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().clickNextButton();
		String camId = pageObj.signupcampaignpage().getCampaignid();

		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));

		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchCampaign(massCampaignName);
		String camStatus = pageObj.campaignspage().getCampaignStatus();
		int count = 0;
		while (!"Processed".equals(camStatus) && count < 20) {
			utils.longWaitInSeconds(3);
			pageObj.campaignspage().searchCampaign(massCampaignName);
			camStatus = pageObj.campaignspage().getCampaignStatus();
			count++;
		}
		if (count == 20 && !"Processed".equals(camStatus)) {
			Assert.fail(massCampaignName + " status is not Processed even after 10 retries");
		}

		String userCampaigns = "select count(*) from user_campaigns where business_id=" + dataSet.get("business_id")
				+ " and campaign_id=" + camId;
		int userCampaignCount = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, userCampaigns, "count(*)"));
		Assert.assertTrue(userCampaignCount == 4, "User_campaigns count is not equal to 4");

		String rewards = "select count(*) from rewards where business_id=" + dataSet.get("business_id")
				+ " and gifted_for_id=" + camId;
		int rewardsCount = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, rewards, "count(*)"));
		if (rewardsCount != 0) {
			Assert.assertTrue(rewardsCount > 0, "Rewards count is not greater than 0");
		}
		utils.longWaitInSeconds(8);
		String checkins = "select count(*) from checkins where business_id=" + dataSet.get("business_id")
				+ " and  gifted_for_id=" + camId;
		int checkinsCount = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, checkins, "count(*)"));
		if (checkinsCount != 0) {
			Assert.assertTrue(checkinsCount > 0, "Checkins count is not greater than 0");
		}
		if (rewardsCount == 0 && checkinsCount == 0) {
			Assert.fail("Mass campaign didn't run");
		}
		utils.logPass("Verified gifting from derived reward mass campaign with bulking enable");

		// delete campaign from mass_giftings table
		String query = "delete from mass_giftings where name = '" + massCampaignName + "'and business_id = '"
				+ dataSet.get("business_id") + "';";
		DBUtils.executeQuery(env, query);
	}

	// piyush
	@Test(description = "SQ-T6476 Verify duplicate campaign from mass offer campaign having recurring redeemable doesnt have redeemable prefilled", groups = "Regression", priority = 1)
	@Owner(name = "Piyush Kumar")
	public void T6476_VerifyDuplicateCampaignRedeemableNotPrefilled() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
				"Enable to show the list of all redeemables including recurrence", "check");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(massCampaignName);
		pageObj.signupcampaignpage().setRichMessage(massCampaignName);
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Duplicate");
		String redeemableValue = pageObj.signupcampaignpage().getRedeemableFieldValue();
		Assert.assertEquals(redeemableValue, "Select",
				"Redeemable field is prefilled or selected after duplicating the campaign");
		utils.logPass("Verified: Recurring redeemable is not prefilled after duplicating the campaign");

	}

//	@Test(description = "SQ-T7395 Verify recurring redeemable in single recurrence mass campaign"
//			+ "SQ-T7396 Verify recurring redeemable in daily/weekly/monthly recurrence mass campaign"
//			+ "SQ-T7398 Verify gifting from mass campaign when recurring redeemable is attached to mass campaign", groups = "Regression", priority = 1)
//	@Owner(name = "Shubham Gupta")
//	public void T7395_VerifyBehaviourOfRecurringRedeemableWithMassCampaign() throws Exception {
//
//		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
//		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
//
//		userEmail = dataSet.get("email");
//		// Login to instance
//		/*
//		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		 * pageObj.instanceDashboardPage().loginToInstance();
//		 */
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//		pageObj.menupage().dashboardPageMiscellaneousConfig();
//		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
//				"Enable to show the list of all redeemables including recurrence", "uncheck");
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//		pageObj.newCamHomePage().clickSwitchToClassicBtn();
//		// Select offer dropdown value
//		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
//		pageObj.campaignspage().clickNewCampaignBtn();
//
//		// set campaign name and gift type
//		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
//		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
//		pageObj.signupcampaignpage().sendRedeemableNameInTextBox(dataSet.get("recurringRedeemableName"));
//
//		boolean isRecurringRedeemablePresentInDropdown = pageObj.signupcampaignpage()
//				.isRedeemablePresentInDropdown(dataSet.get("recurringRedeemableName"));
//		Assert.assertFalse(isRecurringRedeemablePresentInDropdown,
//				"Recurring Redeemable is present in Redeemable dropdown when the flag is disabled");
//
//		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
//		pageObj.signupcampaignpage().clickNextBtn();
//
//		// select segment
//		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
//		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
//
//		// Click on save button
//		pageObj.signupcampaignpage().clickNextButton();
//		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
//		pageObj.signupcampaignpage().scheduleCampaign();
//
//		String query = "update redeemables set occurances = 8, days_distance = 10 where name = '"
//				+ dataSet.get("redeemableName") + "' and business_id = '" + dataSet.get("business_ID") + "';";
//		int updatedRows = DBUtils.executeUpdateQuery(env, query);
//		System.out.println("Rows updated: " + updatedRows);
//
//		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
//		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
//		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
//
//		System.out.println("Rows updated: " + updatedRows);
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//		pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);
//		boolean isLogMsgPresent = pageObj.campaignspage().isCamLogContainsMsg(dataSet.get("logMsg"));
//		Assert.assertTrue(isLogMsgPresent, "Log message is not present in campaign logs");
//		query = "update redeemables set occurances = 0, days_distance = 0 where name = '"
//				+ dataSet.get("redeemableName") + "' and business_id = '" + dataSet.get("business_ID") + "';";
//		updatedRows = DBUtils.executeUpdateQuery(env, query);
//		pageObj.utils().deleteCampaignFromDb(massCampaignName, env);
//
//		massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
//		dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
//		pageObj.menupage().dashboardPageMiscellaneousConfig();
//		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
//				"Enable to show the list of all redeemables including recurrence", "check");
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//
//		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
//		pageObj.campaignspage().clickNewCampaignBtn();
//
//		// set campaign name and gift type
//		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
//		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
//		pageObj.signupcampaignpage().sendRedeemableNameInTextBox(dataSet.get("recurringRedeemableName"));
//		isRecurringRedeemablePresentInDropdown = pageObj.signupcampaignpage()
//				.isRedeemablePresentInDropdown(dataSet.get("recurringRedeemableName"));
//		Assert.assertFalse(isRecurringRedeemablePresentInDropdown,
//				"Recurring Redeemable is present in Redeemable dropdown when the flag is enabled");
//
//		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
//		pageObj.signupcampaignpage().clickNextBtn();
//
//		// select segment
//		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
//		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
//
//		// Click on save button
//		pageObj.signupcampaignpage().clickNextBtn();
//		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
//		pageObj.signupcampaignpage().scheduleCampaign();
//
//		query = "update redeemables set occurances = 8, days_distance = 10 where name = '"
//				+ dataSet.get("redeemableName") + "' and business_id = '" + dataSet.get("business_ID") + "';";
//		updatedRows = DBUtils.executeUpdateQuery(env, query);
//		System.out.println("Rows updated: " + updatedRows);
//
//		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
//		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
//		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
//
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//		pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);
//		isLogMsgPresent = pageObj.campaignspage().isCamLogContainsMsg(dataSet.get("logMsg"));
//		Assert.assertTrue(isLogMsgPresent, "Log message is not present in campaign logs");
//		query = "update redeemables set occurances = 0, days_distance = 0 where name = '"
//				+ dataSet.get("redeemableName") + "' and business_id = '" + dataSet.get("business_ID") + "';";
//		updatedRows = DBUtils.executeUpdateQuery(env, query);
//		System.out.println("Rows updated: " + updatedRows);
//
//		pageObj.utils().deleteCampaignFromDb(massCampaignName, env);
//		logger.info("Verified behaviour of recurring redeemable with mass campaign");
//		TestListeners.extentTest.get().pass("Verified behaviour of recurring redeemable with mass campaign");
//
//	}

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
