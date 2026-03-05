/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location > Deprovision functionality and location group.
 * @fileName ppccLocationDeprovisionTest.java
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
public class ppccLocationDeprovisionTest {
    static Logger logger = LogManager.getLogger(ppccLocationDeprovisionTest.class);
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

    @Test(description = "SQ-T6187 Verify the flow of deprovision a location"
    + "SQ-T6337 Verify the audit log on performing deprovisioning the location")
     @Owner(name = "Kalpana")
    public void T6187_verifyTheFlowOfDeprovisionALocation() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();

        //create first policy using api
        String createdPolicy = pageObj.ppccLocationPage().createPolicy();
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String queryParam = "";
        Response createPolicyResponse = pageObj.endpoints().addPolicy(token,createdPolicy,1,queryParam, "published");
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

        // provision a location
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().searchLocationsInDeprovisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().provisionALocation(createdPolicy,dataSet.get("packageVersion"));
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        //deprovision a location
        pageObj.ppccLocationPage().deProvisionALocation();
        pageObj.utils().logPass("Location is deprovisioned successfully");

        //sending fetch config api to get the last updated at
        Response fetchConfigResponse=pageObj.endpoints().fetchConfig(dataSet.get("locationKey"));
        Assert.assertEquals(fetchConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
                "Status code 400 did not match for Fetch configuration API");
        Assert.assertEquals(fetchConfigResponse.jsonPath().getString("errors[0]"),"Location is not provisioned with a policy.","De-provision is not successful");
        pageObj.utils().logit(fetchConfigResponse.jsonPath().getString("errors[0]"));
        pageObj.utils().logPass("Verified fetch config api");

        // sending heartbeat api with matching package details and policy details to sync the location
        Response heartbeatApiResponseForSync = pageObj.endpoints().heartbeatApi(dataSet.get("locationKey"),dataSet.get("packageVersion"),dataSet.get("packageVersionId"),dataSet.get("lastUpdatedAtRandom"));
        Assert.assertEquals(heartbeatApiResponseForSync.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
                "Status code 400 did not match for heartbeat API");
        Assert.assertEquals(heartbeatApiResponseForSync.jsonPath().getString("errors[0]"),"Location is not provisioned with a policy.","De-provision is not successful");
        pageObj.utils().logit(heartbeatApiResponseForSync.jsonPath().getString("errors[0]"));
        String isPolicyChangedAtSync = heartbeatApiResponseForSync.getHeader("is-policy-changed");
        String isPackageChangedAtSync = heartbeatApiResponseForSync.getHeader("is-package-changed");
        String remoteUpgradeAtSync = heartbeatApiResponseForSync.getHeader("remote-upgrade");
        pageObj.utils().logit("Policy Status: " + isPolicyChangedAtSync + ", Package Status: " + isPackageChangedAtSync + ", Remote Upgrade: " + remoteUpgradeAtSync);
        Assert.assertNull(remoteUpgradeAtSync,"Location is not provisioned with remote upgrade enabled");
        Assert.assertNull(isPolicyChangedAtSync,"Is Policy Changed returned in header as not matching with policy");
        Assert.assertNull(isPackageChangedAtSync,"Is Package Changed returned in header as not matching with package");
        pageObj.utils().logPass("Verified heartbeat api for provisioned location with assigned package and policy");
        pageObj.utils().logPass("E2E flow of Deprovision a Location is verified successfully");

        // verify the columns of audit log
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();
        pageObj.ppccLocationAuditLogPage().searchInAuditLog(dataSet.get("locationName"));

