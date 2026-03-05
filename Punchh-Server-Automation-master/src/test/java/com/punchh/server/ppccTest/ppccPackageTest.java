/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains UI test cases for the POS Control Center Packages tab.
 * @fileName ppccPackageTest.java
 */

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)

public class ppccPackageTest {
	static Logger logger = LogManager.getLogger(ppccPackageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

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
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

	@Test(description = "SQ-T6177 Validate if the headers, search field and filter icon are displayed on the PPCC packages UI", groups = {
			"regression" }, priority = 0)
	@Owner(name = "Aman Jain")
	public void SQ_T6177_verifyPPCCPackagesUIIcons() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();

		String expHeaders = dataSet.get("expectedColumnHeaders");
		List<String> expectedColumnHeaders = Arrays.asList(expHeaders.split(","));
		List<String> actualColumnHeaders = pageObj.ppccPackagePage().getColumnsHeaders();
		Assert.assertEquals(actualColumnHeaders, expectedColumnHeaders, "Column headers do not match!");
		pageObj.utils().logPass("Column headers are displayed as expected.");

		WebElement searchField = pageObj.ppccPackagePage().getSearchFieldOnUI();
		Assert.assertTrue(searchField.isDisplayed(), "Search field is not displayed.");
		pageObj.utils().logPass("Search field is displayed.");

		WebElement filterIcon = pageObj.ppccPackagePage().getFilterIconOnUI();
		Assert.assertTrue(filterIcon.isDisplayed(), "Filter icon is not displayed.");
		pageObj.utils().logPass("Filter icon is displayed.");

		String expSortableHeaders = dataSet.get("expectedSortableHeaders");
		List<String> expectedSortableHeaders = Arrays.asList(expSortableHeaders.split(","));

		List<String> missingHeaders = pageObj.ppccPackagePage().getMissingSortableHeaders();
		Assert.assertTrue(Collections.disjoint(expectedSortableHeaders, missingHeaders),
				"The following headers should have sorting options but do not: ");
		pageObj.utils().logPass("Expected headers have sorting options.");
	}

	@Test(description = "SQ-T6178 Validate if search functionality for PPCC packages is working as expected", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6178_verifyPPCCPackagesSearchFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();
		String searchItem = "Aloha";
		boolean result = pageObj.ppccPackagePage().checkSearchFunctionality(searchItem);
		Assert.assertTrue(result, String.format("Not all rows contain '%s'", searchItem));
		pageObj.utils().logPass("All rows contain the search item.");

		searchItem = "ABCD";
		result = pageObj.ppccPackagePage().checkSearchFunctionality(searchItem);
		String actualText = pageObj.utils().getLocator("ppccPackagePage.noRecordsFound").getText();
		String expectedText = "There are no records found.";
		Assert.assertFalse(result, String.format("Not all rows contain '%s'", searchItem));
		Assert.assertEquals(actualText, expectedText, "The expected message is not displayed!");
		pageObj.utils().logPass("Text is coming as expected");
	}

