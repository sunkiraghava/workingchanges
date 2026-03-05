/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location > Override Ui Test.
 * @fileName ppccLocationProvisionTest.java
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
import org.openqa.selenium.WebElement;
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
public class ppccLocationConfigOverrideUiTest {
    static Logger logger = LogManager.getLogger(ppccLocationConfigOverrideUiTest.class);
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

    @Test(description = "SQ-T6180 Verify the values saved for Override Pop up"
            + "SQ-T6179 Verify the UI of Config Override Pop up"
            + "SQ-T6181 Verify the clear override of single location"
            +  "SQ-T6324 Verify the audit log on performing config override of single location"
            + "SQ-T6184 Verify the filter of Provisioned Location by Has overrides")
    @Owner(name = "Kalpana")
    public void T6180_verifyTheValuesSavedForOverridePopUp() throws Exception{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");

        //create policy using api
        String createdPolicy = pageObj.ppccLocationPage().createPolicy();
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        String queryParam = "";
        Response createPolicyResponse = pageObj.endpoints().addPolicy(token,createdPolicy,1,queryParam, "published");
        Assert.assertEquals(createPolicyResponse.getStatusCode(), 200,
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
        pageObj.ppccLocationPage().navigateToLocationsTab();
        pageObj.ppccLocationPage().searchLocationsInDeprovisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().provisionALocation(createdPolicy,dataSet.get("packageVersion"));
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));

        //validate the UI of Config Override Pop up
        pageObj.ppccLocationPage().clickOnSetConfigButtonOnLocation();
        String textOnConfigOverridePopUp=pageObj.ppccLocationPage().checkStaticTextOnConfigOverridePopUp();
        Assert.assertEquals("By confirming, you will override the policy configuration for the selected location(s).", textOnConfigOverridePopUp, "Static Text is correct on Config Override Pop up");
        pageObj.utils().logPass("Static text is correct on Override Pop up.");

        WebElement addButtonOnConfigOverridePopUp = pageObj.ppccLocationPage().getAddButtonOnConfigOverridePopUp();
        Assert.assertTrue(addButtonOnConfigOverridePopUp.isDisplayed(), "Add Button is displayed on UI");
        pageObj.utils().logPass("Add Button is displayed on UI");

        String headerTextOnConfigOverridePopUp=pageObj.ppccLocationPage().checkHeaderTextOnConfigOverridePopUp();
        Assert.assertEquals("Override Configuration", headerTextOnConfigOverridePopUp, "Header Text is correct on Config Override Pop up");
        pageObj.utils().logPass("Header text is correct on Override Pop up.");
        pageObj.ppccLocationPage().clickOnXIconInConfigOverridePopUp();

        //perform Override Action
        String fieldName =dataSet.get("fieldName");
        String fieldValue=dataSet.get("fieldValue");
        pageObj.ppccLocationPage().addFieldAndValuesInConfigOverride(fieldName,fieldValue);
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        boolean isConfigOverrideVerified=pageObj.ppccLocationPage().verifyValuesSavedInConfigOverridePopUp(fieldName, fieldValue);
        Assert.assertTrue(isConfigOverrideVerified, "Values are not saved successfully in Config Override Pop Up");
        pageObj.utils().logPass("Values are saved successfully in Config Override Pop Up");

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
        Assert.assertEquals(eventType, "Config Override", "EventType do not exists in audit logs");
        pageObj.utils().logPass("EventType is correct in audit Log");

        String appVersion = pageObj.ppccLocationAuditLogPage().getAuditLogValueOfAppVersionColumn();
        Assert.assertEquals(appVersion, "Ready to Install", "appVersion do not exists in audit logs");
        pageObj.utils().logPass("appVersion is correct in audit Log");

        String eventDateTime = pageObj.ppccLocationAuditLogPage().getAuditLogRowColumnText(1, 7);

        // verify the audit log display details for Config Override Event
        pageObj.ppccLocationAuditLogPage().clickOnAuditLogRowToOpenLocationAuditLogDisplayDetails();