        String storeName = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1,1);
        Assert.assertEquals(storeName, dataSet.get("locationName"), "StoreName do not exists in audit logs");
        pageObj.utils().logPass("StoreName is correct in audit Log");

        String status = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1,3);
        Assert.assertEquals(status, "Unprovisioned", "Status do not exists in audit logs");
        pageObj.utils().logPass("Status is correct in audit Log");

        String userName = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1,5);
        Assert.assertEquals(userName, "superadmin4@example.com", "UserName do not exists in audit logs");
        pageObj.utils().logPass("UserName is correct in audit Log");

        String eventType = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1,6);
        Assert.assertEquals(eventType, "Deprovisioned", "EventType do not exists in audit logs");
        pageObj.utils().logPass("EventType is correct in audit Log");

        String appVersion=pageObj.ppccLocationAuditLogPage().getAuditLogValueOfAppVersionColumn();
        Assert.assertEquals(appVersion, "Unprovisioned", "appVersion do not exists in audit logs");
        pageObj.utils().logPass("appVersion is correct in audit Log");

        String eventDateTime=pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1,7);

        //verify the audit log display details for Deprovision Event
        pageObj.ppccLocationAuditLogPage().clickOnAuditLogRowToOpenLocationAuditLogDisplayDetails();

        String storeNameAuditLogDetails=pageObj.ppccLocationAuditLogPage().getStoreNameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(storeNameAuditLogDetails, dataSet.get("locationName"), "Store Name is not correct in audit logs display details");

        String policyIdAuditLogDetails=pageObj.ppccLocationAuditLogPage().getPolicyIdInLocationAuditLogDisplayDetails();
        Assert.assertEquals(policyIdAuditLogDetails, "--", "Policy Id is not correct in audit logs display details");

        String statusAuditLogDetails=pageObj.ppccLocationAuditLogPage().getStatusInLocationAuditLogDisplayDetails();
        Assert.assertEquals(statusAuditLogDetails, "Unprovisioned", "Status is not correct in audit logs display details");

        String eventDateTimeAuditLogDetails=pageObj.ppccLocationAuditLogPage().getEventDateTimeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventDateTimeAuditLogDetails, eventDateTime, "Event Date Time is not correct in audit logs display details");

        String usernameAuditLogDetails=pageObj.ppccLocationAuditLogPage().getUsernameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(usernameAuditLogDetails, userName, "UserName is not correct in audit logs display details");

        String eventTypeTimeAuditLogDetails=pageObj.ppccLocationAuditLogPage().getEventTypeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventTypeTimeAuditLogDetails, "Deprovisioned", "Event Type is not correct in audit logs display details");

        // Access values from changelog for deprovision event
        Map<String, String> posTypeChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("POS Type");
        Assert.assertEquals(posTypeChangeLog.get("oldValue"), "Aloha", "Old value of POS Type mismatch");
        Assert.assertEquals(posTypeChangeLog.get("newValue"), "\"\"", "New value of POS Type mismatch");
        pageObj.utils().logPass("POS Type is verified successfully");

        Map<String, String> policyIdChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Policy ID");
        Assert.assertEquals(policyIdChangeLog.get("oldValue"), String.valueOf(policyId), "Old value Policy ID mismatch");
        Assert.assertEquals(policyIdChangeLog.get("newValue"), "\"\"", "New value  Policy ID mismatch");
        pageObj.utils().logPass("POS ID is verified successfully");

        Map<String, String> policyNameChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Policy Name");
        Assert.assertEquals(policyNameChangeLog.get("oldValue"), createdPolicy, "Old value  Policy Name mismatch");
        Assert.assertEquals(policyNameChangeLog.get("newValue"), "\"\"", "New value Policy Name mismatch");
        pageObj.utils().logPass("Policy Name is verified successfully");

        Map<String, String> configStatusChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Config Status");
        Assert.assertEquals(configStatusChangeLog.get("oldValue"), "Ready to Install", "Old value Config Status mismatch");
        Assert.assertEquals(configStatusChangeLog.get("newValue"), "Unprovisioned", "New value Config Status mismatch");
        pageObj.utils().logPass("Config Status is verified successfully");

        Map<String, String> packageStatusChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Package Status");
        Assert.assertEquals(packageStatusChangeLog.get("oldValue"), "Ready to Install", "Old value Package Status mismatch");
        Assert.assertEquals(packageStatusChangeLog.get("newValue"), "Unprovisioned", "New value  Package Status mismatch");
        pageObj.utils().logPass("Package Status is verified successfully");

        Map<String, String> remoteUpgradeChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Remote Upgrade");
        Assert.assertEquals(remoteUpgradeChangeLog.get("oldValue"), "true", "Old value Remote Upgrade mismatch");
        Assert.assertEquals(remoteUpgradeChangeLog.get("newValue"), "false", "New value Remote Upgrade mismatch");
        pageObj.utils().logPass("Remote Upgrade is verified successfully");

        Map<String, String> packageVersionChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Package Version");
        Assert.assertEquals(packageVersionChangeLog.get("oldValue"),dataSet.get("packageVersion") , "Old value Package Version mismatch");
        Assert.assertEquals(packageVersionChangeLog.get("newValue"), "\"\"", "New value Package Version mismatch");
        pageObj.utils().logPass("Package Version is verified successfully");

        Map<String, String> packageVersionIdChangeLog = pageObj.ppccLocationAuditLogPage().getFieldValuesInAuditDetailsChangeLog("Package Version ID");
        Assert.assertEquals(packageVersionIdChangeLog.get("oldValue"), dataSet.get("packageVersionId"), "Old value Package Version ID mismatch");
        Assert.assertEquals(packageVersionIdChangeLog.get("newValue"), "\"\"", "New value  Package Version ID mismatch");
        pageObj.utils().logPass("Package Version Id is verified successfully");

        pageObj.ppccLocationAuditLogPage().clickCancelButtonOnLocationAuditLogDisplayDetails();
        pageObj.ppccLocationAuditLogPage().clickBackButtonOnLocationAuditLog();

        //delete created policy using api
        pageObj.utils().logit("Delete the policy");
        Response deleteApiResponse = pageObj.endpoints().deletePolicy(token, policyId);
        Assert.assertEquals(deleteApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for delete policy API");
        Assert.assertEquals(deleteApiResponse.jsonPath().getList("errors"), Collections.emptyList(), "Response has some errors");
        pageObj.utils().logPass("Created Policy is deleted Successfully.");
    }
    @Test(description = "SQ-T6189 Verify the presence of Location Group Filter")
    @Owner(name = "Kalpana")
    public void T6189_verifyThePresenceOfLocationGroupFilter() throws InterruptedException{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationPage().navigateToLocationsTab();
        String filterOption = dataSet.get("filterOption");
        boolean isFilterPresent=  pageObj.ppccLocationPage().isFilterPresent(filterOption);
        Assert.assertTrue(isFilterPresent, "Filter is present" + filterOption);
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
