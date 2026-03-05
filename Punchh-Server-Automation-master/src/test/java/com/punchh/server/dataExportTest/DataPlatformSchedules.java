package com.punchh.server.dataExportTest;

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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataPlatformSchedules {
	static Logger logger = LogManager.getLogger(DataPlatformSchedules.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	String exportName;
	Utilities utils;

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
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T5547 Verify the employee schedule.", groups = "Regression")
	public void T5547_verifyEmployeeSchedule() throws InterruptedException {
		logger.info("== Employee schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("EmployeeSchedule");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createEmployeeReviewSchedule(dataSet.get("frequency"), exportName,
				dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Employee schedule is verified");
		TestListeners.extentTest.get().pass("Employee schedule is verified");
	}

	@Test(description = "SQ-T5585 [Schedules]Verify the 'Status' column is added in the Location Summary export..", groups = "Regression")
	public void T5585_verifyLocationSummarySchedule() throws InterruptedException {
		logger.info("== Location Summary export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("LocationSummary");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createLocationSummarySchedule(dataSet.get("frequency"), exportName,
				dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Location summary schedule is verified");
		TestListeners.extentTest.get().pass("Location summary schedule is verified");
	}

	@Test(description = "SQ-T4576 Survey Export Schedule moved to Databricks: Verify that the user is able to create the survey export and logs are appearing without any errors.", groups = "Regression")
	public void T4576_verifySurveySchedule() throws InterruptedException {
		logger.info("== Survey export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("SurveySchedule");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createSurveySchedule(dataSet.get("survey"), exportName, dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Survey export schedule is verified");
		TestListeners.extentTest.get().info("Survey export schedule is verified");
	}

	@Test(description = "SQ-T5877 [Schedules]Verify the logs for Challenge export schedule.", groups = "Regression")
	public void T5877_verifyChallengeExportSchedule() throws InterruptedException {
		logger.info("== Challenge export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("ChallengeExport");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createChallengeExportSchedule(dataSet.get("challengeCampaign"), exportName,
				dataSet.get("frequency"), dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("challenge export schedule is verified");
		TestListeners.extentTest.get().pass("challenge export schedule is verified");

	}

	@Test(description = "SQ-T5886 [Schedules]validate the logs for Svs Payment Reconciliation Schedule and Employee review schedules", groups = "Regression")
	public void T5886_verifySvsPaymentReconciliationExportSchedule() throws InterruptedException {
		logger.info("==Svs Payment Reconciliation export schedule Test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createSvsPaymentReconciliationSchedule(dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("challenge export schedule is verified");
		TestListeners.extentTest.get().pass("challenge export schedule is verified");
	}

	@Test(description = "SQ-T5885 [Schedules]Validate the logs of WoW schedules.", groups = "Regression")
	public void T5885_verifyWowExportSchedule() throws InterruptedException {
		logger.info("== Wow export schedule Test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createWoWSchedule("");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Wow export schedule is verified");
		TestListeners.extentTest.get().pass("Wow export schedule is verified");
	}

	@Test(description = "SQ-T5851 [Schedules]Verify the franchise report schedule logs"
			+ "SQ-T6104 Click and verify “Run Now” button for all four schedules : Franchisee Report Schedule, Location Scoreboard Schedule, WOW Schedule, MOM Schedule.", groups = "Regression")
	public void T5886_verifyFranchiseExportSchedule() throws InterruptedException {
		logger.info("== Franchise Export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("franchiseSchedule");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createFranchiseSchedule("", exportName, dataSet.get("franchise"), dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Franchise Export schedule is verified");
		TestListeners.extentTest.get().pass("Franchise Export schedule is verified");
	}

	@Test(description = "SQ-T6306 Verify the loggings for the Social cause export schedule", groups = "Regression")
	public void T6306_verifySocialCauseSchedule() throws InterruptedException {
		logger.info("==Social cause Export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("SocialCauseSchedule");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createSocialCauseSchedule(dataSet.get("socialCauseCampaign"), exportName,
				dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Social Cause Export schedule is verified");
		TestListeners.extentTest.get().pass("Social Cause Export schedule is verified");
	}

	@Test(description = "SQ-T6307 Validate the loggings for the redemption stats export schedule", groups = "Regression")
	public void T6307_verifyRedemptionStatsExportSchedule() throws InterruptedException {
		logger.info("==Redemption Stats Export schedule Test ==");
		String exportName = CreateDateTime.getUniqueString("RedemptionstatsSchedule");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createRedemptionStatsSchedule(exportName, dataSet.get("frequency"),
				dataSet.get("email"));
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Franchise Export schedule is verified");
		TestListeners.extentTest.get().pass("Franchise Export schedule is verified");
	}

	@Test(description = "SQ-T5219 [Schedule] Verify Location Scoreboard Schedule.", groups = "Regression")
	public void T5219_verifyLocationScoreboardSchedule() throws InterruptedException {
		logger.info("==Location Scoreboard Export schedule Test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().createLocationscoreboardSchedule("");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		pageObj.schedulePage().runSchedules();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleType"));
		boolean status = pageObj.schedulePage().verifyDPScheduleLogs();
		pageObj.schedulePage().deleteSchedule();
		Assert.assertTrue(status, "Log is not displayed.");
		logger.info("Location scoreboard schedule is verified");
		TestListeners.extentTest.get().pass("Location scoreboard schedule is verified");
	}

	@AfterMethod
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
