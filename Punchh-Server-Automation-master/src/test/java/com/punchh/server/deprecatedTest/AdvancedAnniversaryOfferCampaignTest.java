package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

// advance anniversary campaigns depricted 
public class AdvancedAnniversaryOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(AdvancedAnniversaryOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String campaignId;
	private String campaignName;

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
		logger.info(sTCName + " ==>" + dataSet);
		logger.info("Test print statement");
	}

	@Test(description = "SQ-T4924 Verify Advance Anniversary Campaign_Lifespan Duration With Reference Date Only", groups = {
			"regression", "dailyrun" }, priority = 0)
	public void T4924_verifyAdvanceAnniversaryCampaignLifespanDurationReferenceDateOnly() throws Exception {

		// Business must be live, Enable skipping anniversary campaign schedule
		// lock,Enable advance anniversary campaign scheduling must be on in dashboard
		// >> misc.
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be current date's one day advanced
		String annivaersaryDate = CreateDateTime.getPastYearsWithFutureDate(20, 1);
		Response respo = pageObj.endpoints().posSignUpwithAdvanceAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		campaignName = "Automation AdvanceAnniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);

	}

	@Test(description = "SQ-T4925 Verify Advance Anniversary Campaign_Lifespan Duration With N Days from rewarding", groups = {
			"Regression", "unstable", "dailyrun" }, priority = 1)
	public void T4925_verifyAdvancedAnniversaryCampaignLifespanDurationNDaysFromRewarding() throws Exception {

		// Business must be live, Enable skipping anniversary campaign schedule
		// lock,Enable advance anniversary campaign scheduling must be on in dashboard
		// >> misc.
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be N date's one day advanced
		String annivaersaryDate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		Response respo = pageObj.endpoints().posSignUpwithAdvanceAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		campaignName = "Automation AdvanceAnniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("1");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Jobcampaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4698 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With Reference Date Only", groups = {
			"Regression", "unstable", "dailyrun" }, priority = 2)
	public void T4698_verifyAnniversaryHalfBirthdayCampaignLifespanDurationReferenceDateOnly() throws Exception {

		// Business must be live, Enable skipping anniversary campaign schedule
		// lock,Enable advance anniversary campaign scheduling must be on in dashboard
		// >> misc.
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be current date's one day advanced
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 1);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4698 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With N Days from rewarding", groups = {
			"Regression", "unstable", "dailyrun" }, priority = 3)
	public void T4698_verifyAnniversaryHalfBirthdayCampaignLifespanDurationNDaysFromRewarding() throws Exception {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be N date's one day advanced
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 2);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("1");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Jobcampaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4670 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With Reference Date Only", groups = {
			"Regression", "dailyrun" }, priority = 4)
	public void T4670_verifyAnniversaryKidsBirthdayCampaignLifespanDurationReferenceDateOnly() throws Exception {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be current date's one day advanced Kid One
		String kids_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 1);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kids_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Date must be current date's two day advanced Kid Two
		String kids_birthdate2 = CreateDateTime.getPastYearsWithFutureDate(19, 2);
		// Create user relation
		Response createUserRelationResponse2 = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName2"), kids_birthdate2);
		Assert.assertEquals(createUserRelationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4670 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With N Days from rewarding", groups = {
			"Regression", "dailyrun" }, priority = 5)
	public void T4670_verifyAnniversaryKidsBirthdayCampaignLifespanDurationNDaysFromRewarding() throws Exception {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be N date's one day advanced Kid One
		String kid_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kid_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Date must be N date's two day advanced Kid Two
		String kids_birthdate2 = CreateDateTime.getPastYearsWithFutureDate(19, 3);
		// Create user relation
		Response createUserRelationResponse2 = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName2"), kids_birthdate2);
		Assert.assertEquals(createUserRelationResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("1");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		String campaignId = pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Jobcampaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4673 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With Reference Date Only", groups = {
			"Regression", "dailyrun" }, priority = 6)
	public void T4673_verifyAnniversarySpouseBirthdayCampaignLifespanDurationReferenceDateOnly() throws Exception {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be current date's one day advanced
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 1);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4673 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With N Days from rewarding", groups = {
			"Regression", "dailyrun" }, priority = 7)
	public void T4673_verifyAnniversarySpouseBirthdayCampaignLifespanDurationNDaysFromRewarding() throws Exception {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be N date's one day advanced
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("1");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Jobcampaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4672 Verify Anniversary_BirthdayCampaign_Lifespan Duration With Reference Date Only", groups = {
			"Regression", "dailyrun" }, priority = 8)
	public void T4672_verifyAnniversaryBirthdayCampaignLifespanDurationReferenceDateOnly() throws Exception {
		// Business must be live, Enable skipping anniversary campaign schedule
		// lock,Enable advance anniversary campaign scheduling must be on in dashboard
		// >> misc.
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be current date's one day advanced
		String birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 1);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();
		logger.info("user Id is : " + userID);
		pageObj.utils().logit("user Id is : " + userID);
		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T4672 Verify Anniversary_BirthdayCampaign_Lifespan Duration With N Days from rewarding", groups = {
			"Regression", "unstable", "dailyrun" }, priority = 9)
	public void T4672_verifyAnniversaryBirthdayCampaignLifespanDurationNDaysFromRewarding() throws Exception {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be N date's one day advanced
		String birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		campaignId = pageObj.signupcampaignpage().getCampaignid();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("1");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", campaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(campaignId),
				"Scheduled Jobcampaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@Test(description = "SQ-T5303 Verify Advance Program Anniversary Campaign_Lifespan Duration With Reference Date Only", groups = {
			"regression", "dailyrun" }, priority = 2)
	public void T5303_verifyProgramAnniversaryCampaignLifespanDurationReferenceDateOnly() throws Exception {

		// Business must be live, Enable skipping anniversary campaign schedule
		// lock,Enable advance anniversary campaign scheduling must be on in dashboard
		// >> misc.
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be current date's one day advanced
		String programJoinDate = CreateDateTime.getPastYearsWithFutureDate(20, 1) + " 06:40:10";
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		String userID = respo.jsonPath().get("id").toString();

		String query = "UPDATE accounts SET `created_at` = '" + programJoinDate + "' WHERE user_id = '" + userID + "';";

		int rs = DBUtils.executeUpdateQuery(env, query);

		pageObj.utils().logit("User search successful");

		campaignName = "Automation ProgramAnniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Program Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		String CampaignId = pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", CampaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(CampaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName != null && anniversaryCampaignName.equalsIgnoreCase(campaignName),
				"Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	@Test(description = "SQ-T5304 Verify Advance Program Anniversary Campaign_Lifespan Duration With N Days from rewarding", groups = {
			"regression", "dailyrun" }, priority = 3)
	public void T5304_verifyProgramAnniversaryLifespanDurationNDaysFromRewarding() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Date must be current date's 7 days advanced with past year
		String programJoinDate = CreateDateTime.getPastYearsWithFutureDate(2, 8) + " 06:40:10";
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		String query = "UPDATE accounts SET `created_at` = '" + programJoinDate + "' WHERE user_id = '" + userID + "';";
		int rs = DBUtils.executeUpdateQuery(env, query);

		campaignName = "Automation ProgramAnniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Program Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("7");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		String CampaignId = pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().movetoSidekiq(baseUrl);
		String sideKiqJobData = pageObj.schedulespage()
				.validateSidekiqJob("BulkPrescheduleBusinessUserAnniversaryWorker", CampaignId, userID);

		Assert.assertTrue(sideKiqJobData != null && sideKiqJobData.contains(CampaignId),
				"Scheduled Job campaign id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(userID), "Scheduled Job user id did not matched");
		Assert.assertTrue(sideKiqJobData.contains(dataSet.get("businessId")),
				"Scheduled Job business id did not matched");
		pageObj.utils().logPass(
				"Verified campaingId, userId, businessId in sidekiq BulkPrescheduleBusinessUserAnniversaryWorker job : "
						+ sideKiqJobData);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();

		Assert.assertTrue(anniversaryCampaignName != null && anniversaryCampaignName.equalsIgnoreCase(campaignName),
				"Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	@Test(description = "SQ-T5741 Verify gifting from anniversary campaign when user migrated from eClub to loyalty with advance scheduling flag is enable", groups = {
			"regression", "dailyrun" }, priority = 4)
	public void T5741_verifyAnniversaryCampaignWhenUserMigratedFromeClubToLoyaltyWithAdvanceSchedulingFlagenabled()
			throws Exception {

		// Business must be live,Enable advance anniversary campaign scheduling must be
		// on in dashboard >> misc.
		// user creation using pos signup api

		// eClubUser Signup
		pageObj.iframeSingUpPage().navigateToEcrm(baseUrl + dataSet.get("ecrmUrl"));
		userEmail = pageObj.iframeSingUpPage().ecrmSignUp(dataSet.get("location"));

		// update user joined at created at in db to past date
		String pastMonthDate = CreateDateTime.getPastMonthsDate(1) + " 00:00:00";
		String query = "UPDATE users SET created_at = '" + pastMonthDate + "' WHERE email = '" + userEmail + "';";
		DBUtils.executeUpdateQuery(env, query);

		// convert Eclub to Loyalty user by pos signup
		// Date must be current date's one day advanced
		String annivaersaryDate = CreateDateTime.getPastYearsWithFutureDate(20, 1);
		Response respo = pageObj.endpoints().posSignUpwithAdvanceAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		// create anniversary campaign
		campaignName = "Automation AdvanceAnniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason("Test");
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();

		// code for whom details
		pageObj.dashboardpage()
				.checkUncheckAnyFlagWitoutUpdate("Should the above reward be given in the month of signup?", "uncheck");
		pageObj.signupcampaignpage().setReferenceDateOnGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		String CampaignId = pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// run Schedule for anniversary camp
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);

		Assert.assertTrue(anniversaryCampaignName == null || anniversaryCampaignName.isEmpty(),
				"Campaign name found but it should not trigger today");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}
