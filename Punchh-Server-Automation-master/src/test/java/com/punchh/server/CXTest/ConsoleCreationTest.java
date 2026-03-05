package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
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
public class ConsoleCreationTest {

	private static Logger logger = LogManager.getLogger(ConsoleCreationTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private Utilities utils;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	String activate = "/activate/";
	String locationName = "";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		env = prop.getProperty("environment");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	/*
	 * Checked with Raja, this method is covered in E2EcurbsideorderTest class,
	 * therefore it can be commented here.
	 */
	// @Test(description = "SQ-T2289, Validate Console Creation || SQ-T2290,  || SQ-T2320, Deactivate functionality "
	//		+ "of a console " + "|| SQ-T2321, Delete console", groups = { "regression", "dailyrun" }, priority = 0)
	public void verifyConsoleCreation() throws InterruptedException {

		@SuppressWarnings("static-access")
		String storeNumber = utils.getCurrentDate("ddmmyyhhmmss");
		// Instance login and goto dashboard
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		// Add New Location
	    locationName = pageObj.locationPage().newLocation(storeNumber);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch(locationName);
		pageObj.locationPage().SelectLocationConsole();
		String consoleName = "Consolename_" + CreateDateTime.getTimeDateString();
		pageObj.locationPage().addConsole(consoleName);
		// Console login validation
		pageObj.locationPage().navigateTopickupConsole(baseUrl + activate + dataSet.get("slug"));
		pageObj.locationPage().pickupConsole();

		// Console Deactivation followed by deletion of console script
		pageObj.locationPage().deleteConsole(consoleName);
		// Delete New Location
		/*pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().selectLocationSearch(locationName);
		pageObj.locationPage().deletenewLocation();*/
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteLocationByName(locationName,dataSet.get("slugid"),env);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}