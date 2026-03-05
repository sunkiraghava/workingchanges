package com.punchh.server.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class ContentLibraryTest {
	private static Logger logger = LogManager.getLogger(ContentLibraryTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String timstamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		timstamp = CreateDateTime.getTimeDateString();
	}

	// Author Ajeet
	@Test(description = "SQ-T6050 create folder,create a row having multiple section"
			+ "attached the created row to email template, delete the row,create folder edit and save, create new category and delete ", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Ajeet")

	public void T6050_VerifySavedRowAttachToEmailTemplate() throws InterruptedException, IOException {
		String emailTemplateName = "foltemp";
		emailTemplateName = emailTemplateName.replace("temp", timstamp);
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Content Library");
		SoftAssert softAssert = new SoftAssert();
		String labels = pageObj.mobileconfigurationPage().getPageSource();
		softAssert.assertTrue(labels.contains("Content Library"), "Failed to verify title Content Library");
		// verify sortBy Categories and its dropdowns, New Category folder,New Saved Row
		// Button
		softAssert.assertTrue(labels.contains("New Category"), "Failed to verify New category title");
		logger.info("New category title displayed");
		TestListeners.extentTest.get().info("New category title displayed");
		softAssert.assertTrue(labels.contains("Sort categories by"), " Failed to verify Sort categories by ");
		logger.info("Sort categories by drop down button displayed");
		TestListeners.extentTest.get().info("Sort categories by drop down button displayed");
		softAssert.assertTrue(labels.contains("Alphabetical A-Z"), "Failed to verify Alphabetical A-Z");
		logger.info("Drop down's  Alphabetical A-Z web element displayed");
		TestListeners.extentTest.get().info("Drop down's  Alphabetical A-Z web element displayed");
		softAssert.assertTrue(labels.contains("Alphabetical Z-A"), "Failed to verify Alphabetical Z-A");
		logger.info("Drop down's  Alphabetical Z-A web element displayed");
		TestListeners.extentTest.get().info("Drop down's  Alphabetical Z-A web element displayed");
		softAssert.assertTrue(labels.contains("Most Recent"), "Failed to verify Most Recent");
		logger.info("Drop down's  Most Recent element displayed");
		TestListeners.extentTest.get().info("Drop down's  Most Recent element displayedweb element displayed");
		softAssert.assertTrue(labels.contains("Default"), "Failed to verify Default");
		logger.info("Drop down's  Default element displayed");
		TestListeners.extentTest.get().info("Drop down's  Default element web element displayed");
		// create save row
		pageObj.contentLibraryPage().createEditRenameSaveFolderForRow(emailTemplateName);
		pageObj.contentLibraryPage().clickOnNewSavedRowButton();
		String NewRowTitle = pageObj.mobileconfigurationPage().getPageSource();
		softAssert.assertTrue(NewRowTitle.contains("New Row"), "Failed to verify New Row title");
		pageObj.contentLibraryPage().AddSixBlocksToSplitedRow(dataSet.get("youTubeVideoLink"));
		TestListeners.extentTest.get().pass("Six block added successfully ot the splitted row");
		pageObj.contentLibraryPage().clickOutSideOfRowElement();
		pageObj.contentLibraryPage().clickOnSaveRow();
		String rowName = "rowtemp";
		rowName = rowName.replace("temp", timstamp);
		pageObj.contentLibraryPage().fillUpTheRowsaveFormAndsave("Name", "Tags", rowName, rowName);
		pageObj.contentLibraryPage().backToContentLibrary();
		// navigate to email template
		pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");
		pageObj.emailTemplatePage().createNewEmailTemplate(emailTemplateName);
		pageObj.contentLibraryPage().addSavedRowBlock();
		pageObj.contentLibraryPage().attachTheSavedRowToEmailTemplate(dataSet.get("searchRow"), rowName);
		pageObj.emailTemplatePage().deleteTemplate(emailTemplateName);
		TestListeners.extentTest.get().pass("Saved rows attached to email template");
		// navigate to content Library
		pageObj.menupage().navigateToSubMenuItem("Settings", "Content Library");
		pageObj.contentLibraryPage().deleteRow(dataSet.get("searchRow"), rowName);
		pageObj.contentLibraryPage().deleteTheFolder();
		TestListeners.extentTest.get().pass("Folder deleted");
		softAssert.assertAll();
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