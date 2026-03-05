package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestResult;
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
public class SignupReferralCampaignTest {

	private static Logger logger = LogManager.getLogger(SignupReferralCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String referralCampaignName;
	// private String campaignid;
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
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2184 Sign up with invite code/referral campaign]Sign up a user with invite code and check referral campaign", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T2184_userSignupWithReferralCampaign() throws InterruptedException {
		TestListeners.extentTest.get().info(sTCName + " ==>" + dataSet);
		referralCampaignName = CreateDateTime.getUniqueString("Automation ReferralCampaign");
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
		pageObj.signupcampaignpage().createWhatDetailsReferralCampaign(referralCampaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("giftAmount"));
		pageObj.signupcampaignpage().createWhomDetailsReferralCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign1();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status, "Referral Campaign is not created...");
		 */

		// create New User
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		String referralCode = pageObj.guestTimelinePage().getReferralCode();

		String referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"), dataSet.get("secret"),
				referralCode);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(referralCode);

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, response2.getStatusCode(), "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);

		// Timeline validation
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(referralCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignName.equalsIgnoreCase(referralCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOngoingdrpValue("Referral");
		 * pageObj.campaignspage().removeSearchedCampaign(referralCampaignName);
		 */
	}

	@Test(description = "SQ-T5879 Verify gifting from referral campaign when Referral wait for replica is enabled and campaign has non-replica segment in derived reward "
			+ "global flag on", groups = {"nonNightly"}, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T5879_VerigyGiftingFromReferralCampaignWithNonReplicaSegment() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// verify that "Referral wait for replica" is enabled in global configuration
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Referral wait for replica");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		referralCampaignName = CreateDateTime.getUniqueString("Automation ReferralCampaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create referral campaign with derived reward
		pageObj.signupcampaignpage().setCampaignName(referralCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(referralCampaignName);
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("dynamicReward"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsReferralCampaign(referralCampaignName,
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign1();

		// new user signup with referral code
		String referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("inviteCode"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(dataSet.get("inviteCode"));

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("userEmail"));

		// Timeline validation
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(referralCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignName != null && campaignName.equalsIgnoreCase(referralCampaignName),
				"Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass(
				"Referral campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// validate derived reward in guest timeline
		pageObj.guestTimelinePage().clickAccountHistory();
		String expectedItemGifted_Message = "$2.0 OFF (" + referralCampaignName + ")";
		boolean rewardStatus = pageObj.accounthistoryPage().getAccountDetailsforRewardEarned(expectedItemGifted_Message,
				"Item");
		Assert.assertTrue(rewardStatus, "Gifted points did not appeared in account history");
		utils.logPass("Gifted reward validated successfully in account history");

		// remove referral campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOngoingdrpValue("Referral");
		 * pageObj.campaignspage().removeSearchedCampaign(referralCampaignName);
		 */

	}

	// Rakhi
	@Test(description = "SQ-T5911 Verify gifting from referral campaign when Referral wait for replica is enabled and campaign has replica segment in derived reward", 
			groups = {"nonNightly"}, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T5911_VerigyGiftingFromReferralCampaignWithReplicaSegment() throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// verify that "Referral wait for replica" is enabled in global configuration
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Referral wait for replica");
		// verify Referral replica wait minutes
		pageObj.dashboardpage().checkSegmentZipcodeSize("referral_replica_wait_minutes", "1");

		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		referralCampaignName = CreateDateTime.getUniqueString("Automation ReferralCampaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create referral campaign with derived reward
		pageObj.signupcampaignpage().setCampaignName(referralCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(referralCampaignName);
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("dynamicReward"));
		pageObj.signupcampaignpage().clickNextBtn();
		String campaignId = pageObj.signupcampaignpage().getCampaignid();

		pageObj.signupcampaignpage().createWhomDetailsReferralCampaign(referralCampaignName,
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign1();

		// new user signup with referral code
		String referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("inviteCode"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(dataSet.get("inviteCode"));

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");

		// verify after successfully processing AutomaticReferralWorker it should create
		// another AutomaticReferralReplicaWorker
		int count = pageObj.sidekiqPage().checkSidekiqJobWithId(baseUrl, "AutomaticReferralReplicaWorker", campaignId);
		Assert.assertTrue(count == 1, "AutomaticReferralReplicaWorker count did not matched");
		utils.logPass("AutomaticReferralReplicaWorker count matched ie : " + count);
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("userEmail"));

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(dataSet.get("userEmail"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// validate gifted derived reward through Api
		String expectedItemGifted_Message = "Silver $10 Off (" + referralCampaignName + ")";

		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		String campName1 = pageObj.guestTimelinePage().getcampaignNameWithWait(referralCampaignName);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(referralCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(campName1, referralCampaignName,
				"Referral campaign did not trigger for user : " + dataSet.get("userEmail"));
		Assert.assertTrue(rewardGiftedAccountHistory.contains(expectedItemGifted_Message),
				"User did not get reward from referral campaign");
		logger.info("Gifted reward validated successfully in account history");
		TestListeners.extentTest.get().pass("Gifted reward validated successfully in account history");

	}
	
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(referralCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		try {
			pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
			pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Referral wait for replica", "uncheck");
			logger.info("Referral wait for replica flag unchecked after tests");
		} catch (Exception e) {
			logger.error("Failed during navigation/flag update in tearDown: ", e);
		} finally {
			driver.quit();
			logger.info("Browser closed");
		}
	}
}
