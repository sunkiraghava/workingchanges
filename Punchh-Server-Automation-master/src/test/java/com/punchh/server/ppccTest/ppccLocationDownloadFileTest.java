/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Locations tab > Download Test
 * @fileName ppccLocationDownloadFileTest.java
 */

package com.punchh.server.ppccTest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class ppccLocationDownloadFileTest {
    static Logger logger = LogManager.getLogger(ppccLocationDownloadFileTest.class);
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

    @Test(description = "SQ-T6818 Verify the Download Functionality on Locations.")
    @Owner(name = "Kalpana")
    public void T6818_verifyTheDownloadFunctionalityOnLocations() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();

        // this is to test multiple locations download functionality
        pageObj.ppccLocationPage().selectAllCheckBoxAndClickOnDownloadIcon();
        String expectedDownloadFileNameForMultipleLocations = pageObj.ppccLocationPage().getFileNameOnMultipleLocationDownloadAction(dataSet.get("Business Name"));
        boolean isMultipleLocationsDownloadWorking = pageObj.ppccLocationPage().verifyLocationDownloadedFileAndDelete(expectedDownloadFileNameForMultipleLocations,".zip");
        Assert.assertTrue(isMultipleLocationsDownloadWorking, "File is not downloaded successfully.");
        pageObj.utils().logPass("File is downloaded successfully.");
        driver.navigate().refresh();

        // this is to test single locations download functionality
        pageObj.ppccLocationPage().clickOnFirstRowCheckBox();
        pageObj.ppccLocationPage().clickOnDownloadIcon();
        String expectedDownloadFileNameForSingleLocation = pageObj.ppccLocationPage().getFileNameOnSingleLocationDownloadAction();
        boolean isSingleLocationsDownloadWorking = pageObj.ppccLocationPage().verifyLocationDownloadedFileAndDelete(expectedDownloadFileNameForSingleLocation,".json");
        Assert.assertTrue(isSingleLocationsDownloadWorking, "File is not downloaded successfully.");
        pageObj.utils().logPass("File is downloaded successfully.");
    }

	@Test(description = "SQ-T6173 Verify that the actions for the PPCC package are clickable.", groups = {
			"regression" }, priority = 8)
	@Owner(name = "Aman Jain")
	public void SQ_T6173_verifyPPCCPackagesActionsAreWorking() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.ppccPolicyPage().enableHasPosIntegrationCheckBox();
		pageObj.ppccPolicyPage().checkEnablePosControlCenterCheckBoxWithPremium();
		pageObj.ppccPolicyPage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccPackagePage().navigateToPackagesTab();

		WebElement copyPackageLink = pageObj.ppccPackagePage().getCopyPackageLink("[1]");
		Assert.assertTrue(copyPackageLink.isDisplayed() && copyPackageLink.isEnabled(),
				"Copy Package button is not visible or enabled.");
		pageObj.utils().logPass("Copy Package button is visible and enabled.");

		WebElement downloadPackageButton = pageObj.ppccPackagePage().getDownloadPackage("[1]");
		Assert.assertTrue(downloadPackageButton.isDisplayed() && downloadPackageButton.isEnabled(),
				"Download Package button is not visible or enabled.");
		pageObj.utils().logPass("Download Package button is visible and enabled.");
		WebElement fileNameSelector = pageObj.utils().getLocator("ppccPackagePage.versionData");
		String fileName = fileNameSelector.getText();
		downloadPackageButton.click();
		pageObj.utils().longWaitInSeconds(10);

		pageObj.utils().logit("Downloaded file name: " + fileName + " file path " + new File(fileName).getAbsolutePath());
		String downloadDirPath = System.getProperty("user.dir") + "/resources/ExportData/";
		File downloadDir = new File(downloadDirPath);
		File[] matchingFiles = downloadDir.listFiles((dir, name) -> name.startsWith(fileName + ".zip"));
		Assert.assertNotNull(matchingFiles, "Download directory not found or is empty.");
		Assert.assertTrue(matchingFiles.length > 0, "No file found matching pattern: " + fileName + ".zip.*");
		pageObj.utils().logPass("File is downloaded successfully.");

		for (File file : matchingFiles) {
			boolean deleted = file.delete();
			Assert.assertTrue(deleted, "Failed to delete file: " + file.getName());
		}

		WebElement viewReleaseNotes = pageObj.ppccPackagePage().getViewReleaseNotes("[1]");
		Assert.assertTrue(viewReleaseNotes.isDisplayed() && viewReleaseNotes.isEnabled(),
				"View release notes button is not visible or enabled.");
		pageObj.utils().logPass("View release notes button is visible and enabled.");
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
