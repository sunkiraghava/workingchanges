package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class MassNotificationSTOCampaignsTest {

	private static Logger logger = LogManager.getLogger(MassNotificationSTOCampaignsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";
	Utilities utils;

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
		sTCName = method.getName();
		utils = new Utilities(driver);
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

	// not needed in regression suite sto is not supported for except once frequency
	@Test(description = "SQ-T2664 Mass Notification STO Daily Frequency", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2664_verifySTOCampaignWithFrequencyDailySelected() throws InterruptedException, ParseException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		// create campaign
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().clickWhenTab();
		String whenTitle = pageObj.campaignsbetaPage().whenTitle();
		Assert.assertEquals(whenTitle, "When should the campaign be sent?", "What title did not matched");

		pageObj.campaignsbetaPage().setFrequency("Daily");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean flag = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(flag, "Start date not selected");
		pageObj.campaignsbetaPage().setEndDate();
		// boolean
		// flag1=pageObj.campaignsbetaPage().verifyEndDateSelectedOrNot("Select");
		// Assert.assertTrue(flag1 ,"End date not selected");

		boolean status = pageObj.campaignsbetaPage().checkSTOPresence();
		Assert.assertFalse(status, "STO Time Option status did not matched for Daily Frequency ");
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		pageObj.campaignsbetaPage().clickNextBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Daily"),
				"Frequencydid not matched  on summary page");
		softassert.assertTrue(summaryData.get(6).contains("(GMT-12) International Date Line West"),
				"Recommended Time not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
//		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
//		utils.logPass("Campaign created with success message");
	}

	// not needed in regression suite sto is not supported for except once frequency
	@Test(description = "SQ-T2665 Mass Notification STO Weekly Frequency", groups = { "regression", "unstable",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2665_verifySTOCampaignWithFrequencyWeeklySelected() throws InterruptedException, ParseException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		// create campaign
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().clickWhenTab();
		String whenTitle = pageObj.campaignsbetaPage().whenTitle();
		Assert.assertEquals(whenTitle, "When should the campaign be sent?", "What title did not matched");

		pageObj.campaignsbetaPage().setFrequency("Weekly");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setEndDate();
		pageObj.campaignsbetaPage().setRepeat("1");
		pageObj.campaignsbetaPage().setRepeatOn();
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		boolean status = pageObj.campaignsbetaPage().checkSTOPresence();
		Assert.assertFalse(status, "STO Time Option status did not matched for Weekly Frequency ");
		pageObj.campaignsbetaPage().clickNextBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Weekly"),
				"Frequencydid not matched  on summary page");
		softassert.assertTrue(summaryData.get(7).contains("(GMT-12) International Date Line West"),
				"Recommended Time not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Campaign created with success message");
	}

	// not needed in regression suite sto is not supported for except once frequency
	@Test(description = "SQ-T2666 Mass Notification Monthly Frequency", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2666_verifySTOCampaignWithFrequencyMonthlySelected() throws InterruptedException, ParseException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		// create campaign
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().clickWhenTab();
		String whenTitle = pageObj.campaignsbetaPage().whenTitle();
		Assert.assertEquals(whenTitle, "When should the campaign be sent?", "What title did not matched");

		pageObj.campaignsbetaPage().setFrequency("Monthly");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setEndDate();
		pageObj.campaignsbetaPage().setRepeat("1");
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		// pageObj.campaignsbetaPage().setWeekOfMonth("1st Week");
		pageObj.campaignsbetaPage().setDayOfMonth("1st");
		boolean status = pageObj.campaignsbetaPage().checkSTOPresence();
		Assert.assertFalse(status, "STO Time Option status did not matched for Monthly Frequency ");
		pageObj.campaignsbetaPage().clickNextBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Monthly"),
				"Frequencydid not matched  on summary page");
		softassert.assertTrue(summaryData.get(7).contains("(GMT-12) International Date Line West"),
				"Recommended Time not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Campaign created with success message");

	}

// As per Eswara DND support is removed and STO is supported only for once frequency
	/*
	 * @Test(description = "SQ-T2668 STO_DND", groups = "Regression", priority = 4)
	 * Invalid test case public void T2668_verifySTOCampaignWithDND() throws
	 * InterruptedException {
	 * 
	 * String campaignName = "Automation Notification Campaign" +
	 * CreateDateTime.getTimeDateString();
	 * 
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * 
	 * // Click Cockpit and enable STO campaign
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
	 * pageObj.dashboardpage().onEnableSTOCheckbox();
	 * 
	 * // Click Campaigns Link pageObj.menupage().navigateToSubMenuItem("Campaigns",
	 * "Campaigns"); // Select offer dropdown value
	 * pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
	 * 
	 * // validate error msg for blank name field
	 * pageObj.campaignspage().clickNewCampaignBetaBtn();
	 * pageObj.campaignsbetaPage().SetCampaignName(campaignName);
	 * Thread.sleep(2000); pageObj.campaignsbetaPage().clickContinueBtn();
	 * Thread.sleep(2000); utils.switchToWindow();
	 * 
	 * // create campaign pageObj.campaignsbetaPage().setSegmentType("custom");
	 * Thread.sleep(2000); pageObj.campaignsbetaPage().clickNextBtn();
	 * pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
	 * pageObj.campaignsbetaPage().clickNextBtn();
	 * pageObj.campaignsbetaPage().clickWhenTab(); String whenTitle =
	 * pageObj.campaignsbetaPage().whenTitle(); Assert.assertEquals(whenTitle,
	 * "When should the campaign be sent?", "What title did not matched");
	 * 
	 * pageObj.campaignsbetaPage().setFrequency("Monthly");
	 * pageObj.campaignsbetaPage().setStartDate();
	 * pageObj.campaignsbetaPage().setEndDate();
	 * pageObj.campaignsbetaPage().setRepeat("1");
	 * //pageObj.campaignsbetaPage().setRecommendedSendDate();
	 * pageObj.campaignsbetaPage().setTimeZone("Recipients’ Local Time Zone");
	 * //pageObj.campaignsbetaPage().setWeekOfMonth("1st Week");
	 * pageObj.campaignsbetaPage().setDayOfMonth("1st"); // check DND details
	 * pageObj.campaignsbetaPage().viewDNDDetails();
	 * pageObj.campaignsbetaPage().clickNextBtn(); List<String> summaryData =
	 * pageObj.campaignsbetaPage().CheckSummary(); SoftAssert softassert = new
	 * SoftAssert(); softassert.assertTrue(summaryData.get(0).contains("Audience\n"
	 * + "custom"), "Audience type did not matched on summary page");
	 * softassert.assertTrue(summaryData.get(1).contains("Segment size"),
	 * "Segment size did not displayed on summary page");
	 * softassert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Monthly"),
	 * "Frequencydid not matched  on summary page");
	 * softassert.assertTrue(summaryData.get(7).
	 * contains("Recipients’ Local Time Zone"),
	 * "Recommended Time not matched  on summary page"); softassert.assertAll();
	 * TestListeners.extentTest.get().pass("Campaign summary validated");
	 * pageObj.campaignsbetaPage().clickActivateCampaignBtn(); String msg =
	 * pageObj.campaignsbetaPage().validateSuccessMessage();
	 * Assert.assertEquals(msg, "Schedule created successfully.",
	 * "Success message text did not matched");
	 * TestListeners.extentTest.get().pass("Campaign created with success message");
	 * 
	 * 
	 * // Click Cockpit and OFFenable STO campaign
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
	 * pageObj.dashboardpage().offEnableSTOCheckbox(); }
	 */
	// not needed in regression suite sto is not supported for except once frequency
	@Test(description = "SQ-T2667 STO_Blackout Dates", groups = { "Regression", "unstable", "dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T2667_verifySTOBlackoutDates() throws InterruptedException, ParseException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and enable STO campaign
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickCockpitDashboardLink();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();

		// Click Campaigns Link Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		Thread.sleep(4000);
		// Select Blackout date
		pageObj.campaignspage().createBlackoutDate();
		// Click Campaigns Link Select offer dropdown value
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// create campaign
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(4000);
		utils.switchToWindow();

		// Set segment
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom automation");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(9000);
		// utils.refreshPage();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean flag = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(flag, "Start date not selected");
		pageObj.campaignsbetaPage().setRecommendedSendDate();
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		String blackout = pageObj.campaignsbetaPage().validateblackoutDate();
		Assert.assertTrue(blackout.contains("Test Blackout"), "Blackout date did not appeared");
		pageObj.campaignsbetaPage().clickNextBtn();
		utils.logPass("Campaign blackout date validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Campaign created with balckout date and success message :" + blackout);

	}

	// not needed in regression suite
	@Test(description = "SQ-T2660 Include Surveys and Advertised Campaign", groups = { "regression",
			"dailyrun" }, priority = 5)
	@Owner(name = "Amit Kumar")
	public void T2660_verifyIncludeSurveysAndAdvertisedCampaign() throws InterruptedException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and OFFenable STO campaign
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickCockpitDashboardLink();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().offEnableSTOCheckbox();

		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(1000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		// set segement
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().clickNextBtn();

		// select servey from drpdown
		pageObj.campaignsbetaPage().clickIncludeSurvey();
		pageObj.campaignsbetaPage().setServey("Checkin Survey");
		pageObj.campaignsbetaPage().clickIncludeSurvey();

		// seletc campaign name from drp
		pageObj.campaignsbetaPage().clickIncludeAdvertisedCampaign();
		pageObj.campaignsbetaPage().setAdvertisedCampaign("post checkin camp QC");

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
