package com.punchh.server.cambetaTest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import java.lang.reflect.Method;

@Listeners(TestListeners.class)
public class CampaignBetaPreScript {
	private static Logger logger = LogManager.getLogger(CampaignBetaPreScript.class);
	public WebDriver driver;
	@SuppressWarnings("unused")
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	String approveCampaignName;
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
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "Campaign Beta Pre Script", groups = "Regression", priority = 0)
	public void CamBetaPreScript() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().adminUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().verifyUserPermissionForAmitKumar1("amit.kumar+1@punchh.com", "Business Manager");
		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().adminUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().verifyUserPermissionForAmitKumar4("amit.kumar+4@punchh.com", "Business Manager");

	}
}
