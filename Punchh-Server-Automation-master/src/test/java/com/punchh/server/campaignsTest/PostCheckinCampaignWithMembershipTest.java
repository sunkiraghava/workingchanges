package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class PostCheckinCampaignWithMembershipTest {
	private static Logger logger = LogManager.getLogger(PostCheckinCampaignWithMembershipTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private Properties prop;
	private String bronzeUserEmail, silverUserEmail;
	private String bronzeCamName, silverCamName;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2922 Verify Guest membership tier reward with Postcheckin Campaign", groups = "Regression", priority = 0)
    @Owner(name = "Amit Kumar")
	public void T2922_verifyPostCheckinCampaignWithRewardMembershipLevel_PartOne() throws InterruptedException {
		// create bronze campaign and bronze user
		bronzeCamName = CreateDateTime.getUniqueString("Automation Postcheckin Bronze");

		// user creation using pos api for bronze membership
		bronzeUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(bronzeUserEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift button and gift points to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(bronzeUserEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		// boolean status = pageObj.campaignspage().validateSuccessMessage();
		// Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// validate guest membership bronze
		String membershipName = pageObj.guestTimelinePage().verifyGuestmembershipLabel();
		Assert.assertTrue(membershipName.contains(dataSet.get("segmentName")), "Membership level did not matched");
		pageObj.utils().logPass("Geust membership level validated as :" + membershipName);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(bronzeCamName, dataSet.get("camGiftType"),
				dataSet.get("giftReason"), dataSet.get("redemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean camstatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(camstatus, "Post Checkin Campaign is not created...");
		pageObj.utils().logPass("Postcheckin campaign created successfully :" + bronzeCamName);
	}

	@Test(description = "SQ-T2922 Verify Guest membership tier reward with Postcheckin Campaign", groups = "Regression", priority = 1)
    @Owner(name = "Amit Kumar")
	public void T2922_verifyPostCheckinCampaignWithRewardMembershipLevel_PartTwo() throws InterruptedException {
		// create silver campaign and sliver user
		silverCamName = CreateDateTime.getUniqueString("Automation Postcheckin Silver");

		// user creation using pos api for bronze membership
		silverUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(silverUserEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift button and gift points to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(silverUserEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// validate guest membership silver
		String membershipName = pageObj.guestTimelinePage().verifyGuestmembershipLabel();
		Assert.assertTrue(membershipName.contains(dataSet.get("segmentName")), "Membership level did not matched");
		pageObj.utils().logPass("Guest membership level validated as : Silver Guest");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(silverCamName, dataSet.get("camGiftType"),
				dataSet.get("giftReason"), dataSet.get("redemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean camstatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(camstatus, "Post Checkin Campaign is not created...");
		pageObj.utils().logPass("Postcheckin campaign created successfully :" + silverCamName);
	}

	@Test(description = "SQ-T2922 Verify Guest membership tier reward with Postcheckin Campaign", groups = "Regression", priority = 2,dependsOnMethods = {
	        "T2922_verifyPostCheckinCampaignWithRewardMembershipLevel_PartOne",
	        "T2922_verifyPostCheckinCampaignWithRewardMembershipLevel_PartTwo"})
    @Owner(name = "Amit Kumar")
	public void T2922_verifyPostCheckinCampaignWithRewardMembershipLevel_PartThree() throws Exception {

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, bronzeUserEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(bronzeUserEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(bronzeCamName);
		boolean silverCam = pageObj.guestTimelinePage().CheckIfCampaignTriggered(silverCamName);
		String camReward = pageObj.guestTimelinePage().verifyCampaignRewardName();

		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignName.equalsIgnoreCase(bronzeCamName), "Campaign name did not matched");
		softassert.assertFalse(silverCam, "Other postcheckin campaign has also triggered");
		softassert.assertTrue(camReward.contains(dataSet.get("redemable")),
				"Campaign gifted redeemable name did not matched");
		softassert.assertAll();
		pageObj.utils().logPass("Postcheckin campaign validated successfully on timeline :" + campaignName + "," + camReward);
		pageObj.utils().logPass("Other post checkin campaign did not found on timelin.....");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(bronzeCamName);
		 * pageObj.campaignspage().removeSearchedCampaign(silverCamName);
		 */
		// pageObj.utils().deleteCampaignFromDb(bronzeCamName, env);
		// pageObj.utils().deleteCampaignFromDb(silverCamName, env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() throws Exception {
		// these will be deleted after all test methods run
		pageObj.utils().deleteCampaignFromDb(bronzeCamName, env);
		pageObj.utils().deleteCampaignFromDb(silverCamName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}
