package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
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
public class PostCheckinWithXPointsTest {

	private static Logger logger = LogManager.getLogger(PostCheckinWithXPointsTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String run = "ui";
	private String campaignName;
	private String campaignName2;

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

	// redundent cases of - T2254_verifyPostCheckinCampaignWithGiftPoint3xandQC

	@Test(description = "SQ-T3543 Post checkin campaign with gift point 2x and QC", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3543_verifyPostcheckincampaignwithgiftpoint2xandQC() throws Exception {
		campaignName = CreateDateTime.getUniqueString("Automation PostcheckinQC Campaign");
		logger.info("Campaign name is :" + campaignName);
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
		// create postcheckin campaign with qc and points
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinQCCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("giftPoints"), dataSet.get("pointsType"));

		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(campaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"), dataSet.get("qcItem"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Post Checkin Campaign with QC and points is not created...");
		 */

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"),
				dataSet.get("menuItemid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 */

		/*
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * postCheckinCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 * pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 * System.out.println(Itemdata);
		 */

		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory.contains("10 Bonus points earned for participating in " + campaignName),
				"Gifted points did not appeared in account history");
		// Assert.assertTrue(Itemdata.get(2).contains("10 points"), "Gifted points are
		// not equal to 2x");
		pageObj.utils().logPass(
				"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// user creation using pos signup api qc unqualifies should not trigger this
		// campaign for guest
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Pos api checkin
		String key1 = CreateDateTime.getTimeDateString();
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp1 = pageObj.endpoints().posCheckinQC(date1, userEmail1, key1, txn1, dataSet.get("locationKey"),
				dataSet.get("menuItemid1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1); boolean
		 * campaignName1 =
		 * pageObj.guestTimelinePage().CheckIfCampaignTriggered(postCheckinCampaignName)
		 * ; pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata1 =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 * System.out.println(Itemdata1);
		 */

		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		String rewardGiftedAccountHistory1 = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);

		Assert.assertFalse(campaignName1.equalsIgnoreCase(campaignName),
				"Postcheckin campaign with non qualifing qc should not trigger");
		Assert.assertFalse(
				rewardGiftedAccountHistory1.contains("10 Bonus points earned for participating in " + campaignName),
				"Postcheckin campaign with non qualifing qc trigger details should not appear in account history");
		pageObj.utils().logPass("Postcheckin campaign with non qualifing qc did not triggered");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		 * pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdFreePunchhCampaignsTable(campaignid, env);
	}

	// duplicate of T3518_verifySignupCampaignGiftingCurrencyToUserCreationWithApi2
	@Test(description = "SQ-T2183 Validate signup campaign with reward currency", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2183_verifySignupcampaignwithrewardcurrency() throws Exception {
		campaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign With Currency");
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
		pageObj.signupcampaignpage().setCampaignNameandGiftType(campaignName, dataSet.get("giftType"), campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(campaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status, "SignUp Campaign is not created...");
		 */

// User register/signup using api2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		/*
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotification();
		 */
// String campaignName = pageObj.guestTimelinePage().getcampaignName();

		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("$4.00 of banked rewards-" + campaignName),
				"Gifted currency by campaign did not appeared in account history");
		pageObj.utils().logPass(
				"signup campaign with currency detail: push notification, campaign name, reward notification validated successfully on timeline");
		pageObj.utils().logPass("Signup campaign reward currency is validated in acount history");

		/*
		 * pageObj.guestTimelinePage().clickAccountHistory(); String Itemdata =
		 * pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(
		 * signUpCampaignName); //List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		 * System.out.println(Itemdata);
		 */

		/*
		 * Assert.assertTrue(Itemdata.get(0).contains("Rewards Earned"),
		 * "Earned bonus point entry did not appeared in account history");
		 * Assert.assertTrue(Itemdata.get(2).contains("+$4.00"),
		 * "reward item did not increased in account balance");
		 */

// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().selectOngoingdrpValue("Signup");
		 * pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);
		 */
// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid, env);
	}

	// duplicate of - T4331_verifyPostCheckinUpdate
	// Anant
	@Test(description = "MPC-T328 post checkin campaign update", priority = 4, groups = { "regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T328_postCheckinCampaignUpdate() throws InterruptedException {
		campaignName = "AutomationPostCheckinCampaign" + CreateDateTime.getTimeDateString();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		//
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"), "", "");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);

		pageObj.signupcampaignpage().editGiftPointsClassicCampaign(dataSet.get("updateGiftPoints"));
		pageObj.redeemablePage().clickFinishBtn();

		String msg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertTrue(msg.contains("Campaign saved"), "Campaign did not update successfully");
		pageObj.utils().logPass("Campaign updated successfully");

		/*
		 * pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		 * pageObj.campaignspage().classicCampaignPageDeleteCampaign();
		 * logger.info("Campaign deleted successfully");
		 * TestListeners.extentTest.get().pass("Campaign deleted successfully");
		 */
	}

	// duplicate of T4725_VerifyPostRedemptionWithCheckinsSegementType
	// shashank
	@Test(description = "SQ-T4734 E2E -- Post redemption Campaign with Checkin Type Segment")
	@Owner(name = "Shashank Sharma")
	public void T4734_VerifyPostRedemptionWithCheckinSegementType() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("AutomationE2EPostRedemptionCampaignT4734_");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// POS User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));

		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();
		System.out.println("campName-- " + campaignName);
		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), dataSet.get("segmentID"));
//			// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser("Redeemable", dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().navigateToTabs("Rewards");

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail, dataSet.get("locationkey"),
				rewardLst.get(0), "4734");
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(0));

		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campaignName);

		Assert.assertTrue(campaignNameStatus, campaignName + "Campaign name did not matched");
		pageObj.utils().logPass(campaignName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// RecurringDaysOff makes it normal flow which is already covered
	@Test(description = "SQ-T4514 Verify the gifting from post redemption offer campaign when campaign hasn't recurring_days")
	@Owner(name = "Vansham Mishra")
	public void T4514_postRedeemptionOfferCampaignRecurringDaysOff() throws InterruptedException, ParseException {
		campaignName = "AutoPosRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("reason"), dataSet.get("points"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 2
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 3
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 3);

		for (int i = 0; i < 3; i++) {
			Response posRedeem1 = null;
			posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
					rewardLst.get(i));
			Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
			pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardLst.get(i));
		}

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int size = pageObj.guestTimelinePage().noOfTimesCampaignVisibleInTimeLine(campaignName, 3);
		Assert.assertEquals(size, 3, "campaign did not trigger two times");
		pageObj.utils().logPass("Verified as expected campaign trigger 2 times");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	// RecurringDaysOff makes it normal flow which is already covered
	@Test(description = "SQ-T4515 Verify the gifting from post redemption message campaign when campaign hasn't recurring_days")
	@Owner(name = "Vansham Mishra")
	public void T4515_postRedeemptionMsgCampaignRecurringDaysOff() throws InterruptedException, ParseException {
		campaignName = "AutoPosRedeemptionMsg" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("msgDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().posRedeemptionMsgCampaignSetName(campaignName);
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemMsgCampaign(dataSet.get("redeemable2"));
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable 1
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 2
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable 3
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 3);

		for (int i = 0; i < 3; i++) {
			Response posRedeem1 = null;
			posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
					rewardLst.get(i));
			Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
			pageObj.utils().logPass(
					"Verified able to redeem the first redeemable having the redeemable id --" + rewardLst.get(i));
		}

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int size = pageObj.guestTimelinePage().noOfTimesCampaignVisibleInTimeLine(campaignName, 3);
		Assert.assertEquals(size, 3, "campaign did not trigger two times");
		pageObj.utils().logPass("Verified as expected campaign trigger 2 times");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */

	}

