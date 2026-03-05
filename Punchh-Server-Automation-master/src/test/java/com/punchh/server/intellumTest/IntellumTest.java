/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains UI test cases for the intellum module.
 * @fileName IntellumTest.java
 */

package com.punchh.server.intellumTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
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
public class IntellumTest {
    static Logger logger = LogManager.getLogger(IntellumTest.class);
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;

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

    @Test(description = "SQ-T6903 Verify the user is accessing PAR academy powered by Intellum", groups = { "regression" }, priority = 1)
    @Owner(name = "Aman Jain")
    public void T6903_verifyTheNavigationOfParAcademy() throws InterruptedException {
        pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
        Assert.assertTrue(pageObj.intellumPage().isIntellumEnabled(), "Enable Intellum checkbox is not checked");

        Map<String, String> configValues = pageObj.intellumPage().getIntellumConfigValues();
        Assert.assertEquals(configValues.get("baseUrl"), dataSet.get("expectedUrl"), "Base URL mismatch");
        Assert.assertFalse(configValues.get("uid").isEmpty(), "Intellum UID value is empty");
        Assert.assertFalse(configValues.get("privateKey").isEmpty(), "Intellum Private Key value is empty");

        pageObj.dashboardpage().navigateToAllBusinessPage();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        boolean isLearnUponOptionPresent = pageObj.ppccUtilities().isElementNotPresent("intellumPage.learnUponAcademy", "Punchh University");
        Assert.assertTrue(isLearnUponOptionPresent, "Punchh University option is present, expected it to be absent.");

        pageObj.intellumPage().navigateToIntellumParAcademy();
        String actualURL = pageObj.intellumPage().getCurrentPageUrl();
        Assert.assertTrue(actualURL.contains(dataSet.get("expectedUrl")), "URL verification failed");

        List<String> actualNavOptions = pageObj.intellumPage().getNavBarTexts();
        Assert.assertEquals(actualNavOptions, dataSet.get("expectedNavbar"), "Navbar options mismatch");

        String actualTitle = pageObj.intellumPage().getPageTitle();
        Assert.assertEquals(actualTitle, dataSet.get("expectedTitle"), "Page title mismatch");
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