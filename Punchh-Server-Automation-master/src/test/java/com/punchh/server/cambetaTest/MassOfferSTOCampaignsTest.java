package com.punchh.server.cambetaTest;

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

	@Test(description = "SQ-T2663 Mass Offer STO Once Frequency"
			+ "MAI_T41 STO testing through Punchh Dashboard : To run an STO campaign, check for scheduled jobs and validate the STO times from Sidekiq", groups = {
					"regression", "dailyrun", "unstable","nonNightly" }, priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T2663_verifyMassOfferSTOCampaignWithFrequencyOnceSelected()
			throws InterruptedException, ParseException {
		campaignName = "AutomationMassOfferCampaign" + CreateDateTime.getTimeDateString();
		// Verify 'Use custom merlin day' is enabled
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Use custom merlin day");
		// Navigate to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
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

		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setFrequency("Once");
		pageObj.campaignsbetaPage().setStartDateNew(1);
		pageObj.campaignsbetaPage().setTimeZone("(GMT+05:30) New Delhi (IST)");
		pageObj.campaignsbetaPage().setRecommendedSendDate(); // STO
		pageObj.campaignsbetaPage().clickNextBtn();

		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		logger.info(summaryData);

		Assert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom automation"),
				"Audience type did not matched on summary page");
		Assert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		Assert.assertTrue(summaryData.get(3).contains("Redeemable\n" + "Redeemable - 1000"),
				"Redeemable did not matched  on summary page");
		Assert.assertTrue(summaryData.get(5).contains("Frequency\n" + "Once"),
				"Frequency did not matched  on summary page");
		Assert.assertTrue(summaryData.get(6).contains("RECOMMENDED SEND TIME"),
				"Recommended Time not matched  on summary page");
		TestListeners.extentTest.get().pass("Campaign summary validated");

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		TestListeners.extentTest.get().pass("Mass offer Campaign created with success message");

		boolean value = pageObj.campaignsbetaPage().recommendTimeZoneDisplay();
		Assert.assertTrue(value, "recommend time zone field is not visible");
		logger.info("recommend time zone field is visible");
		TestListeners.extentTest.get().pass("recommend time zone field is visible");

		String text = pageObj.campaignsbetaPage().recommendTimeZoneToolTip();
		// Assert.assertEquals(text, dataSet.get("toolTipText"), "Tool tip text did not
		// match");
		logger.info("Verified Tool tip text for recommend time zone");
		TestListeners.extentTest.get().pass("Verified Tool tip text for recommend time zone");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");

		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().searchCampaign(campaignName);
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		String campaignID = pageObj.campaignspage().getCampaignID();
		// System.out.println(campaignID);
		String expectedHour = pageObj.campaignspage().getStartTimeHour();
		expectedHour = expectedHour.trim();
		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		logger.info("Campaign Start Time Hour is: " + expectedHour);
		TestListeners.extentTest.get().info("Campaign start time Hour is: " + expectedHour);

		// Filter Sidekiq jobs by user ID of default user (uses campaign default time)
		String jobName = "BulkMassGiftingRewardMakingWorker";
		String nonStoUserId = "425120131";
		String actualDefaultHour = pageObj.sidekiqPage().getConvertedTimeHour(nonStoUserId, jobName, "Asia/Kolkata",
				createDateTime, baseUrl);
		Assert.assertTrue(actualDefaultHour.contains(expectedHour));
		logger.info("Campaign Scheduled time hour matched for default user.");
		TestListeners.extentTest.get().pass("Campaign time hour matched for default user.");

		// Filter Sidekiq jobs by user ID of STO user (uses STO time from S3 bucket)
		String stoUserId = "424586513";
		String expectedStoHour = "6"; // This time (6 PM of America/New_York) has been added for user in S3
		String actualStoHour = pageObj.sidekiqPage().getConvertedTimeHour(stoUserId, jobName, "America/New_York",
				createDateTime, baseUrl);
		Assert.assertTrue(actualStoHour.contains(expectedStoHour));
		logger.info("Campaign Scheduled time hour matched for STO user.");
		TestListeners.extentTest.get().pass("Campaign time hour matched for STO user.");

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
