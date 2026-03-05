package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class UnifiedDataExportsTest {
	static Logger logger = LogManager.getLogger(UnifiedDataExportsTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	String exportName;
	Utilities utils;
	ApiUtils apiUtils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6964 [Phase 1] Add Additional Scheduled Export tab in Data Export and flag configuration for additional export schedules", groups = "Regression")
	public void T6964_verifyUnifiedDataExport() throws InterruptedException {
		// verify the checkbox is present and enable it
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// go to miscellaneous config under cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_unified_data_export", "check", true);
		// navigate to data export page and verify the new tab is present
		pageObj.dataExportPage().goToDataExport();
		pageObj.dataExportPage().verifyAdditionalScheduledExportTabPresent();
		pageObj.dataExportPage().validationsInAdditionalScheduledExportTab();
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
