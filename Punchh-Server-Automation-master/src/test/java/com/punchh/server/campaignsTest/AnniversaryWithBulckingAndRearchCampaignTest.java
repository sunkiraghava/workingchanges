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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AnniversaryWithBulckingAndRearchCampaignTest {
	private static final Logger logger = LogManager.getLogger(AnniversaryWithBulckingAndRearchCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
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
	}

	// ----------------------------------------------------------------------------------------
	// bulking enable, rearch disaable, business live - enable
	// ------------------------------------------------------------------------------------------
	@Test(description = "SQ-T5242 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With Reference Date bulking enable rearch disabled gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T5242_verifyAnniversaryCampaignLifespanDurationReferenceDateOnlyBulkingEnableRearchDisableGiftingReward()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_live", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Bulk Insert for Anniversary Campaigns?",
				"check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable skipping anniversary campaign schedule lock",
				"check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable advance anniversary campaign scheduling",
				"uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable anniversary rearch", "uncheck");

		// user creation using pos signup api
		String annivaersaryDate = CreateDateTime.getPastYearsDate(2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

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
		pageObj.signupcampaignpage().setReferenceDateGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification") + " " + campaignName + " "
				+ "{{{reward_name}}}" + " and reward id as " + "{{{reward_id}}}");
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject") + " " + campaignName + " "
				+ "{{{business_name}}}" + " and reward name as " + "{{{reward_name}}}");
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate") + " " + campaignName + " "
				+ "{{{business_name}}}" + " and reward name as " + "{{{reward_name}}}");
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
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5242 Verify Anniversary_BirthdayCampaign_Lifespan Duration With Reference Date bulking enable rearch disabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T5242_verifyAnniversaryBirthdayCampaignLifespanDurationReferenceDateOnlyBulkingEnableRearchDisableGiftingPoints()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// user creation using pos signup api
		String birthdate = CreateDateTime.getPastYearsDate(20);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
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

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5242 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With Reference Date bulking enable rearch disabled gifting currency", groups = {
			"Regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T5242_verifyAnniversaryHalfBirthdayCampaignLifespanDurationReferenceDateBulkingEnableRearchDisabledGiftingCurrency()
			throws InterruptedException {
		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();
		// business must be live, Enable skipping anniversary campaign schedule lock on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);
		// user creation
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 0);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Currency");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("currency"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("$10.00 of banked rewards-" + campaignName + ""),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5242 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking enable rearch disabled gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T5242_verifyAnniversarySpouseBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingEnableRearchDisabledGiftingReward()
			throws InterruptedException {
		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today's it will trigger 2 days before
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

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
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
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

	@Test(description = "SQ-T5242 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking enable rearch disabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T5242_verifyAnniversaryKidsBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingEnableRearchDisabledGiftingPoint()
			throws InterruptedException {
		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today
		String kid_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kid_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}
	// ----------------------------------------------------------------------------------------
	// bulking disable, rearch enable
	// ----------------------------------------------------------------------------------------

	@Test(description = "SQ-T5243 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With Reference Date bulking disable rearch enable gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 5)
	@Owner(name = "Amit Kumar")
	public void T5243_verifyAnniversaryCampaignLifespanDurationReferenceDateOnlyBulkingDisableRearchEnableGiftingReward()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// user creation using pos signup api
		String annivaersaryDate = CreateDateTime.getPastYearsDate(2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

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

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5243 Verify Anniversary_BirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch enabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 6)
	@Owner(name = "Amit Kumar")
	public void T5243_verifyAnniversaryBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchEnableGiftingPoints()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// Date must be Nth date from today
		String birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5243 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With Reference Date bulking disable rearch enabled gifting currency", groups = {
			"Regression", "dailyrun" }, priority = 7)
	@Owner(name = "Amit Kumar")
	public void T5243_verifyAnniversaryHalfBirthdayCampaignLifespanDurationReferenceDateBulkingDisableRearchEnabledGiftingCurrency()
			throws InterruptedException {
		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();
		// business must be live, Enable skipping anniversary campaign schedule lock on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);
		// user creation
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 0);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Currency");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("currency"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("$10.00 of banked rewards-" + campaignName + ""),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5243 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch enabled gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 8)
	@Owner(name = "Amit Kumar")
	public void T5243_verifyAnniversarySpouseBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchEnabledGiftingReward()
			throws InterruptedException {
		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today's it will trigger 2 days before
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

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
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
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

	@Test(description = "SQ-T5243 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch enabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 9)
	@Owner(name = "Amit Kumar")
	public void T5243_verifyAnniversaryKidsBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchEnabledGiftingPoint()
			throws InterruptedException {
		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		// enable bulking
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		// enable rearch
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today
		String kid_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kid_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

//----------------------------------------------------------------------------------------
// bulking enable, rearch enable
//----------------------------------------------------------------------------------------
	@Test(description = "SQ-T5244 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With Reference Date bulking enable rearch enable gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 10)
	@Owner(name = "Amit Kumar")
	public void T5244_verifyAnniversaryCampaignLifespanDurationReferenceDateOnlyBulkingEnableRearchEnableGiftingReward()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// user creation using pos signup api
		String annivaersaryDate = CreateDateTime.getPastYearsDate(2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

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

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5244 Verify Anniversary_BirthdayCampaign_Lifespan Duration With N Days from rewarding bulking enable rearch enabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 11)
	@Owner(name = "Amit Kumar")
	public void T5244_verifyAnniversaryBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingEnableRearchEnableGiftingPoints()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// Date must be Nth date from today
		String birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5244 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With Reference Date bulking enable rearch enabled gifting currency", groups = {
			"Regression", "dailyrun" }, priority = 12)
	@Owner(name = "Amit Kumar")
	public void T5244_verifyAnniversaryHalfBirthdayCampaignLifespanDurationReferenceDateBulkingEnableRearchEnabledGiftingCurrency()
			throws InterruptedException {
		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();
		// business must be live, Enable skipping anniversary campaign schedule lock on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);
		// user creation
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 0);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Currency");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("currency"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("$10.00 of banked rewards-" + campaignName + ""),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5244 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking enable rearch enabled gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 13)
	@Owner(name = "Amit Kumar")
	public void T5244_verifyAnniversarySpouseBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingEnableRearchEnabledGiftingReward()
			throws InterruptedException {
		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today's it will trigger 2 days before
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

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
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
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

	@Test(description = "SQ-T5242 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking enable rearch enabled gifting point", groups = {
			"Regression", "dailyrun" }, priority = 14)
	@Owner(name = "Amit Kumar")
	public void T5244_verifyAnniversaryKidsBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingEnableRearchEnabledGiftingPoint()
			throws InterruptedException {
		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		// enable bulking
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns", "check",
				false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		// enable rearch
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "check", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today
		String kid_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kid_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	// ----------------------------------------------------------------------------------------
// bulking disable, rearch disable
//----------------------------------------------------------------------------------------
	@Test(description = "SQ-T5245 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With Reference Date bulking disable rearch disable gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 15)
	@Owner(name = "Amit Kumar")
	public void T5245_verifyAnniversaryCampaignLifespanDurationReferenceDateOnlyBulkingDisableRearchDisableGiftingReward()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// user creation using pos signup api
		String annivaersaryDate = CreateDateTime.getPastYearsDate(2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithAnniversary(userEmail, dataSet.get("locationKey"),
				annivaersaryDate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

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

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String anniversaryCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5245 Verify Anniversary_BirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch disable gifting point", groups = {
			"Regression", "dailyrun" }, priority = 16)
	@Owner(name = "Amit Kumar")
	public void T5245_verifyAnniversaryBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchDisableGiftingPoints()
			throws InterruptedException {
		// business must be live, Enable skipping anniversary campaign schedule lock on
		campaignName = "Automation Birthday Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// Date must be Nth date from today
		String birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Anniversary campaign detail: push notification, campaign name, gift reward validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5245 Verify Anniversary_HalfBirthdayCampaign_Lifespan Duration With Reference Date bulking disable rearch disable gifting currency", groups = {
			"Regression", "dailyrun" }, priority = 17)
	@Owner(name = "Amit Kumar")
	public void T5245_verifyAnniversaryHalfBirthdayCampaignLifespanDurationReferenceDateBulkingDisableRearchDisableGiftingCurrency()
			throws InterruptedException {
		campaignName = "Automation HalfBirthday Campaign" + CreateDateTime.getTimeDateString();
		// business must be live, Enable skipping anniversary campaign schedule lock on
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);
		// user creation
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String birthdate = CreateDateTime.getPastYearsWithFutureMonthAndDate(20, 6, 0);
		Response respo = pageObj.endpoints().posSignUpwithBirthday(userEmail, dataSet.get("locationKey"), birthdate);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos signup api");
		String userID = respo.jsonPath().get("id").toString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Currency");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("currency"));
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Half Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("$10.00 of banked rewards-" + campaignName + ""),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Anniversary Birthday campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */
	}

	@Test(description = "SQ-T5245 Verify Anniversary_SpouseBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch disable gifting reward", groups = {
			"Regression", "dailyrun" }, priority = 18)
	@Owner(name = "Amit Kumar")
	public void T5245_verifyAnniversarySpouseBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchDisableGiftingReward()
			throws InterruptedException {
		campaignName = "Automation Spouse Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today's it will trigger 2 days before
		String spouse_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationSpouseBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, spouse_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

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
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Spouse's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
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

	@Test(description = "SQ-T5245 Verify Anniversary_KidsBirthdayCampaign_Lifespan Duration With N Days from rewarding bulking disable rearch disable gifting point", groups = {
			"Regression", "dailyrun" }, priority = 19)
	@Owner(name = "Amit Kumar")
	public void T5245_verifyAnniversaryKidsBirthdayCampaignLifespanDurationNDaysFromRewardingBulkingDisableRearchDisableGiftingPoint()
			throws InterruptedException {
		campaignName = "Automation Kid Birthday Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		// enable bulking
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_bulk_insert_anniversary_campaigns",
				"uncheck", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
				"business_enable_skipping_anniversary_campaign_schedule_lock", "check", false);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_advance_anniversary_campaign_scheduling",
				"uncheck", false);
		// enable rearch
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_anniversary_rearch", "uncheck", true);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Date must be Nth date from today
		String kid_birthdate = CreateDateTime.getPastYearsWithFutureDate(20, 2);
		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelationKidsBirthdate(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("kidName1"), kid_birthdate);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 Create user relation is successful");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Points");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Kid's Birthday");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("2");
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
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(campaignName);

		Assert.assertTrue(anniversaryCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(Itemdata.contains("20 Bonus points earned-" + campaignName + ""),
				"Gifted points by campaign did not appeared in account history");
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