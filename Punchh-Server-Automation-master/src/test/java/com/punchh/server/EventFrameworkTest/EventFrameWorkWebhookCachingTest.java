package com.punchh.server.EventFrameworkTest;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

//@Author = Shaleen
@Listeners(TestListeners.class)
public class EventFrameWorkWebhookCachingTest {
    static Logger logger = LogManager.getLogger(com.punchh.server.EventFrameworkTest.EventFrameWorkWebhookCachingTest.class);
    public WebDriver driver;
    ApiUtils apiUtils;
    PageObj pageObj;
    String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    Utilities utils;
    Response webhookLogsResp , globalConfigResp;
    int attempts ;
    Boolean flag;

    @BeforeMethod()
    public void beforeMethod(Method method) throws InterruptedException {
        driver = new BrowserUtilities().launchBrowser();
        apiUtils = new ApiUtils();
        pageObj = new PageObj(driver);
        dataSet = new ConcurrentHashMap<>();
        sTCName = method.getName();
        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();
        pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;
        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);
        logger.info(sTCName + " ==>" + dataSet);
        utils = new Utilities(driver);
    }
    
    @Test(description = "SQ-T7053 : Webhook Caching Assertions using Caching API and verification through worker.")
    public void T7053_verifyGlobalConfigWebhookCaching() throws Exception {
        
        String webhookID = dataSet.get("webhookId");
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.menupage().navigateToSubMenuItem("SRE", "Webhook Configuration");
        driver.switchTo().frame(0);
        String valueToEnter = dataSet.get("businessUUID_1") ;
        pageObj.dashboardpage().enterValueinWebhookConfigDashboard(dataSet.get("labelName"), valueToEnter);
        driver.switchTo().defaultContent();

        pageObj.menupage().navigateToSubMenuItem("Businesses", "All");
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        String webhookCookie = pageObj.webhookManagerPage().getWebhookCookie();
        driver.switchTo().defaultContent();

        attempts = 0;
        flag = false ;
        while (attempts < 6) {
            utils.longWaitInSeconds(30);
            logger.info("Attempt: " + (attempts + 1));
            globalConfigResp = pageObj.endpoints().getEventFrameWorkGlobalConfig(dataSet.get("webhookUrl"), webhookCookie);
            Assert.assertEquals(globalConfigResp.getStatusCode(), 200, "Global Config API response status code is not 200");
            String actualValue = globalConfigResp.jsonPath().get("data.evfw_used_outbound_as_lib_for_business_uuids").toString();
            if (actualValue.equals(valueToEnter)) {
                flag = true ;
                utils.longWaitInSeconds(30);
                break;
            }
            attempts++;
        }
        Assert.assertTrue(flag, "Webhook Global Config Caching value is not as expected");
        logger.info("Webhook Global Config Caching value is updated");
        TestListeners.extentTest.get().pass("Webhook Global Config Caching value is updated");

        String currentTimeStamp = CreateDateTime.getTimeDateString();
        long fromTimeStamp = CreateDateTime.convertTimeStampToUnixFormat(currentTimeStamp);
        // trigger event
        String userEmail = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");

        attempts = 0;
        while (attempts < 6) {
            utils.longWaitInSeconds(30);
            logger.info("Attempt: " + (attempts + 1));
            webhookLogsResp = pageObj.endpoints().getPayloadDetailsOfWebhookEventThroughAPI(dataSet.get("webhookUrl"), webhookCookie, fromTimeStamp, "1", "20", webhookID);
            Assert.assertEquals(webhookLogsResp.getStatusCode(), 200, "Failed to retrieve webhook logs");
            logger.info("Successfully retrieved webhook logs");
            String totalRecords = webhookLogsResp.jsonPath().get("meta.total_records").toString();
                if (!totalRecords.equals("0")) {
                    break;
                }
            attempts++;
        }

        String responseHeader = webhookLogsResp.jsonPath().get("logs[0].request_response.response_header").toString();
        Assert.assertTrue(responseHeader.contains("nginx"), "Webhook payload response does not contain expected data");
        logger.info("Webhook payload response contains expected data i.e. webhook global config caching is working as expected");
        TestListeners.extentTest.get().pass("Webhook payload response contains expected data i.e. webhook global config caching is working as expected");

        /*=======*/

        pageObj.dashboardpage().navigateToAllBusinessPage();
        pageObj.menupage().navigateToSubMenuItem("SRE", "Webhook Configuration");
        driver.switchTo().frame(0);
        String valueToEnter2 = dataSet.get("businessUUID_1") + "," + dataSet.get("businessUUID_2") ;
        pageObj.dashboardpage().enterValueinWebhookConfigDashboard(dataSet.get("labelName"), valueToEnter2);
        driver.switchTo().defaultContent();

        pageObj.menupage().navigateToSubMenuItem("Businesses", "All");
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        String webhookCookie2 = pageObj.webhookManagerPage().getWebhookCookie();

        attempts = 0;
        flag = false ;
        while (attempts < 6) {
            utils.longWaitInSeconds(30);
            logger.info("Attempt: " + (attempts + 1));
            globalConfigResp = pageObj.endpoints().getEventFrameWorkGlobalConfig(dataSet.get("webhookUrl"), webhookCookie2);
            Assert.assertEquals(globalConfigResp.getStatusCode(), 200, "Global Config API response status code is not 200");
            String actualValue2 = globalConfigResp.jsonPath().get("data.evfw_used_outbound_as_lib_for_business_uuids").toString();
            if (actualValue2.equals(valueToEnter2)) {
                flag = true ;
                utils.longWaitInSeconds(30);
                break;
            }
            attempts++;
        }
        Assert.assertTrue(flag, "Webhook Global Config Caching value is not as expected");
        logger.info("Webhook Global Config Caching value is updated");
        TestListeners.extentTest.get().pass("Webhook Global Config Caching value is updated");

        String currentTimeStamp2 = CreateDateTime.getTimeDateString();
        long fromTimeStamp2 = CreateDateTime.convertTimeStampToUnixFormat(currentTimeStamp2);
        // trigger event
        String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
        Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"), dataSet.get("secret"));
        Assert.assertEquals(signUpResponse2.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");

        attempts = 0;
        while (attempts < 6) {
            utils.longWaitInSeconds(30);
            logger.info("Attempt: " + (attempts + 1));
            webhookLogsResp = pageObj.endpoints().getPayloadDetailsOfWebhookEventThroughAPI(dataSet.get("webhookUrl"), webhookCookie2, fromTimeStamp2, "1", "20", webhookID);
            Assert.assertEquals(webhookLogsResp.getStatusCode(), 200, "Failed to retrieve webhook logs");
            logger.info("Successfully retrieved webhook logs");
            String totalRecords = webhookLogsResp.jsonPath().get("meta.total_records").toString();
            if (!totalRecords.equals("0")) {
                break;
            }
            attempts++;
        }

        String responseHeader2 = webhookLogsResp.jsonPath().get("logs[0].request_response.response_header").toString();
        Assert.assertFalse(responseHeader2.contains("nginx"), "Webhook payload response does not contain expected data");
        logger.info("Webhook payload response contains expected data i.e. webhook global config caching is working as expected");
        TestListeners.extentTest.get().pass("Webhook payload response contains expected data i.e. webhook global config caching is working as expected");
        
    }

    @AfterMethod
    public void afterClass() {
        dataSet.clear();
        driver.quit();
        logger.info("Browser closed");
    }
}

