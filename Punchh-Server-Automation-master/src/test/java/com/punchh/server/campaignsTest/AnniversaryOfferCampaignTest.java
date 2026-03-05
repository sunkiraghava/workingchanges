package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AnniversaryOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(AnniversaryOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String campaignName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T2548 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With Reference Date Only || "
			+ "SQ-T6580 Create All signup segment and use with trigger based campaigns", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2548_verifyAnniversaryCampaignLifespanDurationReferenceDateOnly() throws InterruptedException {

		// Enable advance anniversary campaign scheduling - should be off in dashboard
		// >> misc. , busineeess must be live

		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Business Live Now?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable advance anniversary campaign scheduling", "uncheck");

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String anniversaryDate = CreateDateTime.getAnniversaryDate();
		Response signUpResponse = pageObj.endpoints().Api2SignUpwithAnniversary(userEmail, dataSet.get("client"),
				dataSet.get("secret"), anniversaryDate);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setRedeemable("$5 Off");
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().setReferenceDateGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("Reference Date Only");
		// pageObj.signupcampaignpage().setExpeiryDateAnniversaryCampaign();
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// Run Segment User Cache Schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Segment User Cache Schedule");
		pageObj.schedulespage().runSchedule();

		// run Schedule for anniversary camp
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().runSchedule();

		// user creation using pos signup api
		/*
		 * userEmail = pageObj.iframeSingUpPage().generateEmail(); Response respo =
		 * pageObj.endpoints().posSignUpwithAnniversary(userEmail,
		 * dataSet.get("locationKey")); Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for pos signup api");
		 */

		Thread.sleep(10000);

		// Timeline validation
		String anniversaryCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * anniversaryCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 */

		Assert.assertTrue(anniversaryCampaignName != null && anniversaryCampaignName.equalsIgnoreCase(campaignName),
				"Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $5 Off (" + campaignName + ")"),
				"Gifted currency by campaign did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Anniversary campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(anniversaryCampaignName);
		 */

		// Deactivate a User
		Response deactivateUserresponse1 = pageObj.endpoints().deactivateUser(userID, dataSet.get("apiKey"));
		Assert.assertEquals(deactivateUserresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate a User");
		logger.info("PLATFORM FUNCTIONS API Deactivate a User is successful, user name " + userEmail);
		TestListeners.extentTest.get()
				.pass("PLATFORM FUNCTIONS API Deactivate a User is successful, user name " + userEmail);

		// Delete a User
		Response deleteUserResponse = pageObj.endpoints().deleteUser(userID, dataSet.get("apiKey"));
		Assert.assertEquals(deleteUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Delete a User");
		logger.info("PLATFORM FUNCTIONS API Delete a User is successful, user name " + userEmail);
		TestListeners.extentTest.get()
				.pass("PLATFORM FUNCTIONS API Delete a User is successful, user name " + userEmail);

	}

	@Test(description = "SQ-T2548 Verify Anniversary_AnniversaryCampaign_Lifespan Duration With N Days from rewarding", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2548_verifyAnniversaryCampaignLifespanDurationNDaysFromRewarding() throws InterruptedException {
		// Enable advance anniversary campaign scheduling - should be off in dashboard
		// >> misc. , busineeess must be live

		campaignName = "Automation Anniversary Campaign" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Business Live Now?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable advance anniversary campaign scheduling", "uncheck");

		// Click Campaigns Link
		// Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOngoingdrpValue("Anniversary");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(campaignName);
		pageObj.signupcampaignpage().setRedeemable("$5 Off");
		pageObj.signupcampaignpage().clickNextBtn();
		// code for whom details
		pageObj.signupcampaignpage().setReferenceDateGuest("Anniversary");
		pageObj.signupcampaignpage().setLifespanDuration("N Days from rewarding");
		pageObj.signupcampaignpage().setDaysBefore("7");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user creation using pos signup api
		/*
		 * userEmail = pageObj.iframeSingUpPage().generateEmail(); Response respo =
		 * pageObj.endpoints().posSignUpwithAnniversaryForNrewardingDays(userEmail,
		 * dataSet.get("locationKey")); Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for pos signup api");
		 */

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// AnniversaryForNrewardingDays
		String futureAnniversaryDate = CreateDateTime.getFutureDateTime(7);
		Response signUpResponse = pageObj.endpoints().Api2SignUpwithAnniversary(userEmail, dataSet.get("client"),
				dataSet.get("secret"), futureAnniversaryDate);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Timeline validation
		String anniversaryCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * anniversaryCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 */

		Assert.assertTrue(anniversaryCampaignName != null && anniversaryCampaignName.equalsIgnoreCase(campaignName),
				"Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains("You were gifted: $5 Off (" + campaignName + ")"),
				"Gifted currency by campaign did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Anniversary campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(anniversaryCampaignName);
		 */

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}