// its simple ui flow whivh is already covered in other cases
	// shaleen
	@Test(description = "SQ-T4364 (1.0) Post Redemption UI & Verbiage updates for GTM", priority = 1)
	@Owner(name = "Shaleen Gupta")
	public void T4364_verifyUpdatesOnPostRedemptionUI() throws Exception {

		campaignName = "AutomationPostRedemption" + CreateDateTime.getTimeDateString();
		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to whom page on post redemption message campaign and observe
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpVal"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		List<String> lst = new ArrayList<>();
		lst = pageObj.signupcampaignpage().campaignTriggerLst();
		boolean verify = lst.contains(dataSet.get("itemNotInList_1"));
		Assert.assertFalse(verify, "Item is present , Verbiage has not been updated");
		boolean verify2 = lst.contains(dataSet.get("itemNotInList_2"));
		Assert.assertFalse(verify2, "Item is present , Verbiage has not been updated");

		boolean verify3 = lst.contains(dataSet.get("itemInList_1"));
		Assert.assertTrue(verify3, "Item is not present , Verbiage has not been updated");
		boolean verify4 = lst.contains(dataSet.get("itemInList_2"));
		Assert.assertTrue(verify4, "Item is not present , Verbiage has not been updated");

		pageObj.utils().logPass("Verified that Verbiage has been updated in whom page of:" + dataSet.get("messageDrpVal")
				+ " campaign");

		// navigate to whom page on post redemption offer campaign and observe
		campaignName2 = "AutomationPostRedemption" + CreateDateTime.getTimeDateString();
		// String campaignid2;
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpVal"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName2);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(dataSet.get("reason"));
		pageObj.signupcampaignpage().setGiftPoints("2");
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid2 = pageObj.signupcampaignpage().getCampaignid();
		List<String> lst2 = new ArrayList<>();
		lst2 = pageObj.signupcampaignpage().campaignTriggerLst();
		boolean verify5 = lst2.contains(dataSet.get("itemNotInList_1"));
		Assert.assertFalse(verify5, "Item is present , Verbiage has not been updated");
		boolean verify6 = lst2.contains(dataSet.get("itemNotInList_2"));
		Assert.assertFalse(verify6, "Item is present , Verbiage has not been updated");

		boolean verify7 = lst2.contains(dataSet.get("itemInList_1"));
		Assert.assertTrue(verify7, "Item is not present , Verbiage has not been updated");
		boolean verify8 = lst2.contains(dataSet.get("itemInList_2"));
		Assert.assertTrue(verify8, "Item is not present , Verbiage has not been updated");

		pageObj.utils().logPass(
				"Verified that Verbiage has been updated in whom page of:" + dataSet.get("offerDrpVal") + " campaign");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		/*
		 * pageObj.campaignspage().searchAndDeleteDraftCampaignClassic(campName);
		 * pageObj.campaignspage().searchAndDeleteDraftCampaignClassic(campName2);
		 */
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
		// pageObj.utils().deleteCampaignFromDb(campaignName2, env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
