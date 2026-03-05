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
public class SignupCampaignTest {

	private static Logger logger = LogManager.getLogger(SignupCampaignTest.class);
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

	@Test(description = "SQ-T2233 Sign Up Campaign Trigger For User"
			+ "SQ-T3679 Sign Up Campaign With Delay", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2233_verifySignupCampaignTriggersonUserCreation() throws InterruptedException, ParseException {

		String signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
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
		pageObj.signupcampaignpage().setExecutionDelay("-2");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		String message = pageObj.utils().getSuccessMessage();
		Assert.assertEquals(message, "Execution Delay must be greater than or equal to 0", "Message did not match.");
		pageObj.utils().logPass("Successfully verified error message for negative execution delay");

		// check execution with positive value
		pageObj.signupcampaignpage().setExecutionDelay("2");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user signup using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(2);

		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(signUpCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean rewardedRedeemableStatus = pageObj.guestTimelinePage().verifyrewardedRedeemable();

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus, "Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");

		int diff = pageObj.guestTimelinePage().timeDiffCampTrigger();
		Assert.assertTrue(diff == 2 | diff == 3, "Campaign Delayed time did not matched :" + diff);
		pageObj.utils().logPass("Signup campaign with delay trigger validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().removeSearchedCampaign(signUpCampaignName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
