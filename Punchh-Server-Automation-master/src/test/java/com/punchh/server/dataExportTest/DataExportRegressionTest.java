package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.pages.SchedulePage;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportRegressionTest {
	static Logger logger = LogManager.getLogger(DataExportRegressionTest.class);
	public WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private Properties prop;
	SchedulePage schedulePage;
	String exportName;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		schedulePage = new SchedulePage(driver);
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		exportName = CreateDateTime.getUniqueString("AutoDataExport");
	}

//	@Test(groups = {
//			"Regression, " }, description = "SQ-T2436 (1.0) [Regression] : Create/Edit/delete/disable FTP Endpoints and check Data export logs should print")
	public void T2436_createFtp() throws InterruptedException {
		// logger.info("== Run SQL exports for all 14 data exports ==");
		String ftpName = "AutoFtp" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "FTP Endpoints");
		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().clickFtpEndPoint();
		pageObj.menupage().navigateToSubMenuItem("Settings", "FTP Endpoints");
		pageObj.settingsPage().createNewFtp(ftpName, Utilities.getConfigProperty("ftpIp"),
				Utilities.getConfigProperty("ftpPort"), Utilities.getConfigProperty("ftpUsername"),
				Utilities.getConfigProperty("ftpPassword"), Utilities.getConfigProperty("ftpPath"),
				Utilities.getConfigProperty("ftpEmail"));
		pageObj.dataExportPage().goToDataExport();
		pageObj.dataExportPage().createNewDataExportTemplate(exportName);
		schedulePage.scheduleNewDataExport(ftpName);
		schedulePage.verifyDataExportSchedule(prop.getProperty("dataExportSchedule"), exportName,
				prop.getProperty("ftpPath"));
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("slug"));
		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().clickFtpEndPoint();
		pageObj.menupage().navigateToSubMenuItem("Settings", "FTP Endpoints");
		pageObj.settingsPage().editFtpConnection(ftpName);
		pageObj.settingsPage().disbaleEnableFtpConnection(ftpName);
		pageObj.settingsPage().deleteFtpConnection(ftpName);
	}

//	@Test(groups = {
//			"Regression, " }, description = "SQ-T2618 (1.0) Verify remove headers from export files functionality")
	public void T2530_verifyRemoveHeaderDataExport() throws InterruptedException {
		logger.info("== Run SQL exports for all 14 data exports ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("wingStopSlug"));
		pageObj.dataExportPage().goToDataExportBeta();
		pageObj.dataExportPage().removeHeaderFromDataExport("");
		schedulePage.scheduleNewDataExport(Utilities.getConfigProperty("ftpEndPointWingStop"));
		int fileCount = schedulePage.verifyDataExportSchedule(prop.getProperty("dataExportSchedule"), exportName,
				prop.getProperty("ftpPath"));
		Assert.assertEquals(fileCount, 17);
	}
	
	@Test (description = "validate the fields of the LLL once the flag is enabled." +
	" SQ-T5298 : Verify the location level liability with the 'fetch between dates' checkbox" )
	public void T5298_verifyLocationLevelLiabilityDataExport () throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		String str = dataSet.get("LLL_dataExportName");

		List<String> businessType = Arrays.asList("PTC", "PTR", "PUR");
		
		for (String bType : businessType) {
			String bSlug = "bSlug_" + bType ;
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get(bSlug));

			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
			pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_location_level_reporting_between_dates", "check");

			HashMap<String, ArrayList<String>> fieldMap = new HashMap<>();
			ArrayList<String> fieldListSelected = new ArrayList<String>();
			List<String> fieldListAvailable = new ArrayList<String>();

			logger.info("=== LLL Data export Field validation test for business type : " + bType + " ===");
			String exportName = CreateDateTime.getUniqueString("T5298_LLLDataExport_" + bType);
			pageObj.menupage().navigateToSubMenuItem("Reports", "Data Export");
			pageObj.dataExportPage().setDataExportNameAndDate(exportName);

			fieldListSelected = pageObj.dataExportPage().clickDataExportNameCheckBoxDefaultFields(str);
			String a = str.replace(" Data", "");
			String b = (a.replace(" ", "_")).toLowerCase();
			fieldMap.put(b, fieldListSelected);

			pageObj.dataExportPage().clickOnDataExportNameLink(str);
			utils.longWaitInSeconds(4);
			fieldListAvailable = pageObj.dataExportPage().getReportFields(str, "available") ;

			// verify that fieldListSelected has desired items
			List<String> LLL_actualSelectedFieldList = fieldMap.get("location_level_liability");
			Assert.assertEquals(LLL_actualSelectedFieldList, dataSet.get("LLL_expectedSelectedFieldListFor_" + bType)) ;
			logger.info("Successfully verified location_level_liability selected field list for business type: " + bType);
			TestListeners.extentTest.get().info("Successfully verified location_level_liability selected field list for business type: " + bType);

			// verify that fieldListAvailable has desired items
			Assert.assertEquals(fieldListAvailable, dataSet.get("LLL_expectedAvailableFieldListFor_" + bType)) ;
			logger.info("Successfully verified location_level_liability available field list for business type: " + bType);
			TestListeners.extentTest.get().info("Successfully verified location_level_liability available field list for business type: " + bType);

			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		}
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.close();
		driver.quit();
	}
}
