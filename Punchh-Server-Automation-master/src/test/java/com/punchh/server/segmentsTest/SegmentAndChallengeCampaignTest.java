package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
public class SegmentAndChallengeCampaignTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String userEmail, segmentName;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T5979 User verifies the functionality of challenge campaign having gift type as points and Challenge point  = Every X points with Universal Auto Enrolment || "
			+ "SQ-T5985 Verify that when the user completes the challenge of any type, the Badge Icon is changed to “completed- icon” as set in the challenge campaign || "
			+ "SQ-T6046 Verify the challenge start icon is reflecting on user timeline || "
			+ "SQ-T6730 Verify that the Challenge campaign having challenge type as Every X points is reflected in the user’s dashboard >  challenge section if the user is targeted by the campaign after user cache enrolment", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5979_EveryXPointsWithUniversalAutoEnrolment() throws Exception {
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		String campaignName = "AutomationChallengeCampaignEveryXPoints" + CreateDateTime.getTimeDateString();
		String giftReason = "Automation " + campaignName;

		// create User
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");
		String token = signUp.jsonPath().get("access_token.token").toString();
		String userID = signUp.jsonPath().get("user.user_id").toString();

		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");

		pageObj.newCamHomePage().challengeWhatPageGift(campaignName, dataSet.get("giftType"), giftReason,
				dataSet.get("Gift Points"));
		pageObj.newCamHomePage().uploadAllImagesInChallengeCompletedCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "uncheck");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);

		// Navigate to Previous page
		pageObj.signupcampaignpage().clickPreviousButton();

		// get Icon Completed Image Src
		String startIconSrc = pageObj.newCamHomePage().getChallengeImageSrc("Icon");
		Assert.assertTrue(startIconSrc.contains("image/upload"), "Start Icon image src is empty");
		utils.logit("Start Icon image src is not empty");
		String iconSrc = pageObj.newCamHomePage().getChallengeImageSrc("Icon Completed");
		Assert.assertTrue(iconSrc.contains("image/upload"), "Icon Completed image src is empty");
		utils.logit("Icon Completed image src is not empty");
		pageObj.newCamHomePage().clickOnNextButton();
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(4);
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");

		// Verify Challenge campaign Icon
		String src = pageObj.guestTimelinePage().checkChallengeCampaignIcon(campaignName);
		Assert.assertEquals(src, startIconSrc,
				"Start icon src is not matching with the expected image src for Challenge campaign " + campaignName);

		utils.logPass(
				"Verified that the Challenge campaign having challenge type as Every X points is reflected in the user’s dashboard >  challenge section "
						+ campaignName);

		utils.logPass(
				"Verified that Challenge campaign start icon src is matching with the expected image src for Challenge campaign "
						+ campaignName);

		// Send challenge Progress using API
		Response sendRewardResponse9 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), campaignID, "39");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse9.getStatusCode(),
				"Status code 201 did not matched for api2 send challenge campaign to user");

		// Account history
		List<Object> obj = new ArrayList<Object>();
		String description;
		int j = 0;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "].description");
			if (description.contains(giftReason)) {
				i = j;
				break;
			}
		}

		String eventValue = accountHistoryResponse.jsonPath().get("[" + j + "].event_value").toString();
		Assert.assertEquals(eventValue, "+" + dataSet.get("Gift Points") + " points",
				"User " + userEmail + " does not get the points from challenge campaign " + campaignName);
		utils.logPass("Verified in the Account history that User " + userEmail
				+ " get the points from challenge campaign " + campaignName);

		String challengneCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(challengneCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		utils.logPass("Challenge campaign push notification and campaign name validated successfully on timeline");

		// Verify campaign status on guest timeline , challenge campaign
//		pageObj.guestTimelinePage().navigateToTabs("Challenges");
		utils.refreshPage();
		boolean flag = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "Active");
		Assert.assertTrue(flag, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// Verify Challenge campaign Icon
		String srcEndIcon = pageObj.guestTimelinePage().checkChallengeCampaignIcon(campaignName);
		Assert.assertEquals(srcEndIcon, iconSrc,
				"End Icon src is not matching with the expected image src for Challenge campaign " + campaignName);
		utils.logPass(
				"Verified that Challenge campaign End Icon src is matching with the expected image src for Challenge campaign "
						+ campaignName);

		// delete camapigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().deleteCampaign(campaignName);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6589 User verifies the functionality of challenge campaign having gift type as points and Challenge point  = Every X points with Segment Auto Enrolment", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T6589_EveryXPointsWithUniversalAutoEnrolment() throws Exception {
		String campaignName = "AutomationChallengeCampaignEveryXPoints" + CreateDateTime.getTimeDateString();
		String giftReason = "Automation " + campaignName;

		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// navigate to guest timeline that exists in segment attached to derived reward
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName"), dataSet.get("segmentId"));
		int guestCountInSegment = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCountInSegment > 0, "Guest count in segment is not greater than 0");
		String randomUserFromSegment = pageObj.segmentsBetaPage().getRandomGuestFromSegmentList(dataSet.get("apiKey"),
				dataSet.get("segmentId"), 10);

		String query5 = "Select `id` from `users` WHERE `email` = \"" + randomUserFromSegment + "\" ;";
		String userID = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "id", 5);
		utils.logit("userID column in users table is equal to " + userID);

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(randomUserFromSegment, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		Assert.assertNotNull(token, "Token is null");

		// create challenge campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");

		// --------------- Segment Auto-enrollment Type--------------
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCompletedCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.newCamHomePage().challengeReachDrpDown("Segment Auto Enrolment");
		pageObj.newCamHomePage().segmentDrpDown(dataSet.get("segmentName"));
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);

		// Navigate to Previous page
		pageObj.signupcampaignpage().clickPreviousButton();

		// get Icon Completed Image Src
		String startIconSrc = pageObj.newCamHomePage().getChallengeImageSrc("Icon");
		Assert.assertTrue(startIconSrc.contains("image/upload"), "Start Icon image src is empty");
		utils.logit("Start Icon image src is not empty");
		String iconSrc = pageObj.newCamHomePage().getChallengeImageSrc("Icon Completed");
		Assert.assertTrue(iconSrc.contains("image/upload"), "Icon Completed image src is empty");
		utils.logit("Icon Completed image src is not empty");
		pageObj.newCamHomePage().clickOnNextButton();
		pageObj.signupcampaignpage().setFrequency("Once");
		pageObj.signupcampaignpage().setStartDateAndTime();
		pageObj.signupcampaignpage().challengeAvailabilityScheduleStartTime("12:00 AM");
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and select campaign
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		// Run Segment User Cache Schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Segment User Cache Schedule");
		pageObj.schedulespage().runSchedule();

		// Run Challenge Availability Schedules
		pageObj.schedulespage().selectScheduleType(dataSet.get("schedule"));
		pageObj.schedulespage().runChallengeCampaignSchedule(campaignName);
		utils.longWaitInSeconds(60);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(randomUserFromSegment);
		pageObj.guestTimelinePage().navigateToTabs("Challenges");

		// Verify Challenge campaign Icon
		String src = pageObj.guestTimelinePage().checkChallengeCampaignIcon(campaignName);
		Assert.assertEquals(src, startIconSrc,
				"Start icon src is not matching with the expected image src for Challenge campaign " + campaignName);
		utils.logPass(
				"Verified that Challenge campaign start icon src is matching with the expected image src for Challenge campaign "
						+ campaignName);

		// Send challenge Progress using API
		Response sendRewardResponse9 = pageObj.endpoints().API2SendMessageToUserChallengeCampaign("challengeCampaign",
				userID, dataSet.get("apiKey"), campaignID, "39");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse9.getStatusCode(),
				"Status code 201 did not matched for api2 send challenge campaign to user");

		// Account history
		List<Object> obj = new ArrayList<Object>();
		String description;
		int j = 0;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "].description");
			if (description.contains(giftReason)) {
				i = j;
				break;
			}
		}

		String eventValue = accountHistoryResponse.jsonPath().get("[" + j + "].event_value").toString();
		Assert.assertEquals(eventValue, "+" + dataSet.get("giftPoints") + " points",
				"User " + randomUserFromSegment + " does not get the points from challenge campaign " + campaignName);
		utils.logPass("Verified in the Account history that User " + randomUserFromSegment
				+ " get the points from challenge campaign " + campaignName);

		String challengneCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(challengneCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		utils.logPass("Challenge campaign push notification and campaign name validated successfully on timeline");

		// Verify campaign status on guest timeline , challenge campaign
		utils.refreshPage();
		boolean flag = pageObj.guestTimelinePage().CheckChallengeCampaignStatus(campaignName, "Active");
		Assert.assertTrue(flag, "Campaign status did not matched");
		utils.logPass("Challenge Campaign status matched on guest timeline");

		// Verify Challenge campaign Icon
		String srcEndIcon = pageObj.guestTimelinePage().checkChallengeCampaignIcon(campaignName);
		Assert.assertEquals(srcEndIcon, iconSrc,
				"End Icon src is not matching with the expected image src for Challenge campaign " + campaignName);
		utils.logPass(
				"Verified that Challenge campaign End Icon src is matching with the expected image src for Challenge campaign "
						+ campaignName);

		// delete camapigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().deleteCampaign(campaignName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}
