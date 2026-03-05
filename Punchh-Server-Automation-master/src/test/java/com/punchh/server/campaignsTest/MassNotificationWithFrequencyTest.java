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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MassNotificationWithFrequencyTest {

	private static Logger logger = LogManager.getLogger(MassNotificationWithFrequencyTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
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

	@Test(description = "SQ-T2664 Mass Notification Campaign With Frequency Daily", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2664_verifyMassNotificationWithFrequencyDaily() throws InterruptedException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			logger.info("Campaign triggered once in timeline for daily frequency as expected.");
			TestListeners.extentTest.get().pass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get().warning(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			logger.info("Campaign triggered twice in timeline for daily frequency as expected.");
			TestListeners.extentTest.get()
					.pass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get()
					.warning("Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass Notification  campaign with frequency daily validated successfully on timeline triggered 3 times");

	}

	@Test(description = "SQ-T2665 Mass Notification Campaign With Frequency Weekly", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2665_verifyMassNotificationWithFrequencyWeekly() throws InterruptedException {
		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequency("Weekly");
		pageObj.signupcampaignpage().setRepeatAndDaysOfWeek("2", "Monday");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			logger.info("Campaign triggered once in timeline for daily frequency as expected.");
			TestListeners.extentTest.get().pass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get().warning(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			logger.info("Campaign triggered twice in timeline for daily frequency as expected.");
			TestListeners.extentTest.get()
					.pass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get()
					.warning("Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass Notification  campaign with frequency daily validated successfully on timeline triggered 3 times");
	}

	@Test(description = "SQ-T2666 Mass Notification Campaign With Frequency Monthly", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2666_verifyMassNotificationWithFrequencyMonthly() throws InterruptedException {
		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		userEmail = dataSet.get("email");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().setFrequency("Monthly");
		pageObj.signupcampaignpage().setRepeatOn("1");
		pageObj.signupcampaignpage().setRepeatAndDaysOfWeek("1", "Monday");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation (move to user timeline)
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// check campaign trigger only once in timeline
		long camTriggerCount = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 1);

		if (camTriggerCount == 1) {
			logger.info("Campaign triggered once in timeline for daily frequency as expected.");
			TestListeners.extentTest.get().pass("Campaign triggered once in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get().warning(
					"Campaign did not trigger once in timeline for daily frequency. Actual count: " + camTriggerCount);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount2 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 2);

		if (camTriggerCount2 == 2) {
			logger.info("Campaign triggered twice in timeline for daily frequency as expected.");
			TestListeners.extentTest.get()
					.pass("Campaign triggered twice in timeline for daily frequency as expected.");
		} else {
			logger.info(
					"Campaign did not trigger twice in timeline for daily frequency. Actual count: " + camTriggerCount);
			TestListeners.extentTest.get()
					.warning("Campaign did not trigger twice in timeline for daily frequency. Actual count: "
							+ camTriggerCount2);
		}

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// check campaign trigger only once in timeline
		long camTriggerCount3 = pageObj.guestTimelinePage().getCampNameTimlineForMassFrequencyCampaign(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token, 3);

		Assert.assertEquals(camTriggerCount3, 3,
				"Campaign did not trigger for three times in timeline for daily frequency");

		TestListeners.extentTest.get().pass(
				"Mass Notification  campaign with frequency daily validated successfully on timeline triggered 3 times");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
