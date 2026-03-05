package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
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
//it cannot run with single login, as it requires two different users to perform the actions
public class CampaignBetaRejectionTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaRejectionTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	String rejectCampaignName;

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

	@Test(description = "SQ-T2507 Campaign Rejection Flow - part-1 submit for approval", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2507_verifyCampaignRejectionFlow_PartOne() throws Exception {

		rejectCampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		pageObj.campaignsbetaPage().SetCampaignName(rejectCampaignName);
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
		// Submit for approval
		pageObj.campaignsbetaPage().clickSubmitForApprovalBtn1();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Your campaign has been submitted for approval.",
				"Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign created with success message");
	}

	@Test(description = "SQ-T2507 Campaign Rejection Flow - part -2 reject campaign", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2507_verifyCampaignRejectionFlow_PartTwo() throws Exception {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"), dataSet.get("password"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campName = pageObj.campaignsbetaPage().searchCampaign(rejectCampaignName);
		String campStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(campName.contains(rejectCampaignName), "Campaign name did not matched");
		Assert.assertEquals(campStatus, "Pending Approval", "Campaign status did not matched");
		TestListeners.extentTest.get().pass("Campaign name and status validated");
		pageObj.campaignsbetaPage().selectCampaign();
		// Reject campaign
		Thread.sleep(3000);
		pageObj.campaignsbetaPage().rejectCampaign();
		String mssg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertTrue(mssg.contains("This campaign was successfully rejected"),
				"Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign rejected with success message");
		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String cName = pageObj.campaignsbetaPage().searchCampaign(rejectCampaignName);
		String cStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(cName.contains(rejectCampaignName), "Campaign name did not matched");
		Assert.assertEquals(cStatus, "Disapproved", "Campaign status did not matched");
		TestListeners.extentTest.get().pass("Campaign name and status validated");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
