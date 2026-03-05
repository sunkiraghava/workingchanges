package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignBetaOptionsTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaOptionsTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	private String campaignName;

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
	}

	@Test(description = "SQ-T2541 Deactivate Campaign", groups = { "regression", "unstable","nonNightly" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T2541_verifyDeactivateCampaign() throws InterruptedException, ParseException { // admin user login
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		campaignName = "Automation Massoffer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();

		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().validateReachablityandControlGroup();
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(6000);

		pageObj.campaignsbetaPage().setPoints("10");
//				pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
//				pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		Thread.sleep(2000);

		pageObj.campaignsbetaPage().setEmailNotification(campaignName, campaignName);
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().setFrequency("Once");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timezone"));
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(scheduleMsg, "Schedule created successfully.", "Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign edit and actiavte by approver is done and validated");
		// validate campaign status
//				pageObj.menupage().clickCampaignsMenu();
//				pageObj.menupage().clickCampaignsBetaLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

//				Thread.sleep(10000);
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);

		// pageObj.campaignsbetaPage().selectCampaign();
		utils.switchToWindow();
		Thread.sleep(4000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Deactivate");
		String value = pageObj.campaignsbetaPage().getPopUpValues();
		Assert.assertTrue(value.contains("Do you want to deactivate this campaign?"),
				"Deactivate popup message did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

		pageObj.campaignsbetaPage().selectdotOptionsValue("Deactivate");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickSubmitBtn();
		Thread.sleep(2000);
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Campaign successfully deactivated."),
				"Deactivate campain message did not matched");
		String status = pageObj.campaignsbetaPage().inactiveStatus();
		Assert.assertEquals(status, "INACTIVE", "Campaign status did not matched as Inactive");

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
