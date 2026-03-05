package com.punchh.server.Test;

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

import com.punchh.server.annotations.Owner;
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
public class SegmentBetaBirthdayBeforeWithCampainsTest {
	static Logger logger = LogManager.getLogger(SegmentBetaBirthdayBeforeWithCampainsTest.class);
	public WebDriver driver;
	SeleniumUtilities selUtils;
	Properties prop;
	String userEmail;
	public static String guestCount;
	public static String segmentName = "", segmentGuest = "";
	int segmentCountBeta;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private PageObj pageObj;
	Utilities utils;
	ApiUtils apiUtils;
	String campaignName, t2SegmentName;
	public static boolean segmentIsCreatedFlag;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		selUtils = new SeleniumUtilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities();
	}

	public boolean crateSegmentsForTheTestCases() throws InterruptedException {

		segmentName = CreateDateTime.getUniqueString("Segment");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// Create new beta segment
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Onboarding Date");
		pageObj.segmentsBetaPage().setDuration("On a date");
		pageObj.segmentsBetaPage().setValuDrp("Yesterday");
		pageObj.segmentsBetaPage().setAttribute("Activation Status"); // active users only

		/*
		 * Optional logic pageObj.segmentsBetaPage().setAttribute("Birthday");
		 * pageObj.segmentsBetaPage().setDuration("Within");
		 * pageObj.segmentsBetaPage().setDateTime();
		 */

		segmentCountBeta = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();

		pageObj.segmentsBetaPage().saveSegment(segmentName);

		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();

		segmentGuest = pageObj.segmentsBetaPage().getSegmentGuest();

		Assert.assertEquals(segmentCountBeta, segmentDefCount,
				"Segment definition count did not match with segment count");
		Assert.assertEquals(segmentCountBeta, guestInSegmentCount,
				"Guest in segment count did not match with segment count");

		segmentIsCreatedFlag = true;
		return true;
	}

