package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class PrivacyPolicyValidationTest {
	static Logger logger = LogManager.getLogger(PrivacyPolicyValidationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
	}

	// Checked with Raja, following test case need not required for automation
	// @Test(description = "SQ-T2730 To validate \"Privacy Policy URL\" field in
	// dashboard")
	public void T2730_verifyprivacypolicy() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().settingsLink();
		// pageObj.menupage().punchhpickuplink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Punchh Pickup");
		pageObj.punchhpickuppage().clickOnMobileWeb(dataSet.get("email"));
//			pageObj.punchhpickuppage().clickonupdate();
	}

}
