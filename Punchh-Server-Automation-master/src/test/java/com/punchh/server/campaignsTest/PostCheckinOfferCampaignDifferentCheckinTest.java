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

// Author:Shashank sharma
@Listeners(TestListeners.class)
public class PostCheckinOfferCampaignDifferentCheckinTest {

	private static Logger logger = LogManager.getLogger(PostCheckinOfferCampaignDifferentCheckinTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName, campaignName2;
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3560 Verify user receive the post check-in offer with gift points by doing Mobile Api check-in-qrcode checkin"
			+ "SQ-T3560 verify Post checkin cam run with segment type ( transactional segment) and check user get reward properly", priority = 0, groups = {
					"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T3560_verifyPostCampaignOfferGiftTypeAsPointsAndMobileCheckin() throws InterruptedException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		campaignName = "AutomationPostCheckinCampaign_" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checking campaign is created");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String qrCode = pageObj.instanceDashboardPage().captureBarcode();
		logger.info(qrCode);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().setPostCheckinPushNotification(campaignName);
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		Response qrcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinQRCode(dataSet.get("client"),
				dataSet.get("secret"), token, qrCode);
		Assert.assertEquals(qrcodeCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Loyalty Checkin by qr code");

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameWithWait(postCheckinOfferName);
		 * boolean pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * 
		 * pageObj.guestTimelinePage().clickAccountHistory(); boolean rewardPointsStatus
		 * = pageObj.accounthistoryPage()
		 * .getAccountDetailsforRewardEarned(dataSet.get("expectedRewardMessage"),
		 * "Bonus");
		 */
		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory.contains("10 Bonus points earned for participating in " + campaignName),
				"Gifted item name did not matched");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// Auth API checkin / Gift Redeemable
	@Test(description = "SQ-T3559 (1.0) Verify user receive the post check-in offer with redeemable by doing api check-in", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3559_verifyPostCampaignOfferGiftTypeAsRedeemableAndAuthAPICheckin() throws InterruptedException {

		campaignName = "AutomationPostCheckinCampaign_" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checking campaign is created");
		String amount = dataSet.get("amount");

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
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redemable"));
		pageObj.signupcampaignpage().setPostCheckinPushNotification(campaignName);
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// do auth api checkin
		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Checkin via auth API
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Timeline validation (move to user timeline)
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); boolean
		 * campaignNameStaus =
		 * pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(
		 * postCheckinOfferName); boolean pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * 
		 * pageObj.guestTimelinePage().clickAccountHistory(); boolean rewardPointsStatus
		 * = pageObj.accounthistoryPage()
		 * .getAccountDetailsforRewardEarned(dataSet.get("expectedRewardMessage"),
		 * "Item");
		 */

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redemable")),
				"Gifted item name did not matched");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// Dashboard checkin / Gift Derived Rewards
	// As per khushboo This feature is not owned by any team currently it will take
	// time so till then dont execute this
	// @Test(description = "SQ-T3558 Verify user receive the post check-in offer
	// with derived reward by doing Dashboard check-in", priority = 2)
	public void T3558_verifyPostCampaignOfferGiftTypeAsDerivedRewardsAndDashboardCheckin() throws InterruptedException {

		campaignName = "Automation_DashboardCheckin_Campaign" + CreateDateTime.getTimeDateString();
		logger.info("campaign Name: " + campaignName);
		userEmail = dataSet.get("email");
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

		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setDelightGuestWithValue(dataSet.get("derivedReward"));
		pageObj.signupcampaignpage().setSurpriseOnceEvery("1");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPostCheckinPushNotification(campaignName);
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().clickPosConsoleBtn();
		pageObj.dashboardpage().searchUser(userEmail);
		pageObj.consolePage().selectRandomCategory();
		pageObj.consolePage().setDiscountAmount(dataSet.get("amount"));

		String msg = pageObj.consolePage().processTransaction();
		Assert.assertTrue(msg.contains("Successfully awarded"),
				"Redemption success message did not displayed on pos console");
		TestListeners.extentTest.get().pass("Redemption done successfully from pos console");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		/*
		 * boolean campaignNameStaus = pageObj.guestTimelinePage()
		 * .verifyIsCampaignExistOnTimeLine(postCheckinOfferCampaign); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		 * 
		 * pageObj.guestTimelinePage().clickAccountHistory(); boolean rewardPointsStatus
		 * = pageObj.accounthistoryPage()
		 * .getAccountDetailsforRewardEarned(dataSet.get("expectedRewardMessage"),
		 * "Rewards");
		 */

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory.contains("20 Bonus points earned for participating in " + campaignName),
				"Gifted item name did not matched");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * Thread.sleep(2000);
		 * pageObj.campaignspage().removeSearchedCampaign(campaignName);
		 */

	}