	@Test(description = "SQ-T6176 Validate if the filters functionality for PPCC packages is working as expected", groups = {
			"regression" }, priority = 2)
	@Owner(name = "Aman Jain")
	public void SQ_T6176_verifyPPCCPackagesFilterFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();
		String filterValue = "Aloha";
		pageObj.newSegmentHomePage().segmentAdvertiseBlock();
		boolean isDataFiltered = pageObj.ppccPackagePage().isDataFiltered("POS Type", filterValue, 2, filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
		pageObj.ppccPackagePage().removeFilters();

		String versionData = pageObj.utils().getLocatorValue("ppccPackagePage.versionDataFilter");
		WebElement firstVersionElement = driver.findElement(By.xpath(versionData + "[1]"));
		filterValue = firstVersionElement.getText();

		isDataFiltered = pageObj.ppccPackagePage().isDataFiltered("Version", filterValue, 1, filterValue); // 11.11.20.0
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
		pageObj.ppccPackagePage().removeFilters();

		filterValue = "Development";
		isDataFiltered = pageObj.ppccPackagePage().isDataFiltered("Stage", filterValue, 3, filterValue);
		Assert.assertTrue(isDataFiltered, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
		pageObj.ppccPackagePage().removeFilters();


		boolean filterResult = pageObj.ppccPackagePage().isDataFiltered("Is Generic", "Yes", 2, "Generic");
		Assert.assertTrue(filterResult, "Filtered results do not match expected value!");
		pageObj.utils().logPass("Filtered results match expected value.");
		pageObj.ppccPackagePage().removeFilters();
	}

	@Test(description = "SQ-T6172 Verify that the created at sorting functionality is working as expected.", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6172_verifyPPCCPackagesCreatedAtSortingFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();
		pageObj.ppccPackagePage().changeEntriesToHundred(100);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a", Locale.ENGLISH);
		List<LocalDateTime> createdAtDateData = pageObj.ppccPackagePage().getDateColumnData("4", formatter);

		List<LocalDateTime> sortedCreatedAtDateData = new ArrayList<>(createdAtDateData);
		sortedCreatedAtDateData.sort(Collections.reverseOrder());
		Assert.assertEquals(sortedCreatedAtDateData, createdAtDateData,
				"Created at column data is not in descending order.");
		pageObj.utils().logPass("Created at column data is in descending order.");

		pageObj.ppccPackagePage().sortData("ppccPackagePage.createdDateHeader");
		createdAtDateData = pageObj.ppccPackagePage().getDateColumnData("4", formatter);
		sortedCreatedAtDateData = new ArrayList<>(createdAtDateData);
		Collections.sort(sortedCreatedAtDateData);
		Assert.assertEquals(sortedCreatedAtDateData, createdAtDateData,
				"Created at column data is not in ascending order.");
		pageObj.utils().logPass("Created at column data is in ascending order.");
	}

	@Test(description = "SQ-T6172 Verify that the stage sorting functionality is working as expected.", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6172_verifyPPCCPackagesStageSortingFunctionality() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();
		pageObj.ppccPackagePage().changeEntriesToHundred(100);

		pageObj.ppccPackagePage().sortData("ppccPackagePage.stageHeader");
		List<String> stageData = pageObj.ppccPackagePage().getColumnData("3");
		List<String> sortedStageData = new ArrayList<>(stageData);
		Collections.sort(sortedStageData);
		Assert.assertEquals(sortedStageData, stageData, "Stages column data is not in ascending order.");
		pageObj.utils().logPass("Stages column data is in ascending order.");

		pageObj.ppccPackagePage().sortData("ppccPackagePage.stageHeader");
		stageData = pageObj.ppccPackagePage().getColumnData("3");
		sortedStageData.sort(Collections.reverseOrder());
		Assert.assertEquals(sortedStageData, stageData, "Stages column data is not in descending order.");
		pageObj.utils().logPass("Stages column data is in descending order.");
	}

	@Test(description = "SQ-T6172: Verify that the version sorting functionality is working as expected.")
	@Owner(name = "Aman Jain")
    public void SQ_T6172_verifyPPCCPackagesVersionSortingFunctionality() throws InterruptedException {

       pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
       pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().waitTillPCCLoaderDisappears();
       pageObj.ppccPackagePage().navigateToPackagesTab();
	   pageObj.ppccPackagePage().changeEntriesToHundred(100);

       pageObj.ppccPackagePage().sortData("ppccPackagePage.versionHeader");
       List<String> versionData = pageObj.ppccPackagePage().getColumnData("1");
	   List<String> versionNameData = pageObj.ppccPackagePage().getCleanedVersionData(versionData);
       List<String> sortedVersionData = pageObj.ppccPackagePage().sortList(versionNameData);
       Assert.assertEquals(versionNameData, sortedVersionData, "Version column data is not in ascending order.");
       pageObj.utils().logPass("Version column data is in ascending order.");

        pageObj.ppccPackagePage().sortData("ppccPackagePage.versionHeader");
        versionData = pageObj.ppccPackagePage().getColumnData("1");
		versionNameData = pageObj.ppccPackagePage().getCleanedVersionData(versionData);
		List<String> sortedVersionDataDesc = pageObj.ppccPackagePage().sortList(versionNameData);
        Collections.reverse(sortedVersionDataDesc);
        Assert.assertEquals(versionNameData, sortedVersionDataDesc, "Version column data is not in descending order.");
        pageObj.utils().logPass("Version column data is in descending order.");
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
