package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CategorySalesReportTest {
	static Logger logger = LogManager.getLogger(CategorySalesReportTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
	    sTCName = method.getName();
	    driver = new BrowserUtilities().launchBrowser();
	    pageObj = new PageObj(driver);
	    env = pageObj.getEnvDetails().setEnv();
	    baseUrl = pageObj.getEnvDetails().setBaseUrl();  // Ensure baseUrl is set here
	    dataSet = new ConcurrentHashMap<>();
	    pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
	    dataSet = pageObj.readData().readTestData;
	    logger.info(sTCName + " ==>" + dataSet);
	    utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4544 Verify the category sales report under reports, SQ-T4545 Verify the UI changes in the category Reporting in cockpit. ", groups = "Regression")
	public void T4544_verifyCategorysalesreport() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Validate the fields in the category reporting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dataExportPage().validateCategoryFieldsInCockpit();

		// For scan data
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		pageObj.dataExportPage().createCategorySalesReport(dataSet.get("exportName"), dataSet.get("bucketName"));

		// For P+ (Benchmark)
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		pageObj.dataExportPage().createCategorySalesReportForP(dataSet.get("exportName1"), dataSet.get("ftpName"));
		// For P+ (Activity)
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		pageObj.dataExportPage().createCategorySalesReportForP(dataSet.get("exportName2"), dataSet.get("ftpName"));
		// For P+ (Age verification)
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		pageObj.dataExportPage().createCategorySalesReportForP(dataSet.get("exportName3"), dataSet.get("ftpName"));

		// verify the UI of the category reports and the deactivation icon
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		pageObj.dataExportPage().validateUIOfCategorySchedulesAndDeactivate();

		// verify the deletion of Scan data
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		boolean flag = pageObj.dataExportPage().validateDeletionOfScanData();
		Assert.assertTrue(flag, "Scan data report is not deleted");
		logger.info("Scan data report is deleted");
		TestListeners.extentTest.get().info("Scan data report is deleted");

		// verify the deletion of Benchmark
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		boolean flag1 = pageObj.dataExportPage().validateDeletionOfBenchmark();
		Assert.assertTrue(flag1, "Benchmark report is not deleted");
		logger.info("Benchmark report is deleted");
		TestListeners.extentTest.get().info("Benchmark report is deleted");

		// verify the deletion of Activity
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		boolean flag2 = pageObj.dataExportPage().validateDeletionOfActivity();
		Assert.assertTrue(flag2, "Activity report is not deleted");
		logger.info("Activity report is deleted");
		TestListeners.extentTest.get().info("Activity report is deleted");

		// verify the deletion of Age Verification
		pageObj.menupage().navigateToSubMenuItem("Reports", "Category Sales Report");
		boolean flag3 = pageObj.dataExportPage().validateDeletionOfAgeVerification();
		Assert.assertTrue(flag3, "Age verification report is not deleted");
		logger.info("Age verification report is deleted");
		TestListeners.extentTest.get().info("Age verification report is deleted");

	}

	@AfterMethod
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
