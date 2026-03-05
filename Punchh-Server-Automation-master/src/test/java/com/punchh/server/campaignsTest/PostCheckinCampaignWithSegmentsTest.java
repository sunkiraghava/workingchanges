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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostCheckinCampaignWithSegmentsTest {

	private static Logger logger = LogManager.getLogger(PostCheckinCampaignWithSegmentsTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String postCheckinCampaignName;
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

//Amit
	@Test(description = "SQ-T4261 Post Checkin Campaign Trigger For Segment User", groups = "Sanity", dataProvider = "TestDataProvider", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T4261_verifyPostCheckinCampaignTriggerForSegmentUser(String segmentName, String guestEmail)
			throws InterruptedException {
		postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
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
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// user creation using pos signup api
		/*
		 * iFrameEmail = pageObj.iframeSingUpPage().generateEmail(); Response respo =
		 * pageObj.endpoints().posSignUp(guestEmail,locationkey);
		 * Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for pos signup api");
		 */

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, guestEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(guestEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * postCheckinCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification(); boolean
		 * pushNotificationStatus =
		 * pageObj.guestTimelinePage().verifyPushNotificationPostCheckin(); String
		 * giftedItemName =
		 * pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();
		 */

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(guestEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postCheckinCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(postCheckinCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName != null && campaignName.equalsIgnoreCase(postCheckinCampaignName),
				"Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(dataSet.get("redeemable")),
				"Gifted item name did not matched");
		pageObj.utils().logPass(
				"Postcheckin campaign with segment user: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value Thread.sleep(2000);
		 * pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		 * pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);
		 */

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				// {"segmentName","guestEmail"},
				{ "ProfileDetailsBirthdaySpecificDate", "autoiframe18040911112022fvqfju@punchh.com" },
				{ "CheckinsChannelEqualtoOnlineOrder", "autoapi17540704022022@punchh.com" }, };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(postCheckinCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
