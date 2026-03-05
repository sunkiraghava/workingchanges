/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains UI test cases for the redirection of user from Punchh to Unified Dashboard functionality.
 * @fileName UnifiedRedirectionTest.java
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
public class UnifiedRedirectionTest {
    static Logger logger = LogManager.getLogger(UnifiedRedirectionTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl, unifiedDashboardUrl;
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
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        unifiedDashboardUrl = Utilities.getConfigProperty(env + ".unifiedDashboardBaseUrl");
        driver.get(baseUrl);
    }

    @Test(description = "EXP-T6, EXP-T7, EXP-T8, EXP-T22, Verify Complete Flow Of Redirection And Session Handling Between Punchh And Unified Dashboard")
    @Owner(name = "Aman Jain")
    public void EXP_T6_7_8_22_verifyCompleteRedirectionAndSessionHandlingFlow() throws Exception {
        // Step 0: Query login_provider from database for the user
        String userEmail = dataSet.get("email");
        String loginProviderDBColumnName = dataSet.get("loginProviderDBColumnName");
        String loginProvider = pageObj.unifiedDashboardPage().getValueFromAdminTable(userEmail, env, loginProviderDBColumnName);
        pageObj.utils().logit("Retrieved login_provider from database for user " + userEmail + ": " + loginProvider);
        Assert.assertNotNull(loginProvider, "login_provider should not be null for user: " + userEmail);
        Assert.assertEquals(loginProvider, "unified_dashboard", 
            "login_provider should be 'unified_dashboard' for user: " + userEmail + ", but found: " + loginProvider);
        pageObj.utils().logit("pass", "Successfully retrieved login_provider from database: " + loginProvider);
        
        // Step 1: Verify initial redirection from Punchh to Unified Dashboard
        pageObj.instanceDashboardPage().enterLoginCreds(dataSet.get("email"), dataSet.get("password"));
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.startsWith(dataSet.get("expectedUnifiedLoginUrl")), 
            "Current URL does not start with expected unified dashboard URL. Actual URL: " + currentUrl);
        TestListeners.extentTest.get().pass("Successfully redirected from Punchh to Unified Dashboard");
        
        // Step 2: Login to Unified Dashboard and verify successful login
        pageObj.unifiedLoginPage().loginToUnifiedDashboard(dataSet.get("email"), dataSet.get("password"));
        boolean isLoginSuccessful = pageObj.unifiedLoginPage().isTextAndLogoPresent("unifiedLoginPage.postLoginText","unifiedLoginPage.postLoginLogoImage");
        Assert.assertTrue(isLoginSuccessful, "Login was not successful on the Unified Dashboard");
        TestListeners.extentTest.get().pass("Login was successful on the Unified Dashboard with valid credentials");
        
        // Step 3: Click on application and verify redirection to Punchh in new tab
        pageObj.unifiedDashboardPage().clickOnApplicationByIndex(0);
        pageObj.utils().switchToNewOpenedWindow();
        String newTabUrl = driver.getCurrentUrl();
        Assert.assertTrue(newTabUrl.contains(dataSet.get("expectedPunchhUrl")), 
            "The URL of the newly opened tab does not match. Actual URL: " + newTabUrl);
        pageObj.utils().logPass("Successfully verified that the new tab URL contains " + dataSet.get("expectedPunchhUrl"));
        
        // Step 4: Verify session handling - Logout from Punchh and verify redirect to Unified Dashboard
        pageObj.utils().waitTillPagePaceDone();
        pageObj.dashboardpage().logoutApp();
        String unifiedUrl = driver.getCurrentUrl();
        Assert.assertTrue(unifiedUrl.contains(dataSet.get("expectedUnifiedUrl")), 
            "The URL after logout does not match expected Unified Dashboard URL. Actual URL: " + unifiedUrl);
        pageObj.utils().logPass("Successfully verified redirect to Unified Dashboard after Punchh logout");
        
        // Step 5: Verify user is still logged in to Unified Dashboard after Punchh logout
        boolean isPostNavigationSuccessful = pageObj.unifiedLoginPage().isTextAndLogoPresent("unifiedLoginPage.postLoginText","unifiedLoginPage.postLoginLogoImage");
        Assert.assertTrue(isPostNavigationSuccessful, "User is not logged in to Unified Dashboard after Punchh logout");
        TestListeners.extentTest.get().pass("User session maintained on Unified Dashboard after Punchh logout");
        
        // Step 6: Navigate back to Punchh and verify user is not logged in
        pageObj.instanceDashboardPage().instanceLogin(baseUrl);
        boolean isUserLoggedInPunchh = pageObj.dashboardpage().verifyUserIsLoggedIn();
        Assert.assertTrue(isUserLoggedInPunchh, "User is not logged in to Punchh after returning from Unified Dashboard");
        TestListeners.extentTest.get().pass("User session maintained on Punchh after returning from Unified Dashboard");
        
        // Step 7: Navigate back to Unified Dashboard, click on application, and logout from Unified Dashboard
        driver.get(unifiedDashboardUrl);
        pageObj.unifiedDashboardPage().clickOnApplicationByIndex(0);
        pageObj.unifiedDashboardPage().logoutFromUnifiedDashboard();
        
        // Step 8: Verify user is still logged in to Punchh after Unified Dashboard logout
        driver.get(baseUrl);
        boolean isUserLoggedInPunchhAfterUnifiedLogout = pageObj.dashboardpage().verifyUserIsLoggedIn();
        Assert.assertTrue(isUserLoggedInPunchhAfterUnifiedLogout, 
            "User is not logged in to Punchh after logging out from Unified Dashboard");
        TestListeners.extentTest.get().pass("User session maintained on Punchh after Unified Dashboard logout");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        logger.info("Data set cleared");
        driver.quit();
        logger.info("Browser closed");
    }
}