	// Barcode checkin / Currency

	@Test(description = "SQ-T3557 Verify user receive the post check-in offer with currency by doing barcode checkin ", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T3557_verifyPostCampaignOfferGiftTypeAsCurrencyAndBarCodeCheckin() throws InterruptedException {

		campaignName = "AutomationPostCheckinCampaignCurrency_" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checking campaign is created");

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
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().addLocationInCampaign("Automation - 1");
		pageObj.signupcampaignpage().setPostCheckinPushNotification(campaignName);
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();

		// User Signip using mobile api 2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		// iFrame Login
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		pageObj.iframeSingUpPage().iframeCheckin(barcode);

		/*
		 * Assert.assertTrue(pageObj.guestTimelinePage().
		 * verifyBarcodeCheckinOnGuestTimeline(barcode),
		 * "Error in verifying barcode in guest time line ");
		 * 
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameWithWait(postCheckinOfferName);
		 * boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		 * Assert.assertTrue(campaignName.equalsIgnoreCase(postCheckinOfferName),
		 * "Campaign name did not matched");
		 * Assert.assertTrue(campaignNotificationStatus,
		 * "Campaign notification did not displayed...");
		 * Assert.assertTrue(pushNotificationStatus,
		 * "Push notification did not displayed...");
		 */

		String camName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(camName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("$4.00 of banked rewards-" + campaignName),
				"Gifted item name did not matched");
		TestListeners.extentTest.get().pass("Push notification is visible on user timeline page");

		/*
		 * pageObj.guestTimelinePage().clickAccountHistory(); boolean rewardPointsStatus
		 * = pageObj.accounthistoryPage()
		 * .getAccountDetailsforRewardEarned(dataSet.get("expectedRewardMessage"),
		 * "Rewards"); Assert.assertTrue(rewardPointsStatus,
		 * "Reward Message not found");
		 */
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * Delete created campaign pageObj.menupage().navigateToSubMenuItem("Campaigns",
		 * "Campaigns"); pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */

	}

	// Rakhi
	@Test(description = "SQ-T5381 Create duplicate classic Post checkin Campaign", priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T5381_duplicatePostCheckinCampaign() throws InterruptedException {
		campaignName = "AutomationPostCheckinCampaign_" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checking campaign is created");

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

		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setPostCheckinPushNotification(dataSet.get("pushNotification") + " " + campaignName
				+ " " + "{{{business_name}}}" + " and reward name as" + "{{{first_name}}}");
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject") + " " + campaignName
				+ " " + "{{{gifted_amount}}}" + "and reward id as" + "{{{business_name}}}");
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// search and duplicate campaign
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		campaignName2 = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(campaignName, "Edit");
		Assert.assertEquals(campaignName2, campaignName + " - copy");
		logger.info("Campaign name is prefilled as : " + campaignName + " - copy");
		TestListeners.extentTest.get().pass("Campaign name is prefilled as : " + campaignName + " - copy");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().activateCampaign();

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		// Delete created campaign
		// pageObj.campaignspage().removeSearchedCampaign(campaignName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}