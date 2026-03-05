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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CampaignSetTest {

	private static Logger logger = LogManager.getLogger(CampaignSetTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
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

	@Test(description = "SQ-T2522 Validate Campaign Set For Mass Offer", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2522_verifyCampaignSetForMassOffer() throws InterruptedException {
		// Workflow flag in cockpit should be off in business
		String massOfferCampaign = "Automation Reward Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(massOfferCampaign, dataSet.get("giftType"),
				dataSet.get("redeemable"));

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentTypeOne"));
		pageObj.signupcampaignpage().setPushNotification(massOfferCampaign);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		logger.info("Campaign 1 created");

		// Create Campaign 2
		String massOfferpointCampaign = "Automation Points Campaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(massOfferpointCampaign);
		pageObj.signupcampaignpage().setGiftType("Gift Fixed Points");
		pageObj.signupcampaignpage().setFixedPoints("10");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentTypeTwo"));
		pageObj.signupcampaignpage().setPushNotification(massOfferpointCampaign);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		logger.info("Campaign 2 created");

		// Create campaign set
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaign Sets");
		pageObj.campaignsetPage().clickNewCampaignSetBtn();
		pageObj.campaignsetPage().setCampaignName("Campaign Set" + CreateDateTime.getTimeDateString());
		pageObj.campaignsetPage().selectCampaignOne(massOfferCampaign);
		pageObj.campaignsetPage().selectCampaignTwo(massOfferpointCampaign);
		pageObj.campaignsetPage().clickSaveandPreviewBtn();
		// Set start time and time zone
		pageObj.signupcampaignpage().setStartTime();
		pageObj.signupcampaignpage().setTimeZone("Delhi");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massOfferCampaign);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaign Sets");

		// Timeline validation
		String userEmail1 = "amit.kumar+1@punchh.com";
		String userEmail2 = "amit.kumar+4@punchh.com";

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massOfferCampaign,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massOfferCampaign,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massOfferCampaign), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redeemable")),
				"reward gifted in account history did not matched");
		TestListeners.extentTest.get().pass(
				"Campaign Set campaign one: push notification, campaign name, validated successfully on timeline");
	}

	@Test(description = "SQ-T3456 - Validate Campaign Set for mass notification ", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3456_verifyCampaignSetForMassNotification() throws InterruptedException {

		// Create Campaign 1
		String notificationCampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);

		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(notificationCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));
		pageObj.signupcampaignpage().setPushNotification(notificationCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		logger.info("First Notification campaign is created");

		// Create Campaign 2
		String massNotificationCampaign = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massNotificationCampaign);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("two user");
		pageObj.signupcampaignpage().setPushNotification(massNotificationCampaign);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		logger.info("Seconed Notification campaign is created");

		// Create campaign set
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaign Sets");
		pageObj.campaignsetPage().clickNewCampaignSetBtn();
		pageObj.campaignsetPage().setCampaignName("Campaign Set" + CreateDateTime.getTimeDateString());
		pageObj.campaignsetPage().selectCampaignOne(notificationCampaignName);
		pageObj.campaignsetPage().selectCampaignTwo(massNotificationCampaign);
		pageObj.campaignsetPage().clickSaveandPreviewBtn();
		// Set start time and time zone
		pageObj.signupcampaignpage().setStartTime();
		pageObj.signupcampaignpage().setTimeZone("Delhi");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(notificationCampaignName);
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignSets();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaign Sets");

		// Timeline validation
		String userEmail1 = "amit.kumar+1@punchh.com";
		String userEmail2 = "amit.kumar+4@punchh.com";

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(notificationCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(notificationCampaignName), "Campaign name did not matched");
		TestListeners.extentTest.get().pass(
				"Campaign Set campaign one: push notification, campaign name, validated successfully on timeline");

	}

	@Test(description = "MPC-T286 Verify favourite location change should recorded in the audit log of the user", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T286_verifyFavouriteLocationChangeShouldRecordedInTheAuditLogOfTheUser() throws InterruptedException {

		// user creation using mobile api2 signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().setFavLocationTimeline(dataSet.get("locationName"));
		String locationId = pageObj.guestTimelinePage().checkLocationInAuditLogs();
		Assert.assertEquals(locationId, dataSet.get("locationId"),
				"updated favlocation id did not matched in time line audit logs ");
		TestListeners.extentTest.get().pass("Updated user favlocation appeared in timline audit logs");

		// Iframelogin
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail, prop.getProperty("iFramePassword"));
		pageObj.iframeSingUpPage().updateGuestFavLocationinIframe(dataSet.get("locationNameIframe"));

		// verify iframe updated fav location on guest timeline audit logs
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String iFramelocationId = pageObj.guestTimelinePage().checkLocationInAuditLogs();
		Assert.assertEquals(iFramelocationId, dataSet.get("locationIdIframe"),
				"updated favlocation id did not matched in time line audit logs ");
		TestListeners.extentTest.get().pass("Iframe Updated user favlocation appeared in timline audit logs");

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