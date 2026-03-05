package com.punchh.server.campaignsTest;

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
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AnniversaryCampaignWithRedeemableTest {
	private static Logger logger = LogManager.getLogger(AnniversaryCampaignWithRedeemableTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private String campaignId;
	private String campaignName;
	private static Map<String, String> dataSet;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5756 Verify redeemable end date in the rewards tab and user's timeline in case of anniversary campaign with Reference date only and kid's birthday when redeemable has N days expiry", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T5756_verifyRedeemablEndDateInRewardsTabAndUsersTimelineInCaseOfAnniversaryCampaignWithReferenceDateOnlyAndKidsBirthdayWhenRedeemableHasNDaysExpiry()
			throws Exception {
		// advance anniversary flag must be off in misc config and bussines should be
		// live, Enable skipping anniversary campaign schedule lock on, rearch on
		// update redeemable end date using api
		String endTime = CreateDateTime.getFutureDate(5) + "T23:59:59";
		Response updateResponse = pageObj.endpoints().offerApi2UpdateRedeemableEndTime(dataSet.get("externalId"),
				endTime, dataSet.get("apiKey"));
		Assert.assertEquals(updateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 update redeemable");
		String updateStatus = updateResponse.jsonPath().get("results[0].success").toString();
		Assert.assertTrue(updateStatus.equalsIgnoreCase("true"), "update sucess reponse is not true");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Date must be current date
		String kids_birthdate = CreateDateTime.getPastYearsDate(10);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName"), kids_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 Create user relation is successful");

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
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Anniversary Kids Birthday with reference date campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		String expiryDate = pageObj.guestTimelinePage().getRewardExpiryDate(dataSet.get("redeemable"));
		String time = CreateDateTime.convertToISTTimeFromDateTime(expiryDate);
		boolean isCurrentDate = CreateDateTime.validatIsCurrentDate(expiryDate);
		String expirytime = CreateDateTime.convertISTtoOtherTimezone(time, "Australia/Darwin");
		Assert.assertTrue(isCurrentDate, "Rewarded redeemable expiry date is not current date");
		Assert.assertEquals(expirytime, "11:59 pm", "Rewarded redeemable expiry date is not current date");
		// expiry time as per redeemable timezone

		TestListeners.extentTest.get()
				.pass("Redeemable expiry date and time zone validated on users reward tab :" + expiryDate);

	}

	@Test(description = "SQ-T5764 Verify redeemable end date in the rewards tab and user's timeline in case of anniversary campaign with Fixed date and spouse's birthday when redeemable has explicit end date", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T5764_verifyRedeemableEndDateInTheRewardsTabAndUsersTimelineInCaseOfAnniversaryCampaignWithFixedDateAndSpousesBirthdayWhenRedeemableHasexplicitEndDate()
			throws Exception {

		// advance anniversary flag must be off in misc config and bussines should be
		// live, Enable skipping anniversary campaign schedule lock should be on
		// update redeemable end date using api
		String endTime = CreateDateTime.getFutureDate(10) + "T23:59:59";
		Response updateResponse = pageObj.endpoints().offerApi2UpdateRedeemableEndTime(dataSet.get("externalId"),
				endTime, dataSet.get("apiKey"));
		Assert.assertEquals(updateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 update redeemable");
		String updateStatus = updateResponse.jsonPath().get("results[0].success").toString();
		Assert.assertTrue(updateStatus.equalsIgnoreCase("true"), "update sucess reponse is not true");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Date must be current date in past year
		String spouse_birthdate = CreateDateTime.getPastYearsDate(30);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 Create user relation is successful");

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
		pageObj.signupcampaignpage().setLifespanDuration("Fixed");
		String fixedExpiryDate = CreateDateTime.getFutureDateinMonthDateYearFormat(5);
		pageObj.signupcampaignpage().setLifespanDurationFixedDate(fixedExpiryDate);
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
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded " + dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		String expiryDate = pageObj.guestTimelinePage().getRewardExpiryDate(dataSet.get("redeemable"));

		/*
		 * String date = "March 17, 2025"; String ab =
		 * CreateDateTime.getAnyTimzoneDateTimeFromAny(date + " 09:29 AM IST", "IST",
		 * "EST"); logger.info("ab : " + ab);
		 */
		// daylight saving time make difference of 1 hour in EST timezone
		// String finalDateTime =
		// CreateDateTime.getIstTimzoneDateTimeFromAny(fixedExpiryDate + " 10:59 PM
		// EST", "EST");
		String finalDateTime = CreateDateTime.getIstTimzoneDateTimeFromAny(fixedExpiryDate + " 11:59 PM EST", "EST"); // America/New_York
		Assert.assertTrue(expiryDate.equalsIgnoreCase(finalDateTime),
				"Rewarded redeemable expiry date did not matched with Expiry date in campaign.");
		TestListeners.extentTest.get()
				.pass("Redeemable expiry date and time zone validated on users reward tab :" + expiryDate);
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);

	}

	@Test(description = "SQ-T5755 Verify the redeemable end date in the rewards tab and user's timeline in case of anniversary campaign with N days from rewarding and anniversary when redeemable has no end date", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5755_verifyAnniversarySpouseBirthdayNDaysFromRewarding() throws Exception {
		// advance anniversary flag must be off in misc config
		// and bussines should be live
		// create redeemable - Automation - 12003

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Date must be N date
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(29, 7);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 Create user relation is successful");

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
		pageObj.signupcampaignpage().setDaysBefore("7");
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
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded " + dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 */
		// this is for deleting created campaign by campaign id
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignId, env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}