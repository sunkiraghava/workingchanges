package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

//@Author = Rohit Doraya 
@Listeners(TestListeners.class)
public class InboundWebhookTest {
	static Logger logger = LogManager.getLogger(InboundWebhookTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String webhookCookie;
	private String env, run = "ui";
	private String baseUrl;
	private String segID = "11554400";
	private String segName = "Test MP Audience " + segID;
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}
	
	@Test(description = "SQ-T2952 [mParticle Inbound] Verify the mParticle Inbound Service with all events",groups = {"nonNightly" }, priority = 0)
	@Owner(name = "Rohit Doraya")
	public void T2952_VerifyInboundSegmentEvents() throws Exception {
		
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Inbound");
		webhookCookie = pageObj.webhookManagerPage().getWebhookCookie();
		logger.info("Successfully retrieved the webhook cookie: " + webhookCookie);
		
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logit(userEmail + " Api1 user signup is successful");
		
		// Segment Create Event via Inbound Webhook Service
		String segMsgUUID = UUID.randomUUID().toString();		
		Response inboundCreateSegResp = pageObj.endpoints().inboundSegmentEventGenerate(dataSet.get("proxyUrl"), dataSet.get("hostUrl"), dataSet.get("apiKey"), segName, segID, "add", segMsgUUID);
		Assert.assertEquals(inboundCreateSegResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundCreateSegResp.jsonPath().getString("id"), segMsgUUID, "ID did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundCreateSegResp.jsonPath().getString("type"), "audience_subscription_response", "Type did not matched for Inbound Segment Event API");
		logger.info("Inbound Segment create API response is " + inboundCreateSegResp);
		utils.logit("Inbound Segment create API is successful");
		
		// Segment Event Create Event via Inbound Webhook Service 
		utils.longWaitInSeconds(10);
		segMsgUUID = UUID.randomUUID().toString();		
		Response inboundDeleteSegResp = pageObj.endpoints().inboundSegmentEventGenerate(dataSet.get("proxyUrl"), dataSet.get("hostUrl"), dataSet.get("apiKey"), segName, segID, "remove", segMsgUUID);
		Assert.assertEquals(inboundDeleteSegResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundDeleteSegResp.jsonPath().getString("id"), segMsgUUID, "ID did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundDeleteSegResp.jsonPath().getString("type"), "audience_subscription_response", "Type did not matched for Inbound Segment Event API");
		logger.info("Inbound Segment create API response is " + inboundDeleteSegResp);
		utils.logit("Inbound Segment create API is successful");
		
		// Segment User Create Event via Inbound Webhook Service
		utils.longWaitInSeconds(10);
		String userMsgUUID = UUID.randomUUID().toString();		
		Response inboundSegUserCreateResp = pageObj.endpoints().inboundSegmentUserEventGenerate(dataSet.get("proxyUrl"), dataSet.get("hostUrl"), dataSet.get("apiKey"), segName, segID, "add", userMsgUUID, email, true, dataSet.get("hostUrl"), "434567890");
		Assert.assertEquals(inboundSegUserCreateResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundSegUserCreateResp.jsonPath().getString("id"), userMsgUUID, "ID did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundSegUserCreateResp.jsonPath().getString("type"), "audience_membership_change_response", "Type did not matched for Inbound Segment Event API");
		logger.info("Inbound Segment user event generated for Segment " + inboundSegUserCreateResp);
		utils.logit("Inbound Segment user event created for Segment " + segName);
		
		// Segment User Delete Event via Inbound Webhook Service
		utils.longWaitInSeconds(10);
		userMsgUUID = UUID.randomUUID().toString();		
		Response inboundSegUserDelResp = pageObj.endpoints().inboundSegmentUserEventGenerate(dataSet.get("proxyUrl"), dataSet.get("hostUrl"), dataSet.get("apiKey"), segName, segID, "delete", userMsgUUID, email, true, dataSet.get("hostUrl"), "434567890");
		Assert.assertEquals(inboundSegUserDelResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundSegUserDelResp.jsonPath().getString("id"), userMsgUUID, "ID did not matched for Inbound Segment Event API");
		Assert.assertEquals(inboundSegUserDelResp.jsonPath().getString("type"), "audience_membership_change_response", "Type did not matched for Inbound Segment Event API");
		logger.info("Inbound Segment user event generated for Segment " + inboundSegUserDelResp);
		utils.logPass("Inbound Segment user deleted for Segment " + segName);
		
		utils.longWaitInSeconds(10);
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTab("Logs");
		List<WebElement> resultList = pageObj.webhookManagerPage().fetchInboundLogsList();
		
		// Check if the resultList is empty
		Assert.assertTrue(!resultList.isEmpty(), "No inbound logs found. The result list is empty.");	
        	logger.info("Inbound logs are found");
		utils.logit("Inbound logs are found");
       
        // Validate the Segment and Segment User Events
        JSONObject segmentAddData = pageObj.webhookManagerPage().fetchInboundLogData(resultList, 3);
		Assert.assertTrue(validateLogsData(segmentAddData, segName, segID, "", "add", true, "434567890"));
		logger.info("Inbound Segment Event Add Data is matched with the expected data {}", segmentAddData);
		utils.logit("Inbound Segment Event Add Data is matched with the expected data");
		
		JSONObject segmentRemoveData = pageObj.webhookManagerPage().fetchInboundLogData(resultList, 2);
		Assert.assertTrue(validateLogsData(segmentRemoveData, segName, segID, "", "remove", true, "434567890"));
		logger.info("Inbound Segment Event Remove Data is matched with the expected data {}" , segmentRemoveData );
		utils.logit("Inbound Segment Event Remove Data is matched with the expected data");
		
		JSONObject segmentUserAddData = pageObj.webhookManagerPage().fetchInboundLogData(resultList, 1);
		Assert.assertTrue(validateLogsData(segmentUserAddData, segName, segID, email, "add", false, "434567890"));
		logger.info("Inbound Segment User Event Add Data is matched with the expected data {}", segmentUserAddData);
		utils.logit("Inbound Segment Event User Add Data is matched with the expected data");
		
		JSONObject segmentUserDeleteData = pageObj.webhookManagerPage().fetchInboundLogData(resultList, 0);
		Assert.assertTrue(validateLogsData(segmentUserDeleteData, segName, segID, email, "delete", false, "434567890"));
		logger.info("Inbound Segment User Event Delete Data is matched with the expected data {}", segmentUserDeleteData);
		utils.logPass("Inbound Segment Event User Delete Data is matched with the expected data");
	}
	
	@Test(description = "SQ-T5662 [mParticle Inbound] Verify the Inbound Segment User Validations", priority = 1)
	@Owner(name = "Rohit Doraya")
	public void T5662_VerifyInboundServiceBodyValidations() throws Exception {
		String userMsgUUID = UUID.randomUUID().toString();		
		Response inboundSegUserCreateResp = pageObj.endpoints().inboundSegmentUserEventGenerate(dataSet.get("proxyUrl"), "aegaeoninbound.staging.punchk.io", dataSet.get("apiKey"), segName, segID, "add", userMsgUUID, "abc@test.com", true, dataSet.get("hostUrl"), "434567890");
		Assert.assertEquals(inboundSegUserCreateResp.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 400 did not matched for Inbound Segment Event API with invalid host URL");
		Assert.assertEquals(inboundSegUserCreateResp.jsonPath().getString("error"), "no topic found for host URL: https://aegaeoninbound.staging.punchk.io", "Error message is not generated as expected for invalid host URL");
		logger.info("Expected error is generated for invalid host URL " + inboundSegUserCreateResp);
		utils.logPass("Expected error is generated for invalid host URL");	
	}

	@Test(description = "SQ-T5663 [mParticle Inbound] Inbound Segment Validations",groups = {"nonNightly" }, priority = 2)
	@Owner(name = "Rohit Doraya")
	public void T5663_VerifyInboundSeviceFilterLogsLogic() throws Exception {
			
	    Response inboundWebhookFilterLogs = pageObj.endpoints().getInboundWebhookFilterLogs(dataSet.get("hostUrl"), webhookCookie, "user", 0);
	    Assert.assertEquals(inboundWebhookFilterLogs.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 200 for Inbound Webhook Filter Logs");
	    Assert.assertTrue(inboundWebhookFilterLogs.jsonPath().getList("logs").size() > 0, "Inbound Webhook Filter Logs list is empty");
	        
	    Object reqBody = inboundWebhookFilterLogs.jsonPath().get("logs[0].req_body");
	    Assert.assertNull(reqBody, "req_body should be NULL but is not");
	        
	    String logId = inboundWebhookFilterLogs.jsonPath().getString("logs[0].id");
	    logger.info("Inbound webhook filter log found without body. Log ID: " + logId);
	    utils.logit("Inbound webhook filter log found without body. Log ID: " + logId);
			
	    Response inboundWebhookMsgContent = pageObj.endpoints().getInboundWebhookMessageContent(dataSet.get("hostUrl"), webhookCookie, logId);
	    Assert.assertEquals(inboundWebhookMsgContent.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Expected status code 200 for Inbound Webhook Log");
	        
	    Object reqBody1 = inboundWebhookMsgContent.jsonPath().get("log.req_body");
	    Assert.assertNotNull(reqBody1, "req_body should NOT be NULL but is null");
	        
	    String actReqType = inboundWebhookMsgContent.jsonPath().getString("log.req_body.type");
	    Assert.assertEquals(actReqType, "audience_membership_change_request", "Incorrect request body type for segment user event");
	        
	    logger.info("Message body is present for message content. Log ID: " + logId);
	    utils.logPass("Message body is present for message content. Log ID: " + logId);
	}
	
	
	private boolean validateLogsData(JSONObject actualData, String expectedName, String expectedId, String expectedEmail, String expectedAction, boolean isSegment, String mpid) {
        try {
            if (isSegment) {
                Assert.assertEquals(actualData.get("audience_name"), expectedName);
                Assert.assertEquals(actualData.get("audience_id"), Integer.parseInt(expectedId));
                Assert.assertEquals(actualData.get("action"), expectedAction);
            } else {
                JSONObject userProfile = actualData.getJSONArray("user_profiles").getJSONObject(0);
                JSONObject audience = userProfile.getJSONArray("audiences").getJSONObject(0);
                Assert.assertEquals(audience.get("audience_name"), expectedName);
                Assert.assertEquals(audience.get("audience_id"), Integer.parseInt(expectedId));
                Assert.assertEquals(audience.get("action"), expectedAction);
                Assert.assertEquals(userProfile.getJSONArray("user_identities").getJSONObject(0).get("value"), expectedEmail);
                Assert.assertEquals(userProfile.getString("mpid"), mpid);
            }
            return true;
        } catch (Exception e) {
	    logger.error("Error in validating "+ e);	
	    utils.logFail("Log data validation failed " + e);
            Assert.fail("Log data validation failed", e);
            return false;
        }
    }
    
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
