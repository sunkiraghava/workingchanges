package com.punchh.server.Test;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ExportPOSscoreboardTest {
	private static Logger logger = LogManager.getLogger(ExportPOSscoreboardTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String externalUID;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "DP1-T229 Enable Databricks flag removal from business cockpit dashboard Miscellaneous Config: Verify that upon clicking the export POS scoreboard, the user receives the email for the exported scoreboard", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shaleen Gupta")
	public void T229_ExportPOSscore() throws InterruptedException {

		// Select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to dashboard menu
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().dashboardPageMiscellaneousConfig();

		Boolean flag = pageObj.menupage().flagPresentorNot("Enable Databricks");
		Assert.assertFalse(flag, "Enable Databricks flag is presnt in Miscellaneous Config");
		logger.info("Enable Databricks flag is not presnt in Miscellaneous Config");
		TestListeners.extentTest.get().pass("Enable Databricks flag is not presnt in Miscellaneous Config");

		// user receives email for exported scoreboard functionality has been removed as
		// report are moved to JS enabled
		// so commenting below code as discussed with ashwini

		// navigate to POS stats
//		pageObj.menupage().navigateToSubMenuItem("Support", "POS Stats");
//		pageObj.menupage().posScoreBoard();
//		String text = utils.getLocator("posStatsPage.posScoreBoardText").getText();
//		Assert.assertEquals(text, "POS Scoreboard");
//		logger.info("user is navigated to POS Scoreboard");
//		TestListeners.extentTest.get().pass("user is navigated to POS Scoreboard");

		// export pos
//		pageObj.menupage().exportPOS();
//		utils.waitTillPagePaceDone();
//		String text1=utils.getLocator("posStatsPage.exportPOSscoreboardText").getText();
//		Assert.assertEquals(text1, "POS Scoreboard stats will be emailed to you shortly.");
//		logger.info("Success messeage appears : POS Scoreboard stats will be emailed to you shortly.");
//		TestListeners.extentTest.get().pass("Success messeage appears : POS Scoreboard stats will be emailed to you shortly.");
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
