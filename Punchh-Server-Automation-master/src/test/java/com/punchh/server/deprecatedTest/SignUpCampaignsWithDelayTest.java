package com.punchh.server.deprecatedTest;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SignUpCampaignsWithDelayTest {

	private static Logger logger = LogManager.getLogger(SignUpCampaignsWithDelayTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String signUpCampaignName;

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

// Amit // covered within SQ-T2233
	@Test(description = "SQ-T3679 Sign Up Campaign With Delay", groups = { "regression", "dailyrun" }, priority = 0)
	public void T3679_verifySignupCampaignWithDelay() throws InterruptedException, ParseException {

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
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(signUpCampaignName, dataSet.get("giftType"),
				signUpCampaignName, dataSet.get("redeemable"));
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(signUpCampaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));

		// check if execution delay is negative value then error message showing
		// properly
		pageObj.signupcampaignpage().setExecutionDelay("-5");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		String message = pageObj.utils().getSuccessMessage();
		Assert.assertEquals(message, "Execution Delay must be greater than or equal to 0", "Message did not match.");
		pageObj.utils().logPass("Successfully verified error message for negative execution delay");

		// check execution with positive value
		pageObj.signupcampaignpage().setExecutionDelay("5");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		pageObj.guestTimelinePage().pingSessionforLongWait(4);
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(signUpCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		int diff = pageObj.guestTimelinePage().timeDiffCampTrigger();
		Assert.assertTrue(diff == 5 | diff == 6, "Campaign Delayed time did not matched :" + diff);
		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redeemable")),
				"reward gifted in account history did not matched");
		pageObj.utils().logPass("Signup campaign with delay trigger validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Signup");
		pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);

	}

	// covered within SQ-T2187
	@Test(description = "SQ-T3680 Post Checkin Campaign With Delay", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3680_verifyPostCheckinCampaignWithDelay() throws InterruptedException, ParseException {
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
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
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), postCheckinCampaignName, dataSet.get("redeemable"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setExecutionDelay("5");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		 */

// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(4);

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postCheckinCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(postCheckinCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		int diff = pageObj.guestTimelinePage().timeDiffCampTrigger();
		Assert.assertTrue(diff == 5 | diff == 6, "Campaign Delayed time did not matched");
		Assert.assertTrue(campaignName.equalsIgnoreCase(postCheckinCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redeemable")),
				"reward gifted in account history did not matched");
		pageObj.utils().logPass(
				"Postcheckin campaign trigger with delay validated successfully on timeline");

// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

	}

	// covered within SQ-T2520
	@Test(description = "SQ-T5387 Create Post Redemption campaign with delay and duplicate", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T5387_postRedemptionCampaignWithDelay() throws InterruptedException, ParseException {

		String reward_Code = "";
		String PostRedemptionCampName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
		logger.info("Campaign name is :" + PostRedemptionCampName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Bulking For Post Redemption Campaign", "uncheck");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(PostRedemptionCampName,
				dataSet.get("giftType"), PostRedemptionCampName, dataSet.get("redemable"));

		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setPushNotification(PostRedemptionCampName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setExecutionDelay("5");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Redemption Campaign is not created...");

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 */

		// Send gift to user and redeem from Iframe
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemableName"), dataSet.get("giftReason"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");

		// iFrame Login and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer("$1.0 OFF (Never Expires)");
		// Navigate to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, reward_Code, key, txn,
				dataSet.get("location"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// int redemptionTime =
		// pageObj.guestTimelinePage().getEventTimeRedemptionTimeFromApi(resp);

		// Navigate to timline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(4);

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(PostRedemptionCampName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(PostRedemptionCampName,
				dataSet.get("client"), dataSet.get("secret"), token);

		int diff = pageObj.guestTimelinePage().getTimeDiffPostRedemptionCampaign();

		Assert.assertTrue(diff == 5 | diff == 6 | diff == 7 | diff == 8,
				"Campaign Delayed time did not matched :" + diff);
		Assert.assertTrue(campaignName.equalsIgnoreCase(PostRedemptionCampName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redemable")),
				"reward gifted in account history did not matched");

		pageObj.utils().logPass(
				"Post redemption campaign detail: push notification, campaign name, validated successfully on timeline");

		// search and duplicate classic campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(PostRedemptionCampName);
		String name = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(PostRedemptionCampName, "Edit");
		Assert.assertEquals(name, PostRedemptionCampName + " - copy");
		pageObj.utils().logPass("Campaign name is prefilled as : " + PostRedemptionCampName + " - copy");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		// pageObj.newCamHomePage().activateChallengeCampaign();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Post Checkin Campaign is not created...");

		// delete camapigns
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(PostRedemptionCampName);

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
