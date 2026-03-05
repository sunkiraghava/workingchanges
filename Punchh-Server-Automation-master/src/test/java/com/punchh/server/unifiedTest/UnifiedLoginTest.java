/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the Unified Dashboard login functionality.
 * @fileName unifiedLoginTest.java
 */

package com.punchh.server.unifiedTest;

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
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class UnifiedLoginTest {
    static Logger logger = LogManager.getLogger(UnifiedLoginTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl,unifiedDashboardUrl;
    private static Map<String, String> dataSet;
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        prop = Utilities.loadPropertiesFile("config.properties");
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        sTCName = method.getName();
        dataSet = new ConcurrentHashMap<>();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        logger.info(sTCName + " ==>" + dataSet);
        unifiedDashboardUrl = Utilities.getConfigProperty(env + ".unifiedDashboardBaseUrl");
        driver.get(unifiedDashboardUrl);
    }
    @Test(description = "T7198 Verify the login screen of unified dashboard")
    @Owner(name = "Kalpana")
    public void T7198_verifyLoginScreenOfUnifiedDashboard() throws Exception {
        boolean isNavigationToUnifiedDashboardSuccessful = pageObj.unifiedLoginPage().isTextAndLogoPresent("unifiedLoginPage.welcomeStaticText","unifiedLoginPage.parCenterLogo");
        Assert.assertTrue(isNavigationToUnifiedDashboardSuccessful,
                "Welcome text or Logo image is not present on the Unified Login page"
        );
        pageObj.utils().logPass("Navigation to Unified Dashboard login page is successful and welcome text and logo are present");
    }
    @Test(description = "T7199 Verify the successful login after entering email and password on unified dashboard")
    @Owner(name = "Kalpana")
    public void T7199_verifySuccessfulLoginAfterEnteringEmailAndPasswordOnUnifiedDashboard() throws Exception {
        pageObj.unifiedLoginPage().loginToUnifiedDashboard(dataSet.get("email"),dataSet.get("password"));
        boolean isLoginSuccessful = pageObj.unifiedLoginPage().isTextAndLogoPresent("unifiedLoginPage.postLoginText","unifiedLoginPage.postLoginLogoImage");
        Assert.assertTrue(isLoginSuccessful,
                "Login was not successful on the Unified Dashboard"
        );
        pageObj.utils().logPass("Login was successful on the Unified Dashboard with valid credentials");
    }
    @Test(description = "T7200 Verify the login after entering invalid email and invalid password on unified dashboard.")
    @Owner(name = "Kalpana")
    public void T7200_VerifyLoginAfterEnteringInvalidEmailAndInvalidPasswordOnUnifiedDashboard() throws Exception {
        pageObj.unifiedLoginPage().loginToUnifiedDashboard(dataSet.get("email"), dataSet.get("password"));
        String actualText =  pageObj.unifiedLoginPage().getErrorMessageOnInvalidCredentials();
        Assert.assertEquals(actualText, "Incorrect email address, username, or password", "User can access unified dashboard");
        pageObj.utils().logPass("Error message displayed for invalid users trying to login to unified dashboard.");
    }
    @Test(description = "Verify the LogOut on unified dashboard.")
    @Owner(name = "Kalpana")
    public void T7201_verifyLogOutOnUnifiedDashboard() throws Exception {
        pageObj.unifiedLoginPage().loginToUnifiedDashboard(dataSet.get("email"), dataSet.get("password"));
        pageObj.unifiedLoginPage().clickOnSignOutOnUnifiedDashboardHomeScreen();
        boolean isLogOutOnUnifiedDashboardSuccessful = pageObj.unifiedLoginPage().isTextAndLogoPresent("unifiedLoginPage.welcomeStaticText","unifiedLoginPage.parCenterLogo");
        Assert.assertTrue(isLogOutOnUnifiedDashboardSuccessful,
                "Sign Out is not present on the Unified Login page");
        pageObj.utils().logPass("SignOut from Unified Dashboard is successful.");
    }
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        logger.info("Data set cleared");
        driver.quit();
        logger.info("Browser closed");
    }
}

