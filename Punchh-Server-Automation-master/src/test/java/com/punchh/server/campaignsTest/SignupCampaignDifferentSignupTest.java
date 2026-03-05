package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.List;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SignupCampaignDifferentSignupTest {

	private static Logger logger = LogManager.getLogger(SignupCampaignDifferentSignupTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String run = "ui";
	private String signUpCampaignName;

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

	@Test(description = "SQ-T3519 SignUp campaign gifting points to user signedup with api1"
			+ ", SQ-T6577 Verify signup campaign with bulking", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T3519_verifySignupCampaignGiftingPointsToUserCreationWithApi1() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Bulking For Signup Campaign", "check");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(signUpCampaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user signup using api1 signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token").toString();

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotification();
		 */

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("10 Bonus points earned-" + signUpCampaignName),
				"Gifted points did not appeared in account history");

		/*
		 * pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 * System.out.println(Itemdata); Assert.assertTrue(Itemdata.get(0).
		 * contains("10 Bonus points earned-Welcome Gift"),
		 * "Gifted points by campaign did not appeared in account history");
		 */
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Bulking For Signup Campaign", "uncheck");
		// Delete created campaign commented as deleting in @aftermethod
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@Test(description = "SQ-T3518 SignUp campaign gifting currency to user signedup with api2", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3518_verifySignupCampaignGiftingCurrencyToUserCreationWithApi2() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(signUpCampaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotification();
		 */
		// boolean rewardedRedeemableStatus =
		// pageObj.guestTimelinePage().verifyrewardedRedeemable();

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");

		/*
		 * pageObj.guestTimelinePage().clickAccountHistory(); String Itemdata =
		 * pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(
		 * signUpCampaignName);
		 */
		// List<String> Itemdata =
		// pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		// System.out.println(Itemdata);
		Assert.assertTrue(rewardGiftedAccountHistory.contains("$10.00 of banked rewards-" + signUpCampaignName),
				"Gifted currency by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // //
		 * Select offer dropdown value
		 * pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@Test(description = "SQ-T3517 SignUp campaign gifting derived reward to user signedup with authapi", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T3517_verifySignupCampaignGiftingDerivedrewardToUserCreationWithAuthapi() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("derivedReward"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(
				(dataSet.get("pushNotification") + " " + signUpCampaignName + " " + "{{{reward_name}}}"
						+ "and reward id as" + "{{{reward_id}}}"),
				(dataSet.get("emailSubject") + " " + signUpCampaignName + " " + "{{{business_name}}}"
						+ "and reward name as" + "{{{reward_name}}}"),
				(dataSet.get("emailTemplate") + " " + signUpCampaignName + " " + "{{{business_name}}}"
						+ "and reward name as" + "{{{reward_name}}}"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user signup using api2 signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		// boolean rewardedRedeemableStatus =
		// pageObj.guestTimelinePage().verifyrewardedRedeemable();

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("20 Bonus points earned-Welcome Gift"),
				"Gifted currency by campaign did not appeared in account history");

		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// Delete created campaign

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);

	}

	@Test(description = "SQ-T3516 Sign Up Campaign with gift type only messaging using user signed with iFrame", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T3516_verifySignUpCampaignWithGiftTypeOnlyMessagingUserSignedWithIFrame() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@Test(description = "SQ-T3545 Sign Up Campaign with recurrence reward", groups = { "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T3545_verifySignUpCampaignWithRecurrenceReward() throws Exception {

		// enable flag in cockpit>misc Enable to show the list of all redeemables
		// including recurrence

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(signUpCampaignName, dataSet.get("giftType"),
				signUpCampaignName, dataSet.get("redeemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(signUpCampaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotification(); String
		 * rewardedRedeemableStatus =
		 * pageObj.guestTimelinePage().verifyCampaignRewardName();
		 */

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory
						.contains("You were gifted: recurrence redeemable (" + signUpCampaignName + ")"),
				"Gifted currency by campaign did not appeared in account history");

		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// validate sidekiq jobs for RecurringRewardWorker
		int count = pageObj.schedulespage().checkSidekiqJob(baseUrl, "RecurringRewardWorker");
		Assert.assertTrue(count > 1, "RecurringRewardWorker count did not matched");

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@Test(description = "SB-T75 Signup campaign gets triggered upon addition of a new signed up user segment all signedup", groups = "Regression", priority = 5)
	@Owner(name = "Amit Kumar")
	public void T75_verifySignUpCampaignTriggeredOnAdditionOfNewSignedUpUserSegmentAllSignedup() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured
		// pageObj.signupcampaignpage().setAudianceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(signUpCampaignName, dataSet.get("giftType"),
				signUpCampaignName, dataSet.get("redeemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(signUpCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status, "SignUp Campaign is not created...");
		 */

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotification();
		 */
		// boolean rewardedRedeemableStatus =
		// pageObj.guestTimelinePage().verifyrewardedRedeemable();

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");

		/*
		 * pageObj.guestTimelinePage().clickAccountHistory(); String Itemdata =
		 * pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(
		 * signUpCampaignName);
		 */

		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $2.0 OFF (" + signUpCampaignName + ")"),
				"Gifted currency by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	// Rakhi
	@Test(description = "SQ-T5382 Create duplicate classic signup Campaign", groups = { "regression",
			"dailyrun" }, priority = 6)
	@Owner(name = "Rakhi Rawat")
	public void T5382_duplicateSignupCampaign() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// search and duplicate classic campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(signUpCampaignName);
		String name = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(signUpCampaignName, "Edit");
		Assert.assertEquals(name, signUpCampaignName + " - copy");
		logger.info("Campaign name is prefilled as : " + signUpCampaignName + " - copy");
		pageObj.utils().logPass("Campaign name is prefilled as : " + signUpCampaignName + " - copy");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().activateCampaign();

		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Post Checkin Campaign is not created...");

		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName + " - copy");
		pageObj.newCamHomePage().deleteCampaign(signUpCampaignName + " - copy");
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(signUpCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
