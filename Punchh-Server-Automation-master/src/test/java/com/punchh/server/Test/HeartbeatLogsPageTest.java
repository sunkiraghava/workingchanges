package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class HeartbeatLogsPageTest {
	private static Logger logger = LogManager.getLogger(HeartbeatLogsPageTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3126 Validate that user is able to view Logs if 'Heartbeat Logs' permission is available to logged in user"
			+ "SQ-T3127 Validate that Franchise Admin is not able to view logs even if ‘Extended Support’ and 'Heartbeat Logs' permission is available", groups = { "regression","dailyrun"},priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3126_HeartbeatLogPageVisibility() throws InterruptedException {
		// instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Heartbeats");
		String text = pageObj.settingsPage().heartbeatLogPageVisibility();
		Assert.assertEquals(text, "Heartbeats", "Heartbeat Logs title doesn't match");
		logger.info("Heartbeat Logs title doesn't match");
		TestListeners.extentTest.get().pass("Heartbeat Logs title doesn't match");
	}

	@Test(description = "SQ-T3127 Validate that Franchise Admin is not able to view logs even if ‘Extended Support’ and 'Heartbeat Logs' permission is available", groups = { "regression","dailyrun"},priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T3127_VerifyFranchiseUserNotToViewHeartbeatLogs() throws InterruptedException {
		// instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("adminUser"), dataSet.get("adminPassword"));
		boolean isDisplayed = pageObj.menupage().navigateToSubMenuItem("Support", "Heartbeats");
		Assert.assertFalse(isDisplayed, "Heartbeat Logs page is visible for Franchise Admin");
		logger.info("Verified that Heartbeat Logs page is not visible for Franchise Admin");
		TestListeners.extentTest.get().pass("Verified that Heartbeat Logs page is not visible for Franchise Admin");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
