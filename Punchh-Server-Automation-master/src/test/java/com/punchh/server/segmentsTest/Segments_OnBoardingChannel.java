package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class Segments_OnBoardingChannel {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

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

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SB-T828 verifying Googlepass in segment and run with mass campaign", priority = 0)
	@Owner(name = "Sachin Bakshi")
	public void T828_verifyGooglePassSegment() throws Exception {
		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");
		pageObj.newSegmentHomePage().clickOnCreateSegmentButton();
		utils.switchToWindow();

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_Channel_GooglePass_");
		pageObj.newSegmentHomePage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Onboarding Channel");
		pageObj.segmentsBetaPage().setOperatorText("Onboarding channel", "GooglePass");
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		String segmentGuest = pageObj.segmentsBetaPage().getSegmentGuest();

		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(segmentGuest, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for User Insegment");
		utils.logPass("Verified that user " + segmentGuest + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logPass("Verified that status of  " + segmentGuest + " is present in Segment");

		String massCampaignName = "AutomationMassOffer_Channel_GooglePass_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not matched");
		utils.logPass("Mass Campaign scheduled successfully : " + massCampaignName);

		// run mass offer
		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// User_Timeline_validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(segmentGuest);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + massCampaignName);

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Push Notification Status is :- " + pushNotificationStatus);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}