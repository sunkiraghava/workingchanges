/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains UI test cases for the POS Control Center Policy Audit Logs.
* @fileName ppccDraftPolicyAuditLogsTest.java
*/

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ppccDraftPolicyAuditLogsTest {
    static Logger logger = LogManager.getLogger(ppccDraftPolicyAuditLogsTest.class);
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

    @Test(description = "SQ-T6160 Validate the values for all columns in policy management for draft policy in audit log and detailed page for event (Created)", groups = {
            "regression" }, priority = 1)
    @Owner(name ="Aman Jain")
    public void SQ_T6160_verifyColumnsInPolicyMgmtForDraftPolicyInAuditLogAndDetailedPageForCreatedEvent()
            throws InterruptedException {
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        String policyStatus = "draft";
        String policyName = pageObj.ppccUtilities().createPolicy(token, policyStatus);
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().searchItem(policyName);

        String policyNameAfterSearch = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 1);
        Assert.assertEquals(policyNameAfterSearch, policyName, "Policy do not exists in audit logs");
        TestListeners.extentTest.get().pass("Policy name match expected value.");

        String policyId = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 2);
        String status = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 3);
        Assert.assertEquals(status, dataSet.get("expectedStatus"), "Status is not published in audit logs");
        TestListeners.extentTest.get().pass("Status match expected value.");

        String userName = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 4);
        Assert.assertEquals(userName, dataSet.get("expectedUserName"),
                "username is not coming as expected in audit logs");
        TestListeners.extentTest.get().pass("Username match expected value.");

        String eventType = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 5);
        Assert.assertEquals(eventType, dataSet.get("expectedEventType"), "Event type is not created in audit logs");
        TestListeners.extentTest.get().pass("Event type match expected value.");

        String eventDateTime = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 6);
        String createdPolicyRow = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.itemEntry");
        driver.findElement(By.xpath(createdPolicyRow)).click();

        String policyNameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyNameDetailedPage").getText();
        String policyIdDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyIdDetailedPage").getText();
        String policyStatusDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.statusDetailedPage").getText();
        String policyEventDateTimeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventDateTimeDetailedPage").getText();
        String policyUsernameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.usernameDetailedPage").getText();
        String policyEventTypeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventTypeDetailedPage").getText();

        Assert.assertEquals(policyNameDetail, policyName, "Policy name are not same");
        Assert.assertEquals(policyIdDetail, policyId, "Policy Id are not same");
        Assert.assertEquals(policyStatusDetail, status, "Policy Status are not same");
        Assert.assertEquals(policyEventDateTimeDetail, eventDateTime, "Policy event date time are not same");
        Assert.assertEquals(policyUsernameDetail, userName, "Policy username are not same");
        Assert.assertEquals(policyEventTypeDetail, eventType, "Policy event type are not same");

        pageObj.ppccPolicyAuditLogs().getCancelButtonInDetailedPage().click();
        pageObj.ppccPolicyAuditLogs().getGoToPolicyMgmtPageLocator().click();
        int id = pageObj.ppccUtilities().getPolicyId(policyName, token);
        pageObj.ppccUtilities().deletePolicy(id, token);
    }

    @Test(description = "SQ-T6161 Validate the values for all columns in policy management for draft policy in audit log and detailed page for event (deleted).", groups = {
            "regression" }, priority = 1)
    @Owner(name ="Aman Jain")
    public void SQ_T6161_verifyColumnsInPolicyMgmtForDraftPolicyInAuditLogAndDetailedPageForDeletedEvent()
            throws InterruptedException {
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        String policyStatus = "draft";
        String policyName = pageObj.ppccUtilities().createPolicy(token, policyStatus);
        int id = pageObj.ppccUtilities().getPolicyId(policyName, token);
        pageObj.ppccUtilities().deletePolicy(id, token);
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().searchItem(policyName);

        String policyNameAfterSearch = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 1);
        Assert.assertEquals(policyNameAfterSearch, policyName, "Policy do not exists in audit logs");
        TestListeners.extentTest.get().pass("Policy name match expected value.");

        String policyId = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 2);
        String status = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 3);
        Assert.assertEquals(status, dataSet.get("expectedStatus"), "Status is not published in audit logs");
        TestListeners.extentTest.get().pass("Status match expected value.");

        String userName = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 4);
        Assert.assertEquals(userName, dataSet.get("expectedUserName"),
                "username is not coming as expected in audit logs");
        TestListeners.extentTest.get().pass("Username match expected value.");

        String eventType = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 5);
        Assert.assertEquals(eventType, dataSet.get("expectedEventType"), "Event type is not created in audit logs");
        TestListeners.extentTest.get().pass("Event type match expected value.");

        String eventDateTime = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 6);
        String createdPolicyRow = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.itemEntry");
        driver.findElement(By.xpath(createdPolicyRow)).click();

        String policyNameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyNameDetailedPage").getText();
        String policyIdDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyIdDetailedPage").getText();
        String policyStatusDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.statusDetailedPage").getText();
        String policyEventDateTimeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventDateTimeDetailedPage").getText();
        String policyUsernameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.usernameDetailedPage").getText();
        String policyEventTypeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventTypeDetailedPage").getText();

        Assert.assertEquals(policyNameDetail, policyName, "Policy name are not same");
        Assert.assertEquals(policyIdDetail, policyId, "Policy Id are not same");
        Assert.assertEquals(policyStatusDetail, status, "Policy Status are not same");
        Assert.assertEquals(policyEventDateTimeDetail, eventDateTime, "Policy event date time are not same");
        Assert.assertEquals(policyUsernameDetail, userName, "Policy username are not same");
        Assert.assertEquals(policyEventTypeDetail, eventType, "Policy event type are not same");
        pageObj.ppccPolicyAuditLogs().getCancelButtonInDetailedPage().click();
    }

    @Test(description = "SQ-T6164 Validate the values for all columns in policy management for draft policy in audit log and detailed page for event (duplicated).", groups = {
            "regression" }, priority = 1)
    @Owner(name ="Aman Jain")
    public void SQ_T6164_verifyColumnsInPolicyMgmtForDraftPolicyInAuditLogAndDetailedPageForDuplicatedEvent()
            throws InterruptedException {
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        String policyStatus = "draft";
        String policyName = pageObj.ppccUtilities().createPolicy(token, policyStatus);
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        String matchPolicy = pageObj.ppccPolicyPage().searchPolicy(policyName);
        Assert.assertEquals(matchPolicy, policyName + " " + dataSet.get("expectedStatus"),
                "Policy is not created successfully");
        String duplicatedPolicyName = pageObj.ppccPolicyPage().duplicateActionOfPolicy();
        pageObj.ppccPolicyPage().clickOnSaveAsDraftButtonOnIframe();
        pageObj.utils().waitTillPagePaceDone();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().searchItem(duplicatedPolicyName);
        String policyNameAfterSearch = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 1);
        Assert.assertTrue(policyNameAfterSearch.contains(duplicatedPolicyName), "Policy do not exists in audit logs");
        TestListeners.extentTest.get().pass("Policy name match expected value.");

        String policyId = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 2);
        String status = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 3);
        Assert.assertEquals(status, dataSet.get("expectedStatus"), "Status is not published in audit logs");
        TestListeners.extentTest.get().pass("Status match expected value.");

        String userName = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 4);
        Assert.assertEquals(userName, dataSet.get("expectedUserName"),
                "username is not coming as expected in audit logs");
        TestListeners.extentTest.get().pass("Username match expected value.");

        String eventType = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 5);
        Assert.assertEquals(eventType, dataSet.get("expectedEventType"), "Event type is not created in audit logs");
        TestListeners.extentTest.get().pass("Event type match expected value.");

        String eventDateTime = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 6);
        String createdPolicyRow = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.itemEntry");
        driver.findElement(By.xpath(createdPolicyRow)).click();

        String policyNameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyNameDetailedPage").getText();
        String policyIdDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyIdDetailedPage").getText();
        String policyStatusDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.statusDetailedPage").getText();
        String policyEventDateTimeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventDateTimeDetailedPage").getText();
        String policyUsernameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.usernameDetailedPage").getText();
        String policyEventTypeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventTypeDetailedPage").getText();

        Assert.assertEquals(policyNameDetail, duplicatedPolicyName, "Policy name are not same");
        Assert.assertEquals(policyIdDetail, policyId, "Policy Id are not same");
        Assert.assertEquals(policyStatusDetail, status, "Policy Status are not same");
        Assert.assertEquals(policyEventDateTimeDetail, eventDateTime, "Policy event date time are not same");
        Assert.assertEquals(policyUsernameDetail, userName, "Policy username are not same");
        Assert.assertEquals(policyEventTypeDetail, eventType, "Policy event type are not same");

        pageObj.ppccPolicyAuditLogs().getCancelButtonInDetailedPage().click();
        pageObj.ppccPolicyAuditLogs().getGoToPolicyMgmtPageLocator().click();
        int id = pageObj.ppccUtilities().getPolicyId(policyName, token);
        pageObj.ppccUtilities().deletePolicy(id, token);
        id = pageObj.ppccUtilities().getPolicyId(duplicatedPolicyName, token);
        pageObj.ppccUtilities().deletePolicy(id, token);
    }

    @Test(description = "SQ-T6163 Validate the values for all columns in policy management for draft policy in audit log for event (updated).", groups = {
            "regression" }, priority = 1)
    @Owner(name ="Aman Jain")
    public void SQ_T6163_verifyColumnsInPolicyMgmtForDraftPolicyInAuditLogForUpdatedEvent()
            throws InterruptedException {
        String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")), dataSet.get("slug"), dataSet.get("businessName"));
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        String policyStatus = "draft";
        String policyName = pageObj.ppccUtilities().createPolicy(token, policyStatus);
        String matchPolicy = pageObj.ppccPolicyPage().searchPolicy(policyName);
        Assert.assertEquals(matchPolicy, policyName + " " + dataSet.get("expectedStatus"),
                "Policy is not created successfully");
        pageObj.ppccPolicyPage().editPolicy();
        pageObj.ppccPolicyPage().clickOnSaveAsDraftButton();
        pageObj.utils().waitTillPagePaceDone();

        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().searchItem(policyName);

        String policyNameAfterSearch = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 1);
        Assert.assertEquals(policyNameAfterSearch, policyName, "Policy do not exists in audit logs");
        TestListeners.extentTest.get().pass("Policy name match expected value.");

        String policyId = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 2);
        String status = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 3);
        Assert.assertEquals(status, dataSet.get("expectedStatus"), "Status is not published in audit logs");
        TestListeners.extentTest.get().pass("Status match expected value.");

        String userName = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 4);
        Assert.assertEquals(userName, dataSet.get("expectedUserName"),
                "username is not coming as expected in audit logs");
        TestListeners.extentTest.get().pass("Username match expected value.");

        String eventType = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 5);
        Assert.assertEquals(eventType, dataSet.get("expectedEventType"), "Event type is not created in audit logs");
        TestListeners.extentTest.get().pass("Event type match expected value.");

        String eventDateTime = pageObj.ppccPolicyAuditLogs().getRowColumnText(1, 6);
        String createdPolicyRow = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.itemEntry");
        driver.findElement(By.xpath(createdPolicyRow)).click();

        String policyNameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyNameDetailedPage").getText();
        String policyIdDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.policyIdDetailedPage").getText();
        String policyStatusDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.statusDetailedPage").getText();
        String policyEventDateTimeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventDateTimeDetailedPage").getText();
        String policyUsernameDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.usernameDetailedPage").getText();
        String policyEventTypeDetail = pageObj.utils().getLocator("ppccPolicyAuditLog.eventTypeDetailedPage").getText();

        Assert.assertEquals(policyNameDetail, policyName, "Policy name are not same");
        Assert.assertEquals(policyIdDetail, policyId, "Policy Id are not same");
        Assert.assertEquals(policyStatusDetail, status, "Policy Status are not same");
        Assert.assertEquals(policyEventDateTimeDetail, eventDateTime, "Policy event date time are not same");
        Assert.assertEquals(policyUsernameDetail, userName, "Policy username are not same");
        Assert.assertEquals(policyEventTypeDetail, eventType, "Policy event type are not same");

        WebElement languageInDetailedPage = pageObj.utils().getLocator("ppccPolicyAuditLog.changedLanguageDetailedPage");
        String value = pageObj.utils().getLocatorValue("ppccPolicyAuditLog.entryInDetailedPage");
        List<WebElement> languageValuesInDetailedPage = languageInDetailedPage.findElements(By.xpath(value));
        Assert.assertEquals(languageValuesInDetailedPage.get(0).getText().trim(), "en-US",
                "Old language did not match");
        Assert.assertEquals(languageValuesInDetailedPage.get(1).getText().trim(), "ro-RO",
                "New language did not match");

        WebElement portInDetailedPage = pageObj.utils().getLocator("ppccPolicyAuditLog.changedPortDetailedPage");
        List<WebElement> portValuesInDetailedPage = portInDetailedPage.findElements(By.xpath(value));
        Assert.assertEquals(portValuesInDetailedPage.get(0).getText().trim(), "8008", "Old port did not match");
        Assert.assertEquals(portValuesInDetailedPage.get(1).getText().trim(), "9090", "New port did not match");

        WebElement redeemMessageInDetailedPage = pageObj.utils()
                .getLocator("ppccPolicyAuditLog.changedRedeemMessageDetailedPage");
        List<WebElement> redeemMessagesValueInDetailedPage = redeemMessageInDetailedPage.findElements(By.xpath(value));
        Assert.assertEquals(redeemMessagesValueInDetailedPage.get(0).getText().trim(), "Redeem Message",
                "Old redeem message did not match");
        Assert.assertEquals(redeemMessagesValueInDetailedPage.get(1).getText().trim(), "Edit Redeem Message",
                "New redeem message did not match");

        pageObj.ppccPolicyAuditLogs().getCancelButtonInDetailedPage().click();
        pageObj.ppccPolicyAuditLogs().getGoToPolicyMgmtPageLocator().click();
        int id = pageObj.ppccUtilities().getPolicyId(policyName, token);
        pageObj.ppccUtilities().deletePolicy(id, token);
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