//Amit
	@SuppressWarnings("unused")
	@Test(description = "SQ-T3192 Verify profile details beta segmenet with birthday before datetime with post checkin campaing", groups = {
			"regression" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T3192_verifyProfileDetailsBetaSegmenetWithBirthdayBeforeDateTimeWithPostCheckinCampaing()
			throws InterruptedException {
		// pos signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		if ((segmentName.equalsIgnoreCase("")) && (segmentIsCreatedFlag == false)) {
			boolean result = crateSegmentsForTheTestCases();
			Assert.assertTrue(result, "Segment is not created successfully");
		}

		// PostCheckin campaign Creation
		campaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redemable"));

		// Set campaign segment and validate guest count
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		int guestReachCount = pageObj.signupcampaignpage().guestsReachCount();
		// not matched with segment count");

		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status,
				"Campaign created successfully msg did not displayed. Post Checkin Campaign is not created...");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(segmentGuest);

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, segmentGuest, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// timeline validation
		String postCheckinCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();

		Assert.assertTrue(postCheckinCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redemable")), "Gifted item name did not matched");
		utils.logPass(
				"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);
	}

//Amit
	@SuppressWarnings("unused")
	@Test(description = "SQ-T3106 Verify profile details beta segmenet with birthday before datetime with mass offer campaing", groups = "regression", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3106_verifyProfileDetailsBetaSegmenetWithBirthdayBeforeDateTimeWithMassOfferCampaing()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation
		// pos signup
		/*
		 * userEmail = pageObj.iframeSingUpPage().generateEmail(); Response response =
		 * pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		 * Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		 */
		campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		if ((segmentName.equalsIgnoreCase("")) && (segmentIsCreatedFlag == false)) {
			boolean result = crateSegmentsForTheTestCases();
			Assert.assertTrue(result, "Segment is not created successfully");
		}

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		int guestReachCount = pageObj.signupcampaignpage().guestsReachCount();

		// not matched with segment count");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");
		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		Assert.assertNotNull(segmentGuest);
		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(segmentGuest);
		String massCampaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		boolean rewardedRedeemableStatus = pageObj.guestTimelinePage().verifyrewardedRedeemableMassCampaign();
		Assert.assertTrue(massCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus, "Rewarded Redeemable notification did not displayed...");
		utils.logPass(
				"Mass offer campaign detail: push notification, campaign name, reward notification validated successfully on timeline");
	}

	// shashank
	@SuppressWarnings("unused")
	@Test(description = "SQ-T3649	Verify that a Post Checkin campaign working properly with Transactional and Non Transactional segments", groups = {
			"regression", "unstable", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3649_VerifyPosCheckinWithTransactionalSegmentsUsers() throws InterruptedException {
		campaignName = CreateDateTime.getUniqueString("AutomationPostCheckinTransaction_");
		String postCheckinPushNotification = dataSet.get("transactionalPostCheckinPushNotification");
		@SuppressWarnings("unused")
		String redeemableAmount = dataSet.get("redeemableAmount");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(
				dataSet.get("transactionalPostCheckinPushNotification"), dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"), dataSet.get("QCName"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user signup using api2
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, iFrameEmail, key, txn, dataSet.get("locationkey"),
				"12003");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		boolean campaignNameIsExist = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);
		Assert.assertTrue(campaignNameIsExist, campaignName + " Campagin not found ");
		logger.info("Verified that " + campaignName + " campaign is visible on user timeline");
		utils.logPass("Verified that " + campaignName + " campaign is visible on user timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(campaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign Or System notification did not displayed...");
		logger.info("Verified that Campaign Or System Notification label is visible on user time line page");
		utils.logPass("Verified that Campaign Or System Notification label is visible on user time line page");

		String actualPushNotificationText = pageObj.guestTimelinePage().getPushNotificationForCampaign(campaignName);
		Assert.assertTrue(actualPushNotificationText.equalsIgnoreCase(postCheckinPushNotification),
				"Push notification did not displayed...");
		logger.info("Verified that Push notification - " + postCheckinPushNotification
				+ "is visible on user timeline page");
		utils.logPass("Verified that Push notification - " + postCheckinPushNotification
				+ "is visible on user timeline page");

		logger.info("Verified that segment user is getting the post checkin offer after the checkin");
		utils.logPass("Verified that segment user is getting the post checkin offer after the checkin");

		// Delete created campaign
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().searchAndDeleteDraftCampaignClassic(postCheckinName);

	}

//shashank //moved to local
	@Test(description = "SQ-T3640	Verify that the Punchh segments browsed in Mass gifting campaign is functioning as expected and the campaign is targeting the users as per segment definition.", groups = "regression", priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T3640_MassOfferCampaignIsGettingToTargetSegmentUsers() throws InterruptedException {

		campaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String pushNotification = "Mass Offer Push Notification " + campaignName;
		String emailSubject = "Mass Offer Email Subject " + campaignName;
		String emailTemplate = "Mass Offer Email Template " + campaignName;

		userEmail = dataSet.get("email");
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				pushNotification, emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameIsExist = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);
		Assert.assertTrue(campaignNameIsExist, campaignName + "Campaign name did not matched");
		logger.info(campaignName + " campaign is visible on user timeline page");
		utils.logPass(campaignName + " campaign is visible on user timeline page");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(campaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		logger.info("Campaign notification is visible on user timeline page");
		utils.logPass("Campaign notification is visible on user timeline page");

		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationForCampaign(campaignName);
		Assert.assertTrue(pushNotificationStatus.contains(pushNotification), "Push notification did not displayed...");
		logger.info("Push notification is visible on user timeline page");
		utils.logPass("Push notification is visible on user timeline page");
	}

	// shashank sharma
	@Test(description = "SQ-T3627	Verify that a Post Redemption campaign working properly with Transactional and Non Transactional segments", groups = "Regression", priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T3627_verifyPostredemptionCampaignForSegmentUser() throws InterruptedException {
		String redeemaableID = dataSet.get("redeemaableID"); // $2.0 OFF is gifted

		String CampaignName = dataSet.get("campaignName");

		String pushNotification = dataSet.get("pushNotification");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(CampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				redeemaableID, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 200 did not matched for sendMessageToUser api");
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemaableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		Response respo = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean campaignNameIsExist = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(CampaignName);
		Assert.assertTrue(campaignNameIsExist, CampaignName + "Campaign name did not matched");
		logger.info(CampaignName + " campaign is visible on user timeline page");
		utils.logPass(CampaignName + " campaign is visible on user timeline page");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(CampaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		logger.info("Campaign notification is visible on user timeline page");
		utils.logPass("Campaign notification is visible on user timeline page");

		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationForCampaign(CampaignName);
		Assert.assertTrue(pushNotificationStatus.contains(pushNotification), "Push notification did not displayed...");
		logger.info("Push notification is visible on user timeline page");
		utils.logPass("Push notification is visible on user timeline page");

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(CampaignName);
		camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Active")) {
			String activeStatus = pageObj.newCamHomePage().deactivateCampaign();
			Assert.assertEquals(activeStatus, "Inactive", "Campaign inativated status did not matched");
		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}
