/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains API test cases for the Unified Dashboard APIs.
 * @fileName UnifiedServiceAPITest.java
 */

package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.Assert;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;
 
@Listeners(TestListeners.class)
public class UnifiedServiceAPITest {
    static Logger logger = LogManager.getLogger(UnifiedServiceAPITest.class);
    public WebDriver driver;
    PageObj pageObj;
    String sTCName;
    String run = "api";
    private String env = "api";
    private String unifiedDashboardUrl;
    private static Map<String, String> dataSet;

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        dataSet = new ConcurrentHashMap<>();
        unifiedDashboardUrl = Utilities.getConfigProperty(env + ".unifiedDashboardBaseUrl");
        sTCName = method.getName();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
        dataSet = pageObj.readData().readTestData;
        logger.info(sTCName + " ==>" + dataSet);
        driver.get(unifiedDashboardUrl);
    }

    @Test(description = "Verify The Tenant Access On Unified Dashboard")
    public void verifyTheTenantAccessOnUnifiedDashboard() throws Exception {
        pageObj.unifiedLoginPage().loginToUnifiedDashboard(dataSet.get("emailId"), dataSet.get("password"));
        String udSessionCookie = pageObj.utils().getCookieByName("ud_session");
        Response response = pageObj.endpoints().getUnifiedTenantsAccess(udSessionCookie);
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 200 but got " + response.getStatusCode());

        List<String> displayNamesFromAPI = pageObj.utils().getAllValuesFromJsonArrayByKey(response, "apps", "display_name");
        List<String> displayNamesFromUI = pageObj.unifiedDashboardPage().getAllApplicationNamesFromUI();

        Assert.assertEquals(displayNamesFromUI.size(), displayNamesFromAPI.size(), 
            "Number of display names from UI (" + displayNamesFromUI.size() + 
            ") does not match API (" + displayNamesFromAPI.size() + ")");

        for (String apiDisplayName : displayNamesFromAPI) {
            Assert.assertTrue(displayNamesFromUI.contains(apiDisplayName), 
                "Display name '" + apiDisplayName + "' from API not found in UI. UI has: " + displayNamesFromUI);
            logger.info("Verified display name '" + apiDisplayName + "' exists in UI");
        }
        TestListeners.extentTest.get().pass("The tiles are displayed as per the tenant access on Unified Dashboard");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        driver.quit();
        logger.info("Browser closed");
    }
}
