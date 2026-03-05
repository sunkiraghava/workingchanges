/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location > Initiate Update functionality.
 * @fileName ppccLocationInitiateUpdateTest.java
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
public class ppccLocationInitiateUpdateTest {
    static Logger logger = LogManager.getLogger(ppccLocationInitiateUpdateTest.class);
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
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(pageObj.readData().getJsonFilePath("ui", env, "Secrets"),
            dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

    @Test(description = "SQ-T6185 Verify the flow of initiate update a location" +
    "SQ-T6341 Verify the audit log on performing initiate update the location")
    @Owner(name = "Kalpana")
    public void T6185_verifyTheFlowOfInitiateUpdateALocation()throws Exception {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        //create first policy using api
        String createdPolicy = pageObj.ppccLocationPage().createPolicy();
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String queryParam = "";
        Response createPolicyResponse = pageObj.endpoints().addPolicy(token, createdPolicy, 1, queryParam, "published");
        Assert.assertEquals(createPolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Status code 200 did not match for create Policy API");
        Assert.assertEquals(createPolicyResponse.jsonPath().get("data"), "Policy `" + createdPolicy + "` created successfully", "Messages do not match");
        Assert.assertEquals(createPolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
        Assert.assertEquals(createPolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Add Policy API executed successfully");
        pageObj.utils().logit("Get the policy id");
        queryParam = "?search=" + createdPolicy;
        Response policyListResponse = pageObj.endpoints().getPolicyList(token, queryParam);
        int policyId = policyListResponse.jsonPath().get("data[0].id");

        // create second policy that will be used for change policy action
        String changePolicy = pageObj.ppccLocationPage().createPolicy();
        Response changePolicyResponse = pageObj.endpoints().addPolicy(token, changePolicy, 1, queryParam, "published");
        Assert.assertEquals(changePolicyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Status code 200 did not match for create Policy API");
        Assert.assertEquals(changePolicyResponse.jsonPath().get("data"), "Policy `" + changePolicy + "` created successfully", "Messages do not match");
        Assert.assertEquals(changePolicyResponse.jsonPath().get("metadata"), Collections.emptyMap(), "Metadata has some value");
        Assert.assertEquals(changePolicyResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Add Policy API executed successfully");
        pageObj.utils().logit("Get the policy id");
        queryParam = "?search=" + changePolicy;
        Response changepolicyListResponse = pageObj.endpoints().getPolicyList(token, queryParam);
        int changepolicyId = changepolicyListResponse.jsonPath().get("data[0].id");

        // provision a location
        pageObj.ppccLocationPage().searchLocationsInDeprovisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().provisionALocation(createdPolicy, dataSet.get("packageVersion"));
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // perform change policy action
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().changePolicyALocation(changePolicy);
        pageObj.utils().logPass("Policy is changed for provisioned location successfully");
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // perform cancel update action
        pageObj.ppccLocationPage().cancelUpdateALocation();
        pageObj.utils().logPass("Cancel Update action is performed successfully");
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // perform initiate update action
        pageObj.ppccLocationPage().InitiateUpdateALocation();
        pageObj.utils().logPass("Initiate Update action is performed successfully");
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // validating UI column app version and policy status before heartbeat api is sent
        String policyNameBeforeSync=pageObj.ppccLocationPage().getPolicyNameofLocation();
        Assert.assertEquals(changePolicy, policyNameBeforeSync, "Policy is not correct on Provisioned Location");
        pageObj.utils().logPass("Policy Name is correct after provisioning Location");
        String policyStatusBeforeSync=pageObj.ppccLocationPage().getPolicyStatusOfLocation();
        Assert.assertEquals(policyStatusBeforeSync, "Pending Update", "Policy Status is not correct on Provisioned Location");
        pageObj.utils().logPass("Policy Status is correct after provisioning Location");
        String packageNameBeforeSync=pageObj.ppccLocationPage().getPackageNameofLocation();
        Assert.assertEquals(packageNameBeforeSync, "--", "Package Name is not correct on Provisioned Location");
        pageObj.utils().logPass("Package Name is correct after provisioning Location");
        String packageStatusBeforeSync=pageObj.ppccLocationPage().getPackageStatusofLocation();
        Assert.assertEquals(packageStatusBeforeSync, "Ready to Install", "Package Status is not correct on Provisioned Location");
        pageObj.utils().logPass("Package Status is correct after provisioning Location");

        //Sending heartbeat api without matching package details and policy last updated at
        Response heartbeatApiResponse = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"), dataSet.get("packageVersionRandom"), dataSet.get("packageVersionIdRandom"), dataSet.get("lastUpdatedAtRandom"));
        Assert.assertEquals(heartbeatApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Status code 200 did not match for heartbeat API");
        String isPolicyChanged = heartbeatApiResponse.getHeader("is-policy-changed");
        String isPackageChanged = heartbeatApiResponse.getHeader("is-package-changed");
        String remoteUpgrade = heartbeatApiResponse.getHeader("remote-upgrade");
        pageObj.utils().logit("Policy Status is : " + isPolicyChanged);
        pageObj.utils().logit("Package Status is : " + isPackageChanged);
        pageObj.utils().logit("Remote Upgrade Status for a location is : " + remoteUpgrade);
        Assert.assertEquals(remoteUpgrade, "True", "Location is not provisioned with remote upgrade enabled");
        Assert.assertEquals(isPolicyChanged, "True", "Is Policy Changed returned in header as not matching with policy");
        Assert.assertEquals(isPackageChanged, "True", "Is Package Changed returned in header as not matching with package");
        pageObj.utils().logPass("Verified headers in heartbeat api for provisioned location at 1st time without assigning package and policy details");

        //sending fetch config api to get the last updated at
        Response fetchConfigResponse=pageObj.endpoints().fetchConfig(dataSet.get("locationKey"));
        Assert.assertEquals(fetchConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Status code 200 did not match for Fetch configuration API");
        String lastUpdatedAt = fetchConfigResponse.jsonPath().get("data.last_updated_at");
        pageObj.utils().logit("Last Update at is fetched :  " + lastUpdatedAt);
        pageObj.utils().logit("Last Updated at of policy is successfully taken");
        pageObj.utils().logPass("Verified fetch config api");

        // sending heartbeat api with matching package details and policy details to sync the location
        Response heartbeatApiResponseForSync = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"), dataSet.get("packageVersion"), dataSet.get("packageVersionId"), lastUpdatedAt);
        Assert.assertEquals(heartbeatApiResponseForSync.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
        "Status code 200 did not match for heartbeat API");
        String isPolicyChangedAtSync = heartbeatApiResponseForSync.getHeader("is-policy-changed");
        String isPackageChangedAtSync = heartbeatApiResponseForSync.getHeader("is-package-changed");
        String remoteUpgradeAtSync = heartbeatApiResponseForSync.getHeader("remote-upgrade");
        pageObj.utils().logit("Policy Status is : " + isPolicyChangedAtSync);
        pageObj.utils().logit("Package Status is : " + isPackageChangedAtSync);
        pageObj.utils().logit("Remote Upgrade Status for a location is : " + remoteUpgradeAtSync);
        Assert.assertEquals(remoteUpgradeAtSync, "True", "Location is not provisioned with remote upgrade enabled");
        Assert.assertNull(isPolicyChangedAtSync, "Is Policy Changed returned in header as not matching with policy");
        Assert.assertNull(isPackageChangedAtSync, "Is Package Changed returned in header as not matching with package");
        pageObj.utils().logPass("Verified heartbeat api for provisioned location with assigned package and policy");
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        // validating the app version and policy status column after syncing the location with heartbeat api
        String policyNameAfterSync=pageObj.ppccLocationPage().getPolicyNameofLocation();
        Assert.assertEquals(changePolicy, policyNameAfterSync, "Policy is not correct on Provisioned Location After Sync");
        pageObj.utils().logPass("Policy Name is correct after provisioning Location after Sync");
        String policyStatusAfterSync=pageObj.ppccLocationPage().getPolicyStatusOfLocation();
        Assert.assertEquals(policyStatusAfterSync, "Synced", "Policy Status is not correct on Provisioned Location After Sync");
        pageObj.utils().logPass("Policy Status is correct after provisioning Location After Sync");
        String packageNameAfterSync=pageObj.ppccLocationPage().getPackageNameofLocation();
        Assert.assertEquals(dataSet.get("packageVersion"), packageNameAfterSync, "Package Name is not correct on Provisioned Location After Sync");
        pageObj.utils().logPass("Package Name is correct after provisioning Location After Sync");
        String packageStatusAfterSync=pageObj.ppccLocationPage().getPackageStatusofLocation();
        Assert.assertEquals(packageStatusAfterSync, "Synced", "Package Status is not correct on Provisioned Location After Sync");
        pageObj.utils().logPass("Package Status is correct after provisioning Location After Sync");
        pageObj.utils().logPass("E2E flow of Initiate Update a Location is verified successfully");

        // verify the columns of audit log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();
        pageObj.ppccLocationAuditLogPage().searchInAuditLog(dataSet.get("locationName"));

        String storeName = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 1);
        Assert.assertEquals(storeName, dataSet.get("locationName"), "StoreName do not exists in audit logs");
        pageObj.utils().logPass("StoreName is correct in audit Log");

        String status = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 3);
        Assert.assertEquals(status, "Pending Update", "Status do not exists in audit logs");
        pageObj.utils().logPass("Status is correct in audit Log");

        String userName = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 5);
        Assert.assertEquals(userName, "superadmin4@example.com", "UserName do not exists in audit logs");
        pageObj.utils().logPass("UserName is correct in audit Log");

        String eventType = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 6);
        Assert.assertEquals(eventType, "Initiate Update", "EventType do not exists in audit logs");
        pageObj.utils().logPass("EventType is correct in audit Log");

        String appVersion=pageObj.ppccLocationAuditLogPage().getAuditLogValueOfAppVersionColumn();
        Assert.assertEquals(appVersion, "Ready to Install", "appVersion do not exists in audit logs");
        pageObj.utils().logPass("appVersion is correct in audit Log");

        String eventDateTime=pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 7);

        //verify the audit log display details for Initiate Update Event
        pageObj.ppccLocationAuditLogPage().clickOnAuditLogRowToOpenLocationAuditLogDisplayDetails();

        String storeNameAuditLogDetails=pageObj.ppccLocationAuditLogPage().getStoreNameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(storeNameAuditLogDetails, dataSet.get("locationName"), "Store Name is not correct in audit logs display details");

        String policyIdAuditLogDetails=pageObj.ppccLocationAuditLogPage().getPolicyIdInLocationAuditLogDisplayDetails();
        Assert.assertEquals(policyIdAuditLogDetails, String.valueOf(changepolicyId), "Policy Id is not correct in audit logs display details");

        String statusAuditLogDetails=pageObj.ppccLocationAuditLogPage().getStatusInLocationAuditLogDisplayDetails();
        Assert.assertEquals(statusAuditLogDetails, "Pending Update", "Status is not correct in audit logs display details");

        String eventDateTimeAuditLogDetails=pageObj.ppccLocationAuditLogPage().getEventDateTimeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventDateTimeAuditLogDetails, eventDateTime, "Event Date Time is not correct in audit logs display details");

        String usernameAuditLogDetails=pageObj.ppccLocationAuditLogPage().getUsernameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(usernameAuditLogDetails, userName, "UserName is not correct in audit logs display details");

        String eventTypeTimeAuditLogDetails=pageObj.ppccLocationAuditLogPage().getEventTypeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventTypeTimeAuditLogDetails, "Initiate Update", "Event Type is not correct in audit logs display details");

        //verify the audit log display details Change Log for Initiate Update Event
        Map<String, String> configStatusChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Config Status");
        Assert.assertEquals(configStatusChangeLog.get("oldValue"), "Updates Cancelled", "Old value Config Status mismatch");
        Assert.assertEquals(configStatusChangeLog.get("newValue"), "Pending Update", "New value Config Status mismatch");
        pageObj.utils().logPass("Config Status is verified successfully");

        pageObj.ppccLocationAuditLogPage().clickCancelButtonOnLocationAuditLogDisplayDetails();
        pageObj.ppccLocationAuditLogPage().clickBackButtonOnLocationAuditLog();

        //deprovision a location
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().deProvisionALocation();
        pageObj.utils().logPass("Location is deprovisioned successfully");

        //delete created policy using api
        pageObj.utils().logit("Delete the policy");
        Response deleteApiResponse = pageObj.endpoints().deletePolicy(token, policyId);
        Assert.assertEquals(deleteApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for delete policy API");
        Assert.assertEquals(deleteApiResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Created Policy is deleted Successfully.");
        Response deletechangepolicyApiResponse = pageObj.endpoints().deletePolicy(token, changepolicyId);
        Assert.assertEquals(deletechangepolicyApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for delete policy API");
        Assert.assertEquals(deletechangepolicyApiResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Created Policy is deleted Successfully.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        pageObj.utils().clearDataSet(dataSet);
        pageObj.utils().logit("Data set cleared");
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        driver.quit();
        pageObj.utils().logit("Browser closed");
    }
}
