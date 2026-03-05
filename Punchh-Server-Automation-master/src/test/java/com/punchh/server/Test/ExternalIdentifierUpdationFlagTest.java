package com.punchh.server.Test;

import com.punchh.server.annotations.Owner;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;

import java.util.Arrays;
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

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import java.lang.reflect.Method;

@Listeners(TestListeners.class)

public class ExternalIdentifierUpdationFlagTest {
	private static Logger logger = LogManager.getLogger(ExternalIdentifierUpdationFlagTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "CCA2-760 | Introduce new config on the backend to Update user Access Token with the configured source only", groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T2954_verifyExternalIdentifierUpdation() throws InterruptedException {
		// instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String expVal = dataSet.get("expectedExternalSourceIdList");
		List<String> expArray = Arrays.asList(expVal.split(","));
		System.out.println(expArray);

		// Click Cockpit and check External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkExternalSourceIdFlag();

		// click on External Identifier Updation IDP
		List<String> externalIdentifierUpdationIDPNames = pageObj.instanceDashboardPage()
				.externalIdentifierUpdationIDPList();
		System.out.println(externalIdentifierUpdationIDPNames);

		// verify elements of External Identifier Updation IDP was visible as expected
		// or not
		Assert.assertTrue(externalIdentifierUpdationIDPNames.equals(expArray),
				"External Identifier Updation IDP were not matched");
		logger.info("External Identifier Updation IDP were matched");
		TestListeners.extentTest.get().info("External Identifier Updation IDP were matched");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}