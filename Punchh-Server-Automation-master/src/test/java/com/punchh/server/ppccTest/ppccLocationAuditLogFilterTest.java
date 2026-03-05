/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location Audit Log > Filters using all columns.
 * @fileName ppccLocationTest.java
 */
package com.punchh.server.ppccTest;
import java.lang.reflect.Method;
import java.util.Collections;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ppccLocationAuditLogFilterTest {
    static Logger logger = LogManager.getLogger(ppccLocationAuditLogFilterTest.class);
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
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(pageObj.readData().getJsonFilePath("ui" , env , "Secrets"),
            dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

    @Test(description = "SQ-T6447 Verify the filter in Location Audit Log by Username")
    @Owner(name = "Kalpana")
    public void T6447_verifyTheFilterInLocationAuditLogByUserName() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        //create policy using api
        String createdPolicy = pageObj.ppccLocationPage().createPolicy();
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String queryParam = "";
        Response createPolicyResponse = pageObj.endpoints().addPolicy(token,createdPolicy,1,queryParam,"published");
        Assert.assertEquals(createPolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for create Policy API");
        Assert.assertEquals(createPolicyResponse.jsonPath().get("data"), "Policy `" + createdPolicy + "` created successfully", "Messages do not match");
        Assert.assertEquals(createPolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
        Assert.assertEquals(createPolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Add Policy API executed successfully");
        logger.info("Get the policy id");
        queryParam = "?search=" + createdPolicy;
        Response policyListResponse = pageObj.endpoints().getPolicyList(token, queryParam);
        int policyId = policyListResponse.jsonPath().get("data[0].id");

        // provision a location
        pageObj.ppccLocationPage().searchLocationsInDeprovisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().provisionALocation(createdPolicy,dataSet.get("packageVersion"));
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // navigate to Audit Log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

        // filter the same locations with other locations as well if returned
        String filterValue = dataSet.get("filterValue");
        String filterOption = "User Name";
        pageObj.ppccLocationAuditLogPage().applyFilterOnLocationsAuditLog(filterOption, filterValue);
        boolean isLocationsFilteredOnAuditLog= pageObj.ppccLocationAuditLogPage().isLocationsFilteredOnAuditLog(filterValue,filterOption);
        Assert.assertTrue(isLocationsFilteredOnAuditLog, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.ppccLocationAuditLogPage().clickBackButtonOnLocationAuditLog();

        //deprovision a location
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().deProvisionALocation();
        pageObj.utils().logPass("Location is deprovisioned successfully");

        //delete created policy using api
        logger.info("Delete the policy");
        Response deleteApiResponse = pageObj.endpoints().deletePolicy(token, policyId);
        Assert.assertEquals(deleteApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for delete policy API");
        Assert.assertEquals(deleteApiResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Created Policy is deleted Successfully.");
    }

   @Test(description = "SQ-T6449 Verify the filter in Location Audit Log by Storename")
   @Owner(name = "Kalpana")
    public void T6449_verifyTheFilterInLocationAuditLogByStorename() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        // navigate to Audit Log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

        // filter the same locations with other locations as well if returned
        String filterValue = dataSet.get("locationName");
        String filterOption = dataSet.get("filterOption");
        pageObj.ppccLocationAuditLogPage().applyFilterOnLocationsAuditLog(filterOption, filterValue);
        boolean isLocationsFilteredOnAuditLog= pageObj.ppccLocationAuditLogPage().isLocationsFilteredOnAuditLog(filterValue,filterOption);
        Assert.assertTrue(isLocationsFilteredOnAuditLog, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
    }

    @Test(description = "SQ-T6450 Verify the filter in Location Audit Log by Package Status")
    @Owner(name = "Kalpana")
    public void T6450_verifyTheFilterInLocationAuditLogByPackageStatus() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        // navigate to Audit Log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

        // filter the same locations with other locations as well if returned
        String filterValue = dataSet.get("filterValue");
        String filterOption = dataSet.get("filterOption");
        pageObj.ppccLocationAuditLogPage().applyFilterOnLocationsAuditLog(filterOption, filterValue);
        boolean isLocationsFilteredOnAuditLog= pageObj.ppccLocationAuditLogPage().isLocationsFilteredOnAuditLog(filterValue,filterOption);
        Assert.assertTrue(isLocationsFilteredOnAuditLog, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
    }

    @Test(description = "SQ-T6451 Verify the filter in Location Audit Log by Event Type")
    @Owner(name = "Kalpana")
    public void T6451_verifyTheFilterInLocationAuditLogByEventType() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        // navigate to Audit Log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

        // filter the same locations with other locations as well if returned
        String filterValue = dataSet.get("filterValue");
        String filterOption = dataSet.get("filterOption");
        pageObj.ppccLocationAuditLogPage().applyFilterOnLocationsAuditLog(filterOption, filterValue);
        boolean isLocationsFilteredOnAuditLog= pageObj.ppccLocationAuditLogPage().isLocationsFilteredOnAuditLog(filterValue,filterOption);
        Assert.assertTrue(isLocationsFilteredOnAuditLog, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
    }

    @Test(description = "SQ-T6452 Verify the filter in Location Audit Log by Policy Status")
    @Owner(name = "Kalpana")
    public void T6452_verifyTheFilterInLocationAuditLogByPolicyStatus() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        // navigate to Audit Log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();

        // filter the same locations with other locations as well if returned
        String filterValue = dataSet.get("filterValue");
        String filterOption = dataSet.get("filterOption");
        pageObj.ppccLocationAuditLogPage().applyFilterOnLocationsAuditLog(filterOption, filterValue);
        boolean isLocationsFilteredOnAuditLog= pageObj.ppccLocationAuditLogPage().isLocationsFilteredOnAuditLog(filterValue,filterOption);
        Assert.assertTrue(isLocationsFilteredOnAuditLog, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
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
