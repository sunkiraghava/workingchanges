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
public class E2E_PosCheckinsCampaignTest {

	private static Logger logger = LogManager.getLogger(E2E_PosCheckinsCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	// private String iFrameEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	// private SeleniumUtilities selUtils;;
	private static Map<String, String> dataSet;
	private String campName;
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
		utils = new Utilities(driver);
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

	@Test(groups = { "regression", "dailyrun" }, priority = -1)
	@Owner(name = "Shashank Sharma")
	public void setFlagCheckinsAndRedemptionsAsOn() throws InterruptedException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Turn off Checkins?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Turn off Redemptions?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
	}

	// shashank
	@Test(description = "SQ-T4733 E2E -- Post Checkin Offer Campaign with Guest Targeted from Campaign type reg 09/06", priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T4733_VerifyPostCheckinWithCheckinSegementType() throws Exception {
		campName = CreateDateTime.getUniqueString("AutomationE2EPostcheckinCampaignT4733_");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campName, dataSet.get("giftType"), campName,
				dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"), dataSet.get("QCName"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > 0, "Segment guest count is " + guestCount);
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuest();

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmendId"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logit("PASS", "Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is present in Segment");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();
		Response checkinResp = pageObj.endpoints().posCheckinWithItemID(date, userEmail, key, txn,
				dataSet.get("locationkey"), "4733", "4733");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campName);

		Assert.assertTrue(campaignNameStatus, campName + "Campaign name did not matched");
		utils.logPass(campName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * 
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campName, env);

	}

	// shashank
	@Test(description = "SQ-T4730 E2E -- Post Checkin Offer 04 sep with total redemption", priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T4730_VerifyPostCheckinWithTotalRedemptionSegementType() throws Exception {
		campName = CreateDateTime.getUniqueString("AutomationE2EPostcheckinCampaignT4730_");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// POS User SignUp
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"), dataSet.get("QCName"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), dataSet.get("segmendId"));

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmendId"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logit("PASS", "Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is present in Segment");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();
		Response checkinResp = pageObj.endpoints().posCheckinWithItemID(date, userEmail, key, txn,
				dataSet.get("locationkey"), "4730", "4730");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campName);

		Assert.assertTrue(campaignNameStatus, campName + "Campaign name did not matched");
		utils.logPass(campName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * 
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campName, env);
	}

	// shashank
	@Test(description = "SQ-T4728 E2E -- Post Checkin Offer Campaign with Top N loyalty type segment", priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T4728_VerifyPostCheckinWithTopNLoayltySegementType() throws Exception {

		campName = CreateDateTime.getUniqueString("AutomationE2EPostcheckinCampaignT4728_");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentname"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"), dataSet.get("QCName"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segmentname"));
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuest();

		Assert.assertTrue(userEmail != null && !userEmail.isEmpty(),
				"User email is not fetched from segment, please check the segment");
		utils.logit("User email fetched from segment: " + userEmail);
		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmendId"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is present in Segment");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";

		String exptectedTimeOfCheckin = Utilities.getCurrentTimeWithZone();
		Response checkinResp = pageObj.endpoints().posCheckinWithItemID(date, userEmail, key, txn,
				dataSet.get("locationkey"), "4728", "4728");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(campName);

		Assert.assertTrue(campaignNameStatus, campName + "Campaign name did not matched");
		utils.logPass(campName + " campaign is visible on user timeline page");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * 
		 * selUtils.longWait(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campName, env);

	}

	// shaleen
	@Test(description = "SQ-T4331 (1.0) post checkin campaign update", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Shaleen Gupta")
	public void T4331_verifyPostCheckinUpdate() throws Exception {
		// login to instance
		campName = CreateDateTime.getUniqueString("PostcheckinCampaignT328");

		// user signup using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = resp.jsonPath().get("id").toString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create new post checkin campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().clickNewCampaignBtn();

		// enter whom,what,when details
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		utils.logit("Post check-in campaign created successfully: " + campName);

		// verify user is able to update post check-in campaign
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().searchAndSelectCamapign(campName);
		utils.logit(" campaign name is searched and selected : " + campName);

		pageObj.campaignspage().selectCPPOptions("Edit");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setEndDateforAnniversaryCampaign();
		utils.logit(" End date in when details is entered successfully ");
		pageObj.signupcampaignpage().clickFinishButton();

		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, " Failed to update post check-in campaign");
		utils.logit("Verified that Post check-in campaign is updated successfully:  " + campName);

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campName, env);

	}

	// Rakhi
	@Test(description = "SQ-T5685 Verify deactivation of post checkin campaign when max gifting reached", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5685_verifyPostCheckinCampiagnDeactivatedMaxGiftingReached() throws Exception {

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Deactivate campaign when max gifting breached");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		campName = CreateDateTime.getUniqueString("PostcheckinCampaign");

		// create new post checkin campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().clickNewCampaignBtn();

		// enter whom,what,when details
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campName, dataSet.get("giftType"), campName,
				dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().setFiniteGifting("2");
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(campName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		utils.logit("Post check-in campaign created successfully: " + campName);

		// user signup using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Do two checkins
		Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.guestTimelinePage().pingSessionforLongWait(4);
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGiftedWithPooling(campName,2);
		Assert.assertTrue(itemCount == 2,
				"User did not get the gifting from first two checkin which is not the expected behaviour");
		utils.logPass("User get the gifting from first two checkin which is expected behaviour");
		pageObj.guestTimelinePage().pingSessionforLongWait(5);

		// Do third checkin
		Response checkinResponse3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");
		utils.logPass("POS checkin is successful for " + dataSet.get("amount") + " dollar");

		// timeline Validation
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount2 = pageObj.accounthistoryPage().getAccountDetailsforItemGiftedWithPooling(campName,2);
		Assert.assertTrue(itemCount2 == 2,
				"User get the gifting from third checkin as well which is not the expected behaviour");
		utils.logPass("User did not get the gifting after third checkin which is expected behaviour");

		pageObj.guestTimelinePage().pingSessionforLongWait(4);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// check if campaign gets deactivated after max gifting
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(campName, "Inactive");
		Assert.assertEquals(camStatus, "Inactive", "Camapign not deactiaved after max gifting");
		utils.logPass("Camapign gets deactiaved after max gifting");
		// pageObj.utils().deleteCampaignFromDb(campName, env);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