        String storeNameAuditLogDetails = pageObj.ppccLocationAuditLogPage().getStoreNameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(storeNameAuditLogDetails, dataSet.get("locationName"), "Store Name is not correct in audit logs display details");
        String policyIdAuditLogDetails = pageObj.ppccLocationAuditLogPage().getPolicyIdInLocationAuditLogDisplayDetails();
        Assert.assertEquals(policyIdAuditLogDetails, String.valueOf(policyId), "Policy Id is not correct in audit logs display details");

        String statusAuditLogDetails = pageObj.ppccLocationAuditLogPage().getStatusInLocationAuditLogDisplayDetails();
        Assert.assertEquals(statusAuditLogDetails, "Pending Update", "Status is not correct in audit logs display details");

        String eventDateTimeAuditLogDetails = pageObj.ppccLocationAuditLogPage().getEventDateTimeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventDateTimeAuditLogDetails, eventDateTime, "Event Date Time is not correct in audit logs display details");

        String usernameAuditLogDetails = pageObj.ppccLocationAuditLogPage()
                .getUsernameInLocationAuditLogDisplayDetails();
        Assert.assertEquals(usernameAuditLogDetails, userName, "UserName is not correct in audit logs display details");

        String eventTypeTimeAuditLogDetails = pageObj.ppccLocationAuditLogPage()
                .getEventTypeInLocationAuditLogDisplayDetails();
        Assert.assertEquals(eventTypeTimeAuditLogDetails, "Config Override",
                "Event Type is not correct in audit logs display details");

        // Access values from changelog for config override
        Map<String, String> giftCardMerchantIdChangeLog = pageObj.ppccLocationAuditLogPage()
                .getFieldValuesInAuditDetailsChangeLog("Gift Card Merchant ID");
        Assert.assertEquals(giftCardMerchantIdChangeLog.get("oldValue"), "Newly Overrided", "Old value mismatch");
        Assert.assertEquals(giftCardMerchantIdChangeLog.get("newValue"), dataSet.get("fieldValue"),
                "New value mismatch");
        pageObj.utils().logPass("Location audit log for Config Override is verified successfully");

        pageObj.ppccLocationAuditLogPage().clickCancelButtonOnLocationAuditLogDisplayDetails();
        pageObj.ppccLocationAuditLogPage().clickBackButtonOnLocationAuditLog();

        //filter the provisioned location by Has overrides filter
        String filterValue = "True";
        String filterOption = "Has Overrides";
        pageObj.ppccLocationPage().applyFilterOnLocations(filterOption, filterValue);
        String expectedValue = "View Config";
        boolean isLocationFiltered= pageObj.ppccLocationPage().isLocationsFiltered(expectedValue, filterOption);
        Assert.assertTrue(isLocationFiltered, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);

        pageObj.ppccLocationPage().removeAllFilters();
        filterValue = "False";
        pageObj.ppccLocationPage().applyFilterOnLocations(filterOption, filterValue);
        pageObj.ppccLocationPage().removeFiltersPopup();
        expectedValue = "Set Config";
        isLocationFiltered = pageObj.ppccLocationPage().isLocationsFiltered(expectedValue, filterOption);
        Assert.assertTrue(isLocationFiltered, "Locations are not filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.utils().logPass("Locations are filtered successfully by " + filterOption + " with value " + filterValue);
        pageObj.ppccLocationPage().removeAllFilters();

        // clearing the overrides
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        pageObj.ppccLocationPage().clickOnClearOverridesInConfigOverridePopUp();
        pageObj.ppccLocationPage().searchLocationsInProvisionedList(dataSet.get("locationName"));
        boolean isConfigOverrideCleared=pageObj.ppccLocationPage().verifyConfigOverrideIsCleared();
        Assert.assertTrue(isConfigOverrideCleared, "Config Override is not cleared successfully");
        pageObj.utils().logPass("Config Override is cleared successfully");

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
