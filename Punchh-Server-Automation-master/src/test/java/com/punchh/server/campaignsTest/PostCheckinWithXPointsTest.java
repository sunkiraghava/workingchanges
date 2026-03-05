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
public class PostCheckinWithXPointsTest {

	private static Logger logger = LogManager.getLogger(PostCheckinWithXPointsTest.class);
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

	@Test(description = "SQ-T2254 Post checkin campaign with gift point 3x and QC", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2254_verifyPostCheckinCampaignWithGiftPoint3xandQC() throws Exception {
		campaignName = CreateDateTime.getUniqueString("Automation PostcheckinQC Campaign");
		logger.info("Campaign name is :" + campaignName);
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
		// create postcheckin campaign with qc and points
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinQCCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("giftPoints"), dataSet.get("pointsType"));

		pageObj.signupcampaignpage().createWhomDetailsPostcheckinQCCampaign(campaignName, dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"), dataSet.get("qcItem"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Post Checkin Campaign with QC and points is not created...");
		 */

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
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"),
				dataSet.get("menuItemid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		 */

		String camnName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		/*
		 * String campaignName =
		 * pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * postCheckinCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 * pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 * System.out.println(Itemdata);
		 */

		Assert.assertTrue(camnName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory.contains("20 Bonus points earned for participating in " + campaignName),
				"Gifted points did not appeared in account history");
		// Assert.assertTrue(Itemdata.get(2).contains("20 points"), "Gifted points are
		// not equal to 3x");
		TestListeners.extentTest.get().pass(
				"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		// user creation using api2 signup api qc unqualifies should not trigger this
		// campaign for guest
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Pos api checkin
		String key1 = CreateDateTime.getTimeDateString();
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp1 = pageObj.endpoints().posCheckinQC(date1, userEmail1, key1, txn1, dataSet.get("locationKey"),
				dataSet.get("menuItemid1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1); boolean
		 * campaignName1 =
		 * pageObj.guestTimelinePage().CheckIfCampaignTriggered(postCheckinCampaignName)
		 * ; pageObj.guestTimelinePage().clickAccountHistory(); List<String> Itemdata1 =
		 * pageObj.accounthistoryPage().getAccountDetailsforBonusPointsEarned();
		 * System.out.println(Itemdata1);
		 */
		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		String rewardGiftedAccountHistory1 = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);

		Assert.assertFalse(campaignName1.equalsIgnoreCase(campaignName),
				"Postcheckin campaign with non qualifing qc should not trigger");
		Assert.assertFalse(
				rewardGiftedAccountHistory1.contains("20 Bonus points earned for participating in " + campaignName),
				"Postcheckin campaign with non qualifing qc trigger details should not appear in account history");
		TestListeners.extentTest.get().pass("Postcheckin campaign with non qualifing qc did not triggered");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		 * pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdFreePunchhCampaignsTable(campaignid, env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
