/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains permission related UI test cases for the POS Control Center.
* @fileName ppccPermissionsTest.java
*/

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.time.Duration;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ppccPermissionsTest {
    static Logger logger = LogManager.getLogger(ppccPermissionsTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {

        prop = Utilities.loadPropertiesFile("config.properties");
        env = prop.getProperty("environment");
        driver = new BrowserUtilities().launchBrowser();
        sTCName = method.getName();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        logger.info(sTCName + " ==>" + dataSet);
    }

    @Test(description = "SQ_T6813 Verify the actions view only user can perform with premium PPCC enabled", groups = {
            "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6813_VerifyTheActionsViewOnlyUserCanPerformWithPremiumPPCCEnabled() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), pageObj.utils().decrypt(dataSet.get("password")));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        String headerText = pageObj.ppccUtilities().getText(pageObj.utils().getLocatorValue("ppccViewOnly.messageTitle"));
        Assert.assertEquals(headerText.trim(), dataSet.get("expectedHeaderText"), 
                   "Header text should match " + dataSet.get("expectedHeaderText"));
        TestListeners.extentTest.get().pass("Header title matched successfully");

        pageObj.utils().waitTillPagePaceDone();
        pageObj.ppccLocationPage().clickSelectAllCheckbox();
        boolean isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.downloadInitializationFileButton", "Download initialization file button");
        Assert.assertTrue(isElementPresentAndClickable, "Download initialization file button should be present and clickable");
        TestListeners.extentTest.get().pass("Download initialization file button is present and clickable");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        boolean isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.deprovisionButton", "Deprovision button");
        Assert.assertTrue(isElementNotPresent, "Deprovision button should not be present");
        TestListeners.extentTest.get().pass("Deprovision button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.changePolicyButton", "Change policy button");
        Assert.assertTrue(isElementNotPresent, "Change policy button should not be present");
        TestListeners.extentTest.get().pass("Change policy button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.remoteUpgradeButton", "Remote upgrade button");
        Assert.assertTrue(isElementNotPresent, "Remote upgrade button should not be present");
        TestListeners.extentTest.get().pass("Remote upgrade button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.moreActionsButton", "More actions button");
        Assert.assertTrue(isElementNotPresent, "More actions button should not be present");
        TestListeners.extentTest.get().pass("More actions button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.provisionButton", "Provision button");
        Assert.assertTrue(isElementNotPresent, "Provision button should not be present");
        TestListeners.extentTest.get().pass("Provision button is not present");

        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccPolicyPage.createPolicyButton", "Create policy button");
        Assert.assertTrue(isElementNotPresent, "Create policy button should not be present");
        TestListeners.extentTest.get().pass("Create policy button is not present");

        pageObj.ppccPackagePage().navigateToPackagesTab();
        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccPackagePage.viewReleaseNotes", "View Release notes button");
        Assert.assertTrue(isElementPresentAndClickable, "View Release notes button should be present and clickable");
        TestListeners.extentTest.get().pass("View Release notes button is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccPackagePage.downloadPackage", "Download package button");
        Assert.assertTrue(isElementPresentAndClickable, "Download package button should be present and clickable");
        TestListeners.extentTest.get().pass("Download package button is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccPackagePage.copyPackageLink", "Copy package link button");
        Assert.assertTrue(isElementPresentAndClickable, "Copy package link button should be present and clickable");
        TestListeners.extentTest.get().pass("Copy package link button is present and clickable");

        pageObj.ppccSettingsPage().navigateToSettingTab();
        pageObj.ppccSettingsPage().selectPosFromUnassignedPos();
        String messageLocator = pageObj.utils().getLocatorValue("ppccViewOnly.messageSettingsPage");
        String messageText = pageObj.ppccUtilities().getText(messageLocator);
        Assert.assertEquals(messageText.trim(), dataSet.get("expectedMessageInSettingsPage"), 
                   "Header text should match " + dataSet.get("expectedMessageInSettingsPage"));
        TestListeners.extentTest.get().pass("Message in settings page matched successfully");

        pageObj.ppccSettingsPage().closePopUp();
        pageObj.ppccSettingsPage().selectPosFromAssignedPos();
        messageText = pageObj.ppccUtilities().getText(messageLocator);
        Assert.assertEquals(messageText.trim(), dataSet.get("expectedMessageInSettingsPage"), 
                   "Header text should match " + dataSet.get("expectedMessageInSettingsPage"));
        TestListeners.extentTest.get().pass("Message in settings page matched successfully");
    }

    @Test(description = "SQ_T6814 Verify the actions superadmin can perform with basic PPCC enabled", groups = {"regression"}, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6814_VerifyTheActionsUserCanPerformWithBasicPPCCEnabled() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        String actualHeader = pageObj.ppccUtilities().getText(pageObj.utils().getLocatorValue("ppccViewOnly.messageTitle")).trim();
        String expectedHeader = dataSet.get("expectedHeaderText");
        Assert.assertEquals(actualHeader, expectedHeader, "Header text mismatch");
        TestListeners.extentTest.get().pass("Header title matched successfully");

        pageObj.utils().waitTillPagePaceDone();
        pageObj.ppccLocationPage().clickSelectAllCheckbox();
        boolean isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.downloadInitializationFileButton", "Download initialization file button");
        Assert.assertTrue(isElementPresentAndClickable, "Download initialization file button should be present and clickable");
        TestListeners.extentTest.get().pass("Download initialization file button is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.deprovisionButton", "Deprovision button");
        Assert.assertTrue(isElementPresentAndClickable, "Deprovision button should be present and clickable");
        TestListeners.extentTest.get().pass("Deprovision button is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.changePolicyButton", "Change policy button");
        Assert.assertTrue(isElementPresentAndClickable, "Change policy button should be present and clickable");
        TestListeners.extentTest.get().pass("Change policy button is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.moreActionsButton", "More actions button");
        Assert.assertTrue(isElementPresentAndClickable, "More actions button should be present and clickable");
        TestListeners.extentTest.get().pass("More actions button is present and clickable");

        boolean isPremium = pageObj.ppccUtilities().verifyPremiumTooltip(
            pageObj.utils().getLocatorValue("ppccLocationPage.remoteUpgradeIconForBasicPPCC"),
            pageObj.utils().getLocatorValue("ppccLocationPage.remoteUpgradePremiumHeader"),
            dataSet.get("exclusivePremiumPPCCText"),
            "Remote upgrade action");
        Assert.assertTrue(isPremium, "Remote upgrade action should show premium tooltip");
        TestListeners.extentTest.get().pass("Remote upgrade action shows premium tooltip");

        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccPolicyPage.createPolicyButton", "Create policy button");
        Assert.assertTrue(isElementPresentAndClickable, "Create policy button should be present and clickable");
        TestListeners.extentTest.get().pass("Create policy button is present and clickable");

        isPremium = pageObj.ppccUtilities().verifyPremiumTooltip(
            pageObj.utils().getLocatorValue("ppccPackagePage.packagesTab"),
            pageObj.utils().getLocatorValue("ppccLocationPage.premiumPackageHeader"),
            dataSet.get("exclusivePremiumPPCCText"),
            "Packages tab");
        Assert.assertTrue(isPremium, "Packages tab should show premium tooltip");
        TestListeners.extentTest.get().pass("Packages tab shows premium tooltip");

        pageObj.ppccSettingsPage().navigateToSettingTab();
        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccSettingsPage.itemEntryInSelectPosColumn", "POS from select pos column");
        Assert.assertTrue(isElementPresentAndClickable, "POS from select pos column should be present and clickable");
        TestListeners.extentTest.get().pass("POS from select pos column is present and clickable");

        isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccSettingsPage.itemEntryInSelectedPosColumn", "POS from selected pos column");
        Assert.assertTrue(isElementPresentAndClickable, "POS from selected pos column should be present and clickable");
        TestListeners.extentTest.get().pass("POS from selected pos column is present and clickable");
    }

    @Test(description = "SQ_T6815 Verify the actions view only user can perform with basic PPCC enabled", groups = {"regression"}, priority = 1)
    @Owner(name = "Aman Jain")
    public void SQ_T6815_VerifyTheActionsViewOnlyUserCanPerformWithBasicPPCCEnabled() throws InterruptedException {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), pageObj.utils().decrypt(dataSet.get("password")));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        String headerText = pageObj.ppccUtilities().getText(pageObj.utils().getLocatorValue("ppccViewOnly.messageTitle"));
        Assert.assertEquals(headerText.trim(), dataSet.get("expectedHeaderText"), 
                   "Header text should match " + dataSet.get("expectedHeaderText"));
        TestListeners.extentTest.get().pass("Header title matched successfully");

        pageObj.utils().waitTillPagePaceDone();
        pageObj.ppccLocationPage().clickSelectAllCheckbox();
        boolean isElementPresentAndClickable = pageObj.ppccUtilities().isElementPresentAndClickable("ppccLocationPage.downloadInitializationFileButton", "Download initialization file button");
        Assert.assertTrue(isElementPresentAndClickable, "Download initialization file button should be present and clickable");
        TestListeners.extentTest.get().pass("Download initialization file button is present and clickable");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        boolean isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.deprovisionButton", "Deprovision button");
        Assert.assertTrue(isElementNotPresent, "Deprovision button should not be present");
        TestListeners.extentTest.get().pass("Deprovision button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.changePolicyButton", "Change policy button");
        Assert.assertTrue(isElementNotPresent, "Change policy button should not be present");
        TestListeners.extentTest.get().pass("Change policy button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.remoteUpgradeButton", "Remote upgrade button");
        Assert.assertTrue(isElementNotPresent, "Remote upgrade button should not be present");
        TestListeners.extentTest.get().pass("Remote upgrade button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.moreActionsButton", "More actions button");
        Assert.assertTrue(isElementNotPresent, "More actions button should not be present");
        TestListeners.extentTest.get().pass("More actions button is not present");

        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccLocationPage.provisionButton", "Provision button");
        Assert.assertTrue(isElementNotPresent, "Provision button should not be present");
        TestListeners.extentTest.get().pass("Provision button is not present");

        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        isElementNotPresent = pageObj.ppccUtilities().isElementNotPresent("ppccPolicyPage.createPolicyButton", "Create policy button");
        Assert.assertTrue(isElementNotPresent, "Create policy button should not be present");
        TestListeners.extentTest.get().pass("Create policy button is not present");

        boolean isPremium = pageObj.ppccUtilities().verifyPremiumTooltip(
            pageObj.utils().getLocatorValue("ppccPackagePage.packagesTab"),
            pageObj.utils().getLocatorValue("ppccLocationPage.premiumPackageHeader"),
            dataSet.get("exclusivePremiumPPCCText"),
            "Packages tab");
        Assert.assertTrue(isPremium, "Packages tab should show premium tooltip");
        TestListeners.extentTest.get().pass("Packages tab shows premium tooltip");

        pageObj.ppccSettingsPage().navigateToSettingTab();
        pageObj.ppccSettingsPage().selectPosFromUnassignedPos();
        String messageLocator = pageObj.utils().getLocatorValue("ppccViewOnly.messageSettingsPage");
        String messageText = pageObj.ppccUtilities().getText(messageLocator);
        Assert.assertEquals(messageText.trim(), dataSet.get("expectedMessageInSettingsPage"), 
                   "Header text should match " + dataSet.get("expectedMessageInSettingsPage"));
        TestListeners.extentTest.get().pass("Message in settings page matched successfully");

        pageObj.ppccSettingsPage().closePopUp();
		pageObj.newSegmentHomePage().segmentAdvertiseBlock();
        pageObj.ppccSettingsPage().selectPosFromAssignedPos();
        messageText = pageObj.ppccUtilities().getText(messageLocator);
        Assert.assertEquals(messageText.trim(), dataSet.get("expectedMessageInSettingsPage"), 
                   "Header text should match " + dataSet.get("expectedMessageInSettingsPage"));
        TestListeners.extentTest.get().pass("Message in settings page matched successfully");
    }

    @AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
