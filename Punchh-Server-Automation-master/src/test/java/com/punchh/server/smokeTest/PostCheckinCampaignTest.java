package com.punchh.server.smokeTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostCheckinCampaignTest {

	private static Logger logger = LogManager.getLogger(PostCheckinCampaignTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Map<String, String> dataSet;
	private String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2187 Post Checkin Campaign Trigger For User"
			+ "SQ-T3680 Post Checkin Campaign With Delay", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2187_verifyPostCheckinCampaignTriggerForUserAfterCheckin()
			throws InterruptedException, ParseException {
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setExecutionDelay("2");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, iFrameEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Timeline validation
		pageObj.guestTimelinePage().pingSessionforLongWait(2);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(postCheckinCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();

		Assert.assertTrue(campaignName.equalsIgnoreCase(postCheckinCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		TestListeners.extentTest.get().pass(
				"Postcheckin campaign detail: push notification, campaign name, reward notification validated successfully on timeline");

		int diff = pageObj.guestTimelinePage().timeDiffCampTrigger();
		Assert.assertTrue(diff == 2 | diff == 3, "Campaign Delayed time did not matched :" + diff);
		TestListeners.extentTest.get()
				.pass("post checkin campaign with delay trigger validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
