package com.punchh.server.EventFrameworkTest;

import java.awt.HeadlessException;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;

import io.restassured.response.Response;

//@Author = Shaleen 
@Listeners(TestListeners.class)
public class EventFrameWorkRateLimitTest {
    static Logger logger = LogManager.getLogger(EventFrameWorkRateLimitTest.class);
    public WebDriver driver;
    String userEmail;
    String email = "autoemailTemp@punchh.com";
    ApiUtils apiUtils;
    PageObj pageObj;
    String sTCName;
    ObjectMapper mapper = new ObjectMapper();
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;
    Utilities utils;

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
    
    @Test(description = "SQ-T7026 : Automating the Rate Limit Webhook Flow with throughput checks performed via API.")
    public void T7026_verifyRateLimitWebhookFlow() throws Exception {

        int totalEventsToTrigger = 500;
        int slowTier = Integer.parseInt(dataSet.get("slowTier"));
        int mediumTier = Integer.parseInt(dataSet.get("mediumTier"));
        String topic = dataSet.get("guestEventTopic");
        String webhookID = dataSet.get("webhookIdSlow");
        String currentTimeStamp = CreateDateTime.getTimeDateString();
        long fromTimeStamp = CreateDateTime.convertTimeStampToUnixFormat(currentTimeStamp);
        long toTimeStamp = fromTimeStamp + 1800; // 0.5 hour ahead

        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.menupage().navigateToSubMenuItem("SRE", "Webhook Configuration");
        driver.switchTo().frame(0);
//		pageObj.dashboardpage().clickOnRefreshCacheButtonOnWebhookConfigDashboard();
        pageObj.dashboardpage().navigateToGlobalWebhookConfigDashboardTabs("Rate Limit Configuration");
        pageObj.dashboardpage().verifyAndUpdateRateLimitTier("Tier 1", "100", "10");
        pageObj.dashboardpage().verifyAndUpdateRateLimitTier("Tier 2", "200", "20");
        driver.switchTo().defaultContent();

        String query1 = "UPDATE users SET subscription_status = 3 , unsubscribe_reason = NULL WHERE zip_code = '" + dataSet.get("zipCode") + "' AND business_id = " + dataSet.get("business_id") + " limit 500 ;" ;
        int flag = DBUtils.executeUpdateQuery(env , query1);
        Assert.assertTrue(flag > 0, "No rows were updated in the database.");
        logger.info("Successfully updated " + flag + " rows in the database.");
        TestListeners.extentTest.get().pass("Successfully updated " + flag + " rows in the database.");

        List<String> userIDList = new ArrayList<String>();
        String[] userDetailsColumns = { "user_id" };

        String query = "select id as user_id from users where business_id = " + dataSet.get("business_id") + " and zip_code = '" + dataSet.get("zipCode") + "' limit 500 ;" ;
        List<Map<String, String>> userDetailsResults = DBUtils.executeQueryAndGetMultipleColumns(env, null , query, userDetailsColumns);

        for (Map<String, String> row : userDetailsResults) {
            userIDList.add(row.get("user_id"));
        }

        // Path to the directory
        String directoryPath = System.getProperty("user.dir") + "/resources";
        File sourceFile = null;
        File destinationFile = null;

        String reNamedCsvFilename = "BMU_DeactivateUsers_" + CreateDateTime.getTimeDateString() + ".csv";

        List<String> fileNameList = ExcelUtils.getCsvFileNameFromDir(directoryPath + "/Testdata");
        for (String str : fileNameList) {
            if (str.startsWith("BMU_DeactivateUsers_")) {
                sourceFile = new File(directoryPath + "/Testdata/" + str);
                destinationFile = new File(directoryPath + "/BMU_TestData/" + reNamedCsvFilename);

            }
        }
        Files.copy(sourceFile.toPath(), destinationFile.toPath());

        logger.info(fileNameList.get(0) + " file is renamed to " + reNamedCsvFilename);
        TestListeners.extentTest.get().pass(fileNameList.get(0) + " file is renamed to " + reNamedCsvFilename);

        String fileName = CreateDateTime.getUniqueString("BMU_DeactivateUsers_");
        String columnName = "user_id";

        // Read CSV
        List<Map<String, String>> records = ExcelUtils.readCSV(destinationFile);

        // Update CSV
        ExcelUtils.updateColumnValuesOfCsvFile(records, columnName, userIDList);

        // Write updated CSV
        ExcelUtils.writeCSV(destinationFile, records);

        Response bmuUploadResponse = pageObj.endpoints().bulkDeactivateLoayltyUsers(fileName, destinationFile,
                dataSet.get("businessAdminKey"));

        Assert.assertEquals(bmuUploadResponse.getStatusCode(), 200, " Bulk upload of users failed");
        logger.info("Bulk upload of csv file is successfully");
        TestListeners.extentTest.get().pass("Bulk upload of csv file is successfully");

        // Navigate to Business and select the business
        pageObj.menupage().navigateToSubMenuItem("Businesses", "All");
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
        String webhookCookie = pageObj.webhookManagerPage().getWebhookCookie();
        logger.info("Successfully retrieved the webhook cookie: " + webhookCookie);

        int attempts = 0;
        while (attempts < 20) {
            utils.longWaitInSeconds(30);
            logger.info("Attempt: " + (attempts + 1));
            Response barGraphLogsResp = pageObj.endpoints().getWebhookBarGraphLogs(dataSet.get("webhookUrl"), webhookCookie, fromTimeStamp, toTimeStamp);
            Assert.assertEquals(barGraphLogsResp.getStatusCode(), 200, "Failed to retrieve bar graph logs");
            logger.info("Successfully retrieved bar graph logs");
            JsonNode barGraphLogs = mapper.readTree(barGraphLogsResp.asString());
            int successCount = -1;
            for (JsonNode log : barGraphLogs.path("bar_graph_logs")) {
                if (log.get("kafka_topic").asText().equals(topic) && log.get("webhook_id").asText().equals(webhookID)) {
                    successCount = log.get("success_count").asInt();
                    break;
                }
            }

            if (successCount > (totalEventsToTrigger - 5)) {
                logger.info("Max. events have been processed successfully. Success count: " + successCount);
                break;
            }
            attempts++;
        }

        Response rateLimitResponseSlowTier = pageObj.endpoints().getWebhookRateLimitPerformanceLog(dataSet.get("webhookUrl"), webhookCookie, dataSet.get("webhookIdSlow"), fromTimeStamp, toTimeStamp);
        Assert.assertEquals(rateLimitResponseSlowTier.getStatusCode(), 200, "Failed to retrieve rate limit performance logs");
        logger.info("Successfully retrieved rate limit performance logs for slow tier webhook");
        TestListeners.extentTest.get().pass("Successfully retrieved rate limit performance logs for slow tier webhook");
        JsonNode publishRateLogs1 = mapper.readTree(rateLimitResponseSlowTier.getBody().asString());
        List<Integer> successWebhookCountValues1 = new ArrayList<>();
        for (JsonNode eachLog : publishRateLogs1.path("publish_rate_logs")) {
            successWebhookCountValues1.add(eachLog.path("success_count").asInt());
        }

        // assertion that all value is less than 100
        for (Integer successCount : successWebhookCountValues1) {
            Assert.assertTrue(successCount < slowTier, "Success count exceeds rate limit: " + successCount);
        }
        logger.info("All success counts are within the Slow Tier rate limit.");
        TestListeners.extentTest.get().pass("All success counts are within the Slow Tier rate limit.");

        // assertion that 50% of the values are greater than accepted value
        int count1 = 0;
        for (Integer successCount : successWebhookCountValues1) {
            if (successCount > (slowTier/2) ) {
                count1++;
            }
        }
        double successPercentage1 = (double) count1 / successWebhookCountValues1.size() * 100;
        Assert.assertTrue(successPercentage1 >= 50.0, "Less than 50% of success counts are greater than accepted value : " + successPercentage1 + "%");
        logger.info("At least 50% of success counts are greater than accepted value: " + successPercentage1 + "%");
        TestListeners.extentTest.get().pass("At least 50% of success counts are greater than accepted value: " + successPercentage1 + "%");

        // Medium Tier Webhook Validations

        Response rateLimitResponseMediumTier = pageObj.endpoints().getWebhookRateLimitPerformanceLog(dataSet.get("webhookUrl"), webhookCookie, dataSet.get("webhookIdMedium"), fromTimeStamp, toTimeStamp);
        Assert.assertEquals(rateLimitResponseMediumTier.getStatusCode(), 200, "Failed to retrieve rate limit performance logs");
        logger.info("Successfully retrieved rate limit performance logs for medium tier webhook");
        TestListeners.extentTest.get().pass("Successfully retrieved rate limit performance logs for medium tier webhook");
        JsonNode publishRateLogs2 = mapper.readTree(rateLimitResponseMediumTier.getBody().asString());
        List<Integer> successWebhookCountValues2 = new ArrayList<>();
        for (JsonNode eachLog : publishRateLogs2.path("publish_rate_logs")) {
            successWebhookCountValues2.add(eachLog.path("success_count").asInt());
        }

        // assertion that all value is less than 100
        for (Integer successCount : successWebhookCountValues2) {
            Assert.assertTrue(successCount < mediumTier, "Success count exceeds rate limit: " + successCount);
        }
        logger.info("All success counts are within the medium Tier rate limit.");
        TestListeners.extentTest.get().pass("All success counts are within the medium Tier rate limit.");

        // assertion that 50% of the values are greater than accepted value
        int count2 = 0;
        for (Integer successCount : successWebhookCountValues2) {
            if (successCount > (mediumTier/2) ) {
                count2++;
            }
        }
        double successPercentage2 = (double) count2 / successWebhookCountValues2.size() * 100;
        Assert.assertTrue(successPercentage2 >= 50.0, "Less than 50% of success counts are greater than accepted value : " + successPercentage2 + "%");
        logger.info("At least 50% of success counts are greater than accepted value: " + successPercentage2 + "%");
        TestListeners.extentTest.get().pass("At least 50% of success counts are greater than accepted value: " + successPercentage2 + "%");
    }

    @AfterMethod
    public void afterClass() {
        String bmuFolderPath = System.getProperty("user.dir") + "/resources/BMU_TestData";
        Utilities.clearFolder(bmuFolderPath, ".csv");
        dataSet.clear();
        driver.quit();
        logger.info("Browser closed");
    }

    public String getCurrentTimeStampAfterEventTriggered() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss a");
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(formatter);
        return formatted.toString();
    }

}
