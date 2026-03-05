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
public class MassOfferSTOCampaignsTest {

	private static Logger logger = LogManager.getLogger(MassOfferSTOCampaignsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";
	Utilities utils;
	CreateDateTime createDateTime;

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		createDateTime = new CreateDateTime();
	}

	// not needed in regression suite sto is not supported except once frequency
	@Test(description = "SQ-T2664 Mass Offer STO Daily Frequency", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2664_verifyMassOfferSTOCampaignWithFrequencyDailySelected()
			throws InterruptedException, ParseException {

		campaignName = "AutomationMassOfferCampaign" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " campaign name is created");
		// Login to instance
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
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		// pageObj.campaignsbetaPage().setPushNotification("Automation Push
		// Notification", "Automation Push Notification");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setFrequency("Daily");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setEndDate();

		boolean status = pageObj.campaignsbetaPage().checkSTOPresence();
		Assert.assertFalse(status, "STO Time Option status did not matched for Daily Frequency ");
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		pageObj.campaignsbetaPage().clickNextBtn();

		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom automation"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(3).contains("Redeemable\n" + "Redeemable - 1000"),
				"Redeemable did not matched  on summary page");
		softassert.assertTrue(summaryData.get(5).contains("Frequency\n" + "Daily"),
				"Frequency did not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Mass offer Campaign created with success message");
		// delete or deactivate campaign to be aadded
	}

	// not needed in regression suite sto is not supported except once frequency
	@Test(description = "SQ-T2665 Mass Offer STO Weekly Frequency", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2665_verifyMassOfferSTOCampaignWithFrequencyWeeklySelected()
			throws InterruptedException, ParseException {

		campaignName = "AutomationMassOfferCampaign" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " campaign name is created");
		// Login to instance
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
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		// pageObj.campaignsbetaPage().setPushNotification("Automation Push
		// Notification", "Automation Push Notification");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setFrequency("Weekly");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean flag = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(flag, "Start date not selected");
		pageObj.campaignsbetaPage().setEndDate();
		boolean flag1 = pageObj.campaignsbetaPage().verifyEndDateSelectedOrNot("Select");
		Assert.assertTrue(flag1, "End date not selected");
		pageObj.campaignsbetaPage().setRepeat("1");
		pageObj.campaignsbetaPage().setRepeatOn();
		pageObj.campaignsbetaPage().setTimeZone("(GMT-12) International Date Line West");
		boolean status = pageObj.campaignsbetaPage().checkSTOPresence();
		Assert.assertFalse(status, "STO Time Option status did not matched for Weekly Frequency ");
		pageObj.campaignsbetaPage().clickNextBtn();

		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom automation"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(3).contains("Redeemable\n" + "Redeemable - 1000"),
				"Redeemable did not matched  on summary page");
		softassert.assertTrue(summaryData.get(5).contains("Frequency\n" + "Weekly"),
				"Frequency did not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Mass offer Campaign created with success message");
		// delete or deactivate campaign to be aadded

	}

	// not needed in regression suite sto is not supported except once frequency
	@Test(description = "SQ-T2666 Mass Offer STO Monthly Frequency", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2666_verifyMassOfferSTOCampaignWithFrequencyMonthlySelected()
			throws InterruptedException, ParseException {
		campaignName = "AutomationMassOfferCampaign" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		pageObj.campaignsbetaPage().clickContinueBtn();
		utils.switchToWindow();

		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		// pageObj.campaignsbetaPage().setPushNotification("Automation Push
		// Notification", "Automation Push Notification");
		pageObj.campaignsbetaPage().clickNextBtn();
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
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom automation"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(3).contains("Redeemable\n" + "Redeemable - 1000"),
				"Redeemable did not matched  on summary page");
		softassert.assertTrue(summaryData.get(5).contains("Frequency\n" + "Monthly"),
				"Frequency did not matched  on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Mass offer Campaign created with success message");

	}
	
	// Rakhi
	// Campaign Set feature will not be available for Split Test campaign from
	// September release
	/*
	 * @Test(description =
	 * "SQ-T6219 Verify campaign set with split mass offer campaigns", groups = {
	 * "regression" })
	 * 
	 * public void T6219_VerifyCampaignSetWithSplitMassOfferCampaign() throws
	 * Exception {
	 * 
	 * String massCampaignName1 = "AutomationMassOfferOne" +
	 * CreateDateTime.getTimeDateString(); String massCampaignName2 =
	 * "AutomationMassOfferTwo" + CreateDateTime.getTimeDateString(); String
	 * userEmail = dataSet.get("userEmail1");
	 * 
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
	 * pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
	 * "business_enable_workflow", "uncheck", true);
	 * 
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
	 * pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
	 * "business_enable_split_testing", "check", true);
	 * pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(
	 * "business_enable_campaign_set", "check", true);
	 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
	 * pageObj.newCamHomePage().clickSwitchToClassicBtn();
	 * 
	 * String[] campaignNames = { massCampaignName1, massCampaignName2 };
	 * 
	 * for (int i = 0; i < campaignNames.length; i++) { String campaignName =
	 * campaignNames[i]; // Select offer dropdown value
	 * pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
	 * pageObj.campaignspage().clickNewCampaignBtn();
	 * 
	 * // set campaign name and gift type
	 * pageObj.signupcampaignpage().setCampaignName(campaignName);
	 * pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
	 * pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
	 * pageObj.signupcampaignpage().clickNextBtn();
	 * 
	 * // select segment
	 * pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
	 * pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
	 * 
	 * // Click on split button pageObj.campaignsplitpage().clickOnSplitButton();
	 * 
	 * // enter segment percentage
	 * pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.
	 * get("varApercent"), dataSet.get("varBpercent"),
	 * dataSet.get("controlgrouppercent")); // Click on save button
	 * pageObj.campaignsplitpage().clickOnNextButton();
	 * 
	 * // Add redeemable in B variant
	 * pageObj.campaignsplitpage().selectOfferOfVarB();
	 * pageObj.campaignsplitpage().enterRedeemableNameVarBOfferField(dataSet.get(
	 * "redeemableNameB")); pageObj.campaignsplitpage().clickOnNextButton();
	 * pageObj.campaignsplitpage().clickOnSaveButton();
	 * utils.longWaitInMiliSeconds(3);
	 * pageObj.signupcampaignpage().clickNextButton(); logger.info(campaignName +
	 * " campaign is created"); // keep the campaign in draft state
	 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // verify
	 * campaign status String camstatus =
	 * pageObj.campaignsbetaPage().getCampaignStatus();
	 * Assert.assertEquals(camstatus, "Draft", "massCampaignName" + i +
	 * " status is not Draft"); logger.info("massCampaignName" + i +
	 * " status is Draft"); TestListeners.extentTest.get().pass("massCampaignName" +
	 * i + " status is Draft");
	 * 
	 * } // Create campaign set
	 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaign Sets");
	 * pageObj.campaignsetPage().clickNewCampaignSetBtn();
	 * pageObj.campaignsetPage().setCampaignName("Split Campaign Set" +
	 * CreateDateTime.getTimeDateString());
	 * pageObj.campaignsetPage().selectCampaignOne(massCampaignName1);
	 * pageObj.campaignsetPage().selectCampaignTwo(massCampaignName2);
	 * pageObj.campaignsetPage().clickSaveandPreviewBtn(); // Set start time and
	 * time zone pageObj.signupcampaignpage().setStartTime();
	 * pageObj.signupcampaignpage().setTimeZone("Delhi");
	 * 
	 * // verify first campaign status should be changed from Draft to Schedule
	 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
	 * pageObj.campaignspage().searchCampaign(massCampaignName1); String camstatus =
	 * pageObj.campaignsbetaPage().getCampaignStatus();
	 * Assert.assertEquals(camstatus, "Scheduled", massCampaignName1 +
	 * " status is not changed to Schedule"); logger.info(massCampaignName1 +
	 * " status is changed to Schedule");
	 * TestListeners.extentTest.get().pass(massCampaignName1 +
	 * " status is changed to Schedule");
	 * 
	 * // run first mass split campaign
	 * pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
	 * pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
	 * pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName1);
	 * pageObj.guestTimelinePage().pingSessionforLongWait(3); // validate campaign
	 * status pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
	 * pageObj.newCamHomePage().clickNewCamHomePageBtn(); String camStatus =
	 * pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(
	 * massCampaignName1, "Processed"); Assert.assertEquals(camStatus, "Processed",
	 * "Camapign status is not processed"); TestListeners.extentTest.get().
	 * pass("Mass offer campaign processed successfully");
	 * 
	 * // Timeline validation for first campaign Response loginResponse =
	 * pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"),
	 * dataSet.get("secret")); Assert.assertEquals(loginResponse.getStatusCode(),
	 * 200); String token =
	 * loginResponse.jsonPath().get("access_token.token").toString(); // fectch
	 * campaign name and reward gifted in account history user email1 String
	 * rewardGiftedAccountHistory =
	 * pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName1,
	 * dataSet.get("client"), dataSet.get("secret"), token);
	 * Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName1),
	 * "reward gifted in account history did not matched");
	 * logger.info("Validated reward gifted in account history for campaign: " +
	 * massCampaignName1); TestListeners.extentTest.get()
	 * .pass("Validated reward gifted in account history for campaign: " +
	 * massCampaignName1);
	 * 
	 * pageObj.guestTimelinePage().pingSessionforLongWait(4); // search and get
	 * campaign id pageObj.menupage().navigateToSubMenuItem("Campaigns",
	 * "Campaigns");
	 * 
	 * // SideKiq scheduled Jobs running for second campaign //
	 * pageObj.schedulespage().movetoSidekiq(baseUrl); // int count =
	 * pageObj.sidekiqPage() // .checkSidekiqJobWithId(baseUrl,
	 * "NotifierBatchWorker",campaignID2); // Assert.assertTrue(count == 1,
	 * "Job did not created in sidekiq for " + massCampaignName2); //
	 * logger.info("Job created in sidekiq for " + massCampaignName2); //
	 * TestListeners.extentTest.get().pass("Job created in sidekiq for " +
	 * massCampaignName2);
	 * 
	 * // timeline validation for second campaign String rewardGiftedAccountHistory1
	 * = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName2,
	 * dataSet.get("client"), dataSet.get("secret"), token);
	 * Assert.assertTrue(rewardGiftedAccountHistory1.contains(massCampaignName2),
	 * "reward gifted in account history did not matched");
	 * logger.info("Validated reward gifted in account history for campaign: " +
	 * massCampaignName2); TestListeners.extentTest.get()
	 * .pass("Validated reward gifted in account history for campaign: " +
	 * massCampaignName2);
	 * 
	 * }
	 */

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
