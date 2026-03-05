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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostpurchaseCampaignTest {

	private static Logger logger = LogManager.getLogger(PostpurchaseCampaignTest.class);
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
	private String duplicateCamname;
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

	@Test(description = "SQ-T2520 Verify Post_redemption_campaign_type_of_rewards_Redeemable"
			+ "SQ-T5387 Create Post Redemption campaign with delay and duplicate", groups = "Regression", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2520_verifyPostredemptioncampaigntypeofrewardsRedeemable() throws Exception {

		String reward_Code = "";
		campaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
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

		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setExecutionDelay("2");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Send gift to user and redeem from Iframe
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemableName"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		// String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// iFrame Login and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer("$1.0 OFF (Never Expires)");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, reward_Code, key, txn,
				dataSet.get("location"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// Navigate to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Navigate to time line
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(2);

		String postRedemptionCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		int diff = pageObj.guestTimelinePage().getTimeDiffPostRedemptionCampaign();

		Assert.assertTrue(postRedemptionCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $2.0 OFF (" + campaignName + ")"),
				"Gifted points did not appeared in account history");
		Assert.assertTrue(diff == 2 | diff == 3 | diff == 4 | diff == 5,
				"Campaign Delayed time did not matched :" + diff);
		TestListeners.extentTest.get().pass(
				"Post redemption campaign detail: push notification, campaign name, validated successfully on timeline");

		// search and duplicate classic campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		duplicateCamname = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(campaignName, "Edit");
		Assert.assertEquals(duplicateCamname, campaignName + " - copy");
		utils.logPass("Campaign name is prefilled as : " + campaignName + " - copy");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		// pageObj.newCamHomePage().activateChallengeCampaign();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Post Checkin Campaign is not created...");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value Thread.sleep(2000);
		 * pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(postRedemptionCampaignName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
	}

	@Test(description = "SQ-T2517 VerifyPost_purchase_campaign_type_of_rewards_Redeemable", groups = "Regression", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2517_verifyPostPurchaseCampaignTypeOfRewardsRedeemable() throws Exception {
		campaignName = CreateDateTime.getUniqueString("Automation Postpurchase Campaign");
		logger.info("Campaign name is :" + campaignName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "100");

		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		// Click Campaigns Link
		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
		pageObj.campaignspage().clickPostPurchaseBtn();
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		// pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));

		// Set amount and Payment type
		pageObj.signupcampaignpage().setAmountandPaymentType("1", "20", "Purchased");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Gift card purchase apiv1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase :"
						+ purchaseGiftCardResp.asPrettyString());

		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * postpurchaseCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 */

		String postpurchaseCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(postpurchaseCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $2.0 OFF (" + campaignName + ")"),
				"Campaign notification did not displayed...");
		// Assert.assertTrue(pushNotificationStatus, "Push notification did not
		// displayed...");
		TestListeners.extentTest.get().pass(
				"Post redemption campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value pageObj.campaignspage().clickPostPurchaseBtn();
		 * pageObj.campaignspage().removeSearchedCampaign(postpurchaseCampaignName);
		 */
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
	}

	@Test(description = "SQ-T2551 Verify Post_checkin_message_Campaign", groups = "Regression", priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2551_verifyPostcheckinmessageCampaign() throws Exception {
		campaignName = "Automation Postcheckin Message Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setPNforPostMessageCampaign(campaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * postcheckinMessageCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 */
		String postcheckinMessageCampaignName = pageObj.guestTimelinePage()
				.getCampaignNameByNotificationsAPI(campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(postcheckinMessageCampaignName.equalsIgnoreCase(campaignName),
				"Campaign name did not matched");
		TestListeners.extentTest.get().pass(
				"Post purchase campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		 * pageObj.campaignspage().removeSearchedCampaign(postcheckinMessageCampaignName
		 * );
		 */
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
	}

	// Shubham
	@Test(description = "SQ-T6479 Verify gifting from post purchase campaign when business has date less than the  UTC date",groups = {"nonNightly" }, priority = 3)
	@Owner(name = "Shubham Gupta")
	public void T6479_verifyGiftingAndRewardEndDate() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		campaignName = CreateDateTime.getUniqueString("Automation PostPurchase Campaign");
		logger.info("Campaign name is :" + campaignName);

		String timezone = pageObj.signupcampaignpage().getTimezoneGreaterOrLessThanUTC();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.cockpitDashboardMiscPage().setBusinessTimezone("New Delhi ( IST )");

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "100");

		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		// Click Campaigns Link
		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
		pageObj.campaignspage().clickPostPurchaseBtn();
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		// pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentType"));

		// Set amount and Payment type
		pageObj.signupcampaignpage().setAmountandPaymentType("1", "20", "Purchased");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Gift card purchase apiv1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase :"
						+ purchaseGiftCardResp.asPrettyString());

		String postpurchaseCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(postpurchaseCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $2.0 OFF (" + campaignName + ")"),
				"Campaign notification did not displayed...");

		pageObj.utils().updateBusinessTimezone(dataSet.get("slug"), timezone, env);

		// user creation using api2
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Gift card purchase apiv1
		Response purchaseGiftCardResp1 = pageObj.endpoints().Api1PurchaseGiftCard(userEmail1, dataSet.get("client"),
				dataSet.get("secret"), "12", token1, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase :"
						+ purchaseGiftCardResp1.asPrettyString());

		postpurchaseCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);

		Assert.assertFalse(postpurchaseCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertFalse(rewardGiftedAccountHistory.contains("You were gifted: $2.0 OFF (" + campaignName + ")"),
				"Campaign notification did not displayed...");
		utils.logPass("Verfied user didn't get the gifting Second time which is the expected behaviour");

		// pageObj.utils().deleteCampaignFromDb(campaignName, env);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().deleteCampaignFromDb(duplicateCamname, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}