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
public class DNDE2ESignUpCampaignWithSegmentsTest {

	private static Logger logger = LogManager.getLogger(DNDE2ESignUpCampaignWithSegmentsTest.class);
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
	Utilities utils;

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4426 Signup Using Guest Profile Based Test CS Team Classic", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T4426_verifySignupUsingGuestProfileBasedTestCSTeamClassic() throws InterruptedException {

		String signUpCampaignName = "Signup Guest profile CS-team do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@Test(description = "SQ-T4427 Signup Using Guest Profile Based Test CS Team Beta", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T4427_verifySignupUsingGuestProfileBasedTestCSTeamBeta() throws InterruptedException {

		String signUpCampaignName = "Signup Guest profile CS-team Beta do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.equalsIgnoreCase("Rewarded $2.0 OFF"),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@Test(description = "SQ-T4456 Signup campaign with all signed up classic", groups = { "Sanity", "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T4456_verifySignupCampaignWithAllSignedUpClassic() throws InterruptedException {

		String signUpCampaignName = "Signup campaign with all signed up do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// pageObj.guestTimelinePage().checkGifting(signUpCampaignName, "base
		// redeemable",
		// dataSet.get("pn"));
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		utils.logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@Test(description = "SQ-T4457 Signup campaign with all signup beta", groups = { "Sanity", "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T4457_verifySignupCampaignWithAllSignedUpBeta() throws InterruptedException {

		String signUpCampaignName = "Signup campaign with all signup beta do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}
		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// pageObj.guestTimelinePage().checkGifting(signUpCampaignName,
		// dataSet.get("redeemable"),
		// dataSet.get("pn"));
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");

		utils.logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@Test(description = "SQ-T4722 Signup Campaign with Average Checkin Type Segment", groups = { "Sanity", "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T4722_verifySignupCampaignWithAverageCheckinTypeSegment() throws InterruptedException {

		String signUpCampaignName = "SignupCampaign Segment AverageCheckinType do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response checkinResp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");

		pageObj.guestTimelinePage().clickAccountHistory();
		// List<String> Itemdata =
		// pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(signUpCampaignName);
		logger.info(Itemdata);
		Assert.assertTrue(Itemdata.contains("You were gifted: $2.0 OFF (" + signUpCampaignName + ")"),
				"Gifted reward by campaign did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@Test(description = "SQ-T4723 Signup Campaign with Checkin Type Segment", groups = { "Sanity", "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T4723_verifySignupCampaignWithCheckinTypeSegment() throws InterruptedException {

		String signUpCampaignName = "SignupCampaign Segment CheckinType do not delete";
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for pos signup api");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response checkinResp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");

		pageObj.guestTimelinePage().clickAccountHistory();
		// List<String> Itemdata =
		// pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(signUpCampaignName);
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.contains("You were gifted: $2.0 OFF (" + signUpCampaignName + ")"),
				"Gifted reward by campaign did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
