package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
//it cannot run with single login, as it requires two different users to perform the actions
public class CampaignBetaApprovalTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaApprovalTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	public static String approveCampaignName;

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
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2510 Create Campaign and Submit for Approval", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2510_verifyCreateCampaignandSubmitforApproval() throws Exception {

		approveCampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		pageObj.campaignsbetaPage().SetCampaignName(approveCampaignName);
		// pageObj.campaignsbetaPage().setCampaignType("message");
		pageObj.campaignsbetaPage().clickContinueBtn();
		// create campaign
		utils.switchToWindow();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timezone"));
		pageObj.campaignsbetaPage().clickNextBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		logger.info(summaryData);

		Assert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom"),
				"Audience type did not matched on summary page");
		Assert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		Assert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Once"),
				"Frequencydid not matched  on summary page");

		TestListeners.extentTest.get().pass("Campaign summary validated");
		// Submit for approval
		pageObj.campaignsbetaPage().clickSubmitForApprovalBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Your campaign has been submitted for approval.",
				"Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign created with success message");
		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campName = pageObj.campaignsbetaPage().searchCampaign(approveCampaignName);
		String campStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(campName.contains(approveCampaignName), "Campaign name did not matched");
		Assert.assertEquals(campStatus, "Pending Approval", "Campaign status did not matched");
		TestListeners.extentTest.get().pass("Campaign name and status validated");
		// Edit campaign
		pageObj.campaignsbetaPage().clickCampaignName();
		pageObj.campaignsbetaPage().gotoWhoTab();
		pageObj.campaignsbetaPage().setSegmentType("custom automation");
//		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickSubmitForApprovalBtn();
		boolean state = pageObj.campaignsbetaPage().checkElementState();
		Assert.assertFalse(state, "Submit for Approval button is not disabled");
		TestListeners.extentTest.get()
				.pass("Campaign edit is done and Submit for Approval button is disabled validated");
	}

	@Test(description = "SQ-T2511 Campaign Approval Flow", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2511_verifyCampaignApprovalFlow() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
		String campName = pageObj.campaignsbetaPage().searchCampaign(approveCampaignName);
		String campStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(campName.contains(approveCampaignName), "Campaign name did not matched");
		Assert.assertEquals(campStatus, "Pending Approval", "Campaign status did not matched");
		TestListeners.extentTest.get().pass("Campaign name and status validated");
		pageObj.campaignsbetaPage().selectCampaign();
		pageObj.campaignsbetaPage().approveCampaign();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("This campaign was approved and scheduled"),
				"Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign approved with success message");

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String cName = pageObj.campaignsbetaPage().searchCampaign(approveCampaignName);
		String cStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(cName.contains(approveCampaignName), "Campaign name did not matched");
		Assert.assertEquals(cStatus, "Scheduled", "Campaign status did not matched");
		TestListeners.extentTest.get().pass("Campaign name and status validated");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
