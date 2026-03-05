package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class DataExportForMultipleEmailRecipients {
	static Logger logger = LogManager.getLogger(DataExportForMultipleEmailRecipients.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	String exportName;

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) {
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		exportName = CreateDateTime.getUniqueString("LocationDataExport");
	}

	@Test(description = "SQ-T4389 Verify the email recipients field is not giving 500 error when the character limit exceeds", groups = "Regression")
	public void T4389_verifyMultipleEmailRecipients() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.dataExportPage().goToDataExport();
		pageObj.dataExportPage().createLocationsDataExport(exportName);
		boolean flag = pageObj.schedulePage().scheduleNewMultipleEmailExport();
		Assert.assertTrue(flag, "schedule is created successfully");
		logger.info("The schedule with multiple email shows an error message");
		TestListeners.extentTest.get().info("The schedule with multiple email shows an error message");
	}

	@AfterMethod
	public void tearDown() throws Exception {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

}
