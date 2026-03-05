package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class STOCampaignFlagTest {
	private static Logger logger = LogManager.getLogger(STOCampaignFlagTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";
	Utilities utils;
	SeleniumUtilities selUtils;

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
		selUtils = new SeleniumUtilities(driver);
	}

	// as discussed with khusbu soni not needed in regression suite
	@Test(description = "SQ-T2658 STO Campaigns Flag", groups = { "regression", "dailyrun" })
	public void T2658_STOCampaignsFlag() throws InterruptedException, ParseException {
		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		// instance
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
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("campaignMessageType"));
		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		pageObj.campaignsbetaPage().clickContinueBtn();
		String parentWindow = selUtils.switchToNewWindow();
		// Set segment
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		// checking for STO flag (Enabled)
		boolean stoLinkIsDisplayed = pageObj.campaignsbetaPage().verifySTOIsDisplayed();
		Assert.assertTrue(stoLinkIsDisplayed, "STO link did not displayed");
		utils.logPass("STO field is visible");
		// switch to main tab
		selUtils.switchToWindow(parentWindow);
		// Disable STO flag

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().offEnableSTOCheckbox();
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// searching for campaign
		pageObj.campaignsbetaPage().searchCampaign(campaignName);
		pageObj.campaignsbetaPage().clickCampaignName();
		pageObj.campaignsbetaPage().clickWhenTab();
		// checking for STO flag (Disabled)
		boolean stoLinkIsDisplayed1 = pageObj.campaignsbetaPage().verifySTOIsDisplayed();
		Assert.assertFalse(stoLinkIsDisplayed1, "STO field should not displayed but it is visible");
		utils.logPass("STO flag is not visible");
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
