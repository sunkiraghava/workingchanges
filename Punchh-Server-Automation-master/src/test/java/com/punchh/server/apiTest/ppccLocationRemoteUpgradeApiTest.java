/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains API test cases for the remote upgrade action API.
 * @fileName ppccLocationRemoteUpgradeApiTest.java
 */
package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.RestAssured;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ppccLocationRemoteUpgradeApiTest {
    static Logger logger = LogManager.getLogger(ppccLocationRemoteUpgradeApiTest.class);
    public WebDriver driver;
    PageObj pageObj;
    String sTCName;
    String run = "api";
    private String env = "api";
    private String baseUrl;
    private static Map<String, String> dataSet;

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        env = pageObj.getEnvDetails().setEnv();
        dataSet = new ConcurrentHashMap<>();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        sTCName = method.getName();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(pageObj.readData().getJsonFilePath("ui" , env , "Secrets"),
                dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
    }

    @Test(description =  "SQ-T6588 Verify the flow of remote upgrade api for a location", groups = { "regression" }, priority = 1)
    public void SQ_T6588_verifyTheFlowOfRemoteUpgradeApiForALocation() throws Exception {

        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));

       // creating the policy
       String status = "published";
        String policyName = pageObj.ppccUtilities().createPolicy(token, status);

        // getting the policy id
        int policyId = pageObj.ppccUtilities().getPolicyId(policyName, token);

        // getting the unprovisioned location's ids
        String storeName = dataSet.get("locationName");
        int locationId = pageObj.ppccUtilities().getLocationId(storeName, false, token);
        List<Integer> locationIds = Arrays.asList(locationId);

        // provisioning the location
        logger.info("Provision the location");
        Response provisionApiResponse = pageObj.endpoints().provisionApi(token, policyId, locationIds, dataSet.get("packageVersionId"));
        Assert.assertEquals(provisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        TestListeners.extentTest.get().pass("Provision location is giving 200 status code");

       // Perform Remote Upgrade action
       Response remoteUpgradeApiResponse = pageObj.endpoints().remoteUpgrade(token, locationIds, dataSet.get("packageVersionIdforRemoteUpgrade"));
       Assert.assertEquals(remoteUpgradeApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for Remote Upgrade API");
        TestListeners.extentTest.get().pass("Remote Upgrade API is giving 200 status code");

        // checking audit logs
        logger.info("Checking audit logs for change policy");
        String queryParam = "?location_id=" + locationId;
        Response locationListAuditLogsResponse = pageObj.endpoints().getLocationListAuditLogs(token, queryParam);
        Assert.assertEquals(locationListAuditLogsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for get location audit logs API");
        int lastIndex = locationListAuditLogsResponse.jsonPath().getList("data").size() - 1;
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].event_type_display"), "Remote Upgrade", "Event type display does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].config_status_display"), "Ready to Install", "Config status display does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].package_status_display"), "Pending Update", "Package status display does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getInt("data[" + lastIndex + "].location_id"), locationId, "Location ID does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].store_name"), storeName, "Store name does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].event_type"), "remote_upgrade", "Event type does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].username"), "testAutomation@ppcc.com", "Username does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].email_address"), "testAutomation@ppcc.com", "Email address does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].config_status"), "ready_to_install", "Config status does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].package_status"), "pending_update", "Package status does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].location_package_version_id"), dataSet.get("packageVersionIdforRemoteUpgrade"), "Location package version ID does not match");
        Assert.assertEquals(locationListAuditLogsResponse.jsonPath().getString("data[" + lastIndex + "].store_app_version"), null, "Store app version should be null");
        Assert.assertTrue(locationListAuditLogsResponse.jsonPath().getList("errors").isEmpty(), "Errors list is not empty");
        TestListeners.extentTest.get().pass("Get location audit logs is giving 200 status code");

        // After Change Policy, validating app version and policy status before heartbeat api is sent
        queryParam = "?is_provisioned=True&search=" + storeName;
        Response locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        Assert.assertEquals(policyName, locationListResponse.jsonPath().get("data[0].policy.name"), "Policy is not correct on Provisioned Location");
        Assert.assertEquals(locationListResponse.jsonPath().get("data[0].config_status_display"), "Ready to Install", "Policy Status is not correct on Provisioned Location");
        Assert.assertNull(locationListResponse.jsonPath().get("data[0].store_app_version"), "Package Name is not correct on Provisioned Location");
        Assert.assertEquals(locationListResponse.jsonPath().get("data[0].package_status_display"),"Pending Update", "Package Status is not correct on Provisioned Location");

        //Sending heartbeat api without matching package details and policy last updated at
        Response heartbeatApiResponse = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"),dataSet.get("packageVersionRandom"),dataSet.get("packageVersionIdRandom"),dataSet.get("lastUpdatedAtRandom"));
        Assert.assertEquals(heartbeatApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for heartbeat API");
        String isPolicyChanged = heartbeatApiResponse.getHeader("is-policy-changed");
        String isPackageChanged = heartbeatApiResponse.getHeader("is-package-changed");
        String remoteUpgrade = heartbeatApiResponse.getHeader("remote-upgrade");
        logger.info("Policy Status is : " + isPolicyChanged);
        logger.info("Package Status is : " + isPackageChanged);
        logger.info("Remote Upgrade Status for a location is : " + remoteUpgrade);
        Assert.assertEquals(remoteUpgrade,"True","Location is not provisioned with remote upgrade enabled");
        Assert.assertEquals(isPolicyChanged,"True","Is Policy Changed returned in header as not matching with policy");
        Assert.assertEquals(isPackageChanged,"True","Is Package Changed returned in header as not matching with package");
        TestListeners.extentTest.get().pass("Verified headers in heartbeat api for provisioned location at 1st time without assigning package and policy details");

        //sending fetch config api to get the last updated at
        Response fetchConfigResponse=pageObj.endpoints().fetchConfig(dataSet.get("locationKey"));
        Assert.assertEquals(fetchConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for Fetch configuration API");
        String lastUpdatedAt = fetchConfigResponse.jsonPath().get("data.last_updated_at");
        logger.info("Last Update at is fetched :  " + lastUpdatedAt);
        logger.info("Last Updated at of policy is successfully taken");
        TestListeners.extentTest.get().pass("Verified fetch config api");

        // sending heartbeat api with matching package details and policy details to sync the location
        Response heartbeatApiResponseForSync = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"),dataSet.get("packageVersionforRemoteUpgrade"),dataSet.get("packageVersionIdforRemoteUpgrade"),lastUpdatedAt);
        Assert.assertEquals(heartbeatApiResponseForSync.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for heartbeat API");
        String isPolicyChangedAtSync = heartbeatApiResponseForSync.getHeader("is-policy-changed");
        String isPackageChangedAtSync = heartbeatApiResponseForSync.getHeader("is-package-changed");
        String remoteUpgradeAtSync = heartbeatApiResponseForSync.getHeader("remote-upgrade");
        logger.info("Policy Status is : " + isPolicyChangedAtSync);
        logger.info("Package Status is : " + isPackageChangedAtSync);
        logger.info("Remote Upgrade Status for a location is : " + remoteUpgradeAtSync);
        Assert.assertEquals(remoteUpgradeAtSync,"True","Location is not provisioned with remote upgrade enabled");
        Assert.assertNull(isPolicyChangedAtSync,"Is Policy Changed returned in header as not matching with policy");
        Assert.assertNull(isPackageChangedAtSync,"Is Package Changed returned in header as not matching with package");
        TestListeners.extentTest.get().pass("Verified heartbeat api for provisioned location with assigned package and policy");

        // validating the app version and policy status column after syncing the location with heartbeat api
        queryParam = "?is_provisioned=True&search=" + storeName;
        locationListResponse = pageObj.endpoints().getLocationList(token, queryParam);
        Assert.assertEquals(locationListResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for location list API");
        Assert.assertEquals(policyName, locationListResponse.jsonPath().get("data[0].policy.name"), "Policy is not correct on Provisioned Location");
        Assert.assertEquals(locationListResponse.jsonPath().get("data[0].config_status_display"), "Synced", "Policy Status is not correct on Provisioned Location");
        Assert.assertEquals(dataSet.get("packageVersionforRemoteUpgrade"),locationListResponse.jsonPath().get("data[0].store_app_version"),"Package Name is not correct on Provisioned Location After Sync");
        Assert.assertEquals(locationListResponse.jsonPath().get("data[0].package_status_display"),"Synced", "Package Status is not correct on Provisioned Location");

        // deprovisioning the location
        logger.info("Deprovision the location");
        Response deprovisionApiResponse = pageObj.endpoints().deprovisionApi(token, locationIds);
        Assert.assertEquals(deprovisionApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not match for provisioning API");
        TestListeners.extentTest.get().pass("Deprovision location is giving 200 status code");

        // deleting the first policy
       pageObj.ppccUtilities().deletePolicy(policyId, token);
    }

    @AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
