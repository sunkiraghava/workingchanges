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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class E2EPostCheckinCampaignWithSegmentsTest {

	private static Logger logger = LogManager.getLogger(E2EPostCheckinCampaignWithSegmentsTest.class);
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

//Amit
	// do not delete post checkin cams
	@Test(description = "SQ-T4506 Post Checkin Campaign with Checkin Type Segment", groups = { "Sanity", "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T4506_verifyPostCheckinCampaignWithCheckinTypeSegment() throws InterruptedException {

		String campName = "PostCheckinCampaign Segment CheckinType do not delete";
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
		pageObj.newCamHomePage().searchCampaign(campName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}

		/*
		 * // navigate to Guests -> Segments >> update segment name
		 * pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		 * pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get(
		 * "segmentname")); int guestInSegmentCount =
		 * pageObj.segmentsBetaPage().getGuestInSegmentCount(); userEmail =
		 * pageObj.segmentsBetaPage().getSegmentGuset();
		 */

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response checkinResp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(campName), "Campaign name did not matched");
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

	@Test(description = "SQ-T4554 Post Checkin Campaign with Average checkin type Segment", groups = { "Sanity",
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T4554_verifyPostCheckinCampaignWithAverageCheckinTypeSegment() throws InterruptedException {

		String campName = "PostCheckinCampaign Segment AverageCheckinType do not delete";
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
		pageObj.newCamHomePage().searchCampaign(campName);
		String camstatus = pageObj.newCamHomePage().getCampaignStatus();
		if (camstatus.equalsIgnoreCase("Inactive")) {
			String activeStatus = pageObj.newCamHomePage().activateCampaign();
			Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		}

		/*
		 * // navigate to Guests -> Segments >> update segment name
		 * pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		 * pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get(
		 * "segmentname")); int guestInSegmentCount =
		 * pageObj.segmentsBetaPage().getGuestInSegmentCount(); userEmail =
		 * pageObj.segmentsBetaPage().getSegmentGuset();
		 */

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmailWithDomainPartech();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response checkinResp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, checkinResp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(campName), "Campaign name did not matched");
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

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
