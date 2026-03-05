/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location tab> override and location group.
 * @fileName ppccLocationTest.java
 */
package com.punchh.server.ppccTest;

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

import io.restassured.response.Response;
@Listeners(TestListeners.class)
public class ppccLocationTest {
    static Logger logger = LogManager.getLogger(ppccLocationTest.class);
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

    @Test(description = "SQ-T6275 Verify the listing of Location group in filter")
    @Owner(name = "Kalpana")
    public void T6275_verifyTheListingOfLocationGroupInFilter() throws InterruptedException{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();

        String filterOption = dataSet.get("filterOption");

        // sending punchh-server Get Location Group List API.
        Response getLocationGroupListresponse = pageObj.endpoints().getLocationGroupList(dataSet.get("adminAuthorization"));
        Assert.assertEquals(getLocationGroupListresponse.getStatusCode(), 200, "Status code 200 did not match for Get Location Group List API");
        pageObj.utils().logPass("API response is received successfully for Get Location Group List API");
        Map<String, List<String>> locationGroupDataFromApi = pageObj.ppccLocationPage().saveListOfLocationGroupWithStoreNumber(getLocationGroupListresponse);
        List<String> locationGroupPresentInFilter = pageObj.ppccLocationPage().getAllLocationGroupFromFilter(filterOption);

        driver.navigate().refresh();
        pageObj.utils().waitTillPagePaceDone();

       //validating the Location Groups and Locations in each group
        boolean isLocationGroupValidated=  pageObj.ppccLocationPage().validateLocationGroupsInFilter(locationGroupDataFromApi,locationGroupPresentInFilter,filterOption);
        Assert.assertTrue(isLocationGroupValidated, "Location Group is not validated successfully");
        pageObj.utils().logPass("All Location Groups are validated successfully in Filter");
    }
    @Test(description = "SQ-T6183 Verify the presence of Has overrides Filter")
    @Owner(name = "Kalpana")
    public void T6183_verifyThePresenceOfHasOverridesFilter() throws InterruptedException{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccLocationPage().navigateToLocationsTab();
        String filterOption = dataSet.get("filterOption");
        boolean isFilterPresent=  pageObj.ppccLocationPage().isFilterPresent(filterOption);
        Assert.assertTrue(isFilterPresent, "Filter is not present : " + filterOption);
        pageObj.utils().logPass("Newly Added Filter is available in Filter: " + filterOption);
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


