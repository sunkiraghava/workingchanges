package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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
//it cannot run with single login, as it requires different user roles to perform the actions
public class CampaignBetaEditTest {

	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	String CampaignName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils.logit(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2505 Approver Edit a Pending Approval Campaign", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2505_verifyApproverEditPendingApprovalCampaign() throws Exception {
		CampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
//		Amit.kumar+1@punchh.com: no access to workflow and config management[manager] submitter
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		Thread.sleep(10000);
		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(CampaignName);
		Thread.sleep(2000);
		// pageObj.campaignsbetaPage().setCampaignType("message");
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		// create campaign
		utils.switchToWindow();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timezone"));
		pageObj.campaignsbetaPage().clickNextBtn();
		// Submit for approval
		pageObj.campaignsbetaPage().clickSubmitForApprovalBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Your campaign has been submitted for approval.",
				"Success message text did not matched");
		utils.logPass("Campaign created with success message");

	}

	@Test(description = "SQ-T2505 Approver Edit a Pending Approval Campaign", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2505_verifyApproverEditPendingApprovalCampaignParttwo() throws Exception {

		// Login with approver and edit pending approval campaign
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"), dataSet.get("password"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campName = pageObj.campaignsbetaPage().searchCampaign(CampaignName);
		String campStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(campName.contains(CampaignName), "Campaign name did not matched");
		Assert.assertEquals(campStatus, "Pending Approval", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated");
		// Edit campaign
		pageObj.campaignsbetaPage().clickCampaignName();
		pageObj.campaignsbetaPage().gotoWhoTab();
		pageObj.campaignsbetaPage().setSegmentType("custom automation");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(scheduleMsg, "Schedule updated successfully.", "Success message text did not matched");
		utils.logPass("Campaign edit and actiavte by approver is done and validated");
		// validate campaign status
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(10000);
		String scheduledcampName = pageObj.campaignsbetaPage().searchCampaign(CampaignName);
		String scheduledcampStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(scheduledcampName.contains(CampaignName), "Campaign name did not matched");
		Assert.assertEquals(scheduledcampStatus, "Scheduled", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated scheduled");

	}

	@Test(description = "SQ-T2539 Campaign Audit Logs with approver login", groups = { "regression",
			"dailyrun" }, priority = 2)

	@Owner(name = "Amit Kumar")
// campaign status should be Draft
	public void T2539_verifyCampaignAuditLogsApproverUser() throws InterruptedException {
// user approver login (with access to workflow and configuration management)
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"), dataSet.get("password"));

// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//Thread.sleep(10000);
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.campaignsbetaPage().searchCampaign("Automation MassOffer Campaign_DND");
		pageObj.campaignsbetaPage().selectCampaign();
		pageObj.campaignsbetaPage().goToAuditLogs();
// pageObj.campaignsbetaPage().auditLogs();
		if (utils.isAlertpresent(driver)) {
			utils.acceptAlert(driver);
		}
		boolean approverStatus = pageObj.campaignsbetaPage().checkAuditLogs();
		Assert.assertTrue(approverStatus, "Audit Logs page did not displayed");
		utils.logPass("Approver user navigated to Audit logs successfully");

	}

	@Test(description = "SQ-T2539 Campaign Audit Logs with submitter login", groups = { "regression",
			"dailyrun" }, priority = 3)

	@Owner(name = "Amit Kumar")
	public void T2539_verifyCampaignAuditLogsSubmitterUser() throws InterruptedException {
		// campaign status should be Draft
		// user submitter login (no access to workflow and configuration management)
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.campaignsbetaPage().searchCampaign("Automation MassOffer Campaign_DND");
		pageObj.campaignsbetaPage().selectCampaign();
		pageObj.campaignsbetaPage().goToAuditLogs();
		// pageObj.campaignsbetaPage().auditLogs();
		if (utils.isAlertpresent(driver)) {
			utils.acceptAlert(driver);
		}
		String msg = pageObj.campaignsbetaPage().getAlertmsg();
		Assert.assertTrue(msg.contains("Insufficient Privileges to access this resource"),
				"Insufficient Privileges message did not displayed");
		utils.logPass("Submitter user access to Audit Logs verified");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		utils.logit("Browser closed");
	}
}
