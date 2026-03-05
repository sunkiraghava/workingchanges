package com.punchh.server.NCHPTest;

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
import com.punchh.server.utilities.TestListeners;

// Author: Amit
@Listeners(TestListeners.class)
public class NewCamHomeStickyBarTest {

	private static Logger logger = LogManager.getLogger(NewCamHomeStickyBarTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";

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
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5169 Verify Sticky bar for All/mass/automation/other categories"
			+ "SQ-T5186 Verify Create access to blackout date for brands", priority = 0, groups = { "regression",
					"dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T5169_verifyStickyBarForAllMassAutomationOtherCategories() throws InterruptedException {

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectCampaignsTab("Mass");
		String val = pageObj.newCamHomePage().moveToBalckoutdates();
		Assert.assertEquals(val, "Blackout Dates", "Blackout Dates Page header did not matched");

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Mass tab values
		pageObj.newCamHomePage().getCampaignType("All");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().selectStickyBarOptions("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Archive");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().unSelectAllCampaignCheckBox();

		pageObj.newCamHomePage().getCampaignType("Mass");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().selectStickyBarOptions("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().unSelectAllCampaignCheckBox();

		pageObj.newCamHomePage().getCampaignType("Automations");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().selectStickyBarOptions("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Archive");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().unSelectAllCampaignCheckBox();

		pageObj.newCamHomePage().getCampaignType("Other");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().selectStickyBarOptions("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Archive");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectStickyBarOptions("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().unSelectAllCampaignCheckBox();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}