package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostCheckinCampaignBetaTest {

	private static Logger logger = LogManager.getLogger(PostCheckinCampaignBetaTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";
	Utilities utils;

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
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2906 CSR-T149 Verify post check-in message beta Campaign", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2906_VerifyPostCheckinMessageBetaCampaign() throws InterruptedException, ParseException {

		campaignName = "AutoPostcheckinMessageBeta Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);

		pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();
		Thread.sleep(4000);
//		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType1(dataSet.get("segment"));
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setExecutionDelay();
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(scheduleMsg, "This campaign was successfully activated.",
				"Success message text did not matched");
		utils.logPass("Verify post check-in message beta Campaign is validated");

		// Edit duplicate beta from classic search
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDeleteNew("Edit");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDeleteNew("Duplicate");
		pageObj.campaignsbetaPage().clickCancelBtn();

	}

	@Test(description = "SQ-T2907 CSR-T149 Verify post check-in offer beta Campaign", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2907_VerifyPostCheckinOfferBetaCampaign() throws InterruptedException, ParseException {

		campaignName = "AutoPostcheckinOfferBeta Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);

		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(8000);
		utils.switchToWindow();
		Thread.sleep(8000);
		pageObj.campaignsbetaPage().setSegmentType1(dataSet.get("segment"));
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		Thread.sleep(10000);
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setExecutionDelay();
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();

		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(scheduleMsg, "This campaign was successfully activated.",
				"Success message text did not matched");
		utils.logPass(" Verify post check-in offer beta Campaig validated");

		// Edit duplicate beta from classic search
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDeleteNew("Edit");

		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDeleteNew("Duplicate");
		pageObj.campaignsbetaPage().clickCancelBtn();

	}

	@Test(description = "PS-T43 CSR-T149 Blaze Pizza | CPP not available for Beta Trigger Post Checkin and Signup Cams", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T43_BlazePizzaCPPNotAvailableForBetaTriggerPostCheckinAndSignupCams() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
		pageObj.campaignspage().searchAndSelectCamapign("Postcheckin Beta Do not Delete Regression");
		String postCheckinState = pageObj.campaignspage().pageState();
		Assert.assertTrue(postCheckinState.contains("Campaign Summary"), "CPP page Campaign Summary did not appeared");
		Assert.assertTrue(postCheckinState.contains("Campaign Type"), "CPP page Campaign Type details not appeared");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign("Signup Beta Do not Delete Regression");
		String signupState = pageObj.campaignspage().pageState();
		Assert.assertTrue(signupState.contains("Campaign Summary"), "CPP page Campaign Summary did not appeared");
		Assert.assertTrue(signupState.contains("Campaign Type"), "CPP page Campaign Type details not appeared");
		// Edit duplicate beta from classic search
		pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDeleteNew("Edit");
		/*
		 * pageObj.menupage().clickCampaignsMenu();
		 * pageObj.menupage().clickCampaignsLink(); pageObj.campaignspage().
		 * searchAndSelectCamapign("Signup Beta Do not Delete Regression");
		 * pageObj.campaignsbetaPage().SelectClassicCSPOptionsEditDelete("Duplicate");
		 * pageObj.campaignsbetaPage().clickCancelBtn();
		 */

	}

	@Test(description = "SQ-T3530 Verify guest targeted with post checkin offer campaign with segment guests targeted by a signupcampaign after checkin",groups = {"nonNightly","regression" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T3530_VerifyVerifyGuestTargetedWithPostCheckinOfferCampaignWithSegmentGuestsRedeemedSignupCampaign()
			throws InterruptedException {

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		// Signup campaign and rewarding must be done
		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// fetch user offers/ reward_id using ap2 mobileOffers
		utils.longWaitInSeconds(5);
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		String redemption_code = redemptionResponse.jsonPath().get("redemption_tracking_code").toString();
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, redemptionResponse.getStatusCode(),
				"Status code 201 did not matched for api2 create redemption using reward_id");
		utils.logPass("Api2 Create Redemption using reward_id is successful");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemption_code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		campaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
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
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType("GuestsRedeemedSignupCampaign");
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// Pos api checkin
		String checkin_key = CreateDateTime.getTimeDateString();
		String checkin_txn = "123456" + CreateDateTime.getTimeDateString();
		String checkin_date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response checkin_resp = pageObj.endpoints().posCheckin(checkin_date, userEmail, checkin_key, checkin_txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkin_resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// timeline validation
		String postCheckinCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		String giftedItemName = pageObj.guestTimelinePage().getRewardName();

		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		softassert.assertTrue(postCheckinCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		softassert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		softassert.assertAll();
		utils.logPass(
				"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value Thread.sleep(2000);
		 * pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);
		 */

	}

	@Test(description = "SQ-T4695 Verify the error message when post checkin offer beta campaign end date is same as redeemable end date", groups = {
			"regression", "unstable", "dailyrun","nonNightly" })
	@Owner(name = "Vansham Mishra")
	public void T4695_verifyErrorMessagePostCampaignEndDateIsSame() throws InterruptedException, ParseException {
		campaignName = CreateDateTime.getUniqueString("Automation Postcheckin beta");
		String redeeamable = CreateDateTime.getUniqueString("Automation redeeemable");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().enterRedeemableWithQCAndFlatDiscountWithEndDate(redeeamable, "Flat Discount", "",
				"2.0", dataSet.get("timeZone"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("redeemable_redeemable_indefinite_expiry", "uncheck");
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(2);
		pageObj.redeemablePage().clickFinishBtn();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		pageObj.campaignsbetaPage().clickContinueBtn();

		utils.switchToWindow();
		pageObj.campaignsbetaPage().clickNextBtn();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(2);
		pageObj.campaignsbetaPage().setRedeemable(redeeamable);
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		utils.waitTillPagePaceDone();
		pageObj.campaignsbetaPage().setStartDateNew();
		pageObj.campaignsbetaPage().setExecutionDelay();
		pageObj.campaignsbetaPage().setEndDate(2);
		pageObj.campaignsbetaPage().clickNextBtn();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(10);
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();

		String msg = pageObj.campaignsbetaPage().errorMsg();
		Assert.assertTrue(msg.contains(dataSet.get("expectedMsg")),
				"error msg is not there when the end date of the campaign is greater than or equal to the redeemable end date");
		logger.info(
				"Verified error msg is there when the end date of the campaign is greater than or equal to the redeemable end date");
		utils.logPass(
				"Verified error msg is there when the end date of the campaign is greater than or equal to the redeemable end date");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
