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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SignupCampaignWithSegment {

	private static Logger logger = LogManager.getLogger(SignupCampaignWithSegment.class);
	public WebDriver driver;
	private Properties prop;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String signUpCampaignName;
	// private String campaignid;

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

	// Commenting this case - covering this under SQ-T6417
//	@Test(description = "SQ-T5990 SignUp campaign with segment and user comes through WebEmail", groups = {
//			"regression", "dailyrun", "dailyrun" }, priority = 0)
	public void T5990_verifySignupCampaignWithSegmentAndUserComesThroughWebEmail() throws Exception {

		signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
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

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(signUpCampaignName);
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(
				signUpCampaignName + " " + dataSet.get("pushNotification"), dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationText();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus.contains(signUpCampaignName),
				"Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

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
